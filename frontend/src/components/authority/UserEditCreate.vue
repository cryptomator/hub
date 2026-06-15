<template>
  <!-- Loading placeholder -->
  <div v-if="onFetchError">
    <FetchError :error="onFetchError" />
  </div>
  <div v-else-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>

  <!-- Edit/Create page -->
  <div v-else>
    <BreadcrumbNav v-if="props.mode === 'EDIT'" :crumbs="[ { label: t('nav.users'), to: '/app/users' }, { label: data.name, to:'/app/users/' + props.id }, { label: t('common.edit') } ]" />
    <BreadcrumbNav v-else :crumbs="[ { label: t('nav.users'), to: '/app/users' }, { label: t('common.create') } ]" />
    <div class="-my-2 -mx-4 sm:-mx-6 lg:-mx-8 overflow-hidden">
      <div class="py-2 align-middle inline-block min-w-full px-4 sm:px-6 lg:px-8">
        <div class="shadow overflow-hidden border-b border-gray-200 rounded-lg bg-white p-6 space-y-8 relative">
          <div>
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ props.mode === 'EDIT' ? t('userEditCreate.title.edit') : t('userEditCreate.title.create') }}
            </h3>
            <hr class="my-4 border-gray-200" />
          </div>
          
          <!-- Profile Picture Preview -->
          <div class="flex flex-col items-center gap-4 mb-8">
            <div class="relative w-32 h-32">
              <img v-if="isValidImageUrl" :src="data.pictureUrl" class="w-full h-full rounded-full object-cover border border-gray-300" />
              <img v-else-if="previewJdenticon" :src="previewJdenticon" class="w-full h-full rounded-full object-cover border border-gray-300" />
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
                  <input id="pictureUrl" v-model="data.pictureUrl" type="url" :class="[errors.pictureUrl ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md pr-10']" />
                  <button v-if="data.pictureUrl" type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="t('userEditCreate.removePicture')" @click="removePicture">
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
                <input id="firstName" v-model="data.firstName" type="text" required :class="[errors.firstName ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md']" />
                <p v-if="errors.firstName" class="mt-1 text-sm text-red-600">{{ errors.firstName }}</p>
              </div>
            </div>

            <!-- Last Name row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="lastName" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.lastName') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <input id="lastName" v-model="data.lastName" type="text" required :class="[errors.lastName ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md']" />
                <p v-if="errors.lastName" class="mt-1 text-sm text-red-600">{{ errors.lastName }}</p>
              </div>
            </div>

            <!-- Username row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="username" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.username') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <input id="username" v-model="data.name" type="text" required :disabled="props.mode === 'EDIT'" :class="[errors.username ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md disabled:bg-gray-200 disabled:cursor-not-allowed']" />
                <p v-if="errors.username" class="mt-1 text-sm text-red-600">{{ errors.username }}</p>
              </div>
            </div>

            <!-- Email row -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <label for="email" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
                {{ t('userEditCreate.email') }}
              </label>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <input id="email" v-model="data.email" type="email" :required="emailRequired" :class="[errors.email ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md']" />
                <p v-if="errors.email" class="mt-1 text-sm text-red-600">{{ errors.email }}</p>
                <p v-else-if="data.email?.trim() && !isValidEmail(data.email.trim())" class="mt-1 text-sm text-red-600">
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
                <Listbox v-model="selectedRoleOptions" multiple as="div">
                  <div class="relative">
                    <ListboxButton class="relative w-full rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-sm focus:ring-primary text-sm">
                      <div class="flex flex-wrap gap-2">
                        <template v-if="selectedRoleOptions.length > 0">
                          <button v-for="role in selectedRoleOptions" :key="role" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20" @click.stop="removeRole(role)">
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
                  <InformationCircleIcon class="w-5 h-5 mt-0.5 text-blue-400 shrink-0" aria-hidden="true" />
                  <p>
                    {{ props.mode === 'EDIT' ? t('userEditCreate.edit.passwordInfo') : t('userEditCreate.create.passwordInfo') }}
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
                  <input id="password" v-model="password" :type="passwordInputType" :class="[errors.password ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md pr-10']" />
                  <button type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="passwordInputType === 'password' ? t('userEditCreate.showPassword') : t('userEditCreate.hidePassword')" @click="togglePasswordVisibility">
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
                  <input id="passwordConfirm" v-model="passwordConfirm" :type="passwordInputType" :class="[errors.passwordConfirm ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md pr-10']" />
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
                  <button type="button" class="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md bg-white text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="cancelAction">
                    {{ t('common.cancel') }}
                  </button>
                  <button type="submit" :disabled="processing || !userDataHasUnsavedChanges || password !== passwordConfirm" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                    <span>{{ props.mode === 'EDIT' ? t('common.save') : t('common.create') }}</span>
                  </button>
                  <div v-if="userDataHasUnsavedChanges && props.mode === 'EDIT'" class="flex items-center whitespace-nowrap gap-1 text-sm text-yellow-700">
                    <ExclamationTriangleIcon class="w-4 h-4 m-1 text-yellow-500" />
                    {{ t('common.unsavedChanges') }}&nbsp;
                    <button type="button" class="underline hover:text-yellow-900" @click="resetUserData()">
                      {{ t('common.undo') }}
                    </button>
                  </div>
                </div>
                <p v-if="submitError" class="mt-2 text-sm text-red-600">{{ submitError }}</p>
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
import { CheckIcon, ChevronUpDownIcon, ExclamationTriangleIcon, EyeIcon, EyeSlashIcon, InformationCircleIcon, TrashIcon, UserIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, reactive, ref, shallowRef, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import backend, { generateFallbackPictureUrl, isAxiosError, isSelectableRealmRole, RealmRole, SelectableRealmRole, UserDto } from '../../common/backend';
import { FormValidator } from '../../common/formvalidator';
import { debounce } from '../../common/util';
import BreadcrumbNav from '../BreadcrumbNav.vue';
import FetchError from '../FetchError.vue';

const props = defineProps<{
  id: undefined,
  mode: 'CREATE',
} | {
  id: string,
  mode: 'EDIT',
}>();

type EditableUserData = Pick<UserDto, 'firstName' | 'lastName' | 'name' | 'email' | 'pictureUrl'> & { realmRoles: RealmRole[] };
const initialData = shallowRef<EditableUserData>({ firstName: undefined, lastName: undefined, name: '', email: '', realmRoles: ['user'], pictureUrl: undefined });
const data = reactive<EditableUserData>(initialData.value);

const userDataHasUnsavedChanges = computed(() => {
  return data.firstName !== initialData.value.firstName
    || data.lastName !== initialData.value.lastName
    || (props.mode !== 'EDIT' && data.name !== initialData.value.name)
    || data.email !== initialData.value.email
    || JSON.stringify([...data.realmRoles].sort()) !== JSON.stringify([...initialData.value.realmRoles].sort())
    || data.pictureUrl !== initialData.value.pictureUrl
    || password.value.length > 0;
});

function resetUserData() {
  data.firstName = initialData.value.firstName;
  data.lastName = initialData.value.lastName;
  data.name = initialData.value.name;
  data.email = initialData.value.email;
  data.realmRoles = [...initialData.value.realmRoles];
  data.pictureUrl = initialData.value.pictureUrl;
  
  password.value = '';
  passwordConfirm.value = '';
}

const { t } = useI18n({ useScope: 'global' });
const router = useRouter();
const loading = ref(true);
const onFetchError = ref<Error>();

const selectedRoleOptions = computed({
  get() {
    return data.realmRoles.filter(isSelectableRealmRole);
  },
  set(newValue) {
    data.realmRoles = ['user', ...newValue];
  }
});
const roleOptions: Record<SelectableRealmRole, string> = {
  'admin': 'Admin',
  'create-vaults': 'Create Vaults',
};

const errors = ref<Record<string, string>>({});
const processing = ref(false);
const submitError = ref<string>();

const password = ref('');
const passwordConfirm = ref('');
const passwordInputType = ref<'password' | 'text'>('password');
const passwordStrength = ref<'weak' | 'medium' | 'strong' | ''>('');

const isValidImageUrl = ref<boolean>(false);
const previewJdenticon = ref<string>();
const debouncedValidateImageUrl = debounce(async (url?: string) => {
  isValidImageUrl.value = await FormValidator.validateImageUrl(url);
}, 500);

watch(() => data.pictureUrl, debouncedValidateImageUrl, { immediate: true });

onMounted(async () => {
  loading.value = true;
  if (props.mode === 'EDIT') {
    previewJdenticon.value = generateFallbackPictureUrl('USER', props.id);
    try {
      const fetchedUser = await backend.users.getUser(props.id, false, true);
      initialData.value = { ...fetchedUser, realmRoles: fetchedUser.realmRoles ?? [] };
    } catch (error) {
      console.error('Failed to fetch user data:', error);
      onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
    } finally {
      loading.value = false;
    }
  } else {
    initialData.value = { firstName: undefined, lastName: undefined, name: '', email: '', realmRoles: [], pictureUrl: undefined };
    loading.value = false;
  }
  resetUserData();
});

function removeRole(role: SelectableRealmRole) {
  selectedRoleOptions.value = selectedRoleOptions.value.filter(r => r !== role);
}

function togglePasswordVisibility() {
  passwordInputType.value = passwordInputType.value === 'password' ? 'text' : 'password';
}

async function validateForm(): Promise<boolean> {
  const result = FormValidator.validateUser({
    firstName: data.firstName,
    lastName: data.lastName,
    username: data.name,
    email: data.email,
    password: password.value,
    passwordConfirm: passwordConfirm.value,
    isEditMode: props.mode === 'EDIT',
    initialEmail: initialData.value.email,
    pictureUrl: data.pictureUrl,
    isValidImageUrl: await FormValidator.validateImageUrl(data.pictureUrl)
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
const emailRequired = computed(() => FormValidator.isEmailRequired(props.mode === 'EDIT', initialData.value.email));

function evaluatePasswordStrength(pw: string): 'weak' | 'medium' | 'strong' | '' {
  return FormValidator.evaluatePasswordStrength(pw);
}

watch(password, (newVal) => {
  passwordStrength.value = evaluatePasswordStrength(newVal);
});

function removePicture() {
  data.pictureUrl = undefined;
  isValidImageUrl.value = false;
}

async function onSubmit() {
  if (!await validateForm()) {
    return;
  }

  processing.value = true;
  submitError.value = undefined;

  data.firstName = data.firstName?.trim();
  data.lastName = data.lastName?.trim();
  data.name = data.name.trim();
  data.email = data.email?.trim();
  data.pictureUrl = data.pictureUrl?.trim();

  try {
    if (props.mode === 'EDIT') { // edit mode
      await backend.users.updateUser(props.id, { ...data, password: password.value || undefined });
      router.push(`/app/users/${props.id}`); // navigate to user detail page after save
    } else { // create mode
      await backend.users.createUser({ ...data, password: password.value });
      router.push('/app/users'); // navigate to user list page after creation
    }
  } catch (error: unknown) {
    console.error('Failed to save user:', error);
    if (!isAxiosError(error)) {
      submitError.value = error instanceof Error ? error.message : 'An error occurred';
    } else if (error.response?.status === 409 && error.response.data === 'EMAIL_EXISTS') {
      errors.value.email = t('userEditCreate.error.emailAlreadyExists');
    } else if (error.response?.status === 409) {
      errors.value.username = t('userEditCreate.error.userAlreadyExists');
    }
  } finally {
    processing.value = false;
  }
}

function cancelAction() {
  if (props.mode === 'EDIT') {
    router.push(`/app/users/${props.id}`);
  } else {
    router.push('/app/users');
  }
}
</script>
