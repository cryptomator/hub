<template>
  <svg :width="size" :height="size" viewBox="0 0 64 64" class="shrink-0 overflow-hidden rounded-full ring-2 ring-white" aria-hidden="true">
    <rect width="64" height="64" :fill="preset.background" />
    <!-- the body path overruns the viewBox on purpose; rounded-full plus overflow-hidden crop it to a circle -->
    <path d="M8 66 Q9 41 32 41 Q55 41 55 66 Z" :fill="preset.shirt" />
    <rect x="28.5" y="32" width="7" height="10" rx="2.5" :fill="preset.skin" />
    <circle cx="32" cy="27.5" r="9.5" :fill="preset.skin" />
    <path d="M21.2 27 Q21.2 15.8 32 15.8 Q42.8 15.8 42.8 27 Q42.8 21.5 32 21.5 Q21.2 21.5 21.2 27 Z" :fill="preset.hair" />
    <circle v-if="preset.bun" cx="32" cy="13.5" r="4.5" :fill="preset.hair" />
  </svg>
</template>

<script setup lang="ts">
import { AvatarPreset } from './avatars';

withDefaults(defineProps<{
  preset: AvatarPreset
  size?: number
}>(), {
  size: 20
});
</script>
