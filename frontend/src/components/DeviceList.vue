<template>
  <div v-if="me == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-if="me?.devices">
    <h2 id="deviceListTitle" class="text-base font-semibold leading-6 text-gray-900">
      {{ t('deviceList.title') }}
    </h2>

    <div class="mt-4 flex flex-col">
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="deviceListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('deviceList.deviceName') }}
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('deviceList.type') }}
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('deviceList.added') }}
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap" :title="t('deviceList.lastAccess.toolTip')">
                    <span class="inline-flex items-center gap-1">
                      {{ t('deviceList.lastAccess') }}
                      <div class="relative group">
                        <QuestionMarkCircleIcon class="h-4 w-4 text-gray-400 cursor-help"/>
                      </div>
                    </span>
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap" :title="t('deviceList.ipAddress.toolTip')">
                    <span class="inline-flex items-center gap-1">
                      {{ t('deviceList.ipAddress') }}
                      <span class="relative group">
                        <QuestionMarkCircleIcon class="h-4 w-4 text-gray-400 cursor-help" />
                      </span>
                    </span>
                  </th>
                  <th scope="col" class="relative px-6 py-3">
                    <span class="sr-only">{{ t('common.remove') }}</span>
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="device in me.devices" :key="device.id">
                  <tr>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      <div class="flex items-center gap-3">
                        <div>{{ device.name }}</div>
                        <div v-if="device.id == myDevice?.id" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20">{{ t('deviceList.thisDevice') }}</div>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <span v-if="device.type == 'BROWSER'" class="flex items-center">
                        <WindowIcon class="mr-1 h-5 w-5" aria-hidden="true" />
                        Browser
                      </span>
                      <span v-else-if="device.type == 'DESKTOP'" class="flex items-center">
                        <ComputerDesktopIcon class="mr-1 h-5 w-5" aria-hidden="true" />
                        Desktop
                      </span>
                      <span v-else-if="device.type == 'MOBILE'" class="flex items-center">
                        <DevicePhoneMobileIcon class="mr-1 h-5 w-5" aria-hidden="true" />
                        Mobile
                      </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {{ d(device.creationTime, 'short') }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <div v-if="device.lastAccessTime">{{ d(device.lastAccessTime, 'short') }}</div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <div v-if="device.lastIpAddress">{{ device.lastIpAddress }}</div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <a v-if="device.id != myDevice?.id" tabindex="0" class="text-red-600 hover:text-red-900" @click="removeDevice(device)">{{ t('common.remove') }}</a>
                    </td>
                  </tr>
                  <!-- TODO: good styling -->
                  <tr v-if="onRemoveDeviceError[device.id] != null" class="bg-red-50">
                    <td colspan="5" class="px-6 py-3 text-center text-xs font-medium text-red-500 uppercase tracking-wider">
                      {{ t('common.unexpectedError', [onRemoveDeviceError[device.id].message]) }}
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ComputerDesktopIcon, DevicePhoneMobileIcon, QuestionMarkCircleIcon, WindowIcon } from '@heroicons/vue/24/solid';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { DeviceDto, NotFoundError, UserDto } from '../common/backend';
import userdata from '../common/userdata';
import FetchError from './FetchError.vue';

const { t, d } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const myDevice = ref<DeviceDto>();
const onFetchError = ref<Error | null>();
const onRemoveDeviceError = ref< {[id: string]: Error} >({});

onMounted(async () => {
  await fetchData();
});

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await userdata.meWithLastAccess;
    myDevice.value = await userdata.browser;
  } catch (error) {
    console.error('Retrieving device list failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function removeDevice(device: DeviceDto) {
  delete onRemoveDeviceError.value[device.id];
  try {
    await backend.devices.removeDevice(device.id);
    userdata.reload();
  } catch (error) {
    console.error('Removing device failed.', error);
    if (error instanceof NotFoundError) {
      // if device is already missing in backend → ignore and proceed to then()
    } else {
      const e = error instanceof Error ? error : new Error('Unknown Error');
      onRemoveDeviceError.value[device.id] = e;
      throw e;
    }
  }
  await fetchData(); // already handle errors
}
</script>
