<template>
  <div v-if="state === State.Loading">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="checkTrust"/>
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
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { TrustDto, UserDto } from '../common/backend';
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
const trustLevel = computed(() => props.me.id === props.userId ? 0 : trust.value?.signatureChain.length || -1);

onMounted(checkTrust);

async function checkTrust() {
  try {
    trust.value = await backend.trust.get(props.userId);
    state.value = State.ShowTrust;
  } catch (error) {
    console.error('Fetching data failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

const sign = () => {
  console.log('Sign');
};

</script>