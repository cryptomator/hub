<template>
  <component :is="content" v-if="content" />
  <p v-if="text">{{ text }}</p>
</template>

<script setup lang="ts">
import type { Component } from 'vue';

export type TourPopoverProps = {
  content?: Component
  text?: string
};

defineProps<TourPopoverProps>();
</script>
