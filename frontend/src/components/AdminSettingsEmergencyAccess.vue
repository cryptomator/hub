<template>
  <section class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
    <h3 class="text-lg font-medium leading-6 text-gray-900">{{ t('admin.emergencyAccess.title') }}</h3>
    <p class="mt-1 text-sm text-gray-500 w-full">{{ t('admin.emergencyAccess.description') }}</p>
    <hr class="my-4 pb-6 border-gray-200"/>

    <form class="space-y-6 md:gap-6" novalidate @submit.prevent="saveRecoverySettings">
      <!-- Key Splitting -->
      <div class="md:grid md:grid-cols-6 md:gap-6">
        <label class="col-span-2 block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
          {{ t('admin.emergencyAccess.keySplitting.title') }}
        </label>
        <div class="mt-1 md:mt-0 lg:col-span-3 md:col-span-4 relative">
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500">{{ t('admin.emergencyAccess.keySplitting.require') }}</span>

            <div v-if="defaultRequiredEmergencyKeySharesLowerThenMinMembersError || defaultRequiredEmergencyKeySharesLessThenTwoError || defaultRequiredEmergencyKeySharesToHighError instanceof FormValidationFailedError" class="absolute  -top-2 transform translate-y-[-100%] z-10">
              <div class="bg-red-50 border border-red-300 text-red-900 px-2 py-1 rounded shadow-sm text-sm hyphens-auto">
                {{ requiredKeySharesValidationText }}
              </div>
            </div>
            <div>
              <input
                v-model.number="requiredShares"
                type="number" min="2" max="255"
                class="w-20 rounded-md border-gray-300 shadow-sm sm:text-sm focus:ring-primary focus:border-primary text-left"
                :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': defaultRequiredEmergencyKeySharesError || defaultRequiredEmergencyKeySharesToHighError || defaultRequiredEmergencyKeySharesLowerThenMinMembersError instanceof FormValidationFailedError}"
                aria-label="Required shares"
              />
              <div v-if="defaultRequiredEmergencyKeySharesLowerThenMinMembersError || defaultRequiredEmergencyKeySharesLessThenTwoError || defaultRequiredEmergencyKeySharesToHighError instanceof FormValidationFailedError" class="absolute  -top-2 transform translate-y-[-100%] z-10">
                <div class="absolute bottom-0 left-5 transform translate-y-1/2 rotate-45 w-2 h-2 bg-red-50 border-r border-b border-red-300"></div>
              </div>
            </div>

            <span class="text-sm text-gray-500">{{ t('admin.emergencyAccess.keySplitting.outOf') }}</span>
            <div>
              <div v-if="defaultMinMembersLessThenTwoError || defaultMinMembersToHighError instanceof FormValidationFailedError" class="absolute -top-2 transform translate-y-[-100%] z-10">
                <div class="bg-red-50 border border-red-300 text-red-900 px-2 py-1 rounded shadow-sm text-sm hyphens-auto">
                  {{ requiredMinMembersValidationText }}
                </div>
              </div>
              <input
                v-model.number="minMembers"
                type="number" min="2" max="255"
                class="w-20 rounded-md border-gray-300 shadow-sm sm:text-sm focus:ring-primary focus:border-primary text-left"
                :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': defaultMinMembersLessThenTwoError || defaultMinMembersToHighError instanceof FormValidationFailedError }"
                aria-label="Min members"
              />
              <div v-if="defaultMinMembersLessThenTwoError || defaultMinMembersToHighError instanceof FormValidationFailedError" class="absolute  -top-2 transform translate-y-[-100%] z-10">
                <div class="absolute bottom-0 left-5 transform translate-y-1/2 rotate-45 w-2 h-2 bg-red-50 border-r border-b border-red-300"></div>
              </div>
            </div>
            <span class="ml-1 text-sm text-gray-500">{{ t('admin.emergencyAccess.keySplitting.keyShards') }}</span>
          </div>
          <p class="mt-2 text-sm text-gray-500">
            {{ t('admin.emergencyAccess.keySplitting.desc') }}
          </p>
          <div class="mt-2">
            <div v-if="!isKeySplittingInvalid" class="flex items-start gap-2 rounded-md border border-gray-200 bg-gray-50 p-3 text-sm text-gray-900">
              <span v-if="isDescLoading" class="mt-0.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"></span>
              <InformationCircleIcon v-else class="mt-0.5 h-8 w-8 max-w-4 max-h-4 text-gray-400" aria-hidden="true" />
              <span class="leading-5">
                <span v-if="isDescLoading">{{ t('common.loading') }}</span>
                <span v-else class="text-gray-500">
                  {{ t('admin.emergencyAccess.keyShardsDesc', [minMembers, requiredShares]) }}
                </span>
              </span>
              <SegmentRing
                v-if="!isDescLoading"
                class="ml-auto shrink-0"
                :total="requiredShares!"
                :completed="requiredShares!"
                :width="36"
                :height="36"
                fill-color="#66cc68bb"
              />
            </div>
            <div v-else class="flex items-start gap-2 rounded-md border border-gray-200 bg-gray-50 p-3 text-sm text-gray-900">
              <span v-if="isDescLoading" class="mt-0.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"></span>
              <InformationCircleIcon v-else class="mt-0.5 h-4 w-4 text-gray-400" aria-hidden="true" />
              <span class="leading-5">
                <span v-if="isDescLoading">{{ t('common.loading') }}</span>
                <span v-else-if="isKeySplittingInvalid">
                  {{ t('admin.emergencyAccess.keySplitting.errors.invalid') }}
                </span>
              </span>
            </div>
            <!-- Needs Redundancy badge -->
            <div v-if="noRedundancy" class="mt-2">
              <span
                class="inline-flex items-center gap-2 rounded-md bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800"
                :title="t('emergencyAccessVaultList.noRedundancyHint')"
              >
                <ExclamationTriangleIcon class="h-6 w-6" aria-hidden="true" />
                {{ t('admin.emergencyAccess.errors.noRedundancy') }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- User Selection -->
      <div class="md:grid md:grid-cols-6 md:gap-6">
        <label class="col-span-2 block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
          Who shall retrieve Key Shards?
        </label>
        <div class="mt-1 md:mt-0 lg:col-span-3 md:col-span-4">
          <div class="relative">
            <MultiUserSelectInputGroup
              :selected-users="selectedUsers"
              :on-search="searchCouncilMembers"
              :input-visible="true"
              :error-message="t('admin.emergencyAccess.councilMembers.errors.notEnoughMembers', [minMembers])"
              :has-error="!!selectedMembersError"
              :placeholder="t('common.search')"
              @action="selectUser"
              @remove="removeUser"
            />
            <p class="mt-2 text-sm text-gray-500">The selected users are responsible for vault recovery.</p>
          </div>
        </div>
      </div>

      <!-- Allow Choosing Council -->
      <div class="md:grid md:grid-cols-6 md:gap-6">
        <label class="col-span-2"></label>
        <div class="mt-1 md:mt-0 col-span-3 flex items-center">
          <input id="allow" v-model="allowChoosing" type="checkbox" class="h-4 w-4 text-primary focus:ring-primary border-gray-300 rounded"/>
          <label for="allow" class="ml-2 text-sm text-gray-500">
            {{ t('admin.emergencyAccess.letChooseDifferentUsersCheckbox.description') }}
          </label>
        </div>
      </div>

      <!-- Example Recovery -->
      <div class="md:grid md:grid-cols-6 md:gap-6">
        <label class="block text-sm text-gray-700 md:text-right md:pr-4 md:mt-2 col-span-2">
          {{ t('admin.emergencyAccess.exampleRecovery.label') }}
        </label>
        <div class="mt-1 md:mt-0 lg:col-span-3 md:col-span-4">
          <EmergencyScenarioVisualization
            :selected-users="selectedUsers"
            :required-key-shares="requiredShares!"
            :min-members="minMembers!"
          />
          <p class="mt-2 text-sm text-gray-500">{{ t('admin.emergencyAccess.exampleRecovery.description') }}</p>
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
import { ExclamationTriangleIcon, InformationCircleIcon } from '@heroicons/vue/20/solid';
import { useI18n } from 'vue-i18n';
import backend, { UserDto, ActivatedUser, didCompleteSetup } from '../common/backend';
import MultiUserSelectInputGroup from './MultiUserSelectInputGroup.vue';
import SegmentRing from './emergencyaccess/SegmentRing.vue';
import EmergencyScenarioVisualization from './emergencyaccess/EmergencyScenarioVisualization.vue';

const { t } = useI18n({ useScope: 'global' });

const noRedundancy = ref(false);
const isKeySplittingInvalid = ref(false);
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

type EmergencyAccessSettings = {
  defaultRequiredEmergencyKeyShares: number;
  defaultMinMembers: number;
  allowChoosingEmergencyCouncil: boolean;
  selectedUsers: UserDto[];
};

const initialEmergencyAccessSettings = ref<EmergencyAccessSettings>({ defaultRequiredEmergencyKeyShares: 0, defaultMinMembers: 0, allowChoosingEmergencyCouncil: false, selectedUsers: [] });

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
    initialEmergencyAccessSettings.value.defaultRequiredEmergencyKeyShares !== requiredShares.value ||
    initialEmergencyAccessSettings.value.defaultMinMembers !== minMembers.value ||
    initialEmergencyAccessSettings.value.allowChoosingEmergencyCouncil !== allowChoosing.value ||
    !sameCouncilMemberIds.value
  );
});

async function fetchEmergencyAccess() {
  const allUsers = await backend.users.listAll();
  const s = await backend.settings.get();

  // Members setzen
  const selected = s.emergencyCouncilMemberIds
    .map((id: string) => allUsers.find(u => u.id === id))
    .filter((u): u is UserDto => !!u)
    .sort((a, b) => a.name.localeCompare(b.name));

  initialCouncilMembers.value = selected;
  addedCouncilMembers.value = [];

  // numerische Settings
  requiredShares.value = s.defaultRequiredEmergencyKeyShares;
  minMembers.value = s.defaultMinMembers;
  allowChoosing.value = s.allowChoosingEmergencyCouncil;

  initialEmergencyAccessSettings.value = {
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
  if (defaultRequiredEmergencyKeySharesToHighError.value != null) return t('admin.emergencyAccess.keySplitting.errors.maxValue');
  else if (defaultRequiredEmergencyKeySharesLessThenTwoError.value != null) return t('admin.emergencyAccess.keySplitting.errors.minValue');
  else if ( defaultRequiredEmergencyKeySharesLowerThenMinMembersError.value != null) return t('admin.emergencyAccess.keySplitting.errors.lowerAsMinMembersOrEqual');
  return 'No text.';
});

const requiredMinMembersValidationText = computed(() => {
  if (defaultMinMembersToHighError.value != null) return t('admin.emergencyAccess.keySplitting.errors.maxValue');
  else if (defaultMinMembersLessThenTwoError.value != null) return t('admin.emergencyAccess.keySplitting.errors.minValue');
  return 'No text.';
});

const onSaveErrorRecovery = ref<Error | null>(null);

const defaultRequiredEmergencyKeySharesLowerThenMinMembersError = ref<Error | null>(null);
const defaultRequiredEmergencyKeySharesLessThenTwoError = ref<Error | null>(null);
const defaultRequiredEmergencyKeySharesToHighError = ref<Error | null>(null);
const defaultRequiredEmergencyKeySharesError = ref<Error | null>(null);

const defaultMinMembersLessThenTwoError = ref<Error | null>(null);
const defaultMinMembersToHighError = ref<Error | null>(null);

const selectedMembersError = ref<Error | null>(null);

watch([minMembers, requiredShares], ([m, r], [pm, pr]) => {
  if (m === pm && r === pr) return;
  isDescLoading.value = true;
  isKeySplittingInvalid.value = false;
  noRedundancy.value = false;

  defaultRequiredEmergencyKeySharesLessThenTwoError.value = null;
  defaultRequiredEmergencyKeySharesLowerThenMinMembersError.value = null;
  defaultRequiredEmergencyKeySharesToHighError.value = null;

  defaultMinMembersLessThenTwoError.value = null;
  defaultMinMembersToHighError.value = null;

  if (r! > m! || r! >= 255 || m! >= 255 || r! < 2 || m! < 2 ){
    isKeySplittingInvalid.value = true;
  }
  if (r! == m!)
    noRedundancy.value = true;
  if (descTimer) window.clearTimeout(descTimer);
  descTimer = window.setTimeout(() => { isDescLoading.value = false; }, 350);
});

async function saveRecoverySettings() {
  defaultRequiredEmergencyKeySharesError.value = null;
  selectedMembersError.value = null;
  onSaveErrorRecovery.value = null;
  defaultRequiredEmergencyKeySharesLowerThenMinMembersError.value = null;
  defaultRequiredEmergencyKeySharesLessThenTwoError.value = null;
  defaultRequiredEmergencyKeySharesToHighError.value = null;
  defaultMinMembersLessThenTwoError.value = null;
  defaultMinMembersToHighError.value = null;

  if (requiredShares.value == null || minMembers.value == null) {
    onSaveErrorRecovery.value = new Error('Missing input');
    return;
  }

  if (requiredShares.value < 2) {
    defaultRequiredEmergencyKeySharesLessThenTwoError.value = new FormValidationFailedError();
    return;
  }
  if (requiredShares.value > 255) {
    defaultRequiredEmergencyKeySharesToHighError.value = new FormValidationFailedError();
    return;
  }

  if (minMembers.value < 2) {
    defaultMinMembersLessThenTwoError.value = new FormValidationFailedError();
    return;
  }
  if (minMembers.value > 255) {
    defaultMinMembersToHighError.value = new FormValidationFailedError();
    return;
  }

  if (requiredShares.value > minMembers.value) {
    defaultRequiredEmergencyKeySharesLowerThenMinMembersError.value = new FormValidationFailedError();
    onSaveErrorRecovery.value = new Error(
      t('admin.emergencyAccess.errors.sharesMustNotExceedMembers') ?? 'Required > members'
    );
    return;
  }
  if (selectedUsers.value.length < minMembers.value) {
    selectedMembersError.value = new FormValidationFailedError();
    onSaveErrorRecovery.value = new Error(
      t('admin.emergencyAccess.councilMembers.errors.notEnoughMembers', [minMembers.value]) ?? 'Not enough selected members'
    );
    return;
  }

  try {
    processing.value = true;
    await backend.settings.update({
      defaultRequiredEmergencyKeyShares: requiredShares.value,
      defaultMinMembers: minMembers.value,
      allowChoosingEmergencyCouncil: allowChoosing.value,
      emergencyCouncilMemberIds: selectedUsers.value.map(u => u.id),
    });
    initialEmergencyAccessSettings.value = {
      defaultRequiredEmergencyKeyShares: requiredShares.value,
      defaultMinMembers: minMembers.value,
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
  addedCouncilMembers.value = [];
  defaultRequiredEmergencyKeySharesError.value = null;
  selectedMembersError.value = null;
  onSaveErrorRecovery.value = null;
}

watch(() => [selectedUsers.value.map(u => u.id).join(','), minMembers.value, requiredShares.value],
  () => { 
    selectedMembersError.value = null; 
    defaultRequiredEmergencyKeySharesError.value = null; 
    defaultRequiredEmergencyKeySharesLowerThenMinMembersError.value = null;
  });
onMounted(async () => {
  await fetchEmergencyAccess();
});
</script>
