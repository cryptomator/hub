<template>
  <div v-if="state == State.Initial">
    {{ t('common.loading') }}
  </div>

  <div v-else-if="state == State.EnterRecoveryKey">
    <form ref="form" novalidate @submit.prevent="validateRecoveryKey()">
      <div class="flex justify-center">
        <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
          <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <ArrowPathIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              {{ t('createVault.enterRecoveryKey.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                {{ t('createVault.enterRecoveryKey.description') }}
              </p>
            </div>
          </div>
          <div class="mt-5 sm:mt-6">
            <label for="recoveryKey" class="sr-only">{{ t('createVault.enterRecoveryKey.recoveryKey') }}</label>
            <textarea id="recoveryKey" v-model="recoveryKey" rows="6" name="recoveryKey" class="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary sm:text-sm" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onRecoverError instanceof FormValidationFailedError }" required />
          </div>
          <div class="mt-5 sm:mt-6">
            <button type="submit" :disabled="processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
              {{ t('createVault.enterRecoveryKey.submit') }}
            </button>
            <div v-if="onRecoverError != null">
              <p v-if="onRecoverError instanceof FormValidationFailedError" class="text-sm text-red-900 mt-2">{{ t('createVault.error.formValidationFailed') }}</p>
              <p v-else class="text-sm text-red-900 mt-2">{{ t('createVault.error.invalidRecoveryKey') }}</p>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.EnterVaultDetails">
    <form ref="form" class="space-y-6" novalidate @submit.prevent="validateVaultDetails()">
      <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6">
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <div class="md:col-span-1">
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ t('createVault.enterVaultDetails.title') }}
            </h3>
            <p class="mt-1 text-sm text-gray-500">
              {{ t('createVault.enterVaultDetails.description') }}
            </p>
          </div>

          <div class="mt-5 md:mt-0 md:col-span-2">
            <div class="grid grid-cols-6 gap-6">
              <div class="col-span-6 sm:col-span-3">
                <label for="vaultName" class="block text-sm font-medium text-gray-700">{{ t('createVault.enterVaultDetails.vaultName') }}</label>
                <input id="vaultName" v-model="vaultName" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" pattern="^(?! )([^\\\/:*?\x22<>|])*(?<! |\.)$" required />
              </div>

              <div class="col-span-6 sm:col-span-4">
                <label for="vaultDescription" class="block text-sm font-medium text-gray-700">
                  {{ t('createVault.enterVaultDetails.vaultDescription') }}
                  <span class="text-xs text-gray-500">({{ t('common.optional') }})</span></label>
                <input id="vaultDescription" v-model="vaultDescription" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200"/>
              </div>

              <div class="col-span-6 sm:col-span-4">
                <label for="password" class="block text-sm font-medium text-gray-700">{{ t('createVault.enterVaultDetails.password') }}</label>
                <input id="password" v-model="password" :disabled="processing" type="password" minlength="8" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" aria-describedby="password-description" required />
                <p id="password-description" class="mt-2 text-sm text-gray-500">{{ t('createVault.enterVaultDetails.password.description') }}</p>
              </div>

              <div class="col-span-6 sm:col-span-4">
                <label for="passwordConfirmation" class="block text-sm font-medium text-gray-700">{{ t('createVault.enterVaultDetails.passwordConfirmation') }}</label>
                <input id="passwordConfirmation" v-model="passwordConfirmation" :disabled="processing" type="password" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" aria-describedby="password-confirmation-description" required />
                <p id="password-confirmation-description" class="mt-2 text-sm text-gray-500">
                  <span v-if="passwordMatches && passwordConfirmation.length != 0">{{ t('createVault.enterVaultDetails.passwordConfirmation.passwordsMatch') }}</span>
                  <span v-else-if="!passwordMatches && passwordConfirmation.length != 0">{{ t('createVault.enterVaultDetails.passwordConfirmation.passwordsDoNotMatch') }}</span>
                  <span v-else>&nbsp;</span>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-end items-center">
        <div v-if="onCreateError != null">
          <p v-if="onCreateError instanceof ConflictError" class="text-sm text-red-900 mr-4">{{ t('createVault.error.vaultAlreadyExists') }}</p>
          <p v-else-if="onCreateError instanceof FormValidationFailedError" class="text-sm text-red-900 mr-4">{{ t('createVault.error.formValidationFailed') }}</p>
          <p v-else-if="onCreateError instanceof PasswordNotMachingError" class="text-sm text-red-900 mr-4">{{ t('createVault.enterVaultDetails.passwordConfirmation.passwordsDoNotMatch') }}</p>
          <p v-else class="text-sm text-red-900 mr-4">{{ t('common.unexpectedError', [onCreateError.message]) }}</p>
        </div>
        <button type="submit" :disabled="processing" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
          {{ t('common.next') }}
        </button>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.ShowRecoveryKey">
    <form @submit.prevent="createVault()">
      <div class="flex justify-center">
        <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
          <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <KeyIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              {{ t('createVault.showRecoveryKey.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                {{ t('createVault.showRecoveryKey.description') }}
              </p>
            </div>
            <div class="relative mt-5 sm:mt-6">
              <div class="overflow-hidden rounded-lg border border-gray-300 shadow-sm focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
                <label for="recoveryKey" class="sr-only">{{ t('createVault.showRecoveryKey.recoveryKey') }}</label>
                <textarea id="recoveryKey" v-model="recoveryKey" rows="6" name="recoveryKey" class="block w-full resize-none border-0 py-3 focus:ring-0 sm:text-sm" readonly />

                <!-- Spacer element to match the height of the toolbar -->
                <div class="py-2" aria-hidden="true">
                  <div class="h-9" />
                </div>
              </div>

              <div class="absolute inset-x-0 bottom-0">
                <div class="flex flex-nowrap justify-end space-x-2 py-2 px-2 sm:px-3">
                  <div class="flex-shrink-0">
                    <button type="button" class="relative inline-flex items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 text-sm font-medium text-gray-500 hover:bg-gray-100 sm:px-3" @click="copyRecoveryKey()">
                      <ClipboardIcon class="h-5 w-5 flex-shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
                      <span v-if="!copiedRecoveryKey" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
                      <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="relative flex items-start text-left mt-5 sm:mt-6">
              <div class="flex h-5 items-center">
                <input id="confirmRecoveryKey" v-model="confirmRecoveryKey" name="confirmRecoveryKey" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary" required>
              </div>
              <div class="ml-3 text-sm">
                <label for="confirmRecoveryKey" class="font-medium text-gray-700">{{ t('createVault.showRecoveryKey.confirmRecoveryKey') }}</label>
              </div>
            </div>
            <div class="mt-5 sm:mt-6">
              <button type="submit" :disabled="!confirmRecoveryKey || processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                {{ t('createVault.showRecoveryKey.submit') }}
              </button>
              <div v-if="onCreateError != null">
                <p v-if="onCreateError instanceof ConflictError" class="text-sm text-red-900 mt-2">{{ t('createVault.error.vaultAlreadyExists') }}</p>
                <p v-else class="text-sm text-red-900 mt-2">{{ t('common.unexpectedError', [onCreateError.message]) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.Finished">
    <div class="flex justify-center">
      <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
        <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
          <CheckIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
        </div>
        <div class="mt-3 sm:mt-5">
          <h3 class="text-lg leading-6 font-medium text-gray-900">
            {{ t('createVault.success.title') }}
          </h3>
          <div class="mt-2">
            <p class="text-sm text-gray-500">
              {{ t('createVault.success.description') }}
            </p>
          </div>
        </div>
        <div class="mt-5 sm:mt-6">
          <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="downloadVaultTemplate()">
            <ArrowDownTrayIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('createVault.success.download') }}
          </button>
          <p v-if="onDownloadTemplateError != null " class="text-sm text-red-900 mr-4">{{ t('createVault.error.downloadTemplateFailed', [onDownloadTemplateError.message]) }}</p> <!-- TODO: not beautiful-->
        </div>
        <div class="mt-2">
          <router-link to="/app/vaults" class="text-sm text-gray-500">
            {{ t('createVault.success.return') }}
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ClipboardIcon } from '@heroicons/vue/20/solid';
import { ArrowPathIcon, CheckIcon, KeyIcon } from '@heroicons/vue/24/outline';
import { ArrowDownTrayIcon } from '@heroicons/vue/24/solid';
import { saveAs } from 'file-saver';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { ConflictError } from '../common/backend';
import { VaultKeys } from '../common/crypto';
import { debounce } from '../common/util';
import { VaultConfig } from '../common/vaultconfig';

enum State {
  Initial,
  EnterRecoveryKey,
  EnterVaultDetails,
  ShowRecoveryKey,
  Finished
}

class FormValidationFailedError extends Error {
  constructor() {
    super('The form is invalid.');
  }
}

class EmptyVaultTemplateError extends Error {
  constructor() {
    super('Vault template is empty.');
  }
}

class PasswordNotMachingError extends Error {
  constructor() {
    super('Passwords do not match.');
  }
}

const { t } = useI18n({ useScope: 'global' });

const form = ref<HTMLFormElement>();

const onCreateError = ref<Error | null >(null);
const onRecoverError = ref<Error | null >(null);
const onDownloadTemplateError = ref<Error | null>(null);

const state = ref(State.Initial);
const processing = ref(false);
const vaultName = ref('');
const vaultDescription = ref('');
const password = ref('');
const passwordConfirmation = ref('');
const passwordMatches = computed(() => password.value == passwordConfirmation.value);
const copiedRecoveryKey = ref(false);
const debouncedCopyFinish = debounce(() => copiedRecoveryKey.value = false, 2000);
const confirmRecoveryKey = ref(false);
const vaultKeys = ref<VaultKeys>();
const recoveryKey = ref<string>('');
const vaultConfig = ref<VaultConfig>();

const props = defineProps<{
  recover: boolean
}>();

onMounted(initialize);

async function initialize() {
  if (props.recover) {
    state.value = State.EnterRecoveryKey;
  } else {
    vaultKeys.value = await VaultKeys.create();
    recoveryKey.value = await vaultKeys.value.createRecoveryKey();
    state.value = State.EnterVaultDetails;
  }
}

async function validateRecoveryKey() {
  onRecoverError.value = null;
  if (!form.value?.checkValidity()) {
    onRecoverError.value = new FormValidationFailedError();
    return;
  }
  await recoverVault();
}

async function recoverVault() {
  onRecoverError.value = null;
  try {
    processing.value = true;
    vaultKeys.value = await VaultKeys.recover(recoveryKey.value);
    state.value = State.EnterVaultDetails;
  } catch (error) {
    console.error('Recovering vault failed.', error);
    onRecoverError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

async function validateVaultDetails() {
  onCreateError.value = null;
  if (!form.value?.checkValidity()) {
    onCreateError.value = new FormValidationFailedError();
    return;
  } else if (!passwordMatches.value) {
    onCreateError.value = new PasswordNotMachingError();
    return;
  }
  if (props.recover) {
    await createVault();
  } else {
    state.value = State.ShowRecoveryKey;
  }
}

async function createVault() {
  onCreateError.value = null;
  try {
    if (!vaultKeys.value) {
      throw new Error('Invalid state');
    }
    processing.value = true;
    const vaultId = crypto.randomUUID();
    vaultConfig.value = await VaultConfig.create(vaultId, vaultKeys.value);
    const wrapped = await vaultKeys.value.wrap(password.value);
    await backend.vaults.createVault(vaultId, vaultName.value, vaultDescription.value, wrapped.masterkey, wrapped.iterations, wrapped.salt, wrapped.signaturePublicKey, wrapped.signaturePrivateKey);
    state.value = State.Finished;
  } catch (error) {
    console.error('Creating vault failed.', error);
    onCreateError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

async function copyRecoveryKey() {
  await navigator.clipboard.writeText(recoveryKey.value);
  copiedRecoveryKey.value = true;
  debouncedCopyFinish();
}

async function downloadVaultTemplate() {
  onDownloadTemplateError.value = null;
  try {
    const blob = await vaultConfig.value?.exportTemplate();
    if (blob != null) {
      saveAs(blob, `${vaultName.value}.zip`);
    } else {
      throw new EmptyVaultTemplateError();
    }
  } catch (error) {
    console.error('Exporting vault template failed.', error);
    onDownloadTemplateError.value = error instanceof Error ? error : new Error('Unknown reason');
  }
}
</script>
