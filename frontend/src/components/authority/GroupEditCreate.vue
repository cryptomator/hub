<template>
  <!-- Loading placeholder -->
  <div v-if="loading" class="text-center py-10">
    {{ t('common.loading') }}
  </div>

  <!-- Edit/Create page -->
  <div v-else>
    <BreadcrumbNav v-if="props.mode === 'EDIT'" :crumbs="[ { label: t('nav.groups'), to: '/app/groups' }, { label: data.name, to:'/app/groups/' + props.id }, { label: t('common.edit') } ]"/>
    <BreadcrumbNav v-else :crumbs="[ { label: t('nav.groups'), to: '/app/groups' }, { label: t('common.create') } ]"/>
    <div class="-my-2 -mx-4 sm:-mx-6 lg:-mx-8 overflow-hidden">
      <div class="py-2 align-middle inline-block min-w-full px-4 sm:px-6 lg:px-8">
        <div class="shadow overflow-hidden border-b border-gray-200 rounded-lg bg-white p-6 space-y-8">
          <div>
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ props.mode === 'EDIT' ? t('groupEditCreate.title.edit') : t('groupEditCreate.title.create') }}
            </h3>
            <hr class="my-4 border-gray-200"/>
          </div>

          <!-- Profile Picture Preview -->
          <div class="flex flex-col items-center gap-4 mb-8">
            <div class="relative w-32 h-32">
              <img v-if="isValidImageUrl" :src="data.pictureUrl" class="w-full h-full rounded-full object-cover border border-gray-300" :alt="t('groupEditCreate.profilePicture')" />
              <img v-else-if="previewJdenticon" :src="previewJdenticon" class="w-full h-full rounded-full object-cover border border-gray-300"/>
              <div v-else class="w-full h-full rounded-full bg-gray-100 flex items-center justify-center text-gray-400">
                <UserGroupIcon class="w-12 h-12" />
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
                  <input id="pictureUrl" v-model="data.pictureUrl" type="url" :class="[errors.pictureUrl ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md pr-10']"/>
                  <button v-if="data.pictureUrl" type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-400 hover:text-gray-600 focus:outline-none" :aria-label="t('groupEditCreate.removePicture')" @click="removePicture">
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
                <input id="name" v-model="data.name" type="text" :class="[errors.name ? 'border-red-300 focus:border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-primary focus:border-primary', 'block w-full max-w-md shadow-sm sm:text-sm rounded-md']" required />
                <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
              </div>
            </div>

            <!-- Actions -->
            <div class="md:grid md:grid-cols-3 md:gap-6">
              <div></div>
              <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
                <div class="flex space-x-3">
                  <button type="button" class="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md bg-white text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="cancelAction">
                    {{ t('common.cancel') }}
                  </button>
                  <button type="submit" :disabled="processing || !groupDataHasUnsavedChanges" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                    <span v-if="!groupSaved">
                      {{ props.mode === 'EDIT' ? t('common.save') : t('common.create') }}
                    </span>
                    <span v-else>{{ t('common.saved') }}</span>
                  </button>
                  <div v-if="groupDataHasUnsavedChanges && props.mode === 'EDIT'" class="flex items-center whitespace-nowrap gap-1 text-sm text-yellow-700">
                    <ExclamationTriangleIcon class="w-4 h-4 m-1 text-yellow-500" />
                    {{ t('common.unsavedChanges') }}&nbsp;
                    <button type="button" class="underline hover:text-yellow-900" @click="resetGroupData()">
                      {{ t('common.undo') }}
                    </button>
                  </div>
                </div>
                <p v-if="onSaveError" class="mt-2 text-sm text-red-600">{{ t('common.unexpectedError', [onSaveError.message]) }}</p>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ExclamationTriangleIcon, TrashIcon, UserGroupIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, reactive, ref, shallowRef, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import backend, { generateFallbackPictureUrl, GroupDto, isAxiosError } from '../../common/backend';
import { FormValidator } from '../../common/formvalidator';
import { debounce } from '../../common/util';
import BreadcrumbNav from '../BreadcrumbNav.vue';

const props = defineProps<{
  id: undefined,
  mode: 'CREATE',
} | {
  id: string,
  mode: 'EDIT',
}>();

type EditableGroupData = Pick<GroupDto, 'name' | 'pictureUrl'>;
const initialData = shallowRef<EditableGroupData>({ name: '', pictureUrl: '' });
const data = reactive<EditableGroupData>(initialData.value);

const groupDataHasUnsavedChanges = computed(() => {
  return data.name !== initialData.value.name
    || data.pictureUrl !== initialData.value.pictureUrl;
});

function resetGroupData() {
  data.name = initialData.value.name;
  data.pictureUrl = initialData.value.pictureUrl;
}

const { t } = useI18n({ useScope: 'global' });
const router = useRouter();
const loading = ref(true);
const errors = ref<Record<string, string>>({});
const processing = ref(false);
const groupSaved = ref(false);
const onSaveError = ref<Error | null>(null);
const debouncedGroupSaved = debounce(() => groupSaved.value = false, 2000);
const isValidImageUrl = ref<boolean>(false);
const previewJdenticon = ref<string>();

watch(() => data.pictureUrl,
  async (newUrl) => {
    isValidImageUrl.value = await FormValidator.validateImageUrl(newUrl);
    previewJdenticon.value = generateFallbackPictureUrl('GROUP', props.id);
  },
  { immediate: true }
);

onMounted(async () => {
  loading.value = true;
  if (props.mode === 'EDIT') {
    previewJdenticon.value = generateFallbackPictureUrl('GROUP', props.id);
    try {
      initialData.value = await backend.groups.getGroup(props.id, false);
    } catch (error) {
      console.error('Failed to fetch group:', error);
    } finally {
      loading.value = false;
    }
  } else {
    initialData.value = {
      name: '',
      pictureUrl: ''
    };
    loading.value = false;
  }
  resetGroupData();
});

function removePicture() {
  data.pictureUrl = undefined;
}

function validateForm() {
  const result = FormValidator.validateGroup({
    name: data.name,
    pictureUrl: data.pictureUrl,
    isValidImageUrl: isValidImageUrl.value
  });

  errors.value = result.errors;
  return result.valid;
}

async function onSubmit() {
  if (!validateForm()) {
    return;
  }

  processing.value = true;
  onSaveError.value = null;

  data.name = data.name.trim();

  try {
    if (props.mode === 'EDIT') {
      await backend.groups.updateGroup(props.id, data);
    } else {
      await backend.groups.createGroup(data);
    }

    // Update initial data to match saved state
    initialData.value = {
      name: data.name,
      pictureUrl: data.pictureUrl
    };

    // Show saved success state
    groupSaved.value = true;
    debouncedGroupSaved();

    // Redirect after successful save
    if (props.mode === 'EDIT') {
      router.push(`/app/groups/${props.id}`);
    } else {
      router.push('/app/groups');
    }
  } catch (error: unknown) {
    console.error('Failed to save group:', error);
    processing.value = false;
    if (isAxiosError(error) && error.response?.status === 409) {
      errors.value.name = t('groupEditCreate.error.groupNameAlreadyExists');
    } else {
      onSaveError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
}

function cancelAction() {
  if (props.mode === 'EDIT') {
    router.push(`/app/groups/${props.id}`);
  } else {
    router.push('/app/groups');
  }
}
</script>