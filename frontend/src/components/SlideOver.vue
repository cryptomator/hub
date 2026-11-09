<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('close')">
    <Dialog as="div" class="fixed inset-0 overflow-hidden" :initial-focus="panel" @close="open = false">
      <div class="absolute inset-0 overflow-hidden">
        <DialogOverlay class="absolute inset-0" />

        <div class="fixed inset-y-0 right-0 sm:pl-10 max-w-full flex">
          <TransitionChild as="template" enter=" transition ease-in-out duration-500 sm:duration-700" enter-from="translate-x-full" enter-to="translate-x-0" leave=" transition ease-in-out duration-500 sm:duration-700" leave-from="translate-x-0" leave-to="translate-x-full">
            <div class="w-screen max-w-md">
              <div ref="panel" tabindex="-1" class="h-full flex flex-col py-6 bg-white shadow-xl overflow-y-scroll focus:outline-hidden">
                <div class="px-4 sm:px-6">
                  <div class="flex items-center justify-between gap-3">
                    <div class="flex min-w-0 items-center gap-1">
                      <DialogTitle class="min-w-0 wrap-break-word text-lg font-medium text-gray-900">
                        {{ title }}
                      </DialogTitle>
                      <slot name="title-actions"></slot>
                    </div>
                    <div class="flex shrink-0 items-center gap-1">
                      <slot name="actions"></slot>
                      <button type="button" class="inline-flex size-8 items-center justify-center rounded-md text-gray-400 hover:bg-gray-100 hover:text-gray-500 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-primary sm:hidden" @click="open = false">
                        <span class="sr-only">{{ t('common.close') }}</span>
                        <XMarkIcon class="h-6 w-6" aria-hidden="true" />
                      </button>
                    </div>
                  </div>
                </div>
                <div class="mt-1 relative flex-1 px-4 sm:px-6">
                  <slot></slot>
                </div>
              </div>
            </div>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { XMarkIcon } from '@heroicons/vue/24/outline';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n({ useScope: 'global' });

defineProps<{
  title: string
}>();

defineEmits<{
  close: []
}>();

defineExpose({
  show
});

const open = ref(false);
const panel = ref<HTMLElement>();

function show() {
  open.value = true;
}
</script>
