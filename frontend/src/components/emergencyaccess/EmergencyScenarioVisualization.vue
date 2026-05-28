<template>
  <div class="p-2 border border-gray-300 rounded-md bg-gray-200 opacity-60 cursor-not-allowed" aria-disabled="true">
    <template v-if="loadingCouncilSelection">
      <div class="flex">
        <svg class="animate-spin h-5 w-5 text-gray-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>
    </template>

    <template v-else>
      <template v-if="isGrantButtonDisabled">
        <div class="flex">
          <span class="inline-flex items-center border border-red-300 bg-red-50 text-red-800 text-sm font-medium px-2 py-1 rounded-full shadow-sm">
            <ExclamationTriangleIcon class="h-4 w-4 m-1 text-red-500 mr-1" />
            <span class="truncate">{{ t('emergencyAccess.notPossible') }}</span>
          </span>
        </div>
      </template>

      <template v-else>
        <div class="flex items-center gap-2 text-sm text-gray-800">
          <template v-for="(slot, index) in visibleSlots" :key="`lane-${index}`">
            <div v-if="slot.type === 'user'" class="flex-1 min-w-0 max-w-1/3">
              <div class="slot-lane relative h-8 overflow-hidden">
                <Transition name="slot-roll">
                  <span :key="slot.user.id" class="absolute inset-0 inline-flex w-full items-center justify-between gap-1 rounded-full border border-grey bg-white px-2 py-1 shadow-sm">
                    <img :src="slot.user.pictureUrl" class="w-4 h-4 rounded-full shrink-0" />
                    <span class="truncate">{{ slot.user.name }}</span>
                    <SegmentRing :start-index="index" :total="requiredKeyShares" :completed="1" :size="24" />
                  </span>
                </Transition>
              </div>
            </div>
            <span v-else class="shrink-0 inline-flex items-center justify-center rounded-full border border-gray-300 bg-gray-100 px-3 py-1 font-medium text-gray-700 shadow-sm">
              +{{ slot.hiddenCount }}
            </span>

            <span v-if="index < visibleSlots.length - 1" class="shrink-0 inline-flex items-center justify-center text-gray-500 font-medium">+</span>
          </template>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, ref, toRefs, onMounted, onBeforeUnmount } from 'vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../../common/backend';
import SegmentRing from './SegmentRing.vue';
import { shuffle, clamp } from 'remeda';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  selectedUsers: UserDto[];
  requiredKeyShares: number;
}>();

let timeoutId: ReturnType<typeof setTimeout> | undefined;
const loadingCouncilSelection = ref(true);
const randomCouncilSelection = ref<UserDto[]>([]);
const randomSelectionInterval = ref<ReturnType<typeof setInterval>>();

const windowWidth = ref(window.innerWidth);
const maxUserPills = computed(() => windowWidth.value < 640 ? 2 : 3);

const { selectedUsers, requiredKeyShares } = toRefs(props);

function onWindowResize() {
  windowWidth.value = window.innerWidth;
}

onMounted(() => {
  window.addEventListener('resize', onWindowResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize);
  stopRandomCouncilInterval();
  if (timeoutId !== undefined) {
    clearTimeout(timeoutId);
  }
  timeoutId = undefined;
});

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
    randomSelectionInterval.value = undefined;
  }
}
const isGrantButtonDisabled = computed(() => {
  return requiredKeyShares.value < 1 || selectedUsers.value.length < requiredKeyShares.value;
});

const visibleUserSlots = computed(() => {
  return clamp(requiredKeyShares.value, { max: maxUserPills.value });
});

const visibleSlots = computed(() => {
  const userSlots = randomCouncilSelection.value.map(user => ({
    type: 'user' as const,
    user: user,
  }));

  if (requiredKeyShares.value <= maxUserPills.value) {
    return userSlots;
  } else {
    return [
      ...userSlots.slice(0, maxUserPills.value),
      {
        type: 'overflow' as const,
        hiddenCount: requiredKeyShares.value - maxUserPills.value,
      }
    ];
  }
});

watch(
  [selectedUsers, requiredKeyShares],
  () => {
    loadingCouncilSelection.value = true;

    pickRandomCouncilMembers();
    if (timeoutId !== undefined) {
      clearTimeout(timeoutId);
    }
    timeoutId = setTimeout(() => {
      loadingCouncilSelection.value = false;
      timeoutId = undefined;
    }, 100);
  },
  { immediate: true }
);

watch([isGrantButtonDisabled], () => {
  if (!isGrantButtonDisabled.value) {
    pickRandomCouncilMembers();
    startRandomCouncilInterval();
  } else {
    stopRandomCouncilInterval();
  }
}, { immediate: true });

function pickRandomCouncilMembers() {
  const available = props.selectedUsers;
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

function needsInitialSelection(available: UserDto[], required: number): boolean {
  if (randomCouncilSelection.value.length !== Math.min(required, visibleUserSlots.value)) return true;

  const currentIds = randomCouncilSelection.value.map(u => u.id);
  const availableIds = new Set(available.map(u => u.id));

  return currentIds.some(id => !availableIds.has(id));
}

function setInitialCouncil(available: UserDto[], required: number) {
  const shuffled = shuffle(available);
  randomCouncilSelection.value = shuffled.slice(0, Math.min(required, visibleUserSlots.value));
}

function rotateCouncilMember(available: UserDto[], required: number) {
  const current = randomCouncilSelection.value;
  const currentIds = new Set(current.map(u => u.id));
  const candidates = available.filter(u => !currentIds.has(u.id));

  const userSlots = Math.min(required, maxUserPills.value);

  const replaceIndex = Math.floor(
    Math.random() // NOSONAR
    * userSlots
  );

  let newUser: UserDto;

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
</script>

<style scoped>
.slot-roll-enter-active,
.slot-roll-leave-active {
  transition: transform 0.5s ease, opacity 0.5s ease, filter 0.5s ease;
}

.slot-roll-enter-from {
  opacity: 0;
  transform: translateY(-100%);
  filter: blur(2px);
}
.slot-roll-enter-to {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
}

.slot-roll-leave-from {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
}
.slot-roll-leave-to {
  opacity: 0;
  transform: translateY(100%);
  filter: blur(2px);
}
.slot-lane {
  contain: layout paint;
}
</style>
