<template>
  <div class="pb-5 border-b border-gray-200">
    <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
      {{ t('userSettings.title') }}
    </h2>
  </div>

  <div class="space-y-6 mt-5">
    <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6">
      <div class="md:grid md:grid-cols-3 md:gap-6">
        <div class="md:col-span-1">
          <h3 class="text-lg font-medium leading-6 text-gray-900">{{ t('userSettings.general.title') }}</h3>
        </div>
        <div class="mt-5 md:mt-0 md:col-span-2">
          <div class="grid grid-cols-6 gap-6">
            <div class="col-span-6 sm:col-span-3">
              <Listbox v-model="$i18n.locale" as="div">
                <ListboxLabel class="block text-sm font-medium text-gray-700">{{ t('userSettings.general.language.title') }}</ListboxLabel>
                <div class="relative mt-1">
                  <ListboxButton class="relative w-full cursor-default rounded-md border border-gray-300 bg-white py-2 pl-3 pr-10 text-left shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary text-sm">
                    <span class="block truncate">{{ t(`locale.${$i18n.locale}`) }}</span>
                    <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                      <ChevronUpDownIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
                    </span>
                  </ListboxButton>
                  <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
                    <ListboxOptions class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none text-sm">
                      <ListboxOption v-for="locale in Locale" :key="locale" v-slot="{ active, selected }" as="template" :value="locale">
                        <li :class="[active ? 'text-white bg-primary' : 'text-gray-900', 'relative cursor-default select-none py-2 pl-3 pr-9']">
                          <span :class="[selected ? 'font-semibold' : 'font-normal', 'block truncate']">{{ t(`locale.${locale}`) }}</span>
                          <span v-if="selected" :class="[active ? 'text-white' : 'text-primary', 'absolute inset-y-0 right-0 flex items-center pr-4']">
                            <CheckIcon class="h-5 w-5" aria-hidden="true" />
                          </span>
                        </li>
                      </ListboxOption>
                    </ListboxOptions>
                  </transition>
                </div>
              </Listbox>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6">
      <div class="md:grid md:grid-cols-3 md:gap-6">
        <div class="md:col-span-1">
          <h3 class="text-lg font-medium leading-6 text-gray-900">
            {{ t('userSettings.serverInfo.title') }}
          </h3>
          <p class="mt-1 text-sm text-gray-500">
            {{ t('userSettings.serverInfo.description') }}
          </p>
        </div>
        <div class="mt-5 md:mt-0 md:col-span-2">
          <div v-if="version == null">
            <div v-if="onFetchError == null">
              {{ t('common.loading') }}
            </div>
            <div v-else>
              <FetchError :error="onFetchError" :retry="fetchData"/>
            </div>
          </div>
          <div v-else class="grid grid-cols-6 gap-6">
            <div class="col-span-6 sm:col-span-3">
              <label for="hubVersion" class="block text-sm font-medium text-gray-700">{{ t('userSettings.serverInfo.hubVersion.title') }}</label>
              <input id="hubVersion" v-model="version.hubVersion" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
            </div>
            <div class="col-span-6 sm:col-span-3">
              <label for="keycloakVersion" class="block text-sm font-medium text-gray-700">{{ t('userSettings.serverInfo.keycloakVersion.title') }}</label>
              <input id="keycloakVersion" v-model="version.keycloakVersion" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- TODO move to overlay dialog -->
    <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6">
      <div class="md:grid md:grid-cols-3 md:gap-6">
        <div class="md:col-span-1">
          <h3 class="text-lg font-medium leading-6 text-gray-900">User Key</h3>
          <p class="mt-1 text-sm text-gray-500">This is your key...</p>
        </div>
        <div class="mt-5 md:mt-0 md:col-span-2">
          <div v-if="user?.publicKey == null">
            no key yet...
            <button @click="createUserKey()">CREATE</button>
          </div>
          <div v-else class="grid grid-cols-6 gap-6">
            you have a key: <span>{{ user.publicKey }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Listbox, ListboxButton, ListboxLabel, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { CheckIcon, ChevronUpDownIcon } from '@heroicons/vue/24/solid';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto, VersionDto } from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import { Locale } from '../i18n';
import FetchError from './FetchError.vue';

const { t } = useI18n({ useScope: 'global' });

const version = ref<VersionDto>();
const user = ref<UserDto>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    let versionInstalled = backend.version.get();
    version.value = await versionInstalled;
    user.value = await backend.users.me();
  } catch (error) {
    console.error('Retrieving version information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function createUserKey() {
  if (!user.value) {
    return;
  }
  const userKeys = await UserKeys.create();
  const recoveryCode = crypto.randomUUID(); // TODO something else?
  const archive = await userKeys.export(recoveryCode);
  const me = user.value;
  me.publicKey = archive.publicKey;
  me.privateKey = archive.encryptedPrivateKey;
  me.salt = archive.salt;
  me.iterations = archive.iterations;

  const browserKeys = await BrowserKeys.create(); // or .load()
  await browserKeys.store();

  const jwe = await userKeys.encryptForDevice(browserKeys.keyPair.publicKey);
  backend.devices.addDevice({
    id: crypto.randomUUID(),
    name: navigator.userAgent, // TODO something
    publicKey: await browserKeys.encodedPublicKey(),
    userKeyJwe: jwe,
    creationTime: new Date()
  });
  backend.users.putMyKeyPair(me);
}
</script>
