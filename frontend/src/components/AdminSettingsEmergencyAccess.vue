<template>
  <section class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
    <h3 class="text-lg font-medium leading-6 text-gray-900">{{ t('admin.emergencyAccess.title') }}</h3>
    <p class="mt-1 text-sm text-gray-500 w-full">
      {{ t('admin.emergencyAccess.description') }}
      <a href="https://docs.cryptomator.org/hub/admin/#" target="_blank" class="ml-1 inline-flex items-center text-primary underline hover:text-primary-darker">
        {{ t('admin.emergencyAccess.learnMore') }}
        <ArrowRightIcon class="ml-1 h-4 w-4" aria-hidden="true" />
      </a>
    </p>
    <hr class="my-4 border-gray-200"/>
    <ContentBanner v-if="!entitlements.emergencyAccessEnabled" type="info" :title="t('missingEntitlements.title')">
      {{ t('missingEntitlements.description') }} <!-- TODO: link to feature comparison? -->
    </ContentBanner>
    <form v-else class="space-y-6 md:gap-6" novalidate @submit.prevent="saveRecoverySettings">
      <ContentBanner v-if="entitlements.showTrialHint" type="info" :title="t('trial.enterpriseFeature.title')" class="mb-6">
        {{ t('trial.enterpriseFeature.description') }} <!-- TODO: link to feature comparison? -->
      </ContentBanner>
      <ContentBanner
        v-if="noRedundancy"
        type="warning"
        :title="t('admin.emergencyAccess.noRedundancy.title')"
        :link-text="t('admin.emergencyAccess.learnMore')"
        link-url="https://docs.cryptomator.org/hub/admin/#"
      >
        {{ t('admin.emergencyAccess.noRedundancy.description') }}
      </ContentBanner>
      <!-- Enable Emergency Access -->
      <div class="md:grid md:grid-cols-6 md:gap-6 items-baseline">
        <label for="enableEmergencyAccess" class="col-span-2 block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
          {{ t('admin.emergencyAccess.enabled.label') }}
        </label>
        <div class="mt-2 md:mt-0 lg:col-span-3 md:col-span-4">
          <input
            id="enableEmergencyAccess"
            v-model="enableEmergencyAccess"
            type="checkbox"
            class="h-4 w-4 text-primary focus:ring-primary border-gray-300 rounded"
          />
          <label for="enableEmergencyAccess" class="ml-2 text-sm text-gray-500">
            {{ t('admin.emergencyAccess.enabled.help') }}
          </label>
        </div>
      </div>

      <!-- Key Splitting -->
      <div class="md:grid md:grid-cols-6 md:gap-6 items-baseline">
        <label for="requiredKeyShares" class="col-span-2 block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
          {{ t('admin.emergencyAccess.requiredKeys.label') }}
        </label>
        <div class="mt-1 md:mt-0 lg:col-span-3 md:col-span-4 relative">
          <div class="flex items-center gap-2">
            <div v-if="defaultRequiredEmergencyKeySharesLessThenTwoError || defaultRequiredEmergencyKeySharesToHighError instanceof FormValidationFailedError" class="absolute  -top-2 transform translate-y-full z-10">
              <div class="bg-red-50 border border-red-300 text-red-900 px-2 py-1 rounded shadow-sm text-sm hyphens-auto">
                {{ requiredKeySharesValidationText }}
              </div>
            </div>
            <div class="relative flex-1">
              <input
                id="requiredKeyShares"
                v-model.number="requiredShares"
                type="number" min="2" max="255"
                :disabled="!enableEmergencyAccess"
                class="rounded-md border-gray-300 shadow-sm sm:text-sm focus:ring-primary focus:border-primary text-left w-full disabled:cursor-not-allowed disabled:bg-gray-200"
                :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': defaultRequiredEmergencyKeySharesError || defaultRequiredEmergencyKeySharesToHighError instanceof FormValidationFailedError}"
                :aria-label="t('admin.emergencyAccess.requiredKeys.ariaLabel')"
              />
              <div v-if="defaultRequiredEmergencyKeySharesLessThenTwoError || defaultRequiredEmergencyKeySharesToHighError instanceof FormValidationFailedError" class="absolute  -top-2 transform translate-y-full z-10">
                <div class="absolute bottom-0 left-5 transform translate-y-1/2 rotate-45 w-2 h-2 bg-red-50 border-r border-b border-red-300"></div>
              </div>
              <p class="mt-2 my-4 text-sm text-gray-500">{{ t('admin.emergencyAccess.requiredKeys.help') }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- User Selection -->
      <div class="md:grid md:grid-cols-6 md:gap-6">
        <label for="searchKeyholder" class="col-span-2 block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
          {{ t('admin.emergencyAccess.keyholders.label') }}
        </label>
        <div class="mt-1 md:mt-0 lg:col-span-3 md:col-span-4">
          <div class="relative">
            <MultiUserSelectInputGroup
              v-if="enableEmergencyAccess"
              input-id="searchKeyholder"
              :selected-users="selectedUsers"
              :on-search="searchCouncilMembers"
              :input-visible="enableEmergencyAccess"
              :error-message="t('admin.emergencyAccess.keyholders.minSelected', [requiredShares])"
              :has-error="!!selectedMembersError"
              :placeholder="t('common.search.placeholder')"
              @action="selectUser"
              @remove="removeUser"
            />
            <MultiUserSelectInputGroup
              v-else
              :selected-users="selectedUsers"
              :on-search="async () => []"
              :input-visible="enableEmergencyAccess"
              :disable-action="true"
            />
            <p class="mt-2 text-sm text-gray-500">{{ t('admin.emergencyAccess.keyholders.help') }}</p>
          </div>
        </div>
      </div>

      <!-- Allow Choosing Council + Min Members -->
      <div class="md:grid md:grid-cols-6 md:gap-6">
        <span class="col-span-2"></span>
        <div class="mt-1 md:mt-0 lg:col-span-3 md:col-span-4 flex items-center h-9.5">
          <input
            id="allow"
            v-model="allowChoosing"
            :disabled="!enableEmergencyAccess"
            type="checkbox"
            class="h-4 w-4 text-primary focus:ring-primary border-gray-300 rounded"
          />
          <label for="allow" class="ml-2 text-sm text-gray-500">
            {{ t('admin.emergencyAccess.allowChoosing.label') }}
            <label for="minMembers" v-if="allowChoosing"> {{ t('admin.emergencyAccess.allowChoosing.atLeast') }}</label>
          </label>

          <div class="relative ml-2 flex-1">
            <!-- Tooltip -->
            <div
              v-if="defaultMinMembersLessThenTwoError || defaultMinMembersToHighError || defaultMinMembersLowerThenRequiredEmergencyKeySharesError instanceof FormValidationFailedError"
              class="absolute -top-2 left-0 translate-y-full z-10"
            >
              <div class="inline-block bg-red-50 border border-red-300 text-red-900 px-2 py-1 rounded shadow-sm text-sm hyphens-auto">
                {{ requiredMinMembersValidationText }}
              </div>
              <!-- Arrow -->
              <div class="absolute bottom-0 left-5 transform translate-y-1/2 rotate-45 w-2 h-2 bg-red-50 border-r border-b border-red-300"></div>
            </div>

            <!-- minMembers Input -->
            <input
              id="minMembers"
              v-model.number="minMembers"
              :disabled="!enableEmergencyAccess"
              type="number"
              min="2" max="255"
              :hidden="!allowChoosing"
              class="w-full rounded-md border-gray-300 shadow-sm sm:text-sm focus:ring-primary focus:border-primary text-left disabled:cursor-not-allowed disabled:bg-gray-200"
              :class="{
                'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500':
                  defaultMinMembersLessThenTwoError || defaultMinMembersToHighError || defaultMinMembersLowerThenRequiredEmergencyKeySharesError instanceof FormValidationFailedError
              }"
              :aria-label="t('admin.emergencyAccess.minMembers.ariaLabel')"
            />
          </div>
        </div>
      </div>

      <!-- Example Recovery -->
      <div class="md:grid md:grid-cols-6 md:gap-6">
        <span class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2 col-span-2">
          {{ t('emergencyAccess.label.exampleRecovery') }}
        </span>
        <div class="mt-1 md:mt-0 lg:col-span-3 md:col-span-4">
          <EmergencyScenarioVisualization
            :selected-users="selectedUsers"
            :required-key-shares="requiredShares!"
          />
          <p class="mt-2 text-sm text-gray-500">{{ t('admin.emergencyAccess.example.help') }}</p>
        </div>
      </div>

      <!-- Save / Undo -->
      <div class="md:grid md:grid-cols-3 md:gap-6">
        <div class="md:col-start-2 flex items-center gap-2">
          <button
            type="submit"
            :disabled="processing || !hasUnsavedChanges"
            class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
          >
            <span v-if="!updated">{{ t('admin.webOfTrust.save') }}</span>
            <span v-else>{{ t('admin.webOfTrust.saved') }}</span>
          </button>
          <div v-if="hasUnsavedChanges" class="flex items-center whitespace-nowrap gap-1 text-sm text-yellow-700">
            <ExclamationTriangleIcon class="w-4 h-4 m-1 text-yellow-500" />
            {{ t('common.unsavedChanges') }}&nbsp;
            <button type="button" class="underline hover:text-yellow-900" @click="reset">
              {{ t('common.undo') }}
            </button>
          </div>
        </div>
        <div class="md:col-start-2 flex items-center gap-2 col-span-2">
          <p v-if="false && onSaveErrorRecovery" class="mt-2 text-sm text-red-900" >
            {{ onSaveErrorRecovery!.message }}
          </p>
        </div>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ArrowRightIcon, ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import ContentBanner from './ContentBanner.vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto, ActivatedUser, didCompleteSetup } from '../common/backend';
import config from '../common/config';
import MultiUserSelectInputGroup from './MultiUserSelectInputGroup.vue';
import EmergencyScenarioVisualization from './emergencyaccess/EmergencyScenarioVisualization.vue';

const { t } = useI18n({ useScope: 'global' });

const entitlements = config.get().entitlements;
const noRedundancy = ref(false);
const isKeySplittingInvalid = ref(false);
const isMinMembersKeySplittingInvalid = ref(false);

const isDescLoading = ref(false);
let descTimer: number | undefined;

class FormValidationFailedError extends Error {
  constructor() {
    super('The form is invalid.');
  }
}
const requiredShares = ref<number>();
const minMembers = ref<number>();
const allowChoosing = ref<boolean>(false);
const enableEmergencyAccess = ref<boolean>(false);

type EmergencyAccessSettings = {
  enableEmergencyAccess: boolean;
  defaultRequiredEmergencyKeyShares?: number;
  defaultMinMembers?: number;
  allowChoosingEmergencyCouncil: boolean;
  selectedUsers: UserDto[];
};

const initialEmergencyAccessSettings = ref<EmergencyAccessSettings>({ enableEmergencyAccess: false, defaultRequiredEmergencyKeyShares: 0, defaultMinMembers: 0, allowChoosingEmergencyCouncil: false, selectedUsers: [] });

const initialCouncilMembers = ref<UserDto[]>([]);
const addedCouncilMembers = ref<UserDto[]>([]);
const selectedUsers = computed(() => [...initialCouncilMembers.value, ...addedCouncilMembers.value]);

const processing = ref(false);
const updated = ref(false);

const selectedCouncilUserIds = computed(() => selectedUsers.value.map(u => u.id).sort().join(','));
const initialCouncilUserIds = computed(() => initialEmergencyAccessSettings.value.selectedUsers.map(u => u.id).sort().join(','));

const sameCouncilMemberIds = computed(() => {
  return initialCouncilUserIds.value === selectedCouncilUserIds.value;
});
const hasUnsavedChanges = computed(() => {
  return (
    initialEmergencyAccessSettings.value.enableEmergencyAccess !== enableEmergencyAccess.value ||
    initialEmergencyAccessSettings.value.defaultRequiredEmergencyKeyShares !== requiredShares.value ||
    initialEmergencyAccessSettings.value.defaultMinMembers !== minMembers.value ||
    initialEmergencyAccessSettings.value.allowChoosingEmergencyCouncil !== allowChoosing.value ||
    !sameCouncilMemberIds.value
  );
});

async function fetchEmergencyAccess() {
  const allUsers = await backend.users.listAll();
  const s = await backend.settings.get();

  const selected = s.emergencyCouncilMemberIds
    .map((id: string) => allUsers.find(u => u.id === id))
    .filter((u): u is UserDto => !!u)
    .sort((a, b) => a.name.localeCompare(b.name));

  initialCouncilMembers.value = selected;
  addedCouncilMembers.value = [];

  requiredShares.value = s.defaultRequiredEmergencyKeyShares;
  minMembers.value = s.defaultMinMembers;
  allowChoosing.value = s.allowChoosingEmergencyCouncil;
  enableEmergencyAccess.value = s.enableEmergencyAccess;

  initialEmergencyAccessSettings.value = {
    enableEmergencyAccess: enableEmergencyAccess.value,
    defaultRequiredEmergencyKeyShares: requiredShares.value,
    defaultMinMembers: minMembers.value,
    allowChoosingEmergencyCouncil: allowChoosing.value,
    selectedUsers: [...selectedUsers.value]
  };
}

async function searchCouncilMembers(query: string): Promise<ActivatedUser[]> {
  const existing = new Set(selectedUsers.value.map(m => m.id));
  const auths = await backend.authorities.search(query, true);
  return auths
    .filter(a => a.type === 'USER')
    .filter(a => didCompleteSetup(a))
    .filter(a => !existing.has(a.id))
    .sort((a, b) => a.name.localeCompare(b.name));
}
function selectUser(u: UserDto) {
  if (!selectedUsers.value.some(x => x.id === u.id)) addedCouncilMembers.value.push(u);
}
function removeUser(u: UserDto) {
  initialCouncilMembers.value = initialCouncilMembers.value.filter(x => x.id !== u.id);
  addedCouncilMembers.value = addedCouncilMembers.value.filter(x => x.id !== u.id);
}

const requiredKeySharesValidationText = computed(() => {
  if (defaultRequiredEmergencyKeySharesToHighError.value != null) return t('admin.emergencyAccess.validation.maxValue', [255]);
  else if (defaultRequiredEmergencyKeySharesLessThenTwoError.value != null) return t('admin.emergencyAccess.validation.minValue', [2]);
  return '';
});

const requiredMinMembersValidationText = computed(() => {
  if (defaultMinMembersToHighError.value != null) return t('admin.emergencyAccess.validation.maxValue', [255]);
  else if (defaultMinMembersLessThenTwoError.value != null) return t('admin.emergencyAccess.validation.minValue', [2]);
  else if (defaultMinMembersLowerThenRequiredEmergencyKeySharesError.value != null) return t('admin.emergencyAccess.validation.minMembersAtLeastRequiredShares');
  return '';
});

const onSaveErrorRecovery = ref<Error | null>(null);

const defaultRequiredEmergencyKeySharesLessThenTwoError = ref<Error | null>(null);
const defaultRequiredEmergencyKeySharesToHighError = ref<Error | null>(null);
const defaultRequiredEmergencyKeySharesError = ref<Error | null>(null);

const defaultMinMembersLessThenTwoError = ref<Error | null>(null);
const defaultMinMembersToHighError = ref<Error | null>(null);
const defaultMinMembersLowerThenRequiredEmergencyKeySharesError = ref<Error | null>(null);

const selectedMembersError = ref<Error | null>(null);

watch([selectedUsers, requiredShares], ([users, shares]) => {
  noRedundancy.value = !!shares && users.length === shares;
});

watch([requiredShares], ([r]) => {
  isDescLoading.value = true;
  isKeySplittingInvalid.value = false;
  isMinMembersKeySplittingInvalid.value = false;

  defaultRequiredEmergencyKeySharesLessThenTwoError.value = null;
  defaultRequiredEmergencyKeySharesToHighError.value = null;

  if (r! >= 255 || r! < 2){
    isKeySplittingInvalid.value = true;
  }
  if (descTimer) window.clearTimeout(descTimer);
  descTimer = window.setTimeout(() => { isDescLoading.value = false; }, 350);
});

function validateRecoverySettings(): boolean {
  defaultRequiredEmergencyKeySharesError.value = null;
  selectedMembersError.value = null;
  onSaveErrorRecovery.value = null;
  defaultRequiredEmergencyKeySharesLessThenTwoError.value = null;
  defaultRequiredEmergencyKeySharesToHighError.value = null;
  defaultMinMembersLessThenTwoError.value = null;
  defaultMinMembersToHighError.value = null;
  defaultMinMembersLowerThenRequiredEmergencyKeySharesError.value = null;
  
  if (requiredShares.value == null || minMembers.value == null) {
    onSaveErrorRecovery.value = new Error('Missing input');
    return false;
  }
  if (requiredShares.value < 2) {
    defaultRequiredEmergencyKeySharesLessThenTwoError.value = new FormValidationFailedError();
    return false;
  }
  if (requiredShares.value > 255) {
    defaultRequiredEmergencyKeySharesToHighError.value = new FormValidationFailedError();
    return false;
  }

  if (minMembers.value < 2) {
    defaultMinMembersLessThenTwoError.value = new FormValidationFailedError();
    return false;
  }
  if (minMembers.value > 255) {
    defaultMinMembersToHighError.value = new FormValidationFailedError();
    return false;
  }

  if (allowChoosing.value && requiredShares.value > minMembers.value) {
    defaultMinMembersLowerThenRequiredEmergencyKeySharesError.value = new FormValidationFailedError();
    onSaveErrorRecovery.value = new Error(t('admin.emergencyAccess.errors.sharesMustNotExceedMembers'));
    return false;
  }
  if (selectedUsers.value.length < requiredShares.value) {
    selectedMembersError.value = new FormValidationFailedError();
    return false;
  }
  return true;
}

async function saveRecoverySettings() {
  if (enableEmergencyAccess.value && !validateRecoverySettings()) {
    return;
  }
  try {
    processing.value = true;
    await backend.settings.update({
      enableEmergencyAccess: enableEmergencyAccess.value,
      defaultRequiredEmergencyKeyShares: requiredShares.value,
      defaultMinMembers: allowChoosing.value ? minMembers.value : selectedUsers.value.length,
      allowChoosingEmergencyCouncil: allowChoosing.value,
      emergencyCouncilMemberIds: selectedUsers.value.map(u => u.id),
    });
    initialEmergencyAccessSettings.value = {
      enableEmergencyAccess: enableEmergencyAccess.value,
      defaultRequiredEmergencyKeyShares: requiredShares.value,
      defaultMinMembers: allowChoosing.value ? minMembers.value : selectedUsers.value.length,
      allowChoosingEmergencyCouncil: allowChoosing.value,
      selectedUsers: selectedUsers.value
    };

    updated.value = true;
    setTimeout(() => (updated.value = false), 2000);
  } catch (e: any) {
    onSaveErrorRecovery.value = e instanceof Error ? e : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

function reset() {
  requiredShares.value = initialEmergencyAccessSettings.value.defaultRequiredEmergencyKeyShares;
  minMembers.value = initialEmergencyAccessSettings.value.defaultMinMembers;
  allowChoosing.value = initialEmergencyAccessSettings.value.allowChoosingEmergencyCouncil;
  initialCouncilMembers.value = [...initialEmergencyAccessSettings.value.selectedUsers];
  enableEmergencyAccess.value = initialEmergencyAccessSettings.value.enableEmergencyAccess;
  addedCouncilMembers.value = [];
  defaultRequiredEmergencyKeySharesError.value = null;
  selectedMembersError.value = null;
  onSaveErrorRecovery.value = null;
}

watch(() => [selectedUsers.value.map(u => u.id).join(','), minMembers.value, requiredShares.value],
  () => { 
    selectedMembersError.value = null; 
    defaultRequiredEmergencyKeySharesError.value = null; 
    defaultMinMembersLowerThenRequiredEmergencyKeySharesError.value = null;
  });
onMounted(async () => {
  await fetchEmergencyAccess();
  updateNoRedundancy();
});

function updateNoRedundancy() {
  const shares = requiredShares.value;
  if (!shares) {
    noRedundancy.value = false;
    return;
  }
  noRedundancy.value = selectedUsers.value.length === shares;
}
</script>
