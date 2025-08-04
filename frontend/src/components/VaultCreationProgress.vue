<template>
  <div class="flex items-center">
    <template v-for="(step, index) in steps" :key="step">
      <div class="flex items-center">
        <div
          class="relative z-10 flex h-7 w-7 items-center justify-center rounded-full border-3 cursor-default"
          :class="{
            'bg-primary border-primary text-white': index < currentStep,
            'bg-white border-gray-300 text-black': index > currentStep,
            'bg-white border-primary text-white': index === currentStep
          }"
          :title="tooltipForStep(step)"
        >
          <template v-if="index < currentStep">
            <CheckIcon class="text-white" />
          </template>
          <template v-else>
            <span class="h-1.5 w-1.5 rounded-full block" :class="index === currentStep ? 'bg-primary' : 'bg-gray-300'"/>
          </template>
        </div>
        <div
          v-if="index < steps.length - 1"
          class="w-10 sm:w-12 h-1"
          :class="index < currentStep ? 'bg-primary' : 'bg-gray-300'"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { CheckIcon } from '@heroicons/vue/24/solid';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

enum State {
  Initial,
  EnterRecoveryKey,
  EnterVaultDetails,
  ShowRecoveryKey,
  Finished
}

const props = defineProps<{
  state: State;
  steps: State[];
}>();

const { t } = useI18n({ useScope: 'global' });

const currentStep = computed(() => props.steps.indexOf(props.state));

const tooltipForStep = (step: State): string => {
  switch (step) {
    case State.EnterVaultDetails:
      return t('createVault.enterVaultDetails.title');
    case State.ShowRecoveryKey:
      return t('createVault.showRecoveryKey.title');
    case State.Finished:
      return t('createVault.success.title');
    default:
      return '';
  }
};
</script>
