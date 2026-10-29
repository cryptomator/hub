<template>
  <HubDialog v-model:open="open" :title="t('grantPermissionDialog.title')" :busy="processing" :error="grantPermissionErrorMessage" :initial-focus="cancelButton" @submit="grantAccess()" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('grantPermissionDialog.description', [vault.name, users.length]) }}
    </DialogDescription>
    <ul class="mt-4 max-h-64 divide-y divide-gray-200 overflow-y-auto border-y border-gray-200">
      <li v-for="member in users" :key="member.id" class="flex items-center gap-3 py-3">
        <img :src="member.pictureUrl" alt="" class="h-8 w-8 shrink-0 rounded-full" />
        <div class="min-w-0 grow">
          <p class="truncate text-sm font-medium text-gray-900">{{ member.name }}</p>
          <p v-if="isUntrusted(member)" class="flex items-center gap-1 text-xs text-yellow-700">
            <ExclamationTriangleIcon class="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
            {{ t('grantPermissionDialog.warning.unverifiedUser') }}
            <template v-if="member.ecdsaPublicKey">
              ·
              <button type="button" :disabled="processing" class="rounded-sm font-medium underline hover:text-yellow-900 focus:outline-hidden focus:ring-2 focus:ring-primary disabled:cursor-not-allowed disabled:opacity-50" @click="verify(member)">
                {{ t('trustDetails.showSignDialogBtn') }}<span class="sr-only"> {{ member.name }}</span>
              </button>
            </template>
          </p>
        </div>
        <span class="inline-flex shrink-0 items-center rounded-md bg-gray-50 px-2 py-1 text-xs font-medium text-gray-600 ring-1 ring-inset ring-gray-500/10">
          {{ member.vaultRole === 'OWNER' ? t('vaultDetails.sharedWith.badge.owner') : t('common.member') }}
        </span>
      </li>
    </ul>
    <UntrustedRecipientsWarning v-if="untrustedUsers.length > 0" v-model:confirmed="confirmedUntrusted" require-confirmation class="mt-4">
      <p>{{ t('untrustedRecipientsWarning.untrustedUsers', [untrustedUsers.map(u => u.name).join(', ')]) }}</p>
    </UntrustedRecipientsWarning>
    <SignUserKeysDialog v-if="verifyingUser" ref="signUserKeysDialog" :user="verifyingUser" @close="verifyingUser = undefined" @signed="refreshTrusts()" />

    <template #footer>
      <button ref="cancelButton" type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing || !trustLevels || (untrustedUsers.length > 0 && !confirmedUntrusted)" :class="primaryButton">
        {{ t('grantPermissionDialog.submit', [users.length]) }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { base64 } from '@scure/base';
import { computed, nextTick, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AccessGrant, ConflictError, MemberDto, NotFoundError, TrustDto, UserDto, VaultDto } from '../common/backend';
import userdata from '../common/userdata';
import { AccessTokenProducing } from '../common/crypto';
import wot from '../common/wot';
import { primaryButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';
import SignUserKeysDialog from './SignUserKeysDialog.vue';
import UntrustedRecipientsWarning from './UntrustedRecipientsWarning.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const cancelButton = ref<HTMLButtonElement>();
const signUserKeysDialog = ref<InstanceType<typeof SignUserKeysDialog>>();
const verifyingUser = ref<MemberDto & UserDto>();
const trustLevels = ref<Map<string, number>>(); // undefined until loaded for the current users
const confirmedUntrusted = ref(false);
const onLoadTrustLevelsError = ref<Error>();
const onGrantPermissionError = ref<Error>();

const props = defineProps<{
  vault: VaultDto
  users: (MemberDto & UserDto)[]
  vaultKeys: AccessTokenProducing
}>();

const emit = defineEmits<{
  close: []
  permissionGranted: []
}>();

defineExpose({
  show
});

watch(() => props.users, () => {
  // trust levels and the confirmation only apply to the recipients they were made for:
  trustLevels.value = undefined;
  confirmedUntrusted.value = false;
  refreshTrusts();
}, { immediate: true });

async function refreshTrusts() {
  const users = props.users;
  onLoadTrustLevelsError.value = undefined;
  try {
    const trusts = await backend.trust.listTrusted();
    const levels = await wot.computeTrustLevels(users, trusts);
    if (users === props.users) {
      trustLevels.value = levels;
    }
  } catch (error) {
    console.error('Loading trust levels failed.', error);
    if (users === props.users) {
      onLoadTrustLevelsError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
}

const untrustedUsers = computed(() => props.users.filter(isUntrusted));

// recipients missing from the computed trust levels count as unverified:
function isUntrusted(member: MemberDto & UserDto): boolean {
  return !!member.ecdhPublicKey && !!trustLevels.value && (trustLevels.value.get(member.id) ?? -1) === -1;
}

const grantPermissionErrorMessage = computed(() => {
  const error = onGrantPermissionError.value ?? onLoadTrustLevelsError.value;
  if (!error) {
    return undefined;
  }
  const message = t('common.unexpectedError', [error.message]);
  return error instanceof ConflictError || error instanceof NotFoundError ? `${message}. ${t('common.reload')}` : message;
});

function show() {
  open.value = true;
}

function verify(member: MemberDto & UserDto) {
  verifyingUser.value = member;
  nextTick(() => signUserKeysDialog.value?.show());
}

async function grantAccess() {
  if (processing.value || !open.value) {
    return;
  }
  onGrantPermissionError.value = undefined;
  try {
    processing.value = true;
    if (!trustLevels.value) {
      throw new Error('Trust levels of the recipients are not known yet');
    }
    if (untrustedUsers.value.length > 0 && !confirmedUntrusted.value) {
      throw new Error('Sharing the vault key with unverified users requires explicit confirmation');
    }
    await giveUsersAccess(props.users);
    emit('permissionGranted');
    open.value = false;
  } catch (error) {
    console.error('Granting access permissions failed.', error);
    onGrantPermissionError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}

async function giveUsersAccess(members: (MemberDto & UserDto)[]) {
  const me = await userdata.me;
  const tokens: AccessGrant[] = [];
  for (const member of members) {
    if (member.ecdhPublicKey) { // some users might not have set up their key pair, so we can't share secrets with them yet
      // never wrap "to myself" using a key from the (server-supplied) member list, only using my verified key:
      const publicKey = member.id === me.id ? await userdata.ecdhPublicKey : base64.decode(member.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
      const includeOwnerKeys = member.vaultRole === 'OWNER';
      const jwe = await props.vaultKeys.encryptForUser(publicKey, includeOwnerKeys);
      tokens.push({ userId: member.id, token: jwe });
    }
  }
  await backend.vaults.grantAccess(props.vault.id, ...tokens);
}

</script>
