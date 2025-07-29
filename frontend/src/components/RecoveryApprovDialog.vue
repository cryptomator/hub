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
              class="relative transform overflow-visible transition-all sm:my-8 sm:w-full sm:max-w-lg"
            >
              <div class="relative rounded-lg bg-white">
                <div class="relative z-10">
                  <div class="px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                    <div class="sm:flex sm:items-start">
                      <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 sm:mx-0 sm:h-10 sm:w-10 relative">
                        <svg width="36" height="36" viewBox="0 0 36 36">
                          <g>
                            <path
                              v-for="i in requiredSegments"
                              :key="i"
                              :d="describeSegment(i - 1, requiredSegments, 16)"
                              :fill="i <= completedSegments ? '#49b04a' : '#e5e7eb'"
                              stroke="white"
                              stroke-width="1"
                            />
                          </g>
                        </svg>
                      </div>
                      <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                        <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                          {{ phaseTitle }}
                        </DialogTitle>
                        <div class="mt-2">
                          <p class="text-sm text-gray-500">
                            {{ phaseDescription }}
                          </p>
                          <p v-if="didAddMyShare" class="text-sm text-gray-500">
                            {{ t('recoveryDialog.alreadyAddedKeyShare') }}
                          </p>
                        </div>
                        <div v-if="phase === 'start'" class="mt-4 space-y-4">
                          <div>
                            <label class="block text-sm font-medium text-gray-700">
                              {{ t('recoveryDialog.selectRecoveryType') }}
                            </label>
                            <select
                              v-model="processType"
                              class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary sm:text-sm"
                            >
                              <option value="RECOVERY">{{ t('recoveryDialog.ownership') }}</option>
                              <option value="COUNCIL_CHANGE">{{ t('recoveryDialog.voteCouncil') }}</option>
                            </select>
                          </div>

                          <div v-if="processType === 'RECOVERY'">
                            <label class="block text-sm font-medium text-gray-700">
                              {{ t('recoveryDialog.selectNewOwner') }}
                            </label>
                            <MultiUserSelectInputGroup
                              :selected-users="newOwners"
                              :on-search="searchUsers"
                              :input-visible="true"
                              @action="addNewOwner"
                              @remove="removeNewOwner"
                            />
                          </div>

                          <div v-if="processType === 'COUNCIL_CHANGE'">
                            <label class="block text-sm font-medium text-gray-700">
                              {{ t('recoveryDialog.selectNewCouncil') }}
                            </label>
                            <MultiUserSelectInputGroup
                              :selected-users="newCouncilMembers"
                              :on-search="searchUsers"
                              :input-visible="true"
                              @action="addCouncilMember"
                              @remove="removeCouncilMember"
                            />
                            <RequiredKeySharesInput
                              v-model="newRequiredKeyShares"
                              :allow-changing-defaults="true"
                              :default-key-shares="vault.requiredEmergencyKeyShares"
                            />
                            <label class="block text-sm font-medium text-gray-700 pt-4">
                              {{ t('grantEmergencyAccessDialog.possibleEmergencyScenario') }}
                            </label>
                            <EmergencyScenarioVisualization
                              :selected-users="newCouncilMembers"
                              :grant-button-disabled="isGrantButtonDisabled"
                              :required-key-shares="newRequiredKeyShares"
                            />
                          </div>
                        </div><!-- if="phase === start" -->

                        <div v-else-if="!recoveryProcess">
                          <!-- every other phase should have a non-null recovery process -->
                          Internal error: No recovery process available. <!-- no need to localize this. -->
                        </div>

                        <div v-else>
                          <div v-if="recoveryProcess.type === 'RECOVERY'">
                            Ownership
                          </div>
                          <div v-if="recoveryProcess.type === 'COUNCIL_CHANGE'">
                            Vote New Council Members
                          </div>

                          <div v-if="recoveryProcess.type === 'RECOVERY'" class="mt-4 space-y-1 text-sm text-gray-500">
                            <div>
                              <span class="font-medium text-gray-700">{{ t('recoveryDialog.selectedOwner') }}:</span>
                              <MultiUserSelectInputGroup
                                :selected-users="newOwners"
                                :on-search="noopSearch"
                                :input-visible="false"
                              />
                            </div>
                          </div>

                          <div v-if="recoveryProcess.type === 'COUNCIL_CHANGE'" class="mt-4 space-y-1 text-sm text-gray-500">
                            <div v-if="recoveryProcess.details.newCouncilMemberIds.length > 0">
                              <span class="font-medium text-gray-700">{{ t('recoveryDialog.selectedCouncil') }}:</span>
                              <MultiUserSelectInputGroup
                                :selected-users="newCouncilMembers"
                                :on-search="noopSearch"
                                :input-visible="false"
                              />
                            </div>
                            <div>
                              <span class="font-medium text-gray-700">{{ t('recoveryDialog.requiredKeyShares') }}:</span>
                              {{ recoveryProcess.details.newRequiredKeyShares }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                
                  <div v-if="onError != null" class="w-full sm:w-auto mb-2 text-right">
                    <p class="inline-block text-sm text-red-700 bg-red-100 rounded px-3 py-1">
                      {{ t('common.unexpectedError', [onError.message]) }}
                    </p>
                  </div>
                  <div class="bg-gray-50 rounded-b-lg px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                    <template v-if="phase === 'start'">
                      <button
                        type="button"
                        class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                        :disabled="!canStartRecovery"
                        @click="startRecovery()"
                      >
                        {{ t('common.start') }}
                      </button>
                    </template>

                    <template v-else-if="phase === 'approve' && !didAddMyShare">
                      <button
                        type="button"
                        class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm"
                        @click="approveRecovery()"
                      >
                        {{ t('common.approve') }}
                      </button>
                    </template>

                    <template v-else-if="phase === 'complete' && !didAddMyShare">
                      <button
                        type="button"
                        class="inline-flex w-full sm:w-auto justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:text-sm"
                        @click="completeRecovery()"
                      >
                        {{ t('common.complete') }}
                      </button>
                    </template>

                    <button
                      type="button"
                      class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:w-auto sm:text-sm"
                      @click="open = false" 
                    >
                      {{ t('common.close') }}
                    </button>
                  </div>
                </div>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import backend, { VaultDto, UserDto, RecoveryProcessDto, didCompleteSetup, RecoveryProcessSetNewOwner, RecoveryProcessChangeCouncil, ActivatedUser, AccessGrant } from '../common/backend';
import { ref, computed, toRaw, Ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { describeSegment } from '../common/svgUtils';
import { EmergencyAccess } from '../common/emergencyaccess';
import userdata from '../common/userdata';
import MultiUserSelectInputGroup from './MultiUserSelectInputGroup.vue';
import RequiredKeySharesInput from './emergencyaccess/RequiredKeySharesInput.vue';
import EmergencyScenarioVisualization from './emergencyaccess/EmergencyScenarioVisualization.vue';
import { VaultKeys } from '../common/crypto';
import { wordEncoder } from '../common/util';
import { base64 } from 'rfc4648';
const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vault: VaultDto;
  me: UserDto;
  recoveryProcess?: RecoveryProcessDto;
}>();

const emit = defineEmits<{
  close: [];
  updated: [recoveryProcess?: RecoveryProcessDto];
}>();

defineExpose({
  show,
});

const processType = ref<RecoveryProcessDto['type']>(props.recoveryProcess?.type ?? 'RECOVERY');

type PhaseType = 'start' | 'approve' | 'complete';
const phase = computed<PhaseType>(() => {
  const p = props.recoveryProcess;
  if (!p) {
    return 'start';
  } else if (completedSegments + 1 < p.requiredKeyShares) {
    return 'approve';
  } else {
    return 'complete';
  }
});
const requiredSegments = props.recoveryProcess?.requiredKeyShares ?? props.vault.requiredEmergencyKeyShares;
const completedSegments = Object.values(props.recoveryProcess?.recoveredKeyShares ?? {}).filter(ks => ks.recoveredKeyShare !== undefined).length;
const didAddMyShare = props.recoveryProcess?.recoveredKeyShares[props.me.id].recoveredKeyShare !== undefined;

const open = ref(false);
const onError = ref<Error | null>();

// NEW OWNER
const newOwners = ref<ActivatedUser[]>([]);
const addNewOwner = addUser.bind(newOwners);
const removeNewOwner = removeUser.bind(newOwners);

// COUNCIL CHANGE
const newRequiredKeyShares = ref<number>(props.vault.requiredEmergencyKeyShares);
const newCouncilMembers = ref<ActivatedUser[]>([]);
const addCouncilMember = addUser.bind(newCouncilMembers);
const removeCouncilMember = removeUser.bind(newCouncilMembers);

const isGrantButtonDisabled = computed(() => newCouncilMembers.value.length < newRequiredKeyShares.value);

const noopSearch = async () => [];
async function searchUsers(query: string): Promise<UserDto[]> {
  const authorities = await backend.authorities.search(query, true);
  return authorities
    .filter((a): a is UserDto => a.type === 'USER' && didCompleteSetup(a))
    .sort((a, b) => a.name.localeCompare(b.name));
}

function addUser(this: Ref<UserDto[]>, user: UserDto) {
  if (!this.value.find(u => u.id === user.id)) {
    this.value.push(user);
  }
}

function removeUser(this: Ref<UserDto[]>, user: UserDto) {
  this.value = this.value.filter(u => u.id !== user.id);
}

const canStartRecovery = computed(() => {
  if (processType.value === 'RECOVERY') {
    return newOwners.value.length > 0;
  } else if (processType.value === 'COUNCIL_CHANGE') {
    return newCouncilMembers.value.length >= newRequiredKeyShares.value && newRequiredKeyShares.value > 0;
  } else {
    return false;
  }
});

async function show() {
  if (props.recoveryProcess?.type === 'COUNCIL_CHANGE') {
    const authorities = await backend.authorities.listSome(props.recoveryProcess.details.newCouncilMemberIds);
    const users = authorities.filter(a => a.type === 'USER').filter(u => didCompleteSetup(u));
    newCouncilMembers.value = users;
  }
  else if (props.recoveryProcess?.type === 'RECOVERY') {
    const authorities = await backend.authorities.listSome(props.recoveryProcess.details.newOwnerIds);
    const users = authorities.filter(a => a.type === 'USER').filter(u => didCompleteSetup(u));
    newOwners.value = users;
  }

  open.value = true;
}

const phaseTitle = computed(() => {
  switch (phase.value) {
    case 'start': return t('recoveryDialog.startTitle');
    case 'approve': return t('recoveryDialog.approveTitle');
    case 'complete': return t('recoveryDialog.completeTitle');
    default: return '';
  }
});

const phaseDescription = computed(() => {
  switch (phase.value) {
    case 'start': return t('recoveryDialog.startDesc');
    case 'approve': return t('recoveryDialog.approveDesc');
    case 'complete': return t('recoveryDialog.completeDesc');
    default: return '';
  }
});

/**
 * PHASE ONE: Starting the recovery process and adding the first share.
 */
async function startRecovery() {
  try {
    onError.value = null;

    // load council members:
    const recoveryCouncilMemberIds = Object.keys(props.vault.emergencyKeyShares);
    const authorities = await backend.authorities.listSome(recoveryCouncilMemberIds);
    const councilMembers = authorities.filter(a => a.type == 'USER').filter(u => didCompleteSetup(u)); // we can basically assume this
    if (councilMembers.length < props.vault.requiredEmergencyKeyShares) {
      throw new Error(`Inconsistent data: Insufficient council members (${councilMembers.length}) to recovery this vault (${props.vault.requiredEmergencyKeyShares}).`);
    }

    // depending on the process type, we need different data:
    let data: RecoveryProcessSetNewOwner | RecoveryProcessChangeCouncil;
    if (processType.value === 'RECOVERY') {
      if (!newOwners.value) {
        throw new Error(t('recoveryDialog.error.noOwnerSelected'));
      }
      data = {
        type: 'RECOVERY',
        details: {
          newOwnerIds: newOwners.value.map(u => u.id)
        }
      };
    } else if (processType.value === 'COUNCIL_CHANGE') {
      if (newCouncilMembers.value.length < newRequiredKeyShares.value) {
        throw new Error(t('recoveryDialog.error.notEnoughCouncilMembers'));
      }
      data = {
        type: 'COUNCIL_CHANGE',
        details: {
          newCouncilMemberIds: newCouncilMembers.value.map(u => u.id),
          newRequiredKeyShares: newRequiredKeyShares.value
        }
      };
    } else {
      throw new Error(t('recoveryDialog.error.invalidRecoveryType'));
    }

    // create recovery process:
    const processKeyPair = await EmergencyAccess.startRecovery(councilMembers);
    const process: RecoveryProcessDto = {
      id: crypto.randomUUID(),
      vaultId: props.vault.id,
      ...data,
      requiredKeyShares: props.vault.requiredEmergencyKeyShares,
      processPublicKey: processKeyPair.recoveryPublicKey,
      recoveredKeyShares: {}
    };
    // initialize recovered key shares for all council members:
    for (const [memberId, jwe] of Object.entries(processKeyPair.recoveryPrivateKeys)) {
      process.recoveredKeyShares[memberId] = {
        processPrivateKey: jwe,
      };
    }

    // add my part of the emergency key:
    const userKeys = await userdata.decryptUserKeysWithBrowser();
    await addMyShare(process, userKeys.ecdhKeyPair.privateKey);

    // save and exit:
    await backend.emergencyAccess.startRecovery(process);
    emit('updated', process);
    open.value = false;
  } catch (error) {
    console.error('Starting emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

/**
 * PHASE TWO: Adding further shares to the recovery process.
 */
async function approveRecovery() {
  if (!props.recoveryProcess) {
    throw new Error('No recovery process to approve.');
  }
  try {
    onError.value = null;

    // get my user private key:
    const userKeys = await userdata.decryptUserKeysWithBrowser();

    // add my part of the emergency key:
    const process = structuredClone(toRaw(props.recoveryProcess));
    const myRecoveredShare = await addMyShare(process, userKeys.ecdhKeyPair.privateKey);
    await backend.emergencyAccess.addMyShare(process.id, myRecoveredShare);

    // done:
    emit('updated', process);
    open.value = false;
  } catch (error) {
    console.error('Approving emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

/**
 * PHASE THREE: By adding the last required share, we can complete the recovery process.
 */
async function completeRecovery() {
  if (!props.recoveryProcess) {
    throw new Error('No recovery process to complete.');
  }
  try {
    onError.value = null;

    // get my user private key:
    const userKeys = await userdata.decryptUserKeysWithBrowser();

    // add my part of the emergency key:
    const process = structuredClone(toRaw(props.recoveryProcess));
    await addMyShare(process, userKeys.ecdhKeyPair.privateKey);

    // collect key parts:
    const keyShares = Object.values(process.recoveredKeyShares).filter(p => p.recoveredKeyShare !== undefined).map(p => p.recoveredKeyShare!);

    // get process private key:
    const processPrivateKey = process.recoveredKeyShares[props.me.id].processPrivateKey;
    const recoveredKeyBytes = await EmergencyAccess.combineRecoveredShares(keyShares, processPrivateKey, userKeys.ecdhKeyPair.privateKey);
    const recoveredKey = wordEncoder.encodePadded(recoveredKeyBytes); // TODO remove word encoding crap:

    if (process.type === 'COUNCIL_CHANGE' && newCouncilMembers.value.length >= process.details.newRequiredKeyShares) {
      // split recovered key into new shares for new council members:
      const keyShares = await EmergencyAccess.split(recoveredKeyBytes, process.details.newRequiredKeyShares, ...newCouncilMembers.value);
      await backend.vaults.createOrUpdateVault(
        props.vault.id,
        props.vault.name,
        props.vault.archived,
        process.details.newRequiredKeyShares,
        keyShares,
        props.vault.description
      );
    } else if (process.type === 'RECOVERY' && newOwners.value.length > 0) {
      // grant access to new owners using recovered key:
      const vaultKeys = await VaultKeys.recover(recoveredKey);
      const accessGrants: AccessGrant[] = await Promise.all(newOwners.value.map(async u => {
        console.log(`Granting access to user ${u.email}.`);
        const publicKey = base64.parse(u.ecdhPublicKey);
        const jwe = vaultKeys.encryptForUser(publicKey);
        await backend.vaults.addUser(props.vault.id, u.id, 'OWNER');
        return { userId: u.id, token: await jwe };
      }));
      await backend.vaults.grantAccess(props.vault.id, ...accessGrants);
      // TODO: shall we remove other owners or is that up to the new owner?
    } else {
      throw new Error(`Unsupported state for recovery process type: ${process.type}`);
    }

    console.log(`Successfully completed recovery process ${process.id} for vault ${props.vault.id}.`);
    await backend.emergencyAccess.delete(process.id);
    emit('updated');
    open.value = false;
  } catch (error) {
    console.error('Completing emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function addMyShare(process: RecoveryProcessDto, userPrivateKey: CryptoKey): Promise<string> {
  const encryptedShare = props.vault.emergencyKeyShares[props.me.id];
  const recoveredShare = await EmergencyAccess.recoverShare(encryptedShare, userPrivateKey, process.processPublicKey);
  process.recoveredKeyShares[props.me.id].recoveredKeyShare = recoveredShare;
  return recoveredShare;
}
</script>
