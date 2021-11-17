<template>
  <div class="space-y-6 mt-12">
    <div class="bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6">
      <div class="md:grid md:grid-cols-3 md:gap-6">
        <div class="md:col-span-1">
          <h3 class="text-lg font-medium leading-6 text-gray-900">Register Device</h3>
          <p class="mt-1 text-sm text-gray-500">
            This device is not yet known to Cryptomator Hub. Before a vault owner can grant access, you need to verify this device.
          </p>
        </div>
        <div class="mt-5 md:mt-0 md:col-span-2">
          <form ref="form" novalidate @submit.prevent="createDevice()">
            <div class="grid grid-cols-6 gap-6">
              <div class="col-span-6 sm:col-span-3">
                <label for="verificationCode" class="block text-sm font-medium text-gray-700">Verification Code</label>
                <input id="verificationCode" v-model="verificationCode" type="text" inputmode="numeric" pattern="[0-9]{6}" class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
              </div>

              <div class="col-span-6 sm:col-span-4">
                <label for="deviceName" class="block text-sm font-medium text-gray-700">Device Name</label>
                <input id="deviceName" v-model="deviceName" type="text" class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" required />
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>

    <div class="flex justify-end">
      <button :disabled="!validDeviceName || !validVerificationCode" type="submit" class="ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed">
        Register Device
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { base64url } from 'rfc4648';
import { computed, ref, watch } from 'vue';
import backend from '../common/backend';

const props = defineProps<{
  deviceId: string
  deviceKey: string
  verificationHash: string
}>();

const form = ref<HTMLFormElement>();
const deviceName = ref('');
const verificationCode = ref('');

const validDeviceName = computed(() => deviceName.value.length > 0);
const validVerificationCode = ref(false);
watch(verificationCode, async (code) => {
  validVerificationCode.value = await verifyCode(code);
});

async function createDevice() {
  await backend.devices.createDevice(props.deviceId, deviceName.value, props.deviceKey);
}

async function verifyCode(code: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const data = encoder.encode(props.deviceId + props.deviceKey + code);
  const hash = await crypto.subtle.digest('SHA-256', data);
  const encodedHash = base64url.stringify(new Uint8Array(hash)).replaceAll('=', '');
  return encodedHash === props.verificationHash;
}
</script>
