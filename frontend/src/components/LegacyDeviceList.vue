<template>
  <div v-if="me == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-if="me?.devices && me.devices.length > 0">
    <h2 id="legacyDeviceListTitle" class="text-base font-semibold leading-6 text-gray-900">
      {{ t('legacyDeviceList.title') }}
    </h2>

    <p class="mt-1 text-sm text-gray-500">
      {{ t('legacyDeviceList.description') }}
      <a href="https://docs.cryptomator.org/hub/your-account/#legacy-devices" target="_blank" class="inline-flex items-center text-primary underline hover:text-primary-darker">
        Learn more
        <ArrowRightIcon class="ml-1 h-4 w-4" aria-hidden="true" />
      </a>
    </p>

    <div class="mt-4 flex flex-col">
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="legacyDeviceListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th scope="col" class="w-12 px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"></th>
                  <th scope="col" class="px-2 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider flex-grow">
                    {{ t('legacyDeviceList.deviceName') }}
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('legacyDeviceList.added') }}
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    <span class="inline-flex items-center gap-1">
                      {{ t('legacyDeviceList.lastAccess') }}
                      <div class="relative group" :title="t('legacyDeviceList.lastAccess.toolTip')">
                        <QuestionMarkCircleIcon class="h-4 w-4 text-gray-400"/>
                      </div>
                    </span>
                  </th>
                  <th scope="col" class="relative px-6 py-3">
                    <span class="sr-only">{{ t('common.remove') }}</span>
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="device in sortedDevices" :key="device.id">
                  <tr>
                    <td class="py-4 text-sm text-gray-500">
                      <div class="grid place-items-center h-12 aspect-square">
                        <span v-if="device.type == 'DESKTOP'" :title="'Desktop'">
                          <ComputerDesktopIcon class="size-5" aria-hidden="true" />
                        </span>
                      </div>
                    </td>
                    <td class="px-2 py-4 whitespace-nowrap text-sm font-medium text-gray-900 flex-grow">
                      <div class="flex items-center gap-3">
                        <div>{{ device.name }}</div>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {{ d(device.creationTime, 'long') }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <div v-if="device.lastAccessTime">
                        {{ d(device.lastAccessTime, 'long') }}
                      </div>
                      <div v-if="device.lastIpAddress" class="text-xs text-gray-400">
                        {{ device.lastIpAddress }}
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <a tabindex="0" class="text-red-600 hover:text-red-900" @click="removeDevice(device)">{{ t('common.remove') }}</a>
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
import { ArrowRightIcon, ComputerDesktopIcon, QuestionMarkCircleIcon } from '@heroicons/vue/24/solid';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { DeviceDto, NotFoundError, UserDto } from '../common/backend';
import userdata from '../common/userdata';
import FetchError from './FetchError.vue';

const { t, d } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const onFetchError = ref<Error | null>();
const onRemoveDeviceError = ref< {[id: string]: Error} >({});

onMounted(async () => {
  await fetchData();
});

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await userdata.meWithLegacyDevicesAndLastAccess;
  } catch (error) {
    console.error('Retrieving legacy device list failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function removeDevice(device: DeviceDto) {
  delete onRemoveDeviceError.value[device.id];
  try {
    await backend.devices.removeLegacyDevice(device.id);
    userdata.reload();
  } catch (error) {
    console.error('Removing legacy device failed.', error);
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

const sortedDevices = computed(() => {
  return (me.value?.devices || []).slice().sort((a, b) => {
    const aCreationTime = new Date(a.creationTime).getTime();
    const bCreationTime = new Date(b.creationTime).getTime();
    return bCreationTime - aCreationTime;
  });
});

</script>
