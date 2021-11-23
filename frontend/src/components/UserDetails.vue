<template>
  <h1>{{ t('user_details_title') }} {{ user?.name }}</h1>
  <div v-if="user == null">
    Loading...
  </div>
  <div v-else>
    <h2>Devices with access to</h2>
    <ul v-if="user.devices.length > 0">
      <li v-for="device in user.devices" :key="device.name">
        📱 {{ device.name }}:
        <ul v-if="device.accessTo.length > 0">
          <li v-for="vault in device.accessTo" :key="vault.name"><a :href="'http://localhost:3000/#/vaults/' + vault.id">{{ vault.name }}</a></li>
        </ul>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { UserDto } from '../common/backend';
import { ref, onMounted } from 'vue';
import services from '../common/backend';
import { useI18n } from 'vue-i18n';

const { t } = useI18n({ useScope: 'global' });

const user = ref<UserDto | null>(null);

onMounted(async () => {
  user.value = await services.users.me(true, true);
});
</script>
