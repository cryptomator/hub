<template>
  <div v-if="me == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else-if="me.devices.length == 0" class="text-center">
    <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
      <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
    </svg>
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('deviceList.empty.message') }}</h3>
    <p class="mt-1 text-sm text-gray-500">{{ t('deviceList.empty.description') }}</p>
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
                        <ComputerDesktopIcon class="mr-1 h-5 w-5" aria-hidden="true" />
                        Computer
                      </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {{ d(device.creationTime, 'short') }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <a role="button" tabindex="0" class="text-red-600 hover:text-red-900" @click="removeDevice(device)">{{ t('common.remove') }}</a>
                      <a v-if="!device.userKeyJwe" role="button" tabindex="0" class="text-primary hover:text-primary-d1" @click="validateDevice(device)">TODO confirm</a>
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
import { ComputerDesktopIcon } from '@heroicons/vue/24/solid';
import { base64, base64url } from 'rfc4648';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { DeviceDto, NotFoundError, UserDto } from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import FetchError from './FetchError.vue';

const { t, d } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const onFetchError = ref<Error | null>();
const onRemoveDeviceError = ref< {[id: string]: Error} >({});

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me(true);
  } catch (error) {
    console.error('Retrieving device list failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function validateDevice(device: DeviceDto) {
  if (!me.value || !me.value.publicKey) {
    throw new Error('User keys not initialized.');
  }
  /* decrypt user key on this browser: */
  const userPublicKey = crypto.subtle.importKey('spki', base64.parse(me.value.publicKey), {
    name: 'ECDH',
    namedCurve: 'P-384'
  }, false, []);
  const browserKeys = await BrowserKeys.load(me.value.id);
  const browserId = await browserKeys.id();
  const browser = me.value.devices.find(d => d.id === browserId);
  if (!browser || !browser.userKeyJwe) {
    throw new Error('Browser not validated.');
  }
  const userKeys = await UserKeys.decryptOnBrowser(browser.userKeyJwe, browserKeys.keyPair.privateKey, await userPublicKey);

  /* encrypt user key for device */
  device.userKeyJwe = await userKeys.encryptForDevice(base64url.parse(device.publicKey));
  await backend.devices.putDevice(device);
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
