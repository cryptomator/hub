<template>
  <!-- Loading placeholder -->
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>

  <!-- Edit/Create page -->
  <div v-else>
    <div class="-my-2 -mx-4 sm:-mx-6 lg:-mx-8 overflow-hidden">
      <div class="py-2 align-middle inline-block min-w-full px-4 sm:px-6 lg:px-8">
        <div class="shadow overflow-hidden border-b border-gray-200 rounded-lg bg-white p-6 space-y-8 relative">
          <div>
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ isEditMode ? t('userEditCreate.title.edit') : t('userEditCreate.title.create') }}
            </h3>
            <hr class="my-4 border-gray-200"/>
          </div>
          
          <!-- Profile Picture Preview -->
          <div class="flex flex-col items-center gap-4 mb-8">
            <div class="relative w-32 h-32">
              <img v-if="isValidImageUrl" :src="pictureUrl" class="w-full h-full rounded-full object-cover border border-gray-300"/>
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
                {{ t('userEditCreate.profilePictureUrl') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <div class="relative">
                  <input id="pictureUrl" v-model="pictureUrl" type="url" :class="[errors.pictureUrl ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md pr-10']"/>
                  <button v-if="pictureUrl" type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="t('userEditCreate.removePicture')" @click="removePicture">
                    <TrashIcon class="w-5 h-5 text-gray-600" />
                  </button>
                </div>
                <p v-if="errors.pictureUrl" class="mt-1 text-sm text-red-600">{{ errors.pictureUrl }}</p>
              </div>
            </div>

            <!-- First Name row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="firstName" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.firstName') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <input id="firstName" v-model="firstName" type="text" required :class="[errors.firstName ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md']"/>
                <p v-if="errors.firstName" class="mt-1 text-sm text-red-600">{{ errors.firstName }}</p>
              </div>
            </div>

            <!-- Last Name row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="lastName" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.lastName') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <input id="lastName" v-model="lastName" type="text" required :class="[errors.lastName ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md']"/>
                <p v-if="errors.lastName" class="mt-1 text-sm text-red-600">{{ errors.lastName }}</p>
              </div>
            </div>

            <!-- Username row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="username" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.username') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <input id="username" v-model="username" type="text" required :class="[errors.username ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md']"/>
                <p v-if="errors.username" class="mt-1 text-sm text-red-600">{{ errors.username }}</p>
              </div>
            </div>

            <!-- Email row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="email" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.email') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <input id="email" v-model="email" type="email" required :class="[errors.email ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md']"/>
                <p v-if="errors.email" class="mt-1 text-sm text-red-600">{{ errors.email }}</p>
                <p v-else-if="email && !isValidEmail(email.trim())" class="mt-1 text-sm text-red-600">
                  {{ t('userEditCreate.invalidEmail') }}
                </p>
              </div>
            </div>

            <!-- Roles row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="roles" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.roles') }}
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
                          <span class="text-gray-500">{{ t('userEditCreate.selectRolesPlaceholder') }}</span>
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

            <!-- Password Info -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <div></div>
              <div class="md:col-span-2 lg:col-span-1">
                <div class="bg-blue-50 text-gray-900 text-sm rounded-md p-4 flex gap-3 items-start">
                  <InformationCircleIcon class="w-5 h-5 mt-0.5 text-blue-400 flex-shrink-0" aria-hidden="true" />
                  <p>
                    {{ isEditMode ? t('userEditCreate.edit.passwordInfo') : t('userEditCreate.create.passwordInfo') }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Password row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="password" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.password') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <div class="relative">
                  <input id="password" v-model="password" :type="passwordInputType" :class="[errors.password ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md pr-10']"/>
                  <button type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="passwordInputType === 'password' ? t('common.showPassword') : t('common.hidePassword')" @click="togglePasswordVisibility">
                    <component :is="passwordInputType === 'password' ? EyeIcon : EyeSlashIcon" class="h-5 w-5" />
                  </button>
                </div>
                <p v-if="errors.password" class="mt-1 text-sm text-red-600">{{ errors.password }}</p>
                <div v-if="password" class="mt-2 max-w-md">
                  <div class="flex items-center">
                    <div class="w-full bg-gray-200 rounded-full h-2">
                      <div 
                        class="h-2 rounded-full transition-all duration-300" 
                        :class="{
                          'w-1/3 bg-red-500': passwordStrength === 'weak',
                          'w-2/3 bg-yellow-500': passwordStrength === 'medium',
                          'w-full bg-green-500': passwordStrength === 'strong'
                        }"
                      ></div>
                    </div>
                    <span class="ml-2 text-xs" :class="passwordStrengthColor">{{ passwordStrengthLabel }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Password Confirm row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="passwordConfirm" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.passwordConfirm') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <div class="relative">
                  <input id="passwordConfirm" v-model="passwordConfirm" :type="passwordInputType" :class="[errors.passwordConfirm ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md pr-10']"/>
                  <button type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="passwordInputType === 'password' ? t('userEditCreate.showPassword') : t('userEditCreate.hidePassword')" @click="togglePasswordVisibility">
                    <component :is="passwordInputType === 'password' ? EyeIcon : EyeSlashIcon" class="h-5 w-5" />
                  </button>
                </div>
                <p v-if="errors.passwordConfirm" class="mt-1 text-sm text-red-600">{{ errors.passwordConfirm }}</p>
                <p v-else-if="passwordConfirm" :class="['mt-1 text-sm', password === passwordConfirm ? 'text-green-600' : 'text-red-600']">
                  {{ password === passwordConfirm ? t('userEditCreate.passwordsMatch') : t('userEditCreate.passwordsDontMatch') }}
                </p>
              </div>
            </div>

            <!-- Actions -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <div></div>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <div class="flex space-x-3">
                  <button type="submit" :disabled="processing" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                    <span v-if="!userSaved">{{ isEditMode ? t('common.save') : t('common.create') }}</span>
                    <span v-else>{{ t('common.saved') }}</span>
                  </button>
                  <button type="button" class="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md bg-white text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="cancelAction">
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
import { CheckIcon, ChevronUpDownIcon, EyeIcon, EyeSlashIcon, InformationCircleIcon, TrashIcon, UserIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { FormValidator } from '../common/formvalidator';
import { debounce } from '../common/util';

interface UserData {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  roles: Role[];
  picture?: File | null;
  previewUrl?: string | null;
}

const { t } = useI18n({ useScope: 'global' });
const route = useRoute();
const router = useRouter();

// Determine if we're in edit or create mode
const userId = route.params.id as string;
const isEditMode = computed(() => !!userId);

const loading = ref(true);
const firstName = ref('');
const lastName = ref('');
const username = ref('');
const email = ref('');

type Role = 'Admin' | 'Create-Vault';
const selectedRoles = ref<Role[]>([]);
const roleOptions: Record<Role, string> = {
  'Admin': 'Admin',
  'Create-Vault': 'Create-Vault',
};

const errors = ref<Record<string, string>>({});
const processing = ref(false);
const userSaved = ref(false);
const debouncedUserSaved = debounce(() => userSaved.value = false, 2000);

const password = ref('');
const passwordConfirm = ref('');
const passwordInputType = ref<'password' | 'text'>('password');
const passwordStrength = ref<'weak' | 'medium' | 'strong' | ''>('');
const pictureUrl = ref<string>('');
const isValidImageUrl = ref<boolean>(false);

watch(pictureUrl, 
  async (newUrl) => {
    isValidImageUrl.value = await FormValidator.validateImageUrl(newUrl);
  },
  { immediate: true }
);

onMounted(() => {
  if (isEditMode.value) {
    // TODO: Replace with actual API call to fetch user data
    // This is temporary mock data for development purposes
    setTimeout(() => {
      firstName.value = 'Max';
      lastName.value = 'Mustermann';
      username.value = 'mustermannmax';
      email.value = 'max.mustermann@mustermail.de';
      pictureUrl.value = 'https://i.pravatar.cc/150?u=placeholder';
      selectedRoles.value = ['Admin']; 
      loading.value = false;
    }, 300);
  } else {
    firstName.value = '';
    lastName.value = '';
    username.value = '';
    email.value = '';
    password.value = '';
    passwordConfirm.value = '';
    pictureUrl.value = '';
    selectedRoles.value = [];
    loading.value = false;
  }
});

function removeRole(role: Role) {
  selectedRoles.value = selectedRoles.value.filter(r => r !== role);
}

function togglePasswordVisibility() {
  passwordInputType.value = passwordInputType.value === 'password' ? 'text' : 'password';
}

function validateForm() {
  const result = FormValidator.validateUser({
    firstName: firstName.value,
    lastName: lastName.value,
    username: username.value,
    email: email.value,
    password: password.value,
    passwordConfirm: passwordConfirm.value,
    isEditMode: isEditMode.value,
    pictureUrl: pictureUrl.value,      
    isValidImageUrl: isValidImageUrl.value 
  });
  
  errors.value = result.errors;
  return result.valid;
}

const passwordStrengthLabel = computed(() => {
  switch (passwordStrength.value) {
    case 'strong': return t('userEditCreate.passwordStrong');
    case 'medium': return t('userEditCreate.passwordMedium');
    case 'weak': return t('userEditCreate.passwordWeak');
    default: return '';
  }
});

const passwordStrengthColor = computed(() => {
  switch (passwordStrength.value) {
    case 'strong': return 'text-green-600';
    case 'medium': return 'text-yellow-600';
    case 'weak': return 'text-red-600';
    default: return 'text-gray-600';
  }
});

const isValidEmail = FormValidator.isValidEmail;

function evaluatePasswordStrength(pw: string): 'weak' | 'medium' | 'strong' | '' {
  return FormValidator.evaluatePasswordStrength(pw);
}

watch(password, (newVal) => {
  passwordStrength.value = evaluatePasswordStrength(newVal);
});

function removePicture() {
  pictureUrl.value = '';
  isValidImageUrl.value = false;
}

function onSubmit() {
  if (!validateForm()) {
    return;
  }
  
  processing.value = true;     
  
  firstName.value = firstName.value.trim();
  lastName.value = lastName.value.trim();
  username.value = username.value.trim();
  email.value = email.value.trim();
  pictureUrl.value = pictureUrl.value.trim();
  
  try {
    // Here would be the actual API call to save the user
    // Different API call depending on edit vs create mode
    if (isEditMode.value) {
      console.log('Updating user:', userId);
    } else {
      console.log('Creating new user');
    }
    
    userSaved.value = true;
    debouncedUserSaved();
    
    // Redirect after successful operation with a slight delay
    setTimeout(() => {
      if (isEditMode.value) {
        // For edit mode, redirect to user details
        router.push(`/app/authority/user/${userId}`);
      } else {
        // For create mode, redirect to user list
        router.push('/app/authority');
      }
    }, 1000);
  } catch (error) {
    console.error('Failed to save user:', error);
  } finally {
    processing.value = false;
  }
}

function cancelAction() {
  if (isEditMode.value) {
    router.push(`/app/authority/user/${userId}`);
  } else {
    router.push('/app/authority');
  }
}
</script>
