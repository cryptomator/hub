<template>
  <div v-if="vaults == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <h2 class="text-2xl font-bold leading-9 text-gray-900 sm:text-3xl sm:truncate">
    {{ t('nav.emergencyAccess') }}
  </h2>

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
                v-if="me && vault.emergencyKeyShares?.[me.id] && vaultRecoveryProcesses[vault.id] || isEmergencyKeyShareHolder(vault)"
                type="button"
                class="inline-flex items-center gap-2 rounded-md bg-white px-2 py-1 text-xs font-medium text-gray-700 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 relative"
                @click.stop="openRecoveryDialog(vault)"
              >
                {{ hasSubmittedEmergencyKeyShare(vault) ? t('vaultDetails.actions.showEmergencyAccessState') : getRecoveryLabel(vault) }}
              </button>
            </div>
            <div class="relative group">
              <svg class="ml-auto shrink-0" width="36" height="36" viewBox="0 0 36 36">
                <g>
                  <path
                    v-for="i in getRequiredSegments(vault)"
                    :key="i"
                    :d="describeSegment(i - 1, getRequiredSegments(vault), 16)"
                    :fill="i <= getCompletedSegments(vault) ? '#22c55e' : '#e5e7eb'"
                    stroke="white"
                    stroke-width="1"
                  />
                </g>
              </svg>

              <!-- Tooltip -->
              <div
                class="absolute z-99999 top-1/2 -translate-y-1/2 right-full mr-2
                  hidden group-hover:block bg-white border border-gray-300
                  rounded-md shadow-md p-2 text-sm text-gray-700 whitespace-nowrap
                  min-w-[200px] overflow-visible"
              >
                <div class="font-medium mb-1">
                  Required Key Shares: {{ vault.requiredEmergencyKeyShares }}
                </div>
                <div v-if="emergencyKeyShareUsersByVaultId[vault.id]?.length">
                  <span class="font-medium">Council Members:</span>
                  <ul class="list-disc ml-5 mt-1">
                    <li v-for="user in emergencyKeyShareUsersByVaultId[vault.id]" :key="user.id">
                      {{ user.name }}
                      <span v-if="vaultRecoveryProcesses[vault.id]?.recoveredKeyShares?.[user.id]?.recoveredKeyShare">
                        &check;
                      </span>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </a>
      </li>
    </ul>
  </div>

  <div v-else-if="filteredVaults && filteredVaults.length == 0" class="mt-3 text-center">
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('vaultList.empty.title') }}</h3>
    <p class="mt-1 text-sm text-gray-500">{{ t('vaultList.empty.description') }}</p>
  </div>

  <RecoveryApprovDialog
    v-if="recoveryApprovVault != null"
    ref="recoveryApprovDialog"
    :vault="recoveryApprovVault"
    :me="me!"
    :recovery-process="vaultRecoveryProcesses[recoveryApprovVault.id]"
    @updated="handleRecoveryUpdated"
    @close="recoveryApprovVault = null"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { VaultDto, RecoveryProcessDto } from '../common/backend';
import FetchError from './FetchError.vue';
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { CheckIcon, ChevronUpDownIcon } from '@heroicons/vue/24/solid';
import userdata from '../common/userdata';
import { UserDto } from '../common/backend';
import { describeSegment } from '../common/svgUtils';
import RecoveryApprovDialog from './RecoveryApprovDialog.vue';

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type?: string;
  memberSize?: number;
}

const { t } = useI18n({ useScope: 'global' });
const me = ref<UserDto>();
const query = ref('');
const vaults = ref<VaultDto[]>();
const onFetchError = ref<Error | null>(null);

const selectedFilter = ref<'recoverableVaults' | 'approved' | 'approvable' | 'startable'>('recoverableVaults');
const filterOptions = ref({
  recoverableVaults: t('vaultList.filter.all'),
  approvable: t('vaultList.filter.approvable'),
  approved: t('vaultList.filter.approved'),
  startable: t('vaultList.filter.startable'),
});
const filteredVaults = computed<VaultDto[]>(() => {
  let result = vaults.value ?? [];

  switch (selectedFilter.value) {
    case 'approved':
      result = result.filter((vault) => hasSubmittedEmergencyKeyShare(vault));
      break;
    case 'approvable':
      result = result.filter((vault) => {
        const proc = vaultRecoveryProcesses.value[vault.id];
        const hasKeyShares = proc?.recoveredKeyShares && Object.keys(proc.recoveredKeyShares).length > 0;
        return hasKeyShares && !hasSubmittedEmergencyKeyShare(vault);
      });
      break;
    case 'startable':
      result = result.filter((vault) => {
        const proc = vaultRecoveryProcesses.value[vault.id];
        const hasNoKeyShares =
          !proc?.recoveredKeyShares ||
          Object.keys(proc.recoveredKeyShares).length === 0;
        return hasNoKeyShares && !hasSubmittedEmergencyKeyShare(vault);
      });
      break;  
  }

  if (query.value !== '') {
    result = result.filter((vault) =>
      vault.name.toLowerCase().includes(query.value.toLowerCase())
    );
  }

  return result;
});

const vaultRecoveryProcesses = ref<Record<string, RecoveryProcessDto>>({});

const emergencyKeyShareUsersByVaultId = ref<Record<string, Item[]>>({});

const recoveryApprovVault = ref<VaultDto | null>(null);
const recoveryApprovDialog = ref<typeof RecoveryApprovDialog>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await userdata.me;
    vaults.value = (await backend.vaults.listRecoverable()).filter(v => !v.archived).sort((a, b) => a.name.localeCompare(b.name));
    for (const vault of vaults.value ?? []) {
      const processes = await backend.emergencyAccess.findProcessesForVault(vault.id);
      if (processes.length > 0) {
        vaultRecoveryProcesses.value[vault.id] = processes[0];
      }
    }
    await resolveEmergencyKeyShareUsers(vaults.value ?? []);
    const allUserIds = new Set<string>();
    const vaultCouncilMap = new Map<string, string[]>();

    for (const vault of vaults.value ?? []) {
      const process = await loadVaultRecoveryProcess(vault.id);
      await loadEmergencyKeyShareUsers(vault);
    }

    const authorities = await backend.authorities.listSome(Array.from(allUserIds));
    const usersById: Record<string, Item> = Object.fromEntries(
      authorities.map((u) => [u.id, u])
    );
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function getRecoveryLabel(vault: VaultDto): string {
  const proc = vaultRecoveryProcesses.value[vault.id];
  const required = proc?.requiredKeyShares ?? 1;
  const completed = Object.values(proc?.recoveredKeyShares ?? {}).filter(ks => ks.recoveredKeyShare !== undefined).length;

  if (completed === 0) {
    return t('vaultDetails.actions.startEmergencyAccess');
  } else if (completed + 1 === required) {
    return t('vaultDetails.actions.completeEmergencyAccess');
  } else {
    return t('vaultDetails.actions.approveEmergencyAccess');
  }
}

function getRequiredSegments(vault: VaultDto): number {
  return vaultRecoveryProcesses.value[vault.id]?.requiredKeyShares ?? vault.requiredEmergencyKeyShares;
}

function getCompletedSegments(vault: VaultDto): number {
  return Object.values(vaultRecoveryProcesses.value[vault.id]?.recoveredKeyShares ?? {}).filter(ks => ks.recoveredKeyShare !== undefined).length;
}

function isEmergencyKeyShareHolder(vault: VaultDto): boolean {
  if (!vault || !me.value) return false;
  return vault.emergencyKeyShares[me.value.id] !== undefined;
};

function hasSubmittedEmergencyKeyShare(vault: VaultDto): boolean {
  const proc = vaultRecoveryProcesses.value[vault.id];
  if (!me.value || !proc?.recoveredKeyShares) return false;
  return proc.recoveredKeyShares[me.value.id]?.recoveredKeyShare !== undefined;
}

async function resolveEmergencyKeyShareUsers(vaults: VaultDto[]) {
  const allUserIds = new Set<string>();
  for (const vault of vaults) {
    Object.keys(vault.emergencyKeyShares).forEach(id => allUserIds.add(id));
  }

  const authorities = await backend.authorities.listSome(Array.from(allUserIds));
  const usersById: Record<string, Item> = Object.fromEntries(
    authorities.map(u => [
      u.id, 
      { 
        id: u.id, 
        name: u.name, 
        pictureUrl: u.pictureUrl 
      }
    ])
  );

  emergencyKeyShareUsersByVaultId.value = Object.fromEntries(
    vaults.map(v => [
      v.id,
      Object.keys(v.emergencyKeyShares).map(id => usersById[id]).filter(Boolean)
    ])
  );
}

async function loadVaultRecoveryProcess(vaultId: string): Promise<RecoveryProcessDto | null> {
  const processes = await backend.emergencyAccess.findProcessesForVault(vaultId);
  const process = processes[0] ?? null; // TODO: handle multiple parallel processes (database allows one per vault and type)
  if (process) {
    vaultRecoveryProcesses.value[vaultId] = process;
  } else {
    delete vaultRecoveryProcesses.value[vaultId];
  }
  return process;
}

async function loadEmergencyKeyShareUsers(vault: VaultDto) {
  const userIds = Object.keys(vault.emergencyKeyShares);
  const users = await backend.authorities.listSome(userIds);
  emergencyKeyShareUsersByVaultId.value[vault.id] = users.map(u => ({
    id: u.id,
    name: u.name,
    pictureUrl: u.pictureUrl,
  }));
}

async function reloadVaultData(vaultId: string) {
  try {
    const updatedVault = await backend.vaults.get(vaultId);

    if (!vaults.value) vaults.value = [];
    const index = vaults.value.findIndex(v => v.id === vaultId);
    if (index >= 0) vaults.value[index] = updatedVault;
    else vaults.value.push(updatedVault);

    await loadEmergencyKeyShareUsers(updatedVault);
  } catch (err) {
    console.error('Fehler beim Nachladen eines Vaults:', err);
  }
}

async function handleRecoveryUpdated(updatedProcess?: RecoveryProcessDto) {
  const vaultId = recoveryApprovVault.value?.id;
  try {
    if (vaultId) {
      await reloadVaultData(vaultId);
    }
  } catch (e) {
    console.error('Fehler beim gezielten Nachladen nach Recovery:', e);
  } finally {
    recoveryApprovVault.value = null;
  }
}

function openRecoveryDialog(vault: VaultDto) {
  recoveryApprovVault.value = vault;
  nextTick(() => recoveryApprovDialog.value?.show());
}

</script>
