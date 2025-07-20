<template>
  <div class="mt-4">
    <label class="block text-sm font-medium text-gray-700 pb-2">
      {{ t('grantEmergencyAccessDialog.possibleEmergencyScenario') }}
    </label>
    <div class="relative flex flex-wrap gap-2 min-h-[40px]">
      <template v-if="loadingCouncilSelection">
        <div class="w-full flex py-2">
          <svg class="animate-spin h-5 w-5 text-gray-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
        </div>
      </template>

      <template v-else>
        <template v-if="grantButtonDisabled">
          <div class="relative flex flex-wrap gap-2 min-h-[40px]">
            <span
              class="pill inline-flex items-center border border-red-300 bg-red-50 text-red-800 text-sm font-medium px-2 py-1 rounded-full shadow-sm absolute"
            >
              <span class="truncate">not possible</span>
            </span>
          </div>
        </template>

        <template v-else>
          <TransitionGroup
            name="pill"
            tag="div"
            class="relative flex flex-wrap gap-2 min-h-[40px]"
          >
            <template v-for="(item, index) in displayItems.value" :key="item.id">
              <span
                v-if="item.type === 'user' && index <= 5"
                class="pill inline-flex items-center border border-indigo-300 bg-indigo-50 text-indigo-800 text-sm font-medium px-2 py-1 rounded-full shadow-sm absolute"
                :style="{ left: `${calcLeft(index)}px`, width: '100px', zIndex: 1 }"
              >
                <img :src="item.user!.pictureUrl" class="w-4 h-4 rounded-full mr-1 shrink-0" />
                <span class="truncate">{{ item.user!.name }}</span>
              </span>

              <span
                v-else-if="item.type === 'plus' && index <= 5"
                class="pill inline-flex items-center justify-center text-gray-500 font-medium absolute"
                :style="{ left: `${calcLeft(index)}px`, zIndex: 0 }"
              >
                +
              </span>
            </template>

            <span
              v-if="requiredKeyShares > 3"
              class="pill inline-flex items-center border border-gray-300 bg-gray-100 text-gray-700 text-sm font-medium px-3 py-1 rounded-full shadow-sm absolute"
              :style="{ left: `${calcLeft(5)}px`, zIndex: 1 }"
            >
              +{{ requiredKeyShares - 3 }}
            </span>
          </TransitionGroup>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends UserDto">
import { computed, watch, ref, toRefs } from 'vue';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../../common/backend';

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type?: string;
  memberSize?: number;
}

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  selectedUsers: T[];
  grantButtonDisabled: boolean,
  requiredKeyShares: number
}>();

let timeoutId: ReturnType<typeof setTimeout> | null = null;
const loadingCouncilSelection = ref(true);
const randomCouncilSelection = ref<UserDto[]>([]);
const randomSelectionInterval = ref<ReturnType<typeof setInterval> | null>(null);

const { selectedUsers, requiredKeyShares, grantButtonDisabled } = toRefs(props);

watch(
  [selectedUsers, requiredKeyShares],
  () => {
    loadingCouncilSelection.value = true;

    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }

    timeoutId = setTimeout(() => {
      pickRandomCouncilMembers();
      loadingCouncilSelection.value = false;
      timeoutId = null;
    }, 100);
  },
  { immediate: true }
);

watch([grantButtonDisabled], () => {
  if (!grantButtonDisabled.value) {
    pickRandomCouncilMembers();
    startRandomCouncilInterval();
  } 
}, { immediate: true });

function startRandomCouncilInterval() {
  stopRandomCouncilInterval();
  pickRandomCouncilMembers();
  randomSelectionInterval.value = setInterval(() => {
    pickRandomCouncilMembers();
  }, 2000);
}

function stopRandomCouncilInterval() {
  if (randomSelectionInterval.value) {
    clearInterval(randomSelectionInterval.value);
    randomSelectionInterval.value = null;
  }
}

function pickRandomCouncilMembers() {
  const available = props.selectedUsers as T[];
  const required = props.requiredKeyShares ?? 1;

  if (available.length < required) {
    randomCouncilSelection.value = [];
    return;
  }

  if (randomCouncilSelection.value.length !== required) {
    const shuffled = [...available].sort(() => 0.5 - Math.random());
    randomCouncilSelection.value = shuffled.slice(0, required) as T[];

    return;
  }

  const maxPills = (props.requiredKeyShares < 3) ? props.requiredKeyShares : 3 ; 
  const current = randomCouncilSelection.value;
  const currentIds = new Set(current.map(u => u.id));

  const candidates = available.filter(u => !currentIds.has(u.id));
  if (candidates.length === 0) return;

  const newUser = candidates[Math.floor(Math.random() * candidates.length)];
  const replaceIndex = Math.floor(Math.random() * maxPills);

  randomCouncilSelection.value = [
    ...current.slice(0, replaceIndex),
    newUser,
    ...current.slice(replaceIndex + 1)
  ];
}

const randomCouncilSelectionWithPluses = computed(() => {
  const items: { type: 'user' | 'plus', user?: UserDto, id: string }[] = [];

  randomCouncilSelection.value.forEach((user, i) => {
    items.push({ type: 'user', user, id: user.id });
    if (i < randomCouncilSelection.value.length - 1) {
      items.push({ type: 'plus', id: `plus-${i}` });
    }
  });

  return items;
});

function calcLeft(index: number): number {
  const PILL_WIDTH = 100;
  const PLUS_WIDTH = 8;
  const GAP = 8;
  let x = 0;

  for (let i = 0; i < index; i++) {
    const el = randomCouncilSelectionWithPluses.value[i];
    if (el.type === 'user') {
      x += PILL_WIDTH + GAP;
    } else {
      x += PLUS_WIDTH + GAP;
    }
  }
  return x;
}

const displayItems = computed(() => randomCouncilSelectionWithPluses);
</script>

<style scoped>
.pill-enter-active,
.pill-leave-active {
  transition: all 0.5s ease;
}

.pill-enter-from {
  opacity: 0;
  transform: translateY(-10px) scale(1.05);
  filter: blur(2px);
}
.pill-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.pill-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}
.pill-leave-to {
  opacity: 0;
  transform: translateY(15px) scale(0.95);
  filter: blur(2px);
}

</style>