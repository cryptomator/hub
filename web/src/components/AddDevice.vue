<template>
  <h1>Add Device {{ deviceId }}</h1>

  <p>
    This device is not yet known to Cryptomator Hub. Before a vault owner can grant access,
    you need to verify this device.
  </p>
  <input v-model="verificationCode" type="text" placeholder="Verification Code" @change="verifyCode()" @keyup="verifyCode()">
  <input v-model="deviceName" type="text" placeholder="Device Name">
  <button :disabled="!validVerificationCode" @click="createDevice()">Create Device</button>
</template>

<script lang="ts">
import backend from '../common/backend'
import { defineComponent } from 'vue'
import { base64url } from "rfc4648";

export default defineComponent({
  props: {
    deviceId: {
      type: String,
      default: null
    },
    deviceKey: {
        type: String,
        default: null
    },
    verificationHash: {
        type: String,
        default: null
    }
  },
  data: () => ({
    deviceName: '' as string,
    verificationCode: '' as string,
    validVerificationCode: false as boolean,
  }),
  methods: {
    async createDevice() {
      await backend.devices.createDevice(this.deviceId, this.deviceName, this.deviceKey);
    },
    async verifyCode() {
      const encoder = new TextEncoder();
      const data = encoder.encode(this.deviceId + this.deviceKey + this.verificationCode);
      const hash = await crypto.subtle.digest('SHA-256', data);
      const actualVerificationCode = base64url.stringify(new Uint8Array(hash)).replaceAll("=", "");
      this.validVerificationCode = actualVerificationCode === this.verificationHash;
      console.log("valid: ", this.validVerificationCode);
    }
  }
})
</script>
