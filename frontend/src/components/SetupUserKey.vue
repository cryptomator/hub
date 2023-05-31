<template>
  <div class="min-h-screen flex flex-col justify-center py-24 -translate-y-24">
    <div v-if="state == State.Preparing">
      <div v-if="onFetchError == null" class="text-center">
        {{ t('common.loading') }}
      </div>
      <div v-else>
        <FetchError :error="onFetchError" :retry="fetchData"/>
      </div>
    </div>

    <div v-else-if="state == State.CreateUserKey">
      <form @submit.prevent="createUserKey()">
        <div class="flex justify-center">
          <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
            <div class="flex justify-center">
              <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
            </div>
            <div class="mt-3 sm:mt-5">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ t('setupUserKey.createUserKey.title') }}
              </h3>
              <div class="mt-2">
                <p class="text-sm text-gray-500">
                  {{ t('setupUserKey.createUserKey.description') }}
                </p>
              </div>
              <div class="mt-5 sm:mt-6 text-left rounded-md px-3 pb-1.5 pt-2.5 shadow-sm ring-1 ring-inset ring-gray-300 focus-within:ring-2 focus-within:ring-primary">
                <label for="deviceName" class="block text-xs font-medium text-gray-900">{{ t('setupUserKey.deviceName') }}</label>
                <input id="deviceName" v-model="deviceName" type="text" name="deviceName" class="block w-full border-0 p-0 text-gray-900 placeholder:text-gray-400 focus:ring-0 sm:text-sm sm:leading-6" :placeholder="t('setupUserKey.deviceName.placeholder')" required />
              </div>
              <div class="mt-5 sm:mt-6">
                <button type="submit" :disabled="processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                  {{ t('setupUserKey.createUserKey.submit') }}
                </button>
                <div v-if="onCreateError != null">
                  <p class="text-sm text-red-900 mt-2">{{ t('common.unexpectedError', [onCreateError.message]) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>

    <div v-else-if="state == State.SaveRecoveryCode">
      <form @submit.prevent="$router.push('/app/vaults')">
        <div class="flex justify-center">
          <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
            <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
              <KeyIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
            </div>
            <div class="mt-3 sm:mt-5">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ t('setupUserKey.saveRecoveryCode.title') }}
              </h3>
              <div class="mt-2">
                <p class="text-sm text-gray-500">
                  {{ t('setupUserKey.saveRecoveryCode.description') }}
                </p>
              </div>
              <div class="relative mt-5 sm:mt-6">
                <div class="overflow-hidden rounded-lg border border-gray-300 shadow-sm focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
                  <label for="recoveryCode" class="sr-only">{{ t('setupUserKey.recoveryCode') }}</label>
                  <textarea id="recoveryCode" v-model="recoveryCode" rows="1" name="recoveryCode" class="block w-full resize-none border-0 py-3 focus:ring-0 sm:text-sm" readonly />
  
                  <!-- Spacer element to match the height of the toolbar -->
                  <div class="py-2" aria-hidden="true">
                    <div class="h-9" />
                  </div>
                </div>
  
                <div class="absolute inset-x-0 bottom-0">
                  <div class="flex flex-nowrap justify-end space-x-2 py-2 px-2 sm:px-3">
                    <div class="flex-shrink-0">
                      <button type="button" class="relative inline-flex items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 text-sm font-medium text-gray-500 hover:bg-gray-100 sm:px-3" @click="copyRecoveryCode()">
                        <ClipboardIcon class="h-5 w-5 flex-shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
                        <span v-if="!copiedRecoveryCode" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
                        <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <div class="relative flex items-start text-left mt-5 sm:mt-6">
                <div class="flex h-5 items-center">
                  <input id="confirmRecoveryCode" v-model="confirmRecoveryCode" name="confirmRecoveryCode" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary" required>
                </div>
                <div class="ml-3 text-sm">
                  <label for="confirmRecoveryCode" class="font-medium text-gray-700">{{ t('setupUserKey.recoveryCode.confirm') }}</label>
                </div>
              </div>
              <div class="mt-5 sm:mt-6">
                <button type="submit" :disabled="!confirmRecoveryCode" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                  {{ t('setupUserKey.saveRecoveryCode.submit') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>

    <div v-else-if="state == State.EnterRecoveryCode">
      <form @submit.prevent="recoverUserKey()">
        <div class="flex justify-center">
          <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
            <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
              <KeyIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
            </div>
            <div class="mt-3 sm:mt-5">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ t('setupUserKey.enterRecoveryCode.title') }}
              </h3>
              <div class="mt-2">
                <p class="text-sm text-gray-500">
                  {{ t('setupUserKey.enterRecoveryCode.description') }}
                </p>
              </div>
              <div class="mt-5 sm:mt-6 text-left isolate -space-y-px rounded-md shadow-sm">
                <div class="relative rounded-md rounded-b-none px-3 pb-1.5 pt-2.5 ring-1 ring-inset ring-gray-300 focus-within:z-10 focus-within:ring-2 focus-within:ring-primary">
                  <label for="recoveryCode" class="block text-xs font-medium text-gray-900">{{ t('setupUserKey.recoveryCode') }}</label>
                  <input id="recoveryCode" v-model="recoveryCode" type="text" name="recoveryCode" class="block w-full border-0 p-0 text-gray-900 placeholder:text-gray-400 focus:ring-0 sm:text-sm sm:leading-6" :placeholder="t('setupUserKey.recoveryCode.placeholder')" required />
                </div>
                <div class="relative rounded-md rounded-t-none px-3 pb-1.5 pt-2.5 ring-1 ring-inset ring-gray-300 focus-within:z-10 focus-within:ring-2 focus-within:ring-primary">
                  <label for="deviceName" class="block text-xs font-medium text-gray-900">{{ t('setupUserKey.deviceName') }}</label>
                  <input id="deviceName" v-model="deviceName" type="text" name="deviceName" class="block w-full border-0 p-0 text-gray-900 placeholder:text-gray-400 focus:ring-0 sm:text-sm sm:leading-6" :placeholder="t('setupUserKey.deviceName.placeholder')" required />
                </div>
              </div>
              <div class="mt-5 sm:mt-6">
                <button type="submit" :disabled="processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                  {{ t('setupUserKey.enterRecoveryCode.submit') }}
                </button>
                <div v-if="onRecoverError != null">
                  <p class="text-sm text-red-900 mt-2">{{ t('common.unexpectedError', [onRecoverError.message]) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ClipboardIcon } from '@heroicons/vue/20/solid';
import { KeyIcon } from '@heroicons/vue/24/outline';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import { JWE } from '../common/jwe';
import { debounce } from '../common/util';
import router from '../router';
import FetchError from './FetchError.vue';

enum State {
  Preparing,
  CreateUserKey,
  SaveRecoveryCode,
  EnterRecoveryCode
}

const { t } = useI18n({ useScope: 'global' });

const onFetchError = ref<Error | null>(null);
const onCreateError = ref<Error | null >(null);
const onRecoverError = ref<Error | null >(null);

const state = ref(State.Preparing);
const processing = ref(false);

const user = ref<UserDto>();
const recoveryCode = ref('');
const deviceName = ref('');
const copiedRecoveryCode = ref(false);
const debouncedCopyFinish = debounce(() => copiedRecoveryCode.value = false, 2000);
const confirmRecoveryCode = ref(false);

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    user.value = await backend.users.me();
    const browserKeys = await BrowserKeys.load(user.value.id);
    if (!user.value.publicKey) {
      state.value = State.CreateUserKey;
    } else if (!browserKeys.keyPair) {
      state.value = State.EnterRecoveryCode;
    } else {
      throw new Error('Invalid state');
    }
  } catch (error) {
    console.error('Retrieving setup information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function createUserKey() {
  onCreateError.value = null;
  try {
    const me = user.value;
    if (!me) {
      throw new Error('Invalid state');
    }
    processing.value = true;

    const userKeys = await UserKeys.create();
    recoveryCode.value = crypto.randomUUID(); // TODO something else?
    const archive = await userKeys.export(recoveryCode.value);
    me.publicKey = archive.publicKey;
    me.recoveryJwe = await JWE.build({ recoveryCode: recoveryCode.value }, userKeys.keyPair.publicKey);
    me.recoveryPbkdf2 = archive.encryptedPrivateKey;
    me.recoverySalt = archive.salt;
    me.recoveryIterations = archive.iterations;
    const browserKeys = await createBrowserKeys(me.id);
    await submitBrowserKeys(browserKeys, me, userKeys);

    state.value = State.SaveRecoveryCode;
  } catch (error) {
    console.error('Creating user key failed.', error);
    onCreateError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

async function recoverUserKey() {
  onRecoverError.value = null;
  try {
    const me = user.value;
    if (!me || !me.publicKey || !me.recoveryJwe || !me.recoveryPbkdf2 || !me.recoverySalt || !me.recoveryIterations) {
      throw new Error('Invalid state');
    }
    processing.value = true;

    const userKeys = await UserKeys.recover(me.publicKey, me.recoveryPbkdf2, recoveryCode.value, me.recoverySalt, me.recoveryIterations);
    const browserKeys = await createBrowserKeys(me.id);
    await submitBrowserKeys(browserKeys, me, userKeys);

    router.push('/app/vaults');
  } catch (error) {
    console.error('Recovering user key failed.', error);
    onRecoverError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

async function createBrowserKeys(userId: string): Promise<BrowserKeys> {
  const browserKeys = await BrowserKeys.create();
  await browserKeys.store(userId);
  return browserKeys;
}

async function submitBrowserKeys(browserKeys: BrowserKeys, me: UserDto, userKeys: UserKeys) {
  const jwe = await userKeys.encryptForDevice(browserKeys.keyPair.publicKey);
  await backend.devices.putDevice({
    id: await browserKeys.id(),
    name: deviceName.value,
    type: 'BROWSER',
    publicKey: await browserKeys.encodedPublicKey(),
    userKeyJwe: jwe,
    creationTime: new Date(),
    lastSeenTime: new Date()
  });
  await backend.users.putMe(me);
}

async function copyRecoveryCode() {
  await navigator.clipboard.writeText(recoveryCode.value);
  copiedRecoveryCode.value = true;
  debouncedCopyFinish();
}
</script>
