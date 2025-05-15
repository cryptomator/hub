<template>
  <TransitionRoot as="template" :show="open">
    <Dialog as="div" class="fixed inset-0 z-10 overflow-y-auto">
      <!-- Backdrop ---------------------------------------------------->
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in  duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75" @click.stop />
      </TransitionChild>

      <!-- Panel ------------------------------------------------------->
      <div class="flex min-h-full items-end justify-center p-4 sm:items-center sm:p-0">
        <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
          <DialogPanel
            class="relative flex w-full max-h-[80vh] min-h-[30rem] transform flex-col overflow-hidden rounded-lg bg-white shadow-xl transition-all sm:my-8 sm:max-w-lg">
            <form novalidate class="flex flex-1 min-h-0 flex-col" @submit.prevent="onSubmit">
              <!-- Body -->
              <div class="flex flex-1 min-h-0 flex-col overflow-hidden bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <DialogTitle class="text-lg font-medium text-gray-900">
                  {{ t('user.groups.addDialog.title', 'Add Groups') }}
                </DialogTitle>

                <div class="mt-6 flex flex-1 min-h-0 flex-col overflow-y-auto">
                  <h3 class="font-medium text-gray-900">
                    {{ t('user.groups.addDialog.searchLabel', 'Search by group name or description') }}
                  </h3>

                  <ul class="mt-2 flex-1 min-h-0 divide-y divide-gray-200">
                    <!-- Search input row -->
                    <li class="py-2 flex flex-col p-1">
                      <SearchInputGroup :action-title="t('common.add')" :on-search="searchGroup" @action="addGroup" />
                      <p v-if="onAddGroupError" class="mt-1 text-sm text-red-900 text-right">
                        {{ t('common.unexpectedError', [onAddGroupError.message]) }}
                      </p>
                    </li>

                    <!-- Selected groups list -->
                    <template v-for="group in sortedNewGroups" :key="group.id">
                      <li class="py-3 flex flex-col">
                        <div class="flex items-center justify-between">
                          <div class="flex items-center w-full" :title="group.name">
                            <img :src="group.userPicture" class="w-8 h-8 rounded-full object-cover border border-gray-300" />
                            <p class="ml-4 text-sm font-medium truncate">
                              {{ group.name }}
                            </p>
                          </div>
                          <button type="button" class="inline-flex items-center gap-2 px-2.5 py-1.5 rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-red-500" :title="t('common.remove')" @click="removeTempGroup(group.id)">
                            <TrashIcon class="h-4 w-4 text-white" aria-hidden="true" />
                          </button>
                        </div>
                      </li>
                    </template>
                  </ul>
                </div>
              </div>

              <!-- Footer -->
              <div
                class="flex-shrink-0 bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:space-x-4 sm:space-x-reverse sm:px-6">
                <button type="submit" class="w-full sm:w-auto inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">
                  {{ t('common.save') }}
                </button>
                <button type="button" class="mt-3 sm:mt-0 w-full sm:w-auto inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="open = false">
                  {{ t('common.cancel') }}
                </button>
                <span class="mt-3 sm:mt-0 flex items-center text-sm text-gray-600 sm:mr-auto">
                  {{ selectedCount }}
                  {{ t('user.groups.addDialog.selected', 'Group(s) selected') }}
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
/* -------------------------------------------------------------------------- */
/* Imports                                                                      */
/* -------------------------------------------------------------------------- */
import {
  Dialog,
  DialogOverlay,
  DialogPanel,
  DialogTitle,
  TransitionChild,
  TransitionRoot
} from '@headlessui/vue';
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import SearchInputGroup from './SearchInputGroup.vue';
import { TrashIcon } from '@heroicons/vue/24/solid';

/* -------------------------------------------------------------------------- */
/* Types & Props                                                               */
/* -------------------------------------------------------------------------- */
interface Group {
  id: string;
  name: string;
  userPicture: string;
  description?: string;
}

const props = defineProps<{ groups: Group[] }>();
const emit = defineEmits<{ saved: (added: Group[]) => void }>();

/* -------------------------------------------------------------------------- */
/* State                                                                       */
/* -------------------------------------------------------------------------- */
const { t } = useI18n({ useScope: 'global' });
const open = ref(false);
const newGroups = ref<Group[]>([]);
const onAddGroupError = ref<Error | null>(null);

const selectedCount = computed(() => newGroups.value.length);
const sortedNewGroups = computed(() => [...newGroups.value].reverse());

/* -------------------------------------------------------------------------- */
/* Helpers                                                                     */
/* -------------------------------------------------------------------------- */

function isKnown(id: string) {
  return props.groups.some(g => g.id === id);
}

/* -------------------------------------------------------------------------- */
/* Search                                                                      */
/* -------------------------------------------------------------------------- */
async function searchGroup(query: string): Promise<Group[]> {
  if (!query.trim()) return [];

  // --- Mock async search --------------------------------------------------
  const results: Group[] = await new Promise(resolve =>
    setTimeout(
      () =>
        resolve([
          { id: 'g101', name: 'Marketing', description: 'Marketing Team', userPicture: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNjYWNhY2EiIGQ9Ik0yNSAwTDUwIDBMNTAgMjVaTTc1IDBMNzUgMjVMNTAgMjVaTTc1IDEwMEw1MCAxMDBMNTAgNzVaTTI1IDEwMEwyNSA3NUw1MCA3NVpNMCAyNUwyNSAyNUwyNSA1MFpNMTAwIDI1TDEwMCA1MEw3NSA1MFpNMTAwIDc1TDc1IDc1TDc1IDUwWk0wIDc1TDAgNTBMMjUgNTBaIi8+PHBhdGggZmlsbD0iI2YxZjlmYiIgZD0iTTI1IDBMMjUgMjVMMCAyNVpNMTAwIDI1TDc1IDI1TDc1IDBaTTc1IDEwMEw3NSA3NUwxMDAgNzVaTTAgNzVMMjUgNzVMMjUgMTAwWiIvPjxwYXRoIGZpbGw9IiNjZWVjZjIiIGQ9Ik0yNSAyNUw1MCAyNUw1MCA1MEwyNSA1MFpNNDEuMyA0Ny41TDQ3LjUgMzVMMzUgMzVaTTc1IDI1TDc1IDUwTDUwIDUwTDUwIDI1Wk01Mi41IDQxLjNMNjUgNDcuNUw2NSAzNVpNNzUgNzVMNTAgNzVMNTAgNTBMNzUgNTBaTTU4LjggNTIuNUw1Mi41IDY1TDY1IDY1Wk0yNSA3NUwyNSA1MEw1MCA1MEw1MCA3NVpNNDcuNSA1OC44TDM1IDUyLjVMMzUgNjVaIi8+PC9zdmc+' },
          { id: 'g102', name: 'Research', description: 'R&D Team', userPicture:'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNjZWVjZjIiIGQ9Ik01MCAwTDUwIDI1TDI1IDI1Wk03NSAyNUw1MCAyNUw1MCAwWk01MCAxMDBMNTAgNzVMNzUgNzVaTTI1IDc1TDUwIDc1TDUwIDEwMFpNMjUgMjVMMjUgNTBMMCA1MFpNMTAwIDUwTDc1IDUwTDc1IDI1Wk03NSA3NUw3NSA1MEwxMDAgNTBaTTAgNTBMMjUgNTBMMjUgNzVaTTMzIDMzTDUwIDMzTDUwIDUwTDMzIDUwWk02NyAzM0w2NyA1MEw1MCA1MEw1MCAzM1pNNjcgNjdMNTAgNjdMNTAgNTBMNjcgNTBaTTMzIDY3TDMzIDUwTDUwIDUwTDUwIDY3WiIvPjxwYXRoIGZpbGw9IiNmMWY5ZmIiIGQ9Ik00LjIgMTIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNzkuMiAxMi41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME03OS4yIDg3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTQuMiA4Ny41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsMCIvPjwvc3ZnPg==' },
          { id: 'g103', name: 'DevOps', description: 'Infrastructure & CI', userPicture:'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNmMWY5ZmIiIGQ9Ik0yNSAwTDUwIDBMNTAgMTIuNVpNNzUgMEw3NSAyNUw2Mi41IDI1Wk03NSAxMDBMNTAgMTAwTDUwIDg3LjVaTTI1IDEwMEwyNSA3NUwzNy41IDc1Wk0wIDI1TDI1IDI1TDI1IDM3LjVaTTEwMCAyNUwxMDAgNTBMODcuNSA1MFpNMTAwIDc1TDc1IDc1TDc1IDYyLjVaTTAgNzVMMCA1MEwxMi41IDUwWiIvPjxwYXRoIGZpbGw9IiNjYWNhY2EiIGQ9Ik0yNSAwTDI1IDI1TDEyLjUgMjVaTTEwMCAyNUw3NSAyNUw3NSAxMi41Wk03NSAxMDBMNzUgNzVMODcuNSA3NVpNMCA3NUwyNSA3NUwyNSA4Ny41WiIvPjxwYXRoIGZpbGw9IiNjZWVjZjIiIGQ9Ik0zMSAzMUw0OCAzMUw0OCA0OEwzMSA0OFpNNjkgMzFMNjkgNDhMNTIgNDhMNTIgMzFaTTY5IDY5TDUyIDY5TDUyIDUyTDY5IDUyWk0zMSA2OUwzMSA1Mkw0OCA1Mkw0OCA2OVoiLz48L3N2Zz4=' },
          { id: 'g104', name: 'Security', description: 'Security Champions', userPicture:'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNmMWY5ZmIiIGQ9Ik0yOS4yIDEyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTU0LjIgMTIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNTQuMiA4Ny41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME0yOS4yIDg3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTQuMiAzNy41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME03OS4yIDM3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTc5LjIgNjIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNC4yIDYyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwIi8+PHBhdGggZmlsbD0iI2NlZWNmMiIgZD0iTTI1IDI1TDAgMjVMMCAxMi41Wk03NSAyNUw3NSAwTDg3LjUgMFpNNzUgNzVMMTAwIDc1TDEwMCA4Ny41Wk0yNSA3NUwyNSAxMDBMMTIuNSAxMDBaIi8+PHBhdGggZmlsbD0iI2NhY2FjYSIgZD0iTTI1IDI1TDUwIDI1TDUwIDI5TDM5LjUgNTBMMjUgNTBaTTc1IDI1TDc1IDUwTDcxIDUwTDUwIDM5LjVMNTAgMjVaTTc1IDc1TDUwIDc1TDUwIDcxTDYwLjUgNTBMNzUgNTBaTTI1IDc1TDI1IDUwTDI5IDUwTDUwIDYwLjVMNTAgNzVaIi8+PC9zdmc+' },
          { id: 'g105', name: 'Localization', description: 'i18n Team', userPicture:'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNmNmY2ZjYiIGQ9Ik0yOS4yIDEyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTU0LjIgMTIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNTQuMiA4Ny41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME0yOS4yIDg3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTQuMiAzNy41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME03OS4yIDM3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTc5LjIgNjIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNC4yIDYyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwIi8+PHBhdGggZmlsbD0iI2NhY2FjYSIgZD0iTTAgMEwyNSAwTDI1IDI1Wk0xMDAgMEwxMDAgMjVMNzUgMjVaTTEwMCAxMDBMNzUgMTAwTDc1IDc1Wk0wIDEwMEwwIDc1TDI1IDc1WiIvPjxwYXRoIGZpbGw9IiNjZWVjZjIiIGQ9Ik0yNSAyNUw1MCAyNUw1MCA1MEwyNSA1MFpNMzQgNDAuNWE2LjUsNi41IDAgMSwwIDEzLDBhNi41LDYuNSAwIDEsMCAtMTMsME03NSAyNUw3NSA1MEw1MCA1MEw1MCAyNVpNNTMgNDAuNWE2LjUsNi41IDAgMSwwIDEzLDBhNi41LDYuNSAwIDEsMCAtMTMsME03NSA3NUw1MCA3NUw1MCA1MEw3NSA1MFpNNTMgNTkuNWE2LjUsNi41IDAgMSwwIDEzLDBhNi41LDYuNSAwIDEsMCAtMTMsME0yNSA3NUwyNSA1MEw1MCA1MEw1MCA3NVpNMzQgNTkuNWE2LjUsNi41IDAgMSwwIDEzLDBhNi41LDYuNSAwIDEsMCAtMTMsMCIvPjwvc3ZnPg==' },
          { id: 'g106', name: 'Design', description: 'UX/UI Designers', userPicture:'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNmMWY5ZmIiIGQ9Ik0yNSAxMi41TDM3LjUgMEw1MCAxMi41TDM3LjUgMjVaTTYyLjUgMEw3NSAxMi41TDYyLjUgMjVMNTAgMTIuNVpNNzUgODcuNUw2Mi41IDEwMEw1MCA4Ny41TDYyLjUgNzVaTTM3LjUgMTAwTDI1IDg3LjVMMzcuNSA3NUw1MCA4Ny41Wk0wIDM3LjVMMTIuNSAyNUwyNSAzNy41TDEyLjUgNTBaTTg3LjUgMjVMMTAwIDM3LjVMODcuNSA1MEw3NSAzNy41Wk0xMDAgNjIuNUw4Ny41IDc1TDc1IDYyLjVMODcuNSA1MFpNMTIuNSA3NUwwIDYyLjVMMTIuNSA1MEwyNSA2Mi41WiIvPjxwYXRoIGZpbGw9IiNjZWVjZjIiIGQ9Ik0xMi41IDBMMjUgMTIuNUwxMi41IDI1TDAgMTIuNVpNMTAwIDEyLjVMODcuNSAyNUw3NSAxMi41TDg3LjUgMFpNODcuNSAxMDBMNzUgODcuNUw4Ny41IDc1TDEwMCA4Ny41Wk0wIDg3LjVMMTIuNSA3NUwyNSA4Ny41TDEyLjUgMTAwWiIvPjxwYXRoIGZpbGw9IiNjYWNhY2EiIGQ9Ik01MCAzNy41TDUwIDUwTDM3LjUgNTBaTTYyLjUgNTBMNTAgNTBMNTAgMzcuNVpNNTAgNjIuNUw1MCA1MEw2Mi41IDUwWk0zNy41IDUwTDUwIDUwTDUwIDYyLjVaIi8+PC9zdmc+' },
          { id: 'g107', name: 'QA', description: 'Quality Assurance', userPicture:'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNhYmRmZTkiIGQ9Ik01MCAwTDUwIDI1TDI1IDI1Wk03NSAyNUw1MCAyNUw1MCAwWk01MCAxMDBMNTAgNzVMNzUgNzVaTTI1IDc1TDUwIDc1TDUwIDEwMFpNMjUgMjVMMjUgNTBMMCA1MFpNMTAwIDUwTDc1IDUwTDc1IDI1Wk03NSA3NUw3NSA1MEwxMDAgNTBaTTAgNTBMMjUgNTBMMjUgNzVaIi8+PHBhdGggZmlsbD0iI2NlZWNmMiIgZD0iTTEyLjUgMjVMMCAxMi41TDEyLjUgMEwyNSAxMi41Wk03NSAxMi41TDg3LjUgMEwxMDAgMTIuNUw4Ny41IDI1Wk04Ny41IDc1TDEwMCA4Ny41TDg3LjUgMTAwTDc1IDg3LjVaTTI1IDg3LjVMMTIuNSAxMDBMMCA4Ny41TDEyLjUgNzVaIi8+PHBhdGggZmlsbD0iI2Y2ZjZmNiIgZD0iTTI1IDI1TDUwIDI1TDUwIDUwTDI1IDUwWk00MS4zIDQ3LjVMNDcuNSAzNUwzNSAzNVpNNzUgMjVMNzUgNTBMNTAgNTBMNTAgMjVaTTUyLjUgNDEuM0w2NSA0Ny41TDY1IDM1Wk03NSA3NUw1MCA3NUw1MCA1MEw3NSA1MFpNNTguOCA1Mi41TDUyLjUgNjVMNjUgNjVaTTI1IDc1TDI1IDUwTDUwIDUwTDUwIDc1Wk00Ny41IDU4LjhMMzUgNTIuNUwzNSA2NVoiLz48L3N2Zz4=' },
          { id: 'g108', name: 'Legal', description: 'Legal Department', userPicture:'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNjYWNhY2EiIGQ9Ik0yNSAwTDUwIDBMNTAgMTIuNVpNNzUgMEw3NSAyNUw2Mi41IDI1Wk03NSAxMDBMNTAgMTAwTDUwIDg3LjVaTTI1IDEwMEwyNSA3NUwzNy41IDc1Wk0wIDI1TDI1IDI1TDI1IDM3LjVaTTEwMCAyNUwxMDAgNTBMODcuNSA1MFpNMTAwIDc1TDc1IDc1TDc1IDYyLjVaTTAgNzVMMCA1MEwxMi41IDUwWiIvPjxwYXRoIGZpbGw9IiNmNmY2ZjYiIGQ9Ik0xMi41IDBMMjUgMTIuNUwxMi41IDI1TDAgMTIuNVpNMTAwIDEyLjVMODcuNSAyNUw3NSAxMi41TDg3LjUgMFpNODcuNSAxMDBMNzUgODcuNUw4Ny41IDc1TDEwMCA4Ny41Wk0wIDg3LjVMMTIuNSA3NUwyNSA4Ny41TDEyLjUgMTAwWiIvPjxwYXRoIGZpbGw9IiNjZWVjZjIiIGQ9Ik0yNSAyNUw1MCAyNUw1MCAyOUwzOS41IDUwTDI1IDUwWk03NSAyNUw3NSA1MEw3MSA1MEw1MCAzOS41TDUwIDI1Wk03NSA3NUw1MCA3NUw1MCA3MUw2MC41IDUwTDc1IDUwWk0yNSA3NUwyNSA1MEwyOSA1MEw1MCA2MC41TDUwIDc1WiIvPjwvc3ZnPg==' }
        ]),
      300
    )
  );

  const q = query.trim().toLowerCase();
  return results
    .filter(r =>
      (r.name.toLowerCase().includes(q) || r.description?.toLowerCase().includes(q)) &&
      !isKnown(r.id) &&
      !newGroups.value.some(n => n.id === r.id)
    )
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))
    .map(u => ({ ...u, pictureUrl: u.userPicture }));
}

/* -------------------------------------------------------------------------- */
/* Add / Remove                                                               */
/* -------------------------------------------------------------------------- */
function addGroup(group: Group) {
  try {
    if (isKnown(group.id) || newGroups.value.some(g => g.id === group.id)) return;
    newGroups.value.push(group);
  } catch (e) {
    onAddGroupError.value = e as Error;
  }
}

function removeTempGroup(id: string) {
  newGroups.value = newGroups.value.filter(g => g.id !== id);
}

/* -------------------------------------------------------------------------- */
/* Submit                                                                     */
/* -------------------------------------------------------------------------- */
function onSubmit() {
  emit('saved', [...newGroups.value]);
  open.value = false;
}

/* -------------------------------------------------------------------------- */
/* Expose                                                                     */
/* -------------------------------------------------------------------------- */
function show() {
  newGroups.value = [];
  open.value = true;
}

defineExpose({ show });
</script>
