<template>
  <div v-if="vault == null && !vaultNotFound">
    Loading…
  </div>

  <div v-else-if="vault == null && vaultNotFound">
    Vault not found
  </div>

  <div v-else-if="vault != null" class="pb-16 space-y-6">
    <!-- TODO: add metadata to vault in backend -->
    <div v-if="false">
      <h3 class="font-medium text-gray-900">Description</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500 italic">Add a description to this vault.</p>
        <button type="button" class="-mr-2 h-8 w-8 bg-white rounded-full flex items-center justify-center text-gray-400 hover:bg-gray-100 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-primary">
          <PencilIcon class="h-5 w-5" aria-hidden="true" />
          <span class="sr-only">Add description</span>
        </button>
      </div>
    </div>

    <div v-if="false">
      <h3 class="font-medium text-gray-900">Information</h3>
      <dl class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
        <div class="py-3 flex justify-between text-sm font-medium">
          <dt class="text-gray-500">Owned by</dt>
          <dd class="text-gray-900">Marie Culver</dd>
        </div>
        <div class="py-3 flex justify-between text-sm font-medium">
          <dt class="text-gray-500">Created</dt>
          <dd class="text-gray-900">June 8, 2020</dd>
        </div>
      </dl>
    </div>

    <div>
      <h3 class="font-medium text-gray-900">Shared with</h3>
      <ul role="list" class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
        <li v-for="member in members" :key="member.id" class="py-3 flex justify-between items-center">
          <div class="flex items-center">
            <img :src="member.pictureUrl" alt="" class="w-8 h-8 rounded-full" />
            <p class="ml-4 text-sm font-medium text-gray-900">{{ member.name }}</p>
          </div>
          <button type="button" class="ml-6 bg-white rounded-md text-sm font-medium text-red-600 hover:text-red-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="revokeUserAccess(member.id)">Remove<span class="sr-only"> {{ member.name }}</span></button>
        </li>
        <li class="py-2 flex justify-between items-center">
          <div v-if="!addingMember">
            <button type="button" class="group -ml-1 bg-white p-1 rounded-md flex items-center focus:outline-none focus:ring-2 focus:ring-primary" @click="addingMember = true">
              <span class="w-8 h-8 rounded-full border-2 border-dashed border-gray-300 flex items-center justify-center text-gray-400">
                <PlusSmIcon class="h-5 w-5" aria-hidden="true" />
              </span>
              <span class="ml-4 text-sm font-medium text-primary group-hover:text-primary-l1">Share</span>
            </button>
          </div>
          <SearchInputGroup v-else-if="addingMember" action-title="Add" :items="allUsers" class="flex-grow" @action="addMember" />
        </li>
      </ul>
    </div>

    <div class="flex gap-3">
      <div v-if="devicesRequiringAccessGrant.length > 0">
        <button type="button" class="flex-1 bg-primary py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showGrantPermissionDialog()">
          Update Permissions
        </button>
      </div>
      <button type="button" class="flex-1 bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showDownloadVaultTemplate()">
        Download Vault Template
      </button>
    </div>

    <GrantPermissionDialog v-if="grantingPermission" ref="grantPermissionDialog" :vault="vault" :devices="devicesRequiringAccessGrant" @close="grantingPermission = false" @permission-granted="permissionGranted()" />
    <DownloadVaultTemplateDialog v-if="downloadingVaultTemplate" ref="downloadVaultTemplateDialog" :vault="vault" @close="downloadingVaultTemplate = false" />
  </div>
</template>

<script setup lang="ts">
import { PencilIcon, PlusSmIcon } from '@heroicons/vue/solid';
import axios from 'axios';
import { nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { DeviceDto, UserDto, VaultDto } from '../common/backend';
import DownloadVaultTemplateDialog from './DownloadVaultTemplateDialog.vue';
import GrantPermissionDialog from './GrantPermissionDialog.vue';
import SearchInputGroup from './SearchInputGroup.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vaultId: string
}>();

const vaultNotFound = ref(false);
const addingMember = ref(false);
const grantingPermission = ref(false);
const grantPermissionDialog = ref<typeof GrantPermissionDialog>();
const downloadingVaultTemplate = ref(false);
const downloadVaultTemplateDialog = ref<typeof DownloadVaultTemplateDialog>();
const vault = ref<VaultDto>();
const members = ref<UserDto[]>([]);
const allUsers = ref<UserDto[]>([]);
const devicesRequiringAccessGrant = ref<DeviceDto[]>([]);

onMounted(async () => {
  try {
    vault.value = await backend.vaults.get(props.vaultId);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      vaultNotFound.value = true;
    } else {
      // TODO: error handling
      console.error('Retrieving vault failed.', error);
    }
  }
  try {
    members.value = await backend.vaults.getMembers(props.vaultId);
  } catch (error) {
    // TODO: error handling
    console.error('Retrieving vault members failed.', error);
  }
  try {
    allUsers.value = await backend.users.listAll();
  } catch (error) {
    // TODO: error handling
    console.error('Retrieving all users failed.', error);
  }
  try {
    devicesRequiringAccessGrant.value = await backend.vaults.getDevicesRequiringAccessGrant(props.vaultId);
  } catch (error) {
    // TODO: error handling
    console.error('Retrieving devices requiring access grant failed.', error);
  }
});

async function addMember(id: string) {
  try {
    const user = allUsers.value.find(u => u.id === id);
    if (user) {
      await backend.vaults.addMember(props.vaultId, id);
      members.value = members.value.concat(user);
      devicesRequiringAccessGrant.value = await backend.vaults.getDevicesRequiringAccessGrant(props.vaultId);
    }
  } catch (error) {
    // TODO: error handling
    console.error('Adding member failed.', error);
  }
}

function showGrantPermissionDialog() {
  grantingPermission.value = true;
  nextTick(() => grantPermissionDialog.value?.show());
}

function showDownloadVaultTemplate() {
  downloadingVaultTemplate.value = true;
  nextTick(() => downloadVaultTemplateDialog.value?.show());
}

function permissionGranted() {
  devicesRequiringAccessGrant.value = [];
}

async function revokeUserAccess(userId: string) {
  try {
    await backend.vaults.revokeUserAccess(props.vaultId, userId);
    members.value = members.value.filter(m => m.id !== userId);
    devicesRequiringAccessGrant.value = await backend.vaults.getDevicesRequiringAccessGrant(props.vaultId);
  } catch (error) {
    // TODO: error handling
    console.error('Revoking user access failed.', error);
  }
}
</script>
