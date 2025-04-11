<template>
  <TransitionRoot as="template" :show="open">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75 transition-opacity" @click.stop />
      </TransitionChild>  
      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
            <DialogPanel class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
              <form novalidate @submit.prevent>
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                    {{ t('createUserDialog.title') }}
                  </DialogTitle>  
                  <div class="mt-4 space-y-4">
                    <div class="text-center relative w-32 h-32 mx-auto">
                      <img v-if="previewUrl" class="w-full h-full rounded-full object-cover" :src="previewUrl" alt="Preview" />
                      <div v-else class="w-full h-full rounded-full bg-gray-100 flex items-center justify-center text-gray-400">
                        <UserIcon class="w-12 h-12" />
                      </div>
                      <button type="button" class="absolute bottom-0 right-0 bg-white rounded-full p-1 shadow-md hover:bg-gray-100" :aria-label="selectedPicture ? t('createUserDialog.removePicture') : t('createUserDialog.addPicture')" @click="selectedPicture ? removePicture() : triggerFileSelect()">
                        <component :is="selectedPicture ? TrashIcon : PlusIcon" class="w-5 h-5 text-gray-600" />
                      </button>
                      <input ref="fileInputRef" type="file" accept="image/*" class="hidden" @change="onPictureChange" />
                    </div>
                    <div>
                      <label for="name" class="block text-sm font-medium text-gray-700">{{ t('createUserDialog.name') }}</label>
                      <input id="name" v-model="name" type="text" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary sm:text-sm" required />
                    </div>
                    <div>
                      <label for="email" class="block text-sm font-medium text-gray-700">
                        {{ t('createUserDialog.email') }}
                      </label>
                      <input id="email" v-model="email" type="email" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary sm:text-sm" required />
                      <p v-if="email" :class="['mt-1 text-sm', emailValidColor]">
                        {{ emailValidLabel }}
                      </p>
                    </div>
                    <div>
                      <label for="password" class="block text-sm font-medium text-gray-700">
                        {{ t('createUserDialog.password') }}
                      </label>
                      <div class="mt-1 relative">
                        <input id="password" v-model="password" :type="passwordInputType" class="block w-full rounded-md border-gray-300 pr-10 shadow-sm focus:border-primary focus:ring-primary sm:text-sm" />
                        <button type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="showPassword ? t('common.hidePassword') : t('common.showPassword')" @click="togglePasswordVisibility">
                          <component :is="passwordInputType == 'password' ? EyeSlashIcon : EyeIcon" class="h-5 w-5" />
                        </button>
                      </div>
                      <p v-if="passwordStrengthLabel" :class="['mt-1 text-sm', passwordStrengthColor]">
                        {{ passwordStrengthLabel }}
                      </p>
                    </div>
                    <div>
                      <label for="confirmPassword" class="block text-sm font-medium text-gray-700">
                        {{ t('createUserDialog.confirmPassword') }}
                      </label>
                      <div class="mt-1 relative">
                        <input id="confirmPassword" v-model="confirmPassword" :type="passwordInputType" class="block w-full rounded-md border-gray-300 pr-10 shadow-sm focus:border-primary focus:ring-primary sm:text-sm" />
                        <button type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="showPassword ? t('common.hidePassword') : t('common.showPassword')" @click="togglePasswordVisibility">
                          <component :is="passwordInputType == 'password' ? EyeSlashIcon : EyeIcon" class="h-5 w-5"/>
                        </button>
                      </div>
                      <p v-if="confirmPassword" :class="['mt-1 text-sm', confirmPasswordColor]">
                        {{ confirmPasswordLabel }}
                      </p>
                    </div>
                  </div>
                </div>  
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm">
                    {{ t('common.create') }}
                  </button>
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                </div>
              </form>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';  
import { PencilIcon, EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline';
import { UserIcon, PlusIcon, TrashIcon } from '@heroicons/vue/24/outline';

const { t } = useI18n({ useScope: 'global' });  

const open = ref(false);
const name = ref('');
const password = ref('');
const showPassword = ref(false);
const passwordInputType = ref<'password' | 'text'>('password');
const confirmPassword = ref('');
const passwordStrength = ref<'weak' | 'medium' | 'strong' | ''>('');
const fileInputRef = ref<HTMLInputElement | null>(null);

function triggerFileSelect() {
  fileInputRef.value?.click();
}

function removePicture() {
  selectedPicture.value = null;
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewUrl.value = null;
  if (fileInputRef.value) fileInputRef.value.value = '';
}

function togglePasswordVisibility() {
  passwordInputType.value = passwordInputType.value == 'password' ? 'text' : 'password';
}

function evaluatePasswordStrength(pw: string): 'weak' | 'medium' | 'strong' | '' {
  if (!pw) return '';
  const strong = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{10,}$/;
  const medium = /^(?=.*[a-zA-Z])(?=.*\d).{6,}$/;
  if (strong.test(pw)) return 'strong';
  if (medium.test(pw)) return 'medium';
  return 'weak';
}

defineExpose({
  show,
});

const email = ref('');

const isValidEmail = (val: string): boolean => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val);
};

const emailValidLabel = computed(() => {
  if (!email.value) return '';
  return isValidEmail(email.value)
    ? t('createUserDialog.validEmail')
    : t('createUserDialog.invalidEmail');
});

const emailValidColor = computed(() => {
  if (!email.value) return 'text-gray-600';
  return isValidEmail(email.value) ? 'text-green-600' : 'text-red-600';
});

function show() {
  open.value = true;
  name.value = '';
  password.value = '';
  passwordStrength.value = '';
}

watch(password, (newVal) => {
  passwordStrength.value = evaluatePasswordStrength(newVal);
});

const passwordStrengthLabel = computed(() => {
  switch (passwordStrength.value) {
    case 'strong': return t('createUserDialog.passwordStrong');
    case 'medium': return t('createUserDialog.passwordMedium');
    case 'weak': return t('createUserDialog.passwordWeak');
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

const confirmPasswordLabel = computed(() => {
  if (!confirmPassword.value) return '';
  return confirmPassword.value === password.value
    ? t('createUserDialog.passwordsMatch')
    : t('createUserDialog.passwordsDontMatch');
});

const confirmPasswordColor = computed(() => {
  if (!confirmPassword.value) return 'text-gray-600';
  return confirmPassword.value === password.value ? 'text-green-600' : 'text-red-600';
});

const selectedPicture = ref<File | null>(null);
const previewUrl = ref<string | null>(null);

function onPictureChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) {
    selectedPicture.value = file;
    previewUrl.value = URL.createObjectURL(file);
  } else {
    selectedPicture.value = null;
    previewUrl.value = null;
  }
}
</script>
