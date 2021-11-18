<template>
  <Navbar />

  <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
    <router-view></router-view>
  </div>
</template>

<script setup lang="ts">
import Navbar from './Navbar.vue';
</script>
