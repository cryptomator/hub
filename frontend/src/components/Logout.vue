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
import authPromise from '../common/auth';


export default defineComponent({
  data: () => ({
    loggedIn: false as boolean,
  }),

  async mounted() {
    const auth = await authPromise;
    this.loggedIn = auth.isAuthenticated();
  },

  methods: {
    async logout() {
      const auth = await authPromise;
      auth.logout();
      this.loggedIn = false;
    }
  }
})
</script>

