<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('close')">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto" @close="open = false">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75 transition-opacity" />
      </TransitionChild>

      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
            <DialogPanel class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
              <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div class="sm:flex sm:items-start">
                  <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-red-100 sm:mx-0 sm:h-10 sm:w-10">
                    <ExclamationTriangleIcon class="h-6 w-6 text-red-600" aria-hidden="true" />
                  </div>
                  <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                    <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                      {{ t('grantPermissionDialog.title') }}
                    </DialogTitle>
                    <div class="mt-2">
                      <p class="text-sm text-gray-500">
                        {{ t('grantPermissionDialog.description') }}
                      </p>
                    </div>
                    <div class="mt-2 h-48 overflow-y-auto">
                      <ul class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
                        <template v-for="member in users.values()" :key="member.id">
                          <li class="py-3 flex flex-col" :class="{ 'bg-yellow-50': isUntrusted(member) }">
                            <div class="flex items-center whitespace-nowrap w-full">
                              <img :src="member.pictureUrl" alt="" class="w-8 h-8 rounded-full" />
                              <div class="ml-4 w-full">
                                <p class="text-sm font-medium text-gray-900">{{ member.name }}</p>
                                <p v-if="isUntrusted(member)" class="text-xs text-yellow-700">{{ t('grantPermissionDialog.warning.unverifiedUser') }}</p>
                              </div>
                              <TrustDetails v-if="member.type === 'USER'" :trusted-user="member" :trusts="trusts" @trust-changed="refreshTrusts()" />
                            </div>
                          </li>
                        </template>
                      </ul>
                    </div>
                    <UntrustedRecipientsWarning v-if="untrustedUsers.length > 0" v-model:confirmed="confirmedUntrusted" require-confirmation class="mt-3">
                      <p>{{ t('untrustedRecipientsWarning.untrustedUsers', [untrustedUsers.map(u => u.name).join(', ')]) }}</p>
                    </UntrustedRecipientsWarning>
                  </div>
                </div>
              </div>
              <form novalidate @submit.prevent="grantAccess">
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" :disabled="untrustedUsers.length > 0 && !confirmedUntrusted" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-xs px-4 py-2 bg-red-600 text-base font-medium text-white hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:hover:bg-red-600 disabled:cursor-not-allowed">
                    {{ t('grantPermissionDialog.submit', [users.length]) }}
                  </button>
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-xs px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                </div>
                <p v-if="onGrantPermissionError" class="text-sm text-red-900 px-4 sm:px-6 text-right bg-red-50">
                  {{ t('common.unexpectedError', [onGrantPermissionError.message]) }}
                </p>
                <p v-if="onGrantPermissionError instanceof ConflictError || onGrantPermissionError instanceof NotFoundError" class="text-sm text-red-900 px-4 sm:px-6 pb-3 text-right bg-red-50">
                  {{ t('common.reload') }}
                </p>
              </form>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/24/outline';
import { base64 } from '@scure/base';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AccessGrant, ConflictError, MemberDto, NotFoundError, TrustDto, UserDto, VaultDto } from '../common/backend';
import { AccessTokenProducing } from '../common/crypto';
import wot from '../common/wot';
import TrustDetails from './TrustDetails.vue';
import UntrustedRecipientsWarning from './UntrustedRecipientsWarning.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const trusts = ref<TrustDto[]>([]);
const trustLevels = ref<Map<string, number>>(new Map());
const confirmedUntrusted = ref(false);
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

onMounted(fetchData);

async function fetchData() {
  await refreshTrusts();
}

async function refreshTrusts() {
  trusts.value = await backend.trust.listTrusted();
  trustLevels.value = await wot.computeTrustLevels(props.users, trusts.value);
}

// only users who will actually receive a key (i.e. have an ecdhPublicKey) are relevant for the warning:
const untrustedUsers = computed(() => props.users.filter(u => u.type === 'USER' && u.ecdhPublicKey && trustLevels.value.get(u.id) === -1));

function isUntrusted(member: MemberDto & UserDto): boolean {
  return member.type === 'USER' && !!member.ecdhPublicKey && trustLevels.value.get(member.id) === -1;
}

function show() {
  open.value = true;
}

async function grantAccess() {
  onGrantPermissionError.value = undefined;
  try {
    if (untrustedUsers.value.length > 0 && !confirmedUntrusted.value) {
      throw new Error('Sharing the vault key with unverified users requires explicit confirmation');
    }
    await giveUsersAccess(props.users);
    emit('permissionGranted');
    open.value = false;
  } catch (error) {
    console.error('Granting access permissions failed.', error);
    onGrantPermissionError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function giveUsersAccess(members: (MemberDto & UserDto)[]) {
  const tokens: AccessGrant[] = [];
  for (const member of members) {
    if (member.ecdhPublicKey) { // some users might not have set up their key pair, so we can't share secrets with them yet
      const publicKey = base64.decode(member.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
      const includeOwnerKeys = member.vaultRole === 'OWNER';
      const jwe = await props.vaultKeys.encryptForUser(publicKey, includeOwnerKeys);
      tokens.push({ userId: member.id, token: jwe });
    }
  }
  await backend.vaults.grantAccess(props.vault.id, ...tokens);
}

</script>
