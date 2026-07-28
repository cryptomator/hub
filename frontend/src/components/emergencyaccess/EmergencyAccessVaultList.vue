<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>
  <div v-else-if="onFetchError == null">
    <LicenseAlert v-if="licenseStatus" :is-admin="isAdmin" :license-status="licenseStatus" />

    <ContentBanner v-if="entitlements.emergencyAccessEnabled && entitlements.showTrialHint" type="info" :title="t('trial.enterpriseFeature.title')" class="mb-6">
      {{ t('trial.enterpriseFeature.description') }} <!-- TODO: link to feature comparison? -->
    </ContentBanner>

    <div v-if="!entitlements.emergencyAccessEnabled" class="flex flex-col justify-center items-center text-center">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
        <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('auditLog.paymentRequired.message') }}</h3>
      <p class="mt-1 text-sm text-gray-500">
        {{ t('emergencyAccess.licenseRequired.message') }}
        <span v-if="isAdmin"> {{ t('emergencyAccess.licenseRequired.adminHint') }}</span>
      </p>
      <router-link to="/app/admin/settings" :hidden="!isAdmin" class="inline-flex items-center px-4 py-2 border border-transparent shadow-xs text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary mt-6">
        <WrenchIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
        {{ t('auditLog.paymentRequired.openAdminSection') }}
      </router-link>
    </div>
    <div v-else-if="!settings?.enableEmergencyAccess" class="mt-3 text-center">
      <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('emergencyAccess.empty.disabled') }}</h3>
    </div>
    <section v-else>
      <!-- entitlements.emergencyAccessEnabled && settings.enableEmergencyAccess -->
      <header class="pb-5 border-b border-gray-200">
        <div class="flex flex-col sm:flex-row sm:justify-between gap-3 w-full">
          <h2 class="text-2xl font-bold leading-9 text-gray-900 sm:text-3xl sm:truncate">
            {{ t('nav.emergencyAccess') }}
          </h2>
          <div class="flex gap-3">
            <button class="w-full bg-primary py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="fetchData()">
              {{ t('common.refresh') }}
            </button>
          </div>
        </div>
        <div class="mt-3 flex flex-wrap sm:flex-nowrap gap-3 items-center whitespace-nowrap">
          <input id="vaultSearch" v-model="query" :placeholder="t('vaultList.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />

          <Listbox v-model="selectedFilter" as="div">
            <div class="relative w-auto whitespace-nowrap">
              <ListboxButton class="min-w-60 relative w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-xs focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary text-sm">
                <span class="block whitespace-nowrap">{{ filterOptions[selectedFilter] }}</span>
                <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                  <ChevronUpDownIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
                </span>
              </ListboxButton>
              <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
                <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black/5 focus:outline-hidden text-sm">
                  <ListboxOption v-for="(name, key) in filterOptions" :key="key" v-slot="{ active, selected }" :value="key" class="relative cursor-default select-none py-2 pl-3 pr-12 ui-not-active:text-gray-900 ui-active:text-white ui-active:bg-primary">
                    <span :class="[selected ? 'font-semibold' : 'font-normal', 'block whitespace-nowrap']">{{ name }}</span>
                    <span v-if="selected" :class="[active ? 'text-white' : 'text-primary', 'absolute inset-y-0 right-0 flex items-center pr-4']">
                      <CheckIcon class="h-5 w-5" aria-hidden="true" />
                    </span>
                  </ListboxOption>
                </ListboxOptions>
              </transition>
            </div>
          </Listbox>
        </div>
      </header>

      <div v-if="filteredVaults.length === 0" class="mt-3 text-center">
        <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('emergencyAccess.empty.noneFound') }}</h3>
      </div>
      <ul role="list" class="mt-5 divide-y divide-gray-200 bg-white shadow-sm rounded-md">
        <li v-for="(vault, index) in filteredVaults" :key="vault.id">
          <details
            class="group/vault hover:bg-gray-50"
            :class="{ 'rounded-t-md': index == 0, 'rounded-b-md': index == filteredVaults.length - 1 }"
            @toggle="onVaultDetailsToggle(vault.id, $event)"
          >
            <summary
              class="list-none px-4 py-4 sm:px-6  cursor-pointer [&::-webkit-details-marker]:hidden"
              :class="{ 'rounded-t-md': index == 0, 'rounded-b-md': index == filteredVaults.length - 1 }"
            >
              <div class="flex flex-wrap gap-3 sm:flex-nowrap sm:items-center sm:justify-between">
                <!-- Name and description -->
                <div class="flex-1 min-w-40">
                  <div class="flex items-center gap-3 min-w-0">
                    <p class="truncate text-sm font-medium text-primary min-w-0">
                      {{ vault.name }}
                    </p>
                    <div v-if="vault.archived" class="inline-flex items-center rounded-md bg-yellow-400/10 px-2 py-1 text-xs font-medium text-yellow-500 ring-1 ring-inset ring-yellow-400/20">{{ t('vaultList.badge.archived') }}</div>
                  </div>
                  <p
                    v-if="vault.description"
                    class="truncate text-sm text-gray-500 mt-2 min-w-0"
                  >
                    {{ vault.description }}
                  </p>
                </div>

                <div class="flex flex-wrap items-center gap-2 sm:justify-end" @click.stop>
                  <EmergencyBadge
                    v-if="isBroken(vault)"
                    type="error"
                    :title="t('emergencyAccess.badge.broken.title')"
                    :message="t('emergencyAccess.badge.broken.message')"
                  />
                  <EmergencyBadge
                    v-else-if="settings && settings.defaultMinMembers > emergencyAccessMembers(vault).length"
                    type="warning"
                    :title="t('emergencyAccess.badge.insufficientCouncilMembers.title')"
                    :message="t('emergencyAccess.badge.insufficientCouncilMembers.message', [settings.defaultMinMembers])"
                  />
                  <EmergencyBadge
                    v-else-if="vault.requiredEmergencyKeyShares === emergencyAccessMembers(vault).length"
                    type="warning"
                    :title="t('emergencyAccess.badge.noRedundancy.title')"
                    :message="t('emergencyAccess.badge.noRedundancy.message')"
                  />
                </div>
                <div class="flex items-center gap-2">
                  <span
                    v-if="getProcesses(vault.id).length > 0"
                    class="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700"
                  >
                    {{ getProcesses(vault.id).length }}
                  </span>
                  <ChevronDownIcon class="h-5 w-5 text-gray-400 transition-transform duration-200 group-open/vault:rotate-180" aria-hidden="true" />
                </div>
              </div>
            </summary>
            <div class="px-4 py-4 sm:px-6 text-sm text-gray-500">
              <div class="flex flex-col gap-4 md:flex-row">
                <!-- EMERGENCY ACCESS COUNCIL -->
                <section class="flex flex-1 min-w-0 flex-col">
                  <h4 class="mb-2 font-semibold text-gray-700">{{ t('emergencyAccess.vaultCouncil') }}</h4>
                  <ul class="max-h-56 overflow-auto pr-1 mb-1">
                    <li
                      v-for="member in getCurrentCouncilMembers(vault)"
                      :key="member.id"
                      class="flex items-center gap-2 h-6"
                    >
                      <img
                        v-if="member.pictureUrl"
                        :src="member.pictureUrl"
                        class="h-4 w-4 rounded-full"
                      />
                      {{ member.name }}
                    </li>
                  </ul>
                  <div class="mb-3">
                    {{ t('emergencyAccess.requiredKeyShares') }}: {{ vault.requiredEmergencyKeyShares }}
                  </div>
                  <div class="mt-auto pt-2 flex flex-wrap items-center gap-2">
                    <EmergencyBadge
                      v-if="!isEmergencyKeyShareHolder(vault)"
                      type="warning"
                      :title="t('emergencyAccess.badge.notCouncil.title')"
                      :message="t('emergencyAccess.badge.notCouncil.message')"
                    />
                    <EmergencyProcessButton
                      v-if="getProcessByType(vault, 'COUNCIL_CHANGE')"
                      :label="getTypeLabel(vault, 'COUNCIL_CHANGE')"
                      :approval-label="getApprovalLabel(getProcessByType(vault, 'COUNCIL_CHANGE')!)"
                      :disabled="isBroken(vault)"
                      :has-process="true"
                      :can-start="false"
                      :required-key-shares="getProcessByType(vault, 'COUNCIL_CHANGE')!.requiredKeyShares"
                      :completed-key-shares="getCompletedSegmentsForProcess(getProcessByType(vault, 'COUNCIL_CHANGE')!)"
                      :council-members="getCouncilMembersForProcess(getProcessByType(vault, 'COUNCIL_CHANGE')!)"
                      :recovered-member-ids="Array.from(recoveredMemberIdsForProcess(getProcessByType(vault, 'COUNCIL_CHANGE')!))"
                      @click-main="onUnifiedButtonClick(vault, 'COUNCIL_CHANGE')"
                    />
                    <EmergencyProcessButton
                      v-else
                      :label="getTypeLabel(vault, 'COUNCIL_CHANGE')"
                      :disabled="isBroken(vault) || !isEmergencyKeyShareHolder(vault)"
                      :has-process="false"
                      :can-start="true"
                      @click-main="onUnifiedButtonClick(vault, 'COUNCIL_CHANGE')"
                    />
                  </div>
                </section>

                <!-- VAULT MEMBERS -->
                <section class="flex flex-1 min-w-0 flex-col">
                  <h4 class="mb-2 font-semibold text-gray-700">Vault Access</h4>
                  <div class="grid grid-cols-[max-content_minmax(0,1fr)] gap-x-3 gap-y-3 items-center mb-3">
                    <div class="text-xs font-medium uppercase tracking-wide text-gray-500 whitespace-nowrap">{{ t('emergencyAccess.label.owners') }}:</div>
                    <UserListGroupVisualization :authorities="getVaultMembers(vault.id).filter(member => member.vaultRole === 'OWNER')" :max="8" />
                      
                    <div class="text-xs font-medium uppercase tracking-wide text-gray-500 whitespace-nowrap">{{ t('emergencyAccess.label.members') }}:</div>
                    <UserListGroupVisualization :authorities="getVaultMembers(vault.id).filter(member => member.vaultRole === 'MEMBER')" :max="8" />
                  </div>
                  <div class="mt-auto pt-2 flex flex-wrap items-center gap-2">
                    <EmergencyProcessButton
                      v-if="getProcessByType(vault, 'CHANGE_PERMISSIONS')"
                      :label="getTypeLabel(vault, 'CHANGE_PERMISSIONS')"
                      :approval-label="getApprovalLabel(getProcessByType(vault, 'CHANGE_PERMISSIONS')!)"
                      :disabled="isBroken(vault)"
                      :has-process="true"
                      :can-start="false"
                      :required-key-shares="getProcessByType(vault, 'CHANGE_PERMISSIONS')!.requiredKeyShares"
                      :completed-key-shares="getCompletedSegmentsForProcess(getProcessByType(vault, 'CHANGE_PERMISSIONS')!)"
                      :council-members="getCouncilMembersForProcess(getProcessByType(vault, 'CHANGE_PERMISSIONS')!)"
                      :recovered-member-ids="Array.from(recoveredMemberIdsForProcess(getProcessByType(vault, 'CHANGE_PERMISSIONS')!))"
                      @click-main="onUnifiedButtonClick(vault, 'CHANGE_PERMISSIONS')"
                    />
                    <EmergencyProcessButton
                      v-else
                      :label="getTypeLabel(vault, 'CHANGE_PERMISSIONS')"
                      :disabled="isBroken(vault) || !isEmergencyKeyShareHolder(vault)"
                      :has-process="false"
                      :can-start="true"
                      @click-main="onUnifiedButtonClick(vault, 'CHANGE_PERMISSIONS')"
                    />
                  </div>
                </section>
              </div>
            </div>
          </details>
        </li>
      </ul>
    </section>
  </div>
  <div v-else>
    <FetchError :error="onFetchError" :retry="fetchData" />
  </div>
  <EmergencyAccessDialog
    v-if="recoveryApprovVault && settings"
    ref="recoveryApprovDialog"
    :settings="settings"
    :vault="recoveryApprovVault"
    :me="me!"
    :recovery-process="selectedProcess"
    :start-type="startType"
    @updated="fetchData"
    @close="recoveryApprovVault = undefined"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import * as R from 'remeda';
import auth from '../../common/auth';
import backend, { LicenseUserInfoDto, VaultDto, RecoveryProcessDto, MemberDto, SettingsDto } from '../../common/backend';
import FetchError from '../FetchError.vue';
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import LicenseAlert from '../LicenseAlert.vue';
import ContentBanner from '../ContentBanner.vue';
import { CheckIcon, ChevronDownIcon, ChevronUpDownIcon, WrenchIcon } from '@heroicons/vue/24/solid';
import userdata from '../../common/userdata';
import { UserDto } from '../../common/backend';
import config from '../../common/config';
import EmergencyAccessDialog from './EmergencyAccessDialog.vue';
import EmergencyBadge from './EmergencyBadge.vue';
import EmergencyProcessButton from './EmergencyProcessButton.vue';
import UserListGroupVisualization from '../UserListGroupVisualization.vue';

const SUPPORTED_PROCESS_TYPES = ['CHANGE_PERMISSIONS', 'COUNCIL_CHANGE'] as const;

const { t } = useI18n({ useScope: 'global' });
const me = ref<UserDto>();
const query = ref('');
const vaults = ref<VaultDto[]>([]);
const loading = ref(true);
const onFetchError = ref<Error>();

const isAdmin = ref<boolean>(false);

const entitlements = config.get().entitlements;
const licenseStatus = ref<LicenseUserInfoDto>();
const settings = ref<SettingsDto>();

const selectedFilter = ref<'recoverableVaults' | 'approved' | 'approvable' | 'startable'>('recoverableVaults');
const filterOptions = computed(() => ({
  recoverableVaults: t('emergencyAccess.filter.all'),
  approvable: t('emergencyAccess.filter.approvable'),
  approved: t('emergencyAccess.filter.approved'),
  startable: t('emergencyAccess.filter.startable'),
}));
const selectedProcess = ref<RecoveryProcessDto>();
const filteredVaults = computed<VaultDto[]>(() => filterVaults(vaults.value));
const vaultRecoveryProcesses = ref<Record<string, RecoveryProcessDto[]>>({});
const startType = ref<RecoveryProcessDto['type']>('CHANGE_PERMISSIONS');
const recoveryApprovVault = ref<VaultDto>();
const recoveryApprovDialog = ref<typeof EmergencyAccessDialog>();
const usersById = ref<Record<string, UserDto>>({});
const membersByVaultId = ref<Record<string, MemberDto[]>>({});
const membersLoadingByVaultId = ref<Record<string, boolean>>({});

onMounted(fetchData);

async function fetchData() {
  loading.value = true;
  onFetchError.value = undefined;
  membersByVaultId.value = {};
  membersLoadingByVaultId.value = {};
  try {
    me.value = await userdata.me;
    isAdmin.value = (await auth).hasRole('admin');
    licenseStatus.value = await backend.license.getUserInfo();
    settings.value = await backend.settings.get();

    if (entitlements.emergencyAccessEnabled && settings.value.enableEmergencyAccess){
      const [fetchedVaults, allProcesses] = await Promise.all([
        backend.vaults.listRecoverable(),
        backend.emergencyAccess.findAllProcesses(),
      ]);
      vaults.value = fetchedVaults.sort((a, b) => a.name.localeCompare(b.name));

      const processesByVaultId = R.groupBy(allProcesses, p => p.vaultId);
      vaultRecoveryProcesses.value = R.pullObject(vaults.value, v => v.id, v => processesByVaultId[v.id] ?? []);

      const memberIdsOfAllRunningProcesses = Object.values(vaultRecoveryProcesses.value)
        .flat()
        .flatMap(p => Object.keys(p.recoveredKeyShares));
      const memberIds = vaults.value
        .flatMap(v => Object.keys(v.emergencyKeyShares));
      const combinedMemberIds = R.unique([...memberIdsOfAllRunningProcesses, ...memberIds]);

      if (combinedMemberIds.length > 0) {
        const allUsers = (await backend.authorities.listSome(combinedMemberIds)).filter(a => a.type === 'USER');
        usersById.value = R.indexBy(allUsers, u => u.id);
      } else {
        usersById.value = {};
      }
    }
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    loading.value = false;
  }
}

function recoveredMemberIdsForProcess(proc: RecoveryProcessDto): Set<string> {
  const set = new Set<string>();
  if (!proc?.recoveredKeyShares) return set;
  for (const [id, ks] of Object.entries(proc.recoveredKeyShares)) {
    if (ks?.recoveredKeyShare) set.add(id);
  }
  return set;
}

function didAddMyShare(proc?: RecoveryProcessDto): boolean {
  if (!proc || !me.value) return false;
  return proc.recoveredKeyShares?.[me.value.id]?.recoveredKeyShare !== undefined;
}

function isProcessFullyApproved(proc?: RecoveryProcessDto): boolean {
  if (!proc) return false;
  const recovered = Object.values(proc.recoveredKeyShares ?? {})
    .filter(ks => ks?.recoveredKeyShare !== undefined).length;
  return recovered >= proc.requiredKeyShares;
}

function isProcessAboutToComplete(proc?: RecoveryProcessDto): boolean {
  if (!proc) return false;
  const recovered = Object.values(proc.recoveredKeyShares ?? {})
    .filter(ks => ks?.recoveredKeyShare !== undefined).length;
  return (recovered + 1 ) >= proc.requiredKeyShares;
}

function getApprovalLabel(proc?: RecoveryProcessDto): string {
  if (!proc) return '';

  if (didAddMyShare(proc))
    return t('emergencyAccess.approval.waitingOthers');
  else if (!isUserInProcess(proc))
    return t('emergencyAccess.approval.showDetails');
  else {
    if (isProcessFullyApproved(proc) || isProcessAboutToComplete(proc)) {
      return t('emergencyAccess.approval.completeNow');
    }
    else 
      return t('emergencyAccess.approval.approveNow');
  }
}

function getProcessByType(vault: VaultDto, type: RecoveryProcessDto['type']): RecoveryProcessDto | undefined {
  return getProcesses(vault.id).find(p => p.type === type);
}

function getTypeLabel(vault: VaultDto, type: RecoveryProcessDto['type']) {
  return type === 'CHANGE_PERMISSIONS'
    ? t('emergencyAccess.processType.changePermissions')
    : t('emergencyAccess.processType.changeCouncil');
}

function onUnifiedButtonClick(vault: VaultDto, type: RecoveryProcessDto['type']) {
  const proc = getProcessByType(vault, type);
  if (proc) {
    openRecoveryDialog(vault, proc);
  } else {
    openRecoveryStartDialog(vault, type);
  }
}

function getCurrentCouncilMembers(vault: VaultDto): UserDto[] {
  const ids = Object.keys(vault.emergencyKeyShares);
  return ids.map(id => usersById.value[id]).filter(u => u !== undefined).sort((a, b) => a.name.localeCompare(b.name));
}

function filterVaults(vaults: VaultDto[]): VaultDto[] {
  if (loading.value) return [];
  const filteredByStatus = vaults.filter(filterByStatus);
  if (query.value !== '') {
    return filteredByStatus.filter((vault) =>
      vault.name.toLowerCase().includes(query.value.toLowerCase())
    );
  } else {
    return filteredByStatus;
  }
}

function filterByStatus(vault: VaultDto): boolean {
  const processes = getProcesses(vault.id);
  switch (selectedFilter.value) {
    case 'approved': // find vaults where there is at least one process to which the user has already submitted their key share
      return processes.some(p => hasSubmittedEmergencyKeyShare(p));
    case 'approvable': // find vaults where there there is at least one process to which the user has not yet submitted their key share
      return processes.length > 0 && processes.some(p => !hasSubmittedEmergencyKeyShare(p));
    case 'startable': { // find vaults where at least one type of process has not yet been started
      const processTypes = processes.map(p => p.type);
      return !SUPPORTED_PROCESS_TYPES.every(t => processTypes.includes(t));
    }
    case 'recoverableVaults': // all
    default:
      return true;
  }
}

function openRecoveryDialog(vault: VaultDto, proc: RecoveryProcessDto) {
  recoveryApprovVault.value = vault;
  selectedProcess.value = proc;
  nextTick(() => recoveryApprovDialog.value?.show());
}

function isUserInProcess(proc: RecoveryProcessDto): boolean {
  const councilMemberIds = Object.keys(proc.recoveredKeyShares);
  return councilMemberIds.includes(me.value?.id ?? '');
}

function getCouncilMembersForProcess(proc: RecoveryProcessDto): UserDto[] {
  return Object.keys(proc.recoveredKeyShares).map(id => usersById.value[id]);
}

function hasSubmittedEmergencyKeyShare(proc: RecoveryProcessDto): boolean {
  if (!me.value || !proc?.recoveredKeyShares) return false;
  return proc.recoveredKeyShares[me.value.id]?.recoveredKeyShare !== undefined;
}

function isEmergencyKeyShareHolder(vault: VaultDto): boolean {
  if (!vault || !me.value) return false;
  return vault.emergencyKeyShares[me.value.id] !== undefined;
}

function emergencyAccessMembers(vault: VaultDto): string[] {
  return Object.keys(vault.emergencyKeyShares);
}

function isBroken(vault: VaultDto): boolean {
  return vault.requiredEmergencyKeyShares > emergencyAccessMembers(vault).length;
}

function getCompletedSegmentsForProcess(proc: RecoveryProcessDto): number {
  return Object.values(proc.recoveredKeyShares)
    .filter(ks => ks?.recoveredKeyShare !== undefined).length;
}

function openRecoveryStartDialog(vault: VaultDto, type: RecoveryProcessDto['type']) {
  recoveryApprovVault.value = vault;
  selectedProcess.value = undefined;
  startType.value = type;
  nextTick(() => recoveryApprovDialog.value?.show());
}

function getProcesses(vaultId: string): RecoveryProcessDto[] {
  return vaultRecoveryProcesses.value[vaultId];
}

async function onVaultDetailsToggle(vaultId: string, event: Event) {
  const details = event.target as HTMLDetailsElement;
  if (!details.open) {
    return;
  }

  await loadMembersForVault(vaultId);
}

async function loadMembersForVault(vaultId: string) {
  if (membersByVaultId.value[vaultId] || membersLoadingByVaultId.value[vaultId]) {
    return;
  }

  membersLoadingByVaultId.value[vaultId] = true;
  try {
    membersByVaultId.value[vaultId] = await backend.vaults.getMembers(vaultId);
  } finally {
    membersLoadingByVaultId.value[vaultId] = false;
  }
}

function getVaultMembers(vaultId: string): MemberDto[] {
  return membersByVaultId.value[vaultId] ?? [];
}

</script>
