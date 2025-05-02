<template>
  <!-- Loading placeholder -->
  <div v-if="loading" class="text-center py-10">{{ t('common.loading') }}</div>

  <!-- Edit page -->
  <div v-else>
    <!-- Content -->
    <div class="space-y-6 mt-5">
      <section class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
        <h3 class="text-lg font-medium leading-6 text-gray-900">
          {{ t('group.edit.title') }}
        </h3>
        <hr class="my-4 pb-6 border-gray-200" />

        <!-- Form -->
        <form class="space-y-6 md:gap-6" novalidate @submit.prevent="onSubmit">
          <!-- Avatar row -->
          <div class="md:grid md:grid-cols-3 md:gap-6 items-center">
            <label class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('group.edit.profileImage') }}
            </label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <div class="relative w-32 h-32 text-center">
                <img v-if="previewUrl" :src="previewUrl" class="w-full h-full rounded-full object-cover" alt="Preview" />
                <div v-else class="w-full h-full rounded-full bg-gray-100 flex items-center justify-center text-gray-400">
                  <UserIcon class="w-12 h-12" />
                </div>
                <button type="button" class="absolute bottom-0 right-0 bg-white rounded-full p-1 shadow-md hover:bg-gray-100" :aria-label="selectedPicture ? t('group.edit.removePicture') : t('group.edit.addPicture')" @click="selectedPicture ? removePicture() : triggerFileSelect()">
                  <component :is="selectedPicture ? TrashIcon : PlusIcon" class="w-5 h-5 text-gray-600" />
                </button>
                <input ref="fileInputRef" type="file" accept="image/*" class="hidden" @change="onPictureChange" />
              </div>
            </div>
          </div>

          <!-- Name row -->
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="name" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('group.detail.name') }}
            </label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="name" v-model="name" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md" required />
            </div>
          </div>

          <!-- Roles row -->
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('group.detail.roles') }}
            </label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <Listbox v-model="selectedRoles" multiple>
                <div class="relative">
                  <!-- Button with selected chips inside -->
                  <ListboxButton class="relative w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary text-sm">
                    <div class="flex flex-wrap gap-2">
                      <template v-if="selectedRoles.length">
                        <button v-for="role in selectedRoles" :key="role" type="button" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20" :aria-label="t('common.remove', { role })" @click.stop="removeRole(role)">
                          <span class="mr-1">{{ roleLabels[role] }}</span>
                          <span class="text-green-800 font-bold" aria-hidden="true">&times;</span>
                        </button>
                      </template>
                      <template v-else>
                        <span class="text-gray-500">{{ t('common.select.placeholder') }}</span>
                      </template>
                    </div>
                    <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                      <ChevronUpDownIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
                    </span>
                  </ListboxButton>

                  <!-- Options -->
                  <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
                    <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black/5 focus:outline-none text-sm">
                      <ListboxOption v-for="role in roles" v-slot="{ selected }" :key="role" :value="role" class="relative cursor-default select-none py-2 pl-3 pr-9 ui-not-active:text-gray-900 ui-active:text-white ui-active:bg-primary">
                        <span :class="[selected ? 'font-semibold' : 'font-normal', 'block truncate']">{{ roleLabels[role] }}</span>
                        <span v-if="selected" class="absolute inset-y-0 right-0 flex items-center pr-4 text-primary">
                          <CheckIcon class="h-5 w-5" aria-hidden="true" />
                        </span>
                      </ListboxOption>
                    </ListboxOptions>
                  </transition>
                </div>
              </Listbox>
            </div>
          </div>

          <!-- Actions -->
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-start-2 flex space-x-3">
              <button type="submit" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">
                {{ t('common.save') }}
              </button>
              <button type="button" class="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md bg-white text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="resetForm">
                {{ t('common.cancel') }}
              </button>
            </div>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PlusIcon, TrashIcon, UserIcon } from '@heroicons/vue/24/outline';
import { CheckIcon, ChevronUpDownIcon } from '@heroicons/vue/24/solid';
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { ref, onMounted, watch } from 'vue';
import { useI18n } from 'vue-i18n';

interface GroupData {
  name: string;
  roles: Role[];
  picture?: File | null;
  previewUrl?: string | null;
}

type Role = 'User' | 'create' | 'admin';

const { t } = useI18n({ useScope: 'global' });

const roleLabels: Record<Role, string> = {
  User: 'User',
  create: 'Create‑Vault',
  admin: 'Admin',
};

const DEFAULT_GROUP: GroupData = {
  name: 'Frontend‑Team',
  roles: ['User', 'create'],
  previewUrl: 'https://i.pravatar.cc/200?u=group',
  picture: new File(['NeuerInhalt'], 'neuedatei.jpg', { type: 'image/jpeg' }),
};

const loading = ref(true);
const name = ref<string>('');
const roles: Role[] = ['User', 'create', 'admin'];
const selectedRoles = ref<Role[]>([]);

const fileInputRef = ref<HTMLInputElement | null>(null);
const selectedPicture = ref<File | null>(null);
const previewUrl = ref<string | null>(null);

onMounted(() => {
  setTimeout(() => {
    applyGroup(DEFAULT_GROUP);
    loading.value = false;
  }, 300);
});

function applyGroup(data: GroupData) {
  name.value = data.name;
  selectedRoles.value = [...data.roles];
  previewUrl.value = data.previewUrl ?? null;
  selectedPicture.value = data.picture ?? null;
}

function triggerFileSelect() {
  fileInputRef.value?.click();
}

function removePicture() {
  selectedPicture.value = null;
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value);
  previewUrl.value = null;
  if (fileInputRef.value) fileInputRef.value.value = '';
}

function onPictureChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (file) {
    selectedPicture.value = file;
    previewUrl.value = URL.createObjectURL(file);
  } else {
    removePicture();
  }
}

function removeRole(role: Role) {
  selectedRoles.value = selectedRoles.value.filter(r => r !== role);
}

watch(selectedRoles, () => {
  selectedRoles.value.sort((a, b) => roleLabels[a].localeCompare(roleLabels[b]));
});

function onSubmit() {
  const payload: GroupData = {
    name: name.value,
    roles: [...selectedRoles.value],
    picture: selectedPicture.value ?? null,
  };
  console.log('Dummy save payload', payload);
}

function resetForm() {
  applyGroup(DEFAULT_GROUP);
}
</script>
