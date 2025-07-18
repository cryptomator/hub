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
                      <p v-if="didAddMyShare" class="text-sm text-gray-500">
                        TODO LOCALIZE: You have already added your part of the emergency key.
                      </p>
                      <p class="text-sm text-gray-500">
                        {{ phaseDescription }}
                      </p>
                    </div>
                    <select v-if="phase === 'start'" v-model="selectedType">
                      <option value="RECOVERY">Ownership</option>
                      <option value="COUNCIL_CHANGE">Vote New Council Members</option>
                    </select> 
                    <div v-else>
                      <div v-if="selectedType === 'RECOVERY'">
                        Ownership
                      </div>
                      <div v-if="selectedType === 'COUNCIL_CHANGE'">
                        Vote New Council Members
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                <template v-if="phase === 'start'">
                  <button
                    type="button"
                    class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm"
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
                  <p v-if="onError != null" class="text-sm text-red-900 px-4 sm:px-6 text-right bg-red-50">
                    {{ t('common.unexpectedError', [onError.message]) }}
                  </p>
                  <button
                    type="button"
                    class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm"
                    @click="completeRecovery()"
                  >
                    Complete
                  </button>
                </template>

                <button
                  type="button"
                  class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:w-auto sm:text-sm"
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
import backend, { VaultDto, UserDto, RecoveryProcessDto, didCompleteSetup } from '../common/backend';
import { ref, computed, toRaw } from 'vue';
import { useI18n } from 'vue-i18n';
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { describeSegment } from '../common/svgUtils';
import { EmergencyAccess } from '../common/emergencyaccess';
import userdata from '../common/userdata';

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

type PhaseType = 'start' | 'approve' | 'complete';
const selectedType = ref<RecoveryProcessDto['type']>(props.recoveryProcess?.type ?? 'RECOVERY');

const selectedNewCouncilMembers = ref<string[]>([]); // TODO store type-specific process details in `details` field of `RecoveryProcessDto`
const phase = computed<PhaseType>(() => {
  const p = props.recoveryProcess;
  if (!p) {
    return 'start';
  } else if (completedSegments.value + 1 < p.requiredKeyShares) {
    return 'approve';
  } else {
    return 'complete';
  }
});
const requiredSegments = computed(() => props.recoveryProcess?.requiredKeyShares ?? props.vault.requiredEmergencyKeyShares );
const completedSegments = computed(() => Object.values(props.recoveryProcess?.recoveredKeyShares ?? {}).filter(ks => ks.recoveredKeyShare !== undefined).length );
const didAddMyShare = computed(() => props.recoveryProcess?.recoveredKeyShares[props.me.id].recoveredKeyShare !== undefined);

const open = ref(false);
const onError = ref<Error | null>();

async function show() {
  open.value = true;
}

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

    // create recovery process:
    const processKeyPair = await EmergencyAccess.startRecovery(councilMembers);
    const process: RecoveryProcessDto = {
      id: crypto.randomUUID(),
      vaultId: props.vault.id,
      type: selectedType.value,
      details: '', // TODO: used to store type-specific details such as `selectedNewCouncilMembers`
      requiredKeyShares: props.vault.requiredEmergencyKeyShares,
      processPublicKey: processKeyPair.recoveryPublicKey,
      recoveredKeyShares: {}
    };
    for (const [memberId, jwe] of Object.entries(processKeyPair.recoveryPrivateKeys)) {
      process.recoveredKeyShares[memberId] = {
        processPrivateKey: jwe,
      };
    }

    // get my user private key:
    const userKeys = await userdata.decryptUserKeysWithBrowser();

    // add my part of the emergency key::
    await addMyShare(process, userKeys.ecdhKeyPair.privateKey);
    await backend.emergencyAccess.startRecovery(process);

    emit('updated', process);
    open.value = false;
  } catch (error) {
    console.error('Starting emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

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

    emit('updated', process);
    open.value = false;
  } catch (error) {
    console.error('Approving emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

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
    const recoveredKey = await EmergencyAccess.combineRecoveredShares(keyShares, processPrivateKey, userKeys.ecdhKeyPair.privateKey);

    // TODO: reset vault key...
    console.debug('Recovered key:', recoveredKey);

    // delete process:
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

function closeDialog() {
  open.value = false;
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
</script>
