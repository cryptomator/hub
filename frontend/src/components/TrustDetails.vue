<template>
  <Popover as="div" class="relative inline-block text-left overflow-visible">
    <PopoverButton class="inline-flex items-center bg-gray-50 ring-1 ring-inset ring-gray-500/10 mx-1 p-1 rounded-full focus:outline-none focus:ring-primary">
      <ShieldExclamationIcon v-if="trustLevel === -1" class="h-4 w-4 text-red-500" aria-label="Unverified" />
      <ShieldCheckIcon v-else class="h-4 w-4 text-primary" aria-label="Verified" />
    </PopoverButton>

    <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
        <PopoverPanel class="absolute right-0 z-10 mt-2 origin-top-right rounded-md bg-white p-4 shadow-2xl ring-1 ring-black ring-opacity-5 focus:outline-none">
          <p v-if="trustLevel === -1" class="text-sm mb-2">{{ t('trustDetails.trustLevel.untrusted') }}</p>
          <p v-else-if="trustLevel === 0" class="text-sm mb-2">{{ t('trustDetails.trustLevel', [ n(1, 'percent')]) }}</p>
          <p v-else-if="trustLevel > 0" class="text-sm mb-2">{{ t('trustDetails.trustLevel', [ n(1 / trustLevel, 'percent')]) }}</p>
          <button v-if="trustLevel !== 0 && trustedUser.ecdhPublicKey && trustedUser.ecdsaPublicKey" @click="showSignUserKeysDialog()" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:w-auto sm:text-sm">
            {{ t('trustDetails.showSignDialogBtn') }}
          </button>
          <p v-if="!trustedUser.ecdhPublicKey || !trustedUser.ecdsaPublicKey" class="text-sm mb-2">{{ t('trustDetails.userNotSetUp') }}</p>
        </PopoverPanel>
    </transition>
  </Popover>

  <SignUserKeysDialog v-if="signingUserKeys" ref="signUserKeysDialog" :user="trustedUser" :trust="trust" @close="signingUserKeys = false" @signed="successfullySigned" />
</template>

<script setup lang="ts">
import { Popover, PopoverButton, PopoverPanel } from '@headlessui/vue';
import { ShieldCheckIcon, ShieldExclamationIcon } from '@heroicons/vue/20/solid';
import { computed, nextTick, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { TrustDto, UserDto } from '../common/backend';
import userdata from '../common/userdata';
import wot from '../common/wot';
import SignUserKeysDialog from './SignUserKeysDialog.vue';

const { t, n } = useI18n({ useScope: 'global' });

const props = defineProps<{
  trustedUser: UserDto,
  trusts: TrustDto[]
}>();

const emit = defineEmits<{
  trustChanged: [TrustDto]
}>();

const signUserKeysDialog = ref<typeof SignUserKeysDialog>();
const signingUserKeys = ref<boolean>(false);
const trust = computed(() => props.trusts.find(trust => trust.trustedUserId === props.trustedUser.id));
const trustLevel = ref<number>(-1);

watch(trust, computeTrustLevel, { immediate: true });

async function computeTrustLevel(trust?: TrustDto) {
  const me = await userdata.me;
  if (me.id === props.trustedUser.id) {
    trustLevel.value = 0; // Self
  } else if (trust && props.trustedUser.ecdhPublicKey && props.trustedUser.ecdsaPublicKey) {
    try {
      await wot.verify(trust.signatureChain, { ecdhPublicKey: props.trustedUser.ecdhPublicKey, ecdsaPublicKey: props.trustedUser.ecdsaPublicKey });
      trustLevel.value = trust.signatureChain.length;
    } catch (error) {
      console.error('WoT signature verification failed.', error);
      trustLevel.value = -1; // Unverified
    }
  } else {
    trustLevel.value = -1; // Unverified
  }
}

function showSignUserKeysDialog() {
  signingUserKeys.value = true;
  nextTick(() => signUserKeysDialog.value?.show());
}

function successfullySigned(newTrust: TrustDto) {
  emit('trustChanged', newTrust);
}

</script>