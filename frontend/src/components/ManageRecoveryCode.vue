<template>
  <div v-if="recoveryCode == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>
  
  <div v-else>
    <h2 class="text-base font-semibold leading-6 text-gray-900">
      {{ t('manageRecoveryCode.title') }}
    </h2>

    <div class="mt-4 bg-white rounded-md shadow-sm flex w-full">
      <div class="rounded-none rounded-l-md px-3 py-2 ring-1 ring-inset ring-gray-300 focus-within:ring-2 focus-within:ring-primary focus-within:z-10 w-full">
        <label for="recoveryCode" class="sr-only">{{ t('manageRecoveryCode.title') }}</label>
        <input id="recoveryCode" v-model="recoveryCode" :type="recoveryCodeInputType" name="recoveryCode" class="block w-full border-0 p-0 text-gray-900 font-mono text-lg placeholder:text-gray-400 focus:ring-0" readonly />
      </div>
      <button type="button" class="relative -ml-px inline-flex items-center gap-x-1.5 px-4 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50" @click="toggleRecoveryCodeVisibility()">
        <EyeIcon v-if="recoveryCodeInputType == 'password'" class="-ml-0.5 h-5 w-5 text-gray-400" aria-hidden="true" />
        <EyeSlashIcon v-else class="-ml-0.5 h-5 w-5 text-gray-400" aria-hidden="true" />
        <span v-if="recoveryCodeInputType == 'password'">{{ t('common.show') }}</span>
        <span v-else>{{ t('common.hide') }}</span>
      </button>
      <button type="button" class="relative -ml-px inline-flex items-center gap-x-1.5 rounded-r-md px-4 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50" @click="copyRecoveryCode()">
        <ClipboardIcon class="-ml-0.5 h-5 w-5 text-gray-400" aria-hidden="true" />
        <span v-if="!copiedRecoveryCode">{{ t('common.copy') }}</span>
        <span v-else>{{ t('common.copied') }}</span>
      </button>
    </div>

    <div class="flex justify-end mt-4">
      <button type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showRegenerateRecoveryCodeDialog()">
        {{ t('manageRecoveryCode.regenerate') }}
      </button>
    </div>
  </div>

  <RegenerateRecoveryCodeDialog v-if="regeneratingRecoveryCode && recoveryCode != null" ref="regenerateRecoveryCodeDialog" v-model:recovery-code="recoveryCode" @close="regeneratingRecoveryCode = false" />
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
import RegenerateRecoveryCodeDialog from './RegenerateRecoveryCodeDialog.vue';
const { t } = useI18n({ useScope: 'global' });

const recoveryCode = ref<string>();
const recoveryCodeInputType = ref<'password' | 'text'>('password');
const copiedRecoveryCode = ref(false);
const debouncedCopyFinish = debounce(() => copiedRecoveryCode.value = false, 2000);
const regeneratingRecoveryCode = ref(false);
const regenerateRecoveryCodeDialog = ref<typeof RegenerateRecoveryCodeDialog>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    const me = await backend.users.me(true);
    if (me.publicKey == null || me.setupCode == null) {
      throw new Error('User not initialized.');
    }
    const browserKeys = await BrowserKeys.load(me.id);
    const browserId = await browserKeys.id();
    const myDevice = me.devices.find(d => d.id == browserId);
    if (myDevice == null) {
      throw new Error('Device not initialized.');
    }
    const userKeys = await UserKeys.decryptOnBrowser(myDevice.userPrivateKey, browserKeys.keyPair.privateKey, base64.parse(me.publicKey));
    const recoveryKey : { setupCode: string } = await JWEParser.parse(me.setupCode).decryptEcdhEs(userKeys.keyPair.privateKey);
    recoveryCode.value = recoveryKey.setupCode;
  } catch (error) {
    console.error('Retrieving recovery code failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function toggleRecoveryCodeVisibility() {
  recoveryCodeInputType.value = recoveryCodeInputType.value == 'password' ? 'text' : 'password';
}

async function copyRecoveryCode() {
  if (recoveryCode.value == null) {
    throw new Error('Invalid state.');
  }
  await navigator.clipboard.writeText(recoveryCode.value);
  copiedRecoveryCode.value = true;
  debouncedCopyFinish();
}

async function showRegenerateRecoveryCodeDialog() {
  regeneratingRecoveryCode.value = true;
  nextTick(() => regenerateRecoveryCodeDialog.value?.show());
}
</script>
