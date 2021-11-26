<template>
  <div v-if="me == null">
    Loading…
  </div>

  <div v-else-if="me.devices.length == 0">
    You have no registered devices.
  </div>

  <div v-else>
    <div class="pb-5 border-b border-gray-200">
      <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
        Devices ({{ me.devices.length }})
      </h2>
    </div>

    <div class="mt-5 flex flex-col">
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Device Name
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Number of Shared Vaults
                  </th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Added
                  </th>
                  <th scope="col" class="relative px-6 py-3">
                    <span class="sr-only">Remove</span>
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="device in me.devices" :key="device.id">
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {{ device.name }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <!-- TODO: actual type -->
                    <span class="inline-flex items-center">
                      <DesktopComputerIcon class="mr-1 h-5 w-5" aria-hidden="true" />
                      Computer
                    </span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ device.accessTo.length }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <!-- TODO: actual added date -->
                    June 8, 2020
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <a role="button" tabindex="0" class="text-red-600 hover:text-red-900" @click="removeDevice(device)">Remove</a>
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

<script setup lang="ts">
import { DesktopComputerIcon } from '@heroicons/vue/solid';
import { onMounted, ref } from 'vue';
import backend, { DeviceDto, UserDto } from '../common/backend';

const me = ref<UserDto>();

onMounted(async () => {
  try {
    me.value = await backend.users.me(true, true);
  } catch (error) {
    // TODO: error handling
    console.error('Retrieving device list failed.', error);
  }
});

function removeDevice(device: DeviceDto) {
  // TODO: add remove device to backend
  console.log('User tried to remove device.', device);
}
</script>
