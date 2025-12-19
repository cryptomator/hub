<template>
  <div
    v-if="hasProcess"
    class="relative inline-flex rounded-md shadow-sm -space-x-px"
    role="group"
  >
    <button
      type="button"
      class="h-10 inline-flex items-center gap-2 px-2 text-xs font-medium text-gray-700 bg-white
             hover:bg-gray-50 border border-gray-300 rounded-s-md
             focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed"
      :disabled="disabled"
      @mouseenter="openTooltip()"
      @mouseleave="closeTooltip()"
      @click.stop.prevent="toggleTooltip()"
    >
      <SegmentRing
        :total="requiredKeyShares || 0"
        :completed="completedKeyShares || 0"
        :size="24"
      />
    </button>

    <button
      type="button"
      class="h-10 inline-flex flex-col justify-center px-2 text-xs font-medium text-gray-700 bg-white
             hover:bg-gray-50 border border-gray-300 rounded-e-md w-full
             focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed text-left"
      :disabled="disabled"
      @click.stop="emit('click-main')"
    >
      <span class="leading-tight">{{ label }}</span>
      <span class="text-[10px] text-gray-500 leading-tight">
        {{ approvalLabel }}
      </span>
    </button>

    <!-- Tooltip -->
    <div
      class="transition-opacity duration-150 absolute right-0 top-10 z-20 w-60 rounded-lg border
             border-gray-200 bg-white p-3 shadow-xl"
      :class="isTooltipOpen ? 'opacity-100 visible' : 'opacity-0 invisible'"
      role="tooltip"
    >
      <div class="flex items-center justify-between mb-1">
        <div>
          <div class="text-xl">{{ label }}</div>
          <div class="text-xs text-gray-500 mb-2">
            Required KeyShares:
            {{ requiredKeyShares }}
          </div>
        </div>
        <SegmentRing
          :total="requiredKeyShares || 0"
          :completed="completedKeyShares || 0"
          :size="42"
        />
      </div>

      <div>Process council</div>
      <ul class="space-y-1 max-h-56 overflow-auto pr-1">
        <li
          v-for="m in councilMembers"
          :key="m.id"
          class="flex items-center justify-between text-sm h-6"
        >
          <span class="truncate flex items-center gap-2">
            <img v-if="getAvatarUrl(m)" :src="getAvatarUrl(m)" :alt="m.name" class="h-4 w-4 rounded-full" />
            <span class="truncate">{{ m.name || m.id }}</span>
          </span>
          <span
            class="ml-2 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px]"
            :class="recoveredSet.has(m.id)
              ? 'bg-green-50 text-green-700 ring-1 ring-green-200'
              : 'bg-gray-50 text-gray-600 ring-1 ring-gray-200'"
          >
            <span
              class="h-2 w-2 rounded-full"
              :class="recoveredSet.has(m.id) ? 'bg-green-500' : 'bg-gray-300'"
            ></span>
            {{ recoveredSet.has(m.id)
              ? 'Added'
              : 'Pending' }}
          </span>
        </li>
      </ul>
    </div>
  </div>

  <button
    v-else-if="canStart"
    type="button"
    class="w-full sm:w-auto h-10 inline-flex items-center gap-2 rounded-md bg-white px-2 py-1
           text-xs font-medium text-gray-700 shadow-sm ring-1 ring-inset ring-gray-300
           hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
    :disabled="disabled"
    @click.stop="emit('click-main')"
  >
    <PlayIcon class="h-6 w-6 text-primary" />
    <span class="flex flex-col leading-tight text-left">
      <span>{{ label }}</span>
      <span class="text-[10px] text-gray-500">Start process</span>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import SegmentRing from './SegmentRing.vue';
import { PlayIcon } from '@heroicons/vue/24/solid';

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type?: string;
  memberSize?: number;
}

const props = defineProps<{
  label: string;
  approvalLabel?: string;
  disabled?: boolean;
  hasProcess: boolean;
  canStart: boolean;
  requiredKeyShares?: number;
  completedKeyShares?: number;
  councilMembers?: Item[];
  recoveredMemberIds?: string[];
}>();

const emit = defineEmits<{
  (e: 'click-main'): void;
}>();

const { t } = useI18n({ useScope: 'global' });

const isTooltipOpen = ref(false);
const recoveredSet = computed(
  () => new Set(props.recoveredMemberIds ?? []),
);

function openTooltip() {
  isTooltipOpen.value = true;
}

function closeTooltip() {
  isTooltipOpen.value = false;
}

function toggleTooltip() {
  isTooltipOpen.value = !isTooltipOpen.value;
}

function getAvatarUrl(u: Item | any): string | undefined {
  return u?.pictureUrl || u?.avatarUrl || u?.imageUrl || undefined;
}
</script>
