<template>
  <TransitionRoot as="template" :show="open">
    <Dialog as="div" class="fixed inset-0 z-10 overflow-y-auto" @close="open = false">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in  duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75" @click.stop />
      </TransitionChild>
      <div class="flex min-h-full items-end justify-center p-4 sm:items-center sm:p-0">
        <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
          <DialogPanel class="relative flex w-full h-[32rem] transform flex-col overflow-hidden rounded-lg bg-white shadow-xl transition-all sm:my-8 sm:max-w-lg">
            <form novalidate class="flex flex-1 min-h-0 flex-col p-4" @submit.prevent="onSubmit">
              <div class="flex flex-1 min-h-0 flex-col overflow-hidden bg-white p-1 mb-4">
                <DialogTitle class="text-lg font-medium text-gray-900 mx-1">
                  {{ t('user.addGroups.title') }}
                </DialogTitle>
                <div class="flex flex-col p-1 mt-4">
                  <SearchInputGroup :action-title="t('common.add')" :place-holder="t('user.addGroups.searchLabel')" :on-search="searchGroup" @action="addGroup" />
                  <p v-if="onAddGroupError" class="mt-1 text-sm text-red-900 text-right">
                    {{ t('common.unexpectedError', [onAddGroupError.message]) }}
                  </p>
                </div>

                <div ref="scrollContainer" class="mt-4 flex flex-1 min-h-0 flex-col overflow-y-auto">
                  <TransitionGroup tag="ul" class="flex-1 min-h-0 divide-y divide-gray-200" enter-active-class="transition-all duration-250 ease-out" enter-from-class="bg-green-50 opacity-0 scale-95" enter-to-class="bg-white opacity-100 scale-100" leave-active-class="transition-all duration-250 ease-in" leave-from-class="bg-white opacity-100 scale-100" leave-to-class="bg-red-50 opacity-0 scale-95">
                    <li v-for="group in sortedNewGroups" :key="group.id" class="flex flex-col py-2 border-b border-gray-200 border-l-4 border-transparent mx-1 last:border-b-0 transform transition">
                      <div class="flex items-center justify-between">
                        <div class="flex items-center w-full" :title="group.name">
                          <div class="w-8 h-8 rounded-full border border-gray-300 bg-white flex items-center justify-center overflow-hidden">
                            <img :src="group.pictureUrl" class="w-full h-full object-cover" alt="group icon" />
                          </div>
                          <p class="ml-4 text-sm font-medium truncate">
                            {{ group.name }}
                          </p>
                        </div>
                        <button type="button" class="cursor-pointer text-red-600 hover:text-red-900" :title="t('common.remove')" @click="removeTempGroup(group.id)">{{ t('common.remove') }}</button>
                      </div>
                    </li>
                  </TransitionGroup>
                </div>
              </div>
              <div class="flex-shrink-0 bg-gray-50 -m-4 py-3 px-6 sm:flex sm:flex-row-reverse sm:space-x-4 sm:space-x-reverse">
                <button type="submit" class="w-full sm:w-auto inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">
                  {{ t('common.save') }}
                </button>
                <button type="button" class="mt-3 sm:mt-0 w-full sm:w-auto inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="open = false">
                  {{ t('common.cancel') }}
                </button>
                <span class="mt-3 sm:mt-0 flex items-center text-sm text-gray-600 sm:mr-auto">
                  {{ selectedCount }}
                  {{ t('user.addGroups.selectedGroups') }}
                </span>
              </div>
            </form>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ref, computed, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import SearchInputGroup from '../SearchInputGroup.vue';
import backend, { GroupDto } from '../../common/backend';

const scrollContainer = ref<HTMLElement | null>(null);

const props = defineProps<{ groups: GroupDto[]; userId: string }>();
const emit = defineEmits<{ saved: [added: GroupDto[]] }>();

const { t } = useI18n({ useScope: 'global' });
const open = ref(false);
const newGroups = ref<GroupDto[]>([]);
const onAddGroupError = ref<Error | null>(null);

const selectedCount = computed(() => newGroups.value.length);
const sortedNewGroups = computed(() => [...newGroups.value].reverse());

function isKnown(id: string) {
  return props.groups.some(g => g.id === id);
}

async function searchGroup(query: string): Promise<GroupDto[]> {
  if (!query.trim()) return [];

  try {
    const results = await backend.authorities.search(query, true);

    return results
      .filter((r): r is GroupDto =>
        r.type === 'GROUP'
        && !isKnown(r.id)
        && !newGroups.value.some(n => n.id === r.id)
      )
      .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))
      .map(g => ({
        id: g.id,
        type: g.type,
        name: g.name,
        description: undefined,
        memberSize: g.memberSize,
        pictureUrl: g.pictureUrl
      }));
  } catch (error) {
    console.error('Search groups failed:', error);
    return [];
  }
}

function addGroup(group: GroupDto) {
  try {
    if (isKnown(group.id) || newGroups.value.some(g => g.id === group.id)) return;
    newGroups.value.push(group);

    nextTick(() => {
      if (scrollContainer.value) {
        scrollContainer.value.scrollTop = 0;
      }
    });
  } catch (e) {
    onAddGroupError.value = e as Error;
  }
}

function removeTempGroup(id: string) {
  newGroups.value = newGroups.value.filter(g => g.id !== id);
}

async function onSubmit() {
  onAddGroupError.value = null;

  try {
    for (const group of newGroups.value) {
      await backend.groups.addMember(group.id, props.userId);
    }

    emit('saved', [...newGroups.value]);
    open.value = false;
  } catch (error) {
    console.error('Adding user to groups failed:', error);
    onAddGroupError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function show() {
  newGroups.value = [];
  open.value = true;
}

defineExpose({ show });
</script>
