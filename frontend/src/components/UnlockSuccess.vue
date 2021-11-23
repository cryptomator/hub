<template>
  <h1 v-if="me != null" class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">Welcome Back, {{ me.name }}!</h1>
  <p>You have access to this vault on the following devices:</p>
  <ul v-if="me != null" class="list-disc list-inside p-2">
    <li v-for="(device) in me.devices" :key="device.id">{{ device.name }}</li>
  </ul>
  <p class="mt-4">You can now close this page and return to your device.</p>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import backend, { UserDto } from '../common/backend';

const me = ref<UserDto | null>(null);

onMounted(async () => {
  me.value = await backend.users.me(true);
});
</script>
