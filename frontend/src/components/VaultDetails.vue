<template>
  <div v-if="vault == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="allowRetryFetch ? fetchData : null"/>
    </div>
  </div>

  <div v-else class="pb-16 space-y-6">
    <div v-if="vault.archived" class="rounded-md bg-yellow-50 p-4">
      <div class="flex">
        <div class="flex-shrink-0">
          <ExclamationTriangleIcon class="h-5 w-5 text-yellow-400" aria-hidden="true" />
        </div>
        <p class="ml-3 text-sm text-yellow-700">{{ t('vaultDetails.warning.archived') }}</p>
      </div>
    </div>

    <div>
      <h3 class="font-medium text-gray-900">{{ t('vaultDetails.description.header') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p v-if="vault.description.length > 0" class="text-sm text-gray-500">{{ vault.description }}</p>
        <p v-else class="text-sm text-gray-500 italic">{{ t('vaultDetails.description.empty') }}</p>
      </div>
    </div>

    <div>
      <h3 class="font-medium text-gray-900">{{ t('vaultDetails.information.header') }}</h3>
      <dl class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
        <div class="py-3 flex justify-between text-sm font-medium">
          <dt class="text-gray-500">{{ t('vaultDetails.information.created') }}</dt>
          <dd class="text-gray-900">{{ d(vault.creationTime, 'short') }}</dd>
        </div>
      </dl>
    </div>

    <div v-if="isVaultAdmin" class="space-y-6">
      <div>
        <h3 class="font-medium text-gray-900">{{ t('vaultDetails.sharedWith.title') }}</h3>
        <ul role="list" class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
          <template v-for="member in members.values()" :key="member.id">
            <li class="py-3 flex flex-col">
              <div class="flex justify-between items-center">
                <div class="flex items-center">
                  <img :src="member.pictureUrl" alt="" class="w-8 h-8 rounded-full" />
                  <p class="ml-4 text-sm font-medium text-gray-900">{{ member.name }}</p>
                </div>
                <button v-if="member.id != me?.id" type="button" class="ml-6 bg-white rounded-md text-sm font-medium text-red-600 hover:text-red-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="revokeUserAccess(member.id)">{{ t('common.remove') }}<span class="sr-only"> {{ member.name }}</span></button>
              </div>

              <p v-if="onRevokeUserAccessError[member.id] != null" class="text-sm text-red-900 text-right">
                {{ t('common.unexpectedError', [onRevokeUserAccessError[member.id].message]) }}
              </p>
            </li>
          </template>
          <li v-if="!vault.archived" class="py-2 flex flex-col">
            <div v-if="!addingUser" class="justify-between items-center">
              <button type="button" class="group -ml-1 bg-white p-1 rounded-md flex items-center focus:outline-none focus:ring-2 focus:ring-primary" @click="addingUser = true">
                <span class="w-8 h-8 rounded-full border-2 border-dashed border-gray-300 flex items-center justify-center text-gray-400">
                  <PlusSmallIcon class="h-5 w-5" aria-hidden="true" />
                </span>
                <span class="ml-4 text-sm font-medium text-primary group-hover:text-primary-l1">{{ t('common.share') }}</span>
              </button>
            </div>
            <SearchInputGroup v-else-if="addingUser" :action-title="t('common.add')" :on-search="searchAuthority" @action="addAuthority" />
            <p v-if="onAddUserError != null" class="text-sm text-red-900 text-right">
              {{ t('common.unexpectedError', [onAddUserError.message]) }}
            </p>
          </li>
        </ul>
      </div>

      <div>
        <h3 class="font-medium text-gray-900">{{ t('vaultDetails.actions.title') }}</h3>

        <div v-if="!vault.archived" class="mt-2 flex flex-col gap-2">
          <div class="flex gap-2">
            <button :disabled="usersRequiringAccessGrant.length == 0" type="button" class="flex-1 bg-primary py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="showGrantPermissionDialog()">
              {{ t('vaultDetails.actions.updatePermissions') }}
            </button>
            <button type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="reloadDevicesRequiringAccessGrant()">
              <span class="sr-only">{{ t('vaultDetails.actions.updatePermissions.reload') }}</span>
              <ArrowPathIcon class="h-5 w-5" aria-hidden="true" />
            </button>
          </div>
          <button type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showEditVaultMetadataDialog()">
            {{ t('vaultDetails.actions.editVaultMetadata') }}
          </button>
          <button type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showDownloadVaultTemplateDialog()">
            {{ t('vaultDetails.actions.downloadVaultTemplate') }}
          </button>
          <button type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showRecoveryKeyDialog()">
            {{ t('vaultDetails.actions.showRecoveryKey') }}
          </button>
          <button type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white  hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showArchiveVaultDialog()">
            {{ t('vaultDetails.actions.archiveVault') }}
          </button>
        </div>

        <div v-else class="mt-2 flex flex-col gap-2">
          <button type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showRecoveryKeyDialog()">
            {{ t('vaultDetails.actions.showRecoveryKey') }}
          </button>
          <button type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white  hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showReactivateVaultDialog()">
            {{ t('vaultDetails.actions.reactivateVault') }}
          </button>
        </div>
      </div>
    </div>

    <div v-else>
      <button type="button" class="bg-primary py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showManageVaultDialog()">
        {{ t('vaultDetails.manageVault') }}
      </button>
    </div>
  </div>

  <AuthenticateVaultAdminDialog v-if="authenticatingVaultAdmin && vault != null" ref="authenticateVaultAdminDialog" :vault="vault" @action="vaultAdminAuthenticated" @close="authenticatingVaultAdmin = false" />
  <GrantPermissionDialog v-if="grantingPermission && vault != null && vaultKeys != null" ref="grantPermissionDialog" :vault="vault" :users="usersRequiringAccessGrant" :vault-keys="vaultKeys" @close="grantingPermission = false" @permission-granted="permissionGranted()" />
  <EditVaultMetadataDialog v-if="editingVaultMetadata && vault != null && vaultKeys != null" ref="editVaultMetadataDialog" :vault="vault" @close="editingVaultMetadata = false" @updated="v => refreshVault(v)" />
  <DownloadVaultTemplateDialog v-if="downloadingVaultTemplate && vault != null && vaultKeys != null" ref="downloadVaultTemplateDialog" :vault="vault" :vault-keys="vaultKeys" @close="downloadingVaultTemplate = false" />
  <RecoveryKeyDialog v-if="showingRecoveryKey && vault != null && vaultKeys != null" ref="recoveryKeyDialog" :vault="vault" :vault-keys="vaultKeys" @close="showingRecoveryKey = false" />
  <ArchiveVaultDialog v-if="archivingVault && vault != null" ref="archiveVaultDialog" :vault="vault" @close="archivingVault = false" @archived="v => refreshVault(v)" />
  <ReactivateVaultDialog v-if="reactivatingVault && vault != null" ref="reactivateVaultDialog" :vault="vault" @close="reactivatingVault = false" @reactivated="v => refreshVault(v)" />
</template>

<script setup lang="ts">
import { ArrowPathIcon, ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { PlusSmallIcon } from '@heroicons/vue/24/solid';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AuthorityDto, ConflictError, NotFoundError, UserDto, VaultDto } from '../common/backend';
import { VaultKeys } from '../common/crypto';
import ArchiveVaultDialog from './ArchiveVaultDialog.vue';
import AuthenticateVaultAdminDialog from './AuthenticateVaultAdminDialog.vue';
import DownloadVaultTemplateDialog from './DownloadVaultTemplateDialog.vue';
import EditVaultMetadataDialog from './EditVaultMetadataDialog.vue';
import FetchError from './FetchError.vue';
import GrantPermissionDialog from './GrantPermissionDialog.vue';
import ReactivateVaultDialog from './ReactivateVaultDialog.vue';
import RecoveryKeyDialog from './RecoveryKeyDialog.vue';
import SearchInputGroup from './SearchInputGroup.vue';

const { t, d } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vaultId: string
}>();

const emit = defineEmits<{
  vaultUpdated: [updatedVault: VaultDto]
}>();

const onFetchError = ref<Error | null>();
const allowRetryFetch = computed(() => onFetchError.value != null && !(onFetchError.value instanceof NotFoundError));  //fetch requests either list something, or query from th vault. In the latter, a 404 indicates the vault does not exists anymore.

const onRevokeUserAccessError = ref< {[id: string]: Error} >({});
const onAddUserError = ref<Error | null>();

const isVaultAdmin = ref(false);
const addingUser = ref(false);
const grantingPermission = ref(false);
const grantPermissionDialog = ref<typeof GrantPermissionDialog>();
const editingVaultMetadata = ref(false);
const editVaultMetadataDialog = ref<typeof EditVaultMetadataDialog>();
const downloadingVaultTemplate = ref(false);
const downloadVaultTemplateDialog = ref<typeof DownloadVaultTemplateDialog>();
const showingRecoveryKey = ref(false);
const recoveryKeyDialog = ref<typeof RecoveryKeyDialog>();
const archivingVault = ref(false);
const archiveVaultDialog = ref<typeof ArchiveVaultDialog>();
const reactivatingVault = ref(false);
const reactivateVaultDialog = ref<typeof ReactivateVaultDialog>();
const vault = ref<VaultDto>();
const members = ref<Map<string, AuthorityDto>>(new Map());
const usersRequiringAccessGrant = ref<UserDto[]>([]);
const authenticateVaultAdminDialog = ref<typeof AuthenticateVaultAdminDialog>();
const authenticatingVaultAdmin = ref(false);
const vaultKeys = ref<VaultKeys>();
const me = ref<UserDto>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    vault.value = await backend.vaults.get(props.vaultId);
    me.value = await backend.users.me(false);
  } catch (error) {
    console.error('Fetching data failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function vaultAdminAuthenticated(keys: VaultKeys) {
  try {
    (await backend.vaults.getMembers(props.vaultId, keys)).forEach(member => members.value.set(member.id, member));
    usersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId, keys);
    isVaultAdmin.value = true; //only set if we can retrieve all necessary information
    vaultKeys.value = keys;
  } catch (error) {
    console.error('Getting members or devices requiring access grant failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function reloadDevicesRequiringAccessGrant() {
  try {
    if (vaultKeys.value) {
      usersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId, vaultKeys.value);
    }
  } catch (error) {
    console.error('Getting devices requiring access grant failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function isAuthorityDto(toCheck: any): toCheck is AuthorityDto {
  return (toCheck as AuthorityDto).type != null;
}

async function addAuthority(authority: unknown) {
  onAddUserError.value = null;
  if (!isAuthorityDto(authority)) {
    throw new Error('Parameter authority is not of type AuthorityDto.');
  }

  try {
    if (isVaultAdmin.value && vaultKeys.value) {
      await addAuthorityBackend(authority);
      members.value.set(authority.id, authority);
      usersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId, vaultKeys.value);
    }
  } catch (error) {
    //even if error instanceof NotFoundError, it is not expected from user perspective
    console.error('Adding member failed.', error);
    onAddUserError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function addAuthorityBackend(authority: AuthorityDto) {
  try {
    if (vaultKeys.value) {
      if (authority.type.toLowerCase() == 'user') {
        await backend.vaults.addUser(props.vaultId, authority.id, vaultKeys.value);
      } else if (authority.type.toLowerCase() == 'group') {
        await backend.vaults.addGroup(props.vaultId, authority.id, vaultKeys.value);
      } else {
        throw new Error('Unknown authority type \'' + authority.type + '\'');
      }
    } else {
      throw new Error('No vault keys provided.');
    }
  } catch (error) {
    if (! (error instanceof ConflictError)) {
      //backend is already up to date
      throw error;
    }
  }
}

async function showManageVaultDialog() {
  authenticatingVaultAdmin.value = true;
  nextTick(() => authenticateVaultAdminDialog.value?.show());
}

function showGrantPermissionDialog() {
  grantingPermission.value = true;
  nextTick(() => grantPermissionDialog.value?.show());
}

function showEditVaultMetadataDialog() {
  editingVaultMetadata.value = true;
  nextTick(() => editVaultMetadataDialog.value?.show());
}

function showDownloadVaultTemplateDialog() {
  downloadingVaultTemplate.value = true;
  nextTick(() => downloadVaultTemplateDialog.value?.show());
}

function showRecoveryKeyDialog() {
  showingRecoveryKey.value = true;
  nextTick(() => recoveryKeyDialog.value?.show());
}

function showArchiveVaultDialog() {
  archivingVault.value = true;
  nextTick(() => archiveVaultDialog.value?.show());
}

function showReactivateVaultDialog() {
  reactivatingVault.value = true;
  nextTick(() => reactivateVaultDialog.value?.show());
}

function permissionGranted() {
  usersRequiringAccessGrant.value = [];
}

function refreshVault(updatedVault: VaultDto) {
  vault.value = updatedVault;
  emit('vaultUpdated', updatedVault);
}

async function searchAuthority(query: string): Promise<AuthorityDto[]> {
  return (await backend.authorities.search(query))
    .filter(authority => !members.value.has(authority.id))
    .sort((a, b) => a.name.localeCompare(b.name));
}

async function revokeUserAccess(userId: string) {
  delete onRevokeUserAccessError.value[userId];
  try {
    if (isVaultAdmin.value && vaultKeys.value) {
      await backend.vaults.revokeUserAccess(props.vaultId, userId, vaultKeys.value);
      members.value.delete(userId);
      usersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId, vaultKeys.value);
    }
  } catch (error) {
    console.error('Revoking user access failed.', error);
    //404 not expected from user perspective
    onRevokeUserAccessError.value[userId] = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>
