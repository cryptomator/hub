<template>
  <TransitionRoot as="template" :show="open" @after-leave="handleAfterLeave">
    <Dialog as="div" class="fixed inset-0 z-10 overflow-y-auto" :class="wantAbort ? 'pointer-events-none' : ''" :aria-hidden="wantAbort ? 'true' : undefined" @close="handleParentClose">
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
            <div class="relative rounded-lg bg-white z-10">
              <div class="px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div class="sm:flex sm:items-start">
                  <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 sm:mx-0 sm:h-10 sm:w-10 relative">
                    <div v-if="phase !== 'start'">
                      <SegmentRing
                        :total="newRequiredKeyShares"
                        :completed="completedSegments"
                        :size="36"
                      />
                    </div>
                    <div v-else>
                      <PlayIcon class="h-8 w-8 text-primary" aria-hidden="true" />
                    </div>
                  </div>
                  <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                    <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                      {{ phaseTitle }}
                    </DialogTitle>

                    <div v-if="phase === 'start'" class="mt-4 space-y-4">
                      <div v-if="processType === 'CHANGE_PERMISSIONS'">
                        <label class="block text-sm font-medium text-gray-700">
                          Select user with role owner
                        </label>
                        <MultiUserSelectInputGroup
                          :selected-users="owners"
                          :on-search="searchUsers"
                          :input-visible="true"
                          placeholder=" "
                          @action="addOwner"
                          @remove="removeOwner"
                        />
                        <div v-if="!hasActivatedOwner" class="flex items-start gap-2 rounded-md border border-yellow-200 bg-yellow-50 p-1 text-sm text-gray-900 mt-1">
                          <span class="leading-5">
                            <span class="text-gray-600">
                              Select at least 1 activated user as owner.
                            </span>
                          </span>
                        </div>

                        <!-- Members (non-owners) selector -->
                        <div class="mt-4">
                          <label class="block text-sm font-medium text-gray-700">
                            Select user with role member
                          </label>
                          <MultiUserSelectInputGroup
                            :selected-users="members"
                            :on-search="searchUsers"
                            :input-visible="true"
                            @action="addMember"
                            @remove="removeMember"
                          />
                        </div>

                        <!-- Removed Members display -->
                        <div v-if="removedMembers.length > 0" class="mt-4">
                          <span class="block text-sm font-medium text-gray-700">
                            Removed
                          </span>
                          <MultiUserSelectInputGroup
                            :selected-users="removedMembers"
                            :on-search="noopSearch"
                            :input-visible="false"
                          />
                        </div>
                      </div>

                      <div v-else-if="processType === 'COUNCIL_CHANGE'">
                        <label class="block text-sm font-medium text-gray-700">
                          Council Members (At least: {{ newRequiredKeyShares }})
                        </label>
                        <MultiUserSelectInputGroup
                          :selected-users="newCouncilMembers"
                          :on-search="searchUsersWithCompleteSetup"
                          :input-visible="true"
                          @action="addCouncilMember"
                          @remove="removeCouncilMember"
                        />
                        <div v-if="newRequiredKeyShares - newCouncilMembers.length > 0" class="flex items-start gap-2 rounded-md border border-yellow-200 bg-yellow-50 p-1 text-sm text-gray-900 mt-1">
                          <span class="leading-5">
                            <span class="text-gray-600">
                              Select at least {{ newRequiredKeyShares - newCouncilMembers.length }} more council member.
                            </span>
                          </span>
                        </div>
                        <label class="block text-sm font-medium text-gray-700 pt-4">
                          Possible Emergency Scenario
                        </label>
                        <EmergencyScenarioVisualization
                          :selected-users="newCouncilMembers"
                          :grant-button-disabled="isGrantButtonDisabled"
                          :required-key-shares="newRequiredKeyShares"
                          :min-members="newRequiredKeyShares"
                        />
                        <div v-if="needsRedundancy()" class="mt-4 mr-3">
                          <span class="inline-flex items-center gap-2 rounded-full bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800">
                            <ExclamationTriangleIcon class="h-4 w-4" aria-hidden="true" />
                            No Redundancy
                          </span>
                        </div>
                      </div>
                      <div v-else class="text-sm text-red-600">
                        {{ t('recoveryDialog.error.invalidRecoveryType') }}
                      </div>
                    </div>

                    <div v-else-if="!recoveryProcess">
                      <!-- every other phase should have a non-null recovery process -->
                      Internal error: No recovery process available. <!-- no need to localize this. -->
                    </div>

                    <div v-else>
                      <div v-if="recoveryProcess.type === 'CHANGE_PERMISSIONS'" >
                        Ownership
                        
                        <div class="mt-4 space-y-1 text-sm text-gray-500">
                          <span class="font-medium text-gray-700">Owners</span>
                          <MultiUserSelectInputGroup
                            :selected-users="selectedNewOwners"
                            :on-search="noopSearch"
                            :input-visible="false"
                          />
                          <!-- Members (non-owners) selector -->
                          <div class="mt-4">
                            <label class="block text-sm font-medium text-gray-700">
                              Members
                            </label>
                            <MultiUserSelectInputGroup
                              :selected-users="selectedNewmembers"
                              :on-search="noopSearch"
                              :input-visible="false"
                            />
                          </div>
                          <!-- Removed Members display -->
                          <div v-if="removedMembers.length > 0" class="mt-4">
                            <span class="block text-sm font-medium text-gray-700">
                              Removed
                            </span>
                            <MultiUserSelectInputGroup
                              :selected-users="removedMembers"
                              :on-search="noopSearch"
                              :input-visible="false"
                            />
                          </div>
                        </div>
                      </div>
                      <div v-if="recoveryProcess.type === 'COUNCIL_CHANGE'" >
                        Council Change
                        <div class="mt-4 space-y-1 text-sm text-gray-500">
                          <div v-if="recoveryProcess.details.newCouncilMemberIds.length > 0">
                            <span class="font-medium text-gray-700">New Council Members:</span>
                            <MultiUserSelectInputGroup
                              :selected-users="newCouncilMembers"
                              :on-search="noopSearch"
                              :input-visible="false"
                              :error-message="'At least ' + defaultMinMembers + ' members must be selected.'"
                              :has-error="hasCouncilMemberError"
                            />
                          </div>
                          <div>
                            <span class="font-medium text-gray-700">Required KeyShares:</span>
                            {{ recoveryProcess.details.newRequiredKeyShares }}
                          </div>
                          <label class="block text-sm font-medium text-gray-700 pt-4">
                            Possible Emergency Scenario
                          </label>
                          <EmergencyScenarioVisualization
                            :selected-users="newCouncilMembers"
                            :grant-button-disabled="isGrantButtonDisabled"
                            :required-key-shares="newRequiredKeyShares"
                            :min-members="defaultMinMembers"
                          />
                        </div>
                      </div>
                      <div v-if="phase === 'complete' && !didAddMyShare" class="text-sm pt-2">
                        <span class="inline-flex items-center gap-2 rounded-md bg-green-50 ring-1 ring-green-300/70 px-2.5 py-1 text-xs font-medium text-green-800">
                          <InformationCircleIcon class="h-4 w-4" aria-hidden="true" />
                          You can finish this emergency access process by adding the last key share and completing it.
                        </span>
                      </div>
                      <div v-else-if="(phase === 'complete' || phase === 'approve') && didAddMyShare" class="text-sm pt-2">
                        <span class="inline-flex items-center gap-2 rounded-full bg-green-50 ring-1 ring-green-300/70 px-2.5 py-1 text-xs font-medium text-green-800">
                          <CheckBadgeIcon class="h-4 w-4" aria-hidden="true" />
                          Your key share has already been added.
                        </span>
                      </div>
                    </div>
                    <div v-if="phase !== 'start' && !isMeInProcessCouncil" class="text-sm pt-4">
                      <span class="inline-flex items-center gap-2 rounded-md bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800 text-left">
                        <ExclamationCircleIcon class="h-4 w-4" aria-hidden="true" />
                        You are not part of the council for this process and cannot add a key share.
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            
              <div v-if="onError != null" class="w-full sm:w-auto mb-2 text-right">
                <p v-if="onError instanceof PaymentRequiredError" class="inline-block text-sm text-red-900 bg-red-100 rounded px-3 py-1 mt-1">
                  {{ t('vaultDetails.error.licenseViolated') }}
                </p>
                <p v-else class="inline-block text-sm text-red-700 bg-red-100 rounded px-3 py-1 mt-1">
                  {{ t('common.unexpectedError', [onError.message]) }}
                </p>
              </div>

              <div v-if="conflictingProcessExists && phase === 'start'" class="w-full sm:w-auto mb-2 text-right">
                <p class="inline-block text-sm text-red-700 bg-red-100 rounded px-3 py-1">
                  {{ t('recoveryDialog.error.processAlreadyExists') }}
                </p>
              </div>
              <div class="bg-gray-50 rounded-b-lg px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                <!-- START -->
                <template v-if="phase === 'start'">
                  <button
                    type="button"
                    class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                    :disabled="!canStartRecovery"
                    @click="startRecovery()"
                  >
                    Start
                  </button>
                </template>

                <!-- APPROVE -->
                <template v-else-if="phase === 'approve'">
                  <button
                    v-if="canSeeApprove"
                    type="button"
                    class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm"
                    @click="approveRecovery()"
                  >
                    Approve
                  </button>
                </template>

                <!-- COMPLETE -->
                <template v-else-if="phase === 'complete'">
                  <button
                    v-if="canSeeComplete"
                    type="button"
                    class="inline-flex w-full sm:w-auto justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:text-sm"
                    @click="completeRecovery()"
                  >
                    Complete Process
                  </button>
                </template>

                <!-- CLOSE -->
                <button
                  type="button"
                  class="mt-3 inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:w-auto sm:text-sm"
                  @click.stop="open = false" 
                >
                  {{ t('common.close') }}
                </button>
                
                <!-- ABORT -->
                <template v-if="phase !== 'start' && isMeInProcessCouncil">
                  <p
                    class="mt-2 text-sm text-red-600 cursor-pointer hover:underline sm:order-last sm:mr-auto"
                    @click.stop="requestCancel()"
                  >
                    Abort this Process
                  </p>
                </template>
              </div>
            </div>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>

  <ProcessAbortDialog
    v-if="props.recoveryProcess"
    ref="abortDialog"
    :recovery-process-id="props.recoveryProcess.id"
    @confirmed="handleRecoveryAborted"
    @close="onAbortClosed"
  />
</template>

<script setup lang="ts">
import backend, { VaultDto, VaultRole, UserDto, RecoveryProcessDto, didCompleteSetup, RecoveryProcessSetNewOwner, RecoveryProcessChangeCouncil, ActivatedUser, AccessGrant, RecoveredKeyShareDto, PaymentRequiredError, AuthorityDto } from '../../common/backend';
import { ref, computed, toRaw, Ref } from 'vue';
import { useI18n } from 'vue-i18n';
import * as R from 'remeda';
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ExclamationTriangleIcon, PlayIcon } from '@heroicons/vue/24/solid';
import { EmergencyAccess } from '../../common/emergencyaccess';
import userdata from '../../common/userdata';
import MultiUserSelectInputGroup from '../MultiUserSelectInputGroup.vue';
import EmergencyScenarioVisualization from './EmergencyScenarioVisualization.vue';
import ProcessAbortDialog from './ProcessAbortDialog.vue';
import { asPublicKey, UserKeys, VaultKeys } from '../../common/crypto';
import { wordEncoder } from '../../common/util';
import { base64 } from 'rfc4648';
import { ECDSA_P384, JWT, JWTHeader } from '../../common/jwt';
import SegmentRing from './SegmentRing.vue';
import { CheckBadgeIcon, ExclamationCircleIcon, InformationCircleIcon } from '@heroicons/vue/20/solid';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vault: VaultDto;
  me: UserDto;
  recoveryProcess?: RecoveryProcessDto;
  startType?: RecoveryProcessDto['type'];
}>();

const emit = defineEmits<{
  close: [];
  updated: [recoveryProcess?: RecoveryProcessDto];
}>();

defineExpose({ show });

const processType = ref<RecoveryProcessDto['type']>(
  props.recoveryProcess?.type ?? props.startType ?? 'CHANGE_PERMISSIONS'
);

const meId = computed(() => props.me.id);

const processCouncilIds = computed(() =>
  props.recoveryProcess ? Object.keys(props.recoveryProcess.recoveredKeyShares ?? {}) : []
);

const isMeInProcessCouncil = computed(() =>
  !!props.recoveryProcess && processCouncilIds.value.includes(meId.value)
);

const canSeeApprove = computed(() =>
  phase.value === 'approve' 
  && isMeInProcessCouncil.value 
  && !didAddMyShare.value
);

const canSeeComplete = computed(() =>
  phase.value === 'complete' 
  && isMeInProcessCouncil.value 
  && (!didAddMyShare.value || completedSegments.value >= requiredSegments.value)
);

type PhaseType = 'start' | 'approve' | 'complete';
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

const requiredSegments = computed(() =>
  props.recoveryProcess?.requiredKeyShares ?? props.vault.requiredEmergencyKeyShares
);

const completedSegments = computed(() =>
  Object.values(props.recoveryProcess?.recoveredKeyShares ?? {}).filter(
    ks => ks.recoveredKeyShare !== undefined
  ).length
);

const didAddMyShare = computed(() => {
  return props.recoveryProcess?.recoveredKeyShares?.[props.me.id]?.recoveredKeyShare !== undefined;
});

const open = ref(false);
const onError = ref<Error | null>();

const hasCouncilMemberError = computed(() =>
  processType.value === 'COUNCIL_CHANGE' &&
  newCouncilMembers.value.length > 0 &&
  newCouncilMembers.value.length < defaultMinMembers.value
);

const existingProcesses = ref<RecoveryProcessDto[]>([]);

const conflictingProcessExists = computed(() => {
  return existingProcesses.value.some(p => p.type === processType.value);
});

function processConflicts(type: RecoveryProcessDto['type']) {
  return existingProcesses.value.some(p => p.type === type);
}

// OWNERS
const owners = ref<UserDto[]>([]);
const existingOwnerIds = ref<Set<string>>(new Set());
const existingOwners = ref<UserDto[]>([]);
const newOwnerIds = computed(() => owners.value.map(u => u.id));

// MEMBERS (non-owners)
const members = ref<UserDto[]>([]);
const existingMembers = ref<UserDto[]>([]);
const existingMemberIds = ref<Set<string>>(new Set());
const newMemberIds = computed(() => members.value.map(u => u.id));

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type?: string;
  memberSize?: number;
}

function getCurrentCouncilMembers(vault: VaultDto): Item[] {
  const ids = Object.keys(vault.emergencyKeyShares ?? {});
  return ids.map((id) => {
    const a = authoritiesById.value[id];
    if (a && (a as any).name) {
      return { id: a.id, name: (a as any).name, pictureUrl: (a as any).pictureUrl };
    } else {
      return { id, name: id };
    }
  }).sort((a, b) => a.name.localeCompare(b.name));
}

const authoritiesById = ref<Record<string, AuthorityDto>>({});

// --- helper: generic add/remove by id ---
function addUnique(list: Ref<UserDto[]>, user: UserDto) {
  if (!list.value.find(u => u.id === user.id)) list.value.push(user);
}
function removeFrom(list: Ref<UserDto[]>, user: UserDto) {
  list.value = list.value.filter(u => u.id !== user.id);
}

// Owner handlers keep lists in sync
const addOwner = (user: UserDto) => {
  addUnique(owners, user);
  removeFrom(members, user);
};
const removeOwner = (user: UserDto) => {
  removeFrom(owners, user);
};

// Member handlers keep lists in sync
const addMember = (user: UserDto) => {
  addUnique(members, user);
  removeFrom(owners, user);
};
const removeMember = (user: UserDto) => {
  removeFrom(members, user);
};

const removedMembers = computed<UserDto[]>(() => {
  const initialOwnerAndMemberIds = new Set<string>([
    ...initialOwnerIds.value,
    ...initialMemberIds.value,
  ]);

  const newOwnerIdList =
    props.recoveryProcess?.type === 'CHANGE_PERMISSIONS'
      ? props.recoveryProcess.details.newOwnerIds ?? []
      : newOwnerIds.value;

  const newMemberIdList =
    props.recoveryProcess?.type === 'CHANGE_PERMISSIONS'
      ? props.recoveryProcess.details.newMemberIds ?? []
      : newMemberIds.value;

  const newIds = new Set<string>([...newOwnerIdList, ...newMemberIdList]);

  const removedIds = Array.from(initialOwnerAndMemberIds).filter((id) => !newIds.has(id));

  const byId = R.indexBy(
    [...existingOwners.value, ...existingMembers.value],
    (u) => u.id,
  );

  return removedIds
    .map((id) => byId[id])
    .filter((u): u is UserDto => !!u);
});

const selectedNewOwners = computed<UserDto[]>(() => {
  if (props.recoveryProcess?.type === 'CHANGE_PERMISSIONS') {
    const ids = new Set(props.recoveryProcess.details.newOwnerIds);
    return owners.value.filter(u => ids.has(u.id));
  }
  const ids = new Set(newOwnerIds.value);
  return owners.value.filter(u => ids.has(u.id));
});

const selectedNewmembers = computed<UserDto[]>(() => {
  if (props.recoveryProcess?.type === 'CHANGE_PERMISSIONS') {
    const ids = new Set(props.recoveryProcess.details.newMemberIds);
    return members.value.filter(u => ids.has(u.id));
  }
  const ids = new Set(newMemberIds.value);
  return members.value.filter(u => ids.has(u.id));
});

function setAndArrayDifferById(setIds: Set<string>, arr: { id: string }[]): boolean {
  if (setIds.size !== arr.length) return true;
  for (const u of arr) if (!setIds.has(u.id)) return true;
  return false;
}

const ownersDifferFromExistingIds = computed(() => setAndArrayDifferById(existingOwnerIds.value, owners.value));
const membersDifferFromExistingIds = computed(() => setAndArrayDifferById(existingMemberIds.value, members.value));

// COUNCIL CHANGE
const newRequiredKeyShares = ref<number>(props.vault.requiredEmergencyKeyShares);
const newCouncilMembers = ref<ActivatedUser[]>([]);
function addCouncilMember(user: UserDto) { addUnique(newCouncilMembers, user); }
function removeCouncilMember(user: UserDto) { removeFrom(newCouncilMembers, user); }

const isGrantButtonDisabled = computed(() => newCouncilMembers.value.length < newRequiredKeyShares.value);

const canStartRecovery = computed(() => {
  if (processType.value == null) return false;
  if (conflictingProcessExists.value) return false;

  if (processType.value === 'CHANGE_PERMISSIONS') {
    return (
      hasActivatedOwner.value &&
      (ownersDifferFromExistingIds.value || membersDifferFromExistingIds.value) &&
      owners.value.length !== 0
    );
  }

  if (processType.value === 'COUNCIL_CHANGE') {
    return (
      newCouncilMembers.value.length >= defaultMinMembers.value
      && isCouncilChanged.value
    );
  }

  return false;
});

const isCouncilChanged = computed(() => {
  const currentCouncilIds = Object.keys(props.vault.emergencyKeyShares ?? {});
  const newCouncilIds = newCouncilMembers.value.map(u => u.id);

  if (currentCouncilIds.length !== newCouncilIds.length) {
    return true;
  }

  const currentSet = new Set(currentCouncilIds);
  return newCouncilIds.some(id => !currentSet.has(id));
});

const hasActivatedOwner = computed(() =>
  owners.value.some(u => didCompleteSetup(u as ActivatedUser))
);

const noopSearch = async () => [];

async function searchUsers(query: string): Promise<UserDto[]> {
  const authorities = await backend.authorities.search(query, true);
  return authorities
    .filter((a): a is UserDto => a.type === 'USER')
    .sort((a, b) => a.name.localeCompare(b.name));
}

async function searchUsersWithCompleteSetup(query: string): Promise<UserDto[]> {
  const authorities = await backend.authorities.search(query, true);
  return authorities
    .filter((a): a is UserDto => a.type === 'USER' && didCompleteSetup(a))
    .sort((a, b) => a.name.localeCompare(b.name));
}

function needsRedundancy(): boolean {
  return newRequiredKeyShares.value == newCouncilMembers.value.length;
}

const abortDialog = ref<InstanceType<typeof ProcessAbortDialog> | null>(null);
const wantAbort = ref(false);

function requestCancel() {
  if (!props.recoveryProcess) return;
  wantAbort.value = true;
  abortDialog.value?.show();
}

function onAbortClosed() { wantAbort.value = false; }

function handleParentClose() { if (!wantAbort.value) open.value = false; }

function handleAfterLeave() { if (!open.value) emit('close'); }

async function handleRecoveryAborted() {
  if (!props.recoveryProcess) return;
  onError.value = null;
  try {
    await backend.emergencyAccess.abort(props.recoveryProcess.id);
    emit('updated');
    wantAbort.value = false;
    open.value = false;
  } catch (error) {
    console.error('Cancelling emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

const initialOwnerIds = ref<Set<string>>(new Set());
const initialMemberIds = ref<Set<string>>(new Set());

async function show() {
  await loadExistingProcessesForVault();
  initProcessType();
  await initOwnersAndMembers();
  await loadAuthoritiesForCouncilAndProcesses();
  await initProcessSpecificState();

  open.value = true;
}

async function loadExistingProcessesForVault() {
  existingProcesses.value = await backend.emergencyAccess.findProcessesForVault(props.vault.id);
}

function initProcessType() {
  if (props.recoveryProcess) {
    processType.value = props.recoveryProcess.type;
  } else if (props.startType) {
    processType.value = props.startType;
  } else {
    const allTypes: RecoveryProcessDto['type'][] = ['CHANGE_PERMISSIONS', 'COUNCIL_CHANGE'];
    const firstFree = allTypes.find(t => !processConflicts(t));
    processType.value = firstFree ?? 'CHANGE_PERMISSIONS';
  }
}

async function initOwnersAndMembers() {
  try {
    const memberList = await backend.vaults.getMembers(props.vault.id);
    const initialOwners = memberList.filter(m => m.type === 'USER' && m.role === 'OWNER') as UserDto[];
    const initialMembers = memberList.filter(m => m.type === 'USER' && m.role === 'MEMBER') as UserDto[];

    existingOwners.value = initialOwners;
    owners.value = [...initialOwners];
    existingOwnerIds.value = new Set(initialOwners.map(u => u.id));

    existingMembers.value = initialMembers;
    members.value = [...initialMembers];
    existingMemberIds.value = new Set(initialMembers.map(u => u.id));

    initialOwnerIds.value = new Set(initialOwners.map(u => u.id));
    initialMemberIds.value = new Set(initialMembers.map(u => u.id));
  } catch (e) {
    console.error('Loading existing owners/members failed', e);
  }
}

async function loadAuthoritiesForCouncilAndProcesses() {
  const memberIdsOfAllRunningProcesses = existingProcesses.value.flatMap(p =>
    Object.keys(p.recoveredKeyShares ?? {})
  );

  const councilIds = Object.keys(props.vault.emergencyKeyShares ?? {});
  const allIds = Array.from(new Set([...memberIdsOfAllRunningProcesses, ...councilIds]));

  if (allIds.length > 0) {
    const auths = await backend.authorities.listSome(allIds);
    authoritiesById.value = R.indexBy(auths, u => u.id);
  } else {
    authoritiesById.value = {};
  }
}

async function initProcessSpecificState() {
  if (props.recoveryProcess?.type === 'COUNCIL_CHANGE') {
    const users = await backend.authorities.listSome(props.recoveryProcess.details.newCouncilMemberIds);
    const sorted = users
      .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
      .sort((a, b) => a.name.localeCompare(b.name));
    newCouncilMembers.value = [...sorted];
    newRequiredKeyShares.value = props.recoveryProcess.details.newRequiredKeyShares;
  } else if (!props.recoveryProcess && processType.value === 'COUNCIL_CHANGE') {
    await loadDefaultSettings();
    newCouncilMembers.value = [...defaultEmergencyCouncilMembers.value];
    newRequiredKeyShares.value = defaultRequiredEmergencyKeyShares.value;
  } else if (props.recoveryProcess?.type === 'CHANGE_PERMISSIONS') {
    const newOwners = await backend.authorities.listSome(props.recoveryProcess.details.newOwnerIds);
    for (const u of newOwners) {
      if (!owners.value.find(x => x.id === u.id)) {
        owners.value.push(u as UserDto);
      }
    }
    const newMembers = await backend.authorities.listSome(props.recoveryProcess.details.newMemberIds);
    for (const u of newMembers) {
      if (!members.value.find(x => x.id === u.id)) {
        members.value.push(u as UserDto);
      }
    }
  }
}

const phaseTitle = computed(() => {
  switch (phase.value) {
    case 'start': {
      if (processType.value === 'COUNCIL_CHANGE') {
        return 'Change council';
      }
      return 'Change vault permissions';
    }
    case 'approve': {
      if (!isMeInProcessCouncil.value) {
        return 'Process details';
      }
      return didAddMyShare.value ? 'Approved' : 'Approve Emergency Access';
    }
    case 'complete': {
      return !didAddMyShare.value ? 'Complete Emergency Access' : 'Approved';
    }
    default:
      return '';
  }
});

const phaseDescription = computed(() => {
  switch (phase.value) {
    case 'start': return 'Start Emergency Access Desc';
    case 'approve': return didAddMyShare.value ? 'You have already added your part of the emergency key.' : 'Approve Emergency Access Desc';
    case 'complete': return 'Complete Emergency Access Desc';
    default: return '';
  }
});

/**
 * PHASE ONE: Starting the recovery process and adding the first share.
 */
async function startRecovery() {
  onError.value = null;
  try {
    const recoveryCouncilMemberIds = Object.keys(props.vault.emergencyKeyShares);
    const authorities = await backend.authorities.listSome(recoveryCouncilMemberIds);
    const councilMembers = authorities.filter(a => a.type == 'USER').filter(u => didCompleteSetup(u));
    if (councilMembers.length < props.vault.requiredEmergencyKeyShares) {
      throw new Error(`Inconsistent data: Insufficient council members (${councilMembers.length}) to recovery this vault (${props.vault.requiredEmergencyKeyShares}).`);
    }

    let data: RecoveryProcessSetNewOwner | RecoveryProcessChangeCouncil;
    if (processType.value === 'CHANGE_PERMISSIONS') {
      if (newOwnerIds.value.length === 0) {
        throw new Error(t('recoveryDialog.error.noOwnerSelected'));
      }
      data = {
        type: 'CHANGE_PERMISSIONS',
        details: {
          newOwnerIds: newOwnerIds.value,
          newMemberIds: newMemberIds.value
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

    const processKeyPair = await EmergencyAccess.startRecovery(councilMembers);
    const process: RecoveryProcessDto = {
      id: crypto.randomUUID(),
      vaultId: props.vault.id,
      ...data,
      requiredKeyShares: props.vault.requiredEmergencyKeyShares,
      processPublicKey: processKeyPair.recoveryPublicKey,
      recoveredKeyShares: {}
    };
    for (const [memberId, jwe] of Object.entries(processKeyPair.recoveryPrivateKeys)) {
      process.recoveredKeyShares[memberId] = {
        unrecoveredKeyShare: props.vault.emergencyKeyShares[memberId],
        processPrivateKey: jwe,
      };
    }

    const userKeys = await userdata.decryptUserKeysWithBrowser();
    process.recoveredKeyShares[props.me.id] = await addMyShare(process, userKeys);

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
  onError.value = null;
  try {
    const verifiedProcess = await verifyProcessInfo(props.recoveryProcess);
    if (!verifiedProcess) {
      throw new Error('Recovery process has been tampered with.');
    }

    const userKeys = await userdata.decryptUserKeysWithBrowser();
    const process = structuredClone(toRaw(props.recoveryProcess));
    process.recoveredKeyShares[props.me.id] = await addMyShare(process, userKeys);
    await backend.emergencyAccess.addMyShare(process.id, process.recoveredKeyShares[props.me.id]);

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
  onError.value = null;
  try {
    const verifiedProcess = await verifyProcessInfo(props.recoveryProcess);
    if (!verifiedProcess) {
      throw new Error('Recovery process has been tampered with.');
    }

    const userKeys = await userdata.decryptUserKeysWithBrowser();

    const process = structuredClone(toRaw(props.recoveryProcess));
    process.recoveredKeyShares[props.me.id] = await addMyShare(process, userKeys);

    const keyShares = Object.values(process.recoveredKeyShares).filter(p => p.recoveredKeyShare !== undefined).map(p => p.recoveredKeyShare!);

    const processPrivateKey = process.recoveredKeyShares[props.me.id].processPrivateKey;
    const recoveredKeyBytes = await EmergencyAccess.combineRecoveredShares(keyShares, processPrivateKey, userKeys.ecdhKeyPair.privateKey);
    const recoveredKey = wordEncoder.encodePadded(recoveredKeyBytes);

    if (process.type === 'COUNCIL_CHANGE' && newCouncilMembers.value.length >= process.details.newRequiredKeyShares) {
      const keyShares = await EmergencyAccess.split(recoveredKeyBytes, process.details.newRequiredKeyShares, ...newCouncilMembers.value);
      await backend.vaults.createOrUpdateVault(
        props.vault.id,
        props.vault.name,
        props.vault.archived,
        process.details.newRequiredKeyShares,
        keyShares,
        props.vault.description
      );
    } else if (process.type === 'CHANGE_PERMISSIONS') {
      const vaultKeys = await VaultKeys.recover(recoveredKey);

      const membersWithRole = Object.fromEntries([
        ...selectedNewOwners.value.map(u => [u.id, 'OWNER']),
        ...selectedNewmembers.value.map(u => [u.id, 'MEMBER'])
      ]) as Record<string, VaultRole>;

      await backend.vaults.setMembersWithRole(props.vault.id, membersWithRole);

      const didCompleteSetupMembers = [...selectedNewOwners.value, ...selectedNewmembers.value]
        .filter(u => didCompleteSetup(u));

      const accessGrants: AccessGrant[] = await Promise.all(
        didCompleteSetupMembers.map(async u => {
          const publicKey = base64.parse(u.ecdhPublicKey);
          const jwe = await vaultKeys.encryptForUser(publicKey);
          return { userId: u.id, token: jwe };
        })
      );

      if (accessGrants.length > 0) { await backend.vaults.grantAccess(props.vault.id, ...accessGrants); }
    } else {
      throw new Error(`Unsupported state for recovery process type: ${process.type}`);
    }

    await backend.emergencyAccess.complete(process.id);
    await backend.emergencyAccess.delete(process.id);
    emit('updated');
    open.value = false;
  } catch (error) {
    console.error('Completing emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function addMyShare(process: RecoveryProcessDto, userKeys: UserKeys): Promise<RecoveredKeyShareDto> {
  const encryptedShare = process.recoveredKeyShares[props.me.id].unrecoveredKeyShare;
  const recoveredShare = await EmergencyAccess.recoverShare(encryptedShare, userKeys.ecdhKeyPair.privateKey, process.processPublicKey);

  const processInfo = R.pick(process, ['type', 'details']);

  const signedProcessInfo = await JWT.build({
    alg: 'ES384',
    typ: 'JWT',
    b64: true,
    iss: props.me.id,
    sub: process.id,
    iat: Math.floor(Date.now() / 1000)
  }, processInfo, userKeys.ecdsaKeyPair.privateKey);

  return {
    ...process.recoveredKeyShares[props.me.id],
    recoveredKeyShare: recoveredShare,
    signedProcessInfo: signedProcessInfo
  };
}

async function verifyProcessInfo(process: RecoveryProcessDto): Promise<boolean> {
  const councilMemberIds = Object.keys(process.recoveredKeyShares);
  const authorities = await backend.authorities.listSome(councilMemberIds);
  const councilMembers = R.indexBy(
    authorities.filter(a => a.type === 'USER').filter(u => didCompleteSetup(u)),
    u => u.id
  );

  for (const [councilMemberId, recoveredKeyShare] of Object.entries(process.recoveredKeyShares)) {
    if (!recoveredKeyShare.recoveredKeyShare) {
      continue;
    } else if (!recoveredKeyShare.signedProcessInfo) {
      console.error(`Missing signed process info for council member ${councilMemberId}.`);
      return false;
    } else if (!councilMembers[councilMemberId]) {
      console.error(`Unknown council member ${councilMemberId}.`);
      return false;
    }
    const councilMember = councilMembers[councilMemberId];
    const publicKey = await asPublicKey(base64.parse(councilMember.ecdsaPublicKey), ECDSA_P384, ['verify']);
    const [header, payload] = await JWT.parse(recoveredKeyShare.signedProcessInfo, publicKey) as [JWTHeader, RecoveryProcessDto];
    if (header.sub !== process.id || header.iss !== councilMemberId) {
      console.error(`Invalid signed process info for council member ${councilMemberId}.`);
      return false;
    }
    if (payload.type !== process.type || !R.isDeepEqual(payload.details, process.details)) {
      console.error(`Signed process info for council member ${councilMemberId} does not match the recovery process`, process, payload);
      return false;
    }
  }
  return true;
}

const defaultEmergencyCouncilMembers = ref<ActivatedUser[]>([]);
const defaultRequiredEmergencyKeyShares = ref<number>(0);
const defaultMinMembers = ref<number>(0);
const allowChangingDefaults = ref<boolean>(false);

async function loadDefaultSettings() {
  try {
    const settings = await backend.settings.get();
    const authorities = await backend.authorities.listSome(settings.emergencyCouncilMemberIds);
    const sortedActivatedUsers = authorities
      .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
      .sort((a, b) => a.name.localeCompare(b.name));

    defaultEmergencyCouncilMembers.value = [...sortedActivatedUsers];
    allowChangingDefaults.value = settings.allowChoosingEmergencyCouncil;
    defaultRequiredEmergencyKeyShares.value = settings.defaultRequiredEmergencyKeyShares;
    defaultMinMembers.value = settings.defaultMinMembers;
  } catch (error) {
    console.error('Loading emergency council members failed:', error);
    // TODO: don't set defaults, hard-fail with error message instead
    //resetCouncilMembers();
    defaultRequiredEmergencyKeyShares.value = 0;
    allowChangingDefaults.value = false;
  }
}
</script>
