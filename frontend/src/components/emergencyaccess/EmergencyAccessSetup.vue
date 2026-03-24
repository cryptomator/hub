<template>
  <div class="relative">
    <div class="flex items-center justify-between gap-2 pt-2 pb-2">
      <label :for="id + '-cm'" class="text-sm font-medium text-gray-700 flex items-center">
        {{ readonly || !allowChoosingCouncil ? t('emergencyAccess.label.newCouncilMembers') : t('emergencyAccessDialog.label.councilMembersAtLeast', [minMembers]) }}
      </label>
      <button
        v-if="hasCouncilChanges && !readonly"
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
      :input-visible="allowChoosingCouncil && !readonly"
      :error-message="t('emergencyAccess.validation.minimumMembers', [minMembers])"
      :has-error="hasValidationErrors"
      @action="addCouncilMember"
      @remove="removeCouncilMember"
    />
  </div>

  <div v-if="showRequiredKeyShares" class="mt-4 space-y-1 text-sm text-gray-500">
    <div>
      <span class="font-medium text-gray-700">{{ t('emergencyAccess.requiredKeyShares') }}:</span>
      {{ requiredKeyShares }}
    </div>
  </div>

  <span class="block text-sm font-medium text-gray-700 pt-4">
    {{ readonly ? t('emergencyAccess.label.possibleScenario') : t('emergencyAccess.label.exampleRecovery') }}
  </span>
  <EmergencyScenarioVisualization
    :selected-users="emergencyCouncilMembers"
    :required-key-shares="requiredKeyShares"
  />
  <div v-if="requiredKeyShares === emergencyCouncilMembers.length && allowChoosingCouncil && !readonly" class="mt-4 mr-3">
    <span class="inline-flex items-center gap-2 rounded-full bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800">
      <ExclamationTriangleIcon class="h-4 w-4" aria-hidden="true" />
      {{ t('emergencyAccess.noRedundancy') }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { ArrowUturnLeftIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/solid';
import { useId, computed, onMounted, ref, isReadonly } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { ActivatedUser, RecoveredKeyShareDto, didCompleteSetup } from '../../common/backend';
import { VaultKeys } from '../../common/crypto';
import { EmergencyAccess } from '../../common/emergencyaccess';
import { wordEncoder } from '../../common/util';
import MultiUserSelectInputGroup from '../MultiUserSelectInputGroup.vue';
import EmergencyScenarioVisualization from './EmergencyScenarioVisualization.vue';

const { t } = useI18n({ useScope: 'global' });
const id = useId();

const props = withDefaults(defineProps<{
  currentEmergencyCouncilMembers?: ActivatedUser[];
  readonly?: boolean;
  showRequiredKeyShares?: boolean;
  allowChoosingCouncil?: boolean;
}>(), {
  currentEmergencyCouncilMembers: () => [],
  readonly: false,
  showRequiredKeyShares: false,
  allowChoosingCouncil: false,
});

export type SplitResult = {
  keyShares: Record<string, string>;
  requiredKeyShares: number;
};

// from settings:
const defaultRequiredEmergencyKeyShares = ref<number>(0);
const minMembers = ref<number>(0);
const requiredKeyShares = ref<number>(0);

// current council:
const currentEmergencyCouncilMembers = ref<ActivatedUser[]>([...props.currentEmergencyCouncilMembers]);

// user choice:
const emergencyCouncilMembers = ref<ActivatedUser[]>([]);

// validation:
const isInvalidKeyShares = computed(() => requiredKeyShares.value < 1);
const isInvalidCouncilMembers = computed(() => emergencyCouncilMembers.value.length < 1);
const hasTooFewCouncilMembers = computed(() =>
  emergencyCouncilMembers.value.length < requiredKeyShares.value
  || (props.allowChoosingCouncil && emergencyCouncilMembers.value.length < minMembers.value)
);
const hasCouncilChanges = computed(() => {
  if (emergencyCouncilMembers.value.length !== currentEmergencyCouncilMembers.value.length) {
    return true;
  }

  const defaultIds = new Set(currentEmergencyCouncilMembers.value.map(member => member.id));
  return emergencyCouncilMembers.value.some(member => !defaultIds.has(member.id));
});
const hasValidationErrors = computed(() =>
  isInvalidKeyShares.value || isInvalidCouncilMembers.value || hasTooFewCouncilMembers.value
);

defineExpose({
  split,
  hasValidationErrors,
  requiredKeyShares,
  emergencyCouncilMembers
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
  minMembers.value = settings.defaultMinMembers;
  defaultRequiredEmergencyKeyShares.value = settings.defaultRequiredEmergencyKeyShares;
  requiredKeyShares.value = settings.defaultRequiredEmergencyKeyShares;

  // if there is no current council, load default from settings:
  if (currentEmergencyCouncilMembers.value.length === 0) {
    const authorities = await backend.authorities.listSome(settings.emergencyCouncilMemberIds);
    const sortedActivatedUsers = authorities
      .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
      .sort((a, b) => a.name.localeCompare(b.name));
    currentEmergencyCouncilMembers.value = [...sortedActivatedUsers];
  }

  // pre-initialize emergencyCouncilMembers with current council or default from settings:
  if (emergencyCouncilMembers.value.length === 0) {
    emergencyCouncilMembers.value = [...currentEmergencyCouncilMembers.value];
  }
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
  emergencyCouncilMembers.value = [...currentEmergencyCouncilMembers.value];
}
</script>
