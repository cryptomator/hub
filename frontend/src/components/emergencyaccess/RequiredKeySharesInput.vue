<template>
  <div class="mt-4">
    <template v-if="allowChangingDefaults">
      <label for="keyshares" class="block text-sm font-medium text-gray-700">
        Required Emergency Key Shares
      </label>
      <div class="mt-1">
        <input
          id="keyshares"
          v-model.number="localValue"
          type="number"
          min="1"
          class="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary sm:text-sm"
        />
      </div>
    </template>
    <template v-else>
      <div class="text-sm text-gray-500">
        {{ t('vaultDetails.actions.requiredEmergencyKeyShares') }}:
        <span class="font-medium text-gray-900">{{ defaultKeyShares }}</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  allowChangingDefaults: boolean;
  defaultKeyShares: number;
  modelValue: number;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: number): void;
}>();

// Lokales computed property mit getter/setter für v-model
const localValue = computed({
  get: () => props.modelValue,
  set: (val: number) => emit('update:modelValue', val),
});

</script>
