<template>
  <span class="inline-flex items-baseline gap-2">
    <span v-if="name != null" class="text-sm text-gray-900">{{ name }}</span>
    <code class="text-xs" :class="{ 'text-gray-600': name != null }">{{ id }}</code>
  </span>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import auditlog from '../common/auditlog';
import type { AuthorityDto } from '../common/backend';

const props = defineProps<{
  id: string
}>();

const name = ref<string>();

watch(() => props.id, async (id) => {
  name.value = undefined;
  try {
    const authority: AuthorityDto = await auditlog.entityCache.getAuthority(id);
    if (props.id === id) { // ignore out-of-order resolution if the id changed meanwhile
      name.value = authority?.name;
    }
  } catch {
    if (props.id === id) {
      name.value = undefined;
    }
  }
}, { immediate: true });
</script>
