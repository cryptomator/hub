<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('closed')">
    <Dialog as="div" class="relative z-50" :role="role" :initial-focus="initialFocus" @close="requestClose">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <div class="fixed inset-0 bg-gray-500/75 transition-opacity" aria-hidden="true" />
      </TransitionChild>

      <div class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 sm:items-center sm:p-0">
          <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
            <DialogPanel as="fieldset" :disabled="busy" class="relative min-w-0 transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all w-full sm:my-8" :class="size === 'narrow' ? 'sm:max-w-lg' : 'sm:max-w-lg md:max-w-2xl'">
              <form novalidate @submit.prevent="$emit('submit')">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="flex items-start">
                    <div v-if="icon" class="shrink-0 flex items-center justify-center h-10 w-10 rounded-full mr-4" :class="tone === 'danger' ? 'bg-red-100' : 'bg-gray-100'">
                      <component :is="icon" class="h-6 w-6" :class="tone === 'danger' ? 'text-red-600' : 'text-gray-600'" aria-hidden="true" />
                    </div>
                    <div class="min-w-0 grow">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900 wrap-break-word">
                        {{ title }}
                      </DialogTitle>
                      <slot />
                      <div v-if="error" role="alert" class="mt-4 flex gap-2 rounded-md bg-red-50 p-3 text-sm text-red-700">
                        <ExclamationCircleIcon class="h-5 w-5 shrink-0" aria-hidden="true" />
                        <p>{{ error }}</p>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="flex flex-col-reverse gap-3 bg-gray-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-end sm:px-6">
                  <slot name="footer" />
                </div>
              </form>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ExclamationCircleIcon } from '@heroicons/vue/20/solid';
import { nextTick, watch } from 'vue';
import type { Component } from 'vue';

const open = defineModel<boolean>('open', { required: true });

const props = withDefaults(defineProps<{
  title: string
  icon?: Component
  tone?: 'danger' | 'neutral'
  size?: 'default' | 'narrow'
  busy?: boolean
  error?: string
  initialFocus?: HTMLElement
  role?: 'dialog' | 'alertdialog'
}>(), {
  icon: undefined,
  tone: 'neutral',
  size: 'default',
  error: undefined,
  initialFocus: undefined,
  role: 'dialog'
});

defineEmits<{
  submit: []
  closed: []
}>();

// Controls are disabled while busy, which drops the focus; after a failed action (dialog still open) hand it back.
watch(() => props.busy, (busy, wasBusy) => {
  if (wasBusy && !busy && open.value) {
    nextTick(() => props.initialFocus?.focus());
  }
});

function requestClose() {
  if (!props.busy) {
    open.value = false;
  }
}
</script>
