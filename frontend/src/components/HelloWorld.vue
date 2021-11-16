<template>
  <span class="text-primary text-xl" >hello {{ username }}</span>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import services from '../common/backend';

export default defineComponent({
  name: 'HelloWorld',
  data: () => ({
    username: '' as string,
  }),
  mounted() {
    services.users.me().then(username => {
      this.username = username;
    });
  }
});
</script>
