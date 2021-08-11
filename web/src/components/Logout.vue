<template>
  <h1>Goodbye</h1>

  <p v-if="loggedIn">
    <button @click="logout()">Logout</button>
  </p>
  <p v-else>
    Logged out
  </p>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import auth from '../common/auth';

export default defineComponent({
  data: () => ({
    loggedIn: auth.isAuthenticated() as boolean,
  }),

  methods: {
    logout() {
      auth.logout();
      this.loggedIn = true;
    }
  }
})
</script>

