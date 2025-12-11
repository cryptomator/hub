<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>
  <div v-else>
    <BreadcrumbNav :crumbs="[ { label: t('nav.users'), to: '/app/users' }, { label: user.username } ]" />
    <div class="flex flex-row items-center justify-between gap-3 pb-1 w-full border-b border-gray-200 mb-2">
      <!-- Headline -->
      <h2 id="title" class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl mb-4">
        {{ t('user.detail.info') }}
      </h2>

      <div class="flex gap-3 items-center -mt-4">
        <!-- Edit-Button -->
        <button class="w-full bg-primary py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showUserEdit()">
          {{ t('common.edit') }}
        </button>

        <!-- Ellipsis-Menü -->
        <Menu as="div" class="relative inline-block shrink-0 text-left">
          <MenuButton class="group p-1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2">
            <span class="sr-only">Open options menu</span>
            <EllipsisVerticalIcon class="h-5 w-5 text-gray-400 group-hover:text-gray-500" aria-hidden="true" />
          </MenuButton>

          <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 translate-y-1 scale-95" enter-to-class="transform opacity-100 translate-y-0 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 translate-y-0 scale-100" leave-to-class="transform opacity-0 translate-y-1 scale-95">
            <MenuItems class="absolute right-0 mt-2 z-10 w-48 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black/5 focus:outline-hidden">
              <div class="py-1">
                <MenuItem v-slot="{ active }">
                  <div :class="[ active ? 'bg-gray-100 text-red-900' : 'text-red-700', 'cursor-pointer block px-4 py-2 text-sm']" @click="showDeleteUserDialog()">
                    {{ t('common.remove') }}
                  </div>
                </MenuItem>
              </div>
            </MenuItems>
          </transition>
        </Menu>
      </div>
    </div>
    <div class="hidden lg:grid grid-cols-1 lg:grid-cols-2 gap-6 items-start pt-3">
      <section class="lg:col-start-1 grid gap-6">
        <!-- User Info -->
        <UserInfo :user="user"/>
        <!-- Devices -->
        <UserDeviceList :devices="user.devices" :page-size="10" :title="t('user.detail.devices')"/>      
        <UserDeviceList :devices="user.legacyDevices" :page-size="10" :visible="user.legacyDevices.length != 0" :title="t('legacyDeviceList.title')" :info="t('legacyDeviceList.title')"/>
      </section>
      <section class="lg:col-start-2 grid gap-6">
        <!-- Groups -->
        <UserGroupsList :user="user" :user-id="props.id" :groups="user.groups" :page-size="10" :on-saved="handleGroupsSaved"/>
        <!-- Vaults -->
        <VaultList :vaults="user.vaults" :page-size="10" :visible="true"/>
      </section>
    </div>
    <div class="grid lg:hidden grid-cols-1 gap-6 items-start pt-3">
      <!-- User Info -->
      <UserInfo :user="user"/>
      <!-- Groups -->
      <UserGroupsList :user="user" :user-id="props.id" :groups="user.groups" :page-size="10" :on-saved="handleGroupsSaved"/>
      <!-- Devices -->
      <UserDeviceList :devices="user.devices" :page-size="10" :title="t('user.detail.devices')"/>
      <UserDeviceList :devices="user.legacyDevices" :page-size="10" :visible="user.legacyDevices.length != 0" :title="t('legacyDeviceList.title')" :info="t('legacyDeviceList.title')"/>
      <!-- Vaults -->
      <VaultList :vaults="user.vaults" :page-size="10" :visible="true"/>
    </div>
  </div>

  <!-- Dialogs -->
  <UserDeleteDialog v-if="deletingUser != null" ref="deleteUserDialog" :user="deletingUser" @close="deletingUser = null" @delete="onUserDeleted"/>
</template>

<script setup lang="ts">
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { onMounted, ref, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import backend from '../../common/backend';
import BreadcrumbNav from '../BreadcrumbNav.vue';
import UserDeleteDialog from './UserDeleteDialog.vue';
import UserDeviceList from './UserDeviceList.vue';
import UserGroupsList from './UserGroupsList.vue';
import UserInfo from './UserInfo.vue';
import VaultList from './VaultList.vue';

interface Group {
  id: string;
  name: string;
  pictureUrl?: string;
}

interface Vault {
  id: string;
  name: string;
  description?: string;
  archived: boolean;
  role?: 'OWNER' | 'MEMBER';
}

interface Device {
  id: string;
  name: string;
  type: 'DESKTOP' | 'MOBILE' | 'TABLET';
  creationTime: string;
  lastAccessTime?: string;
  lastIpAddress?: string;
}

interface DetailUser {
  firstName?: string;
  lastName?: string;
  username: string;
  email: string;
  userPicture?: string;
  creationTime: string;
  groups: Group[];
  vaults: Vault[];
  devices: Device[];
  legacyDevices: Device[];
  roles: string[];
}

const props = defineProps<{ id: string }>();
const { t } = useI18n({ useScope: 'global' });
const router = useRouter();

interface UserDto {
  id: string;
  name: string;
  email: string;
  pictureUrl?: string;
  language?: string;
  devices?: Device[];
  legacyDevices?: Device[];
  groups?: Group[];
  vaults?: Vault[];
  createdTimestamp?: number;
  type?: 'USER';
  ecdhPublicKey?: string;
  ecdsaPublicKey?: string;
  firstName?: string;
  lastName?: string;
}

const deleteUserDialog = ref<typeof UserDeleteDialog>();
const deletingUser = ref<UserDto | null>(null);

const showDeleteUserDialog = () => {
  deletingUser.value = {
    id: props.id,
    name: user.value.username,
    email: user.value.email,
    pictureUrl: user.value.userPicture,
    firstName: user.value.firstName,
    lastName: user.value.lastName
  };
  nextTick(() => deleteUserDialog.value?.show());
};

const onUserDeleted = (deletedUser: UserDto) => {
  
};

const user = ref<DetailUser>({
  firstName: undefined,
  lastName: undefined,
  username: '',
  email: '',
  userPicture: undefined,
  creationTime: new Date().toISOString(),
  groups: [],
  vaults: [],
  devices: [],
  legacyDevices: [],
  roles: []
});

const loading = ref<boolean>(true);

function handleGroupsSaved(newGroups: Group[]) {
  const ids = new Set(user.value.groups.map(g => g.id));
  newGroups.forEach(g => {
    if (!ids.has(g.id)) user.value.groups.push(g);
  });
  user.value.groups.sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
}

onMounted(async () => {
  try {
    const fetchedUser = await backend.users.getUser(props.id);

    const userData = fetchedUser as { firstName?: string; lastName?: string };
    user.value.firstName = userData.firstName;
    user.value.lastName = userData.lastName;
    user.value.username = fetchedUser.name;
    user.value.email = fetchedUser.email;
    user.value.userPicture = fetchedUser.pictureUrl;

    if (fetchedUser.createdTimestamp) {
      user.value.creationTime = new Date(fetchedUser.createdTimestamp).toISOString();
    }

    // Load groups
    if (fetchedUser.groups) {
      user.value.groups = fetchedUser.groups;
    }

    // Load vaults
    if (fetchedUser.vaults) {
      user.value.vaults = fetchedUser.vaults;
    }

    // Load devices
    if (fetchedUser.devices) {
      user.value.devices = fetchedUser.devices;
    }

    // Load legacy devices
    if (fetchedUser.legacyDevices) {
      user.value.legacyDevices = fetchedUser.legacyDevices;
    }

    // Load roles
    const userRoles = (fetchedUser as { roles?: string[] }).roles || [];
    user.value.roles = userRoles.filter(r => r === 'admin' || r === 'create-vaults');
  } catch (error) {
    console.error('Failed to fetch user:', error);
  } finally {
    loading.value = false;
  }
});

function showUserEdit() {
  router.push(`/app/users/${props.id}/edit`);
}
</script>