<template>
  <div v-if="vault === undefined">
    <div v-if="!onFetchError">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="allowRetryFetch ? fetchData : undefined" />
    </div>
  </div>

  <div v-else class="pb-16 space-y-8">
    <div>
      <p v-if="vault.description && vault.description.length > 0" class="text-sm text-gray-500">{{ vault.description }}</p>
      <p v-else class="text-sm text-gray-500 italic">{{ t('vaultDetails.description.empty') }}</p>
      <p class="mt-1 text-sm text-gray-500">{{ t('vaultDetails.information.createdOn', [d(vault.creationTime, 'short')]) }}</p>
    </div>

    <div v-if="vault.archived" class="rounded-md bg-yellow-50 p-4">
      <div class="flex">
        <div class="shrink-0">
          <ExclamationTriangleIcon class="h-5 w-5 text-yellow-400" aria-hidden="true" />
        </div>
        <p class="ml-3 text-sm text-yellow-700">{{ t('vaultDetails.warning.archived') }}</p>
      </div>
    </div>

    <div v-if="vaultRole == 'OWNER' && !vaultRecoveryRequired" class="space-y-6">
      <div>
        <div class="flex items-center justify-between">
          <h3 class="font-medium text-gray-900">{{ t('vaultDetails.sharedWith.title') }}</h3>
          <button v-if="canUpdatePermissions" type="button" class="inline-flex size-8 items-center justify-center rounded-md text-gray-400 hover:bg-gray-100 hover:text-gray-500 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-primary" :title="t('common.refresh')" @click="reloadMembersRequiringAccessGrant()">
            <span class="sr-only">{{ t('common.refresh') }}</span>
            <ArrowPathIcon class="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
        <ul class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
          <!-- member list -->
          <template v-for="member in members" :key="member.id">
            <li class="py-3 flex flex-col">
              <div class="flex justify-between items-center">
                <div class="flex items-center whitespace-nowrap w-full" :title="member.name">
                  <img :src="member.pictureUrl" alt="" class="w-8 h-8 rounded-full" />
                  <p class="w-full ml-4 text-sm font-medium text-gray-900 truncate">{{ member.name }}</p>
                  <span v-if="member.type === 'GROUP'" class="ml-3 text-xs text-gray-500 italic whitespace-nowrap">{{ t('common.xMembers', [member.memberSize]) }}</span>
                  <TrustDetails v-if="member.type === 'USER'" :trusted-user="member" :trusts="trusts" @trust-changed="refreshTrusts()" />
                  <div v-if="member.vaultRole == 'OWNER'" class="ml-3 inline-flex items-center rounded-md bg-gray-50 px-2 py-1 text-xs font-medium text-gray-600 ring-1 ring-inset ring-gray-500/10">{{ t('vaultDetails.sharedWith.badge.owner') }}</div>
                  <Menu v-if="member.id != me?.id" as="div" class="relative ml-2 inline-block shrink-0 text-left">
                    <MenuButton class="group relative inline-flex h-8 w-8 items-center justify-center rounded-full bg-white focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2">
                      <span class="absolute -inset-1.5" />
                      <span class="sr-only">Open options menu</span>
                      <span class="flex h-full w-full items-center justify-center rounded-full">
                        <EllipsisVerticalIcon class="h-5 w-5 text-gray-400 group-hover:text-gray-500" aria-hidden="true" />
                      </span>
                    </MenuButton>
                    <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
                      <MenuItems class="absolute right-9 top-0 z-10 w-48 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black/5 focus:outline-hidden">
                        <div class="py-1">
                          <MenuItem v-if="member.vaultRole == 'MEMBER'" v-slot="{ active }" @click="requestUpdateMemberRole(member, 'OWNER')">
                            <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'cursor-pointer block px-4 py-2 text-sm']">
                              {{ t('vaultDetails.sharedWith.grantOwnership') }}
                            </div>
                          </MenuItem>
                          <MenuItem v-if="member.vaultRole == 'OWNER'" v-slot="{ active }" @click="requestUpdateMemberRole(member, 'MEMBER')">
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
              </div>

              <p v-if="onUpdateVaultMembershipError[member.id]" class="text-sm text-red-900 text-right mt-1">
                {{ t('common.unexpectedError', [onUpdateVaultMembershipError[member.id].message]) }}
              </p>
            </li>
          </template>
          <!-- add member -->
          <li v-if="!vault.archived && !licenseViolated" class="py-2 flex flex-col">
            <div v-if="!addingUser" class="justify-between items-center">
              <button type="button" class="group -ml-1 bg-white p-1 rounded-md flex items-center focus:outline-hidden focus:ring-2 focus:ring-primary" @click="addingUser = true">
                <span class="w-8 h-8 rounded-full border-2 border-dashed border-gray-300 flex items-center justify-center text-gray-400">
                  <PlusSmallIcon class="h-5 w-5" aria-hidden="true" />
                </span>
                <span class="ml-4 text-sm font-medium text-primary group-hover:text-primary-l1">{{ t('common.share') }}</span>
              </button>
            </div>
            <SearchInputGroup v-else-if="addingUser" :action-title="t('common.add')" place-holder="John Doe" :on-search="searchAuthority" @action="addAuthority" />
            <div v-if="onAddUserError">
              <p v-if="onAddUserError instanceof PaymentRequiredError" class="text-sm text-red-900 text-right mt-1">
                {{ t('vaultDetails.error.licenseViolated') }}
              </p>
              <p v-else class="text-sm text-red-900 text-right mt-1">
                {{ t('common.unexpectedError', [onAddUserError.message]) }}
              </p>
            </div>
          </li>
        </ul>
        <div v-if="canUpdatePermissions && membersRequiringAccessGrant.length > 0" class="mt-3 rounded-md bg-yellow-50 p-4">
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div class="flex flex-1 items-center gap-3">
              <ExclamationTriangleIcon class="h-5 w-5 shrink-0 text-yellow-400" aria-hidden="true" />
              <p class="text-sm text-yellow-700">{{ t('vaultDetails.sharedWith.accessPending', [membersRequiringAccessGrantNames]) }}</p>
            </div>
            <button type="button" class="w-full shrink-0 bg-primary py-2 px-3 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:w-auto sm:py-1.5" @click="showGrantPermissionDialog()">
              {{ t('vaultDetails.actions.grantAccess') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="hasActions">
      <h3 class="font-medium text-gray-900">{{ t('vaultDetails.actions.title') }}</h3>

      <!-- required legacy migration stuff, otherwise there is no owner -->
      <div v-if="isLegacyVault" class="mt-2 flex flex-col gap-2">
        <button type="button" class="bg-primary py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showClaimOwnershipDialog()">
          {{ t('vaultDetails.actions.claimOwnership') }}
        </button>
      </div>

      <!-- special owner reset stuff -->
      <div v-else-if="vaultRecoveryRequired && !licenseViolated" class="mt-2 flex flex-col gap-2">
        <div class="flex">
          <div class="shrink-0">
            <ExclamationTriangleIcon class="mt-1 h-5 w-5 text-yellow-400" aria-hidden="true" />
          </div>
          <h3 class="ml-3 font-medium text-gray-900">{{ t('vaultDetails.recoverVault.title') }}</h3>
        </div>
        <p class="text-sm text-gray-500">{{ t('vaultDetails.recoverVault.description') }}</p>
        <button type="button" class="bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white  hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showRecoverVaultDialog()">
          {{ t('vaultDetails.actions.recoverVault') }}
        </button>
      </div>

      <ul v-if="hasActionRows" class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
        <li v-if="canShowRecoveryKey" class="py-3 flex items-center gap-4">
          <KeyIcon class="h-5 w-5 shrink-0 text-gray-400" aria-hidden="true" />
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium text-gray-900">{{ t('vaultDetails.actions.recoveryKey') }}</p>
            <p class="text-sm text-gray-500">{{ t('vaultDetails.actions.recoveryKey.description') }}</p>
          </div>
          <button type="button" class="shrink-0 bg-white py-1.5 px-3 border border-gray-300 rounded-md shadow-xs text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showDisplayRecoveryKeyDialog()">
            {{ t('common.show') }}<span class="sr-only"> {{ t('vaultDetails.actions.recoveryKey') }}</span>
          </button>
        </li>
        <li v-if="emergencyAccessAction" class="py-3 flex items-center gap-4">
          <ExclamationTriangleIcon class="h-5 w-5 shrink-0 text-yellow-500" aria-hidden="true" />
          <div class="min-w-0 flex-1">
            <p class="text-sm font-medium text-gray-900">{{ t('vaultDetails.emergencyAccess') }}</p>
            <p class="text-sm text-gray-500">{{ emergencyAccessAction == 'setUp' ? t('vaultDetails.emergencyAccess.notSetUp') : t('vaultDetails.emergencyAccess.councilTooSmall') }}</p>
          </div>
          <button type="button" class="shrink-0 bg-white py-1.5 px-3 border border-gray-300 rounded-md shadow-xs text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showGrantEmergencyAccessDialog()">
            {{ emergencyAccessAction == 'setUp' ? t('vaultDetails.emergencyAccess.setUp') : t('vaultDetails.emergencyAccess.fix') }}<span class="sr-only"> {{ t('vaultDetails.emergencyAccess') }}</span>
          </button>
        </li>
        <li v-if="canToggleArchive" class="py-3 flex flex-col">
          <div class="flex items-center gap-4">
            <ArchiveBoxIcon class="h-5 w-5 shrink-0 text-gray-400" aria-hidden="true" />
            <div class="min-w-0 flex-1">
              <p class="text-sm font-medium text-gray-900">{{ vault.archived ? t('vaultDetails.actions.reactivate') : t('vaultDetails.actions.archive') }}</p>
              <p class="text-sm text-gray-500">{{ vault.archived ? t('vaultDetails.actions.reactivate.description') : t('vaultDetails.actions.archive.description') }}</p>
            </div>
            <button type="button" class="shrink-0 bg-white py-1.5 px-3 border border-gray-300 rounded-md shadow-xs text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="vault.archived ? reactivateVault() : showArchiveVaultDialog()">
              {{ vault.archived ? t('vaultDetails.actions.reactivate') : t('vaultDetails.actions.archive') }}
            </button>
          </div>
          <div v-if="onReactivateError">
            <p v-if="onReactivateError instanceof PaymentRequiredError" class="text-sm text-red-900 text-right mt-1">
              {{ t('vaultDetails.error.reactivateFailed') }}
            </p>
            <p v-else class="text-sm text-red-900 text-right mt-1">
              {{ t('common.unexpectedError', [onReactivateError.message]) }}
            </p>
          </div>
        </li>
      </ul>
    </div>

    <p v-if="onClaimOwnershipError" class="text-sm text-red-900 text-right mt-1">
      {{ t('common.unexpectedError', [onClaimOwnershipError.message]) }}
    </p>
  </div>

  <ClaimVaultOwnershipDialog v-if="claimingVaultOwnership && vault" ref="claimVaultOwnershipDialog" :vault="vault" @action="provedOwnership" @close="claimingVaultOwnership = false" />
  <GrantPermissionDialog v-if="grantingPermission && vault && (vaultFormat8 || uvfVault)" ref="grantPermissionDialog" :vault="vault" :users="membersRequiringAccessGrant" :vault-keys="(vaultFormat8 || uvfVault)!" @close="grantingPermission = false" @permission-granted="permissionGranted()" />
  <ConfirmDialog
    v-if="pendingRoleUpdate"
    ref="roleUpdateConfirmDialog"
    :title="t(pendingRoleUpdate.role === 'OWNER' ? 'vaultDetails.confirmRoleUpdate.grantOwnershipTitle' : 'vaultDetails.confirmRoleUpdate.revokeOwnershipTitle', [pendingRoleUpdate.member.name])"
    :description="t(pendingRoleUpdate.role === 'OWNER' ? 'vaultDetails.confirmRoleUpdate.grantOwnershipDescription' : 'vaultDetails.confirmRoleUpdate.revokeOwnershipDescription', [pendingRoleUpdate.member.name])"
    :confirm-label="t(pendingRoleUpdate.role === 'OWNER' ? 'vaultDetails.sharedWith.grantOwnership' : 'vaultDetails.sharedWith.revokeOwnership')"
    :tone="pendingRoleUpdate.untrusted ? 'danger' : 'neutral'"
    :icon="pendingRoleUpdate.untrusted ? undefined : KeyIcon"
    @confirmed="confirmPendingRoleUpdate()"
    @close="pendingRoleUpdate = undefined"
  >
    <UntrustedRecipientsWarning v-if="pendingRoleUpdate.untrusted">
      <p>{{ t('untrustedRecipientsWarning.untrustedUsers', [pendingRoleUpdate.member.name]) }}</p>
    </UntrustedRecipientsWarning>
  </ConfirmDialog>
  <EditVaultMetadataDialog v-if="editingVaultMetadata && vault" ref="editVaultMetadataDialog" :vault="vault" @close="editingVaultMetadata = false" @updated="refreshVault" />
  <DownloadVaultTemplateDialog v-if="downloadingVaultTemplate && vault && (vaultFormat8 || uvfVault)" ref="downloadVaultTemplateDialog" :vault="vault" :vault-keys="(vaultFormat8 || uvfVault)!" @close="downloadingVaultTemplate = false" />
  <DisplayRecoveryKeyDialog v-if="displayingRecoveryKey && vault && (vaultFormat8 || uvfVault?.recoveryKey.privateKey)" ref="displayRecoveryKeyDialog" :vault="vault" @close="displayingRecoveryKey = false" />
  <ArchiveVaultDialog v-if="archivingVault && vault" ref="archiveVaultDialog" :vault="vault" @close="archivingVault = false" @archived="refreshVault" />
  <RecoverVaultDialog v-if="recoveringVault && vault" ref="recoverVaultDialog" :vault="vault" @close="recoveringVault = false" @recovered="onClaimOwnershipError = undefined; fetchOwnerData()" />
  <GrantEmergencyAccessDialog v-if="grantingEmergencyAccess && vault && (vaultFormat8 || uvfVault) && settings" ref="grantEmergencyAccessDialog" :vault="vault" :vault-keys="(vaultFormat8 || uvfVault)!" :settings="settings" @close="grantingEmergencyAccess = false" @updated="refreshVault" />
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { ArrowPathIcon, EllipsisVerticalIcon, ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { ArchiveBoxIcon, KeyIcon } from '@heroicons/vue/24/outline';
import { PlusSmallIcon } from '@heroicons/vue/24/solid';
import { base64 } from '@scure/base';
import * as R from 'remeda';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import config from '../common/config';
import backend, { AuthorityDto, ConflictError, ForbiddenError, LicenseUserInfoDto, MemberDto, NotFoundError, PaymentRequiredError, SettingsDto, TrustDto, UserDto, VaultDto, VaultRole } from '../common/backend';
import { JWT, JWTHeader } from '../common/jwt';
import { UniversalVaultFormat } from '../common/universalVaultFormat';
import userdata from '../common/userdata';
import { VaultFormat8 } from '../common/vaultFormat8';
import { unwrapVaultKeys } from '../common/vaultKeys';
import wot from '../common/wot';
import ArchiveVaultDialog from './ArchiveVaultDialog.vue';
import ClaimVaultOwnershipDialog from './ClaimVaultOwnershipDialog.vue';
import ConfirmDialog from './ConfirmDialog.vue';
import DisplayRecoveryKeyDialog from './DisplayRecoveryKeyDialog.vue';
import DownloadVaultTemplateDialog from './DownloadVaultTemplateDialog.vue';
import EditVaultMetadataDialog from './EditVaultMetadataDialog.vue';
import FetchError from './FetchError.vue';
import GrantPermissionDialog from './GrantPermissionDialog.vue';
import RecoverVaultDialog from './RecoverVaultDialog.vue';
import SearchInputGroup from './SearchInputGroup.vue';
import TrustDetails from './TrustDetails.vue';
import UntrustedRecipientsWarning from './UntrustedRecipientsWarning.vue';
import GrantEmergencyAccessDialog from './emergencyaccess/GrantEmergencyAccessDialog.vue';

const { t, d, locale } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vaultId: string,
  vaultRole: VaultRole | 'NONE',
  isAdmin: boolean,
}>();

const emit = defineEmits<{
  vaultUpdated: [updatedVault: VaultDto]
  licenseStatusUpdated: [license: LicenseUserInfoDto]
}>();

const onFetchError = ref<Error>();
const allowRetryFetch = computed(() => onFetchError.value && !(onFetchError.value instanceof NotFoundError));  //fetch requests either list something, or query from th vault. In the latter, a 404 indicates the vault does not exists anymore.

const onUpdateVaultMembershipError = ref< { [id: string]: Error } >({});
const onAddUserError = ref<Error>();
const onReactivateError = ref<Error>();
const onClaimOwnershipError = ref<Error>();

const cfg = config.get();
const settings = ref<SettingsDto>();
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
const recoveringVault = ref(false);
const recoverVaultDialog = ref<typeof RecoverVaultDialog>();
const vault = ref<VaultDto>();
const vaultFormat8 = ref<VaultFormat8>();
const uvfVault = ref<UniversalVaultFormat>();
const members = ref<Record<string, MemberDto>>({});
const membersRequiringAccessGrant = ref<(MemberDto & UserDto)[]>([]);
const trusts = ref<TrustDto[]>([]);
const pendingRoleUpdate = ref<{ member: MemberDto, role: VaultRole, untrusted: boolean }>();
const roleUpdateConfirmDialog = ref<typeof ConfirmDialog>();
const claimVaultOwnershipDialog = ref<typeof ClaimVaultOwnershipDialog>();
const claimingVaultOwnership = ref(false);
const me = ref<UserDto>();
const grantingEmergencyAccess = ref(false);
const grantEmergencyAccessDialog = ref<typeof GrantEmergencyAccessDialog>();

const vaultRecoveryRequired = ref<boolean>(false);

const isLegacyVault = computed(() => vault.value?.authPublicKey !== undefined);
const licenseViolated = computed(() => license.value?.isViolated() ?? false);
const canToggleArchive = computed(() => (props.vaultRole === 'OWNER' || props.isAdmin) && !isLegacyVault.value);
const ownsVault = computed(() => vault.value !== undefined && props.vaultRole === 'OWNER' && !isLegacyVault.value);
const canManageVault = computed(() => ownsVault.value && !licenseViolated.value && !vaultRecoveryRequired.value);
const canEditVaultMetadata = computed(() => ownsVault.value && (licenseViolated.value || (!vaultRecoveryRequired.value && !vault.value?.archived)));
const canDownloadVaultTemplate = computed(() => canManageVault.value);
const canUpdatePermissions = computed(() => canManageVault.value && !vault.value?.archived);
const canShowRecoveryKey = computed(() => canManageVault.value && (vaultFormat8.value !== undefined || (!vault.value?.archived && !!uvfVault.value?.recoveryKey.privateKey)));
const membersRequiringAccessGrantNames = computed(() => new Intl.ListFormat(locale.value, { type: 'conjunction' }).format(membersRequiringAccessGrant.value.map(m => m.name)));

defineExpose({
  canEditVaultMetadata,
  canDownloadVaultTemplate,
  showEditVaultMetadataDialog,
  showDownloadVaultTemplateDialog
});

const emergencyKeyShareAuthorities = ref<Record<string, AuthorityDto>>({});

const hasEmergencyKeys = computed(() => Object.keys(vault.value?.emergencyKeyShares ?? {}).length > 0 );

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = undefined;
  try {
    vault.value = await backend.vaults.get(props.vaultId);
    settings.value = await backend.settings.get();

    if (vault.value && Object.keys(vault.value.emergencyKeyShares).length > 0) {
      const authorities = await backend.authorities.listSome(Object.keys(vault.value.emergencyKeyShares));
      emergencyKeyShareAuthorities.value = R.indexBy(authorities, a => a.id);
    }

    me.value = await userdata.me;
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
  if (!vault.value) {
    throw new Error('Vault not initialized.');
  }
  try {
    members.value = R.indexBy(await backend.vaults.getMembers(props.vaultId), m => m.id);
    await refreshTrusts();
    membersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId);
    vaultRecoveryRequired.value = false;
    const vaultKeys = await unwrapVaultKeys(vault.value);
    if (vaultKeys instanceof UniversalVaultFormat) {
      uvfVault.value = vaultKeys;
    } else if (vaultKeys instanceof VaultFormat8) {
      vaultFormat8.value = vaultKeys;
    }
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
const emergencyAccessMembers = computed(() =>
  Object.keys(vault.value?.emergencyKeyShares ?? {})
);
const emergencyAccessAction = computed(() => {
  if (!canUpdatePermissions.value || !cfg.entitlements.emergencyAccessEnabled || !settings.value?.enableEmergencyAccess) {
    return undefined;
  } else if (!hasEmergencyKeys.value) {
    return 'setUp';
  } else if (emergencyAccessMembers.value.length < (vault.value?.requiredEmergencyKeyShares ?? 0)) {
    return 'fix';
  } else {
    return undefined;
  }
});
const hasActionRows = computed(() => canShowRecoveryKey.value || emergencyAccessAction.value !== undefined || canToggleArchive.value);
const hasActions = computed(() => isLegacyVault.value || (vaultRecoveryRequired.value && !licenseViolated.value) || hasActionRows.value);

async function provedOwnership(keys: VaultFormat8, ownerKeyPair: CryptoKeyPair) {
  onClaimOwnershipError.value = undefined;
  if (!me.value) {
    throw new Error('illegal state.');
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
    onClaimOwnershipError.value = error instanceof Error ? error : new Error('Unknown Error');
    return;
  }

  const myPublicKey = await userdata.ecdhPublicKey;
  const vaultKeyJwe = keys.encryptForUser(myPublicKey);
  try {
    await backend.vaults.grantAccess(props.vaultId, { userId: me.value.id, token: await vaultKeyJwe });
  } catch (error) {
    if (error instanceof ConflictError) {
      console.debug('User already member of this vault.');
    } else {
      console.error('Failed to grant access to self.', error);
      onClaimOwnershipError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }

  refreshVault(vault.value);
  await fetchOwnerData();
}

async function reloadMembersRequiringAccessGrant() {
  try {
    membersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId);
  } catch (error) {
    console.error('Getting devices requiring access grant failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function addAuthority(authority: AuthorityDto) {
  onAddUserError.value = undefined;

  try {
    await addAuthorityBackend(authority);
    const addedMember: MemberDto = {
      ...authority,
      vaultRole: 'MEMBER'
    };
    members.value[authority.id] = addedMember;
    membersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId);
  } catch (error) {
    //even if error instanceof NotFoundError, it is not expected from user perspective
    console.error('Adding member failed.', error);
    onAddUserError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function addAuthorityBackend(authority: AuthorityDto) {
  try {
    switch (authority.type) {
      case 'USER':
        await backend.vaults.addUser(props.vaultId, authority.id);
        break;
      case 'GROUP':
        await backend.vaults.addGroup(props.vaultId, authority.id);
        break;
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
  const recoveryKey: Promise<string> = uvfVault.value?.recoveryKey.createRecoveryKey()
    ?? vaultFormat8.value?.createRecoveryKey()
    ?? Promise.reject('No private key material present');
  nextTick(() => displayRecoveryKeyDialog.value?.show(recoveryKey));
}

function showGrantEmergencyAccessDialog() {
  grantingEmergencyAccess.value = true;
  nextTick(() => grantEmergencyAccessDialog.value?.show?.());
}

function showArchiveVaultDialog() {
  archivingVault.value = true;
  nextTick(() => archiveVaultDialog.value?.show());
}

async function reactivateVault() {
  onReactivateError.value = undefined;
  try {
    const vaultDto = await backend.vaults.setArchived(props.vaultId, false);
    refreshVault(vaultDto);
  } catch (error) {
    console.error('Reactivating vault failed.', error);
    onReactivateError.value = error instanceof Error ? error : new Error('Unknown Error');
    return;
  }
  await refreshLicense();
}

function showRecoverVaultDialog() {
  recoveringVault.value = true;
  nextTick(() => recoverVaultDialog.value?.show());
}

function permissionGranted() {
  membersRequiringAccessGrant.value = [];
}

async function refreshTrusts() {
  trusts.value = await backend.trust.listTrusted();
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
  return (await backend.authorities.search(query, true))
    .filter(authority => !members.value[authority.id])
    .sort((a, b) => a.name.localeCompare(b.name));
}

async function requestUpdateMemberRole(member: MemberDto, role: VaultRole) {
  // changing the role of a UVF vault member re-wraps the vault key (and, for owners, the recovery key) to their
  // current public key, so additionally warn if their identity has not been verified:
  const untrusted = !!uvfVault.value && member.type == 'USER' && !!member.ecdhPublicKey && await wot.computeTrustLevel(member, trusts.value) === -1;
  pendingRoleUpdate.value = { member, role, untrusted };
  nextTick(() => roleUpdateConfirmDialog.value?.show());
}

async function confirmPendingRoleUpdate() {
  if (pendingRoleUpdate.value) {
    await updateMemberRole(pendingRoleUpdate.value.member, pendingRoleUpdate.value.role);
  }
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
    const updatedMember = members.value[member.id];
    if (updatedMember) {
      updatedMember.vaultRole = role;
    }
    if (uvfVault.value && member.type == 'USER' && member.ecdhPublicKey) {
      const includeOwnerKeys = role == 'OWNER';
      const updatedAccessToken = await uvfVault.value.encryptForUser(base64.decode(member.ecdhPublicKey) as Uint8Array<ArrayBuffer>, includeOwnerKeys);
      await backend.vaults.grantAccess(props.vaultId, { userId: member.id, token: updatedAccessToken });
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
    delete members.value[memberId];
    if (!licenseViolated.value) {
      membersRequiringAccessGrant.value = await backend.vaults.getUsersRequiringAccessGrant(props.vaultId);
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
