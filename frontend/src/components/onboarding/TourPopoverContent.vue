<template>
  <component :is="vignette" v-if="vignette" />
  <p v-if="text">{{ text }}</p>
</template>

<script setup lang="ts">
import type { Component } from 'vue';

export type TourPopoverProps = {
  vignette?: Component
  text?: string
};

defineProps<TourPopoverProps>();
</script>
