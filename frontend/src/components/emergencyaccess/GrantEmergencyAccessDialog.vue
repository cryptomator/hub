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
                        <label for="coundcilMembers" class="block text-sm font-medium text-gray-700 flex items-center">
                          {{ t('admin.emergencyAccess.councilMembers.title') }}
                          <button 
                            type="button" 
                            class="ml-2 p-1 flex items-center justify-center focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-30 disabled:cursor-not-allowed"
                            :disabled="emergencyCouncilMembers.length == 0"
                            :title="emergencyCouncilMembers.length > 0 ? 'Reset' : ''"
                            @click="emergencyCouncilMembers = []"
                          >
                            <TrashIcon v-if="allowChangingDefaults" class="h-4 w-4 text-gray-500 hover:text-gray-700 disabled:text-gray-300" aria-hidden="true" />
                          </button>
                        </label>
                      </div>

                      <!-- Suchfeld -->
                      <SearchInputGroup
                        v-if="allowChangingDefaults"
                        :action-title="t('common.add')"
                        :on-search="searchCouncilMembers"
                        @action="addCouncilMember"
                      />

                      <!-- Ausgewählte Nutzer als Chips -->
                      <div class="mt-2 min-h-[66px]">
                        <div class="flex flex-wrap gap-2">
                          <span
                            v-for="user in emergencyCouncilMembers"
                            :key="user.id"
                            class="inline-flex items-center border border-primary bg-white text-gray-800 text-sm font-medium px-3 py-1 rounded-full shadow-sm"
                          >
                            <img :src="user.pictureUrl" class="w-4 h-4 rounded-full mr-2" />
                            {{ user.name }}
                            <TrustDetails v-if="user.type === 'USER'" :trusted-user="user" :trusts="trusts" @trust-changed="refreshTrusts()"/>

                            <button
                              v-if="user.type === 'USER' && allowChangingDefaults"
                              type="button"
                              class="ml-2 text-gray-400 hover:text-red-500"
                              :aria-label="t('grantEmergencyAccessDialog.removeUser', { name: user.name })"
                              @click="removeCouncilMember(user)"
                            >
                              &times;
                            </button>
                          </span>
                        </div>
                      </div>
                    </div>
                    <div class="mt-4">
                      <template v-if="allowChangingDefaults">
                        <label for="keyshares" class="block text-sm font-medium text-gray-700">
                          Required Emergeny Key Shares
                        </label>
                        <div class="mt-1">
                          <input
                            id="keyshares"
                            v-model.number="requiredKeyShares"
                            type="number"
                            min="1"
                            class="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary sm:text-sm"
                          />
                        </div>
                      </template>
                      <template v-else>
                        <div class="text-sm text-gray-500">
                          {{ t('vaultDetails.actions.requiredEmergencyKeyShares') }}:
                          <span class="font-medium text-gray-900">{{ defaultRequiredEmergencyKeyShares }}</span>
                        </div>
                      </template>
                    </div>
                    <div class="mt-4">
                      <label class="block text-sm font-medium text-gray-700 pb-2" >
                        {{ t('grantEmergencyAccessDialog.possibleEmergencyScenario') }}
                      </label>
                      <template v-if="emergencyCouncilMembers.length != requiredKeyShares || randomCouncilSelection.length > 3">
                        <Transition
                          mode="out-in"
                          enter-active-class="transition duration-0 ease-out"
                          leave-active-class="transition duration-500 ease-in"
                          enter-from-class="opacity-0 transform translate-y-2 scale-95"
                          enter-to-class="opacity-100 transform translate-y-0 scale-100"
                          leave-from-class="opacity-100 transform translate-y-0 scale-100"
                          leave-to-class="opacity-0 transform -translate-y-2 scale-95"
                        >
                          <div
                            v-if="randomCouncilSelection.length > 0"
                            :key="randomCouncilSelection.map(u => u.id).join('-')"
                            class="flex flex-wrap gap-2"
                          >
                            <template v-for="(user, index) in randomCouncilSelection.slice(0,3)" :key="user.id">
                              <span
                                class="inline-flex items-center border border-indigo-300 bg-indigo-50 text-indigo-800 text-sm font-medium px-2 py-1 rounded-full shadow-sm max-w-[80px]"
                                :style="{ maxWidth: randomCouncilSelection.length <= 3 ? '120px' : '102px' }"
                              >
                                <img :src="user.pictureUrl" class="w-4 h-4 rounded-full mr-1 shrink-0" />
                                <span class="truncate">
                                  {{ user.name }}
                                </span>
                              </span>
                              <span
                                v-if="index < randomCouncilSelection.slice(0,3).length - 1 || (index === randomCouncilSelection.slice(0,3).length - 1 && randomCouncilSelection.length > 3)"
                                class="inline-flex items-center justify-center text-gray-500 font-medium"
                                style="line-height: 1; margin: 0 -5px;"
                              >
                                +
                              </span>
                            </template>

                            <span
                              v-if="randomCouncilSelection.length > 3"
                              class="inline-flex items-center border border-gray-300 bg-gray-100 text-gray-700 text-sm font-medium px-3 py-1 rounded-full shadow-sm"
                            >
                              +{{ randomCouncilSelection.length - 3 }}
                            </span>
                          </div>
                        </Transition>
                      </template>
                      <template v-else>
                        <div
                          v-if="randomCouncilSelection.length > 0"
                          class="flex flex-wrap gap-2"
                        >
                          <div
                            v-if="randomCouncilSelection.length > 0"
                            :key="randomCouncilSelection.map(u => u.id).join('-')"
                            class="flex flex-wrap gap-2"
                          >
                            <template v-for="(user, index) in emergencyCouncilMembers" :key="user.id">
                              <span
                                class="inline-flex items-center border border-indigo-300 bg-indigo-50 text-indigo-800 text-sm font-medium px-2 py-1 rounded-full shadow-sm max-w-[80px]"
                                :style="{ maxWidth: randomCouncilSelection.length <= 3 ? '120px' : '102px' }"
                              >
                                <img :src="user.pictureUrl" class="w-4 h-4 rounded-full mr-1 shrink-0" />
                                <span class="truncate">
                                  {{ user.name }}
                                </span>
                              </span>
                              <span
                                v-if="index < randomCouncilSelection.slice(0,3).length - 1 || (index === randomCouncilSelection.slice(0,3).length - 1 && randomCouncilSelection.length > 3)"
                                class="inline-flex items-center justify-center text-gray-500 font-medium"
                                style="line-height: 1; margin: 0 -5px;"
                              >
                                +
                              </span>
                            </template>

                            <span
                              v-if="randomCouncilSelection.length > 3 && emergencyCouncilMembers.length != requiredKeyShares"
                              class="inline-flex items-center border border-gray-300 bg-gray-100 text-gray-700 text-sm font-medium px-3 py-1 rounded-full shadow-sm"
                            >
                              +{{ randomCouncilSelection.length - 3 }}
                            </span>
                          </div>
                        </div>
                      </template>
                    </div>
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
                  class="w-full inline-flex justify-center rounded-md border border-transparent shadow-xs px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm"
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
import { ExclamationTriangleIcon, TrashIcon, PlusIcon } from '@heroicons/vue/24/outline';
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { TrustDto, MemberDto, UserDto, AuthorityDto, VaultDto, didCompleteSetup, ActivatedUser } from '../../common/backend';
import TrustDetails from '../TrustDetails.vue';
import SearchInputGroup from '../SearchInputGroup.vue';
import { VaultKeys } from '../../common/crypto';
import { wordEncoder } from '../../common/util';
import { EmergencyAccess } from '../../common/emergencyaccess';

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
const emergencyCouncilMembers = ref<ActivatedUser[]>([]);
const randomCouncilSelection = ref<UserDto[]>([]);

watch(userQuery, async (newQuery) => {
  const trimmedQuery = newQuery.trim();
  if (trimmedQuery.length > 0) {
    searchResults.value = await searchCouncilMembers(trimmedQuery);
  } else {
    searchResults.value = [];
  }
});

watch(
  [emergencyCouncilMembers, requiredKeyShares],
  () => {
    pickRandomCouncilMembers();
  },
  { immediate: true }
);

let randomSelectionInterval: ReturnType<typeof setInterval> | null = null;

async function show() {
  open.value = true;
  await loadDefaultSettings();
  requiredKeyShares.value = defaultRequiredEmergencyKeyShares.value;
  emergencyCouncilMembers.value = [...defaultEmergencyCouncilMembers.value];
  await refreshTrusts();

  pickRandomCouncilMembers();

  if (randomSelectionInterval) clearInterval(randomSelectionInterval);
  randomSelectionInterval = setInterval(() => {
    pickRandomCouncilMembers();
  }, 2000);
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

function removeCouncilMember(user: UserDto) {
  emergencyCouncilMembers.value = emergencyCouncilMembers.value.filter(u => u.id !== user.id);
}

async function addCouncilMember(authority: ActivatedUser) {
  onAddCouncilMemberError.value = null;
  try {
    emergencyCouncilMembers.value = [...emergencyCouncilMembers.value, authority];
    addingCouncilMember.value = false;
  } catch (error) {
    console.error('Adding council member failed.', error);
    onAddCouncilMemberError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
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

function closeDialog() {
  open.value = false;
  if (randomSelectionInterval) {
    clearInterval(randomSelectionInterval);
    randomSelectionInterval = null;
  }
}

async function loadDefaultSettings() {
  try {
    const settings = await backend.settings.get();
    defaultEmergencyCouncilMembers.value = (await backend.authorities.listSome(settings.emergencyCouncilMemberIds))
      .filter(a => a.type === 'USER')
      .filter(a => didCompleteSetup(a)); // only include users with a public key
    allowChangingDefaults.value = settings.allowChoosingEmergencyCouncil;
    defaultRequiredEmergencyKeyShares.value = settings.defaultRequiredEmergencyKeyShares;
  } catch (error) {
    console.error('Loading emergency council members failed:', error);
    // TODO: don't set defaults, hard-fail with error message instead
    defaultEmergencyCouncilMembers.value = [];
    defaultRequiredEmergencyKeyShares.value = 0;
    allowChangingDefaults.value = false;
  }
}

function pickRandomCouncilMembers() {
  const available = emergencyCouncilMembers.value;
  const required = requiredKeyShares.value ?? 2;

  if (available.length >= required) {
    const shuffled = [...available].sort(() => 0.5 - Math.random());
    randomCouncilSelection.value = shuffled.slice(0, required);
  } else {
    randomCouncilSelection.value = [];
  }
}

async function refreshTrusts() {
  trusts.value = await backend.trust.listTrusted();
}
</script>
