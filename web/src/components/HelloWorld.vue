<template>
  <h2>hello {{ username }}</h2>
</template>

<script lang="ts">
import { ref, defineComponent } from 'vue'
import services from '../common/backend'

defineComponent((p, c)=>{});

export default defineComponent({
  name: 'HelloWorld',
  data: () => ({
    username: '' as string,
  }),
  mounted() {
    services.users.me().then(username => {
      this.$data.username = username;
    });
  }
  
})
</script>
