<template>
  <div class="flex flex-col">
    <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
      <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
        <div class="shadow overflow-hidden border-b border-gray-200 sm:rounded-lg">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                  Owner
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                  Device-name
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                  Id
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                  Edit
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(user, userIdx) in users" :key="user.id" :class="userIdx % 2 === 0 ? 'bg-white' : 'bg-gray-50'">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {{ user.name }}
                </td>
                <td v-for="(device, userIdx) in user.devices" class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ device.name }}
                </td>
                <td v-for="(device, userIdx) in user.devices" class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ device.id }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <a class="text-indigo-600 hover:text-indigo-900" href="#">
                    Edit
                  </a>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="me != null">
            I am owner of these vaults:
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                    Owner
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                    Device-Name
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                    Vault-Name
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                    Id
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" scope="col">
                    Edit
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(device, deviceIdx) in me.devices" :key="device.id">
                  <td v-for="(vault, vaultIdx) in device.accessTo" :key="vault.id" class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {{ me.name }}
                  </td>
                  <td v-for="(vault, vaultIdx) in device.accessTo" :key="vault.id" class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ device.name }}
                  </td>
                  <td v-for="(vault, vaultIdx) in device.accessTo" :key="vault.id" class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ vault.name }}
                  </td>
                  <td v-for="(vault, vaultIdx) in device.accessTo" :key="vault.id" class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <a class="text-indigo-600 hover:text-indigo-900" href="vaults/{{ vault.id }}">
                      not working: {{ vault.id }}
                    </a>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <a class="text-indigo-600 hover:text-indigo-900" href="#">
                      Edit
                    </a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">

import {defineComponent} from "vue";
import {useI18n} from "vue-i18n";
import backend, {UserDto, VaultDto} from "../common/backend";

export default defineComponent({
	name: 'VaultList',
	props: {
		vaultId: {
			type: String,
			default: null
		}
	},
	setup() {
		const {t} = useI18n({
			useScope: 'global'
		})
		return {t}
	},
	data: () => ({
		Error,
		//errorCode: Error.None as Error,
		me: null as UserDto,
		users: [] as UserDto[],
		vaults: [] as VaultDto[],
		//devices: [] as DeviceDto[]
	}),

	mounted() {
		backend.users.meIncludingDevices().then(me => {
			this.me = me
		}),
		backend.users.listAllUsersIncludingDevices().then(users => {
			this.users = users
		})
	}
})

</script>

<style scoped>

</style>