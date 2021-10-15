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
          <h3 class="text-lg font-medium leading-6 text-gray-700">Connect to Keycloak</h3>
          <p class="mt-1 text-sm text-gray-500">
            This information is used during setup. Admin credentials are discarded when done.
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

          <button type="button" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500" @click="auth()">
            Connect
          </button>
        </div>
      </fieldset>
    </div>
  </form>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import axios, { AxiosResponse } from 'axios';

export default defineComponent({
  data: () => ({
    kcUrl: '' as string,
    kcUser: 'admin' as string,
    kcPass: 'admin' as string,
    kcAuthToken: '' as string,
  }),

  mounted() {
    let baseURL = import.meta.env.DEV ? 'http://localhost:9090' : '';
    axios.get(baseURL + '/setup/keycloak-url').then(response => {
      this.kcUrl = response.data;
    }).catch(error => {
      this.kcUrl = 'http://localhost:8080';
    });
  },

  methods: {
    auth() {
      const params = new URLSearchParams();
      params.append('grant_type', 'password');
      params.append('client_id', 'admin-cli');
      params.append('username', this.kcUser);
      params.append('password', this.kcPass);
      axios.post(`${this.kcUrl}/auth/realms/master/protocol/openid-connect/token`, params,
        {
          headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }
      ).then(response => {
        console.log('success: ', response.data);
      }).catch(error => {
        console.error('failed', error);
      });
    }
  }
})
</script>

