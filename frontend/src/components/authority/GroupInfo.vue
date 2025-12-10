<template>
  <section class="bg-white rounded-lg shadow-sm overflow-hidden">
    <div class="px-6 py-6">
      <div class="flex flex-col items-center justify-center h-full text-center">
        <img :src="group.picture" class="w-48 h-48 rounded-full object-cover border border-gray-300 mb-4" />
        <h2 class="text-xl font-semibold text-gray-900">{{ group.name }}</h2>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
interface GroupInfo {
  id: string;
  name: string;
  picture?: string;
}

defineProps<{
  group: GroupInfo;
}>();
</script>