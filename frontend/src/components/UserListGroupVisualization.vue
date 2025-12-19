<template>
  <div v-if="users.length" class="relative inline-flex -space-x-2">
    <template v-for="u in preview.list" :key="u.id">
      <div
        class="relative h-8 w-8 rounded-full ring-1 ring-gray-200 bg-white overflow-hidden
               flex items-center justify-center"
      >
        <img
          v-if="getAvatarUrl(u)"
          :src="getAvatarUrl(u)"
          :alt="u.name"
          class="h-full w-full object-cover"
        />
        <div
          v-else
          class="h-full w-full flex items-center justify-center
                 text-[9px] font-semibold text-gray-700"
        >
          {{ initials(u.name) }}
        </div>
      </div>
    </template>

    <div
      v-if="preview.extra > 0"
      class="relative z-10 h-8 w-8 rounded-full ring-1 ring-gray-200 bg-gray-200
             flex items-center justify-center text-[10px] font-semibold text-gray-700"
      style="margin-left: 4px;"
    >
      +{{ preview.extra }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

export type UserVisualItem = {
  id: string;
  name: string;
  pictureUrl?: string;
  avatarUrl?: string;
  imageUrl?: string;
};

const props = withDefaults(defineProps<{
  users: UserVisualItem[];
  max?: number;
}>(), {
  max: 3,
});

const preview = computed(() => {
  const extra = Math.max(0, props.users.length - props.max);
  return {
    list: props.users.slice(0, props.max),
    extra,
  };
});

function getAvatarUrl(u: any): string | undefined {
  return u?.pictureUrl || u?.avatarUrl || u?.imageUrl;
}

function initials(name: string): string {
  return (name ?? '')
    .split(' ')
    .map(p => p.trim()[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase();
}
</script>
