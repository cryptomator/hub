<template>
  <div v-if="state == State.Initial || state == State.Processing">
    <form ref="form" novalidate @submit.prevent="registerDevice()">
      <div class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">Register Device</h3>
              <p class="mt-1 text-sm text-gray-500">
                This device is not yet known to Cryptomator Hub. Before a vault owner can grant access, you need to verify this device.
              </p>
            </div>
            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-3">
                  <label for="verificationCode" class="block text-sm font-medium text-gray-700">Verification Code</label>
                  <input id="verificationCode" v-model="verificationCode" :disabled="state == State.Processing" type="text" inputmode="numeric" pattern="[0-9]{6}" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
                </div>

                <div class="col-span-6 sm:col-span-4">
                  <label for="deviceName" class="block text-sm font-medium text-gray-700">Device Name</label>
                  <input id="deviceName" v-model="deviceName" :disabled="state == State.Processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" required />
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="px-4 py-3 bg-gray-50 text-right sm:px-6">
          <button :disabled="!validDeviceName || !validVerificationCode || state == State.Processing" type="submit" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
            Register Device
          </button>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.Finished">
    <div class="flex justify-center">
      <div class="shadow sm:rounded-lg sm:overflow-hidden sm:max-w-lg">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100">
            <CheckIcon class="h-6 w-6 text-green-600" aria-hidden="true" />
          </div>
          <div class="mt-3 text-center sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              Device registration successful
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                The vault owner will be notified about your registration. Please wait until they grant access. You can now close this window.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckIcon } from '@heroicons/vue/outline';
import { base64url } from 'rfc4648';
import { computed, ref, watch } from 'vue';
import backend from '../common/backend';

enum State {
  Initial,
  Processing,
  Finished
}

const props = defineProps<{
  deviceId: string
  deviceKey: string
  verificationHash: string
}>();

const form = ref<HTMLFormElement>();

const state = ref(State.Initial);
const deviceName = ref('');
const verificationCode = ref('');

const validDeviceName = computed(() => deviceName.value.length > 0);
const validVerificationCode = ref(false);
watch(verificationCode, async (code) => {
  validVerificationCode.value = await verifyCode(code);
});

async function registerDevice() {
  state.value = State.Processing;
  const response = await backend.devices.createDevice(props.deviceId, deviceName.value, props.deviceKey);
  if (response.status == 201) {
    state.value = State.Finished;
  } else {
    state.value = State.Initial;
    console.error('Registering device failed with status ' + response.status + ': ' + response.statusText);
  }
}

async function verifyCode(code: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const data = encoder.encode(props.deviceId + props.deviceKey + code);
  const hash = await crypto.subtle.digest('SHA-256', data);
  const encodedHash = base64url.stringify(new Uint8Array(hash)).replaceAll('=', '');
  return encodedHash === props.verificationHash;
}
</script>
