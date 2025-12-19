<template>
  <div
    class="invisible opacity-0 group-hover:visible group-hover:opacity-100
            transition-opacity duration-150
            absolute left-0 top-9 z-20 w-60 rounded-lg border
            border-gray-200 bg-white p-3 shadow-xl"
  >
    <div class="flex items-center justify-between mb-1">
      <div>
        <div class="text-xl">Vault Council</div>
        <div class="text-xs text-gray-500 mb-2">
          Required KeyShares: {{ requiredKeyShares }}
        </div>
      </div>

      <SegmentRing
        :total="requiredKeyShares"
        :completed="completed"
        :size="42"
      />
    </div>

    <ul class="space-y-1 max-h-56 overflow-auto pr-1">
      <li
        v-for="m in sortedMembers"
        :key="m.id"
        class="flex items-center gap-2 text-sm h-6"
      >
        <img
          v-if="getAvatarUrl(m)"
          :src="getAvatarUrl(m)"
          class="h-4 w-4 rounded-full"
        />
        <span class="truncate">{{ m.name }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import SegmentRing from './SegmentRing.vue';
import UserListGroupVisualization, {
  UserVisualItem,
} from '../UserListGroupVisualization.vue';

const props = defineProps<{
  members: UserVisualItem[];
  requiredKeyShares: number;
  completed: number;
}>();

const sortedMembers = computed(() =>
  [...props.members].sort((a, b) => a.name.localeCompare(b.name))
);

function getAvatarUrl(u: any): string | undefined {
  return u?.pictureUrl || u?.avatarUrl || u?.imageUrl;
}
</script>
