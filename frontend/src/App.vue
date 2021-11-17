<template>
  <vue-notification-list position="top-right"></vue-notification-list>
  <Navbar v-show="showNavbar" />

  <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
    <router-view></router-view>
  </div>
</template>

<script lang="ts">
import Navbar from './components/Navbar.vue';
import { useI18n } from 'vue-i18n';

export default {
  name: 'App',
  components: {
    Navbar,
    //Footer,
  },
  setup() {
    const { t } = useI18n({
      useScope: 'global'
    });
    return { t };
  },
  computed: {
    showNavbar() {
      return this.$route.meta.navbar == null ? true : this.$route.meta.navbar;
    },
    /*showFooter() {
      return this.$route.meta.footer == null ? true : this.$route.meta.footer;
    },*/
  },
};
</script>
