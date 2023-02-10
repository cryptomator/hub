<template>
  <div v-if="state == State.Initial">
    ...
  </div>

  <div v-else-if="state == State.EnterRecoveryKey">
    <form @submit.prevent="recoverKey()">
      <div class="flex justify-center">
        <div class="shadow sm:rounded-lg sm:overflow-hidden sm:max-w-lg">
          <div class="text-center bg-white px-4 py-5 sm:p-6">
            <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
              <CheckIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
            </div>
            <div class="mt-3 sm:mt-5">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ t('createVault.success.title') }}
              </h3>
              <div class="mt-2 space-y-6">
                <p class="text-sm text-gray-500">
                  {{ t('createVault.success.description') }}
                </p>
                <div>
                  <label for="recoveryKey" class="sr-only">Recovery Key for your vault</label>
                  <textarea id="recoveryKey" v-model="recoveryKey" rows="4" name="recoveryKey" class="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"></textarea>
                </div>
              </div>
            </div>

            <div class="flex justify-end items-center px-4 py-3 bg-gray-50 sm:px-6">
              <p v-if="onRecoverError != null" class="text-sm text-red-900 mr-4">{{ t('createVault.error.invalidRecoveryKey') }}</p>
              <button type="submit" :disabled="processing" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                {{ t('createVault.submit') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.EnterVaultDetails">
    <form ref="form" novalidate @submit.prevent="validateVaultDetails()">
      <div class="shadow sm:rounded-lg">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">
                {{ t('createVault.title') }}
              </h3>
              <p class="mt-1 text-sm text-gray-500">
                {{ t('createVault.description') }}
              </p>
            </div>

            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-3">
                  <label for="vaultName" class="block text-sm font-medium text-gray-700">{{ t('createVault.vaultName') }}</label>
                  <input id="vaultName" v-model="vaultName" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" pattern="^(?! )([^\\\/:*?\x22<>|])*(?<! |\.)$" required />
                </div>

                <div class="col-span-6 sm:col-span-4">
                  <label for="vaultDescription" class="block text-sm font-medium text-gray-700">
                    {{ t('createVault.vaultDescription') }}
                    <span class="text-xs text-gray-500">({{ t('common.optional') }})</span></label>
                  <input id="vaultDescription" v-model="vaultDescription" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200"/>
                </div>

                <div class="col-span-6 sm:col-span-4">
                  <label for="password" class="block text-sm font-medium text-gray-700">{{ t('createVault.password') }}</label>
                  <input id="password" v-model="password" :disabled="processing" type="password" minlength="8" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" aria-describedby="password-description" required />
                  <p id="password-description" class="mt-2 text-sm text-gray-500">{{ t('createVault.password.description') }}</p>
                </div>
                <div class="col-span-6 sm:col-span-4">
                  <label for="passwordConfirmation" class="block text-sm font-medium text-gray-700">{{ t('createVault.passwordConfirmation') }}</label>
                  <input id="passwordConfirmation" v-model="passwordConfirmation" :disabled="processing" type="password" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" aria-describedby="password-confirmation-description" required />
                  <p id="password-confirmation-description" class="mt-2 text-sm text-gray-500">
                    <span v-if="passwordMatches && passwordConfirmation.length != 0">{{ t('createVault.passwordConfirmation.passwordsMatch') }}</span>
                    <span v-else-if="!passwordMatches && passwordConfirmation.length != 0">{{ t('createVault.passwordConfirmation.passwordsDoNotMatch') }}</span>
                    <span v-else>&nbsp;</span>
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end items-center px-4 py-3 bg-gray-50 sm:px-6">
          <div v-if="onCreateError != null" >
            <p v-if="onCreateError instanceof ConflictError" class="text-sm text-red-900 mr-4">{{ t('createVault.error.vaultAlreadyExists') }}</p>
            <p v-else-if="onCreateError instanceof FormValidationFailedError" class="text-sm text-red-900 mr-4">{{ t('createVault.error.formValidationFailed') }}</p>
            <p v-else-if="onCreateError instanceof PasswordNotMachingError" class="text-sm text-red-900 mr-4">{{ t('createVault.passwordConfirmation.passwordsDoNotMatch') }}</p>
            <p v-else class="text-sm text-red-900 mr-4">{{ t('common.unexpectedError', [onCreateError.message]) }}</p>
          </div>
          <button type="submit" :disabled="processing" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
            {{ t('createVault.submit') }}
          </button>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.ShowRecoveryKey">
    <form @submit.prevent="createVault()">
      <div class="flex justify-center">
        <div class="shadow sm:rounded-lg sm:overflow-hidden sm:max-w-lg">
          <div class="text-center bg-white px-4 py-5 sm:p-6">
            <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
              <CheckIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
            </div>
            <div class="mt-3 sm:mt-5">
              <h3 class="text-lg leading-6 font-medium text-gray-900">
                {{ t('createVault.success.title') }}
              </h3>
              <div class="mt-2 space-y-6">
                <p class="text-sm text-gray-500">
                  {{ t('createVault.success.description') }}
                </p>
                <div>
                  <label for="recoveryKey" class="sr-only">Recovery Key for your vault</label>
                  <textarea id="recoveryKey" v-model="recoveryKey" readonly rows="4" name="recoveryKey" class="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"></textarea>
                </div>
                <div class="flex items-center">
                  <input id="confirmRecoveryKey" required name="confirmRecoveryKey" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500">
                  <label for="confirmRecoveryKey" class="ml-2 block">Remember me</label>
                </div>
              </div>
            </div>
            
            <div class="flex justify-end items-center px-4 py-3 bg-gray-50 sm:px-6">
              <div v-if="onCreateError != null" >
                <p v-if="onCreateError instanceof ConflictError" class="text-sm text-red-900 mr-4">{{ t('createVault.error.vaultAlreadyExists') }}</p>
                <p v-else-if="onCreateError instanceof FormValidationFailedError" class="text-sm text-red-900 mr-4">{{ t('createVault.error.formValidationFailed') }}</p>
                <p v-else-if="onCreateError instanceof PasswordNotMachingError" class="text-sm text-red-900 mr-4">{{ t('createVault.passwordConfirmation.passwordsDoNotMatch') }}</p>
                <p v-else class="text-sm text-red-900 mr-4">{{ t('common.unexpectedError', [onCreateError.message]) }}</p>
              </div>
              <button type="submit" :disabled="processing" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                {{ t('createVault.submit') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.Finished">
    <div class="flex justify-center">
      <div class="shadow sm:rounded-lg sm:overflow-hidden sm:max-w-lg">
        <div class="text-center bg-white px-4 py-5 sm:p-6">
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
  </div>
</template>

<script setup lang="ts">
import { CheckIcon } from '@heroicons/vue/24/outline';
import { ArrowDownTrayIcon } from '@heroicons/vue/24/solid';
import { saveAs } from 'file-saver';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { ConflictError } from '../common/backend';
import { VaultKeys } from '../common/crypto';
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
const vaultKeys = ref<VaultKeys>();
const recoveryKey = ref<string>();
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

async function recoverKey() {
  if (!recoveryKey.value) {
    throw new Error('Invalid recovery key');
  }
  vaultKeys.value = await VaultKeys.recover(recoveryKey.value);
  state.value = State.EnterVaultDetails;
}

async function validateVaultDetails() {
  if (!form.value?.checkValidity()) {
    throw new FormValidationFailedError();
  } else if (!passwordMatches.value) {
    throw new PasswordNotMachingError();
  }
  if (props.recover) {
    await createVault();
    state.value = State.Finished;
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
    state.value = State.Initial;
  } finally {
    processing.value = false;
  }
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
