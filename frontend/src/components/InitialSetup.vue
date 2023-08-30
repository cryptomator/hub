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

    <div v-else-if="state == State.CreateUserKey" class="text-sm text-gray-600">
      <form @submit.prevent="createUserKey()">
        <div class="flex justify-center">
          <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-left text-base text-gray-600 sm:w-full sm:max-w-lg">
            <div class="flex justify-center">
              <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
            </div>
            <div class="mt-3 sm:mt-5">
              <h3 class="text-lg leading-6 font-medium text-gray-900 text-center">
                {{ t('initialSetup.title') }}
              </h3>
              <p class="mt-2 text-center">
                {{ t('initialSetup.description') }}
              </p>
              
              <div class="relative mt-5 sm:mt-6 text-left">
                <div class="overflow-hidden rounded-lg border border-gray-300 shadow-sm focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
                  <textarea id="setupCode" v-model="setupCode" :aria-label="t('initialSetup.setupCode')" rows="1" name="setupCode" class="block w-full resize-none border-0 py-3 font-mono text-lg text-center focus:ring-0" readonly />
                  <div class="py-2" aria-hidden="true">
                    <div class="h-9" />
                  </div>
                </div>
                <div class="absolute inset-x-0 bottom-0">
                  <div class="flex flex-nowrap justify-end space-x-2 py-2 px-2 sm:px-3">
                    <div class="flex-shrink-0">
                      <button type="button" class="relative inline-flex items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 font-medium hover:bg-gray-100 sm:px-3" @click="copySetupCode()">
                        <ClipboardIcon class="h-5 w-5 flex-shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
                        <span v-if="!copiedSetupCode" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
                        <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mt-5 flex items-start">
                <div class="mx-3">
                  <KeyIcon class="h-6 w-6 text-primary" aria-hidden="true" />
                </div>
                <p>
                  {{ t('initialSetup.details.setupCode') }}
                </p>
              </div>

              <div class="mt-5 flex items-start">
                <div class="mx-3">
                  <ListBulletIcon class="h-6 w-6 text-primary" aria-hidden="true" />
                </div>
                <p>
                  {{ t('initialSetup.details.devicesList') }}
                </p>
              </div>

              <div class="mt-5 flex items-start">
                <div class="mx-3">
                  <ComputerDesktopIcon class="h-6 w-6 text-primary" aria-hidden="true" />
                </div>
                <p>
                  {{ t('initialSetup.details.devicesName.before') }}
                  <span ref="deviceNameField" :aria-label="t('initialSetup.details.devicesName.label')" contenteditable class="cursor-pointer focus:cursor-text focus:outline-primary focus:selection:bg-primary-l2 focus:selection:text-primary select-all font-mono" @click="editBrowserName()" @keydown.enter.prevent="deviceNameField?.blur()" @blur="changeBrowserName" v-text="deviceName" />
                  {{ ' ' }}
                  <PencilIcon class="inline-block h-4 w-4 -mt-1 cursor-pointer" aria-hidden="true" @click="editBrowserName()" />
                  {{ t('initialSetup.details.devicesName.after') }}
                </p>
              </div>

              <div class="text-center mt-5 sm:mt-6">
                <input id="confirmSetupKey" v-model="confirmSetupKey" name="confirmSetupKey" type="checkbox" class="h-4 w-4 mx-2 rounded border-gray-300 text-primary focus:ring-primary" required>
                <label for="confirmSetupKey" class="font-medium cursor-pointer">{{ t('initialSetup.confirmSetupKey') }}</label>
              </div>

              <div class="mt-5 sm:mt-6">
                <button type="submit" :disabled="!confirmSetupKey || processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
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

      <p class="mt-10 text-center text-sm">
        <router-link to="/app/logout" class="leading-6 text-gray-400 hover:text-gray-700">{{ t('nav.profile.signOut') }}</router-link>
      </p>
    </div>

    <div v-else-if="state == State.EnterSetupCode">
      <form @submit.prevent="recoverUserKey()">
        <div class="flex flex-col items-center">
          <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
            <div class="flex justify-center">
              <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
            </div>
            <div class="mt-3 sm:mt-5">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ t('registerDevice.title') }}
              </h3>
              <div class="mt-2">
                <p class="text-sm text-gray-500">
                  {{ t('registerDevice.description') }}
                </p>
              </div>
              <div class="mt-5 sm:mt-6 text-left">      
                <label for="setupCode" class="block text-sm font-medium text-gray-700">{{ t('registerDevice.setupCode') }}</label>
                <input id="setupCode" v-model="setupCode" v-focus type="text" name="setupCode" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" aria-describedby="setupCodeDescription" />
                <p id="setupCodeDescription" class="mt-2 text-sm text-gray-500">{{ t('registerDevice.setupCode.description') }}</p>
              </div>
              <div class="mt-5 sm:mt-6 text-left">      
                <label for="deviceName" class="block text-sm font-medium text-gray-700">{{ t('registerDevice.deviceName') }}</label>
                <input id="deviceName" v-model="deviceName" type="text" name="deviceName" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" aria-describedby="deviceNameDescription" />
                <p id="deviceNameDescription" class="mt-2 text-sm text-gray-500">{{ t('registerDevice.deviceName.description') }}</p>
              </div>
              <div class="mt-5 sm:mt-6">
                <button type="submit" :disabled="processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                  {{ t('registerDevice.submit') }}
                </button>
                <div class="text-sm text-red-900 mt-2">
                  <p v-if="onRecoverError instanceof UnwrapKeyError">{{ t('registerDevice.error.wrongSetupCode') }}</p>
                  <p v-else-if="onRecoverError != null">{{ t('common.unexpectedError', [onRecoverError.message]) }}</p>
                </div>
              </div>
            </div>
          </div>
          <p class="mt-10 text-center text-sm text-gray-500">
            {{ t('registerDevice.lostSetupCode.title') }}
            {{ ' ' }}
            <a role="button" tabindex="0" class="font-medium leading-6 text-red-600 hover:text-red-900" @click="showResetUserAccountDialog()">
              {{ t('registerDevice.lostSetupCode.resetUserAccount') }}
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
import { PencilIcon } from '@heroicons/vue/24/outline';
import { ComputerDesktopIcon, KeyIcon, ListBulletIcon } from '@heroicons/vue/24/solid';
import { nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import { BrowserKeys, UnwrapKeyError, UserKeys } from '../common/crypto';
import { JWEBuilder } from '../common/jwe';
import { debounce } from '../common/util';
import router from '../router';
import FetchError from './FetchError.vue';
import ResetUserAccountDialog from './ResetUserAccountDialog.vue';

enum State {
  Preparing,
  CreateUserKey,
  EnterSetupCode
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
const setupCode = ref<string>('');
const deviceName = ref(guessBrowserName());
const deviceNameField = ref<HTMLSpanElement>();
const copiedSetupCode = ref(false);
const debouncedCopyFinish = debounce(() => copiedSetupCode.value = false, 2000);
const confirmSetupKey = ref(false);
const resettingUserAccount = ref(false);
const resetUserAccountDialog = ref<typeof ResetUserAccountDialog>();

onMounted(fetchData);

function guessBrowserName(): string {
  var match = navigator.userAgent.toLowerCase().match(/(android|iphone|opr|edge|chrome|safari|firefox)/) || [''];
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
  const span = deviceNameField.value!;
  span.focus();
  const range = document.createRange();
  range.selectNodeContents(span);
  const sel = window.getSelection() as Selection;
  sel.removeAllRanges();
  sel.addRange(range);
}

function changeBrowserName(e: Event) {
  const span = e.target as HTMLElement;
  let val = span.innerText.trim();
  if (val == '') {
    val = guessBrowserName(); // reset to default TODO: or previous value?
    span.innerText = val;
    editBrowserName(); // keep editing
  } else {
    deviceName.value = val;
  }
}

async function fetchData() {
  onFetchError.value = null;
  try {
    user.value = await backend.users.me();
    const browserKeys = await BrowserKeys.load(user.value.id);
    const browserId = await browserKeys?.id();
    if (!user.value.publicKey) {
      setupCode.value = crypto.randomUUID();
      state.value = State.CreateUserKey;
    } else if (!browserKeys || user.value.devices.find(d => d.id == browserId) == null) {
      state.value = State.EnterSetupCode;
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
    me.publicKey = await userKeys.encodedPublicKey();
    me.privateKey = await userKeys.encryptedPrivateKey(setupCode.value);
    me.setupCode = await JWEBuilder.ecdhEs(userKeys.keyPair.publicKey).encrypt({ setupCode: setupCode.value });
    const browserKeys = await createBrowserKeys(me.id);
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
    const me = user.value;
    if (!me || !me.publicKey || !me.privateKey) {
      throw new Error('Invalid state');
    }
    processing.value = true;

    const userKeys = await UserKeys.recover(me.publicKey, me.privateKey, setupCode.value);
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
