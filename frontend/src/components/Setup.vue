<template>
  <form class="" action="#">
    <div class="md:flex md:items-center md:justify-between">
      <div class="flex-1 min-w-0">
        <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
          Setup Cryptomator Hub
        </h2>
      </div>
    </div>

    <div class="my-8 space-y-8 divide-y divide-gray-200">
      <fieldset class="bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6 md:grid md:grid-cols-3 md:gap-6">
        <div class="block md:col-span-1">
          <h3 class="text-lg font-medium leading-6 text-gray-700">Configure Realm</h3>
          <p class="mt-1 text-sm text-gray-500">
            These values are used to create a realm configuration file.
            This configuration is then used to configure Keycloak to provide <abbr title="Identity and Access Management">IAM</abbr> services for Cryptomator Hub.
          </p>
        </div>
        <div class="mt-5 md:mt-0 md:col-span-2 space-y-6">
          <div class="sm:col-span-3">
            <label for="hub-url" class="block text-sm font-medium text-gray-700">
              Cryptomator Hub URL *
            </label>
            <div class="mt-1">
              <input id="hub-url" v-model="hubUrl" type="text" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" @change="updateRealmJson" />
            </div>
          </div>

          <div class="sm:col-span-3">
            <label for="hub-user" class="block text-sm font-medium text-gray-700">
              Initial Hub Admin User *
            </label>
            <div class="mt-1">
              <input id="hub-user" v-model="hubUser" type="text" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" @change="updateRealmJson" />
            </div>
          </div>

          <div class="sm:col-span-3">
            <label for="hub-pass" class="block text-sm font-medium text-gray-700">
              Initial Hub Admin Password *
            </label>
            <div class="mt-1">
              <input id="hub-pass" v-model="hubPass" type="password" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" @change="updateRealmJson" />
            </div>
          </div>

          <div>
            <label for="hub-realm-json" class="block text-sm font-medium text-gray-700">
              Realm Config File
            </label>
            <div class="mt-1">
              <textarea id="hub-realm-json" v-model="hubRealmCfg" rows="10" class="resize-y shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border border-gray-300 rounded-md" readonly></textarea>
            </div>
            <p class="mt-2 text-sm text-gray-500">
              This JSON file will be used to configure a new Keycloak realm.
            </p>
          </div>
        </div>
      </fieldset>
    </div>

    <div v-if="hubUrl.length>0 && hubUser.length>0 && hubPass.length>0 && hubRealmCfg.length>0" class="my-8 space-y-8 divide-y divide-gray-200">
      <fieldset class="bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6 md:grid md:grid-cols-3 md:gap-6">
        <div class="block md:col-span-1">
          <h3 class="text-lg font-medium leading-6 text-gray-700">Upload Realm to Keycloak</h3>
          <p class="mt-1 text-sm text-gray-500">
            Use the Keycloak admin user to create a new realm. The credentials are discarded immediately after upload.
          </p>
        </div>
        <div class="mt-5 md:mt-0 md:col-span-2 space-y-6">
          <div class="sm:col-span-3">
            <label for="kc-url" class="block text-sm font-medium text-gray-700">
              Keycloak URL
            </label>
            <div class="mt-1">
              <input id="kc-url" v-model="kcUrl" type="text" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" />
            </div>
          </div>

          <div class="sm:col-span-3">
            <label for="kc-user" class="block text-sm font-medium text-gray-700">
              Admin User
            </label>
            <div class="mt-1">
              <input id="kc-user" v-model="kcUser" type="text" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" />
            </div>
          </div>

          <div class="sm:col-span-3">
            <label for="kc-pass" class="block text-sm font-medium text-gray-700">
              Admin Password
            </label>
            <div class="mt-1">
              <input id="kc-pass" v-model="kcPass" type="password" class="shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" />
            </div>
          </div>

          <button type="button" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="createRealm()">
            Upload
          </button>
        </div>
      </fieldset>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';
import config from '../common/config';
import createRealmJson from '../common/realm';
import { useNotificationStore } from '@dafcoe/vue-notification';
const { setNotification } = useNotificationStore();

let backendBaseURL = import.meta.env.DEV ? 'http://localhost:9090' : '';

const hubUrl = ref(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port);
const hubUser = ref('owner');
const hubPass = ref('');
const hubRealmCfg = ref('');
const kcUrl = ref(config.get().keycloakUrl);
const kcUser = ref('admin');
const kcPass = ref('admin');

onMounted(() => {
  updateRealmJson();
});

function updateRealmJson() {
  hubRealmCfg.value = createRealmJson(hubUrl.value, hubUser.value, hubPass.value);
}

function createRealm() {
  const params = new URLSearchParams();
  params.append('kcUrl', kcUrl.value);
  params.append('user', kcUser.value);
  params.append('password', kcPass.value);
  params.append('realmCfg', hubRealmCfg.value);

  axios.post(`${backendBaseURL}/setup/create-realm`, params).then(response => {
    const realmSuccessfulCreatedNotification = {
      'message': 'Successfully created realm.',
      'type': 'success',
      'showIcon': true,
      'dismiss': {
        'manually': true,
        'automatically': true
      },
      'showDurationProgress': true,
    };
    setNotification(realmSuccessfulCreatedNotification);
    config.reload();
  }).catch(error => {
    console.error('failed to create realm', error);
    let realmErrorNotification = {
      'message': 'error',
      'type': 'alert',
      'showIcon': true,
      'dismiss': {
        'manually': true,
        'automatically': false
      },
    };
    if (error.response.status === 404){
      realmErrorNotification.message = 'Error while creating realm. URL can\'t be found.';
    } else if (error.response.status === 409){
      realmErrorNotification.message = 'Error while creating realm. It might already exist.';
    }
    setNotification(realmErrorNotification);
  });
}
</script>
