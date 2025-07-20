<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('close')">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto" @close="open = false">
      <TransitionChild
        as="template"
        enter="ease-out duration-300"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="ease-in duration-200"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <DialogOverlay class="fixed inset-0 bg-gray-500/75 transition-opacity" />
      </TransitionChild>

      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild
            as="template"
            enter="ease-out duration-300"
            enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
            enter-to="opacity-100 translate-y-0 sm:scale-100"
            leave="ease-in duration-200"
            leave-from="opacity-100 translate-y-0 sm:scale-100"
            leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
          >
            <DialogPanel
              class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg"
            >
              <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div class="sm:flex sm:items-start">
                  <div
                    class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-red-100 sm:mx-0 sm:h-10 sm:w-10"
                  >
                    <ExclamationTriangleIcon class="h-6 w-6 text-red-600" aria-hidden="true" />
                  </div>
                  <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                    <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                      {{ t('grantEmergencyAccessDialog.title') }}
                    </DialogTitle>
                    <div class="mt-2">
                      <p class="text-sm text-gray-500">
                        {{ t('grantEmergencyAccessDialog.description') }}
                      </p>
                    </div>
                    <div class="relative">
                      <div class="sm:grid sm:grid-cols-2 sm:items-center sm:gap-2 pt-2 pb-2">
                        <label for="coundcilMembers" class="text-sm font-medium text-gray-700 flex items-center">
                          {{ t('admin.emergencyAccess.councilMembers.title') }}
                        </label>
                      </div>
                      <MultiUserSelectInputGroup
                        :selected-users="emergencyCouncilMembers"
                        :on-search="searchCouncilMembers"
                        :input-visible="allowChangingDefaults"
                        @action="addCouncilMember"
                        @remove="removeCouncilMember"
                      />
                    </div>
                    <RequiredKeySharesInput
                      v-model="requiredKeyShares"
                      :allow-changing-defaults="allowChangingDefaults"
                      :default-key-shares="defaultRequiredEmergencyKeyShares"
                    />

                    <EmergencyScenarioVisualization
                      :loading="loadingCouncilSelection"
                      :grant-button-disabled="isGrantButtonDisabled"
                      :required-key-shares="requiredKeyShares"
                      :random-council-selection="randomCouncilSelection"
                    />
                  </div>
                </div>

                <p v-if="onAddCouncilMemberError" class="mt-2 text-right text-sm text-red-600">
                  {{ onAddCouncilMemberError.message }}
                </p>
              </div>

              <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                <!-- Grant-Button -->
                <button
                  type="button"
                  class="w-full inline-flex justify-center rounded-md border border-transparent shadow-xs px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                  :disabled="isGrantButtonDisabled"
                  @click="splitRecoveryKey()"
                >
                  {{ t('common.grant') }}
                </button>
                <!-- Close-Button -->
                <button
                  type="button"
                  class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-xs px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:w-auto sm:text-sm"
                  @click="closeDialog()"
                >
                  {{ t('common.close') }}
                </button>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/24/outline';
import { ref, watch, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { TrustDto, UserDto, VaultDto, didCompleteSetup, ActivatedUser } from '../../common/backend';
import { VaultKeys } from '../../common/crypto';
import { wordEncoder } from '../../common/util';
import { EmergencyAccess } from '../../common/emergencyaccess';
import MultiUserSelectInputGroup from '../MultiUserSelectInputGroup.vue';
import RequiredKeySharesInput from './RequiredKeySharesInput.vue';
import EmergencyScenarioVisualization from './EmergencyScenarioVisualization.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vault: VaultDto,
  vaultKeys: VaultKeys
}>();

const emit = defineEmits<{
  close: []
  updated: [updatedVault: VaultDto]
}>();

defineExpose({
  show,
});

const open = ref(false);
const trusts = ref<TrustDto[]>([]);

const defaultEmergencyCouncilMembers = ref<ActivatedUser[]>([]);
const defaultRequiredEmergencyKeyShares = ref<number>(0);
const allowChangingDefaults = ref<boolean>(false);

const addingCouncilMember = ref(false);
const onAddCouncilMemberError = ref<Error | null>();

const userQuery = ref('');
const searchResults = ref<UserDto[]>([]);

const requiredKeyShares = ref<number>(0);

const initialEmergencyCouncilMembers = ref<ActivatedUser[]>([]);
const addedEmergencyCouncilMembers = ref<ActivatedUser[]>([]);

const emergencyCouncilMembers = computed(() =>
  [...initialEmergencyCouncilMembers.value, ...addedEmergencyCouncilMembers.value]
);

const loadingCouncilSelection = ref(false);
const randomCouncilSelection = ref<UserDto[]>([]);
const randomSelectionInterval = ref<ReturnType<typeof setInterval> | null>(null);

let timeoutId: ReturnType<typeof setTimeout> | null = null;

const isInvalidKeyShares = computed(() => {
  return requiredKeyShares.value < 1;
});

const isInvaildCouncilMembers = computed(() => {
  return emergencyCouncilMembers.value.length < 1;
});

const hasTooFewCouncilMembers = computed(() => {
  return emergencyCouncilMembers.value.length < requiredKeyShares.value;
});

const isEmergencyCouncilMembersRequiredKeySharesNotEqual = computed(() =>
  emergencyCouncilMembers.value.length !== requiredKeyShares.value
);

const isGrantButtonDisabled = computed(() => {
  return isInvalidKeyShares.value || isInvaildCouncilMembers.value || hasTooFewCouncilMembers.value;
});

watch(userQuery, async (newQuery) => {
  const trimmedQuery = newQuery.trim();
  if (trimmedQuery.length > 0) {
    searchResults.value = await searchCouncilMembers(trimmedQuery);
  } else {
    searchResults.value = [];
  }
});

watch(isEmergencyCouncilMembersRequiredKeySharesNotEqual, (isNotEqual) => {
  if (isNotEqual) {
    startRandomCouncilInterval();
  } else {
    stopRandomCouncilInterval();
  }
});

watch(
  [emergencyCouncilMembers, requiredKeyShares],
  () => {
    loadingCouncilSelection.value = true;

    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }

    timeoutId = setTimeout(() => {
      pickRandomCouncilMembers();
      loadingCouncilSelection.value = false;
      timeoutId = null;
    }, 300);
  },
  { immediate: true }
);

async function show() {
  open.value = true;
  await loadDefaultSettings();
  requiredKeyShares.value = defaultRequiredEmergencyKeyShares.value;
  initialEmergencyCouncilMembers.value = [...defaultEmergencyCouncilMembers.value];
  addedEmergencyCouncilMembers.value = [];
  await refreshTrusts();

  pickRandomCouncilMembers();
}

function closeDialog() {
  open.value = false;
  if (randomSelectionInterval.value) {
    clearInterval(randomSelectionInterval.value);
    randomSelectionInterval.value = null;
  }
}

async function searchCouncilMembers(query: string): Promise<ActivatedUser[]> {
  const existingIds = new Set(emergencyCouncilMembers.value.map(m => m.id));
  const authorities = await backend.authorities.search(query, true);
  return authorities
    .filter(a => a.type === 'USER')
    .filter(a => didCompleteSetup(a)) // only include users with a public key
    .filter(a => !existingIds.has(a.id))
    .sort((a, b) => a.name.localeCompare(b.name));
}

async function addCouncilMember(authority: ActivatedUser) {
  onAddCouncilMemberError.value = null;
  try {
    const alreadyExists =
      initialEmergencyCouncilMembers.value.some(u => u.id === authority.id) ||
      addedEmergencyCouncilMembers.value.some(u => u.id === authority.id);

    if (!alreadyExists) {
      addedEmergencyCouncilMembers.value = [...addedEmergencyCouncilMembers.value, authority];
    }   
    addingCouncilMember.value = false;
  } catch (error) {
    console.error('Adding council member failed.', error);
    onAddCouncilMemberError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function removeCouncilMember(user: ActivatedUser) {
  initialEmergencyCouncilMembers.value = initialEmergencyCouncilMembers.value.filter(u => u.id !== user.id);
  addedEmergencyCouncilMembers.value = addedEmergencyCouncilMembers.value.filter(u => u.id !== user.id);
}

function resetCouncilMembers() {
  initialEmergencyCouncilMembers.value = [];
  addedEmergencyCouncilMembers.value = [];
}

async function splitRecoveryKey() {
  try {
    onAddCouncilMemberError.value = null;

    if (requiredKeyShares.value == null || requiredKeyShares.value < 1) {
      throw new Error(t('grantEmergencyAccessDialog.error.invalidKeyShares'));
    }

    if (emergencyCouncilMembers.value.length < 1) {
      throw new Error(t('grantEmergencyAccessDialog.error.invalidCouncilMemberLengt'));
    }

    if (emergencyCouncilMembers.value.length < requiredKeyShares.value) {
      throw new Error(
        t('grantEmergencyAccessDialog.error.notEnoughCouncilMembers', {
          required: requiredKeyShares.value,
          actual: emergencyCouncilMembers.value.length,
        })
      );
    }

    if (emergencyCouncilMembers.value.length < requiredKeyShares.value) {
      throw new Error(
        t('grantEmergencyAccessDialog.error.notEnoughCouncilMembers', {
          required: requiredKeyShares.value,
          actual: emergencyCouncilMembers.value.length,
        })
      );
    }

    const recoveryKey = await props.vaultKeys.createRecoveryKey();
    const recoveryKeyBytes = wordEncoder.decode(recoveryKey); // TODO: skip word encode/decode, once merged with UVF branch
    const keyShares = await EmergencyAccess.split(recoveryKeyBytes, requiredKeyShares.value, ...emergencyCouncilMembers.value);

    const updatedVault = await backend.vaults.createOrUpdateVault(
      props.vault.id,
      props.vault.name,
      props.vault.archived,
      requiredKeyShares.value,
      keyShares,
      props.vault.description
    );

    emit('updated', updatedVault);
    open.value = false;
  } catch (error) {
    console.error('Granting emergency access failed.', error);
    onAddCouncilMemberError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function loadDefaultSettings() {
  try {
    const settings = await backend.settings.get();
    defaultEmergencyCouncilMembers.value = (await backend.authorities.listSome(settings.emergencyCouncilMemberIds))
      .filter(a => a.type === 'USER')
      .filter(a => didCompleteSetup(a)); // only include users with a public key
    
    const authorities = await backend.authorities.listSome(settings.emergencyCouncilMemberIds);
    const sortedActivatedUsers = authorities
      .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
      .sort((a, b) => a.name.localeCompare(b.name));

    defaultEmergencyCouncilMembers.value = [...sortedActivatedUsers];
    initialEmergencyCouncilMembers.value = [...sortedActivatedUsers];

    allowChangingDefaults.value = settings.allowChoosingEmergencyCouncil;
    defaultRequiredEmergencyKeyShares.value = settings.defaultRequiredEmergencyKeyShares;
  } catch (error) {
    console.error('Loading emergency council members failed:', error);
    // TODO: don't set defaults, hard-fail with error message instead
    resetCouncilMembers();
    defaultRequiredEmergencyKeyShares.value = 0;
    allowChangingDefaults.value = false;
  }
}

function pickRandomCouncilMembers() {
  const available = emergencyCouncilMembers.value;
  const required = requiredKeyShares.value ?? 2;

  if (available.length < required) {
    randomCouncilSelection.value = [];
    return;
  }

  if (randomCouncilSelection.value.length !== required) {
    const shuffled = [...available].sort(() => 0.5 - Math.random());
    randomCouncilSelection.value = shuffled.slice(0, required);
    return;
  }

  const maxPills = (requiredKeyShares.value < 3) ? requiredKeyShares.value : 3 ; 
  const current = randomCouncilSelection.value;
  const currentIds = new Set(current.map(u => u.id));

  const candidates = available.filter(u => !currentIds.has(u.id));
  if (candidates.length === 0) return;

  const newUser = candidates[Math.floor(Math.random() * candidates.length)];
  const replaceIndex = Math.floor(Math.random() * maxPills);

  randomCouncilSelection.value = [
    ...current.slice(0, replaceIndex),
    newUser,
    ...current.slice(replaceIndex + 1)
  ];
}

function startRandomCouncilInterval() {
  stopRandomCouncilInterval();
  randomSelectionInterval.value = setInterval(() => {
    pickRandomCouncilMembers();
  }, 2000);
}

function stopRandomCouncilInterval() {
  if (randomSelectionInterval.value) {
    clearInterval(randomSelectionInterval.value);
    randomSelectionInterval.value = null;
  }
}

async function refreshTrusts() {
  trusts.value = await backend.trust.listTrusted();
}
</script>