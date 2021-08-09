<template>
  <h1>Unlock {{ vaultId }}</h1>

  <div v-if="state === State.Processing">
    TODO: show spinner?
  </div>
  <div v-else-if="state === State.Unlocked">
    Vault unlocked successfully. You may close this window now.
  </div>
  <div v-else-if="state === State.ChangedPublicKey">
    This device reports a new public key. Please ask your admin to remove it.
  </div>
  <div v-else-if="state === State.NewDevice">
    This is the first time you try to unlock this vault.
    <input v-model="deviceName" type="text" placeholder="Device Name">
    <button @click="createDevice()">Request Access</button>
  </div>
</template>

<script lang="ts">
import backend from '../common/backend'
import { defineComponent } from 'vue'
import { AxiosError } from 'axios';
import { CallbackService } from '../common/callback'

enum State {
  Processing,
  ChangedPublicKey,
  NewDevice,
  Unlocked,
}

export default defineComponent({
  name: 'Other',
  props: {
    vaultId: {
      type: String,
      default: null
    },
    deviceId: {
      type: String,
      default: null
    },
    deviceKey: {
        type: String,
        default: null
    },
    redirectTo: {
        type: String,
        default: null
    }
  },
  data: () => ({
    State,
    state: State.Processing as State,
    deviceName: '' as string,
  }),
  mounted() {
    backend.devices.getDevice(this.deviceId).then(device => {
      if(device.data.publicKey === this.deviceKey) {
        this.getKeyForThisVault()
      } else {
        this.$data.state = State.ChangedPublicKey; // TODO redirect CANCELLED
      }
    }).catch((error: AxiosError) => {
      if (error.response?.status === 404) {
        this.$data.state = State.NewDevice; // TODO redirect CANCELLED
      } else {
        throw error; // TODO redirect CANCELLED(/ERROR?)
      }
    });
  },
  methods: {
    async createDevice() {
      await backend.devices.createDevice(this.deviceId, this.$data.deviceName, this.deviceKey)
    },
    getKeyForThisVault() {
      backend.vaults.getKeyFor(this.vaultId, this.deviceId)
      .then(response => {
        return new CallbackService(this.$props.redirectTo).unlockSuccess(response.data.vault_specific_masterkey, response.data.ephemeral_public_key);
      })
      .then(response => {
        this.$data.state = State.Unlocked;
      })
      .catch((error: AxiosError) => {
        if (error.response?.status === 404) {
          console.warn('device not authorized'); // TODO redirect UNAUTHORIZED
        } else {
          throw error; // TODO redirect CANCELLED(/ERROR?)
        }
      });
    }
  }
})
</script>
