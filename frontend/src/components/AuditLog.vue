<template>
  <div class="flex flex-col sm:flex-row sm:justify-between gap-3 pb-5 border-b border-gray-200 w-full">
    <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
      {{ t('auditLog.title') }}
    </h2>

    <div class="flex gap-3">
      <button role="button" class="w-full bg-primary py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="refreshData()">
        {{ t('common.refresh') }}
      </button>

      <Listbox v-model="selectedOrder" as="div">
        <div class="relative w-36">
          <ListboxButton class="relative w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary text-sm">
            <span class="block truncate">{{ orderOptions[selectedOrder].label }}</span>
            <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
              <ChevronUpDownIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
            </span>
          </ListboxButton>
          <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
            <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none text-sm">
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
        <Popover as="div" class="relative inline-block text-left">
          <PopoverButton class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">
            <span>{{ t('auditLog.filter') }}</span>
            <ChevronDownIcon class="-mr-1 ml-2 h-5 w-5" aria-hidden="true" />
          </PopoverButton>

          <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
            <PopoverPanel class="absolute right-0 z-10 mt-2 origin-top-right rounded-md bg-white p-4 shadow-2xl ring-1 ring-black ring-opacity-5 focus:outline-none w-80">
              <form class="space-y-4">
                <div class="sm:grid sm:grid-cols-2 sm:items-center sm:gap-2">
                  <label for="filter-start-date" class="block text-sm font-medium text-gray-700">
                    {{ t('auditLog.filter.startDate') }}
                  </label>
                  <input id="filter-start-date" v-model="startDateFilter" type="text" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': !startDateFilterIsValid }" placeholder="yyyy-MM-dd" />
                </div>
                <div class="sm:grid sm:grid-cols-2 sm:items-center sm:gap-2">
                  <label for="filter-end-date" class="block text-sm font-medium text-gray-700">
                    {{ t('auditLog.filter.endDate') }}
                  </label>
                  <input id="filter-end-date" v-model="endDateFilter" type="text" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': !endDateFilterIsValid }" placeholder="yyyy-MM-dd" />
                </div>
                <div class="flex flex-col sm:flex-row gap-2 pt-4 border-t border-gray-200">
                  <button type="button" class="w-full border border-gray-300 rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow hover:bg-gray-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-50 disabled:hover:bg-white disabled:cursor-not-allowed" :disabled="filterIsReset" @click="resetFilter()">
                    {{ t('common.reset') }}
                  </button>
                  <button type="button" class="w-full rounded-md bg-primary px-3 py-2 text-sm font-medium text-white shadow hover:bg-primary-d1 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" :disabled="!filterIsValid" @click="applyFilter()">
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
        <div class="overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
          <table class="min-w-full divide-y divide-gray-300">
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
                <AuditLogCreateVaultEventDetails v-if="auditEvent.type == 'CREATE_VAULT'" :event="(auditEvent as CreateVaultEventDto)" />
                <AuditLogGrantVaultAccessDetails v-if="auditEvent.type == 'GRANT_VAULT_ACCESS'" :event="(auditEvent as GrantVaultAccessEventDto)" />
                <AuditLogRegisterDeviceEventDetails v-if="auditEvent.type == 'REGISTER_DEVICE'" :event="(auditEvent as RegisterDeviceEventDto)" />
                <AuditLogRemoveDeviceEventDetails v-if="auditEvent.type == 'REMOVE_DEVICE'" :event="(auditEvent as RemoveDeviceEventDto)" />
                <AuditLogUnlockVaultEventDetails v-else-if="auditEvent.type == 'UNLOCK_VAULT'" :event="(auditEvent as UnlockVaultEventDto)" />
                <AuditLogUpdateVaultEventDetails v-if="auditEvent.type == 'UPDATE_VAULT'" :event="(auditEvent as UpdateVaultEventDto)" />
                <AuditLogUpdateVaultMembershipDetails v-else-if="auditEvent.type == 'UPDATE_VAULT_MEMBERSHIP'" :event="(auditEvent as UpdateVaultMembershipEventDto)" />
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
</template>

<script setup lang="ts">
import { Listbox, ListboxButton, ListboxOption, ListboxOptions, Popover, PopoverButton, PopoverGroup, PopoverPanel } from '@headlessui/vue';
import { ChevronDownIcon } from '@heroicons/vue/20/solid';
import { CheckIcon, ChevronUpDownIcon } from '@heroicons/vue/24/solid';
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventDto, CreateVaultEventDto, GrantVaultAccessEventDto, RegisterDeviceEventDto, RemoveDeviceEventDto, UnlockVaultEventDto, UpdateVaultEventDto, UpdateVaultMembershipEventDto } from '../common/auditlog';
import AuditLogCreateVaultEventDetails from './AuditLogCreateVaultEventDetails.vue';
import AuditLogGrantVaultAccessDetails from './AuditLogGrantVaultAccessDetails.vue';
import AuditLogRegisterDeviceEventDetails from './AuditLogRegisterDeviceEventDetails.vue';
import AuditLogRemoveDeviceEventDetails from './AuditLogRemoveDeviceEventDetails.vue';
import AuditLogUnlockVaultEventDetails from './AuditLogUnlockVaultEventDetails.vue';
import AuditLogUpdateVaultEventDetails from './AuditLogUpdateVaultEventDetails.vue';
import AuditLogUpdateVaultMembershipDetails from './AuditLogUpdateVaultMembershipDetails.vue';

const { t } = useI18n({ useScope: 'global' });

const auditEvents = ref<AuditEventDto[]>([]);
const onFetchError = ref<Error | null>();

const startDate = ref(beginOfDate(new Date()));
const startDateFilter = ref(startDate.value.toISOString().split('T')[0]);
const endDate = ref(endOfDate(new Date()));
const endDateFilter = ref(endDate.value.toISOString().split('T')[0]);

const filterIsReset = computed(() => startDateFilter.value == startDate.value.toISOString().split('T')[0] && endDateFilter.value == endDate.value.toISOString().split('T')[0]);
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

const currentPage = ref(0);
const pageSize = ref(20);
const paginationBegin = computed(() => auditEvents.value ? currentPage.value * pageSize.value + Math.min(1, auditEvents.value.length) : 0);
const paginationEnd = computed(() => auditEvents.value ? currentPage.value * pageSize.value + auditEvents.value.length : 0);
const hasNextPage = ref(false);
let lastIdOfPreviousPage = [Number.MAX_SAFE_INTEGER];

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    // Fetch one more event than the page size to determine if there is a next page
    const events = await auditlog.service.getAllEvents(startDate.value, endDate.value, lastIdOfPreviousPage[currentPage.value], selectedOrder.value, pageSize.value + 1);
    // If the lastIdOfPreviousPage for the first page has not been set yet, set it to an id "before"/"after" the first event
    if (currentPage.value == 0 && lastIdOfPreviousPage[0] == 0 && events.length > 0) {
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
      lastIdOfPreviousPage[currentPage.value + 1] = events[events.length - 1].id;
    }
    auditEvents.value = events;
  } catch (error) {
    console.error('Retrieving audit log events failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function refreshData() {
  lastIdOfPreviousPage = [orderOptions[selectedOrder.value].initialLastIdOfPreviousPage];
  currentPage.value = 0;
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

function resetFilter() {
  startDateFilter.value = startDate.value.toISOString().split('T')[0];
  endDateFilter.value = endDate.value.toISOString().split('T')[0];
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
  currentPage.value++;
  await fetchData();
}

async function showPreviousPage() {
  currentPage.value--;
  await fetchData();
}
</script>
