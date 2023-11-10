<template>
  <div v-if="me == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else>
    <h1 class="sr-only">{{ t('userProfile.title') }}</h1>
    <div class="grid grid-cols-1 items-start gap-4 lg:grid-cols-4 lg:gap-8">
      <div class="grid grid-cols-1 gap-6">
        <div class="text-center">
          <img class="h-32 w-32 rounded-full bg-white mx-auto" :src="me.pictureUrl" alt="" />
          <p class="mt-3 font-semibold">{{ me.name }}</p>
          <p v-if="me.email != null" class="text-sm text-gray-500">{{ me.email }}</p>
        </div>
        <div class="flex flex-col gap-2">
          <button type="button" class="inline-flex items-center justify-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openKeycloakUserAccount()">
            <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('userProfile.actions.manageAccount') }}
          </button>
          <Listbox v-model="$i18n.locale" as="div">
            <div class="relative">
              <ListboxButton class="relative w-full inline-flex items-center justify-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">
                <LanguageIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
                {{ t('userProfile.actions.changeLanguage') }}
                <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                  <ChevronUpDownIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
                </span>
              </ListboxButton>
              <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
                <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none text-sm">
                  <ListboxOption v-for="locale in Locale" :key="locale" v-slot="{ active, selected }" class="relative cursor-default select-none py-2 pl-3 pr-9 ui-not-active:text-gray-900 ui-active:text-white ui-active:bg-primary" :value="locale">
                    <span :class="[selected ? 'font-semibold' : 'font-normal', 'block truncate']">{{ t(`locale.${locale}`) }}</span>
                    <span v-if="selected" :class="[active ? 'text-white' : 'text-primary', 'absolute inset-y-0 right-0 flex items-center pr-4']">
                      <CheckIcon class="h-5 w-5" aria-hidden="true" />
                    </span>
                  </ListboxOption>
                </ListboxOptions>
              </transition>
            </div>
          </Listbox>
        </div>
        <div v-if="version != null" class="text-center">
          <p class="text-xs text-gray-500">
            Hub {{ version.hubVersion }} • Keycloak {{ version.keycloakVersion }}
          </p>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-8 lg:col-span-3">
        <ManageSetupCode />
        <DeviceList />
        <UserkeyFingerprint :user-public-key="me.publicKey"/>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { ArrowTopRightOnSquareIcon, CheckIcon, ChevronUpDownIcon, LanguageIcon } from '@heroicons/vue/24/solid';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto, VersionDto } from '../common/backend';

import config from '../common/config';
import { Locale } from '../i18n';
import DeviceList from './DeviceList.vue';
import FetchError from './FetchError.vue';
import ManageSetupCode from './ManageSetupCode.vue';
import UserkeyFingerprint from './UserkeyFingerprint.vue';

const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const keycloakUserAccountURL = ref<string>();
const version = ref<VersionDto>();
const onFetchError = ref<Error | null>();

onMounted(async () => {
  let cfg = config.get();
  keycloakUserAccountURL.value = `${cfg.keycloakUrl}/realms/${cfg.keycloakRealm}/account`;
  await fetchData();
});

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me(true);
    version.value = await backend.version.get();
  } catch (error) {
    console.error('Retrieving user information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function openKeycloakUserAccount() {
  window.open(keycloakUserAccountURL.value, '_blank');
}

</script>
