<template>
  <div
    class="rounded-md p-4 mb-3 ring-1"
    :class="[typeStyles.bg, typeStyles.border]"
  >
    <div class="flex">
      <div class="shrink-0">
        <component :is="typeStyles.icon" class="h-5 w-5" :class="[typeStyles.iconColor]" aria-hidden="true" />
      </div>
      <div class="ml-3 flex-1">
        <h3 v-if="props.title" class="text-sm font-medium" :class="[typeStyles.titleColor]">
          {{ props.title }}
        </h3>
        <div :class="[props.title ? 'mt-2' : '', 'text-sm', typeStyles.textColor]">
          <slot></slot>
          <a
            v-if="props.linkText && props.linkUrl"
            :href="props.linkUrl"
            target="_blank"
            class="ml-1 inline-flex items-center text-primary underline hover:text-primary-darker"
          >
            {{ props.linkText }}
            <ArrowRightIcon class="ml-1 h-4 w-4" aria-hidden="true" />
          </a>
        </div>
      </div>
      <div v-if="props.dismissible" class="ml-auto pl-3">
        <div class="-mx-1.5 -my-1.5">
          <button
            type="button"
            class="inline-flex rounded-md p-1.5 focus:outline-none focus:ring-2 focus:ring-offset-2"
            :class="[typeStyles.dismissButton]"
            @click="emit('dismiss')"
          >
            <span class="sr-only">Dismiss</span>
            <XMarkIcon class="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ExclamationTriangleIcon, XCircleIcon, InformationCircleIcon, ArrowRightIcon, XMarkIcon } from '@heroicons/vue/20/solid';

const props = withDefaults(defineProps<{
  type?: 'info' | 'warning' | 'error';
  title?: string;
  linkText?: string;
  linkUrl?: string;
  dismissible?: boolean;
}>(), {
  type: 'info',
  dismissible: false
});

const emit = defineEmits<{
  dismiss: [];
}>();

const typeStyles = computed(() => {
  switch (props.type) {
    case 'warning':
      return {
        bg: 'bg-yellow-50',
        border: 'ring-yellow-300/70',
        icon: ExclamationTriangleIcon,
        iconColor: 'text-yellow-400',
        titleColor: 'text-yellow-800',
        textColor: 'text-yellow-700',
        dismissButton: 'bg-yellow-50 text-yellow-500 hover:bg-yellow-100 focus:ring-yellow-600 focus:ring-offset-yellow-50'
      };
    case 'error':
      return {
        bg: 'bg-red-50',
        border: 'ring-red-300/70',
        icon: XCircleIcon,
        iconColor: 'text-red-400',
        titleColor: 'text-red-800',
        textColor: 'text-red-700',
        dismissButton: 'bg-red-50 text-red-500 hover:bg-red-100 focus:ring-red-600 focus:ring-offset-red-50'
      };
    default: // info
      return {
        bg: 'bg-blue-50',
        border: 'ring-blue-300/70',
        icon: InformationCircleIcon,
        iconColor: 'text-blue-400',
        titleColor: 'text-blue-800',
        textColor: 'text-blue-700',
        dismissButton: 'bg-blue-50 text-blue-500 hover:bg-blue-100 focus:ring-blue-600 focus:ring-offset-blue-50'
      };
  }
});
</script>
