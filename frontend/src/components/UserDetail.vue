<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>
  <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start ">
    <section class="lg:col-start-1 grid gap-6">
      <!-- User Info -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('user.detail.info') }}
          </h3>
          <button type="button" class="bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showUserEdit">
            {{ t('user.detail.edit') }}
          </button>
        </div>
        <div class="px-6 py-6">
          <div class="flex flex-col items-center text-center mb-6">
            <span class="sr-only">{{ t('user.edit.profileImage') }}</span>
            <img :src="user.userPicture" :alt="t('user.edit.profileImage')" class="h-40 w-40 rounded-full object-contain bg-tertiary-2 border border-gray-300" />
            <h2 class="text-xl font-semibold text-gray-900 mt-4">
              <template v-if="user.firstName || user.lastName">
                {{ user.firstName }} {{ user.lastName }}
              </template>
              <template v-else>
                {{ user.username }}
              </template>
            </h2>
            <p v-if="user.firstName || user.lastName" class="text-sm text-gray-500 mt-1">
              {{ user.username }}
            </p>
          </div>
          <dl class="divide-y divide-gray-100">
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('user.detail.email') }}</dt>
              <dd class="text-sm text-gray-900 font-medium">{{ user.email }}</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('user.detail.roles') }}</dt>
              <dd class="flex flex-wrap justify-end gap-2">
                <span v-for="role in sortedRoles" :key="role" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20 capitalize">
                  {{ role }}
                </span>
                <span v-if="!sortedRoles.length" class="text-sm text-gray-500">{{ t('common.none') }}</span>
              </dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('user.detail.createdOn') }}</dt>
              <dd class="text-sm text-gray-900 font-medium">{{ d(user.creationTime, 'long') }}</dd>
            </div>
          </dl>
        </div>
      </section>
      <!-- Devices -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('user.detail.devices') }}
          </h3>
        </div>

        <!-- Search bar -->
        <div class="px-6 py-3 border-b border-gray-200">
          <input id="deviceSearch" v-model="deviceQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
        </div>
        
        <!-- Device Table -->
        <div>
          <table class="w-full table-fixed divide-y divide-gray-200" aria-describedby="deviceListTitle">
            <thead class="bg-gray-50">
              <tr>
                <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  {{ t('common.device') }}
                </th>
                <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  {{ t('legacyDeviceList.added') }}
                </th>
                <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  <span class="inline-flex items-center gap-1">
                    {{ t('legacyDeviceList.lastAccess') }}
                    <div class="relative group" :title="t('legacyDeviceList.lastAccess.toolTip')">
                      <QuestionMarkCircleIcon class="h-4 w-4 text-gray-400"/>
                    </div>
                  </span>
                </th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <template v-for="device in paginatedDevices" :key="device.id">
                <tr>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    <div class="flex items-center gap-2">
                      <span v-if="device.type == 'TABLET'" :title="'Browser'">
                        <WindowIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                      </span>
                      <span v-else-if="device.type == 'DESKTOP'" :title="'Desktop'">
                        <ComputerDesktopIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                      </span>
                      <span v-else-if="device.type == 'MOBILE'" :title="'Mobile'">
                        <DevicePhoneMobileIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                      </span>
                      <span class="truncate max-w-xs" :title="device.name">
                        {{ device.name }}
                      </span>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ new Date(device.creationTime).toISOString().slice(0, 16).replace('T', ' ') }}
                  </td>
                  <td class="h-17 px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div v-if="device.lastAccessTime">
                      {{ new Date(device.lastAccessTime).toISOString().slice(0, 16).replace('T', ' ') }}
                    </div>
                    <div v-if="device.lastIpAddress" class="text-xs text-gray-400">
                      {{ device.lastIpAddress }}
                    </div>
                  </td>
                </tr>
              </template>
              <tr v-if="!filteredDevices.length">
                <td colspan="5" class="py-4 px-6 text-sm text-gray-500 text-center">
                  {{ t(deviceQuery ? 'common.nothingFound' : 'common.none') }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="showPaginationDevice" class="bg-gray-50 border-t border-gray-200">
          <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
            <div class="hidden sm:block">
              <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
                <span class="font-medium">{{ paginationBeginDevice }}</span>
                <span class="font-medium">{{ paginationEndDevice }}</span>
              </i18n-t>
            </div>
            <div class="flex flex-1 justify-between sm:justify-end space-x-3">
              <button v-if="currentPageDevice > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPageDevice">
                {{ t('common.previous') }}
              </button>
              <button v-if="hasNextPageDevice" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPageDevice">
                {{ t('common.next') }}
              </button>
            </div>
          </nav>
        </div>
      </section>      
      <!-- Legacy Devices -->
      <section v-if="user.legacyDevices.length != 0" class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="flex items-center bg-gray-50 px-6 py-4 border-b border-gray-200 space-x-2">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('legacyDeviceList.title') }}
          </h3>
          <div class="relative group" :title="t('user.detail.legacyDeviceList.info')">
            <QuestionMarkCircleIcon class="h-4 w-4 text-gray-400"/>
          </div>
        </div>

        <!-- Search bar -->
        <div class="px-6 py-3 border-b border-gray-200">
          <input id="legacyDeviceSearch" v-model="legacyDeviceQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
        </div>

        <div>
          <table class="w-full table-fixed divide-y divide-gray-200" aria-describedby="deviceListTitle">
            <thead class="bg-gray-50">
              <tr>
                <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  {{ t('common.device') }}
                </th>
                <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  {{ t('legacyDeviceList.added') }}
                </th>
                <th scope="col" class="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  <span class="inline-flex items-center gap-1">
                    {{ t('legacyDeviceList.lastAccess') }}
                    <div class="relative group" :title="t('legacyDeviceList.lastAccess.toolTip')">
                      <QuestionMarkCircleIcon class="h-4 w-4 text-gray-400"/>
                    </div>
                  </span>
                </th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <template v-for="device in paginatedLegacyDevices" :key="device.id">
                <tr>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    <div class="flex items-center gap-2">
                      <span v-if="device.type == 'TABLET'" :title="'Browser'">
                        <WindowIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                      </span>
                      <span v-else-if="device.type == 'DESKTOP'" :title="'Desktop'">
                        <ComputerDesktopIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                      </span>
                      <span v-else-if="device.type == 'MOBILE'" :title="'Mobile'">
                        <DevicePhoneMobileIcon class="h-5 w-5 text-gray-500" aria-hidden="true" />
                      </span>
                      <span class="truncate max-w-xs" :title="device.name">
                        {{ device.name }}
                      </span>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ new Date(device.creationTime).toISOString().slice(0, 16).replace('T', ' ') }}
                  </td>
                  <td class="h-17 px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div v-if="device.lastAccessTime">
                      {{ new Date(device.lastAccessTime).toISOString().slice(0, 16).replace('T', ' ') }}
                    </div>
                    <div v-if="device.lastIpAddress" class="text-xs text-gray-400">
                      {{ device.lastIpAddress }}
                    </div>
                  </td>
                </tr>
              </template>
              <tr v-if="!filteredDevices.length">
                <td colspan="5" class="py-4 px-6 text-sm text-gray-500 text-center">
                  {{ t(deviceQuery ? 'common.nothingFound' : 'common.none') }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="showPaginationLegacyDevice" class="bg-gray-50 border-t border-gray-200">
          <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
            <div class="hidden sm:block">
              <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
                <span class="font-medium">{{ paginationBeginLegacyDevice }}</span>
                <span class="font-medium">{{ paginationEndLegacyDevice }}</span>
              </i18n-t>
            </div>
            <div class="flex flex-1 justify-between sm:justify-end space-x-3">
              <button v-if="currentPageLegacyDevice > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPageLegacyDevice">
                {{ t('common.previous') }}
              </button>
              <button v-if="hasNextPageLegacyDevice" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPageLegacyDevice">
                {{ t('common.next') }}
              </button>
            </div>
          </nav>
        </div>
      </section>
    </section>
    <section class="lg:col-start-2 grid gap-6">
      <!-- Groups -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <div class="flex items-baseline gap-1">
            <h3 id="groupsTitle" class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
              {{ t('user.detail.groups') }}
            </h3>
            <span class="text-xs text-gray-500">{{ user.groups.length }}</span>
          </div>
          <button type="button" class="bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openAddGroupDialog">
            {{ t('user.detail.groups.join') }}
          </button>
        </div>

        <!-- Search bar -->
        <div class="px-6 py-3 border-b border-gray-200">
          <input id="groupSearch" v-model="groupQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
        </div>

        <!-- Group table -->
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-300" aria-describedby="groupsTitle">
            <tbody class="divide-y divide-gray-200 bg-white">
              <tr v-for="group in paginatedGroups" :key="group.id">
                <td class="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 flex items-center gap-3 sm:pl-6">
                  <img :src="group.userPicture" class="w-8 h-8 rounded-full object-cover border border-gray-300" />
                  <span class="truncate">{{ group.name }}</span>
                </td>

                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <a tabindex="0" class="cursor-pointer text-red-600 hover:text-red-900" :title="t('common.leave')" @click="showDeleteDialog(group)">{{ t('common.leave') }}</a>
                </td>
              </tr>
              <tr v-if="!filteredGroups.length">
                <td colspan="2" class="py-4 px-4 text-sm text-center text-gray-500">
                  {{ t(groupQuery ? 'group.members.search.empty' : 'common.none') }}
                </td>
              </tr>
            </tbody>

            <!-- GROUP pagination -->
            <tfoot v-if="showPaginationGroup" class="bg-gray-50">
              <tr>
                <td colspan="2">
                  <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
                    <div class="hidden sm:block">
                      <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
                        <span class="font-medium">{{ paginationBeginGroup }}</span>
                        <span class="font-medium">{{ paginationEndGroup }}</span>
                      </i18n-t>
                    </div>
                    <div class="flex flex-1 justify-end space-x-3">
                      <button v-if="currentPageGroup > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPageGroup">
                        {{ t('common.previous') }}
                      </button>
                      <button v-if="hasNextPageGroup" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPageGroup">
                        {{ t('common.next') }}
                      </button>
                    </div>
                  </nav>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>
      <!-- Vaults -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('nav.vaults') }}
          </h3>
        </div>

        <!-- Search bar -->
        <div class="px-6 py-3 border-b border-gray-200">
          <input id="vaultSearch" v-model="vaultQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
        </div>

        <!-- Vault list -->
        <div class="py-0">
          <ul class="divide-y divide-gray-200 bg-white">
            <li v-for="vault in paginatedVaults" :key="vault.id" class="py-2 px-6">
              <div class="text-sm font-medium text-gray-900 truncate">{{ vault.name }}</div>
              <div v-if="vault.description" class="text-sm text-gray-500 truncate">{{ vault.description }}</div>
            </li>
            <li v-if="!filteredVaults.length" class="text-sm text-gray-500">
              {{ t(vaultQuery ? 'common.nothingFound' : 'common.none') }}
            </li>
          </ul>
        </div>

        <!-- Pagination -->
        <div v-if="showPaginationVault" class="bg-gray-50 border-t border-gray-200">
          <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
            <div class="hidden sm:block">
              <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
                <span class="font-medium">{{ paginationBeginVault }}</span>
                <span class="font-medium">{{ paginationEndVault }}</span>
              </i18n-t>
            </div>
            <div class="flex flex-1 justify-between sm:justify-end space-x-3">
              <button v-if="currentPageVault > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPageVault">
                {{ t('common.previous') }}
              </button>
              <button v-if="hasNextPageVault" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPageVault">
                {{ t('common.next') }}
              </button>
            </div>
          </nav>
        </div>
      </section>
    </section>
  </div>

  <!-- Dialogs -->
  <UserAddGroupDialog ref="addGroupDialog" :groups="user.groups" @saved="onGroupsSaved" />
  <UserGroupRemoveDialog ref="deleteGroupMemberDialog" :group="deletingGroup" @close="deletingGroup = null" @delete="onGroupRemoved"/>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import UserAddGroupDialog from './UserAddGroupDialog.vue';
import { GroupDto } from '../common/backend';
import UserGroupRemoveDialog from './UserGroupRemoveDialog.vue';
import { ComputerDesktopIcon, QuestionMarkCircleIcon, DevicePhoneMobileIcon, WindowIcon } from '@heroicons/vue/24/solid';

const deletingGroup = ref<Group | null>(null);
const deleteGroupMemberDialog = ref<InstanceType<typeof UserGroupRemoveDialog> | null>(null);

interface Group {
  id: string;
  name: string;
  userPicture?: string;
}

interface Vault {
  id: string;
  name: string;
  description?: string;
}

interface Device {
  id: string;
  name: string;
  type: 'DESKTOP' | 'MOBILE' | 'TABLET';
  creationTime: string;
  lastAccessTime?: string;
  lastIpAddress?: string;
}

interface DetailUser {
  firstName?: string;
  lastName?: string;
  username: string;
  roles: string[];
  password: string;
  email: string;
  userPicture?: string;
  creationTime: string;
  groups: Group[];
  vaults: Vault[];
  devices: Device[];
  legacyDevices: Device[];
}

const props = defineProps<{ id: string }>();
const { t, d } = useI18n({ useScope: 'global' });
const router = useRouter();

const user = ref<DetailUser>({
  firstName: 'Edgar',
  lastName: 'Frank',
  username: 'edgar.frank',
  roles: ['create-vault', 'admin'],
  password: 'password123',
  email: 'edgar.frank@example.com',
  userPicture: 'https://i.pravatar.cc/150?u=alex',
  //userPicture: 'https://cryptomator.org/img/logo.svg',
  //userPicture: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iI0Y3RjdGNyIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiM1MTUxNTEiIGQ9Ik0yOS4yIDEyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTU0LjIgMTIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNTQuMiA4Ny41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME0yOS4yIDg3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTQuMiAzNy41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME03OS4yIDM3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTc5LjIgNjIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNC4yIDYyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwIi8+PHBhdGggZmlsbD0iI2E0OGIyYSIgZD0iTTI1IDBMMjUgMjVMMTIuNSAyNVpNMTAwIDI1TDc1IDI1TDc1IDEyLjVaTTc1IDEwMEw3NSA3NUw4Ny41IDc1Wk0wIDc1TDI1IDc1TDI1IDg3LjVaTTUwIDM3LjVMNTAgNTBMMzcuNSA1MFpNNjIuNSA1MEw1MCA1MEw1MCAzNy41Wk01MCA2Mi41TDUwIDUwTDYyLjUgNTBaTTM3LjUgNTBMNTAgNTBMNTAgNjIuNVoiLz48L3N2Zz4=',        
  creationTime: '2023-05-01T10:00:00Z',
  groups: [
    { id: 'g1', name: 'Admin', userPicture: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNmMWY5ZmIiIGQ9Ik0yOS4yIDEyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTU0LjIgMTIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNTQuMiA4Ny41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME0yOS4yIDg3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTQuMiAzNy41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME03OS4yIDM3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTc5LjIgNjIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNC4yIDYyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwIi8+PHBhdGggZmlsbD0iI2NlZWNmMiIgZD0iTTI1IDI1TDAgMjVMMCAxMi41Wk03NSAyNUw3NSAwTDg3LjUgMFpNNzUgNzVMMTAwIDc1TDEwMCA4Ny41Wk0yNSA3NUwyNSAxMDBMMTIuNSAxMDBaIi8+PHBhdGggZmlsbD0iI2NhY2FjYSIgZD0iTTI1IDI1TDUwIDI1TDUwIDI5TDM5LjUgNTBMMjUgNTBaTTc1IDI1TDc1IDUwTDcxIDUwTDUwIDM5LjVMNTAgMjVaTTc1IDc1TDUwIDc1TDUwIDcxTDYwLjUgNTBMNzUgNTBaTTI1IDc1TDI1IDUwTDI5IDUwTDUwIDYwLjVMNTAgNzVaIi8+PC9zdmc+' },
    { id: 'g2', name: 'Support', userPicture: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNhYmRmZTkiIGQ9Ik01MCAxMi41TDM3LjUgMjVMMjUgMTIuNUwzNy41IDBaTTYyLjUgMjVMNTAgMTIuNUw2Mi41IDBMNzUgMTIuNVpNNTAgODcuNUw2Mi41IDc1TDc1IDg3LjVMNjIuNSAxMDBaTTM3LjUgNzVMNTAgODcuNUwzNy41IDEwMEwyNSA4Ny41Wk0yNSAzNy41TDEyLjUgNTBMMCAzNy41TDEyLjUgMjVaTTg3LjUgNTBMNzUgMzcuNUw4Ny41IDI1TDEwMCAzNy41Wk03NSA2Mi41TDg3LjUgNTBMMTAwIDYyLjVMODcuNSA3NVpNMTIuNSA1MEwyNSA2Mi41TDEyLjUgNzVMMCA2Mi41WiIvPjxwYXRoIGZpbGw9IiNmNmY2ZjYiIGQ9Ik0wIDI1TDAgMEwyNSAwWk03NSAwTDEwMCAwTDEwMCAyNVpNMTAwIDc1TDEwMCAxMDBMNzUgMTAwWk0yNSAxMDBMMCAxMDBMMCA3NVoiLz48cGF0aCBmaWxsPSIjY2VlY2YyIiBkPSJNMzEgMzFMNDggMzFMNDggNDhMMzEgNDhaTTY5IDMxTDY5IDQ4TDUyIDQ4TDUyIDMxWk02OSA2OUw1MiA2OUw1MiA1Mkw2OSA1MlpNMzEgNjlMMzEgNTJMNDggNTJMNDggNjlaIi8+PC9zdmc+' },
    { id: 'g3', name: 'User', userPicture: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNmNmY2ZjYiIGQ9Ik0yNSAyNUwyNSAwTDM3LjUgMFpNNTAgMEw3NSAwTDc1IDEyLjVaTTc1IDc1TDc1IDEwMEw2Mi41IDEwMFpNNTAgMTAwTDI1IDEwMEwyNSA4Ny41Wk0wIDUwTDAgMjVMMTIuNSAyNVpNNzUgMjVMMTAwIDI1TDEwMCAzNy41Wk0xMDAgNTBMMTAwIDc1TDg3LjUgNzVaTTI1IDc1TDAgNzVMMCA2Mi41WiIvPjxwYXRoIGZpbGw9IiNjZWVjZjIiIGQ9Ik0wIDI1TDAgMEwyNSAwWk03NSAwTDEwMCAwTDEwMCAyNVpNMTAwIDc1TDEwMCAxMDBMNzUgMTAwWk0yNSAxMDBMMCAxMDBMMCA3NVpNMjUgMjVMNTAgMjVMNTAgNDIuNUwzNSAzNUw0Mi41IDUwTDI1IDUwWk03NSAyNUw3NSA1MEw1Ny41IDUwTDY1IDM1TDUwIDQyLjVMNTAgMjVaTTc1IDc1TDUwIDc1TDUwIDU3LjVMNjUgNjVMNTcuNSA1MEw3NSA1MFpNMjUgNzVMMjUgNTBMNDIuNSA1MEwzNSA2NUw1MCA1Ny41TDUwIDc1WiIvPjwvc3ZnPg==' },
    { id: 'g4', name: 'Guest', userPicture: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNjZWVjZjIiIGQ9Ik0yOS4yIDEyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTU0LjIgMTIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNTQuMiA4Ny41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME0yOS4yIDg3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTQuMiAzNy41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME03OS4yIDM3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTc5LjIgNjIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNC4yIDYyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwIi8+PHBhdGggZmlsbD0iI2Y2ZjZmNiIgZD0iTTAgMEwyNSAwTDI1IDI1Wk0xMDAgMEwxMDAgMjVMNzUgMjVaTTEwMCAxMDBMNzUgMTAwTDc1IDc1Wk0wIDEwMEwwIDc1TDI1IDc1WiIvPjxwYXRoIGZpbGw9IiNjYWNhY2EiIGQ9Ik0yNSAyNUw1MCAyNUw1MCAyOUwzOS41IDUwTDI1IDUwWk03NSAyNUw3NSA1MEw3MSA1MEw1MCAzOS41TDUwIDI1Wk03NSA3NUw1MCA3NUw1MCA3MUw2MC41IDUwTDc1IDUwWk0yNSA3NUwyNSA1MEwyOSA1MEw1MCA2MC41TDUwIDc1WiIvPjwvc3ZnPg==' },
  ],
  vaults: [
    { id: 'v1', name: 'Travel', description: 'Reiseunterlagen und Pläne' },
    { id: 'v2', name: 'Tax', description: 'Steuerdokumente' },
    { id: 'v3', name: 'HR', description: 'Personalunterlagen und Onboarding' },
    { id: 'v4', name: 'Finances', description: 'Finanzberichte und Budgetplanung' },
    { id: 'v5', name: 'Products', description: 'Produkt-Spezifikationen und Roadmaps' },
    { id: 'v6', name: 'Orga', description: 'Organisationsrichtlinien und -prozesse' },
    { id: 'v7', name: 'Sales', description: 'Vertriebsunterlagen und Angebote' },
    { id: 'v8', name: 'Marketing', description: 'Kampagnenpläne und Werbematerial' },
    { id: 'v9', name: 'Legal', description: 'Rechtsdokumente und Verträge' },
    { id: 'v10', name: 'Support', description: 'Support-Tickets und FAQs' },
    { id: 'v11', name: 'Engineering', description: 'Technische Spezifikationen und Architektur' },
    { id: 'v12', name: 'R&D', description: 'Forschungsprojekte und Prototypen' },
    { id: 'v13', name: 'IT', description: 'IT-Infrastruktur und Zugriffsverwaltung' },
    { id: 'v14', name: 'Compliance', description: 'Compliance-Berichte und Audits' },
    { id: 'v15', name: 'Security', description: 'Sicherheitsrichtlinien und Vorfälle' },
    { id: 'v16', name: 'Contracts', description: 'Vertragsvorlagen und Abschlüsse' },
    { id: 'v17', name: 'Meetings', description: 'Protokolle und Präsentationen' },
    { id: 'v18', name: 'Training', description: 'Sch Schulungsunterlagen und Workshops' },
    { id: 'v19', name: 'Customer Data', description: 'Kundendaten und Profilinformationen' },
    { id: 'v20', name: 'Beta Tests', description: 'Ergebnisse von Betatest-Programmen' },
    { id: 'v21', name: 'Archives', description: 'Archivierte Dokumente und historische Daten' },
    { id: 'v22', name: 'Legacy', description: 'Altsystem-Dokumentation und Migration' },
  ],
  devices: [
    { id: 'd1', name: 'MacBook Pro M3 2023 - Work', type: 'DESKTOP', creationTime: '2023-02-10T09:30:00Z', lastAccessTime: '2024-11-01T15:12:00Z', lastIpAddress: '192.168.178.23' },
    { id: 'd2', name: 'iPhone 13', type: 'MOBILE', creationTime: '2022-12-01T11:00:00Z' },
    { id: 'd3', name: 'iPad Air', type: 'TABLET', creationTime: '2023-06-15T14:00:00Z', lastAccessTime: '2025-03-10T12:10:00Z', lastIpAddress: '192.168.178.88' },
    { id: 'd4', name: 'Office Desktop', type: 'DESKTOP', creationTime: '2023-10-01T09:15:00Z', lastAccessTime: '2025-05-10T17:00:00Z', lastIpAddress: '10.0.0.101' },
  ],
  legacyDevices: [
    { id: 'd1', name: 'MacBook 2012', type: 'DESKTOP', creationTime: '2023-02-10T09:30:00Z', lastAccessTime: '2024-11-01T15:12:00Z', lastIpAddress: '192.168.178.23' },
    { id: 'd2', name: 'iPhone 11', type: 'MOBILE', creationTime: '2021-10-01T11:00:00Z' },
  ]
});

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

const loading = ref<boolean>(true);

function showDeleteDialog(g: Group) {
  deletingGroup.value = g;
  nextTick(() => {
    deleteGroupMemberDialog.value?.show();
  });
}

function onGroupRemoved(removed: GroupDto) {
  user.value.groups = user.value.groups.filter(g => g.id !== removed.id);
  deletingGroup.value = null;
}

onMounted(async () => {
  try {
    await new Promise((r) => setTimeout(r, 300));
    // TODO: fetch real data here
  } finally {
    loading.value = false;
  }
});

// ---------------------------------------------------------------------------
// Sorted roles
// ---------------------------------------------------------------------------
const sortedRoles = computed(() => [...user.value.roles ?? []].sort((a, b) => a.localeCompare(b, 'de', { sensitivity: 'base' })));

// ---------------------------------------------------------------------------
// DEVICES – search & pagination
// ---------------------------------------------------------------------------

// Filter und Pagination
const deviceQuery = ref('');
const filteredDevices = computed(() =>
  user.value.devices.filter((device) =>
    device.name.toLowerCase().includes(deviceQuery.value.toLowerCase())
  )
);

const pageSize = 10;
const currentPageDevice = ref(0);

const paginatedDevices = computed(() =>
  filteredDevices.value.slice(
    currentPageDevice.value * pageSize,
    (currentPageDevice.value + 1) * pageSize
  )
);

const showPaginationDevice = computed(() => filteredDevices.value.length > pageSize);
const hasNextPageDevice = computed(() =>
  (currentPageDevice.value + 1) * pageSize < filteredDevices.value.length
);
const paginationBeginDevice = computed(() => currentPageDevice.value * pageSize + 1);
const paginationEndDevice = computed(() =>
  Math.min((currentPageDevice.value + 1) * pageSize, filteredDevices.value.length)
);

function showPreviousPageDevice() {
  if (currentPageDevice.value > 0) currentPageDevice.value--;
}

function showNextPageDevice() {
  if (hasNextPageDevice.value) currentPageDevice.value++;
}

watch(() => [filteredDevices.value.length, deviceQuery.value], () => (currentPageDevice.value = 0));

// ---------------------------------------------------------------------------
// LEGACY DEVICES – search & pagination
// ---------------------------------------------------------------------------

// Filter und Pagination
const legacyDeviceQuery = ref('');
const filteredLegacyDevices = computed(() =>
  user.value.legacyDevices.filter((device) =>
    device.name.toLowerCase().includes(legacyDeviceQuery.value.toLowerCase())
  )
);

const currentPageLegacyDevice = ref(0);

const paginatedLegacyDevices = computed(() =>
  filteredLegacyDevices.value.slice(
    currentPageLegacyDevice.value * pageSize,
    (currentPageLegacyDevice.value + 1) * pageSize
  )
);

const showPaginationLegacyDevice = computed(() => filteredLegacyDevices.value.length > pageSize);
const hasNextPageLegacyDevice = computed(() =>
  (currentPageLegacyDevice.value + 1) * pageSize < filteredLegacyDevices.value.length
);
const paginationBeginLegacyDevice = computed(() => currentPageLegacyDevice.value * pageSize + 1);
const paginationEndLegacyDevice = computed(() =>
  Math.min((currentPageLegacyDevice.value + 1) * pageSize, filteredLegacyDevices.value.length)
);

function showPreviousPageLegacyDevice() {
  if (currentPageLegacyDevice.value > 0) currentPageLegacyDevice.value--;
}

function showNextPageLegacyDevice() {
  if (hasNextPageLegacyDevice.value) currentPageLegacyDevice.value++;
}

watch(() => [filteredLegacyDevices.value.length, legacyDeviceQuery.value], () => (currentPageLegacyDevice.value = 0));

// Vaults – search & pagination
const pageSizeVault = ref(10);
const currentPageVault = ref(0);
const vaultQuery = ref('');

const filteredVaults = computed(() => {
  const q = vaultQuery.value.trim().toLowerCase();
  return user.value.vaults
    .filter(v => !q || v.name.toLowerCase().includes(q) || (v.description && v.description.toLowerCase().includes(q)))
    .sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
});

const showPaginationVault = computed(() => filteredVaults.value.length > pageSizeVault.value);

const paginatedVaults = computed(() =>
  filteredVaults.value.slice(currentPageVault.value * pageSizeVault.value, (currentPageVault.value + 1) * pageSizeVault.value)
);

const hasNextPageVault = computed(() => (currentPageVault.value + 1) * pageSizeVault.value < filteredVaults.value.length);

const paginationBeginVault = computed(() => (filteredVaults.value.length ? currentPageVault.value * pageSizeVault.value + 1 : 0));
const paginationEndVault = computed(() => Math.min((currentPageVault.value + 1) * pageSizeVault.value, filteredVaults.value.length));

function showNextPageVault() {
  if (hasNextPageVault.value) currentPageVault.value++;
}
function showPreviousPageVault() {
  if (currentPageVault.value > 0) currentPageVault.value--;
}

watch(() => [filteredVaults.value.length, vaultQuery.value], () => (currentPageVault.value = 0));

// ---------------------------------------------------------------------------
// GROUPS – search & pagination (member-list style)
// ---------------------------------------------------------------------------
const pageSizeGroup = ref(10);
const currentPageGroup = ref(0);
const groupQuery = ref('');

const filteredGroups = computed(() => {
  const q = groupQuery.value.trim().toLowerCase();
  return (user.value.groups ?? [])
    .filter((g) => !q || g.name.toLowerCase().includes(q))
    .sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
});

const showPaginationGroup = computed(() => filteredGroups.value.length > pageSizeGroup.value);

const paginatedGroups = computed(() =>
  filteredGroups.value.slice(currentPageGroup.value * pageSizeGroup.value, (currentPageGroup.value + 1) * pageSizeGroup.value)
);

const hasNextPageGroup = computed(() => (currentPageGroup.value + 1) * pageSizeGroup.value < filteredGroups.value.length);

const paginationBeginGroup = computed(() => (filteredGroups.value.length ? currentPageGroup.value * pageSizeGroup.value + 1 : 0));
const paginationEndGroup = computed(() => Math.min((currentPageGroup.value + 1) * pageSizeGroup.value, filteredGroups.value.length));

function showNextPageGroup() {
  if (hasNextPageGroup.value) currentPageGroup.value++;
}
function showPreviousPageGroup() {
  if (currentPageGroup.value > 0) currentPageGroup.value--;
}

watch(() => [filteredGroups.value.length, groupQuery.value], () => (currentPageGroup.value = 0));

// ---------------------------------------------------------------------------
// GROUP dialog actions
// ---------------------------------------------------------------------------
const addGroupDialog = ref<InstanceType<typeof UserAddGroupDialog> | null>(null);

function openAddGroupDialog() {
  addGroupDialog.value?.show();
}

function onGroupsSaved(newGroups: Group[]) {
  const ids = new Set(user.value.groups.map(g => g.id));
  newGroups.forEach(g => { if (!ids.has(g.id)) user.value.groups.push(g); });
  user.value.groups.sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
}

// ---------------------------------------------------------------------------
// Navigation
// ---------------------------------------------------------------------------
function showUserEdit() {
  router.push(`/app/authority/user/${props.id}/edit`);
}
</script>