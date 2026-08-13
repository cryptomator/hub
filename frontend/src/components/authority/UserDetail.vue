<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>
  <FetchError v-else-if="fetchError" :error="fetchError" :retry="fetchUser" />
  <div v-else>
    <BreadcrumbNav :crumbs="[ { label: t('nav.users'), to: '/app/users' }, { label: user.name } ]" />
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
                <MenuItem v-if="user.enabled" v-slot="{ active, disabled }" :disabled="user.id === currentUserId">
                  <div :class="[ disabled ? 'text-gray-400 cursor-not-allowed' : ['cursor-pointer', active ? 'bg-gray-100 text-red-900' : 'text-red-700'], 'block px-4 py-2 text-sm']" @click="!disabled && showDisableUserDialog()">
                    {{ t('user.detail.disable') }}
                  </div>
                </MenuItem>
                <MenuItem v-else v-slot="{ active }">
                  <div :class="[ active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'cursor-pointer block px-4 py-2 text-sm']" @click="enableUser()">
                    {{ t('user.detail.enable') }}
                  </div>
                </MenuItem>
                <MenuItem v-slot="{ active, disabled }" :disabled="user.id === currentUserId">
                  <div :class="[ disabled ? 'text-gray-400 cursor-not-allowed' : ['cursor-pointer', active ? 'bg-gray-100 text-red-900' : 'text-red-700'], 'block px-4 py-2 text-sm']" @click="!disabled && showDeleteUserDialog()">
                    {{ t('common.remove') }}
                  </div>
                </MenuItem>
              </div>
            </MenuItems>
          </transition>
        </Menu>
      </div>
    </div>
    <p v-if="onEnableUserError" class="text-sm text-red-600 mb-2">{{ onEnableUserError.message }}</p>
    <div class="hidden lg:grid grid-cols-1 lg:grid-cols-2 gap-6 items-start pt-3">
      <section class="lg:col-start-1 grid gap-6">
        <!-- User Info -->
        <UserInfo :user="user" />
        <!-- Devices -->
        <UserDeviceList :devices="user.devices" :title="t('user.detail.devices')" />      
        <UserDeviceList :devices="user.legacyDevices" :visible="user.legacyDevices.length != 0" :title="t('legacyDeviceList.title')" :info="t('user.detail.legacyDeviceList.info')" />
      </section>
      <section class="lg:col-start-2 grid gap-6">
        <!-- Groups -->
        <UserGroupsList :user="user" :user-id="props.id" :groups="user.groups" @on-saved="handleGroupsSaved" />
        <!-- Vaults -->
        <VaultList :vaults="user.accessibleVaults" :visible="true" />
      </section>
    </div>
    <div class="grid lg:hidden grid-cols-1 gap-6 items-start pt-3">
      <!-- User Info -->
      <UserInfo :user="user" />
      <!-- Groups -->
      <UserGroupsList :user="user" :user-id="props.id" :groups="user.groups" @on-saved="handleGroupsSaved" />
      <!-- Devices -->
      <UserDeviceList :devices="user.devices" :title="t('user.detail.devices')" />
      <UserDeviceList :devices="user.legacyDevices" :visible="user.legacyDevices.length != 0" :title="t('legacyDeviceList.title')" :info="t('user.detail.legacyDeviceList.info')" />
      <!-- Vaults -->
      <VaultList :vaults="user.accessibleVaults" :visible="true" />
    </div>
  </div>

  <!-- Dialogs -->
  <UserDisableDialog v-if="disablingUser" ref="disableUserDialog" :user="disablingUser" @close="disablingUser = undefined" @disable="onUserDisabled" />
  <UserDeleteDialog v-if="deletingUser" ref="deleteUserDialog" :user="deletingUser" @close="deletingUser = undefined" @delete="onUserDeleted" />
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import backend, { asError, GroupDto, UserDto, UserDtoWithDetails } from '../../common/backend';
import userdata from '../../common/userdata';
import BreadcrumbNav from '../BreadcrumbNav.vue';
import FetchError from '../FetchError.vue';
import UserDeleteDialog from './UserDeleteDialog.vue';
import UserDisableDialog from './UserDisableDialog.vue';
import UserDeviceList from './UserDeviceList.vue';
import UserGroupsList from './UserGroupsList.vue';
import UserInfo from './UserInfo.vue';
import VaultList from './VaultList.vue';

const props = defineProps<{ id: string }>();
const { t } = useI18n({ useScope: 'global' });
const router = useRouter();

const deleteUserDialog = ref<typeof UserDeleteDialog>();
const deletingUser = ref<UserDto>();
const disableUserDialog = ref<typeof UserDisableDialog>();
const disablingUser = ref<UserDto>();

const showDeleteUserDialog = () => {
  deletingUser.value = user.value;
  nextTick(() => deleteUserDialog.value?.show());
};

const onUserDeleted = () => {
  router.push('/app/users');
};

const showDisableUserDialog = () => {
  disablingUser.value = user.value;
  nextTick(() => disableUserDialog.value?.show());
};

const onUserDisabled = async () => {
  await fetchUser();
};

const enableUser = async () => {
  onEnableUserError.value = undefined;
  try {
    await backend.users.setUserEnabled(props.id, true);
    await fetchUser();
  } catch (error) {
    console.error('Enabling user failed.', error);
    onEnableUserError.value = new Error(t('user.detail.error.enableFailed'));
  }
};

const user = ref<UserDtoWithDetails>({
  type: 'USER',
  id: props.id,
  name: '',
  pictureUrl: undefined,
  email: '',
  firstName: undefined,
  lastName: undefined,
  language: undefined,
  accessibleVaults: [],
  realmRoles: [],
  enabled: true,
  groups: [],
  devices: [],
  legacyDevices: [],
});

const loading = ref<boolean>(true);
const fetchError = ref<Error>();
const onEnableUserError = ref<Error>();
const currentUserId = ref<string>('');

async function handleGroupsSaved(newGroups: GroupDto[]) {
  await fetchUser(); // reload user to get updated vault list
}

async function fetchUser() {
  loading.value = true;
  fetchError.value = undefined;
  try {
    user.value = await backend.users.getUser(props.id);
    user.value.groups.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
  } catch (error) {
    console.error('Failed to fetch user:', error);
    fetchError.value = asError(error);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  currentUserId.value = (await userdata.me).id;
  await fetchUser();
});

function showUserEdit() {
  router.push(`/app/users/${props.id}/edit`);
}
</script>