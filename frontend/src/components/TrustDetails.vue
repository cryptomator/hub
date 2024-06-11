<template>
  <div v-if="state === State.Loading">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
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
          <button v-if="trustLevel === -1 && trustedUser.ecdsaPublicKey" @click="sign()" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary  text-base font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm">
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
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { TrustDto, UserDto } from '../common/backend';
import { UserKeys, asPublicKey } from '../common/crypto';
import { JWT, JWTHeader } from '../common/jwt';
import userdata from '../common/userdata';
import FetchError from './FetchError.vue';

enum State {
  Loading,
  ShowTrust
}

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  trustedUser: UserDto
}>();

const state = ref(State.Loading);
const onFetchError = ref<Error | null>();
const trust = ref<TrustDto>();
const trustLevel = ref<number>(-1);

onMounted(fetchData);

async function fetchData() {
  try {
    trust.value = await backend.trust.get(props.trustedUser.id);
    trustLevel.value = await computeTrustLevel(trust.value);
    state.value = State.ShowTrust;
  } catch (error) {
    console.error('Fetching data failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function computeTrustLevel(trust?: TrustDto) {
  const me = await userdata.me;
  if (me.id === props.trustedUser.id) {
    return 0; // Self
  } else if (trust && props.trustedUser.ecdsaPublicKey) {
    try {
      await verify(trust.signatureChain, props.trustedUser.ecdsaPublicKey);
      return trust.signatureChain.length;
    } catch (error) {
      console.error('WoT signature verification failed.', error);
      return -1; // Unverified
    }
  } else {
    return -1; // Unverified
  }
}

async function sign() {
  if (!props.trustedUser.ecdsaPublicKey) {
    throw new Error('No public key to sign');
  }
  const me = await userdata.me;
  const userKeys = await userdata.decryptUserKeysWithBrowser();
  const signature = await JWT.build({
    alg: 'ES384',
    typ: 'JWT',
    b64: true,
    iss: me.id,
    sub: props.trustedUser.id,
    iat: Math.floor(Date.now() / 1000)
  }, props.trustedUser.ecdsaPublicKey, userKeys.ecdsaKeyPair.privateKey);
  await backend.trust.trustUser(props.trustedUser.id, signature);
  trust.value = await backend.trust.get(props.trustedUser.id);


  // TODO: remove debug output (for verifification onbly)
  console.log('Signature: ', signature);
  console.log('Signer\'s Public Key: ', await crypto.subtle.exportKey('jwk', userKeys.ecdsaKeyPair.publicKey));
}

async function verify(signatureChain: string[], allegedSignedKey: string) {
  let signerPublicKey = await userdata.decryptUserKeysWithBrowser().then(keys => keys.ecdsaKeyPair.publicKey);
  await verifyRescursive(signatureChain, signerPublicKey, allegedSignedKey);
}

/**
 * Recursively verifies a chain of signatures, where each signature signs the public key of the next signature.
 * @param signatureChain The chain of signatures to verify
 * @param signerPublicKey A trusted public key to verify the first signature in the chain
 * @param allegedSignedKey The public key that should be signed by the last signature in the chain
 * @throws Error if the signature chain is invalid
 */
async function verifyRescursive(signatureChain: string[], signerPublicKey: CryptoKey, allegedSignedKey: string) {
  // get first element of signature chain:
  const [signature, ...remainingChain] = signatureChain;
  const [_, signedPublicKey] = await JWT.parse(signature, signerPublicKey) as [JWTHeader, string];
  if (remainingChain.length === 0) {
    // last element in chain should match signed public key
    if (signedPublicKey !== allegedSignedKey) {
      throw new Error('Alleged public key does not match signed public key');
    }
  } else {
    // otherwise, the payload is an intermediate public key used to sign the next element
    const nextTrustedPublicKey = await asPublicKey(base64.parse(signedPublicKey), UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES);
    await verifyRescursive(remainingChain, nextTrustedPublicKey, allegedSignedKey);
  }
}

</script>