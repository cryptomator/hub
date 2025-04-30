<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>

  <div v-else class="p-6 space-y-6">
    <div class="bg-white shadow rounded-lg sm:rounded-lg p-6 space-y-8">
      <div class="flex flex-col items-center gap-4">
        <div class="relative w-32 h-32">
          <img v-if="previewUrl" :src="previewUrl" class="w-full h-full rounded-full object-cover border border-gray-300" alt="Profile Picture"/>
          <div v-else class="w-full h-full rounded-full bg-gray-100 flex items-center justify-center text-gray-400">
            <UserIcon class="w-12 h-12" />
          </div>
          <button type="button" class="absolute bottom-0 right-0 bg-white rounded-full p-1 shadow-md hover:bg-gray-100" :aria-label="previewUrl ? t('editUser.removePicture') : t('editUser.addPicture')" @click="previewUrl ? removePicture() : triggerFileSelect()">
            <component :is="previewUrl ? TrashIcon : PlusIcon" class="w-5 h-5 text-gray-600" />
          </button>
          <input ref="fileInputRef" type="file" accept="image/*" class="hidden" @change="onPictureChange" />
        </div>
      </div>
      <form id="user-edit-form" novalidate class="space-y-6 md:space-y-8" @submit.prevent="saveUser">
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <label for="firstName" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
            {{ t('user.edit.firstName') }}
          </label>
          <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
            <input id="firstName" v-model="firstName" type="text" required class="focus:ring-primary focus:border-primary block w-full max-w-md shadow-sm sm:text-sm border-gray-300 rounded-md" />
          </div>
        </div>
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <label for="lastName" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
            {{ t('user.edit.lastName') }}
          </label>
          <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
            <input id="lastName" v-model="lastName" type="text" required class="focus:ring-primary focus:border-primary block w-full max-w-md shadow-sm sm:text-sm border-gray-300 rounded-md"/>
          </div>
        </div>
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <label for="username" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
            {{ t('user.edit.username') }}
          </label>
          <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
            <input id="username" v-model="username" type="text" required class="focus:ring-primary focus:border-primary block w-full max-w-md shadow-sm sm:text-sm border-gray-300 rounded-md"/>
          </div>
        </div>
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <label for="email" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
            {{ t('user.edit.email') }}
          </label>
          <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
            <input id="email" v-model="email" type="email" required class="focus:ring-primary focus:border-primary block w-full max-w-md shadow-sm sm:text-sm border-gray-300 rounded-md"/>
          </div>
        </div>
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <label for="roles" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
            {{ t('user.edit.roles') }}
          </label>
          <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1 max-w-md">
            <Listbox v-model="selectedRoles" multiple as="div">
              <div class="relative">
                <ListboxButton class="relative w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-sm focus:ring-primary text-sm">
                  <div class="flex flex-wrap gap-2">
                    <template v-if="selectedRoles.length > 0">
                      <button v-for="role in selectedRoles" :key="role" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20" @click.stop="removeRole(role)">
                        <span class="mr-1">{{ roleOptions[role] }}</span>
                        <span class="text-green-800 font-bold">&times;</span>
                      </button>
                    </template>
                    <template v-else>
                      <span class="text-gray-500">{{ t('userEdit.selectRolesPlaceholder') }}</span>
                    </template>
                  </div>
                  <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                    <ChevronUpDownIcon class="h-5 w-5 text-gray-400" />
                  </span>
                </ListboxButton>

                <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
                  <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black/5 text-sm">
                    <ListboxOption v-for="(label, key) in roleOptions" :key="key" v-slot="{ selected }" :value="key" class="relative cursor-default select-none py-2 pl-3 pr-9 ui-not-active:text-gray-900 ui-active:text-white ui-active:bg-primary">
                      <span :class="[selected ? 'font-semibold' : 'font-normal', 'block truncate']">{{ label }}</span>
                      <span v-if="selected" class="absolute inset-y-0 right-0 flex items-center pr-4 text-primary">
                        <CheckIcon class="h-5 w-5" />
                      </span>
                    </ListboxOption>
                  </ListboxOptions>
                </transition>
              </div>
            </Listbox>
          </div>
        </div>
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <div></div>
          <div class="md:col-span-2 lg:col-span-1">
            <div class="bg-blue-50 text-gray-900 text-sm rounded-md p-4 flex gap-3 items-start">
              <InformationCircleIcon class="w-5 h-5 mt-0.5 text-blue-400 flex-shrink-0" aria-hidden="true" />
              <p>
                {{ t('user.edit.passwordInfo') }}
              </p>
            </div>
          </div>
        </div>
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <label for="password" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
            {{ t('user.edit.password') }}
          </label>
          <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
            <input id="password" v-model="password" type="password" required class="focus:ring-primary focus:border-primary block w-full max-w-md shadow-sm sm:text-sm border-gray-300 rounded-md"/>
          </div>
        </div>
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <label for="passwordRepeat" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
            {{ t('user.edit.passwordRepeat') }}
          </label>
          <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
            <input id="passwordRepeat" v-model="passwordRepeat" type="password" required class="focus:ring-primary focus:border-primary block w-full max-w-md shadow-sm sm:text-sm border-gray-300 rounded-md"/>
          </div>
        </div>
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <div></div>
          <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
            <button type="submit" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
              {{ t('common.save') }}
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { CheckIcon, ChevronUpDownIcon, InformationCircleIcon, PlusIcon, TrashIcon, UserIcon } from '@heroicons/vue/24/outline';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const { t } = useI18n({ useScope: 'global' });
const route = useRoute();
const router = useRouter();

const userId = route.params.id as string;

const loading = ref(true);
const firstName = ref('');
const lastName = ref('');
const username = ref('');
const email = ref('');
type UserRole = 'Admin' | 'Create-Vault';

const selectedRoles = ref<UserRole[]>([]);
const roleOptions: Record<UserRole, string> = {
  'Admin': 'Admin',
  'Create-Vault': 'Create-Vault',
};

function removeRole(role: UserRole) {
  selectedRoles.value = selectedRoles.value.filter(r => r !== role);
}

const password = ref('');
const passwordRepeat = ref('');
const previewUrl = ref<string | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);

function triggerFileSelect() {
  fileInputRef.value?.click();
}

function onPictureChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) {
    previewUrl.value = URL.createObjectURL(file);
  }
}

function removePicture() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewUrl.value = null;
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
}

function saveUser() {
  router.push('/app/authority');
}

onMounted(() => {
  setTimeout(() => {
    firstName.value = 'Max';
    lastName.value = 'Mustermann';
    username.value = 'maxmustermann';
    email.value = 'max@example.com';
    password.value = 'password123';
    passwordRepeat.value = 'password123';
    previewUrl.value = 'https://i.pravatar.cc/150?u=placeholder';
    selectedRoles.value = ['Admin', 'Create-Vault'];
    loading.value = false;
  }, 300);
});
</script>
