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
                          v-for="i in props.totalSegments"
                          :key="i"
                          :d="describeSegment(i - 1, props.totalSegments, 16)"
                          :fill="i <= phaseSegmentCount ? '#49b04a' : '#e5e7eb'"
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
                    </div>
                    <select v-if="phase === 'start'" v-model="selectedType">
                      <option value="ownership">Ownership</option>
                      <option value="voteNewCouncilMembers">Vote New Council Members</option>
                    </select> 
                    <div v-else-if="phase === 'approve'">
                      <div v-if="selectedType === 'ownership'">
                        Ownership
                      </div>
                      <div v-if="selectedType === 'voteNewCouncilMembers'">
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

                <template v-else-if="phase === 'approve'">
                  <button
                    type="button"
                    class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm"
                    @click="approveRecovery()"
                  >
                    {{ t('common.approve') }}
                  </button>
                </template>

                <template v-else-if="phase === 'complete'">                
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
import backend, { VaultDto, UserDto } from '../common/backend';
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { EmergencyKeyShare, EmergencyKeyShareData } from '../common/EmergencyKeyShare';
import { describeSegment } from '../common/svgUtils';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vault: VaultDto;
  me: UserDto;
  totalSegments: number;
  approvedSegments: number;
}>();

const emit = defineEmits<{
  close: [];
  updated: [updatedVault: VaultDto];
}>();

defineExpose({
  show,
});

type PhaseType = 'start' | 'approve' | 'complete';
const selectedType = ref<'ownership' | 'voteNewCouncilMembers'>('ownership');

const selectedNewCouncilMembers = ref<string[]>([]);
const phase = ref<PhaseType>('start');
const phaseSegmentCount = computed(() => {
  return Math.min(props.approvedSegments, props.totalSegments);
});

const open = ref(false);
const onError = ref<Error | null>();

async function show(initialPhase?: PhaseType) {
  if (initialPhase) {
    phase.value = initialPhase;
  } else {
    if (props.approvedSegments === 0) {
      phase.value = 'start';
    } else if (props.approvedSegments + 1 === props.totalSegments) {
      phase.value = 'complete';
    } else {
      phase.value = 'approve';
    }
  }

  if (phase.value === 'approve' || phase.value === 'complete') {
    const parsedEntry = Object.entries(props.vault.emergencyKeyShares).find(([_, raw]) => EmergencyKeyShare.parse(raw) != null);

    if (parsedEntry) {
      const [, raw] = parsedEntry;
      const parsed = EmergencyKeyShare.parse(raw)!;

      if (parsed.typ === 'ownership' || parsed.typ === 'voteNewCouncilMembers') {
        selectedType.value = parsed.typ;
        if (parsed.typ === 'voteNewCouncilMembers' && Array.isArray(parsed.newCouncilMembers)) {
          selectedNewCouncilMembers.value = parsed.newCouncilMembers;
        }
      }
    }
  }

  open.value = true;
}

async function startRecovery() {
  try {
    onError.value = null;

    let share: EmergencyKeyShareData;

    if (selectedType.value === 'ownership') {
      share = {
        typ: 'ownership',
        newowner: 'admin', //props.me.id,
        timestamp: new Date().toISOString()
      };
    } else if (selectedType.value === 'voteNewCouncilMembers') {
      selectedNewCouncilMembers.value = ['bela','rod','farin'];
      share = {
        typ: 'voteNewCouncilMembers',
        newCouncilMembers: selectedNewCouncilMembers.value,
        timestamp: new Date().toISOString()
      };
    } else {
      throw new Error(`Unsupported type: ${selectedType.value}`);
    }

    const updatedShares: Record<string, string> = {
      ...props.vault.emergencyKeyShares,
      [props.me.id]: EmergencyKeyShare.create(share)
    };

    const updatedVault = await backend.vaults.createOrUpdateVault(
      props.vault.id,
      props.vault.name,
      props.vault.archived,
      props.vault.requiredEmergencyKeyShares,
      updatedShares,
      props.vault.description
    );

    emit('updated', updatedVault);
    open.value = false;
  } catch (error) {
    console.error('Starting emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function approveRecovery() {
  try {
    onError.value = null;

    const parsedEntry = Object.entries(props.vault.emergencyKeyShares).find(([_, raw]) =>
      EmergencyKeyShare.parse(raw) != null
    );

    if (!parsedEntry) {
      throw new Error('No valid emergency access data found.');
    }

    const [, firstShareRaw] = parsedEntry;
    const parsed = EmergencyKeyShare.parse(firstShareRaw)!;

    const updatedShares: Record<string, string> = {
      ...props.vault.emergencyKeyShares,
      [props.me.id]: EmergencyKeyShare.create(parsed)
    };

    const updatedVault = await backend.vaults.createOrUpdateVault(
      props.vault.id,
      props.vault.name,
      props.vault.archived,
      props.vault.requiredEmergencyKeyShares,
      updatedShares,
      props.vault.description
    );

    emit('updated', updatedVault);
    open.value = false;
  } catch (error) {
    console.error('Approving emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function completeRecovery() {
  try {
    onError.value = null;

    const parsedEntry = Object.entries(props.vault.emergencyKeyShares).find(([_, raw]) => EmergencyKeyShare.parse(raw) != null);
    if (!parsedEntry) {
      throw new Error('No valid emergency access data found.');
    }

    const [, firstShareRaw] = parsedEntry;
    const parsed = EmergencyKeyShare.parse(firstShareRaw)!;

    const updatedShares: Record<string, string> = {
      ...props.vault.emergencyKeyShares,
      [props.me.id]: EmergencyKeyShare.create(parsed)
    };

    const updatedVault = await backend.vaults.createOrUpdateVault(
      props.vault.id,
      props.vault.name,
      props.vault.archived,
      props.vault.requiredEmergencyKeyShares,
      updatedShares,
      props.vault.description
    );

    emit('updated', updatedVault);
    open.value = false;
  } catch (error) {
    console.error('Completing emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
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
