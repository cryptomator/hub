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
                    <div v-if="showSuccess">
                      <CheckCircleIcon class="h-12 w-12 text-primary" aria-hidden="true" />
                    </div>
                    <div v-else-if="phase !== 'start'">
                      <SegmentRing
                        :total="requiredSegments"
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
                    <div v-if="showSuccess" class="mt-4">
                      <div>
                        {{ t('emergencyAccessDialog.message.completed') }}
                      </div>
                    </div>
                    <div v-else-if="phase === 'start'" class="mt-4 space-y-4">
                      <div v-if="processType === 'CHANGE_PERMISSIONS'">
                        <label class="block text-sm font-medium text-gray-700">
                          {{ t('emergencyAccessDialog.label.selectOwner') }}
                        </label>
                        <MultiUserSelectInputGroup
                          ref="ownersSelect"
                          :selected-users="owners"
                          :on-search="searchUsers"
                          :input-visible="true"
                          @action="addOwner"
                          @remove="removeOwner"
                        />

                        <!-- Members (non-owners) selector -->
                        <div class="mt-4">
                          <label class="block text-sm font-medium text-gray-700">
                            {{ t('emergencyAccessDialog.label.selectMember') }}
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
                            {{ t('emergencyAccess.label.removed') }}
                          </span>
                          <MultiUserSelectInputGroup
                            :selected-users="removedMembers"
                            :on-search="noopSearch"
                            :input-visible="false"
                          />
                        </div>
                      </div>

                      <div v-else-if="processType === 'COUNCIL_CHANGE'">
                        <EmergencyAccessSetup ref="emergencyAccessSetup" :settings="settings" :council-members="councilMembers" :required-key-shares="settings.defaultRequiredEmergencyKeyShares" :allow-choosing-council="true" />
                      </div>
                      <div v-else class="text-sm text-red-600">
                        {{ t('recoveryDialog.error.invalidRecoveryType') }}
                      </div>
                    </div>

                    <div v-else-if="!recoveryProcess">
                      <!-- every other phase should have a recovery process -->
                      Internal error: No recovery process available. <!-- no need to localize this. -->
                    </div>

                    <div v-else>
                      <div v-if="recoveryProcess.type === 'CHANGE_PERMISSIONS'">
                        {{ t('emergencyAccessDialog.section.ownership') }}
                        
                        <div class="mt-4 space-y-1 text-sm text-gray-500">
                          <span class="font-medium text-gray-700">{{ t('emergencyAccess.label.owners') }}</span>
                          <MultiUserSelectInputGroup
                            :selected-users="selectedNewOwners"
                            :on-search="noopSearch"
                            :input-visible="false"
                          />
                          <!-- Members (non-owners) selector -->
                          <div class="mt-4">
                            <label class="block text-sm font-medium text-gray-700">
                              {{ t('emergencyAccess.label.members') }}
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
                              {{ t('emergencyAccess.label.removed') }}
                            </span>
                            <MultiUserSelectInputGroup
                              :selected-users="removedMembers"
                              :on-search="noopSearch"
                              :input-visible="false"
                            />
                          </div>
                        </div>
                      </div>
                      <div v-if="recoveryProcess.type === 'COUNCIL_CHANGE'">
                        {{ t('emergencyAccessDialog.section.councilChange') }}
                        <div class="mt-4">
                          <EmergencyAccessSetup :settings="settings" :council-members="councilMembers" :required-key-shares="recoveryProcess.details.newRequiredKeyShares" :readonly="true" :show-required-key-shares="true" />
                        </div>
                      </div>
                      <div v-if="phase === 'complete' && !didAddMyShare && isMeInProcessCouncil" class="text-sm pt-2">
                        <span class="inline-flex items-center gap-2 rounded-md bg-green-50 ring-1 ring-green-300/70 px-2.5 py-1 text-xs font-medium text-green-800">
                          <InformationCircleIcon class="h-4 w-4" aria-hidden="true" />
                          {{ t('emergencyAccessDialog.hint.finishProcess') }}
                        </span>
                      </div>
                      <div v-else-if="(phase === 'complete' || phase === 'approve') && didAddMyShare" class="text-sm pt-2">
                        <span class="inline-flex items-center gap-2 rounded-full bg-green-50 ring-1 ring-green-300/70 px-2.5 py-1 text-xs font-medium text-green-800">
                          <CheckBadgeIcon class="h-4 w-4" aria-hidden="true" />
                          {{ t('emergencyAccessDialog.hint.keyShareAdded') }}
                        </span>
                      </div>
                    </div>
                    <div v-if="phase !== 'start' && !isMeInProcessCouncil" class="text-sm pt-4">
                      <span class="inline-flex items-center gap-2 rounded-md bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800 text-left">
                        <ExclamationCircleIcon class="h-4 w-4" aria-hidden="true" />
                        {{ t('emergencyAccessDialog.hint.notInCouncil') }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            
              <div v-if="onError" class="w-full sm:w-auto mb-2 text-right">
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

              <div class="bg-gray-50 rounded-b-lg px-4 py-3 sm:px-6 sm:flex">
                <!-- ABORT -->
                <template v-if="phase !== 'start' && (isMeInProcessCouncil || isMeInCouncil) && !showSuccess">
                  <button
                    class=" text-sm text-red-600 hover:underline sm:mr-auto focus:outline-none focus:underline rounded"
                    @click.stop="requestCancel()"
                  >
                    {{ t('emergencyAccessDialog.action.abortProcess') }}
                  </button>
                </template>
                <!-- CLOSE -->
                <button
                  ref="closeButton"
                  type="button"
                  class="mt-3 inline-flex w-full justify-center sm:ml-auto rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:w-auto sm:text-sm"
                  @click.stop="open = false" 
                >
                  {{ t('common.close') }}
                </button>               
                <!-- START -->
                <template v-if="phase === 'start'">
                  <button
                    type="button"
                    class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                    :disabled="!canStartRecovery"
                    @click="startRecovery()"
                  >
                    {{ t('emergencyAccessDialog.action.start') }}
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
                    {{ t('emergencyAccessDialog.action.approve') }}
                  </button>
                </template>

                <!-- COMPLETE -->
                <template v-else-if="phase === 'complete' && !showSuccess">
                  <button
                    v-if="canSeeComplete"
                    ref="completeButton"
                    type="button"
                    class="inline-flex w-full sm:w-auto justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:text-sm"
                    @click="completeRecovery()"
                  >
                    {{ t('emergencyAccessDialog.action.completeProcess') }}
                  </button>
                </template>
              </div>
            </div>
          </DialogPanel>
        </TransitionChild>
      </div>
      <ProcessAbortDialog
        v-if="props.recoveryProcess"
        ref="abortDialog"
        :recovery-process-id="props.recoveryProcess.id"
        @confirmed="handleRecoveryAborted"
        @close="onAbortClosed"
      />
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { CheckBadgeIcon, ExclamationCircleIcon, InformationCircleIcon } from '@heroicons/vue/20/solid';
import { CheckCircleIcon, PlayIcon } from '@heroicons/vue/24/solid';
import { base64 } from '@scure/base';
import * as R from 'remeda';
import { computed, nextTick, ref, Ref, toRaw } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AccessGrant, ActivatedUser, AuthorityDto, didCompleteSetup, GroupDto, PaymentRequiredError, RecoveredKeyShareDto, RecoveryProcessChangeCouncil, RecoveryProcessDto, RecoveryProcessSetNewOwner, SettingsDto, UserDto, VaultDto, VaultRole } from '../../common/backend';
import { asPublicKey, UserKeys, VaultKeys } from '../../common/crypto';
import { EmergencyAccess } from '../../common/emergencyaccess';
import { ECDSA_P384, JWT, JWTHeader } from '../../common/jwt';
import userdata from '../../common/userdata';
import { wordEncoder } from '../../common/util';
import type { MultiUserSelectExpose } from '../MultiUserSelectInputGroup.vue';
import MultiUserSelectInputGroup from '../MultiUserSelectInputGroup.vue';
import EmergencyAccessSetup from './EmergencyAccessSetup.vue';
import ProcessAbortDialog from './ProcessAbortDialog.vue';
import SegmentRing from './SegmentRing.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vault: VaultDto;
  me: UserDto;
  settings: SettingsDto;
  recoveryProcess?: RecoveryProcessDto;
  startType: RecoveryProcessDto['type'];
}>();

const emit = defineEmits<{
  close: [];
  updated: [recoveryProcess?: RecoveryProcessDto];
}>();

defineExpose({ show });
const closeButton = ref<HTMLElement>();
const ownersSelect = ref<MultiUserSelectExpose>();
const emergencyAccessSetup = ref<InstanceType<typeof EmergencyAccessSetup>>();

const processType = ref<RecoveryProcessDto['type']>(
  props.recoveryProcess?.type ?? props.startType
);

const processCouncilIds = computed(() =>
  props.recoveryProcess ? Object.keys(props.recoveryProcess.recoveredKeyShares ?? {}) : []
);

const isMeInProcessCouncil = computed(() =>
  processCouncilIds.value.includes(props.me.id)
);

const isMeInCouncil = computed(() =>
  Object.keys(props.vault.emergencyKeyShares ?? {}).includes(props.me.id)
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
const onError = ref<Error>();

const existingProcesses = ref<RecoveryProcessDto[]>([]);

const conflictingProcessExists = computed(() => {
  return existingProcesses.value.some(p => p.type === processType.value);
});

function processConflicts(type: RecoveryProcessDto['type']) {
  return existingProcesses.value.some(p => p.type === type);
}

// OWNERS
const owners = ref<AuthorityDto[]>([]);
const existingOwners = ref<AuthorityDto[]>([]);
const newOwnerIds = computed(() => owners.value.map(u => u.id));

// MEMBERS (non-owners)
const members = ref<AuthorityDto[]>([]);
const existingMembers = ref<AuthorityDto[]>([]);
const newMemberIds = computed(() => members.value.map(u => u.id));

const authoritiesById = ref<Record<string, AuthorityDto>>({});

// --- helper: generic add/remove by id ---
function addUnique(list: Ref<AuthorityDto[]>, user: AuthorityDto) {
  if (!list.value.find(u => u.id === user.id)) list.value.push(user);
}
function removeFrom(list: Ref<AuthorityDto[]>, user: AuthorityDto) {
  list.value = list.value.filter(u => u.id !== user.id);
}

// Owner handlers keep lists in sync
const addOwner = (user: AuthorityDto) => {
  addUnique(owners, user);
  removeFrom(members, user);
};
const removeOwner = (user: AuthorityDto) => {
  removeFrom(owners, user);
};

// Member handlers keep lists in sync
const addMember = (user: AuthorityDto) => {
  addUnique(members, user);
  removeFrom(owners, user);
};
const removeMember = (user: AuthorityDto) => {
  removeFrom(members, user);
};

const removedMembers = computed<AuthorityDto[]>(() => {
  const oldMembers = [...existingOwners.value, ...existingMembers.value];
  const oldIds = oldMembers.map(u => u.id);
  const oldMembersById = R.indexBy<AuthorityDto, string>(oldMembers, (u) => u.id);

  const newIds: string[] = props.recoveryProcess?.type === 'CHANGE_PERMISSIONS'
    ? [...(props.recoveryProcess.details.newOwnerIds ?? []), ...(props.recoveryProcess.details.newMemberIds ?? [])]
    : [...newOwnerIds.value, ...newMemberIds.value];

  const removedIds = R.difference(oldIds, newIds);
  return removedIds.map((id) => oldMembersById[id]);
});

const selectedNewOwners = computed<AuthorityDto[]>(() => {
  if (props.recoveryProcess?.type === 'CHANGE_PERMISSIONS') {
    const ids = new Set(props.recoveryProcess.details.newOwnerIds);
    return owners.value.filter(u => ids.has(u.id));
  } else {
    return owners.value;
  }
});

const selectedNewmembers = computed<AuthorityDto[]>(() => {
  if (props.recoveryProcess?.type === 'CHANGE_PERMISSIONS') {
    const ids = new Set(props.recoveryProcess.details.newMemberIds);
    return members.value.filter(u => ids.has(u.id));
  } else {
    return members.value;
  }
});

// COUNCIL CHANGE
const councilMembers = ref<ActivatedUser[]>([]);

const canStartRecovery = computed(() => {
  if (processType.value === undefined) return false;
  if (conflictingProcessExists.value) return false;

  if (processType.value === 'CHANGE_PERMISSIONS') {
    const existingOwnerIds = new Set(existingOwners.value.map(u => u.id));
    const sameOwners = owners.value.length === existingOwners.value.length && owners.value.every(o => existingOwnerIds.has(o.id));
    const existingMemberIds = new Set(existingMembers.value.map(u => u.id));
    const sameMembers = members.value.length === existingMembers.value.length && members.value.every(m => existingMemberIds.has(m.id));
    return !(sameOwners && sameMembers) && owners.value.length !== 0;
  } else if (processType.value === 'COUNCIL_CHANGE' && emergencyAccessSetup.value) {
    const existingCouncilIds = new Set(Object.keys(props.vault.emergencyKeyShares));
    const sameCouncil = emergencyAccessSetup.value.emergencyCouncilMembers.length === existingCouncilIds.size && emergencyAccessSetup.value.emergencyCouncilMembers.every(m => existingCouncilIds.has(m.id));
    const sameRequiredKeyShares = props.settings.defaultRequiredEmergencyKeyShares === props.vault.requiredEmergencyKeyShares; // does the vault's current number of required key shares differ from the configured default?
    return !emergencyAccessSetup.value.hasValidationErrors && (!sameCouncil || !sameRequiredKeyShares);
  }

  return false;
});

const noopSearch = async () => [];

async function searchUsers(query: string): Promise<AuthorityDto[]> {
  const authorities = await backend.authorities.search(query, true);
  return authorities
    .filter((a): a is AuthorityDto => true)
    .sort((a, b) => a.name.localeCompare(b.name));
}

const abortDialog = ref<InstanceType<typeof ProcessAbortDialog>>();
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
  onError.value = undefined;
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

async function show() {
  await loadExistingProcessesForVault();
  initProcessType();
  await initOwnersAndMembers();
  await loadAuthoritiesForCouncilAndProcesses();
  await initProcessSpecificState();
  showSuccess.value = false;
  open.value = true;
  await nextTick();

  if (phase.value === 'start' && processType.value === 'CHANGE_PERMISSIONS') {
    return ownersSelect.value?.focus();
  } else {
    return closeButton.value?.focus();
  }
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
    const initialOwners = memberList.filter(m => m.vaultRole === 'OWNER') as AuthorityDto[];
    const initialMembers = memberList.filter(m => m.vaultRole === 'MEMBER') as AuthorityDto[];

    existingOwners.value = initialOwners;
    owners.value = [...initialOwners];

    existingMembers.value = initialMembers;
    members.value = [...initialMembers];
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
    councilMembers.value = await loadActivatedUsers(props.recoveryProcess.details.newCouncilMemberIds);
  } else if (!props.recoveryProcess && processType.value === 'COUNCIL_CHANGE') {
    councilMembers.value = await loadActivatedUsers(Object.keys(props.vault.emergencyKeyShares));
  } else if (props.recoveryProcess?.type === 'CHANGE_PERMISSIONS') {
    const newOwners = await backend.authorities.listSome(props.recoveryProcess.details.newOwnerIds);
    const enrichedOwners = await enrichGroupsMemberSize([...owners.value, ...newOwners]);
    owners.value = R.uniqueBy(enrichedOwners, u => u.id);

    const newMembers = await backend.authorities.listSome(props.recoveryProcess.details.newMemberIds);
    const enrichedMembers = await enrichGroupsMemberSize([...members.value, ...newMembers]);
    members.value = R.uniqueBy(enrichedMembers, u => u.id);
  }
};

async function enrichGroupsMemberSize(authorities: AuthorityDto[]): Promise<AuthorityDto[]> {
  const groups = authorities.filter(a => a.type === 'GROUP') as GroupDto[];
  if (groups.length === 0) return authorities;

  const lookups = await Promise.all(groups.map(async g => {
    const res = await backend.authorities.search(g.name, true);
    const hit = res.find(a => a.type === 'GROUP' && a.id === g.id) as GroupDto | undefined;
    return [g.id, hit?.memberSize] as const;
  }));
  const byId = Object.fromEntries(lookups);

  return authorities.map(a =>
    a.type === 'GROUP'
      ? { ...a, memberSize: byId[a.id] ?? (a as GroupDto).memberSize }
      : a
  );
}

const showSuccess = ref(false);

const phaseTitle = computed(() => {
  if (showSuccess.value) return t('emergencyAccessDialog.title.success');

  switch (phase.value) {
    case 'start': {
      if (processType.value === 'COUNCIL_CHANGE') {
        return t('emergencyAccessDialog.title.changeCouncil');
      }
      return t('emergencyAccessDialog.title.changePermissions');
    }
    case 'approve': {
      if (!isMeInProcessCouncil.value) {
        return t('emergencyAccessDialog.title.processDetails');
      }
      return didAddMyShare.value
        ? t('emergencyAccessDialog.title.approved')
        : t('emergencyAccessDialog.title.approveEmergencyAccess');
    }
    case 'complete': {
      return !didAddMyShare.value
        ? t('emergencyAccessDialog.title.completeEmergencyAccess')
        : t('emergencyAccessDialog.title.approved');
    }
    default:
      return '';
  }
});

/**
 * PHASE ONE: Starting the recovery process and adding the first share.
 */
async function startRecovery() {
  onError.value = undefined;
  try {
    const recoveryCouncilMemberIds = Object.keys(props.vault.emergencyKeyShares);
    const councilMembers = await loadActivatedUsers(recoveryCouncilMemberIds);
    if (councilMembers.length < props.vault.requiredEmergencyKeyShares) {
      throw new Error(t('emergencyAccessDialog.error.insufficientCouncilMembers', [councilMembers.length, props.vault.requiredEmergencyKeyShares]));
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
    } else if (processType.value === 'COUNCIL_CHANGE' && emergencyAccessSetup.value) {
      if (emergencyAccessSetup.value.hasValidationErrors) {
        throw new Error(t('recoveryDialog.error.notEnoughCouncilMembers')); // TODO: change error message
      }
      data = {
        type: 'COUNCIL_CHANGE',
        details: {
          newCouncilMemberIds: emergencyAccessSetup.value.emergencyCouncilMembers.map(u => u.id),
          newRequiredKeyShares: props.settings.defaultRequiredEmergencyKeyShares
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
    throw new Error(t('emergencyAccessDialog.error.noProcessToApprove'));
  }
  onError.value = undefined;
  try {
    const verifiedProcess = await verifyProcessInfo(props.recoveryProcess);
    if (!verifiedProcess) {
      throw new Error(t('emergencyAccessDialog.error.processTampered'));
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
    throw new Error(t('emergencyAccessDialog.error.noProcessToComplete'));
  }
  onError.value = undefined;
  try {
    const verifiedProcess = await verifyProcessInfo(props.recoveryProcess);
    if (!verifiedProcess) {
      throw new Error(t('emergencyAccessDialog.error.processTampered'));
    }

    const userKeys = await userdata.decryptUserKeysWithBrowser();

    const process = structuredClone(toRaw(props.recoveryProcess));
    process.recoveredKeyShares[props.me.id] = await addMyShare(process, userKeys);

    const keyShares = Object.values(process.recoveredKeyShares).filter(p => p.recoveredKeyShare !== undefined).map(p => p.recoveredKeyShare!);

    const processPrivateKey = process.recoveredKeyShares[props.me.id].processPrivateKey;
    const recoveredKeyBytes = await EmergencyAccess.combineRecoveredShares(keyShares, processPrivateKey, userKeys.ecdhKeyPair.privateKey);
    const recoveredKey = wordEncoder.encodePadded(recoveredKeyBytes);

    if (process.type === 'COUNCIL_CHANGE') {
      if (councilMembers.value.length < process.details.newRequiredKeyShares) {
        throw new Error(t('emergencyAccessDialog.error.insufficientCouncilMembers', [councilMembers.value.length, process.details.newRequiredKeyShares]));
      }
      const keyShares = await EmergencyAccess.split(recoveredKeyBytes, process.details.newRequiredKeyShares, ...councilMembers.value);
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
        ...selectedNewmembers.value.map(u => [u.id, 'MEMBER']),
        ...selectedNewOwners.value.map(u => [u.id, 'OWNER'])
      ]) as Record<string, VaultRole>;

      await backend.vaults.setMembersWithRole(props.vault.id, membersWithRole);

      const activatedUsersToGrant = (await backend.vaults.getUsersRequiringAccessGrant(props.vault.id))
        .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a));

      const accessGrants: AccessGrant[] = await Promise.all(
        activatedUsersToGrant.map(async u => {
          const publicKey = base64.decode(u.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
          const jwe = await vaultKeys.encryptForUser(publicKey);
          return { userId: u.id, token: jwe };
        })
      );

      if (accessGrants.length > 0) { await backend.vaults.grantAccess(props.vault.id, ...accessGrants); }
    }

    await backend.emergencyAccess.complete(process.id);
    emit('updated');
    showSuccess.value = true; 
    closeButton.value?.focus();
  } catch (error) {
    console.error('Completing emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

type SignedProcessInfoPayload = Pick<RecoveryProcessDto, 'type' | 'details'> & {
  iss: string;
  sub: string;
  iat: number;
};

async function addMyShare(process: RecoveryProcessDto, userKeys: UserKeys): Promise<RecoveredKeyShareDto> {
  const encryptedShare = process.recoveredKeyShares[props.me.id].unrecoveredKeyShare;
  const recoveredShare = await EmergencyAccess.recoverShare(encryptedShare, userKeys.ecdhKeyPair.privateKey, process.processPublicKey);

  const processInfo = R.pick(process, ['type', 'details']);
  const payload: SignedProcessInfoPayload = {
    iss: props.me.id,
    sub: process.id,
    iat: Math.floor(Date.now() / 1000),
    ...processInfo
  };

  const signedProcessInfo = await JWT.build({
    alg: 'ES384',
    typ: 'JWT',
    b64: true
  }, payload, userKeys.ecdsaKeyPair.privateKey);

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
    const publicKey = await asPublicKey(base64.decode(councilMember.ecdsaPublicKey) as Uint8Array<ArrayBuffer>, ECDSA_P384, ['verify']);
    const [_header, payload] = await JWT.parse(recoveredKeyShare.signedProcessInfo, publicKey) as [JWTHeader, SignedProcessInfoPayload];
    if (payload.sub !== process.id || payload.iss !== councilMemberId) {
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

async function loadActivatedUsers(ids: string[]): Promise<ActivatedUser[]> {
  const authorities = await backend.authorities.listSome(ids);
  return authorities
    .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
    .sort((a, b) => a.name.localeCompare(b.name));
}
</script>
