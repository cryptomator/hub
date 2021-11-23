<template>
  <div v-if="state === State.Processing">
    <form class="space-y-6" @submit.prevent="createVault()">
      <div>
        <label for="vaultName" class="block text-sm font-medium text-gray-700">
          Vault Name
        </label>
        <div class="mt-1">
          <input id="vaultName" v-model="vaultName" name="vaultName" type="text" required="" class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-primary focus:border-primary sm:text-sm" />
        </div>
      </div>

      <div>
        <label for="password" class="block text-sm font-medium text-gray-700">
          Master Password
        </label>
        <div class="mt-1">
          <input id="password" v-model="password" name="password" type="password" autocomplete="current-password" required="" class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-primary focus:border-primary sm:text-sm" />
        </div>
      </div>

      <div>
        <button type="submit" class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">
          Create Vault
        </button>
      </div>
    </form>
  </div>

  <div v-else-if="state === State.Created">
    <div class="text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
        <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m-9 1V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">Vault created</h3>
      <p class="mt-1 text-sm text-gray-500">
        After downloading the zipped vault folder, unpack it to any location shared with your team members.
      </p>
      <div class="mt-6">
        <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="createVaultFolder()">
          <DownloadIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
          Download zipped vault folder
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DownloadIcon } from '@heroicons/vue/solid';
import { saveAs } from 'file-saver';
import JSZip from 'jszip';
import { ref } from 'vue';
import backend from '../common/backend';
import config from '../common/config';
import { Masterkey, VaultConfigHeaderHub, VaultConfigPayload } from '../common/crypto';
import { uuid } from '../common/util';

enum State {
  Processing,
  Created
}

const state = ref(State.Processing);
const password = ref('');
const vaultName = ref('');
const token = ref('');
const rootDirHash = ref('');

async function createVault() {
  const masterkey = await Masterkey.create();
  const vaultId = uuid();
  const kid = `hub+http://localhost:9090/vaults/${vaultId}`;

  const hubConfig: VaultConfigHeaderHub = {
    clientId: 'cryptomator-hub',
    authEndpoint: `${config.get().keycloakUrl}realms/cryptomator/protocol/openid-connect/auth`, // TODO: read full endpoint url from config
    tokenEndpoint: `${config.get().keycloakUrl}realms/cryptomator/protocol/openid-connect/token`,
    deviceRegistrationUrl: `${location.protocol}//${location.host}${import.meta.env.BASE_URL}#/devices/add`,
    authSuccessUrl: `${location.protocol}//${location.host}${import.meta.env.BASE_URL}#/unlock-success`,
    authErrorUrl: `${location.protocol}//${location.host}${import.meta.env.BASE_URL}#/unlock-error`
  };

  const jwtPayload: VaultConfigPayload = {
    jti: vaultId,
    format: 8,
    cipherCombo: 'SIV_GCM',
    shorteningThreshold: 220
  };

  token.value = await masterkey.createVaultConfig(kid, hubConfig, jwtPayload);
  const wrapped = await masterkey.wrap(password.value);
  backend.vaults.createVault(vaultId, vaultName.value, wrapped.encrypted, wrapped.iterations, wrapped.salt).then(() => {
    masterkey.hashDirectoryId('').then(hash => {
      rootDirHash.value = hash;
      state.value = State.Created;
    });
  });
}

async function createVaultFolder() {
  if (state.value === State.Created) {
    const zip = new JSZip();
    zip.file('vault.cryptomator', token.value);
    zip.folder('d')?.folder(rootDirHash.value.substring(0, 2))?.folder(rootDirHash.value.substring(2));
    const blob = await zip.generateAsync({ type: 'blob' });
    saveAs(blob, `${vaultName.value}.zip`);
  }
}
</script>
