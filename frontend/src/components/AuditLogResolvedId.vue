<template>
  <span class="inline-flex items-baseline gap-2">
    <span v-if="name != null" class="text-sm text-gray-900">{{ name }}</span>
    <code class="text-xs" :class="{ 'text-gray-600': name != null }">{{ id }}</code>
  </span>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import auditlog from '../common/auditlog';
import type { AuthorityDto } from '../common/backend';

const props = defineProps<{
  id: string
}>();

const name = ref<string>();

onMounted(async () => {
  try {
    const authority: AuthorityDto = await auditlog.entityCache.getAuthority(props.id);
    name.value = authority?.name;
  } catch {
    name.value = undefined;
  }
});
</script>
