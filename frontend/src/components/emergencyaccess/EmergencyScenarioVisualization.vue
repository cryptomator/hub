<template>
  <div class="mt-2">
    <div
      ref="pillContainer"
      class="relative flex flex-wrap gap-2 min-h-[40px] p-2 border border-gray-300 rounded-md bg-gray-100 opacity-60 cursor-not-allowed"
      aria-disabled="true"
    >
      <template v-if="loadingCouncilSelection">
        <div class="w-full flex py-2.5">
          <svg class="animate-spin h-5 w-5 text-gray-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
        </div>
      </template>

      <template v-else>
        <template v-if="isGrantButtonDisabled">
          <div class="relative flex flex-wrap gap-2 min-h-[40px]">
            <span
              class="pill inline-flex items-center border border-red-300 bg-red-50 text-red-800 text-sm font-medium px-2 py-1 rounded-full shadow-sm absolute"
            >
              <ExclamationTriangleIcon class="h-4 w-4 text-red-500 mr-1" />
              <span class="truncate">{{ t('recoveryDialog.notPossible') }}</span>
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
                class="pill inline-flex items-center border border-grey bg-white text-sm font-medium px-2 py-1 rounded-full shadow-sm absolute"
                :style="{ left: `${calcLeft(index)}px`, width: pillWidth + 'px', zIndex: 1 }"
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
              class="inline-flex items-center border border-gray-300 bg-gray-100 text-gray-700 text-sm font-medium px-3 py-1 rounded-full shadow-sm absolute"
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
import { computed, watch, ref, toRefs, onMounted, onBeforeUnmount } from 'vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../../common/backend';
import { nextTick } from 'vue';

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
  requiredKeyShares: number
}>();

let timeoutId: ReturnType<typeof setTimeout> | null = null;
const loadingCouncilSelection = ref(true);
const randomCouncilSelection = ref<UserDto[]>([]);
const randomSelectionInterval = ref<ReturnType<typeof setInterval> | null>(null);

const { selectedUsers, requiredKeyShares } = toRefs(props);

const pillContainer = ref<HTMLElement | null>(null);
const containerWidth = ref(0);
const maxVisiblePills = 3;

function updateContainerWidth() {
  if (pillContainer.value) {
    containerWidth.value = pillContainer.value.clientWidth;
  }
}

onMounted(() => {
  nextTick(() => {
    updateContainerWidth();
    window.addEventListener('resize', updateContainerWidth);
  });
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateContainerWidth);
});

const pillWidth = computed(() => {
  const totalGap = (maxVisiblePills - 1) * 8;
  const usableWidth = Math.max(containerWidth.value - totalGap, 0) - 100;
  return Math.floor(usableWidth / maxVisiblePills) || 200;
});

watch(
  [selectedUsers, requiredKeyShares],
  () => {
    loadingCouncilSelection.value = true;
   
    pickRandomCouncilMembers();
    updateContainerWidth();
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }
    timeoutId = setTimeout(() => {
      loadingCouncilSelection.value = false;
      timeoutId = null;
    }, 100);
  },
  { immediate: true }
);

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
const isGrantButtonDisabled = computed(() => {
  return selectedUsers.value.length < requiredKeyShares.value;
});

watch([isGrantButtonDisabled], () => {
  if (!isGrantButtonDisabled.value) {
    pickRandomCouncilMembers();
    startRandomCouncilInterval();
  } 
}, { immediate: true });

function pickRandomCouncilMembers() {
  const available = props.selectedUsers as T[];
  const required = props.requiredKeyShares ?? 1;

  if (available.length < required) {
    randomCouncilSelection.value = [];
    return;
  }

  if (needsInitialSelection(available, required)) {
    setInitialCouncil(available, required);
    return;
  }
  if (selectedUsers.value.length != requiredKeyShares.value)
    rotateCouncilMember(available, required);
}

function needsInitialSelection(available: T[], required: number): boolean {
  if (randomCouncilSelection.value.length !== required) return true;

  const currentIds = randomCouncilSelection.value.map(u => u.id);
  const availableIds = new Set(available.map(u => u.id));

  return currentIds.some(id => !availableIds.has(id));
}

function setInitialCouncil(available: T[], required: number) {
  const shuffled = [...available].sort(() => 0.5 - 
    Math.random() // NOSONAR
  ); 
  randomCouncilSelection.value = shuffled.slice(0, required);
}

function rotateCouncilMember(available: T[], required: number) {
  const current = randomCouncilSelection.value;
  const currentIds = new Set(current.map(u => u.id));
  const candidates = available.filter(u => !currentIds.has(u.id));

  const maxPills = Math.min(required, 3);
  const replaceIndex = Math.floor(
    Math.random() // NOSONAR
    * maxPills
  );

  let newUser: T;

  if (candidates.length > 0) {
    newUser = candidates[Math.floor(
      Math.random() // NOSONAR
      * candidates.length
    )];
  } else {
    const alternatives = available.filter(u => u.id !== current[replaceIndex].id);
    if (alternatives.length === 0) return;
    newUser = alternatives[Math.floor(
      Math.random() // NOSONAR
      * alternatives.length
    )];
  }

  randomCouncilSelection.value = [
    ...current.slice(0, replaceIndex),
    newUser,
    ...current.slice(replaceIndex + 1),
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
  const GAP = 8;
  const PILL_WIDTH = pillWidth.value;
  const PLUS_WIDTH = 8;

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