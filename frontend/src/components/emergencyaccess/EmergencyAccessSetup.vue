<template>
  <div class="relative">
    <div class="flex items-center justify-between gap-2 pt-2 pb-2">
      <label :for="id + '-cm'" class="text-sm font-medium text-gray-700 flex items-center">
        {{ t('emergencyAccess.label.councilMembers') }}
      </label>
      <button
        v-if="hasCouncilChanges"
        type="button"
        class="inline-flex cursor-pointer items-center justify-center rounded text-primary hover:text-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2"
        :aria-label="t('common.reset')"
        :title="t('common.reset')"
        @click="resetCouncilMembers()"
      >
        <ArrowUturnLeftIcon class="h-4 w-4" aria-hidden="true" />
      </button>
    </div>
    <MultiUserSelectInputGroup
      :selected-users="emergencyCouncilMembers"
      :on-search="searchCouncilMembers"
      :input-id="id + '-cm'"
      :input-visible="allowChangingDefaults"
      @action="addCouncilMember"
      @remove="removeCouncilMember"
    />
    <div
      v-if="allowChangingDefaults && emergencyCouncilMembers.length < minMembers"
      class="mt-1 flex items-start gap-2 rounded-md border border-yellow-200 bg-yellow-50 p-1 text-sm text-gray-900"
    >
      <span class="leading-5">
        <span class="text-gray-600">
          {{ t('emergencyAccess.validation.selectMoreCouncilMembers', [minMembers - emergencyCouncilMembers.length]) }}
        </span>
      </span>
    </div>
  </div>

  <span class="block text-sm font-medium text-gray-700 pt-4">
    {{ t('emergencyAccess.label.exampleRecovery') }}
  </span>
  <EmergencyScenarioVisualization
    :selected-users="emergencyCouncilMembers"
    :required-key-shares="requiredKeyShares"
    :min-members="minMembers"
  />
  <div v-if="requiredKeyShares === emergencyCouncilMembers.length && allowChangingDefaults" class="mt-4 mr-3">
    <span class="inline-flex items-center gap-2 rounded-full bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800">
      <ExclamationTriangleIcon class="h-4 w-4" aria-hidden="true" />
      {{ t('emergencyAccess.noRedundancy') }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { ArrowUturnLeftIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/solid';
import { useId, computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { ActivatedUser, didCompleteSetup } from '../../common/backend';
import { VaultKeys } from '../../common/crypto';
import { EmergencyAccess } from '../../common/emergencyaccess';
import { wordEncoder } from '../../common/util';
import MultiUserSelectInputGroup from '../MultiUserSelectInputGroup.vue';
import EmergencyScenarioVisualization from './EmergencyScenarioVisualization.vue';

const { t } = useI18n({ useScope: 'global' });
const id = useId();

export type SplitResult = {
  keyShares: Record<string, string>;
  requiredKeyShares: number;
};

// from settings:
const defaultEmergencyCouncilMembers = ref<ActivatedUser[]>([]);
const defaultRequiredEmergencyKeyShares = ref<number>(0);
const minMembers = ref<number>(0);
const allowChangingDefaults = ref<boolean>(false);
const requiredKeyShares = ref<number>(0);

// user choice:
const emergencyCouncilMembers = ref<ActivatedUser[]>([]);

// validation:
const isInvalidKeyShares = computed(() => requiredKeyShares.value < 1);
const isInvaildCouncilMembers = computed(() => emergencyCouncilMembers.value.length < 1);
const hasTooFewCouncilMembers = computed(() =>
  emergencyCouncilMembers.value.length < requiredKeyShares.value
  || (allowChangingDefaults.value && emergencyCouncilMembers.value.length < minMembers.value)
);
const hasCouncilChanges = computed(() => {
  if (emergencyCouncilMembers.value.length !== defaultEmergencyCouncilMembers.value.length) {
    return true;
  }

  const defaultIds = new Set(defaultEmergencyCouncilMembers.value.map(member => member.id));
  return emergencyCouncilMembers.value.some(member => !defaultIds.has(member.id));
});
const hasValidationErrors = computed(() =>
  isInvalidKeyShares.value || isInvaildCouncilMembers.value || hasTooFewCouncilMembers.value
);

defineExpose({
  split,
  hasValidationErrors,
  allowChangingDefaults
});

onMounted(async () => {
  await initialize();
});

async function split(vaultKeys: VaultKeys): Promise<SplitResult> {
  if (requiredKeyShares.value < 1) {
    throw new Error(t('grantEmergencyAccessDialog.error.keySharesRequired'));
  }

  if (emergencyCouncilMembers.value.length < 1) {
    throw new Error(t('grantEmergencyAccessDialog.error.councilMembersRequired'));
  }

  if (emergencyCouncilMembers.value.length < requiredKeyShares.value) {
    throw new Error(t('grantEmergencyAccessDialog.error.tooFewCouncilMembers'));
  }

  const recoveryKey = await vaultKeys.createRecoveryKey();
  const recoveryKeyBytes = wordEncoder.decode(recoveryKey); // TODO: remove encode/decode once UVF is merged
  const keyShares = await EmergencyAccess.split(
    recoveryKeyBytes,
    requiredKeyShares.value,
    ...emergencyCouncilMembers.value
  );

  return {
    keyShares: keyShares,
    requiredKeyShares: requiredKeyShares.value
  };
}

async function initialize() {
  const settings = await backend.settings.get();
  allowChangingDefaults.value = settings.allowChoosingEmergencyCouncil;
  minMembers.value = settings.defaultMinMembers;
  defaultRequiredEmergencyKeyShares.value = settings.defaultRequiredEmergencyKeyShares;
  requiredKeyShares.value = settings.defaultRequiredEmergencyKeyShares;

  const authorities = await backend.authorities.listSome(settings.emergencyCouncilMemberIds);
  const sortedActivatedUsers = authorities
    .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
    .sort((a, b) => a.name.localeCompare(b.name));

  defaultEmergencyCouncilMembers.value = [...sortedActivatedUsers];
  emergencyCouncilMembers.value = [...sortedActivatedUsers];
}

async function searchCouncilMembers(query: string): Promise<ActivatedUser[]> {
  const existingIds = new Set(emergencyCouncilMembers.value.map(member => member.id));
  const authorities = await backend.authorities.search(query, true);
  return authorities
    .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
    .filter(a => !existingIds.has(a.id))
    .sort((a, b) => a.name.localeCompare(b.name));
}

function addCouncilMember(authority: ActivatedUser) {
  const alreadyExists = emergencyCouncilMembers.value.some(u => u.id === authority.id);

  if (!alreadyExists) {
    emergencyCouncilMembers.value  = [...emergencyCouncilMembers.value, authority];
  }
}

function removeCouncilMember(user: ActivatedUser) {
  emergencyCouncilMembers.value = emergencyCouncilMembers.value.filter(u => u.id !== user.id);
}

function resetCouncilMembers() {
  emergencyCouncilMembers.value = [...defaultEmergencyCouncilMembers.value];
}
</script>
