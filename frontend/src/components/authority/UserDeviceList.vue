<template>
  <section v-if="visible" class="bg-white rounded-lg shadow-sm overflow-hidden">
    <div class="flex items-center bg-gray-50 px-6 py-4 border-b border-gray-200 space-x-2">
      <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
        {{ title }}
      </h3>
      <div v-if="info != ''" class="relative group" :title="t('user.detail.legacyDeviceList.info')">
        <QuestionMarkCircleIcon class="h-4 w-4 text-gray-400"/>
      </div>
    </div>

    <!-- Search bar -->
    <div class="px-6 py-3 border-b border-gray-200">
      <input id="legacyDeviceSearch" v-model="deviceQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
    </div>
    <div>
      <table class="w-full table-fixed divide-y divide-gray-200" aria-describedby="deviceListTitle">
        <!-- Desktop Header -->
        <thead v-if="filteredDevices.length != 0" class="bg-gray-50 hidden sm:table-header-group">
          <tr>
            <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {{ t('common.device') }}
            </th>
            <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {{ t('legacyDeviceList.added') }}
            </th>
            <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              <span class="inline-flex items-center gap-1">
                {{ t('legacyDeviceList.lastAccess') }}
                <div class="relative group" :title="t('legacyDeviceList.lastAccess.toolTip')">
                  <QuestionMarkCircleIcon class="h-4 w-4 text-gray-400"/>
                </div>
              </span>
            </th>
          </tr>
        </thead>

        <tbody class="bg-white divide-y divide-gray-200">
          <template v-for="device in paginatedDevices" :key="device.id">
            <!-- Desktop -->
            <tr class="hidden sm:table-row">
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                <div class="flex items-center gap-2">
                  <span v-if="device.type == 'TABLET'" :title="'Browser'">
                    <WindowIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                  </span>
                  <span v-else-if="device.type == 'DESKTOP'" :title="'Desktop'">
                    <ComputerDesktopIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                  </span>
                  <span v-else-if="device.type == 'MOBILE'" :title="'Mobile'">
                    <DevicePhoneMobileIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                  </span>
                  <span class="truncate max-w-xs" :title="device.name">
                    {{ device.name }}
                  </span>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ new Date(device.creationTime).toISOString().slice(0, 16).replace('T', ' ') }}
              </td>
              <td class="h-17 px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                <div v-if="device.lastAccessTime">
                  {{ new Date(device.lastAccessTime).toISOString().slice(0, 16).replace('T', ' ') }}
                </div>
                <div v-if="device.lastIpAddress" class="text-xs text-gray-400">
                  {{ device.lastIpAddress }}
                </div>
              </td>
            </tr>
            <!-- Mobile -->
            <tr class="sm:hidden">
              <td class="px-4 py-4 text-sm text-gray-900">
                <div class="flex items-top gap-2">
                  <span v-if="device.type == 'TABLET'" :title="'Browser'">
                    <WindowIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                  </span>
                  <span v-else-if="device.type == 'DESKTOP'" :title="'Desktop'">
                    <ComputerDesktopIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                  </span>
                  <span v-else-if="device.type == 'MOBILE'" :title="'Mobile'">
                    <DevicePhoneMobileIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                  </span>
                  <div>
                    <div class="font-medium truncate max-w-xs" :title="device.name">
                      {{ device.name }}
                    </div>
                  </div>
                </div>
                <table class="text-xs text-gray-400 mt-2 ml-8">
                  <tbody>
                    <tr>
                      <td class="pr-2 align-top text-left whitespace-nowrap font-normal text-gray-500">
                        {{ t('legacyDeviceList.added') }}
                      </td>
                      <td class="text-left">
                        {{ new Date(device.creationTime).toISOString().slice(0, 16).replace('T', ' ') }}
                      </td>
                    </tr>
                    <tr>
                      <td class="inline-flex pr-2 align-top text-left whitespace-nowrap font-normal text-gray-500">
                        {{ t('legacyDeviceList.lastAccess') }}
                        <div class="relative group" :title="t('legacyDeviceList.lastAccess.toolTip')">
                          <QuestionMarkCircleIcon class="h-3 w-3 text-gray-400 m-0.5"/>
                        </div>
                      </td>
                      <td v-if="device.lastAccessTime" class="text-left">
                        {{ new Date(device.lastAccessTime).toISOString().slice(0, 16).replace('T', ' ') }}
                      </td>
                    </tr>
                    <tr v-if="device.lastIpAddress">
                      <td class="pr-2 align-top text-right whitespace-nowrap font-normal text-gray-500">
                      </td>
                      <td class="text-left">
                        {{ device.lastIpAddress }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </td>
            </tr>
          </template>

          <tr v-if="!filteredDevices.length">
            <td colspan="5" class="py-4 px-6 text-sm text-gray-500 text-center">
              {{ t(deviceQuery ? 'common.nothingFound' : 'common.none') }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="showPaginationDevice" class="bg-gray-50 border-t border-gray-200">
      <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
        <div class="hidden sm:block">
          <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
            <span class="font-medium">{{ paginationBeginDevice }}</span>
            <span class="font-medium">{{ paginationEndDevice }}</span>
          </i18n-t>
        </div>
        <div class="flex flex-1 justify-between sm:justify-end space-x-3">
          <button v-if="currentPageDevice > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPageDevice">
            {{ t('common.previous') }}
          </button>
          <button v-if="hasNextPageDevice" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPageDevice">
            {{ t('common.next') }}
          </button>
        </div>
      </nav>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ComputerDesktopIcon, QuestionMarkCircleIcon, DevicePhoneMobileIcon, WindowIcon } from '@heroicons/vue/24/solid';

const { t } = useI18n({ useScope: 'global' });

const props = withDefaults(defineProps<{
  devices: Device[];
  pageSize: number;
  visible?: boolean;
  title: string;
  info?: string;
}>(), {
  visible: true,
  info: ''
});
interface Device {
  id: string;
  name: string;
  type: 'DESKTOP' | 'MOBILE' | 'TABLET';
  creationTime: string;
  lastAccessTime?: string;
  lastIpAddress?: string;
}

// ---------------------------------------------------------------------------
// DEVICES – search & pagination
// ---------------------------------------------------------------------------

// Filter und Pagination
const deviceQuery = ref('');
const filteredDevices = computed(() =>
  props.devices.filter((device) =>
    device.name.toLowerCase().includes(deviceQuery.value.toLowerCase())
  )
);

const currentPageDevice = ref(0);

const paginatedDevices = computed(() =>
  filteredDevices.value.slice(
    currentPageDevice.value * props.pageSize,
    (currentPageDevice.value + 1) * props.pageSize
  )
);

const showPaginationDevice = computed(() => filteredDevices.value.length > props.pageSize);
const hasNextPageDevice = computed(() =>
  (currentPageDevice.value + 1) * props.pageSize < filteredDevices.value.length
);
const paginationBeginDevice = computed(() => currentPageDevice.value * props.pageSize + 1);
const paginationEndDevice = computed(() =>
  Math.min((currentPageDevice.value + 1) * props.pageSize, filteredDevices.value.length)
);

function showPreviousPageDevice() {
  if (currentPageDevice.value > 0) currentPageDevice.value--;
}

function showNextPageDevice() {
  if (hasNextPageDevice.value) currentPageDevice.value++;
}

watch(() => [filteredDevices.value.length, deviceQuery.value], () => (currentPageDevice.value = 0));

</script>