<template>
  <div v-if="vaults == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>
  <LicenseAlert v-if="isLicenseViolated && isAdmin != undefined && licenseStatus" :is-admin="isAdmin" :license-status="licenseStatus" />

  <div v-if="isCommunityLicense" class="flex flex-col justify-center items-center text-center">
    <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
      <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
    </svg>
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('auditLog.paymentRequired.message') }}</h3>
    <p class="mt-1 text-sm text-gray-500">Emergency Access is only available with a paid license. {{ isAdmin ? 'You can get one in the admin section.' : '' }}</p>
    <router-link v-slot="{ navigate }" to="/app/admin/settings" :hidden="!isAdmin" custom>
      <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-xs text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary mt-6" @click="navigate()">
        <WrenchIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
        {{ t('auditLog.paymentRequired.openAdminSection') }}
      </button>
    </router-link>
  </div>
  <div v-else>
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
    <div class="pb-5 mt-3 border-b border-gray-200 flex flex-wrap sm:flex-nowrap gap-3 items-center whitespace-nowrap">
      <input id="vaultSearch" v-model="query" :placeholder="t('vaultList.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200"/>

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

    <div v-if="filteredVaults?.length > 0" class="mt-5 bg-white shadow-sm rounded-md">
      <ul class="divide-y divide-gray-200">
        <li v-for="(vault, index) in filteredVaults" :key="vault.masterkey">
          <a class="block" :class="{'rounded-t-md': index == 0, 'rounded-b-md': index == filteredVaults.length - 1}">
            <div class="px-4 py-4 sm:px-6">

              <div class="flex flex-wrap gap-3 sm:flex-nowrap sm:items-center sm:justify-between">
                <!-- Name and description -->
                <div class="flex-1 min-w-[10rem]">
                  <div class="flex items-center gap-3 min-w-0">
                    <p class="truncate text-sm font-medium text-primary min-w-0">
                      {{ vault.name }}
                    </p>
                  </div>
                  <p
                    v-if="vault.description"
                    class="truncate text-sm text-gray-500 mt-2 min-w-0"
                  >
                    {{ vault.description }}
                  </p>
                </div>

                <div class="flex flex-wrap items-center gap-2 sm:justify-end">
                  <EmergencyBadge
                    v-if="!isEmergencyKeyShareHolder(vault)"
                    type="notCouncil"
                    title="No Vault Council Member anymore"
                    message="You are no longer part of the actual vault's emergency council. But you are still part of an running emergency access process."
                  />

                  <EmergencyBadge
                    v-else-if="isBroken(vault)"
                    type="broken"
                    title="Broken EA"
                    message="Emergency Access is not possible anymore. One or more council members performed an account reset and lost their key shares."
                  />

                  <EmergencyBadge
                    v-else-if="noRedundancy(vault)"
                    type="noRedundancy"
                    title="No Redundancy"
                    message="This Emergency Access Council has no redundancy. Consider assigning a council with redundancy."
                  />

                  <!-- Council Members -->
                  <div v-if="getCurrentCouncilMembers(vault).length && isEmergencyKeyShareHolder(vault)" class="mt-2 mr-5">
                    <div class="relative group inline-flex -space-x-2">
                      <template v-for="m in getCouncilPreview(vault).list" :key="m.id">
                        <div class="relative h-8 w-8 rounded-full ring-1 ring-gray-200 bg-white overflow-hidden flex items-center justify-center">
                          <img
                            v-if="getAvatarUrl(m)"
                            :src="getAvatarUrl(m)"
                            :alt="m.name"
                            class="h-full w-full object-cover"
                          />
                          <div
                            v-else
                            class="h-full w-full flex items-center justify-center text-[9px] font-semibold text-gray-700"
                          >
                            {{ initials(m.name) }}
                          </div>
                        </div>
                      </template>

                      <!-- +N Circle -->
                      <div
                        v-if="getCouncilPreview(vault).extra > 0"
                        class="relative z-10 h-8 w-8 rounded-full ring-1 ring-gray-200 bg-gray-200 overflow-hidden
                              flex items-center justify-center text-[10px] font-semibold text-gray-700"
                        :title="`+${getCouncilPreview(vault).extra}`"
                        style="margin-left: 4px;"
                      >
                        +{{ getCouncilPreview(vault).extra }}
                      </div>

                      <!-- Hover-Card -->
                      <div
                        class="invisible opacity-0 group-hover:visible group-hover:opacity-100 transition-opacity duration-150
                              absolute left-0 top-9 z-20 w-80 rounded-lg border border-gray-200 bg-white p-3 shadow-xl"
                        role="tooltip"
                      >
                        <div class="flex items-center justify-between mb-1">
                          <div>
                            <div class="text-xl">Vault Council</div>
                            <div class="text-xs text-gray-500 mb-2">
                              Required KeyShares:
                              {{ vault!.requiredEmergencyKeyShares }}
                            </div>
                          </div>
                          <SegmentRing
                            :total="vault!.requiredEmergencyKeyShares"
                            :completed="0"
                            :size="42"
                          />
                        </div>
                        <ul class="space-y-1 max-h-56 overflow-auto pr-1">
                          <li
                            v-for="m in getCurrentCouncilMembers(vault)"
                            :key="'hc-' + vault.id + '-' + m.id"
                            class="flex items-center justify-between text-sm h-6"
                          >
                            <span class="truncate flex items-center gap-2">
                              <img v-if="getAvatarUrl(m)" :src="getAvatarUrl(m)" :alt="m.name" class="h-4 w-4 rounded-full" />
                              <span class="truncate">{{ m.name }}</span>
                            </span>
                          </li>
                        </ul>
                      </div>
                    </div>
                  </div>

                  <!-- ASSIGN OWNER Button - old council -->
                  <div
                    v-if="!isEmergencyKeyShareHolder(vault)"
                    class="flex flex-wrap items-center gap-2 pr-2 self-center"
                  >
                    <template v-for="proc in getProcesses(vault.id)" :key="proc.id">
                      <EmergencyProcessButton
                        v-if="me && isUserInProcess(proc) && proc.type === 'CHANGE_PERMISSIONS'"
                        label="Change Permissions"
                        :approval-label="getApprovalLabel(proc)"
                        :disabled="isBroken(vault)"
                        :has-process="true"
                        :can-start="false"
                        :required-key-shares="proc.requiredKeyShares"
                        :completed-key-shares="getCompletedSegmentsForProcess(proc)"
                        :council-members="getCouncilMembersForProcess(proc)"
                        :recovered-member-ids="Array.from(recoveredMemberIdsForProcess(proc))"
                        @click-main="openRecoveryDialog(vault, proc)"
                      />
                    </template>
                  </div>

                  <!-- EA Buttons -->
                  <div
                    v-if="(me && vault.emergencyKeyShares?.[me.id]) || isEmergencyKeyShareHolder(vault)"
                    class="flex flex-col gap-2 pr-2 self-stretch lg:flex-row flex-wrap lg:items-center lg:justify-end"
                  >
                    <template v-for="type in SUPPORTED_PROCESS_TYPES" :key="'unified-' + vault.id + '-' + type">
                      <EmergencyProcessButton
                        v-if="getProcessByType(vault, type)"
                        :label="getTypeLabel(vault, type)"
                        :approval-label="getApprovalLabel(getProcessByType(vault, type)!)"
                        :disabled="isBroken(vault)"
                        :has-process="true"
                        :can-start="false"
                        :required-key-shares="getProcessByType(vault, type)!.requiredKeyShares"
                        :completed-key-shares="getCompletedSegmentsForProcess(getProcessByType(vault, type)!)"
                        :council-members="getCouncilMembersForProcess(getProcessByType(vault, type)!)"
                        :recovered-member-ids="Array.from(recoveredMemberIdsForProcess(getProcessByType(vault, type)!))"
                        @click-main="onUnifiedButtonClick(vault, type)"
                      />
                      <EmergencyProcessButton
                        v-else
                        :label="getTypeLabel(vault, type)"
                        :disabled="isBroken(vault)"
                        :has-process="false"
                        :can-start="true"
                        @click-main="onUnifiedButtonClick(vault, type)"
                      />
                    </template>
                  </div>

                </div>
              </div>
            </div>
          </a>
        </li>
      </ul>
    </div>

    <div v-else-if="filteredVaults && filteredVaults.length == 0" class="mt-3 text-center">
      <h3 class="mt-2 text-sm font-medium text-gray-900">No emergency access vaults found</h3>
    </div>
  </div>


  <EmergencyAccessDialog
    v-if="recoveryApprovVault != null"
    ref="recoveryApprovDialog"
    :vault="recoveryApprovVault"
    :me="me!"
    :recovery-process="selectedProcess"
    :start-type="startType"
    @updated="fetchData"
    @close="recoveryApprovVault = null"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import * as R from 'remeda';
import auth from '../../common/auth';
import backend, { BillingDto, LicenseUserInfoDto, VaultDto, RecoveryProcessDto, AuthorityDto } from '../../common/backend';
import FetchError from '../FetchError.vue';
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import LicenseAlert from '../LicenseAlert.vue';
import { CheckIcon, ChevronUpDownIcon, WrenchIcon } from '@heroicons/vue/24/solid';
import userdata from '../../common/userdata';
import { UserDto } from '../../common/backend';
import EmergencyAccessDialog from './EmergencyAccessDialog.vue';
import SegmentRing from './SegmentRing.vue';
import EmergencyBadge from './EmergencyBadge.vue';
import EmergencyProcessButton from './EmergencyProcessButton.vue';

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type?: string;
  memberSize?: number;
}

const SUPPORTED_PROCESS_TYPES = ['CHANGE_PERMISSIONS', 'COUNCIL_CHANGE'] as const;

const { t } = useI18n({ useScope: 'global' });
const me = ref<UserDto>();
const query = ref('');
const vaults = ref<VaultDto[]>([]);
const onFetchError = ref<Error | null>(null);

const isAdmin = ref<boolean>(false);

const licenseStatus = ref<LicenseUserInfoDto>();
const isLicenseViolated = computed(() => {
  if (licenseStatus.value) {
    return licenseStatus.value.isExceeded() || licenseStatus.value.isExpired();
  } else {
    return false;
  }
});

const isCommunityLicense = computed(() => {
  return !licenseStatus.value?.expiresAt;
});

const selectedFilter = ref<'recoverableVaults' | 'approved' | 'approvable' | 'startable'>('recoverableVaults');
const filterOptions = ref({
  recoverableVaults: 'All',
  approvable: 'Approvable',
  approved: 'Approved',
  startable: 'Startable',
});
const selectedProcess = ref<RecoveryProcessDto | undefined>(undefined);
const filteredVaults = computed<VaultDto[]>(() => filterVaults(vaults.value));
const vaultRecoveryProcesses = ref<Record<string, RecoveryProcessDto[]>>({});
const recoveryApprovVault = ref<VaultDto | null>(null);
const recoveryApprovDialog = ref<typeof EmergencyAccessDialog>();
const authoritiesById = ref<Record<string, AuthorityDto>>({});
const billing = ref<BillingDto>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  loadDefaultSettings();
  try {
    me.value = await userdata.me;
    isAdmin.value = (await auth).hasRole('admin');

    licenseStatus.value = await backend.license.getUserInfo();
    //billing.value = await backend.billing.get();

    vaults.value = (await backend.vaults.listRecoverable())
      .filter(v => !v.archived)
      .sort((a, b) => a.name.localeCompare(b.name));

    for (const vault of vaults.value) {
      const processes = await backend.emergencyAccess.findProcessesForVault(vault.id);
      vaultRecoveryProcesses.value[vault.id] = processes;
    }

    const memberIdsOfAllRunningProcesses = Object
      .values(vaultRecoveryProcesses.value)
      .flat()
      .flatMap(p => Object.keys(p.recoveredKeyShares));

    const councilIds = vaults.value
      .flatMap(v => Object.keys(v.emergencyKeyShares ?? {}));

    const allIds = Array.from(new Set([...memberIdsOfAllRunningProcesses, ...councilIds]));

    if (allIds.length > 0) {
      const auths = await backend.authorities.listSome(allIds);
      authoritiesById.value = R.indexBy(auths, u => u.id);
    } else {
      authoritiesById.value = {};
    }
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
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
  console.log('recovered:' + recovered);
  console.log('proc.requiredKeyShares:' + proc.requiredKeyShares);
  return (recovered + 1 ) >= proc.requiredKeyShares;
}

function getApprovalLabel(proc?: RecoveryProcessDto): string {
  if (!proc) return '';

  if (didAddMyShare(proc))
    return 'Waiting for other approvals';
  else if (!isUserInProcess(proc))
    return 'Show details';
  else {
    if (isProcessFullyApproved(proc) || isProcessAboutToComplete(proc)) {
      return 'Complete now';
    }
    else 
      return 'Approve now';
  }
}

function getProcessByType(vault: VaultDto, type: RecoveryProcessDto['type']): RecoveryProcessDto | undefined {
  return getProcesses(vault.id).find(p => p.type === type);
}

function getTypeLabel(vault: VaultDto, type: RecoveryProcessDto['type']) {
  return type === 'CHANGE_PERMISSIONS'
    ? 'Change Permissions'
    : 'Change Council';
}

const allowChoosingEmergencyCouncil = ref<boolean>(false);
async function loadDefaultSettings() {
  try {
    const settings = await backend.settings.get();
   
    allowChoosingEmergencyCouncil.value = settings.allowChoosingEmergencyCouncil;
  } catch (error) {
    console.error('Loading allowChoosingEmergencyCouncil failed:', error);
    // TODO: don't set defaults, hard-fail with error message instead
    allowChoosingEmergencyCouncil.value = false;
  }
}

function onUnifiedButtonClick(vault: VaultDto, type: RecoveryProcessDto['type']) {
  const proc = getProcessByType(vault, type);
  if (proc) {
    openRecoveryDialog(vault, proc);
  } else {
    openRecoveryStartDialog(vault, type);
  }
}

function getCouncilPreview(vault: VaultDto): { list: Item[]; extra: number } {
  const all = getCurrentCouncilMembers(vault);
  const max = 3;
  const extra = Math.max(0, all.length - max);
  return { list: all.slice(0, max), extra };
}

function getAvatarUrl(u: Item | UserDto | AuthorityDto | any): string | undefined {
  return u?.pictureUrl || u?.avatarUrl || u?.imageUrl || undefined;
}

function initials(name: string): string {
  return (name ?? '')
    .split(' ')
    .map((p: string) => p.trim()[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase();
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

function filterVaults(vaults: VaultDto[]): VaultDto[] {
  let result: VaultDto[];

  switch (selectedFilter.value) {
    case 'recoverableVaults': // "All"
      result = vaults;
      break;
    case 'approved':
      result = vaults.filter((vault) => hasSubmittedEmergencyKeyShare(vault));
      break;
    case 'approvable':
      result = vaults.filter((vault) => {
        return activeProcessForVault(vault) && !hasSubmittedEmergencyKeyShare(vault);
      });
      break;
    case 'startable':
      result = vaults.filter((vault) => {
        return !hasAllProcessTypesStarted(vault);
      });
      break;
    default: throw new Error(`Unknown filter type: ${selectedFilter.value}`);
  }

  if (query.value !== '') {
    result = result.filter((vault) =>
      vault.name.toLowerCase().includes(query.value.toLowerCase())
    );
  }

  return result;
}

function hasAllProcessTypesStarted(vault: VaultDto): boolean {
  const started = getProcesses(vault.id).map(p => p.type);
  return SUPPORTED_PROCESS_TYPES.every(t => started.includes(t));
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

function getCouncilMembersForProcess(proc: RecoveryProcessDto): Item[] {
  return Object.keys(proc.recoveredKeyShares).map((id) => authoritiesById.value[id] ?? { id, name: id });
}

function hasSubmittedEmergencyKeyShare(vault: VaultDto): boolean {
  const proc = activeProcessForVault(vault);
  if (!me.value || !proc?.recoveredKeyShares) return false;
  return proc.recoveredKeyShares[me.value.id]?.recoveredKeyShare !== undefined;
}

function isEmergencyKeyShareHolder(vault: VaultDto): boolean {
  if (!vault || !me.value) return false;
  return vault.emergencyKeyShares[me.value.id] !== undefined;
}

function noRedundancy(vault: VaultDto): boolean {
  const members = Object.keys(vault.emergencyKeyShares).length;
  return vault.requiredEmergencyKeyShares == members;
}

function isBroken(vault: VaultDto): boolean {
  const members = Object.keys(vault.emergencyKeyShares).length;
  return vault.requiredEmergencyKeyShares > members;
}

function getCompletedSegmentsForProcess(proc: RecoveryProcessDto): number {
  return Object.values(proc.recoveredKeyShares)
    .filter(ks => ks?.recoveredKeyShare !== undefined).length;
}

const startType = ref<RecoveryProcessDto['type'] | undefined>(undefined);

function openRecoveryStartDialog(vault: VaultDto, type?: RecoveryProcessDto['type']) {
  recoveryApprovVault.value = vault;
  selectedProcess.value = undefined;
  startType.value = type;
  nextTick(() => recoveryApprovDialog.value?.show());
}

function getProcesses(vaultId: string): RecoveryProcessDto[] {
  return vaultRecoveryProcesses.value[vaultId] ?? [];
}

// TODO: currently, we always return "any" process
function activeProcessForVault(vault: VaultDto): RecoveryProcessDto | undefined {
  const list = getProcesses(vault.id);
  if (list.length === 0) return undefined;
  return list[0];
}

</script>
