<template>
  <div v-if="me == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else-if="me.devices.length == 0">
    {{ t('deviceList.notFound') }}
  </div>

  <div v-else>
    <div class="pb-5 border-b border-gray-200">
      <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
        {{ t('deviceList.title', [me.devices.length]) }}
      </h2>
    </div>

    <div class="mt-5 flex flex-col">
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('deviceList.deviceName') }}
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('deviceList.type') }}
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('deviceList.numberOfSharedVaults') }}
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('deviceList.added') }}
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
                      {{ device.name }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <!-- TODO: actual type -->
                      <span class="inline-flex items-center">
                        <DesktopComputerIcon class="mr-1 h-5 w-5" aria-hidden="true" />
                        Computer
                      </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {{ device.accessTo.length }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <!-- TODO: actual added date -->
                      June 8, 2020
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <a role="button" tabindex="0" class="text-red-600 hover:text-red-900" @click="removeDevice(device)">{{ t('common.remove') }}</a>
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
import { DesktopComputerIcon } from '@heroicons/vue/solid';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { DeviceDto, NotFoundError, UserDto } from '../common/backend';
import FetchError from './FetchError.vue';

const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const onFetchError = ref<Error | null>();
const onRemoveDeviceError = ref< {[id: string]: Error} >({});

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me(true, true);
  } catch (err) {
    console.error('Retrieving device list failed.', err);
    onFetchError.value = err instanceof Error ? err : new Error('Unknown Error');
  }
}

async function removeDevice(device: DeviceDto) {
  delete onRemoveDeviceError.value[device.id];
  try {
    await backend.devices.removeDevice(device.id);
  } catch (error) {
    console.error('Removing device failed.', error);
    if (error instanceof NotFoundError) {
      // if device is already missing in backend → ignore and proceed to then()
    } else {
      let e = error instanceof Error ? error : new Error('Unknown Error');
      onRemoveDeviceError.value[device.id] = e;
      throw e;
    }
  }
  await fetchData(); // already handle errors
}
</script>
