<template>
  <HubDialog v-model:open="open" :title="phaseTitle" :busy="processing" :error="errorMessage" :initial-focus="initialFocus" @submit="submit()" @closed="emit('close')">
    <DialogDescription v-if="showSuccess" as="p" class="mt-2 text-sm text-gray-500">
      {{ t('emergencyAccessDialog.message.completed') }}
    </DialogDescription>

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
          @trust-changed="refreshTrusts"
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
            @trust-changed="refreshTrusts"
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

    <p v-else-if="!recoveryProcess" class="mt-2 text-sm">
      <!-- every other phase should have a recovery process -->
      Internal error: No recovery process available. <!-- no need to localize this. -->
    </p>

    <template v-else>
      <DialogDescription as="p" class="mt-1 text-sm text-gray-500">
        {{ starter ? t('emergencyAccessDialog.context.vaultStartedBy', [vault.name, starter.name, d(starter.startedAt, 'short')]) : t('emergencyAccessDialog.context.vault', [vault.name]) }}
      </DialogDescription>

      <div class="group relative mt-3 inline-flex items-center gap-2">
        <SegmentRing :total="requiredSegments" :completed="completedSegments" :size="20" />
        <button type="button" :aria-describedby="approvalsTooltipId" class="cursor-default rounded-sm text-sm text-gray-700 underline decoration-dotted underline-offset-4 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-primary">
          {{ t('emergencyAccessDialog.approvals.count', [completedSegments, requiredSegments]) }}
        </button>
        <div :id="approvalsTooltipId" role="tooltip" class="invisible absolute left-0 top-full z-20 mt-2 w-72 max-w-[calc(100vw-3rem)] rounded-lg border border-gray-200 bg-white p-3 opacity-0 shadow-xl transition-[opacity,visibility] duration-150 group-hover:visible group-hover:opacity-100 group-has-focus-visible:visible group-has-focus-visible:opacity-100 pointer-coarse:group-focus-within:visible pointer-coarse:group-focus-within:opacity-100">
          <ul class="space-y-2">
            <li v-for="approval in approvals" :key="approval.id" class="flex items-center justify-between gap-3 text-sm">
              <span class="flex min-w-0 items-center gap-2">
                <img :src="approval.pictureUrl" alt="" class="h-5 w-5 shrink-0 rounded-full" />
                <span class="truncate">{{ approval.id === me.id ? t('emergencyAccessDialog.approvals.you', [approval.name]) : approval.name }}</span>
              </span>
              <span v-if="approval.approved" class="inline-flex shrink-0 items-center gap-1 text-xs text-green-700">
                <CheckCircleIcon class="h-4 w-4" aria-hidden="true" />
                <template v-if="approval.approvedAt"><span class="sr-only">{{ t('emergencyAccess.status.added') }}</span>{{ d(approval.approvedAt, 'short') }}</template>
                <template v-else>{{ t('emergencyAccess.status.added') }}</template>
              </span>
              <span v-else class="shrink-0 text-xs text-gray-500">{{ t('emergencyAccess.status.pending') }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div v-if="recoveryProcess.type === 'CHANGE_PERMISSIONS'" class="mt-5">
        <h4 class="text-sm font-medium text-gray-900">{{ t('emergencyAccessDialog.section.ownership') }}</h4>
        <div v-if="permissionChanges.newOwners.length > 0" class="mt-3 space-y-1">
          <span class="block text-sm font-medium text-gray-700">{{ t('emergencyAccessDialog.label.newOwners') }}</span>
          <MultiUserSelectInputGroup :selected-users="showKeptOwners ? selectedNewOwners : permissionChanges.newOwners" :on-search="noopSearch" :input-visible="false" @trust-changed="refreshTrusts">
            <button v-if="!showKeptOwners && permissionChanges.keptOwners.length > 0" type="button" :title="t('emergencyAccessDialog.hint.showKept')" class="mx-1 text-sm text-gray-500 hover:text-gray-700 hover:underline cursor-pointer rounded-sm focus:outline-hidden focus:ring-2 focus:ring-primary" @click="showKeptOwners = true">
              {{ t('emergencyAccessDialog.hint.alreadyHaveAccess', [permissionChanges.keptOwners.length]) }}
            </button>
          </MultiUserSelectInputGroup>
        </div>
        <div v-if="permissionChanges.newMembers.length > 0" class="mt-4 space-y-1">
          <span class="block text-sm font-medium text-gray-700">{{ t('emergencyAccessDialog.label.newMembers') }}</span>
          <MultiUserSelectInputGroup :selected-users="showKeptMembers ? selectedNewmembers : permissionChanges.newMembers" :on-search="noopSearch" :input-visible="false" @trust-changed="refreshTrusts">
            <button v-if="!showKeptMembers && permissionChanges.keptMembers.length > 0" type="button" :title="t('emergencyAccessDialog.hint.showKept')" class="mx-1 text-sm text-gray-500 hover:text-gray-700 hover:underline cursor-pointer rounded-sm focus:outline-hidden focus:ring-2 focus:ring-primary" @click="showKeptMembers = true">
              {{ t('emergencyAccessDialog.hint.alreadyHaveAccess', [permissionChanges.keptMembers.length]) }}
            </button>
          </MultiUserSelectInputGroup>
        </div>
        <div v-if="removedMembers.length > 0" class="mt-4 space-y-1">
          <span class="block text-sm font-medium text-gray-700">{{ t('emergencyAccess.label.removed') }}</span>
          <MultiUserSelectInputGroup :selected-users="removedMembers" :on-search="noopSearch" :input-visible="false" />
        </div>
      </div>
      <div v-if="recoveryProcess.type === 'COUNCIL_CHANGE'" class="mt-5">
        <h4 class="text-sm font-medium text-gray-900">{{ t('emergencyAccessDialog.section.councilChange') }}</h4>
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
    </template>
    <UntrustedRecipientsWarning v-if="!showSuccess && showUntrustedWarning" v-model:confirmed="confirmedUntrusted" :require-confirmation="phase !== 'complete'" class="mt-4">
      <p v-if="untrustedSelected.length > 0">{{ t('untrustedRecipientsWarning.untrustedUsers', [untrustedSelected.map(u => u.name).join(', ')]) }}</p>
      <p v-if="selectedGroups.length > 0">{{ t('emergencyAccessDialog.warning.groupRecipients', [selectedGroups.map(g => g.name).join(', ')]) }}</p>
      <p v-if="unresolvedRecipientCount > 0">{{ t('emergencyAccessDialog.warning.unresolvedRecipients', [unresolvedRecipientCount]) }}</p>
    </UntrustedRecipientsWarning>
    <UntrustedRecipientsWarning v-if="!showSuccess && untrustedGrantees.length > 0" v-model:confirmed="confirmedUntrustedGrantees" require-confirmation class="mt-4">
      <p>{{ t('emergencyAccessDialog.warning.untrustedGrantees', [untrustedGrantees.map(u => u.name).join(', ')]) }}</p>
    </UntrustedRecipientsWarning>
    <p v-if="accessProgress" role="status" aria-live="polite" class="mt-4 text-sm text-gray-500">
      {{ t('emergencyAccessDialog.hint.configuringAccess', [accessProgress.done, accessProgress.total]) }}
    </p>
    <div v-if="phase !== 'start' && !isMeInProcessCouncil" class="text-sm pt-4">
      <span class="inline-flex items-center gap-2 rounded-md bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800 text-left">
        <ExclamationCircleIcon class="h-4 w-4" aria-hidden="true" />
        {{ t('emergencyAccessDialog.hint.notInCouncil') }}
      </span>
    </div>
    <ProcessAbortDialog v-if="recoveryProcess" ref="abortDialog" :recovery-process-id="recoveryProcess.id" @confirmed="handleRecoveryAborted" />

    <template #footer>
      <button v-if="canAbort" type="button" :disabled="processing" class="rounded-sm text-sm font-medium text-red-600 hover:underline cursor-pointer focus:outline-hidden focus:ring-2 focus:ring-red-500 disabled:cursor-not-allowed disabled:opacity-50 sm:mr-auto" @click="abortDialog?.show()">
        {{ t('emergencyAccessDialog.action.abortProcess') }}
      </button>
      <button ref="closeButton" type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t(phase === 'start' ? 'common.cancel' : 'common.close') }}
      </button>
      <button v-if="primaryAction" type="submit" :disabled="processing || !primaryAction.enabled" :class="primaryButton">
        {{ primaryAction.label }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { CheckBadgeIcon, CheckCircleIcon, ExclamationCircleIcon, InformationCircleIcon } from '@heroicons/vue/20/solid';
import { base64 } from '@scure/base';
import * as R from 'remeda';
import { computed, reactive, ref, Ref, toRaw, useId, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AccessGrant, ActivatedUser, AuthorityDto, didCompleteSetup, GroupDto, MemberDto, PaymentRequiredError, RecoveredKeyShareDto, RecoveryProcessChangeCouncil, RecoveryProcessDto, RecoveryProcessSetNewOwner, SettingsDto, TrustDto, UserDto, VaultDto, VaultRole } from '../../common/backend';
import { AccessTokenProducing, asPublicKey, UserKeys } from '../../common/crypto';
import { EmergencyAccess } from '../../common/emergencyaccess';
import { ECDSA_P384, JWT, JWTHeader } from '../../common/jwt';
import userdata from '../../common/userdata';
import wot from '../../common/wot';
import { wordEncoder } from '../../common/util';
import { primaryButton, secondaryButton } from '../dialogButtons';
import HubDialog from '../HubDialog.vue';
import MultiUserSelectInputGroup from '../MultiUserSelectInputGroup.vue';
import UntrustedRecipientsWarning from '../UntrustedRecipientsWarning.vue';
import EmergencyAccessSetup from './EmergencyAccessSetup.vue';
import ProcessAbortDialog from './ProcessAbortDialog.vue';
import SegmentRing from './SegmentRing.vue';
import type { MultiUserSelectExpose } from '../MultiUserSelectInputGroup.vue';
import { VaultFormat8 } from '../../common/vaultFormat8';
import { UniversalVaultFormat } from '../../common/universalVaultFormat';

const { t, d } = useI18n({ useScope: 'global' });

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
const closeButton = ref<HTMLButtonElement>();
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
const processing = ref(false);
const onError = ref<Error>();
// shown while the membership has been replaced but the vault key has not been shared with every recipient yet:
const accessProgress = ref<{ done: number, total: number }>();

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

const permissionChanges = computed(() => {
  const existingOwnerIds = new Set(existingOwners.value.map(u => u.id));
  const existingMemberIds = new Set(existingMembers.value.map(u => u.id));
  return {
    newOwners: selectedNewOwners.value.filter(u => !existingOwnerIds.has(u.id)),
    keptOwners: selectedNewOwners.value.filter(u => existingOwnerIds.has(u.id)),
    newMembers: selectedNewmembers.value.filter(u => !existingMemberIds.has(u.id)),
    keptMembers: selectedNewmembers.value.filter(u => existingMemberIds.has(u.id))
  };
});

// kept owners/members are collapsed into a count by default; clicking it reveals them among the new ones:
const showKeptOwners = ref(false);
const showKeptMembers = ref(false);

// COUNCIL CHANGE
const councilMembers = ref<ActivatedUser[]>([]);

// WEB OF TRUST: a CHANGE_PERMISSIONS process re-wraps the recovered vault key to the selected users' public keys on
// completion, so unverified identities must be surfaced to every involved council member (start, approval, completion):
const trusts = ref<TrustDto[]>([]);
const trustLevels = ref<Map<string, number>>(); // undefined until loaded for the current recipients
const confirmedUntrusted = ref(false);
const onLoadTrustLevelsError = ref<Error>();

// COMPLETION of a CHANGE_PERMISSIONS process: the server resolves every user who will have access afterwards (including
// members of groups that were never shown in any picker) once when the dialog opens. All of them receive a freshly wrapped
// vault key, existing token holders included, so all their identities are checked and acknowledged before the Complete
// button, and exactly this state is committed:
type PendingPermissionChange = {
  membersWithRole: Record<string, VaultRole>;
  usersToGrant: (MemberDto & ActivatedUser)[];
};
const pendingPermissionChange = ref<PendingPermissionChange>(); // undefined until loaded (stays undefined if loading failed)
const granteeTrustLevels = ref<Map<string, number>>(); // undefined until computed for the current grantees and trusts
const confirmedUntrustedGrantees = ref(false);
const onLoadGranteesError = ref<Error>();

const untrustedGrantees = computed<UserDto[]>(() => {
  const levels = granteeTrustLevels.value;
  return levels ? (pendingPermissionChange.value?.usersToGrant ?? []).filter(u => (levels.get(u.id) ?? -1) === -1) : [];
});

watch([pendingPermissionChange, trusts], async () => {
  // trust levels and the confirmation only apply to the grantees and trusts they were computed for:
  const pending = pendingPermissionChange.value;
  const currentTrusts = trusts.value;
  const isCurrent = () => pending === pendingPermissionChange.value && currentTrusts === trusts.value;
  granteeTrustLevels.value = undefined;
  confirmedUntrustedGrantees.value = false;
  if (!pending) {
    return;
  }
  try {
    const levels = await wot.computeTrustLevels(pending.usersToGrant, currentTrusts);
    if (isCurrent()) {
      granteeTrustLevels.value = levels;
    }
  } catch (error) {
    console.error('Computing trust levels of the grantees failed.', error);
    if (isCurrent()) {
      onLoadGranteesError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
});

// a selected user's identity has been signed via their badge:
async function refreshTrusts() {
  trusts.value = await backend.trust.listTrusted();
}

// before completion the server-resolved grantee list is unknown, so the recipients named in the process stand in for it.
// When starting, every selected user is shown (the starter may want to remove unverified ones who would keep their access);
// when approving, kept owners/members are left out, as they keep their existing token and will not be wrapped for:
const grantRelevantAuthorities = computed<AuthorityDto[]>(() => {
  if ((props.recoveryProcess?.type ?? processType.value) !== 'CHANGE_PERMISSIONS' || phase.value === 'complete') {
    return [];
  }
  const selected = [...selectedNewOwners.value, ...selectedNewmembers.value];
  if (phase.value === 'start') {
    return selected;
  }
  const existingIds = new Set([...existingOwners.value, ...existingMembers.value].map(a => a.id));
  return selected.filter(a => !existingIds.has(a.id));
});

watch([grantRelevantAuthorities, trusts], async () => {
  // trust levels and the confirmation only apply to the recipients they were made for:
  const authorities = grantRelevantAuthorities.value;
  const currentTrusts = trusts.value;
  const isCurrent = () => authorities === grantRelevantAuthorities.value && currentTrusts === trusts.value;
  trustLevels.value = undefined;
  confirmedUntrusted.value = false;
  onLoadTrustLevelsError.value = undefined;
  try {
    const levels = await wot.computeTrustLevels(authorities.filter(a => a.type === 'USER') as UserDto[], currentTrusts);
    if (isCurrent()) {
      trustLevels.value = levels;
    }
  } catch (error) {
    console.error('Computing trust levels failed.', error);
    if (isCurrent()) {
      onLoadTrustLevelsError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
});

// recipients of the signed process the server did not resolve (deleted, or withheld) are missing from the changes, so they are called out in the warning:
const unresolvedRecipientCount = computed(() => {
  if (props.recoveryProcess?.type !== 'CHANGE_PERMISSIONS') {
    return 0;
  }
  const shownIds = new Set([...selectedNewOwners.value, ...selectedNewmembers.value].map(a => a.id));
  return new Set([...props.recoveryProcess.details.newOwnerIds, ...props.recoveryProcess.details.newMemberIds].filter(id => !shownIds.has(id))).size;
});

// only users who will actually receive a key (i.e. have an ecdhPublicKey) are relevant for the warning:
const untrustedSelected = computed(() => grantRelevantAuthorities.value.filter((a): a is UserDto => a.type === 'USER' && !!(a as UserDto).ecdhPublicKey && !!trustLevels.value && (trustLevels.value.get(a.id) ?? -1) === -1));

// members of selected groups cannot be individually verified before completion:
const selectedGroups = computed(() => grantRelevantAuthorities.value.filter(a => a.type === 'GROUP'));

const showUntrustedWarning = computed(() => !!trustLevels.value && (untrustedSelected.value.length > 0 || selectedGroups.value.length > 0 || unresolvedRecipientCount.value > 0));

const needsUntrustedConfirmation = computed(() => !trustLevels.value || (showUntrustedWarning.value && !confirmedUntrusted.value));

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

const canAbort = computed(() => !showSuccess.value && phase.value !== 'start' && (isMeInProcessCouncil.value || isMeInCouncil.value));

function handleRecoveryAborted() {
  emit('updated');
  open.value = false;
}

const initialFocus = computed(() => phase.value === 'start' && processType.value === 'CHANGE_PERMISSIONS' ? ownersSelect.value?.focusTarget : closeButton.value);

const errorMessage = computed(() => {
  if (onError.value instanceof PaymentRequiredError) {
    return t('vaultDetails.error.licenseViolated');
  } else if (onError.value) {
    return t('common.unexpectedError', [onError.value.message]);
  } else if (phase.value === 'start' && conflictingProcessExists.value) {
    return t('recoveryDialog.error.processAlreadyExists');
  } else if (onLoadTrustLevelsError.value) {
    return t('common.unexpectedError', [onLoadTrustLevelsError.value.message]);
  } else if (onLoadGranteesError.value) {
    return t('common.unexpectedError', [onLoadGranteesError.value.message]);
  } else {
    return undefined;
  }
});

const primaryAction = computed(() => {
  if (showSuccess.value) {
    return undefined;
  } else if (phase.value === 'start') {
    return { label: t('emergencyAccessDialog.action.start'), enabled: canStartRecovery.value && !needsUntrustedConfirmation.value, run: startRecovery };
  } else if (canSeeApprove.value) {
    return { label: t('emergencyAccessDialog.action.approve'), enabled: !needsUntrustedConfirmation.value, run: approveRecovery };
  } else if (canSeeComplete.value) {
    const granteesResolved = props.recoveryProcess?.type !== 'CHANGE_PERMISSIONS' || !!granteeTrustLevels.value;
    return { label: t('emergencyAccessDialog.action.completeProcess'), enabled: granteesResolved && (untrustedGrantees.value.length === 0 || confirmedUntrustedGrantees.value), run: completeRecovery };
  } else {
    return undefined;
  }
});

async function submit() {
  const action = primaryAction.value;
  if (processing.value || !open.value || !action?.enabled) {
    return;
  }
  try {
    processing.value = true;
    await action.run();
  } finally {
    processing.value = false;
  }
}

async function show() {
  await loadExistingProcessesForVault();
  initProcessType();
  await initOwnersAndMembers();
  await loadAuthoritiesForCouncilAndProcesses();
  await initProcessSpecificState();
  const [, trustList] = await Promise.all([loadProcessInfos(), backend.trust.listTrusted()]);
  trusts.value = trustList;
  await loadPendingPermissionChange();
  showKeptOwners.value = false;
  showKeptMembers.value = false;
  showSuccess.value = false;
  open.value = true;
}

async function loadPendingPermissionChange() {
  pendingPermissionChange.value = undefined;
  onLoadGranteesError.value = undefined;
  if (props.recoveryProcess?.type !== 'CHANGE_PERMISSIONS' || !canSeeComplete.value) {
    return;
  }
  try {
    const membersWithRole = Object.fromEntries([
      ...selectedNewmembers.value.map(u => [u.id, 'MEMBER']),
      ...selectedNewOwners.value.map(u => [u.id, 'OWNER'])
    ]) as Record<string, VaultRole>;
    // nothing is persisted by the preview:
    const usersToGrant = (await backend.vaults.previewEffectiveUsers(props.vault.id, membersWithRole)).filter(didCompleteSetup);
    pendingPermissionChange.value = { membersWithRole: membersWithRole, usersToGrant: usersToGrant }; // trust levels follow via the watch above
  } catch (error) {
    console.error('Resolving the recipients of the permission change failed.', error);
    onLoadGranteesError.value = error instanceof Error ? error : new Error('Unknown Error');
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

    const processId = crypto.randomUUID();
    const processKeyPair = await EmergencyAccess.startRecovery(processId, councilMembers);
    const process: RecoveryProcessDto = {
      id: processId,
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
  onError.value = undefined;
  try {
    if (!props.recoveryProcess) {
      throw new Error(t('emergencyAccessDialog.error.noProcessToApprove'));
    }
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
/**
 * A CHANGE_PERMISSIONS completion ready to be committed: the recovered vault key plus the membership and grantees exactly as
 * shown to (and acknowledged by) the user when the dialog was opened. Nothing is reloaded from the server at this point.
 */
type PreparedPermissionChange = {
  processId: string;
  vaultKeys: AccessTokenProducing;
  membersWithRole: Record<string, VaultRole>;
  usersToGrant: (MemberDto & ActivatedUser)[];
};

async function completeRecovery() {
  onError.value = undefined;
  try {
    if (!props.recoveryProcess) {
      throw new Error(t('emergencyAccessDialog.error.noProcessToComplete'));
    }
    const verifiedProcess = await verifyProcessInfo(props.recoveryProcess);
    if (!verifiedProcess) {
      throw new Error(t('emergencyAccessDialog.error.processTampered'));
    }

    const userKeys = await userdata.decryptUserKeysWithBrowser();

    const process = structuredClone(toRaw(props.recoveryProcess));
    process.recoveredKeyShares[props.me.id] = await addMyShare(process, userKeys);

    const keyShares = Object.values(process.recoveredKeyShares).filter(p => p.recoveredKeyShare !== undefined).map(p => p.recoveredKeyShare!);

    const processPrivateKey = process.recoveredKeyShares[props.me.id].processPrivateKey;
    const recoveredKeyBytes = await EmergencyAccess.combineRecoveredShares(keyShares, processPrivateKey, { vaultId: props.vault.id, processId: process.id }, props.me.id, userKeys.ecdhKeyPair.privateKey);
    const recoveredKey = wordEncoder.encodePadded(recoveredKeyBytes);

    if (process.type === 'COUNCIL_CHANGE') {
      if (councilMembers.value.length < process.details.newRequiredKeyShares) {
        throw new Error(t('emergencyAccessDialog.error.insufficientCouncilMembers', [councilMembers.value.length, process.details.newRequiredKeyShares]));
      }
      const keyShares = await EmergencyAccess.split(props.vault.id, recoveredKeyBytes, process.details.newRequiredKeyShares, ...councilMembers.value);
      await backend.vaults.createOrUpdateVault({
        ...props.vault,
        requiredEmergencyKeyShares: process.details.newRequiredKeyShares,
        emergencyKeyShares: keyShares
      });
      await backend.emergencyAccess.complete(process.id);
      emit('updated');
      showSuccess.value = true;
    } else if (process.type === 'CHANGE_PERMISSIONS') {
      const vaultKeys: AccessTokenProducing = props.vault.uvfMetadataFile
        ? await UniversalVaultFormat.recover(props.vault.uvfMetadataFile, recoveredKey)
        : await VaultFormat8.recover(recoveredKey);

      const pending = pendingPermissionChange.value;
      if (!pending) {
        throw new Error('Recipients of the permission change have not been resolved.'); // unreachable: the button is disabled until they are
      }
      await commitPermissionChange({ processId: process.id, vaultKeys: vaultKeys, membersWithRole: pending.membersWithRole, usersToGrant: pending.usersToGrant });
    }
  } catch (error) {
    console.error('Completing emergency recovery failed.', error);
    onError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

// replaces the membership, shares the vault key with exactly the prepared recipients and completes the process:
async function commitPermissionChange(prepared: PreparedPermissionChange) {
  // the process may have been aborted (e.g. by another council member) while this dialog was open;
  // the membership endpoint does not know about the process, so make sure it still exists before replacing anything:
  const processes = await backend.emergencyAccess.findProcessesForVault(props.vault.id);
  if (!processes.some(p => p.id === prepared.processId)) {
    throw new Error(t('emergencyAccessDialog.error.processNoLongerExists'));
  }
  // from here on the vault is in an intermediate state until every recipient holds a key, so the dialog stays blocked:
  const progress = reactive({ done: 0, total: prepared.usersToGrant.length });
  accessProgress.value = progress;
  try {
    await backend.vaults.setMembersWithRole(props.vault.id, prepared.membersWithRole);

    // existing tokens are replaced; anyone who joined a selected group after the dialog was opened stays pending for a later (manual or automatic) grant:
    const accessGrants: AccessGrant[] = await Promise.all(
      prepared.usersToGrant.map(async u => {
        // never wrap "to myself" using a key from the (server-supplied) recipient list, only using my verified key:
        const publicKey = u.id === props.me.id ? await userdata.ecdhPublicKey : base64.decode(u.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
        const jwe = await prepared.vaultKeys.encryptForUser(publicKey, u.vaultRole === 'OWNER');
        progress.done++;
        return { userId: u.id, token: jwe };
      })
    );
    if (accessGrants.length > 0) {
      await backend.vaults.grantAccess(props.vault.id, ...accessGrants);
    }

    await backend.emergencyAccess.complete(prepared.processId);
    emit('updated');
    showSuccess.value = true;
  } finally {
    accessProgress.value = undefined;
  }
}

type SignedProcessInfoPayload = Pick<RecoveryProcessDto, 'type' | 'details' | 'vaultId' | 'processPublicKey' | 'requiredKeyShares'> & {
  iss: string;
  sub: string;
  iat: number;
};

async function addMyShare(process: RecoveryProcessDto, userKeys: UserKeys): Promise<RecoveredKeyShareDto> {
  // the per-process copy is a consistent snapshot of one Shamir split, even if the vault's canonical shares rotate
  // while this process is pending; its `ctx` binding guarantees it is a genuine share for this vault and member:
  const encryptedShare = process.recoveredKeyShares[props.me.id].unrecoveredKeyShare;
  const recoveredShare = await EmergencyAccess.recoverShare(encryptedShare, { vaultId: props.vault.id, processId: process.id }, props.me.id, userKeys.ecdhKeyPair.privateKey, process.processPublicKey);

  const processInfo = R.pick(process, ['type', 'details', 'vaultId', 'processPublicKey', 'requiredKeyShares']);
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

async function verifyProcessInfo(process: RecoveryProcessDto): Promise<Record<string, SignedProcessInfoPayload> | undefined> {
  if (process.vaultId !== props.vault.id) {
    console.error(`Recovery process ${process.id} does not belong to vault ${props.vault.id}.`);
    return undefined;
  }
  const councilMemberIds = Object.keys(process.recoveredKeyShares);
  const authorities = await backend.authorities.listSome(councilMemberIds);
  const councilMembers = R.indexBy(
    authorities.filter(a => a.type === 'USER').filter(u => didCompleteSetup(u)),
    u => u.id
  );

  const verifiedPayloads: Record<string, SignedProcessInfoPayload> = {};
  for (const [councilMemberId, recoveredKeyShare] of Object.entries(process.recoveredKeyShares)) {
    if (!recoveredKeyShare.recoveredKeyShare) {
      continue;
    } else if (!recoveredKeyShare.signedProcessInfo) {
      console.error(`Missing signed process info for council member ${councilMemberId}.`);
      return undefined;
    } else if (!councilMembers[councilMemberId]) {
      console.error(`Unknown council member ${councilMemberId}.`);
      return undefined;
    }
    const councilMember = councilMembers[councilMemberId];
    const publicKey = await asPublicKey(base64.decode(councilMember.ecdsaPublicKey) as Uint8Array<ArrayBuffer>, ECDSA_P384, ['verify']);
    const [_header, payload] = await JWT.parse(recoveredKeyShare.signedProcessInfo, publicKey) as [JWTHeader, SignedProcessInfoPayload];
    if (payload.sub !== process.id || payload.iss !== councilMemberId) {
      console.error(`Invalid signed process info for council member ${councilMemberId}.`);
      return undefined;
    }
    if (payload.type !== process.type || !R.isDeepEqual(payload.details, process.details)) {
      console.error(`Signed process info for council member ${councilMemberId} does not match the recovery process`, process, payload);
      return undefined;
    }
    if (payload.vaultId !== process.vaultId || payload.processPublicKey !== process.processPublicKey || payload.requiredKeyShares !== process.requiredKeyShares) {
      console.error(`Signed process info for council member ${councilMemberId} does not match the recovery process`, process, payload);
      return undefined;
    }
    verifiedPayloads[councilMemberId] = payload;
  }
  return verifiedPayloads;
}

// filled from verified signatures only:
const processInfos = ref<Record<string, SignedProcessInfoPayload>>({});
const approvalsTooltipId = useId();

async function loadProcessInfos() {
  processInfos.value = {};
  if (!props.recoveryProcess) {
    return;
  }
  try {
    processInfos.value = (await verifyProcessInfo(props.recoveryProcess)) ?? {};
  } catch (error) {
    console.error('Verifying approvals failed.', error);
  }
}

const approvals = computed(() => processCouncilIds.value
  .map(id => ({
    id: id,
    name: authoritiesById.value[id]?.name ?? id,
    pictureUrl: authoritiesById.value[id]?.pictureUrl,
    approved: props.recoveryProcess?.recoveredKeyShares[id]?.recoveredKeyShare !== undefined,
    approvedAt: processInfos.value[id] && toDate(processInfos.value[id].iat)
  }))
  .sort((a, b) => a.name.localeCompare(b.name)));

const starter = computed(() => {
  const payloads = Object.values(processInfos.value).filter(p => toDate(p.iat));
  if (payloads.length === 0) {
    return undefined;
  }
  const first = payloads.reduce((a, b) => (b.iat < a.iat ? b : a));
  return { name: authoritiesById.value[first.iss]?.name ?? first.iss, startedAt: toDate(first.iat)! };
});

// iat is claimed by the signer, so it may be missing or out of range:
function toDate(iat: unknown): Date | undefined {
  const date = new Date(typeof iat === 'number' ? iat * 1000 : NaN);
  return Number.isNaN(date.getTime()) ? undefined : date;
}

async function loadActivatedUsers(ids: string[]): Promise<ActivatedUser[]> {
  const authorities = await backend.authorities.listSome(ids);
  return authorities
    .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
    .sort((a, b) => a.name.localeCompare(b.name));
}
</script>
