<template>
  <form class="">
    <div class="md:flex md:items-center md:justify-between">
      <div class="flex-1 min-w-0">
        <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
          Create new vault
        </h2>
      </div>
    </div>

    <div v-if="state === State.Processing">
      <div class="space-y-8 divide-y divide-gray-200">
        <div class="pt-8">
          <div class="mt-6 grid grid-cols-1 gap-y-6 gap-x-4 sm:grid-cols-6">
            <div class="sm:col-span-3">
              <label for="first-name" class="block text-sm font-medium text-gray-700">
                Vault Name
              </label>
              <div class="mt-1">
                <input v-model="vaultName" type="text" class="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md" />
              </div>
            </div>

            <div class="sm:col-span-3">
              <label for="last-name" class="block text-sm font-medium text-gray-700">
                Masterpassword
              </label>
              <div class="mt-1">
                <input v-model="password" type="password" class="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md" />
              </div>
            </div>
          </div>
          <br>
          <input v-model="vaultName" type="text" placeholder="Vault Name"/>
          <input v-model="password" type="password" placeholder="Masterpassword"/>
        </div>

        <!--<button type="button" @click="createVault()">createVault</button>-->
        <button type="submit" @click="createVault()" class="ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
          Create vault
        </button>
      </div>
    </div>
    <div v-else-if="state === State.Created">
      <h2>Vault created!</h2>

      <button type="button" @click="createVaultFolder()" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
        Download zipped vault folder
        <DownloadIcon class="ml-2 -mr-1 h-5 w-5" aria-hidden="true" />
      </button>
    </div>
  </form>
</template>

<script lang="ts">
import backend from '../common/backend'
import config from '../common/config';
import { uuid } from '../common/util'
import { Masterkey, VaultConfigHeaderHub, VaultConfigPayload } from '../common/crypto'
import { defineComponent } from 'vue'
import JSZip from 'jszip';
import { saveAs } from 'file-saver';
import { DownloadIcon } from '@heroicons/vue/solid';

enum State {
  Processing,
  Created
}

export default defineComponent({
  data: () => ({
    State,
    state: State.Processing as State,
    password: '' as string,
    vaultName: '' as string,
    token: '' as string,
    rootDirHash: '' as string,
  }),

  methods: {
    async createVault() {
      const masterkey = await Masterkey.create();
      const vaultId = uuid();
      const kid = `hub+http://localhost:9090/vaults/${vaultId}`

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

      this.token = await masterkey.createVaultConfig(kid, hubConfig, jwtPayload);
      const wrapped = await masterkey.wrap(this.password);
      backend.vaults.createVault(vaultId, this.vaultName, wrapped.encrypted, wrapped.iterations, wrapped.salt).then(() => {
        masterkey.hashDirectoryId("").then(rootDirHash => {
          this.rootDirHash = rootDirHash
          this.state = State.Created
        })
      })
    },
    async createVaultFolder() {
      if(this.state === State.Created) {
        const zip = new JSZip()
        zip.file("vault.cryptomator", this.token)
        zip.folder("d")?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2))
        zip.generateAsync({type: "blob"}).then((blob) => {
          saveAs(blob, `${this.vaultName}.zip`);
        })
      }
    }
  },
  components: {
    DownloadIcon,
  }
}
)

</script>
