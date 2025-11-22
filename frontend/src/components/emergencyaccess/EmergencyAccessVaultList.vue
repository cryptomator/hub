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

                <!-- Council Members -->
                <div v-if="getCurrentCouncilMembers(vault).length && isEmergencyKeyShareHolder(vault)" class="mt-2 mr-5">
                  <div class="relative group inline-flex -space-x-2">
                    <template v-for="m in getCouncilPreview(vault).list" :key="m.id">
                      <div class="relative h-7 w-7 rounded-full ring-1 ring-gray-400 bg-white overflow-hidden flex items-center justify-center">
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
                      class="relative z-10 h-7 w-7 rounded-full ring-2 ring-white bg-gray-200 overflow-hidden
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
                            {{ t('recoveryDialog.requiredKeyShares') }}:
                            {{ vault!.requiredEmergencyKeyShares }}
                          </div>
                        </div>
                        <SegmentRing
                          :total="vault!.requiredEmergencyKeyShares"
                          :completed="0"
                          class="shrink-0 pointer-events-none select-none"
                          :width="42"
                          :height="42"
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
                <!-- Not a council member badge -->
                <div v-if="!isEmergencyKeyShareHolder(vault)" class="relative mr-3 group">
                  <span
                    class="inline-flex items-center gap-2 rounded-full bg-yellow-50 ring-1 ring-yellow-300/70 
                          px-2.5 py-1 text-xs font-medium text-yellow-800 cursor-default"
                  >
                    <ExclamationTriangleIcon class="h-4 w-4" aria-hidden="true" />
                  </span>

                  <!-- Tooltip -->
                  <div
                    class="invisible opacity-0 group-hover:visible group-hover:opacity-100 transition-opacity duration-150
                          absolute left-1/2 -translate-x-1/2 -top-2 transform -translate-y-full w-max max-w-xs z-10"
                  >
                    <div class="bg-yellow-50 border border-yellow-300 text-yellow-900 px-2 py-1 rounded shadow-sm text-xs hyphens-auto relative">
                      You are no longer part of the actual vault's emergency council.
                      <div
                        class="absolute bottom-0 left-1/2 transform translate-y-1/2 -translate-x-1/2 rotate-45 
                              w-2 h-2 bg-yellow-50 border-r border-b border-yellow-300"
                      ></div>
                    </div>
                  </div>
                </div>
                <!-- Broken EA -->
                <div v-else-if="isBroken(vault) && isEmergencyKeyShareHolder(vault)" class="mr-3">
                  <span
                    class="inline-flex items-center gap-2 rounded-full bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800"
                    :title="t('emergencyAccessVaultList.noRedundancyHint')"
                  >
                    <ExclamationTriangleIcon class="h-4 w-4" aria-hidden="true" />
                    Broken EA
                  </span>
                </div>
                <!-- Needs Redundancy badge -->
                <div v-else-if="noRedundancy(vault) && isEmergencyKeyShareHolder(vault)" class="mr-3">
                  <span
                    class="inline-flex items-center gap-2 rounded-full bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800"
                    :title="t('emergencyAccessVaultList.noRedundancyHint')"
                  >
                    <ExclamationTriangleIcon class="h-4 w-4" aria-hidden="true" />
                    {{ t('emergencyAccessVaultList.noRedundancy') }}
                  </span>
                </div>
                <!-- ASSIGN OWNER Button - old council -->
                <div v-if="!isEmergencyKeyShareHolder(vault)" class="flex flex-wrap items-center gap-2 pr-2 self-center">
                  <template v-for="proc in getProcesses(vault.id)" :key="proc.id">
                    <div v-if="me && isUserInProcess(proc) && proc.type == 'ASSIGN_OWNER'" class="relative group inline-block">
                      <button
                        type="button"
                        class="h-8 inline-flex items-center gap-2 rounded-md bg-white px-2 py-1 text-xs font-medium text-gray-700 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                        @click.stop="openRecoveryDialog(vault, proc)"
                      >
                        <div class="relative">
                          <svg class="shrink-0 pointer-events-none select-none" width="20" height="20" viewBox="0 0 36 36">
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
                        </div>
                        <div>
                          {{ t('emergencyAccessVaultList.assignOwner') }}
                        </div>
                      </button>
                      <!-- Hover-Card -->
                      <div
                        class="invisible opacity-0 group-hover:visible group-hover:opacity-100 transition-opacity duration-150
                              absolute -left-40 top-9 z-20 w-80 rounded-lg border border-gray-200 bg-white p-3 shadow-xl"
                        role="tooltip"
                      >
                        <div class="flex items-center justify-between mb-1">
                          <div class="text-xl">{{ t('emergencyAccessVaultList.assignOwner') }}</div>
                          <svg
                            class="shrink-0 pointer-events-none select-none"
                            width="42"
                            height="42"
                            viewBox="0 0 36 36"
                            aria-hidden="true"
                          >
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
                        </div>

                        <div class="text-xs text-gray-500 mb-2">
                          {{ t('recoveryDialog.requiredKeyShares') }}:
                          {{ proc.requiredKeyShares }}
                        </div>
                        <div>
                          Process council
                        </div>
                        <ul class="space-y-1 max-h-56 overflow-auto pr-1">
                          <li
                            v-for="m in getCouncilMembersForProcess(proc)"
                            :key="'hc-proc-top-' + vault.id + '-' + proc.id + '-' + m.id"
                            class="flex items-center justify-between text-sm h-6"
                          >
                            <span class="truncate flex items-center gap-2">
                              <img v-if="getAvatarUrl(m)" :src="getAvatarUrl(m)" :alt="m.name" class="h-4 w-4 rounded-full" />
                              <span class="truncate">{{ m.name || m.id }}</span>
                            </span>
                            <span
                              class="ml-2 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px]"
                              :class="recoveredMemberIdsForProcess(proc).has(m.id)
                                ? 'bg-green-50 text-green-700 ring-1 ring-green-200'
                                : 'bg-gray-50 text-gray-600 ring-1 ring-gray-200'"
                            >
                              <span
                                class="h-2 w-2 rounded-full"
                                :class="recoveredMemberIdsForProcess(proc).has(m.id) ? 'bg-green-500' : 'bg-gray-300'"
                              ></span>
                              {{ recoveredMemberIdsForProcess(proc).has(m.id)
                                ? t('recoveryDialog.status.added')
                                : t('recoveryDialog.status.pending') }}
                            </span>
                          </li>
                        </ul>
                      </div>
                    </div>
                  </template>
                </div>

                <!-- EA Buttons -->
                <div
                  v-if="((me && vault.emergencyKeyShares?.[me.id] || isEmergencyKeyShareHolder(vault)))"
                  class="flex flex-col gap-2 pr-2 self-stretch lg:flex-row flex-wrap lg:items-center lg:justify-end"
                >
                  <template v-for="type in SUPPORTED_PROCESS_TYPES" :key="'unified-' + vault.id + '-' + type">
                    <div class="relative group block md:inline-block">
                      <button
                        type="button"
                        class="w-full sm:w-auto h-8 inline-flex items-center gap-2 rounded-md bg-white px-2 py-1 text-xs font-medium text-gray-700 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                        :disabled="isBroken(vault)"
                        @click.stop="onUnifiedButtonClick(vault, type)"
                      >
                        <template v-if="getProcessByType(vault, type)">
                          <SegmentRing
                            :total="getProcessByType(vault, type)!.requiredKeyShares"
                            :completed="getCompletedSegmentsForProcess(getProcessByType(vault, type)!)"
                            class="shrink-0 pointer-events-none select-none"
                          />
                          <span v-if="isEmergencyKeyShareHolder(vault)">
                            {{ getTypeLabel(vault,type) }} - {{ getApprovalLabel(getProcessByType(vault, type)!) }}
                          </span>
                        </template>
                        <template v-else>
                          <PlayIcon class="h-4 w-4 text-primary" aria-hidden="true" />
                          <span>{{ getTypeLabel(vault, type) }}</span>
                        </template>
                      </button>
                      <!-- Hover-Card -->
                      <div
                        class="invisible opacity-0 group-hover:opacity-100 transition-opacity duration-150
                              absolute right-0 top-9 z-20 w-80 rounded-lg border border-gray-200 bg-white p-3 shadow-xl"
                        :class="getProcessByType(vault, type) ? 'group-hover:visible' : ''"
                        role="tooltip"
                      >
                        <!-- Running process -->
                        <template v-if="getProcessByType(vault, type)">
                          <div class="flex items-center justify-between mb-1">
                            <div>
                              <div class="text-xl">{{ getTypeLabel(vault,type) }}</div>
                              <div class="text-xs text-gray-500 mb-2">
                                {{ t('recoveryDialog.requiredKeyShares') }}:
                                {{ getProcessByType(vault, type)!.requiredKeyShares }}
                              </div>
                            </div>
                            <SegmentRing
                              :total="getProcessByType(vault, type)!.requiredKeyShares"
                              :completed="getCompletedSegmentsForProcess(getProcessByType(vault, type)!)"
                              class="shrink-0 pointer-events-none select-none"
                              :width="42"
                              :height="42"
                            />
                          </div>
                          <div>
                            Process council
                          </div>
                          <ul class="space-y-1 max-h-56 overflow-auto pr-1">
                            <li
                              v-for="m in getCouncilMembersForProcess(getProcessByType(vault, type)!)"
                              :key="'hc-proc-' + vault.id + '-' + type + '-' + m.id"
                              class="flex items-center justify-between text-sm h-6"
                            >
                              <span class="truncate flex items-center gap-2">
                                <img v-if="getAvatarUrl(m)" :src="getAvatarUrl(m)" :alt="m.name" class="h-4 w-4 rounded-full" />
                                <span class="truncate">{{ m.name || m.id }}</span>
                              </span>
                              <span
                                class="ml-2 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px]"
                                :class="recoveredMemberIdsForProcess(getProcessByType(vault, type)!).has(m.id)
                                  ? 'bg-green-50 text-green-700 ring-1 ring-green-200'
                                  : 'bg-gray-50 text-gray-600 ring-1 ring-gray-200'"
                              >
                                <span
                                  class="h-2 w-2 rounded-full"
                                  :class="recoveredMemberIdsForProcess(getProcessByType(vault, type)!).has(m.id) ? 'bg-green-500' : 'bg-gray-300'"
                                ></span>
                                {{ recoveredMemberIdsForProcess(getProcessByType(vault, type)!).has(m.id)
                                  ? t('recoveryDialog.status.added')
                                  : t('recoveryDialog.status.pending') }}
                              </span>
                            </li>
                          </ul>
                        </template>

                        <!-- Startable process -->
                        <template v-else>
                          <div class="flex items-center justify-between mb-1">
                            <div class="text-xl">{{ getTypeLabel(vault, type) }}</div>
                          </div>
                          <div class="text-xs text-gray-500 mb-2">
                            {{ t('recoveryDialog.requiredKeyShares') }}:
                            {{ vault!.requiredEmergencyKeyShares }}
                          </div>
                          <div>
                            Vault council
                          </div>
                          <ul class="space-y-1 max-h-56 overflow-auto pr-1">
                            <li
                              v-for="m in getCurrentCouncilMembers(vault)"
                              :key="'hc-start-' + vault.id + '-' + type + '-' + m.id"
                              class="flex items-center justify-between text-sm h-6"
                            >
                              <span class="truncate flex items-center gap-2">
                                <img v-if="getAvatarUrl(m)" :src="getAvatarUrl(m)" :alt="m.name" class="h-4 w-4 rounded-full" />
                                <span class="truncate">{{ m.name }}</span>
                              </span>
                            </li>
                          </ul>
                        </template>
                      </div>
                    </div>
                  </template>
                </div>

              </div>
            </div>
          </div>
          <!-- TODO: remove this dev area -->
          <div v-if="getProcesses(vault.id).length" class="px-4 pb-4 sm:px-6" hidden="true">
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
    :start-type="startType"
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
import { CheckIcon, ChevronUpDownIcon, ExclamationTriangleIcon, PlayIcon } from '@heroicons/vue/24/solid';
import userdata from '../../common/userdata';
import { UserDto } from '../../common/backend';
import { describeSegment } from '../../common/svgUtils';
import EmergencyAccessDialog from './EmergencyAccessDialog.vue';
import SegmentRing from './SegmentRing.vue';

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
  loadDefaultSettings();
  try {
    me.value = await userdata.me;

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
    return 'Approved';
  else {
    if (isProcessFullyApproved(proc) || isProcessAboutToComplete(proc)) {
      return 'Complete';
    }
    else 
      return 'Approve';
  }
}

function getProcessByType(vault: VaultDto, type: RecoveryProcessDto['type']): RecoveryProcessDto | undefined {
  return getProcesses(vault.id).find(p => p.type === type);
}

function getTypeLabel(vault: VaultDto, type: RecoveryProcessDto['type']) {
  return type === 'ASSIGN_OWNER'
    ? t('emergencyAccessVaultList.assignOwner')
    : ( allowChoosingEmergencyCouncil.value ? t('emergencyAccessVaultList.changeCouncil') : 'Reset Council');
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
