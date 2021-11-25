<template>
  <div v-if="state === State.Initial || state == State.Processing">
    <form ref="form" novalidate @submit.prevent="createVault()">
      <div class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">Create Vault</h3>
              <p class="mt-1 text-sm text-gray-500">
                The master password should be kept secret and is only needed for administrative purposes. Please make sure that you remember the password.
              </p>
            </div>

            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-3">
                  <label for="vaultName" class="block text-sm font-medium text-gray-700">Vault Name</label>
                  <input id="vaultName" v-model="vaultName" :disabled="state == State.Processing" type="text" inputmode="numeric" pattern="[0-9]{6}" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
                </div>

                <div class="col-span-6 sm:col-span-4">
                  <label for="password" class="block text-sm font-medium text-gray-700">Master Password</label>
                  <input id="password" v-model="password" :disabled="state == State.Processing" type="password" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" required />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="px-4 py-3 bg-gray-50 text-right sm:px-6">
          <button :disabled="!validVaultName || !validPassword || state == State.Processing" type="submit" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed">
            Create Vault
          </button>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state === State.Finished">
    <div class="flex justify-center">
      <div class="shadow sm:rounded-lg sm:overflow-hidden sm:max-w-lg">
        <div class="text-center bg-white px-4 py-5 sm:p-6">
          <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100">
            <CheckIcon class="h-6 w-6 text-green-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              Vault created
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                After downloading the zipped vault folder, unpack it to any location shared with your team members.
              </p>
            </div>
          </div>
          <div class="mt-5 sm:mt-6">
            <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="downloadVaultTemplate()">
              <DownloadIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
              Download zipped vault folder
            </button>
          </div>
          <div class="mt-2">
            <router-link to="/" class="text-sm text-gray-500">
              Return to vault list
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
import { computed, ref } from 'vue';
import backend from '../common/backend';
import { Masterkey } from '../common/crypto';
import { uuid } from '../common/util';
import { createVaultConfig, createVaultTemplate } from '../common/vaultconfig';

enum State {
  Initial,
  Processing,
  Finished
}

const state = ref(State.Initial);
const vaultName = ref('');
const password = ref('');
const token = ref('');
const rootDirHash = ref('');

const validVaultName = computed(() => vaultName.value.length > 0);
const validPassword = computed(() => password.value.length >= 8);

async function createVault() {
  state.value = State.Processing;
  const masterkey = await Masterkey.create();
  const vaultId = uuid();
  token.value = await createVaultConfig(vaultId,masterkey);
  const wrapped = await masterkey.wrap(password.value);
  backend.vaults.createVault(vaultId, vaultName.value, wrapped.encrypted, wrapped.iterations, wrapped.salt).then(() => {
    masterkey.hashDirectoryId('').then(hash => {
      rootDirHash.value = hash;
      state.value = State.Finished;
    });
  });
}

async function downloadVaultTemplate() {
  if (state.value === State.Finished) {
    const blob = await createVaultTemplate(rootDirHash.value, token.value);
    saveAs(blob, `${vaultName.value}.zip`);
  }
}
</script>
