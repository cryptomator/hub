<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>
  <div v-else>
    <div class="flex flex-row items-center justify-between gap-3 pb-1 w-full border-b border-gray-200 mb-2">
      <!-- Headline -->
      <h2 id="title" class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl mb-4">
        {{ t('user.detail.info') }}
      </h2>

      <div class="flex gap-3 items-center -mt-4">
        <!-- Edit-Button -->
        <button class="w-full bg-primary py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showUserEdit()">
          {{ t('common.edit') }}
        </button>

        <!-- Ellipsis-Menü -->
        <Menu as="div" class="relative inline-block shrink-0 text-left">
          <MenuButton class="group p-1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2">
            <span class="sr-only">Open options menu</span>
            <EllipsisVerticalIcon class="h-5 w-5 text-gray-400 group-hover:text-gray-500" aria-hidden="true" />
          </MenuButton>

          <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 translate-y-1 scale-95" enter-to-class="transform opacity-100 translate-y-0 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 translate-y-0 scale-100" leave-to-class="transform opacity-0 translate-y-1 scale-95">
            <MenuItems class="absolute right-0 mt-2 z-10 w-48 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black/5 focus:outline-hidden">
              <div class="py-1">
                <MenuItem v-slot="{ active }">
                  <div :class="[ active ? 'bg-gray-100 text-red-900' : 'text-red-700', 'cursor-pointer block px-4 py-2 text-sm']">
                    {{ t('common.remove') }}
                  </div>
                </MenuItem>
              </div>
            </MenuItems>
          </transition>
        </Menu>
      </div>
    </div>
    <div class="hidden lg:grid grid-cols-1 lg:grid-cols-2 gap-6 items-start pt-3">
      <section class="lg:col-start-1 grid gap-6">
        <!-- User Info -->
        <AuthorityUserInfo :user="user"/>
        <!-- Devices -->
        <AuthorityDeviceList :devices="user.devices" :page-size="10" :title="t('user.detail.devices')"/>      
        <AuthorityDeviceList :devices="user.legacyDevices" :page-size="10" :visible="user.legacyDevices.length != 0" :title="t('legacyDeviceList.title')" :info="t('legacyDeviceList.title')"/>
      </section>
      <section class="lg:col-start-2 grid gap-6">
        <!-- Groups -->
        <AuthorityUserGroupsList :user="user" :groups="user.groups" :page-size="10" :on-saved="handleGroupsSaved"></AuthorityUserGroupsList>
        <!-- Vaults -->
        <AuthorityVaultList :vaults="user.vaults" :page-size="10" :visible="user.legacyDevices.length != 0"></AuthorityVaultList>
      </section>
    </div>
    <div class="grid lg:hidden grid-cols-1 gap-6 items-start pt-3">
      <!-- User Info -->
      <AuthorityUserInfo :user="user"/>
      <!-- Groups -->
      <AuthorityUserGroupsList :user="user" :groups="user.groups" :page-size="10" :on-saved="handleGroupsSaved"></AuthorityUserGroupsList>
      <!-- Devices -->
      <AuthorityDeviceList :devices="user.devices" :page-size="10" :title="t('user.detail.devices')"/>      
      <AuthorityDeviceList :devices="user.legacyDevices" :page-size="10" :visible="user.legacyDevices.length != 0" :title="t('legacyDeviceList.title')" :info="t('legacyDeviceList.title')"/>
      <!-- Vaults -->
      <AuthorityVaultList :vaults="user.vaults" :page-size="10" :visible="user.legacyDevices.length != 0"></AuthorityVaultList>
    </div>
  </div>

  <!-- Dialogs -->
</template>

<script setup lang="ts">
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import AuthorityVaultList from './AuthorityVaultList.vue';
import AuthorityDeviceList from './AuthorityDeviceList.vue';
import AuthorityUserGroupsList from './AuthorityUserGroupsList.vue';
import AuthorityUserInfo from './AuthorityUserInfo.vue';

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
const { t } = useI18n({ useScope: 'global' });
const router = useRouter();

const user = ref<DetailUser>({
  firstName: 'Edgar',
  lastName: 'Frank',
  username: 'edgar.frank',
  roles: ['create-vault', 'admin'],
  password: 'password123',
  email: 'edgar.frank@example.com',
  userPicture: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iI0Y3RjdGNyIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiM1MTUxNTEiIGQ9Ik0yOS4yIDEyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTU0LjIgMTIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNTQuMiA4Ny41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME0yOS4yIDg3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTQuMiAzNy41YTguMyw4LjMgMCAxLDEgMTYuNywwYTguMyw4LjMgMCAxLDEgLTE2LjcsME03OS4yIDM3LjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwTTc5LjIgNjIuNWE4LjMsOC4zIDAgMSwxIDE2LjcsMGE4LjMsOC4zIDAgMSwxIC0xNi43LDBNNC4yIDYyLjVhOC4zLDguMyAwIDEsMSAxNi43LDBhOC4zLDguMyAwIDEsMSAtMTYuNywwIi8+PHBhdGggZmlsbD0iI2E0OGIyYSIgZD0iTTI1IDBMMjUgMjVMMTIuNSAyNVpNMTAwIDI1TDc1IDI1TDc1IDEyLjVaTTc1IDEwMEw3NSA3NUw4Ny41IDc1Wk0wIDc1TDI1IDc1TDI1IDg3LjVaTTUwIDM3LjVMNTAgNTBMMzcuNSA1MFpNNjIuNSA1MEw1MCA1MEw1MCAzNy41Wk01MCA2Mi41TDUwIDUwTDYyLjUgNTBaTTM3LjUgNTBMNTAgNTBMNTAgNjIuNVoiLz48L3N2Zz4=',        
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

const loading = ref<boolean>(true);

function handleGroupsSaved(newGroups: Group[]) {
  const ids = new Set(user.value.groups.map(g => g.id));
  newGroups.forEach(g => {
    if (!ids.has(g.id)) user.value.groups.push(g);
  });
  user.value.groups.sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
}

onMounted(async () => {
  try {
    await new Promise((r) => setTimeout(r, 300));
    // TODO: fetch real data here
  } finally {
    loading.value = false;
  }
});

function showUserEdit() {
  router.push(`/app/authority/user/${props.id}/edit`);
}
</script>