<template>
  <div v-if="vaults == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>
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
        <a class="block hover:bg-gray-50" :class="{'rounded-t-md': index == 0, 'rounded-b-md': index == filteredVaults.length - 1}">
          <div class="px-4 py-4 flex items-center sm:px-6">
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-3">
                <p class="truncate text-sm font-medium text-primary">{{ vault.name }}</p>
              </div>
              <p v-if="vault.description" class="truncate text-sm text-gray-500 mt-2">{{ vault.description }}</p>
            </div>
            <!-- Recovery-Button -->
            <div class="mt-2 flex flex-wrap items-center pr-2">
              <button
                v-if="(me && vault.emergencyKeyShares?.[me.id] || isEmergencyKeyShareHolder(vault)) && !hasAllProcessTypesStarted(vault)"
                type="button"
                class="inline-flex items-center gap-2 rounded-md bg-white px-2 py-1 text-xs font-medium text-gray-700 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 relative"
                @click.stop="openRecoveryStartDialog(vault)"
              >
                {{ t('common.start') }}
              </button>
            </div>

            <div class="mt-2 flex flex-wrap items-center gap-2 pr-2">
              <template v-for="proc in getProcesses(vault.id)" :key="proc.id">
                <button
                  v-if="me && isUserInProcess(proc)"
                  type="button"
                  class="inline-flex items-center gap-2 rounded-md bg-white px-2 py-1 text-xs font-medium text-gray-700 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                  @click.stop="openRecoveryDialog(vault, proc)"
                >
                  <div class="relative group">
                    <svg class="shrink-0" width="20" height="20" viewBox="0 0 36 36">
                      <g>
                        <path
                          v-for="i in proc.requiredKeyShares"
                          :key="i"
                          :d="describeSegment(i - 1, proc.requiredKeyShares, 16)"
                          :fill="i <= getCompletedSegmentsForProcess(proc) ? '#22c55e' : '#e5e7eb'"
                          stroke="white"
                          stroke-width="1"
                        />
                      </g>
                    </svg>

                    <div
                      class="absolute z-50 top-1/2 -translate-y-1/2 left-full ml-2
                            hidden group-hover:block bg-white border border-gray-300
                            rounded-md shadow-md p-2 text-sm text-gray-700 whitespace-nowrap
                            min-w-[200px]"
                    >
                      <div class="font-medium mb-1">
                        {{ t('vaultDetails.emergency.requiredKeyShares') }}: {{ proc.requiredKeyShares }}
                      </div>
                      <div v-if="getCouncilMembersForProcess(proc)?.length">
                        <span class="font-medium">{{ t('vaultDetails.emergency.councilMembers') }}:</span>
                        <ul class="list-disc ml-5 mt-1">
                          <li v-for="user in getCouncilMembersForProcess(proc)" :key="user.id">
                            {{ user.name }}
                            <span v-if="proc.recoveredKeyShares?.[user.id]?.recoveredKeyShare">
                              &check;
                            </span>
                          </li>
                        </ul>
                      </div>
                    </div>
                  </div>

                  <span class="rounded-full px-1.5 py-0.5 ring-1 ring-gray-300 text-[10px] uppercase tracking-wide">
                    {{ proc.type }}
                  </span>

                  {{ hasSubmittedEmergencyKeyShareForProcess(proc)
                    ? t('vaultDetails.actions.showEmergencyAccessState')
                    : getRecoveryLabelForProcess(proc) }}
                </button>
              </template>
            </div>

            <div v-if="needsRedundancy(vault)" class="ml-3">
              <span
                class="inline-flex items-center gap-2 rounded-full bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800"
                :title="t('vaultList.emergency.noRedundancyHint')"
              >
                <ExclamationTriangleIcon class="h-4 w-4" aria-hidden="true" />
                {{ t('vaultList.emergency.noRedundancy') }}
              </span>
            </div>
          </div>
          <!-- TODO: remove this dev area -->
          <div v-if="getProcesses(vault.id).length" class="px-4 pb-4 sm:px-6">
            <div v-for="proc in getProcesses(vault.id)" :key="proc.id" class="mb-3">
              <div class="text-xs text-gray-700 mb-1">
                <strong>Council ({{ getCouncilMembersForProcess(proc).length }}):</strong>
                <span v-for="u in getCouncilMembersForProcess(proc)" :key="u.id" class="inline-flex items-center gap-1 mr-2">
                  <img v-if="u.pictureUrl" :src="u.pictureUrl" alt="" class="w-4 h-4 rounded-full" />
                  <span>{{ u.name || u.id }}</span>
                  <span v-if="proc.recoveredKeyShares[u.id]?.recoveredKeyShare">✔</span>
                  <span v-else-if="proc.recoveredKeyShares[u.id]?.unrecoveredKeyShare">•</span>
                </span>
              </div>

              <pre class="text-xs text-gray-700 bg-gray-50 rounded p-2 overflow-x-auto">{{ stringifyProcess(proc) }}</pre>
            </div>
          </div>
        </a>
      </li>
    </ul>
  </div>

  <div v-else-if="filteredVaults && filteredVaults.length == 0" class="mt-3 text-center">
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('emergencyAccessVaultList.empty.title') }}</h3>
  </div>

  <EmergencyAccessDialog
    v-if="recoveryApprovVault != null"
    ref="recoveryApprovDialog"
    :vault="recoveryApprovVault"
    :me="me!"
    :recovery-process="selectedProcess"
    @updated="fetchData"
    @close="recoveryApprovVault = null"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import * as R from 'remeda';
import backend, { VaultDto, RecoveryProcessDto, RecoveredKeyShareDto, AuthorityDto } from '../../common/backend';
import FetchError from '../FetchError.vue';
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { CheckIcon, ChevronUpDownIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/solid';
import userdata from '../../common/userdata';
import { UserDto } from '../../common/backend';
import { describeSegment } from '../../common/svgUtils';
import EmergencyAccessDialog from './EmergencyAccessDialog.vue';

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type?: string;
  memberSize?: number;
}

const SUPPORTED_PROCESS_TYPES = ['ASSIGN_OWNER', 'COUNCIL_CHANGE'] as const;

const { t } = useI18n({ useScope: 'global' });
const me = ref<UserDto>();
const query = ref('');
const vaults = ref<VaultDto[]>([]);
const onFetchError = ref<Error | null>(null);

const selectedFilter = ref<'recoverableVaults' | 'approved' | 'approvable' | 'startable'>('recoverableVaults');
const filterOptions = ref({
  recoverableVaults: t('vaultList.filter.all'),
  approvable: t('vaultList.filter.approvable'),
  approved: t('vaultList.filter.approved'),
  startable: t('vaultList.filter.startable'),
});
const selectedProcess = ref<RecoveryProcessDto | undefined>(undefined);
const filteredVaults = computed<VaultDto[]>(() => filterVaults(vaults.value));
const vaultRecoveryProcesses = ref<Record<string, RecoveryProcessDto[]>>({});
const recoveryApprovVault = ref<VaultDto | null>(null);
const recoveryApprovDialog = ref<typeof EmergencyAccessDialog>();
const authoritiesById = ref<Record<string, AuthorityDto>>({});

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await userdata.me;
    vaults.value = (await backend.vaults.listRecoverable()).filter(v => !v.archived).sort((a, b) => a.name.localeCompare(b.name));
    for (const vault of vaults.value) {
      const processes = await backend.emergencyAccess.findProcessesForVault(vault.id);
      if (processes.length > 0) {
        vaultRecoveryProcesses.value[vault.id] = processes;
      }
    }

    const memberIdsOfAllRunningProcesses = Object.values(vaultRecoveryProcesses.value).flat().flatMap(p => Object.keys(p.recoveredKeyShares));
    if (memberIdsOfAllRunningProcesses.length > 0) {
      const auths = await backend.authorities.listSome(memberIdsOfAllRunningProcesses);
      authoritiesById.value = R.indexBy(auths, u => u.id);
    }
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
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

function getRecoveryLabelForProcess(proc: RecoveryProcessDto): string {
  const required = proc?.requiredKeyShares ?? 1;
  const completed = Object.values(proc?.recoveredKeyShares ?? {})
    .filter(ks => ks.recoveredKeyShare !== undefined).length;

  if (completed === 0) return t('vaultDetails.actions.startEmergencyAccess');
  if (completed + 1 === required) return t('vaultDetails.actions.completeEmergencyAccess');
  return t('vaultDetails.actions.approveEmergencyAccess');
}

function hasSubmittedEmergencyKeyShareForProcess(proc: RecoveryProcessDto): boolean {
  if (!me.value || !proc?.recoveredKeyShares) return false;
  return proc.recoveredKeyShares[me.value.id]?.recoveredKeyShare !== undefined;
}

function stringifyProcess(proc: RecoveryProcessDto): string {
  try {
    return JSON.stringify(proc, null, 2);
  } catch {
    return String(proc);
  }
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

function needsRedundancy(vault: VaultDto): boolean {
  const members = Object.keys(vault.emergencyKeyShares).length;
  return vault.requiredEmergencyKeyShares >= members;
}

async function loadVaultRecoveryProcess(vaultId: string): Promise<RecoveryProcessDto[] | null> {
  const processes = await backend.emergencyAccess.findProcessesForVault(vaultId);
  if (processes.length > 0) {
    vaultRecoveryProcesses.value[vaultId] = processes;
    return processes;
  } else {
    delete vaultRecoveryProcesses.value[vaultId];
    return null;
  }
}

function getCompletedSegmentsForProcess(proc: RecoveryProcessDto): number {
  return Object.values(proc.recoveredKeyShares)
    .filter(ks => ks?.recoveredKeyShare !== undefined).length;
}

function openRecoveryStartDialog(vault: VaultDto) {
  recoveryApprovVault.value = vault;
  selectedProcess.value = undefined;
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
