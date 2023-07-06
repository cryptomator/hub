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
              <div class="mt-5 sm:mt-6 text-left">      
                <label for="deviceName" class="block text-sm font-medium text-gray-700">{{ t('setupUserKey.deviceName') }}</label>
                <input id="deviceName" v-model="deviceName" v-focus type="text" name="deviceName" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" aria-describedby="deviceNameDescription" />
                <p id="deviceNameDescription" class="mt-2 text-sm text-gray-500">{{ t('setupUserKey.deviceName.description') }}</p>
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
                  <textarea id="recoveryCode" v-model="recoveryCode" rows="1" name="recoveryCode" class="block w-full resize-none border-0 py-3 font-mono text-lg text-center focus:ring-0" readonly />
  
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
              <div class="mt-2">
                <p class="text-sm text-gray-500">{{ t('setupUserKey.saveRecoveryCode.recoveryCodeHint') }}</p>
              </div>
              <div class="mt-5 sm:mt-6">
                <button type="submit" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm">
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
        <div class="flex flex-col items-center">
          <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
            <div class="flex justify-center">
              <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
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
              <div class="mt-5 sm:mt-6 text-left">      
                <label for="recoveryCode" class="block text-sm font-medium text-gray-700">{{ t('setupUserKey.recoveryCode') }}</label>
                <input id="recoveryCode" v-model="recoveryCode" v-focus type="text" name="recoveryCode" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" aria-describedby="recoveryCodeDescription" />
                <p id="recoveryCodeDescription" class="mt-2 text-sm text-gray-500">{{ t('setupUserKey.recoveryCode.description') }}</p>
              </div>
              <div class="mt-5 sm:mt-6 text-left">      
                <label for="deviceName" class="block text-sm font-medium text-gray-700">{{ t('setupUserKey.deviceName') }}</label>
                <input id="deviceName" v-model="deviceName" type="text" name="deviceName" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" aria-describedby="deviceNameDescription" />
                <p id="deviceNameDescription" class="mt-2 text-sm text-gray-500">{{ t('setupUserKey.deviceName.description') }}</p>
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
          <p class="mt-10 text-center text-sm text-gray-500">
            {{ t('setupUserKey.lostRecoveryCode.title') }}
            {{ ' ' }}
            <a role="button" tabindex="0" class="font-medium leading-6 text-red-600 hover:text-red-900" @click="showResetUserAccountDialog()">
              {{ t('setupUserKey.lostRecoveryCode.resetUserAccount') }}
            </a>
          </p>
        </div>
      </form>
    </div>
  </div>

  <ResetUserAccountDialog v-if="resettingUserAccount" ref="resetUserAccountDialog" @close="resettingUserAccount = false" />
</template>

<script setup lang="ts">
import { ClipboardIcon } from '@heroicons/vue/20/solid';
import { KeyIcon } from '@heroicons/vue/24/outline';
import { nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import { JWEBuilder } from '../common/jwe';
import { debounce } from '../common/util';
import router from '../router';
import FetchError from './FetchError.vue';
import ResetUserAccountDialog from './ResetUserAccountDialog.vue';

enum State {
  Preparing,
  CreateUserKey,
  SaveRecoveryCode,
  EnterRecoveryCode
}

const { t } = useI18n({ useScope: 'global' });

const vFocus = {
  mounted: (el: HTMLElement) => {
    el.focus();
  }
};

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
const resettingUserAccount = ref(false);
const resetUserAccountDialog = ref<typeof ResetUserAccountDialog>();

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
    recoveryCode.value = crypto.randomUUID();
    me.publicKey = await userKeys.encodedPublicKey();
    me.privateKey = await userKeys.encryptedPrivateKey(recoveryCode.value);
    me.setupCode = await JWEBuilder.ecdhEs(userKeys.keyPair.publicKey).encrypt({ setupCode: recoveryCode.value });
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
    if (!me || !me.publicKey || !me.privateKey) {
      throw new Error('Invalid state');
    }
    processing.value = true;

    const userKeys = await UserKeys.recover(me.publicKey, me.privateKey, recoveryCode.value);
    const browserKeys = await createBrowserKeys(me.id);
    await submitBrowserKeys(browserKeys, me, userKeys);

    await router.push('/app/vaults');
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
    userPrivateKey: jwe,
    creationTime: new Date()
  });
  await backend.users.putMe(me);
}

async function copyRecoveryCode() {
  await navigator.clipboard.writeText(recoveryCode.value);
  copiedRecoveryCode.value = true;
  debouncedCopyFinish();
}

async function showResetUserAccountDialog() {
  resettingUserAccount.value = true;
  nextTick(() => resetUserAccountDialog.value?.show());
}
</script>
