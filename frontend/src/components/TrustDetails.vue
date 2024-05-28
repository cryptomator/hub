<template>
  <div v-if="state === State.Loading">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="loadTrust"/>
    </div>
  </div>
  <Popover v-else-if="state === State.ShowTrust" as="div" class="relative inline-block text-left">
    <PopoverButton class="inline-flex items-center bg-gray-50 ring-1 ring-inset ring-gray-500/10 mx-1 p-1 rounded-full focus:outline-none focus:ring-primary">
      <ShieldExclamationIcon v-if="trustLevel === -1" class="h-4 w-4 text-red-500" aria-label="Unverified" />
      <ShieldCheckIcon v-else class="h-4 w-4 text-primary" aria-label="Verified" />
    </PopoverButton>

    <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
        <PopoverPanel class="absolute right-0 z-10 mt-2 origin-top-right rounded-md bg-white p-4 shadow-2xl ring-1 ring-black ring-opacity-5 focus:outline-none">
          <p class="text-sm">Trust Level {{ trustLevel }}</p>
          <button v-if="trustLevel === -1" @click="sign()" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary  text-base font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm">
            TODO sign
          </button>
        </PopoverPanel>
    </transition>
  </Popover>
</template>

<script setup lang="ts">
import { Popover, PopoverButton, PopoverPanel } from '@headlessui/vue';
import { ShieldCheckIcon, ShieldExclamationIcon } from '@heroicons/vue/20/solid';
import { base64 } from 'rfc4648';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { TrustDto, UserDto } from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import { JWT } from '../common/jwt';
import FetchError from './FetchError.vue';

enum State {
  Loading,
  ShowTrust
}

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  me: UserDto,
  userId: string
}>();

const state = ref(State.Loading);
const onFetchError = ref<Error | null>();
const trust = ref<TrustDto | undefined>();
const trustLevel = computed(computeTrustLevel);

onMounted(loadTrust);

async function loadTrust() {
  try {
    trust.value = await backend.trust.get(props.userId);
    state.value = State.ShowTrust;
  } catch (error) {
    console.error('Fetching data failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function computeTrustLevel() {
  if (props.me.id === props.userId) {
    return 0; // Self
  } else if (trust.value) {
    return trust.value.signatureChain.length;
  } else {
    return -1; // Unverified
  }
}

async function sign() {
  // TODO: begin dedup and cache
  if (props.me.publicKey == null || props.me.setupCode == null) {
    throw new Error('User not initialized.');
  }
  const browserKeys = await BrowserKeys.load(props.me.id);
  if (browserKeys == null) {
    throw new Error('Browser keys not found.');
  }
  const browserId = await browserKeys.id();
  const myDevice = props.me.devices.find(d => d.id == browserId);
  if (myDevice == null) {
    throw new Error('Device not initialized.');
  }
  const userKeys = await UserKeys.decryptOnBrowser(myDevice.userPrivateKey, browserKeys.keyPair.privateKey, base64.parse(props.me.publicKey));
  // TODO: end dedup
  const signature = JWT.build({
    alg: 'ES384',
    typ: 'JWT',
    b64: true,
    iss: props.me.id,
    sub: props.userId,
    iat: Math.floor(Date.now() / 1000)
  }, props.userId, userKeys.keyPair.privateKey);
  // backend.trust.trustUser(props.userId, signature);
  console.log('Sign', signature);
};

</script>