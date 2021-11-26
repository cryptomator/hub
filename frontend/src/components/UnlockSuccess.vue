<template>
  <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">
    <div v-if="me == null">
      Loading…
    </div>

    <div v-else>
      <div class="pt-8 pb-4 flex-shrink-0 flex items-center">
        <img src="/logo.svg" class="h-8" alt="Logo"/>
        <span class="font-headline font-bold text-primary ml-2 pb-px">CRYPTOMATOR HUB</span>
      </div>

      <div class="relative shadow-xl sm:rounded-2xl sm:overflow-hidden">
        <div class="absolute inset-0">
          <div class="absolute inset-0 bg-gradient-to-r from-primary-l1 to-primary mix-blend-multiply" />
        </div>
        <div class="relative px-4 py-16 sm:px-6 sm:py-24 lg:py-32 lg:px-8">
          <h1 class="text-center text-4xl font-extrabold tracking-tight sm:text-5xl lg:text-6xl text-white">
            Welcome back, {{ me.name }}!
          </h1>
          <!-- TODO: can we figure out if the _current_ device is on the device list? -->
          <div v-if="me.devices.length == 0" class="max-w-lg mx-auto text-center text-xl text-primary-l2 sm:max-w-3xl">
            <p class="mt-6">
              You have no registered devices.
            </p>
            <p class="mt-3">
              Please return to Cryptomator and register your device.
            </p>
          </div>

          <div v-else class="max-w-lg mx-auto text-center text-xl text-primary-l2 sm:max-w-3xl">
            <p class="mt-6">
              Your unlock was successful.
            </p>
            <p class="mt-3">
              You can now close this page and continue using Cryptomator.
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import backend, { UserDto } from '../common/backend';

const me = ref<UserDto>();

onMounted(async () => {
  try {
    me.value = await backend.users.me(true, true);
  } catch (error) {
    // TODO: error handling
    console.error('Retrieving user information failed.', error);
  }
});
</script>
