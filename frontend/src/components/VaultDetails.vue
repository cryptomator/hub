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
        <p v-if="vault.description && vault.description.length > 0" class="text-sm text-gray-500">{{ vault.description }}</p>
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

    <div v-if="vaultRole == 'OWNER' && !vaultRecoveryRequired" class="space-y-6">
      <div>
        <h3 class="font-medium text-gray-900">{{ t('vaultDetails.sharedWith.title') }}</h3>
        <ul role="list" class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
          <!-- member list -->
          <template v-for="member in members.values()" :key="member.id">
            <li class="py-3 flex flex-col">
              <div class="flex justify-between items-center">
                <div class="flex items-center text-ellipsis whitespace-nowrap overflow-hidden" :title="member.name">
                  <img :src="member.pictureUrl" alt="" class="w-8 h-8 rounded-full" />
                  <p class="w-full ml-4 text-sm font-medium text-gray-900 truncate">{{ member.name }}</p>
                  <div v-if="member.role == 'OWNER'" class="ml-3 inline-flex items-center rounded-md bg-gray-50 px-2 py-1 text-xs font-medium text-gray-600 ring-1 ring-inset ring-gray-500/10">{{ t('vaultDetails.sharedWith.badge.owner') }}</div>
                </div>
                <Menu v-if="member.id != me?.id" as="div" class="relative ml-2 inline-block flex-shrink-0 text-left">
                  <MenuButton class="group relative inline-flex h-8 w-8 items-center justify-center rounded-full bg-white focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2">
                    <span class="absolute -inset-1.5" />
                    <span class="sr-only">Open options menu</span>
                    <span class="flex h-full w-full items-center justify-center rounded-full">
                      <EllipsisVerticalIcon class="h-5 w-5 text-gray-400 group-hover:text-gray-500" aria-hidden="true" />
                    </span>
                  </MenuButton>
                  <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
                    <MenuItems class="absolute right-9 top-0 z-10 w-48 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
                      <div class="py-1">
                        <MenuItem v-if="member.role == 'MEMBER'" v-slot="{ active }" @click="updateMemberRole(member, 'OWNER')">
                          <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'cursor-pointer block px-4 py-2 text-sm']">
                            {{ t('vaultDetails.sharedWith.grantOwnership') }}
                          </div>
                        </MenuItem>
                        <MenuItem v-if="member.role == 'OWNER'" v-slot="{ active }" @click="updateMemberRole(member, 'MEMBER')">
                          <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'cursor-pointer block px-4 py-2 text-sm']">
                            {{ t('vaultDetails.sharedWith.revokeOwnership') }}
                          </div>
                        </MenuItem>
                        <MenuItem v-slot="{ active }" @click="removeMember(member.id)">
                          <div :class="[active ? 'bg-gray-100 text-red-900' : 'text-red-700', 'cursor-pointer block px-4 py-2 text-sm']">
                            {{ t('common.remove') }}<span class="sr-only"> {{ member.name }}</span>
                          </div>
                        </MenuItem>
                      </div>
                    </MenuItems>
                  </transition>
                </Menu>
              </div>

              <p v-if="onUpdateVaultMembershipError[member.id] != null" class="text-sm text-red-900 text-right mt-1">
                {{ t('common.unexpectedError', [onUpdateVaultMembershipError[member.id].message]) }}
              </p>
            </li>
          </template>
          <!-- add member -->
          <li v-if="!vault.archived && !licenseViolated" class="py-2 flex flex-col">
            <div v-if="!addingUser" class="justify-between items-center">
              <button type="button" class="group -ml-1 bg-white p-1 rounded-md flex items-center focus:outline-none focus:ring-2 focus:ring-primary" @click="addingUser = true">
                <span class="w-8 h-8 rounded-full border-2 border-dashed border-gray-300 flex items-center justify-center text-gray-400">
                  <PlusSmallIcon class="h-5 w-5" aria-hidden="true" />
                </span>
                <span class="ml-4 text-sm font-medium text-primary group-hover:text-primary-l1">{{ t('common.share') }}</span>
              </button>
            </div>
            <SearchInputGroup v-else-if="addingUser" :action-title="t('common.add')" :on-search="searchAuthority" @action="addAuthority" />
            <div v-if="onAddUserError != null">
              <p v-if="onAddUserError instanceof PaymentRequiredError" class="text-sm text-red-900 text-right mt-1">
                {{ t('vaultDetails.error.licenseViolated') }}
              </p>
              <p v-else class="text-sm text-red-900 text-right mt-1">
                {{ t('common.unexpectedError', [onAddUserError.message]) }}
              </p>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <!-- button bar -->
    <div>
      <h3 class="font-medium text-gray-900">{{ t('vaultDetails.actions.title') }}</h3>

      <!-- required legacy migration stuff, otherwise there is no owner -->
      <div v-if="isLegacyVault" class="mt-2 flex flex-col gap-2">
        <button type="button" class="bg-primary py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showClaimOwnershipDialog()">
          {{ t('vaultDetails.actions.claimOwnership') }}
        </button>
      </div>

      <!-- license violated -->
      <div v-else-if="licenseViolated" class="mt-2 flex flex-col gap-2">
        <!-- editMetadata button -->
        <button v-if="vaultRole == 'OWNER'" type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showEditVaultMetadataDialog()">
          {{ t('vaultDetails.actions.editVaultMetadata') }}
        </button>
        <!-- archiveVault button -->
        <button v-if="(vaultRole == 'OWNER' || isAdmin)" type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white  hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showArchiveVaultDialog()">
          {{ t('vaultDetails.actions.archiveVault') }}
        </button>
      </div>

      <!-- special owner reset stuff -->
      <div v-else-if="vaultRecoveryRequired" class="mt-2 flex flex-col gap-2">
        <div class="flex">
          <div class="flex-shrink-0">
            <ExclamationTriangleIcon class="mt-1 h-5 w-5 text-yellow-400" aria-hidden="true" />
          </div>
          <h3 class="ml-3 font-medium text-gray-900">{{ t('vaultDetails.recoverVault.title') }}</h3>
        </div>
        <p class="text-sm text-gray-500">{{ t('vaultDetails.recoverVault.description') }}</p>
        <button type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white  hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showRecoverVaultDialog()">
          {{ t('vaultDetails.actions.recoverVault') }}
        </button>
      </div>

      <!-- vault is archived -->
      <div v-else-if="vault.archived" class="mt-2 flex flex-col gap-2">
        <!-- downloadTemplate button -->
        <button v-if="vaultRole == 'OWNER'" type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showDownloadVaultTemplateDialog()">
          {{ t('vaultDetails.actions.downloadVaultTemplate') }}
        </button>
        <!-- displayRecoveryKey button -->
        <button v-if="vaultRole == 'OWNER'" type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showDisplayRecoveryKeyDialog()">
          {{ t('vaultDetails.actions.displayRecoveryKey') }}
        </button>
        <!-- reactivateVault button -->
        <button v-if="(vaultRole == 'OWNER' || isAdmin)" type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white  hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showReactivateVaultDialog()">
          {{ t('vaultDetails.actions.reactivateVault') }}
        </button>
      </div>

      <!-- regular vault -->
      <div v-else class="mt-2 flex flex-col gap-2">
        <!-- grantAccess button -->
        <div v-if="vaultRole == 'OWNER'" class="flex gap-2">
          <button :disabled="usersRequiringAccessGrant.length == 0" type="button" class="flex-1 bg-primary py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="showGrantPermissionDialog()">
            {{ t('vaultDetails.actions.updatePermissions') }}
          </button>
          <button type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="reloadDevicesRequiringAccessGrant()">
            <span class="sr-only">{{ t('vaultDetails.actions.updatePermissions.reload') }}</span>
            <ArrowPathIcon class="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
        <!-- editMetadata button -->
        <button v-if="vaultRole == 'OWNER'" type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showEditVaultMetadataDialog()">
          {{ t('vaultDetails.actions.editVaultMetadata') }}
        </button>
        <!-- downloadTemplate button -->
        <button v-if="vaultRole == 'OWNER'" type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showDownloadVaultTemplateDialog()">
          {{ t('vaultDetails.actions.downloadVaultTemplate') }}
        </button>
        <!-- displayRecoveryKey button -->
        <button v-if="vaultRole == 'OWNER'" type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showDisplayRecoveryKeyDialog()">
          {{ t('vaultDetails.actions.displayRecoveryKey') }}
        </button>
        <!-- archiveVault button -->
        <button v-if="(vaultRole == 'OWNER' || isAdmin)" type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white  hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showArchiveVaultDialog()">
          {{ t('vaultDetails.actions.archiveVault') }}
        </button>
      </div>
    </div>
  </div>

  <ClaimVaultOwnershipDialog v-if="claimingVaultOwnership && vault" ref="claimVaultOwnershipDialog" :vault="vault" @action="provedOwnership" @close="claimingVaultOwnership = false" />
  <GrantPermissionDialog v-if="grantingPermission && vault && vaultKeys" ref="grantPermissionDialog" :vault="vault" :users="usersRequiringAccessGrant" :vault-keys="vaultKeys" @close="grantingPermission = false" @permission-granted="permissionGranted()" />
  <EditVaultMetadataDialog v-if="editingVaultMetadata && vault" ref="editVaultMetadataDialog" :vault="vault" @close="editingVaultMetadata = false" @updated="v => refreshVault(v)" />
  <DownloadVaultTemplateDialog v-if="downloadingVaultTemplate && vault && vaultKeys" ref="downloadVaultTemplateDialog" :vault="vault" :vault-keys="vaultKeys" @close="downloadingVaultTemplate = false" />
  <DisplayRecoveryKeyDialog v-if="displayingRecoveryKey && vault && vaultKeys" ref="displayRecoveryKeyDialog" :vault="vault" :vault-keys="vaultKeys" @close="displayingRecoveryKey = false" />
  <ArchiveVaultDialog v-if="archivingVault && vault" ref="archiveVaultDialog" :vault="vault" @close="archivingVault = false" @archived="v => refreshVault(v)" />
  <ReactivateVaultDialog v-if="reactivatingVault && vault" ref="reactivateVaultDialog" :vault="vault" @close="reactivatingVault = false" @reactivated="v => { refreshVault(v); refreshLicense();}" />
  <RecoverVaultDialog v-if="recoveringVault && vault && me" ref="recoverVaultDialog" :vault="vault" :me="me" @close="recoveringVault = false" @recovered="fetchOwnerData()" />
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { ArrowPathIcon, EllipsisVerticalIcon, ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { PlusSmallIcon } from '@heroicons/vue/24/solid';
import { base64 } from 'rfc4648';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auth from '../common/auth';
import backend, { AuthorityDto, ConflictError, ForbiddenError, LicenseUserInfoDto, MemberDto, NotFoundError, PaymentRequiredError, UserDto, VaultDto, VaultRole } from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import { JWT, JWTHeader } from '../common/jwt';
import { VaultKeys } from '../common/vaultv8';
import ArchiveVaultDialog from './ArchiveVaultDialog.vue';
import ClaimVaultOwnershipDialog from './ClaimVaultOwnershipDialog.vue';
import DisplayRecoveryKeyDialog from './DisplayRecoveryKeyDialog.vue';
import DownloadVaultTemplateDialog from './DownloadVaultTemplateDialog.vue';
import EditVaultMetadataDialog from './EditVaultMetadataDialog.vue';
import FetchError from './FetchError.vue';
import GrantPermissionDialog from './GrantPermissionDialog.vue';
import ReactivateVaultDialog from './ReactivateVaultDialog.vue';
import RecoverVaultDialog from './RecoverVaultDialog.vue';
import SearchInputGroup from './SearchInputGroup.vue';

const { t, d } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vaultId: string,
  vaultRole: VaultRole | 'NONE',
}>();

const emit = defineEmits<{
  vaultUpdated: [updatedVault: VaultDto]
  licenseStatusUpdated: [license: LicenseUserInfoDto]
}>();

const onFetchError = ref<Error | null>();
const allowRetryFetch = computed(() => onFetchError.value != null && !(onFetchError.value instanceof NotFoundError));  //fetch requests either list something, or query from th vault. In the latter, a 404 indicates the vault does not exists anymore.

const onUpdateVaultMembershipError = ref< {[id: string]: Error} >({});
const onAddUserError = ref<Error | null>();

const license = ref<LicenseUserInfoDto>();
const addingUser = ref(false);
const grantingPermission = ref(false);
const grantPermissionDialog = ref<typeof GrantPermissionDialog>();
const editingVaultMetadata = ref(false);
const editVaultMetadataDialog = ref<typeof EditVaultMetadataDialog>();
const downloadingVaultTemplate = ref(false);
const downloadVaultTemplateDialog = ref<typeof DownloadVaultTemplateDialog>();
const displayingRecoveryKey = ref(false);
const displayRecoveryKeyDialog = ref<typeof DisplayRecoveryKeyDialog>();
const archivingVault = ref(false);
const archiveVaultDialog = ref<typeof ArchiveVaultDialog>();
const reactivatingVault = ref(false);
const reactivateVaultDialog = ref<typeof ReactivateVaultDialog>();
const recoveringVault = ref(false);
const recoverVaultDialog = ref<typeof RecoverVaultDialog>();
const vault = ref<VaultDto>();
const vaultKeys = ref<VaultKeys>();
const members = ref<Map<string, MemberDto>>(new Map());
const usersRequiringAccessGrant = ref<UserDto[]>([]);
const claimVaultOwnershipDialog = ref<typeof ClaimVaultOwnershipDialog>();
const claimingVaultOwnership = ref(false);
const me = ref<UserDto>();

const vaultRecoveryRequired = ref<boolean>(false);
const isAdmin = ref<boolean>();

const isLegacyVault = computed(() => vault.value?.authPublicKey != null);
const licenseViolated = computed(() => license.value?.isExpired() || license.value?.isExceeded());

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    isAdmin.value = (await auth).isAdmin();
    vault.value = await backend.vaults.get(props.vaultId);
    me.value = await backend.users.me(true);
    license.value = await backend.license.getUserInfo();
    if (props.vaultRole == 'OWNER') {
      await fetchOwnerData();
    }
  } catch (error) {
    console.error('Fetching data failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function fetchOwnerData() {
  try {
    (await backend.vaults.getMembers(props.vaultId)).forEach(member => members.value.set(member.id, member));
    usersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId);
    vaultRecoveryRequired.value = false;
    const vaultKeyJwe = await backend.vaults.accessToken(props.vaultId, true);
    vaultKeys.value = await loadVaultKeys(vaultKeyJwe);
  } catch (error) {
    if (error instanceof ForbiddenError) {
      vaultRecoveryRequired.value = true;
    } else if (error instanceof PaymentRequiredError) {
      //refetch license
      await refreshLicense();
    } else {
      console.error('Retrieving ownership failed.', error);
      onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
}

async function loadVaultKeys(vaultKeyJwe: string): Promise<VaultKeys> {
  if (!me.value || !me.value.publicKey) {
    throw new Error('User not initialized.');
  }
  const browserKeys = await BrowserKeys.load(me.value.id);
  if (browserKeys == null) {
    throw new Error('Browser keys not found.');
  }
  const browserId = await browserKeys.id();
  const myDevice = me.value.devices.find(d => d.id == browserId);
  if (myDevice == null) {
    throw new Error('Device not initialized.');
  }
  const userKeys = await UserKeys.decryptOnBrowser(myDevice.userPrivateKey, browserKeys.keyPair.privateKey, base64.parse(me.value.publicKey));
  return VaultKeys.decryptWithUserKey(vaultKeyJwe, userKeys.keyPair.privateKey);
}

async function provedOwnership(keys: VaultKeys, ownerKeyPair: CryptoKeyPair) {
  if (!me.value || !me.value.publicKey) {
    throw new Error('User not initialized.');
  }

  const header: JWTHeader = { alg: 'ES384', typ: 'JWT', b64: true };
  const now = Math.floor(Date.now() / 1000); // seconds since epoch in UTC
  const expire = now + 10;
  const payload = { sub: me.value.id, vaultId: props.vaultId.toLocaleLowerCase(), iat: now, nbf: now, exp: expire };
  const proof = await JWT.build(header, payload, ownerKeyPair.privateKey);
  try {
    vault.value = await backend.vaults.claimOwnership(props.vaultId, proof);
  } catch (error) {
    console.error('Failed to claim ownership of vault.', error);
    return;
  }

  const vaultKeyJwe = keys.encryptForUser(base64.parse(me.value.publicKey));
  try {
    await backend.vaults.grantAccess(props.vaultId, { userId: me.value.id, token: await vaultKeyJwe });
  } catch (error) {
    if (error instanceof ConflictError) {
      console.debug('User already member of this vault.');
    } else {
      console.error('Failed to grant access to self.', error);
    }
  }

  refreshVault(vault.value);
  await fetchOwnerData();
}

async function reloadDevicesRequiringAccessGrant() {
  try {
    usersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId);
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
    await addAuthorityBackend(authority);
    const addedMember = new MemberDto(authority.id, authority.name, authority.type, 'MEMBER', authority.pictureUrl);
    members.value.set(authority.id, addedMember);
    usersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId);
  } catch (error) {
    //even if error instanceof NotFoundError, it is not expected from user perspective
    console.error('Adding member failed.', error);
    onAddUserError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function addAuthorityBackend(authority: AuthorityDto) {
  try {
    if (authority.type.toLowerCase() == 'user') {
      await backend.vaults.addUser(props.vaultId, authority.id);
    } else if (authority.type.toLowerCase() == 'group') {
      await backend.vaults.addGroup(props.vaultId, authority.id);
    } else {
      throw new Error('Unknown authority type \'' + authority.type + '\'');
    }
  } catch (error) {
    if (! (error instanceof ConflictError)) {
      //backend is already up to date
      throw error;
    }
  }
}

async function showClaimOwnershipDialog() {
  claimingVaultOwnership.value = true;
  nextTick(() => claimVaultOwnershipDialog.value?.show());
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

function showDisplayRecoveryKeyDialog() {
  displayingRecoveryKey.value = true;
  nextTick(() => displayRecoveryKeyDialog.value?.show());
}

function showArchiveVaultDialog() {
  archivingVault.value = true;
  nextTick(() => archiveVaultDialog.value?.show());
}

function showReactivateVaultDialog() {
  reactivatingVault.value = true;
  nextTick(() => reactivateVaultDialog.value?.show());
}

function showRecoverVaultDialog() {
  recoveringVault.value = true;
  nextTick(() => recoverVaultDialog.value?.show());
}

function permissionGranted() {
  usersRequiringAccessGrant.value = [];
}

async function refreshLicense() {
  license.value = await backend.license.getUserInfo();
  emit('licenseStatusUpdated', license.value);
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

async function updateMemberRole(member: MemberDto, role: VaultRole) {
  delete onUpdateVaultMembershipError.value[member.id];
  try {
    switch (member.type) {
      case 'USER':
        await backend.vaults.addUser(props.vaultId, member.id, role);    
        break;
      case 'GROUP':
        await backend.vaults.addGroup(props.vaultId, member.id, role);
        break;
    }
    const updatedMember = members.value.get(member.id);
    if (updatedMember) {
      updatedMember.role = role;
    }
  } catch (error) {
    console.error('Updating member role failed.', error);
    //404 not expected from user perspective
    onUpdateVaultMembershipError.value[member.id] = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function removeMember(memberId: string) {
  delete onUpdateVaultMembershipError.value[memberId];
  try {
    await backend.vaults.removeAuthority(props.vaultId, memberId);
    members.value.delete(memberId);
    if (!licenseViolated.value) {
      usersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId);
    } else {
      await refreshLicense();
    }
  } catch (error) {
    console.error('Removing member access failed.', error);
    //404 not expected from user perspective
    onUpdateVaultMembershipError.value[memberId] = error instanceof Error ? error : new Error('Unknown Error');
  }
}

</script>
