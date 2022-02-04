<template>
  <div v-if="state == State.Initial || state == State.Processing">
    <form ref="form" novalidate @submit.prevent="createVault()">
      <div class="shadow sm:rounded-lg sm:overflow-hidden">
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
                  <input id="vaultName" v-model="vaultName" :disabled="state == State.Processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" required />
                  <p v-if="onCreateError instanceof FormValidationFailedError" id="vaultName-required-description" class="mt-2 text-sm text-gray-500">{{ t('common.form.required') }}</p>
                </div>

                <div class="col-span-6 sm:col-span-4">
                  <label for="password" class="block text-sm font-medium text-gray-700">{{ t('createVault.masterPassword') }}</label>
                  <input id="password" v-model="password" :disabled="state == State.Processing" type="password" minlength="8" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" aria-describedby="password-description" required />
                  <p id="password-description" class="mt-2 text-sm text-gray-500">{{ t('createVault.masterPassword.description') }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end items-center px-4 py-3 bg-gray-50 sm:px-6">
          <div v-if="onCreateError != null" >
            <p v-if="onCreateError instanceof ConflictError" class="text-sm text-red-900 mr-4">{{ t('createVault.error.vaultAlreadyExists') }}</p>
            <p v-else-if="onCreateError instanceof FormValidationFailedError" class="text-sm text-red-900 mr-4">{{ t('createVault.error.formValidationFailed') }}</p>
            <p v-else class="text-sm text-red-900 mr-4">{{ t('common.unexpectedError', [onCreateError.message]) }}</p>
          </div>
          <button :disabled="state == State.Processing" type="submit" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
            {{ t('createVault.submit') }}
          </button>
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
              <DownloadIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
              {{ t('createVault.success.download') }}
            </button>
            <p v-if="onDownloadTemplateError != null " class="text-sm text-red-900 mr-4">{{ t('createVault.error.downloadTemplateFailed', [onDownloadTemplateError.message]) }}</p> <!-- TODO: not beautiful-->
          </div>
          <div class="mt-2">
            <router-link to="/" class="text-sm text-gray-500">
              {{ t('createVault.success.return') }}
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckIcon } from '@heroicons/vue/outline';
import { DownloadIcon } from '@heroicons/vue/solid';
import { saveAs } from 'file-saver';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { ConflictError } from '../common/backend';
import { Masterkey } from '../common/crypto';
import { uuid } from '../common/util';
import { VaultConfig } from '../common/vaultconfig';

enum State {
  Initial,
  Processing,
  Finished
}

class FormValidationFailedError extends Error {
  constructor() {
    super('The html form is not correctly filled.');
  }
}

class ExportingTemplateFailedError extends Error {
  constructor() {
    super('Exporting template function returned null.');
  }
}

const { t } = useI18n({ useScope: 'global' });

const form = ref<HTMLFormElement>();

const onCreateError = ref<Error | null >(null);
const onDownloadTemplateError = ref<Error | null>(null);

const state = ref(State.Initial);
const vaultName = ref('');
const password = ref('');
const vaultConfig = ref<VaultConfig>();

async function createVault() {
  onCreateError.value = null;

  if (!form.value?.checkValidity()) {
    onCreateError.value = new FormValidationFailedError();
    return;
  }

  try {
    state.value = State.Processing;
    const vaultId = uuid();
    const masterkey = await Masterkey.create();
    vaultConfig.value = await VaultConfig.create(vaultId, masterkey);
    const wrapped = await masterkey.wrap(password.value);
    await backend.vaults.createVault(vaultId, vaultName.value, wrapped.encrypted, wrapped.iterations, wrapped.salt);
    state.value = State.Finished;
  } catch (error) {
    console.error('Creating vault failed.', error);
    onCreateError.value = error instanceof Error? error : new Error('Unknown Reason');
    state.value = State.Initial;
  }
  return;
}

async function downloadVaultTemplate() {
  onDownloadTemplateError.value = null;
  try {
    const blob = await vaultConfig.value?.exportTemplate();
    if (blob != null) {
      saveAs(blob, `${vaultName.value}.zip`);
    } else {
      throw new ExportingTemplateFailedError();
    }
  } catch (error) {
    console.error('Exporting Template returned failed.', error);
    onDownloadTemplateError.value = error instanceof Error? error : new Error('Unknown Reason');
  }
}

</script>
