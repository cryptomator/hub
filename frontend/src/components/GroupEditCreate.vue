<template>
  <!-- Loading placeholder -->
  <div v-if="loading" class="text-center py-10">
    {{ t('common.loading') }}
  </div>

  <!-- Edit/Create page -->
  <div v-else>
    <div class="-my-2 -mx-4 sm:-mx-6 lg:-mx-8 overflow-hidden">
      <div class="py-2 align-middle inline-block min-w-full px-4 sm:px-6 lg:px-8">
        <div class="shadow overflow-hidden border-b border-gray-200 rounded-lg bg-white p-6 space-y-8">
          <div>
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ isEditMode ? t('groupEditCreate.title.edit') : t('groupEditCreate.title.create') }}
            </h3>
            <hr class="my-4 border-gray-200"/>
          </div>

          <!-- Profile Picture Preview -->
          <div class="flex flex-col items-center gap-4 mb-8">
            <div class="relative w-32 h-32">
              <img v-if="isValidImageUrl" :src="pictureUrl" class="w-full h-full rounded-full object-cover border border-gray-300" :alt="t('groupEditCreate.profilePicture')" />
              <div v-else class="w-full h-full rounded-full bg-gray-100 flex items-center justify-center text-gray-400">
                <UserIcon class="w-12 h-12" />
              </div>
            </div>
          </div>

          <!-- Form -->
          <form class="space-y-6 md:space-y-8" novalidate @submit.prevent="onSubmit">
            <!-- Profile Picture URL row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="pictureUrl" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('groupEditCreate.profilePictureUrl') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <div class="relative">
                  <input id="pictureUrl" v-model="pictureUrl" type="url" :class="[errors.pictureUrl ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md pr-10']"/>
                  <button v-if="pictureUrl" type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="t('groupEditCreate.removePicture')" @click="removePicture">
                    <TrashIcon class="w-5 h-5 text-gray-600" />
                  </button>
                </div>
                <p v-if="errors.pictureUrl" class="mt-1 text-sm text-red-600">{{ errors.pictureUrl }}</p>
              </div>
            </div>

            <!-- Name row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="name" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('groupEditCreate.name') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <input id="name" v-model="name" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" required />
                <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
              </div>
            </div>

            <!-- Roles row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('groupEditCreate.roles') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1 max-w-md">
                <Listbox v-model="selectedRoles" multiple>
                  <div class="relative">
                    <ListboxButton class="relative w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary text-sm">
                      <div class="flex flex-wrap gap-2">
                        <template v-if="selectedRoles.length">
                          <button v-for="role in selectedRoles" :key="role" type="button" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20" :aria-label="t('common.remove', { role })" @click.stop="removeRole(role)">
                            <span class="mr-1">{{ roleOptions[role] }}</span>
                            <span class="text-green-800 font-bold" aria-hidden="true">&times;</span>
                          </button>
                        </template>
                        <template v-else>
                          <span class="text-gray-500">{{ t('groupEditCreate.selectRolesPlaceholder') }}</span>
                        </template>
                      </div>
                      <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                        <ChevronUpDownIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
                      </span>
                    </ListboxButton>

                    <!-- Roles row -->
                    <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
                      <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black/5 focus:outline-none text-sm">
                        <ListboxOption v-for="(label, key) in roleOptions" v-slot="{ selected }" :key="key" :value="key" class="relative cursor-default select-none py-2 pl-3 pr-9 ui-not-active:text-gray-900 ui-active:text-white ui-active:bg-primary">
                          <span :class="[selected ? 'font-semibold' : 'font-normal', 'block truncate']">{{ label }}</span>
                          <span v-if="selected" class="absolute inset-y-0 right-0 flex items-center pr-4 text-primary">
                            <CheckIcon class="h-5 w-5" aria-hidden="true" />
                          </span>
                        </ListboxOption>
                      </ListboxOptions>
                    </transition>
                  </div>
                </Listbox>
                <p v-if="errors.roles" class="mt-1 text-sm text-red-600">{{ errors.roles }}</p>
              </div>
            </div>

            <!-- Actions -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <div></div>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <div class="flex space-x-3">
                  <button type="submit" :disabled="processing" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                    <span v-if="!groupSaved">
                      {{ isEditMode ? t('common.save') : t('common.create') }}
                    </span>
                    <span v-else>{{ t('common.saved') }}</span>
                  </button>
                  <button type="button" class="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md bg-white text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="cancelAction">
                    {{ t('common.cancel') }}
                  </button>
                </div>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { CheckIcon, ChevronUpDownIcon, TrashIcon, UserIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { FormValidator } from '../common/formvalidator';
import { debounce } from '../common/util';

interface GroupData {
  id?: string;
  name: string;
  roles: Role[];
  pictureUrl?: string;
}

const { t } = useI18n({ useScope: 'global' });
const route = useRoute();
const router = useRouter();

// Determine if we're in edit or create mode based on route params
const groupId = computed(() => route.params.id as string | undefined);
const isEditMode = computed(() => !!groupId.value);

const loading = ref(true);
const name = ref<string>('');

type Role = 'Admin' | 'Create-Vault';
const selectedRoles = ref<Role[]>([]);
const roleOptions: Record<Role, string> = {
  'Admin': 'Admin',
  'Create-Vault': 'Create-Vault',
};

const errors = ref<Record<string, string>>({});
const processing = ref(false);
const groupSaved = ref(false);
const debouncedGroupSaved = debounce(() => groupSaved.value = false, 2000);

const pictureUrl = ref<string>('');
const isValidImageUrl = ref<boolean>(false);

watch(pictureUrl, 
  async (newUrl) => {
    isValidImageUrl.value = await FormValidator.validateImageUrl(newUrl);
  },
  { immediate: true }
);

onMounted(() => {
  // TODO: Replace with actual API call to fetch group data
  // This is temporary mock data for development purposes
  setTimeout(() => {
    if (isEditMode.value) {
      name.value = 'Frontend-Team';
      selectedRoles.value = ['Admin', 'Create-Vault'];
      pictureUrl.value = 'https://i.pravatar.cc/200?u=group';
    } else {
      name.value = '';
      selectedRoles.value = [];
      pictureUrl.value = '';
    }
    loading.value = false;
  }, 300);
});

function removePicture() {
  pictureUrl.value = '';
}

function validateForm() {
  const result = FormValidator.validateGroup({
    name: name.value,
    pictureUrl: pictureUrl.value,
    isValidImageUrl: isValidImageUrl.value
  });
  
  errors.value = result.errors;
  return result.valid;
}

function removeRole(role: Role) {
  selectedRoles.value = selectedRoles.value.filter(r => r !== role);
}

watch(selectedRoles, () => {
  if (selectedRoles.value.length > 0) {
    selectedRoles.value.sort((a, b) => roleOptions[a].localeCompare(roleOptions[b]));
  }
});

function onSubmit() {
  if (!validateForm()) {
    return;
  }
  
  processing.value = true;
  
  name.value = name.value.trim();
  
  try {
    const payload = {
      ...(isEditMode.value && { id: groupId.value }),
      name: name.value,
      roles: [...selectedRoles.value],
      pictureUrl: pictureUrl.value
    };
    
    console.log(`${isEditMode.value ? 'Updating' : 'Creating'} group:`, payload);
    
    // Show saved success state
    groupSaved.value = true;
    debouncedGroupSaved();
    
    // Redirect after successful save
    setTimeout(() => {
      router.push('/app/authority');
    }, 1000);
  } catch (error) {
    console.error('Failed to save group:', error);
  } finally {
    processing.value = false;
  }
}

function cancelAction() {
  if (isEditMode.value) {
    router.push(`/app/authority/group/${groupId.value}`);
  } else {
    router.push('/app/authority');
  }
}
</script>