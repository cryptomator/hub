<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>

  <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
    <div class="flex flex-col gap-6">
      <!-- User Info -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('user.detail.info') }}
          </h3>
          <button class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="goToUserEdit">
            <PencilIcon class="h-4 w-4 text-gray-500" aria-hidden="true" />
            {{ t('user.detail.edit') }}
          </button>
        </div>

        <div class="px-6 py-6">
          <div class="flex gap-6 items-start mb-6">
            <img :src="user.userPicture" alt="Profilbild" class="w-20 h-20 rounded-full object-cover border border-gray-300" />
            <div>
              <h2 class="text-xl font-semibold text-gray-900">{{ user.firstName }} {{ user.lastName }}</h2>
              <p class="text-sm text-gray-500 mt-1">{{ user.email }}</p>
            </div>
          </div>

          <div class="divide-y divide-gray-100">
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">
                {{ t('user.detail.name') }}
              </dt>
              <dd class="text-sm text-gray-900 font-medium">{{ user.firstName }} {{ user.lastName }}</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">
                {{ t('user.detail.username') }}
              </dt>
              <dd class="text-sm text-gray-900 font-medium">{{ user.username }}</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">
                {{ t('user.detail.email') }}
              </dt>
              <dd class="text-sm text-gray-900 font-medium">{{ user.email }}</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">
                {{ t('user.detail.roles') }}
              </dt>
              <dd class="flex flex-wrap justify-end gap-2">
                <span v-for="role in user.roles" :key="role" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20 capitalize">
                  {{ role }}
                </span>
              </dd>
            </div>
          </div>
        </div>
      </section>

      <!-- Devices -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('user.detail.devices') }}
          </h3>
        </div>
        <div class="px-6 py-6">
          <ul class="divide-y divide-gray-100">
            <li v-for="device in user.devices" :key="device" class="py-2 text-sm text-gray-900">
              {{ device }}
            </li>
            <li v-if="!user.devices?.length" class="text-sm text-gray-500">{{ t('common.none') }}</li>
          </ul>
        </div>
      </section>
    </div>

    <!-- Groups -->
    <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
      <div class="bg-gray-50 px-6 py-4 border-b border-gray-200">
        <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
          {{ t('user.detail.groups') }}
        </h3>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-300" aria-describedby="groupsTitle">
          <tbody class="divide-y divide-gray-200 bg-white">
            <tr v-for="group in paginatedGroups" :key="group">
              <td class="py-4 px-6 text-sm text-gray-900">{{ group }}</td>
            </tr>
            <tr v-if="!user.groups?.length">
              <td class="py-4 px-4 text-sm text-center text-gray-500">{{ t('common.none') }}</td>
            </tr>
          </tbody>
          <tfoot v-if="user.groups?.length" class="bg-gray-50">
            <tr>
              <td>
                <nav class="flex items-center justify-between px-4 py-3" :aria-label="t('common.pagination')">
                  <div class="text-sm text-gray-700 hidden sm:block">
                    <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p">
                      <span class="font-medium">{{ paginationBegin }}</span>
                      <span class="font-medium">{{ paginationEnd }}</span>
                    </i18n-t>
                  </div>
                  <div class="flex flex-1 justify-between sm:justify-end">
                    <button type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0 disabled:opacity-50 disabled:hover:bg-white disabled:cursor-not-allowed" :disabled="currentPage === 0" @click="showPreviousPage">
                      {{ t('common.previous') }}
                    </button>
                    <button type="button" class="ml-3 inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0 disabled:opacity-50 disabled:hover:bg-white disabled:cursor-not-allowed" :disabled="!hasNextPage" @click="showNextPage">
                      {{ t('common.next') }}
                    </button>
                  </div>
                </nav>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  </div>
</template>
  
<script setup lang="ts">
import { PencilIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
  
  interface DetailUser {
    firstName: string;
    lastName: string;
    username: string;
    roles: string[];
    password: string;
    email: string;
    userPicture?: string;
    groups?: string[];
    vaults?: { id: string }[];
    devices?: string[];
  }
  
const props = defineProps<{ id: string }>();
const { t } = useI18n({ useScope: 'global' });
const pageSize = ref(10);
const currentPage = ref(0);
  
const user = ref<DetailUser>({
  firstName: 'Max',
  lastName: 'Mustermann',
  username: 'maxmustermann',
  roles: ['create-vault', 'admin'],
  password: 'password123',
  email: 'max@example.com',
  userPicture: 'https://i.pravatar.cc/150?u=placeholder',
  groups: ['Admin', 'Support', 'User', 'Guest'],
  vaults: [{ id: 'Travel' }, { id: 'Tax' }],
  devices: ['Device1', 'Device2', 'Device3', 'Device4'], 
});
const loading = ref<boolean>(true);
  
onMounted(async () => {
  try {
    await new Promise((r) => setTimeout(r, 300));
  } finally {
    loading.value = false;
  }
});

const paginatedGroups = computed(() =>
  user.value.groups?.slice(
    currentPage.value * pageSize.value,
    (currentPage.value + 1) * pageSize.value
  ) ?? []
);

const hasNextPage = computed(() =>
  user.value.groups ? (currentPage.value + 1) * pageSize.value < user.value.groups.length : false
);

const paginationBegin = computed(() =>
  user.value.groups?.length ? currentPage.value * pageSize.value + 1 : 0
);

const paginationEnd = computed(() =>
  Math.min((currentPage.value + 1) * pageSize.value, user.value.groups?.length ?? 0)
);

function showNextPage() {
  if (hasNextPage.value) currentPage.value++;
}

function showPreviousPage() {
  if (currentPage.value > 0) currentPage.value--;
}

const router = useRouter();

function goToUserEdit() {
  router.push(`/app/authority/user/${props.id}/edit`);
};
</script>
