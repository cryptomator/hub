<template>
  <!-- object: one row per entry; primitive values inline, nested values indented below -->
  <dl v-if="kind === 'object'" class="flex flex-col gap-1">
    <div v-for="([key, val]) in objectEntries" :key="key" :class="isObject(val) ? 'flex flex-col gap-1' : 'flex items-baseline gap-2'">
      <dt class="text-xs text-gray-500">
        <code>{{ key }}</code>
      </dt>
      <dd class="text-sm text-gray-900" :class="{ 'pl-3 border-l border-gray-100': isObject(val) }">
        <AuditLogJsonView :value="val" :force-id="isAuthorityId(key)" />
      </dd>
    </div>
  </dl>

  <!-- array: one item per line; id-ness propagates from the enclosing key -->
  <ul v-else-if="kind === 'array'" class="flex flex-col gap-1">
    <li v-for="(item, i) in arrayItems" :key="i" :class="{ 'pl-3 border-l border-gray-100': isObject(item) }">
      <AuditLogJsonView :value="item" :force-id="forceId" />
    </li>
  </ul>

  <!-- resolvable id (enclosing key is whitelisted as id-bearing) -->
  <AuditLogResolvedId v-else-if="resolvableId != null" :id="resolvableId" />

  <!-- primitives -->
  <code v-else-if="typeof value === 'number' || typeof value === 'boolean'" class="text-xs text-gray-600">{{ value }}</code>
  <span v-else-if="value === null || value === undefined || value === ''" class="text-gray-400">&lt;empty&gt;</span>
  <span v-else class="text-sm text-gray-900 break-words">{{ value }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { AUTHORITY_ID_KEYS } from '../common/auditlog';
import AuditLogResolvedId from './AuditLogResolvedId.vue';

const props = defineProps<{
  value: unknown,
  forceId?: boolean
}>();

function isAuthorityId(key: string): boolean {
  return AUTHORITY_ID_KEYS.has(key);
}

function isObject(v: unknown): boolean {
  return v !== null && typeof v === 'object';
}

const kind = computed<'object' | 'array' | 'other'>(() => {
  if (Array.isArray(props.value)) {
    return 'array';
  } else if (props.value !== null && typeof props.value === 'object') {
    return 'object';
  } else {
    return 'other';
  }
});

const objectEntries = computed<[string, unknown][]>(() =>
  kind.value === 'object' ? Object.entries(props.value as Record<string, unknown>) : []
);

const arrayItems = computed<unknown[]>(() =>
  Array.isArray(props.value) ? props.value : []
);

const resolvableId = computed<string | null>(() => {
  const v = props.value;
  if (typeof v === 'string' && v !== '' && props.forceId) {
    return v;
  } else {
    return null;
  }
});
</script>
