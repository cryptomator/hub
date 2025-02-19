<template>
  <div v-if="state == State.Loading">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else-if="state == State.ShowAuditLog">
    <div class="flex flex-col sm:flex-row sm:justify-between gap-3 pb-5 border-b border-gray-200 w-full">
      <h2 id="title" class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
        {{ t('auditLog.title') }}
      </h2>

      <div class="flex gap-3">
        <button class="w-full bg-primary py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="refreshData()">
          {{ t('common.refresh') }}
        </button>

        <Listbox v-model="selectedOrder" as="div">
          <div class="relative w-36">
            <ListboxButton class="relative w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-xs focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary text-sm">
              <span class="block truncate">{{ orderOptions[selectedOrder].label }}</span>
              <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                <ChevronUpDownIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
              </span>
            </ListboxButton>
            <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
              <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black/5 focus:outline-hidden text-sm">
                <ListboxOption v-for="(option, key) in orderOptions" :key="key" v-slot="{ active, selected }" class="relative cursor-default select-none py-2 pl-3 pr-9 ui-not-active:text-gray-900 ui-active:text-white ui-active:bg-primary" :value="key">
                  <span :class="[selected ? 'font-semibold' : 'font-normal', 'block truncate']">{{ option.label }}</span>
                  <span v-if="selected" :class="[active ? 'text-white' : 'text-primary', 'absolute inset-y-0 right-0 flex items-center pr-4']">
                    <CheckIcon class="h-5 w-5" aria-hidden="true" />
                  </span>
                </ListboxOption>
              </ListboxOptions>
            </transition>
          </div>
        </Listbox>

        <PopoverGroup class="flex items-baseline space-x-8">
          <Popover v-slot="{ close }" as="div" class="relative inline-block text-left">
            <PopoverButton class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-xs text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">
              <span>{{ t('auditLog.filter') }}</span>
              <ChevronDownIcon class="-mr-1 ml-2 h-5 w-5" aria-hidden="true" />
            </PopoverButton>

            <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
              <PopoverPanel class="absolute right-0 z-10 mt-2 origin-top-right rounded-md bg-white p-4 shadow-2xl ring-1 ring-black/5 focus:outline-hidden w-96">
                <form class="space-y-4">
                  <div class="sm:grid sm:grid-cols-2 sm:items-center sm:gap-2">
                    <label for="filter-start-date" class="block text-sm font-medium text-gray-700">
                      {{ t('auditLog.filter.startDate') }}
                    </label>
                    <input id="filter-start-date" v-model="startDateFilter" type="text" class="shadow-xs focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': !startDateFilterIsValid }" placeholder="yyyy-MM-dd" />
                  </div>
                  <div class="sm:grid sm:grid-cols-2 sm:items-center sm:gap-2">
                    <label for="filter-end-date" class="block text-sm font-medium text-gray-700">
                      {{ t('auditLog.filter.endDate') }}
                    </label>
                    <input id="filter-end-date" v-model="endDateFilter" type="text" class="shadow-xs focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': !endDateFilterIsValid }" placeholder="yyyy-MM-dd" />
                  </div>
                  <div class="sm:grid sm:grid-cols-2 sm:items-center sm:gap-2">
                    <label class="block text-sm font-medium text-gray-700 flex items-center">
                      {{ t('auditLog.type') }}
                      <button 
                        type="button" 
                        class="ml-2 p-1 flex items-center justify-center focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-30 disabled:cursor-not-allowed"
                        :disabled="selectedEventTypes.length === 0"
                        :title="selectedEventTypes.length > 0 ? t('auditLog.filter.clerEventFilter') : ''"
                        @click="selectedEventTypes = []"
                      >
                        <TrashIcon class="h-5 w-5 text-gray-500 hover:text-gray-700 disabled:text-gray-300" aria-hidden="true" />
                      </button>
                    </label>
                  </div>
                  <Listbox v-model="selectedEventTypes" as="div" multiple>
                    <div class="relative w-88">
                      <ListboxButton class="relative w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary text-sm">
                        <div class="flex flex-wrap gap-2">
                          <template v-if="selectedEventTypes.length > 0">
                            <button 
                              v-for="type in selectedEventTypes" 
                              :key="type"
                              class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20"
                              @click.stop="removeEventType(type)"
                            >
                              <span class="mr-1">{{ eventTypeOptions[type] }}</span>
                              <span class="text-green-800 font-bold">&times;</span>
                            </button>
                          </template>
                          <template v-else>
                            <span class="text-gray-500">{{ t('auditLog.filter.selectEventFilter') }}</span>
                          </template>
                        </div>
                        <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                          <ChevronUpDownIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
                        </span>
                      </ListboxButton>
                      <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
                        <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black/5 focus:outline-none text-sm">
                          <ListboxOption 
                            v-for="(label, key) in eventTypeOptions" 
                            :key="key" 
                            v-slot="{ active, selected }" 
                            class="relative cursor-default select-none py-2 pl-3 pr-9 ui-not-active:text-gray-900 ui-active:text-white ui-active:bg-primary" 
                            :value="key"
                          >
                            <span :class="[selected ? 'font-semibold' : 'font-normal', 'block truncate']">{{ label }}</span>
                            <span v-if="selected" :class="['absolute inset-y-0 right-0 flex items-center pr-4', selected ? 'text-primary' : 'text-gray-400']">
                              <CheckIcon class="h-5 w-5" aria-hidden="true" />
                            </span>
                          </ListboxOption>
                        </ListboxOptions>
                      </transition>
                    </div>
                  </Listbox>
                  <div class="flex flex-col sm:flex-row gap-2 pt-4 border-t border-gray-200">
                    <button type="button" class="w-full border border-gray-300 rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-50 disabled:hover:bg-white disabled:cursor-not-allowed" :disabled="filterIsReset" @click="resetFilter()">
                      {{ t('common.reset') }}
                    </button>
                    <button type="button" class="w-full rounded-md bg-primary px-3 py-2 text-sm font-medium text-white shadow-sm hover:bg-primary-d1 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" :disabled="!filterIsValid" @click="async () => { close(); await applyFilter(); }">
                      {{ t('common.apply') }}
                    </button>
                  </div>
                </form>
              </PopoverPanel>
            </transition>
          </Popover>
        </PopoverGroup>
      </div>
    </div>

    <div class="mt-5 flow-root">
      <div class="-mx-4 -my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="inline-block min-w-full py-2 align-middle sm:px-6 lg:px-8">
          <div class="overflow-hidden shadow-sm ring-1 ring-black/5 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-300" aria-describedby="title">
              <thead class="bg-gray-50">
                <tr>
                  <th scope="col" class="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6">
                    {{ t('auditLog.timestamp') }}
                  </th>
                  <th scope="col" class="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">
                    {{ t('auditLog.type') }}
                  </th>
                  <th scope="col" class="py-3.5 pl-3 pr-4 text-left text-sm font-semibold text-gray-900 sm:pr-6">
                    {{ t('auditLog.details') }}
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200 bg-white">
                <tr v-for="auditEvent in auditEvents" :key="auditEvent.id">
                  <td class="whitespace-nowrap py-4 pl-4 pr-3 text-sm text-gray-500 sm:pl-6">
                    <code>{{ auditEvent.timestamp.toLocaleString('sv') }}</code>
                  </td>
                  <AuditLogDetailsDeviceRegister v-if="auditEvent.type == 'DEVICE_REGISTER'" :event="auditEvent" />
                  <AuditLogDetailsDeviceRemove v-else-if="auditEvent.type == 'DEVICE_REMOVE'" :event="auditEvent" />
                  <AuditLogDetailsSettingWotUpdate v-else-if="auditEvent.type == 'SETTING_WOT_UPDATE'" :event="auditEvent" />
                  <AuditLogDetailsSignedWotId v-else-if="auditEvent.type == 'SIGN_WOT_ID'" :event="auditEvent" />
                  <AuditLogDetailsUserAccountReset v-else-if="auditEvent.type == 'USER_ACCOUNT_RESET'" :event="auditEvent" />
                  <AuditLogUserKeysChange v-else-if="auditEvent.type == 'USER_KEYS_CHANGE'" :event="auditEvent" />
                  <AuditLogUserSetupCodeChanged v-else-if="auditEvent.type == 'USER_SETUP_CODE_CHANGE'" :event="auditEvent" />
                  <AuditLogDetailsVaultCreate v-else-if="auditEvent.type == 'VAULT_CREATE'" :event="auditEvent" />
                  <AuditLogDetailsVaultUpdate v-else-if="auditEvent.type == 'VAULT_UPDATE'" :event="auditEvent" />
                  <AuditLogDetailsVaultAccessGrant v-else-if="auditEvent.type == 'VAULT_ACCESS_GRANT'" :event="auditEvent" />
                  <AuditLogDetailsVaultKeyRetrieve v-else-if="auditEvent.type == 'VAULT_KEY_RETRIEVE'" :event="auditEvent" />
                  <AuditLogDetailsVaultMemberAdd v-else-if="auditEvent.type == 'VAULT_MEMBER_ADD'" :event="auditEvent" />
                  <AuditLogDetailsVaultMemberRemove v-else-if="auditEvent.type == 'VAULT_MEMBER_REMOVE'" :event="auditEvent" />
                  <AuditLogDetailsVaultMemberUpdate v-else-if="auditEvent.type == 'VAULT_MEMBER_UPDATE'" :event="auditEvent" />
                  <AuditLogDetailsVaultOwnershipClaim v-else-if="auditEvent.type == 'VAULT_OWNERSHIP_CLAIM'" :event="auditEvent" />
                </tr>
              </tbody>
              <tfoot class="bg-gray-50">
                <tr>
                  <td colspan="3">
                    <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
                      <div class="hidden sm:block">
                        <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
                          <span class="font-medium">{{ paginationBegin }}</span>
                          <span class="font-medium">{{ paginationEnd }}</span>
                        </i18n-t>
                      </div>
                      <div class="flex flex-1 justify-between sm:justify-end">
                        <button type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0 disabled:opacity-50 disabled:hover:bg-white disabled:cursor-not-allowed" :disabled="currentPage == 0" @click="showPreviousPage()">
                          {{ t('common.previous') }}
                        </button>
                        <button type="button" class="relative ml-3 inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0 disabled:opacity-50 disabled:hover:bg-white disabled:cursor-not-allowed" :disabled="!hasNextPage" @click="showNextPage()">
                          {{ t('common.next') }}
                        </button>
                      </div>
                    </nav>
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
          <p v-if="onFetchError != null" class="text-sm text-red-900 mt-2">{{ onFetchError.message }}</p>
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="state == State.PaymentRequired" class="flex flex-col justify-center items-center text-center">
    <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
      <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
    </svg>
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('auditLog.paymentRequired.message') }}</h3>
    <p class="mt-1 text-sm text-gray-500">{{ t('auditLog.paymentRequired.description') }}</p>
    <router-link v-slot="{ navigate }" to="/app/admin/settings" custom>
      <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-xs text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary mt-6" @click="navigate()">
        <WrenchIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
        {{ t('auditLog.paymentRequired.openAdminSection') }}
      </button>
    </router-link>
  </div>
</template>

<script setup lang="ts">
import { Listbox, ListboxButton, ListboxOption, ListboxOptions, Popover, PopoverButton, PopoverGroup, PopoverPanel } from '@headlessui/vue';
import { ChevronDownIcon } from '@heroicons/vue/20/solid';
import { CheckIcon, ChevronUpDownIcon, WrenchIcon, TrashIcon } from '@heroicons/vue/24/solid';
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventDto } from '../common/auditlog';
import { PaymentRequiredError } from '../common/backend';
import AuditLogDetailsDeviceRegister from './AuditLogDetailsDeviceRegister.vue';
import AuditLogDetailsDeviceRemove from './AuditLogDetailsDeviceRemove.vue';
import AuditLogDetailsSettingWotUpdate from './AuditLogDetailsSettingWotUpdate.vue';
import AuditLogDetailsSignedWotId from './AuditLogDetailsSignedWotId.vue';
import AuditLogDetailsUserAccountReset from './AuditLogDetailsUserAccountReset.vue';
import AuditLogDetailsVaultAccessGrant from './AuditLogDetailsVaultAccessGrant.vue';
import AuditLogDetailsVaultCreate from './AuditLogDetailsVaultCreate.vue';
import AuditLogDetailsVaultKeyRetrieve from './AuditLogDetailsVaultKeyRetrieve.vue';
import AuditLogDetailsVaultMemberAdd from './AuditLogDetailsVaultMemberAdd.vue';
import AuditLogDetailsVaultMemberRemove from './AuditLogDetailsVaultMemberRemove.vue';
import AuditLogDetailsVaultMemberUpdate from './AuditLogDetailsVaultMemberUpdate.vue';
import AuditLogDetailsVaultOwnershipClaim from './AuditLogDetailsVaultOwnershipClaim.vue';
import AuditLogDetailsVaultUpdate from './AuditLogDetailsVaultUpdate.vue';
import AuditLogUserKeysChange from './AuditLogUserKeysChange.vue';
import AuditLogUserSetupCodeChanged from './AuditLogUserSetupCodeChanged.vue';
import FetchError from './FetchError.vue';

enum State {
  Loading,
  ShowAuditLog,
  PaymentRequired
}

const { t } = useI18n({ useScope: 'global' });

const state = ref(State.Loading);
const auditEvents = ref<AuditEventDto[]>([]);
const onFetchError = ref<Error | null>();

const startDate = ref(beginOfDate(new Date(new Date().setMonth(new Date().getMonth() - 1))));
const startDateFilter = ref(startDate.value.toISOString().split('T')[0]);
const endDate = ref(endOfDate(new Date()));
const endDateFilter = ref(endDate.value.toISOString().split('T')[0]);

const filterIsReset = computed(() =>
  startDateFilter.value == startDate.value.toISOString().split('T')[0] &&
  endDateFilter.value == endDate.value.toISOString().split('T')[0] &&
  selectedEventTypes.value.length == 0
);
const startDateFilterIsValid = computed(() => validateDateFilterValue(startDateFilter.value) != null);
const endDateFilterIsValid = computed(() => {
  const endDate = validateDateFilterValue(endDateFilter.value);
  if (endDate == null) {
    return false;
  } else if (endDate != null && startDateFilterIsValid.value) {
    const startDate = new Date(startDateFilter.value);
    return startDate <= endDate;
  } else {
    return true;
  }
});
const filterIsValid = computed(() => startDateFilterIsValid.value && endDateFilterIsValid.value);

const selectedOrder = ref<'asc' | 'desc'>('desc');
const orderOptions = {
  desc: {
    label: t('auditLog.order.descending'),
    sign: 1,
    initialLastIdOfPreviousPage: Number.MAX_SAFE_INTEGER,
  },
  asc: {
    label: t('auditLog.order.ascending'),
    sign: -1,
    initialLastIdOfPreviousPage: 0,
  },
};
watch(selectedOrder, refreshData);

const eventTypeOptions = Object.fromEntries(
  Object.entries({
    DEVICE_REGISTER: t('auditLog.details.device.register'),
    DEVICE_REMOVE: t('auditLog.details.device.remove'),
    SETTING_WOT_UPDATE: t('auditLog.details.setting.wot.update'),
    SIGN_WOT_ID: t('auditLog.details.wot.signedIdentity'),
    USER_ACCOUNT_RESET: t('auditLog.details.user.account.reset'),
    USER_KEYS_CHANGE: t('auditLog.details.user.keys.change'),
    USER_SETUP_CODE_CHANGE: t('auditLog.details.user.setupCode.change'),
    VAULT_ACCESS_GRANT: t('auditLog.details.vaultAccess.grant'),
    VAULT_CREATE: t('auditLog.details.vault.create'),
    VAULT_KEY_RETRIEVE: t('auditLog.details.vaultKey.retrieve'),
    VAULT_MEMBER_ADD: t('auditLog.details.vaultMember.add'),
    VAULT_MEMBER_REMOVE: t('auditLog.details.vaultMember.remove'),
    VAULT_MEMBER_UPDATE: t('auditLog.details.vaultMember.update'),
    VAULT_OWNERSHIP_CLAIM: t('auditLog.details.vaultOwnership.claim'),
    VAULT_UPDATE: t('auditLog.details.vault.update')
  }).sort(([,valueA], [,valueB]) => valueA.localeCompare(valueB))
);
const allEventTypes = Object.keys(eventTypeOptions);
const selectedEventTypes = ref<string[]>([]);

const currentPage = ref(0);
const pageSize = ref(20);
const paginationBegin = computed(() => auditEvents.value ? currentPage.value * pageSize.value + Math.min(1, auditEvents.value.length) : 0);
const paginationEnd = computed(() => auditEvents.value ? currentPage.value * pageSize.value + auditEvents.value.length : 0);
const hasNextPage = ref(false);
let lastIdOfPreviousPage = [Number.MAX_SAFE_INTEGER];

onMounted(fetchData);

watch(selectedEventTypes, (newSelection, oldSelection) => {
  selectedEventTypes.value.sort((a, b) => eventTypeOptions[a].localeCompare(eventTypeOptions[b]));
});

async function fetchData(page: number = 0) {
  onFetchError.value = null;
  try {
    // Fetch one more event than the page size to determine if there is a next page
    const events = await auditlog.service.getAllEvents(startDate.value, endDate.value, selectedEventTypes.value, lastIdOfPreviousPage[page], selectedOrder.value, pageSize.value + 1);
    // If the lastIdOfPreviousPage for the first page has not been set yet, set it to an id "before"/"after" the first event
    if (page == 0 && lastIdOfPreviousPage[0] == 0 && events.length > 0) {
      lastIdOfPreviousPage[0] = events[0].id + orderOptions[selectedOrder.value].sign;
    }
    // Determine if there is a next page and discard the last event if there is one
    if (events.length > pageSize.value) {
      hasNextPage.value = true;
      events.pop();
    } else {
      hasNextPage.value = false;
    }
    // Set the lastIdOfPreviousPage for the next page to the id of the last event of the current page
    if (events.length > 0) {
      lastIdOfPreviousPage[page + 1] = events[events.length - 1].id;
    }
    auditEvents.value = events;
    currentPage.value = page;
    state.value = State.ShowAuditLog;
  } catch (error) {
    if (error instanceof PaymentRequiredError) {
      state.value = State.PaymentRequired;
    } else {
      console.error('Retrieving audit log events failed.', error);
      onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
}

async function refreshData() {
  lastIdOfPreviousPage = [orderOptions[selectedOrder.value].initialLastIdOfPreviousPage];
  auditlog.entityCache.invalidateAll();
  await fetchData();
}

async function applyFilter() {
  if (filterIsValid.value) {
    startDate.value = beginOfDate(new Date(startDateFilter.value));
    endDate.value = endOfDate(new Date(endDateFilter.value));
    await fetchData();
  }
}

function removeEventType(type: string): void {
  selectedEventTypes.value = selectedEventTypes.value.filter(t => t !== type);
}

function resetFilter() {
  startDateFilter.value = startDate.value.toISOString().split('T')[0];
  endDateFilter.value = endDate.value.toISOString().split('T')[0];
  selectedEventTypes.value = [];
}

function beginOfDate(date: Date): Date {
  date.setUTCHours(0, 0, 0, 0);
  return date;
}

function endOfDate(date: Date): Date {
  date.setUTCHours(23, 59, 59, 999);
  return date;
}

function validateDateFilterValue(dateFilterValue: string): Date | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dateFilterValue)) {
    return null;
  }
  const date = new Date(dateFilterValue);
  if (isNaN(date.getTime())) {
    return null;
  } else {
    return date;
  }
}

async function showNextPage() {
  await fetchData(currentPage.value + 1);
}

async function showPreviousPage() {
  await fetchData(currentPage.value - 1);
}
</script>
