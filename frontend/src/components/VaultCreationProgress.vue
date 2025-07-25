<template>
  <div class="flex items-center">
    <template v-for="(_, index) in 4" :key="index">
      <div class="flex items-center">
        <div
          class="relative z-10 flex h-7 w-7 items-center justify-center rounded-full border-3 cursor-default"
          :class="{
            'bg-primary border-primary text-white': index < currentStep,
            'bg-white border-gray-300 text-black': index > currentStep,
            'bg-white border-primary text-white': index == currentStep
          }"
          :title="tooltipForStep(index)"
        >
          <template v-if="index < currentStep">
            <CheckIcon class="text-white"></CheckIcon>
          </template>
          <template v-if="index > currentStep">
            <span class="h-1.5 w-1.5 rounded-full bg-gray-300 block"></span>
          </template>
          <template v-if="index == currentStep">
            <span class="h-1.5 w-1.5 rounded-full bg-primary block"></span>
          </template>
        </div>
        <div
          v-if="index < 3"
          class="w-10 sm:w-12 h-1"
          :class="{
            'bg-primary': index < currentStep,
            'bg-gray-300': index >= currentStep,
          }"
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
  DefineEmergencyAccess,
  ShowRecoveryKey,
  Finished
}

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  state: State;
}>();

const currentStep = computed(() => {
  switch (props.state) {
    case State.EnterVaultDetails:
      return 0;
    case State.DefineEmergencyAccess:
      return 1;
    case State.ShowRecoveryKey:
      return 2;
    case State.Finished:
      return 3;
    default:
      return -1;
  }
});

const tooltipForStep = (index: number): string => {
  switch (index) {
    case 0:
      return t('createVault.enterVaultDetails.title');
    case 1:
      return t('createVault.emergencyAccessDetails.title');
    case 2:
      return t('createVault.showRecoveryKey.title');
    case 3:
      return t('createVault.success.title');
    default:
      return '';
  }
};

</script>
