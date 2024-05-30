<template>
  <div v-if="setupCode == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>
  
  <div v-else>
    <h2 class="text-base font-semibold leading-6 text-gray-900">
      {{ t('manageAccountKey.title') }}
    </h2>
    <p class="mt-1 text-sm text-gray-500">
      {{ t('manageAccountKey.description') }}
    </p>

    <div class="mt-4 bg-white rounded-md shadow-sm flex w-full">
      <div class="rounded-none rounded-l-md px-3 py-2 ring-1 ring-inset ring-gray-300 focus-within:ring-2 focus-within:ring-primary focus-within:z-10 w-full">
        <label for="setupCode" class="sr-only">{{ t('manageAccountKey.title') }}</label>
        <input id="setupCode" v-model="setupCode" :type="setupCodeInputType" name="setupCode" class="block w-full border-0 p-0 text-gray-900 font-mono text-lg placeholder:text-gray-400 focus:ring-0" readonly />
      </div>
      <button type="button" class="relative -ml-px inline-flex items-center gap-x-1.5 px-4 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50" @click="toggleSetupCodeVisibility()">
        <EyeIcon v-if="setupCodeInputType == 'password'" class="-ml-0.5 h-5 w-5 text-gray-400" aria-hidden="true" />
        <EyeSlashIcon v-else class="-ml-0.5 h-5 w-5 text-gray-400" aria-hidden="true" />
        <span v-if="setupCodeInputType == 'password'">{{ t('common.show') }}</span>
        <span v-else>{{ t('common.hide') }}</span>
      </button>
      <button type="button" class="relative -ml-px inline-flex items-center gap-x-1.5 rounded-r-md px-4 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50" @click="copySetupCode()">
        <ClipboardIcon class="-ml-0.5 h-5 w-5 text-gray-400" aria-hidden="true" />
        <span v-if="!copiedSetupCode">{{ t('common.copy') }}</span>
        <span v-else>{{ t('common.copied') }}</span>
      </button>
    </div>

    <div class="flex justify-end mt-4">
      <button type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showRegenerateSetupCodeDialog()">
        {{ t('manageAccountKey.regenerate') }}
      </button>
    </div>
  </div>

  <RegenerateSetupCodeDialog v-if="regeneratingSetupCode && setupCode != null" ref="regenerateSetupCodeDialog" v-model:setup-code="setupCode" @close="regeneratingSetupCode = false" />
</template>

<script setup lang="ts">
import { ClipboardIcon, EyeIcon, EyeSlashIcon } from '@heroicons/vue/20/solid';
import { base64 } from 'rfc4648';
import { nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import { JWEParser } from '../common/jwe';
import { debounce } from '../common/util';
import FetchError from './FetchError.vue';
import RegenerateSetupCodeDialog from './RegenerateSetupCodeDialog.vue';
const { t } = useI18n({ useScope: 'global' });

const setupCode = ref<string>();
const setupCodeInputType = ref<'password' | 'text'>('password');
const copiedSetupCode = ref(false);
const debouncedCopyFinish = debounce(() => copiedSetupCode.value = false, 2000);
const regeneratingSetupCode = ref(false);
const regenerateSetupCodeDialog = ref<typeof RegenerateSetupCodeDialog>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    const me = await backend.users.me(true);
    if (me.ecdhPublicKey == null || me.ecdsaPublicKey == null || me.setupCode == null) {
      throw new Error('User not initialized.');
    }
    const browserKeys = await BrowserKeys.load(me.id);
    if (browserKeys == null) {
      throw new Error('Browser keys not found.');
    }
    const browserId = await browserKeys.id();
    const myDevice = me.devices.find(d => d.id == browserId);
    if (myDevice == null) {
      throw new Error('Device not initialized.');
    }
    const userKeys = await UserKeys.decryptOnBrowser(myDevice.userPrivateKeys, browserKeys.keyPair.privateKey, base64.parse(me.ecdhPublicKey), base64.parse(me.ecdsaPublicKey));
    const payload : { setupCode: string } = await JWEParser.parse(me.setupCode).decryptEcdhEs(userKeys.ecdhKeyPair.privateKey);
    setupCode.value = payload.setupCode;
  } catch (error) {
    console.error('Retrieving setup code failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function toggleSetupCodeVisibility() {
  setupCodeInputType.value = setupCodeInputType.value == 'password' ? 'text' : 'password';
}

async function copySetupCode() {
  if (setupCode.value == null) {
    throw new Error('Invalid state.');
  }
  await navigator.clipboard.writeText(setupCode.value);
  copiedSetupCode.value = true;
  debouncedCopyFinish();
}

async function showRegenerateSetupCodeDialog() {
  regeneratingSetupCode.value = true;
  nextTick(() => regenerateSetupCodeDialog.value?.show());
}
</script>
