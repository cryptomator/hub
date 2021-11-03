<template>
  <!-- Notification for successfully creation of a realm -->
  <!-- TODO remove if staying with @dafcoe/vue-notification plugin
  <div id="realmSuccessfulCreatedNotification" v-if="realmSuccessfulCreatedNotification" class="rounded-md bg-green-50 p-4">
    <div class="flex">
      <div class="flex-shrink-0">
        <CheckCircleIcon class="h-5 w-5 text-green-400" aria-hidden="true" />
      </div>
      <div class="ml-3">
        <p class="text-sm font-medium text-green-800">
          Successfully created realm
        </p>
      </div>
      <div class="ml-auto pl-3">
        <div class="-mx-1.5 -my-1.5">
          <button type="button" class="inline-flex bg-green-50 rounded-md p-1.5 text-green-500 hover:bg-green-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-green-50 focus:ring-green-600" @click="closeNotifications()">
            <span class="sr-only">Dismiss</span>
            <XIcon class="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
      </div>
    </div>
  </div>
  -->
  <!-- Notification for error during creation of a realm -->
  <!-- TODO remove if staying with @dafcoe/vue-notification plugin
  <div id="realmErrorNotification" v-if="realmErrorNotification" class="rounded-md bg-red-50 p-4">
    <div class="flex">
      <div class="flex-shrink-0">
        <XCircleIcon class="h-5 w-5 text-red-400" aria-hidden="true" />
      </div>
      <div class="ml-3">
        <p class="text-sm font-medium text-red-800">
          Error while creating realm. It might already exist.
        </p>
      </div>
      <div class="ml-auto pl-3">
        <div class="-mx-1.5 -my-1.5">
          <button type="button" class="inline-flex bg-red-50 rounded-md p-1.5 text-red-500 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-red-50 focus:ring-red-600" @click="closeNotifications()">
            <span class="sr-only">Dismiss</span>
            <XIcon class="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
      </div>
    </div>
  </div>
  -->

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
              <input id="hub-url" v-model="hubUrl" type="text" class="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md" @change="updateRealmJson" />
            </div>
          </div>

          <div class="sm:col-span-3">
            <label for="hub-user" class="block text-sm font-medium text-gray-700">
              Initial Hub Admin User *
            </label>
            <div class="mt-1">
              <input id="hub-user" v-model="hubUser" type="text" class="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md" @change="updateRealmJson" />
            </div>
          </div>

          <div class="sm:col-span-3">
            <label for="hub-pass" class="block text-sm font-medium text-gray-700">
              Initial Hub Admin Password *
            </label>
            <div class="mt-1">
              <input id="hub-pass" v-model="hubPass" type="password" class="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md" @change="updateRealmJson" />
            </div>
          </div>

          <div>
            <label for="hub-realm-json" class="block text-sm font-medium text-gray-700">
              Realm Config File
            </label>
            <div class="mt-1">
              <textarea id="hub-realm-json" v-model="hubRealmCfg" rows="10" class="resize-y shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border border-gray-300 rounded-md" readonly></textarea>
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
              <input id="kc-url" v-model="kcUrl" type="text" class="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md" />
            </div>
          </div>

          <div class="sm:col-span-3">
            <label for="kc-user" class="block text-sm font-medium text-gray-700">
              Admin User
            </label>
            <div class="mt-1">
              <input id="kc-user" v-model="kcUser" type="text" class="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md" />
            </div>
          </div>

          <div class="sm:col-span-3">
            <label for="kc-pass" class="block text-sm font-medium text-gray-700">
              Admin Password
            </label>
            <div class="mt-1">
              <input id="kc-pass" v-model="kcPass" type="password" class="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md" />
            </div>
          </div>

          <button type="button" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500" @click="createRealm()">
            Upload
          </button>
        </div>
      </fieldset>
    </div>
  </form>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import axios from 'axios';
import config from '../common/config';
import createRealmJson from '../common/realm';
import { CheckCircleIcon, XCircleIcon, XIcon } from '@heroicons/vue/solid';
import { useNotificationStore } from '@dafcoe/vue-notification'
const { setNotification } = useNotificationStore()


let backendBaseURL = import.meta.env.DEV ? 'http://localhost:9090' : '';

export default defineComponent({
  components: {
	  CheckCircleIcon,
	  XCircleIcon,
	  XIcon,
  },
  data: () => ({
    hubUrl: window.location.protocol + '//' + window.location.hostname + ':' + window.location.port as string,
    hubUser: 'owner' as string,
    hubPass: '' as string,
    hubRealmCfg: '' as string,
    kcUrl: config.get().keycloakUrl as string,
    kcUser: 'admin' as string,
    kcPass: 'admin' as string,
    kcAuthToken: '' as string,
    //realmSuccessfulCreatedNotification: false as boolean,
    //realmErrorNotification: false as boolean,
  }),

  mounted() {
    this.updateRealmJson();
  },

  methods: {
    updateRealmJson() {
      this.hubRealmCfg = createRealmJson(this.hubUrl, this.hubUser, this.hubPass);
    },

    createRealm() {
      const params = new URLSearchParams();
      params.append('kcUrl', this.kcUrl);
      params.append('user', this.kcUser);
      params.append('password', this.kcPass);
      params.append('realmCfg', this.hubRealmCfg);

      axios.post(`${backendBaseURL}/setup/create-realm`, params).then(response => {
		//this.realmSuccessfulCreatedNotification = true;
		const realmSuccessfulCreatedNotification = {
		 "message": "Successfully created realm.",
		 "type": "success",
		 "showIcon": true,
		 "dismiss": {
		  "manually": true,
		  "automatically": true
		 },
		 "showDurationProgress": true,
		}
		setNotification(realmSuccessfulCreatedNotification)
        config.reload();
      }).catch(error => {
        console.error('failed to create realm', error);
		//this.realmErrorNotification = true;
		let realmErrorNotification = {
				"message": "error",
				"type": "alert",
				"showIcon": true,
				"dismiss": {
					"manually": true,
					"automatically": false
				},
		}
		if (error.response.status === 404){
			realmErrorNotification.message = "Error while creating realm. URL can't be found.";
		} else if (error.response.status === 409){
			realmErrorNotification.message = "Error while creating realm. It might already exist.";
		}
		setNotification(realmErrorNotification)
	  });
    },

	/*closeNotifications(){
	  this.realmSuccessfulCreatedNotification = false;
	  this.realmErrorNotification = false;
	}*/
  }
})
</script>

