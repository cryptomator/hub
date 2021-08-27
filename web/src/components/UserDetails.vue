<template>
  <h1>Details of {{ username }}</h1>
  <div v-if="user == null">
    Loading...
  </div>
  <div v-else>
    <h2>Devices with access to</h2>
    <ul>
      <ul v-if="user.devices.length > 0">
        <li v-for="device in user.devices" :key="device.name">📱 {{ device.name }}: {{ device.vaultsAccessTo }}</li>
      </ul>
    </ul>
  </div>
</template>

<script lang="ts">
import { UserDto } from '../common/backend'
import { defineComponent } from 'vue'
import services from '../common/backend'

export default defineComponent({
  name: 'UserDetails',
  props: {
  },

  data: () => ({
    username: '' as string,
    user: null as UserDto | null
  }),

  mounted() {
    services.users.me().then(username => {
      this.username = username;
    });

    services.users.meIncludingDevices().then(user => {
      this.user = user;
    });
  },

  methods: {
  }
})
</script>
