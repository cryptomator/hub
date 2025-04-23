<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>

  <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6 p-6 items-start">
    <div class="flex flex-col gap-6">
      <!-- Group Info -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('group.detail.info') }}
          </h3>
          <button class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openEditDialog">
            <PencilIcon class="h-4 w-4 text-gray-500" aria-hidden="true" />
            {{ t('group.detail.edit') }}
          </button>
        </div>

        <div class="px-6 py-6">
          <div class="flex gap-6 items-start mb-6">
            <img :src="group.picture" alt="Group" class="w-20 h-20 rounded-full object-cover border border-gray-300"/>
            <div>
              <h2 class="text-xl font-semibold text-gray-900">{{ group.name }}</h2>
              <p v-if="group.description" class="text-sm text-gray-500 mt-1 whitespace-pre-wrap">
                {{ group.description }}
              </p>
            </div>
          </div>

          <div class="divide-y divide-gray-100">
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('group.detail.name') }}</dt>
              <dd class="text-sm text-gray-900 font-medium">{{ group.name }}</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('group.detail.visibility') }}</dt>
              <dd class="text-sm text-gray-900 font-medium">Private</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('group.detail.createdOn') }}</dt>
              <dd class="text-sm text-gray-900 font-medium">{{ d(group.createdAt, 'long') }}</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('group.detail.roles') }}</dt>
              <dd class="flex flex-wrap justify-end gap-2">
                <span v-for="role in ['create-vault', 'admin']" :key="role" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20 capitalize">
                  {{ role }}
                </span>
              </dd>
            </div>
          </div>
        </div>
      </section>

      <!-- Vaults -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('group.detail.vaults') }}
          </h3>
        </div>
        <div class="px-6 py-6">
          <ul class="divide-y divide-gray-100">
            <li v-for="vault in group.vaults" :key="vault.id" class="py-2">
              <div class="text-sm font-medium text-primary truncate">{{ vault.name }}</div>
              <div class="text-sm text-gray-500 truncate">{{ vault.path }}</div>
            </li>
            <li v-if="!group.vaults?.length" class="text-sm text-gray-500">{{ t('common.none') }}</li>
          </ul>
        </div>
      </section>
    </div>

    <!-- Users -->
    <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
      <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
        <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
          {{ t('group.detail.users') }}
        </h3>
        <button class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openEditDialog">
          <PencilIcon class="h-4 w-4 text-gray-500" aria-hidden="true" />
          {{ t('group.detail.edit') }}
        </button>
      </div>
      <div class="px-6 py-6">
        <ul class="divide-y divide-gray-100">
          <li v-for="user in group.users" :key="user.id + user.name" class="flex items-center justify-between gap-3 py-3">
            <div class="flex items-center gap-3">
              <img :src="user.userPicture" class="w-8 h-8 rounded-full object-cover border border-gray-300" />
              <div class="flex items-center gap-2">
                <span class="text-sm font-medium text-gray-900 truncate">
                  {{ user.name }}
                </span>
                <span v-if="user.role" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20 capitalize">
                  {{ user.role }}
                </span>
              </div>
            </div>
          </li>
        </ul>
      </div>
    </section>
  </div>

  <GroupEditDialog ref="editGroupDialog" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { PencilIcon } from '@heroicons/vue/24/outline';
import GroupEditDialog from './GroupEditDialog.vue';

interface User {
  id: string;
  name: string;
  userPicture?: string;
  role?: string;
}

interface Vault {
  id: string;
  name: string;
  path: string;
}

interface DetailGroup {
  id: string;
  name: string;
  picture?: string;
  createdAt: string;
  users: User[];
  vaults: Vault[];
  description?: string;
}

const props = defineProps<{ id: string }>();
const { t, d } = useI18n({ useScope: 'global' });
const editGroupDialog = ref<InstanceType<typeof GroupEditDialog> | null>(null);

const group = ref<DetailGroup>({
  id: props.id,
  name: 'Frontend‑Team',
  picture: 'https://i.pravatar.cc/150?u=group',
  createdAt: '2017-02-15T13:12:00Z',
  description: 'Das Frontend-Team ist für die Entwicklung der Benutzeroberfläche verantwortlich.',
  users: [
    { id: '1', name: 'Anna Marie Schmidtson', userPicture: 'https://i.pravatar.cc/50?u=anna', role: 'admin' },
    { id: '2', name: 'Abdelmajid Achhoud', userPicture: 'https://i.pravatar.cc/50?u=majid', role: 'admin' },
    { id: '3', name: 'Dr. Alexander v. d. Heide', userPicture: 'https://i.pravatar.cc/50?u=alex', role: 'admin' },
    { id: '4', name: 'Max Mustermann', userPicture: 'https://i.pravatar.cc/50?u=max', role: 'admin' },
    { id: '5', name: 'Erika Mustermann', userPicture: 'https://i.pravatar.cc/50?u=erika', role: 'admin' },
    { id: '6', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '7', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
    { id: '2', name: 'Abdelmajid Achhoud', userPicture: 'https://i.pravatar.cc/50?u=majid', role: 'admin' },
    { id: '3', name: 'Dr. Alexander v. d. Heide', userPicture: 'https://i.pravatar.cc/50?u=alex', role: 'admin' },
    { id: '4', name: 'Max Mustermann', userPicture: 'https://i.pravatar.cc/50?u=max', role: 'admin' },
    { id: '5', name: 'Erika Mustermann', userPicture: 'https://i.pravatar.cc/50?u=erika', role: 'admin' },
    { id: '6', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '7', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
    { id: '8', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '9', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
    { id: '10', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '11', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
    { id: '12', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '13', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
    { id: '14', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '15', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
    { id: '16', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '17', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
    { id: '18', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '19', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
    { id: '20', name: 'John Doe', userPicture: 'https://i.pravatar.cc/50?u=john', role: 'admin' },
    { id: '21', name: 'Jane Doe', userPicture: 'https://i.pravatar.cc/50?u=jane', role: 'admin' },
  ],
  vaults: [
    { id: 'v1', name: 'cryptobot-os', path: 'cryptomator/cryptobot-os.git' },
    { id: 'v2', name: 'ios', path: 'cryptomator/ios.git' },
    { id: 'v2', name: 'ios', path: 'cryptomator/ios.git' },
    { id: 'v3', name: 'android', path: 'cryptomator/android.git' },
    { id: 'v4', name: 'web', path: 'cryptomator/web.git' },
    { id: 'v5', name: 'desktop', path: 'cryptomator/desktop.git' },
    { id: 'v6', name: 'vaults', path: 'cryptomator/vaults.git' },
  ]
});

function openEditDialog() {
  editGroupDialog.value?.show();
}

const loading = ref(false);
</script>
