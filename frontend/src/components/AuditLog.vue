<template>
  <div v-if="auditEvents == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else>
    <div class="flex flex-col sm:flex-row sm:justify-between gap-3 pb-5 border-b border-gray-200 w-full">
      <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
        {{ t('auditLog.title') }}
      </h2>

      <div class="flex gap-3">
        <button role="button" class="w-full bg-primary py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="refreshData()">
          {{ t('common.refresh') }}
        </button>
        <PopoverGroup class="flex items-baseline space-x-8">
          <Popover as="div" class="relative inline-block text-left">
            <PopoverButton class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">
              <span>{{ t('auditLog.filter') }}</span>
              <ChevronDownIcon class="-mr-1 ml-2 h-5 w-5" aria-hidden="true" />
            </PopoverButton>

            <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
              <PopoverPanel class="absolute right-0 z-10 mt-2 origin-top-right rounded-md bg-white p-4 shadow-2xl ring-1 ring-black ring-opacity-5 focus:outline-none w-80">
                <form class="space-y-4">
                  <div class="sm:grid sm:grid-cols-2 sm:items-baseline sm:gap-2">
                    <label for="filter-start-date" class="block text-sm font-medium text-gray-700">
                      {{ t('auditLog.filter.startDate') }}
                    </label>
                    <input id="filter-start-date" v-model="startDateFilter" type="text" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': !startDateFilterIsValid }" placeholder="yyyy-MM-dd" />
                  </div>
                  <div class="sm:grid sm:grid-cols-2 sm:items-baseline sm:gap-2">
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
                  <AuditLogCreateVaultEventDetails v-if="auditEvent.type == 'CREATE_VAULT'" :event="auditEvent" />
                  <AuditLogUnlockVaultEventDetails v-else-if="auditEvent.type == 'UNLOCK_VAULT'" :event="auditEvent" />
                  <AuditLogUpdateVaultMembershipDetails v-else-if="auditEvent.type == 'UPDATE_VAULT_MEMBERSHIP'" :event="auditEvent" />
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Popover, PopoverButton, PopoverGroup, PopoverPanel } from '@headlessui/vue';
import { ChevronDownIcon } from '@heroicons/vue/20/solid';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AuditEventDto } from '../common/backend';
import AuditLogCreateVaultEventDetails from './AuditLogCreateVaultEventDetails.vue';
import AuditLogUnlockVaultEventDetails from './AuditLogUnlockVaultEventDetails.vue';
import AuditLogUpdateVaultMembershipDetails from './AuditLogUpdateVaultMembershipDetails.vue';
import FetchError from './FetchError.vue';

const { t } = useI18n({ useScope: 'global' });

const auditEvents = ref<AuditEventDto[]>();
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

const currentPage = ref(0);
const pageSize = ref(20);
const paginationBegin = computed(() => auditEvents.value ? currentPage.value * pageSize.value + Math.min(1, auditEvents.value.length) : 0);
const paginationEnd = computed(() => auditEvents.value ? currentPage.value * pageSize.value + auditEvents.value.length : 0);
const hasNextPage = ref(false);
let lastIdOfPreviousPage = [0];

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    // Fetch one more event than the page size to determine if there is a next page
    const events = await backend.auditLogs.getAllEvents(startDate.value, endDate.value, lastIdOfPreviousPage[currentPage.value], pageSize.value + 1);
    // If the lastIdOfPreviousPage for the first page has not been set yet, set it to an id "before" the first event
    if (currentPage.value == 0 && lastIdOfPreviousPage[0] == 0 && events.length > 0) {
      lastIdOfPreviousPage[0] = events[0].id - 1;
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
  lastIdOfPreviousPage = [0];
  currentPage.value = 0;
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
