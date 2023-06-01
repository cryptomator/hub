<template>
  <div v-if="me == null || myDevice == null">
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
      <button type="button" class="bg-red-500 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-red-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="regenerateRecoveryCode()">
        {{ t('manageRecoveryCode.regenerate') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ClipboardIcon, EyeIcon, EyeSlashIcon } from '@heroicons/vue/20/solid';
import { base64 } from 'rfc4648';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { DeviceDto, UserDto } from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import { JWE } from '../common/jwe';
import { debounce } from '../common/util';
import FetchError from './FetchError.vue';
const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const myDevice = ref<DeviceDto>();
const recoveryCode = ref<string>('');
const recoveryCodeInputType = ref<'password' | 'text'>('password');
const copiedRecoveryCode = ref(false);
const debouncedCopyFinish = debounce(() => copiedRecoveryCode.value = false, 2000);
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me(true);
    if (me.value?.publicKey == null || me.value?.recoveryJwe == null) {
      throw new Error('User not initialized.');
    }
    const browserKeys = await BrowserKeys.load(me.value.id);
    const browserId = await browserKeys.id();
    myDevice.value = me.value.devices.find(d => d.id == browserId);
    if (myDevice.value == null) {
      throw new Error('Device not initialized.');
    }
    const userKeys = await UserKeys.decryptOnBrowser(myDevice.value.userKeyJwe, browserKeys.keyPair.privateKey, base64.parse(me.value.publicKey));
    const recoveryKey : { recoveryCode: string } = await JWE.parse(me.value.recoveryJwe, userKeys.keyPair.privateKey);
    recoveryCode.value = recoveryKey.recoveryCode;
  } catch (error) {
    console.error('Retrieving my device failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function toggleRecoveryCodeVisibility() {
  recoveryCodeInputType.value = recoveryCodeInputType.value == 'password' ? 'text' : 'password';
}

async function copyRecoveryCode() {
  await navigator.clipboard.writeText(recoveryCode.value);
  copiedRecoveryCode.value = true;
  debouncedCopyFinish();
}

async function regenerateRecoveryCode() {
  if (me.value?.publicKey == null || myDevice.value == null) {
    throw new Error('Illegal state.');
  }
  const browserKeys = await BrowserKeys.load(me.value.id);
  const newCode = crypto.randomUUID(); // TODO something else?
  const userKeys = await UserKeys.decryptOnBrowser(myDevice.value.userKeyJwe, browserKeys.keyPair.privateKey, base64.parse(me.value.publicKey));
  const archive = await userKeys.export(newCode);
  me.value.recoveryJwe = await JWE.build({ recoveryCode: newCode }, userKeys.keyPair.publicKey);
  me.value.recoveryPbkdf2 = archive.encryptedPrivateKey;
  me.value.recoverySalt = archive.salt;
  me.value.recoveryIterations = archive.iterations;
  await backend.users.putMe(me.value);
  recoveryCode.value = newCode;
}
</script>
