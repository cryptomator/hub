<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>
  <FetchError v-else-if="fetchError" :error="fetchError" :retry="fetchGroup" />
  <div v-else>
    <BreadcrumbNav :crumbs="[ { label: t('nav.groups'), to: '/app/groups' }, { label: group.name } ]"/>
    <div class="flex flex-row items-center justify-between gap-3 pb-1 w-full border-b border-gray-200 mb-2">
      <!-- Headline -->
      <h2 id="title" class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl mb-4">
        {{ t('group.detail.info') }}
      </h2>

      <div class="flex gap-3 items-center -mt-4">
        <!-- Edit-Button -->
        <button class="w-full bg-primary py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showGroupEdit">
          {{ t('common.edit') }}
        </button>

        <!-- Menu -->
        <Menu as="div" class="relative inline-block text-left">
          <MenuButton class="group p-1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2">
            <span class="sr-only">Open options menu</span>
            <EllipsisVerticalIcon class="h-5 w-5 text-gray-400 group-hover:text-gray-500" aria-hidden="true" />
          </MenuButton>

          <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 translate-y-1 scale-95" enter-to-class="transform opacity-100 translate-y-0 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 translate-y-0 scale-100" leave-to-class="transform opacity-0 translate-y-1 scale-95">
            <MenuItems class="absolute right-0 mt-2 z-10 w-48 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black/5 focus:outline-hidden">
              <div class="py-1">
                <MenuItem v-slot="{ active }">
                  <div :class="[ active ? 'bg-gray-100 text-red-900' : 'text-red-700', 'cursor-pointer block px-4 py-2 text-sm']" @click="showDeleteGroupDialog(group)">
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
        <!-- Group Info -->
        <GroupInfo :group="group"/>
        <!-- Vaults -->
        <VaultList :vaults="group.vaults" :page-size="10" :visible="true"/>
      </section>
      <section class="lg:col-start-2 grid gap-6">
        <!-- Members -->
        <GroupMemberList v-model:members="group.members" :group="group" :page-size="10" :on-saved="onMembersSaved" />
      </section>
    </div>
    <div class="grid lg:hidden grid-cols-1 gap-6 items-start pt-3">
      <!-- Group Info -->
      <GroupInfo :group="group"/>
      <!-- Members -->
      <GroupMemberList v-model:members="group.members" :group="group" :page-size="10" :on-saved="onMembersSaved" />
      <!-- Vaults -->
      <VaultList :vaults="group.vaults" :page-size="10" :visible="true"/>
    </div>
  </div>
  <!-- Delete Dialog -->
  <GroupDeleteDialog v-if="deletingGroup" ref="deleteGroupDialog" :group="deletingGroup" @close="deletingGroup = undefined" @delete="onGroupDeleted"/>
</template>

<script setup lang="ts">
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { ref, nextTick, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import backend, { AuthorityDto, GroupDto, GroupDtoWithDetails, UserDto } from '../../common/backend';
import GroupDeleteDialog from './GroupDeleteDialog.vue';
import GroupMemberList from './GroupMemberList.vue';
import GroupInfo from './GroupInfo.vue';
import VaultList from './VaultList.vue';
import BreadcrumbNav from '../BreadcrumbNav.vue';
import FetchError from '../FetchError.vue';

const router = useRouter();

const deletingGroup = ref<GroupDto>();

function showDeleteGroupDialog(grp: GroupDtoWithDetails) {
  deletingGroup.value = grp;
  nextTick(() => deleteGroupDialog.value?.show());
}

const deleteGroupDialog = ref<typeof GroupDeleteDialog>();

function showGroupEdit() {
  router.push(`/app/groups/${group.value.id}/edit`);
}

function onGroupDeleted() {
  router.push('/app/groups');
}

const props = defineProps<{ id: string }>();
const { t } = useI18n({ useScope: 'global' });

const group = ref<GroupDtoWithDetails>({
  type: 'GROUP',
  id: props.id,
  name: '',
  pictureUrl: undefined,
  members: [],
  vaults: []
});

function onMembersSaved(newMembers: AuthorityDto[]) {
  const ids = new Set(group.value.members.map(u => u.id));
  newMembers.forEach(u => { if (!ids.has(u.id)) group.value.members.push(u); });
  group.value.members.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
}

const loading = ref(true);
const fetchError = ref<Error | null>(null);

async function fetchGroup() {
  loading.value = true;
  fetchError.value = null;
  try {
    group.value = await backend.groups.getGroup(props.id);
  } catch (error) {
    console.error('Failed to fetch group:', error);
    fetchError.value = error instanceof Error ? error : new Error('Unknown error');
  } finally {
    loading.value = false;
  }
}

onMounted(fetchGroup);
</script>