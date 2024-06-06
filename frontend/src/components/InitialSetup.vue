<template>
  <SimpleNavigationBar v-if="me != null" :me="me"/>

  <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
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
                {{ t('initialSetup.createUserKey.title') }}
              </h3>
              <div class="mt-2">
                <p class="text-sm text-gray-500">
                  {{ t('initialSetup.createUserKey.description') }}
                </p>
              </div>

              <div class="relative mt-5 sm:mt-6">
                <div class="overflow-hidden rounded-lg border border-gray-300 shadow-sm focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
                  <textarea id="setupCode" v-model="setupCode" :aria-label="t('initialSetup.accountKey')" rows="1" name="setupCode" class="block w-full resize-none border-0 py-3 font-mono text-lg text-center focus:ring-0" readonly />
                  <div class="py-2" aria-hidden="true">
                    <div class="h-9" />
                  </div>
                </div>
                <div class="absolute inset-x-0 bottom-0">
                  <div class="flex flex-nowrap justify-end space-x-2 py-2 px-2 sm:px-3">
                    <div class="flex-shrink-0">
                      <button type="button" class="relative inline-flex items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 text-sm font-medium text-gray-500 hover:bg-gray-100 sm:px-3" @click="copySetupCode()">
                        <ClipboardIcon class="h-5 w-5 flex-shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
                        <span v-if="!copiedSetupCode" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
                        <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mt-5 sm:mt-6 text-left flex flex-col gap-5">
                <div class="flex">
                  <div class="mx-3">
                    <KeyIcon class="h-6 w-6 text-primary" aria-hidden="true" />
                  </div>
                  <p class="text-gray-600">
                    {{ t('initialSetup.createUserKey.details.accountKey') }}
                  </p>
                </div>

                <div class="flex">
                  <div class="mx-3">
                    <ListBulletIcon class="h-6 w-6 text-primary" aria-hidden="true" />
                  </div>
                  <p class="text-gray-600">
                    {{ t('initialSetup.createUserKey.details.devicesList') }}
                  </p>
                </div>

                <div class="flex">
                  <div class="mx-3">
                    <ComputerDesktopIcon class="h-6 w-6 text-primary" aria-hidden="true" />
                  </div>
                  <i18n-t keypath="initialSetup.createUserKey.details.devicesName" scope="global" tag="p" class="text-gray-600">
                    <template #deviceName>
                      <span class="inline-flex items-center gap-1">
                        <span ref="deviceNameField" :aria-label="t('initialSetup.createUserKey.details.devicesName.label')" :contenteditable="deviceNameFieldIsActive" class="cursor-pointer focus:cursor-text focus:outline-primary focus:selection:bg-primary-l2 focus:selection:text-primary select-all font-mono break-all" @click="!deviceNameFieldIsActive && editBrowserName()" @blur="deviceNameFieldIsActive && revertBrowserName()" @keydown.enter.prevent="confirmBrowserName()" @keydown.esc.prevent="revertBrowserName()" v-text="deviceName" />
                        <PencilIcon v-if="!deviceNameFieldIsActive" class="cursor-pointer inline-block h-4 w-4" aria-hidden="true" @click="editBrowserName()" />
                        <CheckIcon v-else class="cursor-pointer inline-block h-4 w-4 text-primary" aria-hidden="true" @mousedown.prevent="" @click="confirmBrowserName()" />
                      </span>
                    </template>
                  </i18n-t>
                </div>
              </div>

              <div class="relative flex justify-center text-left mt-5 sm:mt-6">
                <div class="flex h-5 items-center">
                  <input id="confirmSetupCode" v-model="confirmSetupCode" name="confirmSetupCode" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary" required>
                </div>
                <div class="ml-3 text-sm">
                  <label for="confirmSetupCode" class="font-medium text-gray-700">{{ t('initialSetup.createUserKey.confirmAccountKey') }}</label>
                </div>
              </div>

              <div class="mt-5 sm:mt-6">
                <button type="submit" :disabled="!confirmSetupCode || processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                  {{ t('initialSetup.submit') }}
                </button>
                <div v-if="onCreateError != null">
                  <p class="text-red-900 mt-2">{{ t('common.unexpectedError', [onCreateError.message]) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>

    <div v-else-if="state == State.RecoverUserKey">
      <form @submit.prevent="recoverUserKey()">
        <div class="flex flex-col items-center">
          <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
            <div class="flex justify-center">
              <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
            </div>
            <div class="mt-3 sm:mt-5">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ t('initialSetup.recoverUserKey.title') }}
              </h3>
              <div class="mt-2">
                <p class="text-sm text-gray-500">
                  {{ t('initialSetup.recoverUserKey.description') }}
                </p>
              </div>
              <div class="mt-5 sm:mt-6 text-left">      
                <label for="setupCode" class="block text-sm font-medium text-gray-700">{{ t('initialSetup.accountKey') }}</label>
                <input id="setupCode" v-model="setupCode" v-focus type="text" name="setupCode" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" aria-describedby="setupCodeDescription" />
                <p id="setupCodeDescription" class="mt-2 text-sm text-gray-500">{{ t('initialSetup.recoverUserKey.accountKey.description') }}</p>
              </div>
              <div class="mt-5 sm:mt-6 text-left">      
                <label for="deviceName" class="block text-sm font-medium text-gray-700">{{ t('initialSetup.recoverUserKey.deviceName') }}</label>
                <input id="deviceName" v-model="deviceName" type="text" name="deviceName" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" aria-describedby="deviceNameDescription" />
                <p id="deviceNameDescription" class="mt-2 text-sm text-gray-500">{{ t('initialSetup.recoverUserKey.deviceName.description') }}</p>
              </div>
              <div class="mt-5 sm:mt-6">
                <button type="submit" :disabled="processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                  {{ t('initialSetup.submit') }}
                </button>
                <div class="text-sm text-red-900 mt-2">
                  <p v-if="onRecoverError instanceof UnwrapKeyError">{{ t('initialSetup.recoverUserKey.error.wrongAccountKey') }}</p>
                  <p v-else-if="onRecoverError != null">{{ t('common.unexpectedError', [onRecoverError.message]) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>

      <i18n-t keypath="initialSetup.recoverUserKey.lostAccountKey" scope="global" tag="p" class="mt-10 text-center text-sm text-gray-500">
        <a tabindex="0" class="font-medium leading-6 text-red-600 hover:text-red-900" @click="showResetUserAccountDialog()">
          {{ t('initialSetup.recoverUserKey.lostAccountKey.resetUserAccount') }}
        </a>
      </i18n-t>
    </div>

    <div v-else-if="state == State.SetupAlreadyCompleted">
      <div class="flex flex-col items-center">
        <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
          <div class="flex justify-center">
            <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              {{ t('initialSetup.setupAlreadyCompleted.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                {{ t('initialSetup.setupAlreadyCompleted.description') }}
              </p>
            </div>
            <div class="mt-5 sm:mt-6">
              <router-link v-slot="{ navigate }" to="/app/profile" custom>
                <button type="button" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="navigate()">
                  {{ t('initialSetup.setupAlreadyCompleted.goToYourProfile') }}
                </button>
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <ResetUserAccountDialog v-if="resettingUserAccount" ref="resetUserAccountDialog" :me="me!" @close="resettingUserAccount = false" />
</template>

<script setup lang="ts">
import { ClipboardIcon } from '@heroicons/vue/20/solid';
import { CheckIcon, PencilIcon } from '@heroicons/vue/24/outline';
import { ComputerDesktopIcon, KeyIcon, ListBulletIcon } from '@heroicons/vue/24/solid';
import { nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import { BrowserKeys, UnwrapKeyError, UserKeys } from '../common/crypto';
import { JWEBuilder } from '../common/jwe';
import userdata from '../common/userdata';
import { debounce } from '../common/util';
import router from '../router';
import FetchError from './FetchError.vue';
import ResetUserAccountDialog from './ResetUserAccountDialog.vue';
import SimpleNavigationBar from './SimpleNavigationBar.vue';

enum State {
  Preparing,
  CreateUserKey,
  RecoverUserKey,
  SetupAlreadyCompleted
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

const me = ref<UserDto>();
const setupCode = ref<string>('');
const deviceName = ref(guessBrowserName());
const previousDeviceName = ref(deviceName.value);
const deviceNameField = ref<HTMLSpanElement>();
const deviceNameFieldIsActive = ref(false);
const copiedSetupCode = ref(false);
const debouncedCopyFinish = debounce(() => copiedSetupCode.value = false, 2000);
const confirmSetupCode = ref(false);
const resettingUserAccount = ref(false);
const resetUserAccountDialog = ref<typeof ResetUserAccountDialog>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await userdata.me;
    if (!me.value.setupCode) {
      setupCode.value = crypto.randomUUID();
      state.value = State.CreateUserKey;
    } else if (!await userdata.browser) {
      state.value = State.RecoverUserKey;
    } else {
      state.value = State.SetupAlreadyCompleted;
    }
  } catch (error) {
    console.error('Retrieving setup information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function createUserKey() {
  onCreateError.value = null;
  try {
    processing.value = true;
    const me = await userdata.me;

    const userKeys = await UserKeys.create();
    me.ecdhPublicKey = await userKeys.encodedEcdhPublicKey();
    me.ecdsaPublicKey = await userKeys.encodedEcdsaPublicKey();
    me.privateKey = await userKeys.encryptWithSetupCode(setupCode.value);
    me.setupCode = await JWEBuilder.ecdhEs(userKeys.ecdhKeyPair.publicKey).encrypt({ setupCode: setupCode.value });
    const browserKeys = await userdata.createBrowserKeys();
    await submitBrowserKeys(browserKeys, me, userKeys);

    await router.push('/app/vaults');
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
    processing.value = true;

    const me = await userdata.me;
    const userKeys = await userdata.decryptUserKeysWithSetupCode(setupCode.value);
    const browserKeys = await userdata.createBrowserKeys();
    await submitBrowserKeys(browserKeys, me, userKeys);

    await router.push('/app/vaults');
  } catch (error) {
    console.error('Recovering user key failed.', error);
    onRecoverError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
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
  userdata.reload();
}

function guessBrowserName(): string {
  const match = navigator.userAgent.toLowerCase().match(/(android|iphone|opr|edge|chrome|safari|firefox)/) || [''];
  switch (match[0]) {
    case 'android': return 'Android';
    case 'iphone': return 'iPhone';
    case 'opr': return 'Opera';
    case 'edge': return 'Edge';
    case 'chrome': return 'Chrome';
    case 'safari': return 'Safari';
    case 'firefox': return 'Firefox';
    default: return 'Browser';
  }
}

function editBrowserName() {
  deviceNameFieldIsActive.value = true;
  previousDeviceName.value = deviceName.value;
  nextTick(() => {
    const span = deviceNameField.value!;
    span.focus();
    const range = document.createRange();
    range.selectNodeContents(span);
    const sel = window.getSelection() as Selection;
    sel.removeAllRanges();
    sel.addRange(range);
  });
}

function confirmBrowserName() {
  deviceNameFieldIsActive.value = false;
  const span = deviceNameField.value!;
  let val = span.innerText.trim();
  if (val == '') {
    val = guessBrowserName(); // reset to default
  }
  span.innerText = val;
  deviceName.value = val;
}

function revertBrowserName() {
  deviceNameFieldIsActive.value = false;
  const span = deviceNameField.value!;
  span.innerText = previousDeviceName.value;
}

async function copySetupCode() {
  await navigator.clipboard.writeText(setupCode.value);
  copiedSetupCode.value = true;
  debouncedCopyFinish();
}

async function showResetUserAccountDialog() {
  resettingUserAccount.value = true;
  nextTick(() => resetUserAccountDialog.value?.show());
}
</script>
