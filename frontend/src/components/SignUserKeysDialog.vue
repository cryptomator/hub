<template>
  <HubDialog v-model:open="open" :title="t('signUserKeysDialog.title', [user.name])" :busy="processing" :error="onSignError && t('common.unexpectedError', [onSignError.message])" :initial-focus="fingerprintInput" @submit="sign()" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ minVerificationLen === 0 ? t('signUserKeysDialog.descriptionPrefilled', [user.name]) : t('signUserKeysDialog.description', [minVerificationLen, user.name]) }}
    </DialogDescription>
    <div class="mt-5">
      <label for="fingerprint" class="block text-sm font-medium text-gray-700">{{ t('signUserKeysDialog.fingerprint') }}</label>
      <div class="relative mt-1">
        <input
          id="fingerprint"
          ref="fingerprintInput"
          v-model="enteredFingerprint"
          type="text"
          name="fingerprint"
          spellcheck="false"
          autocomplete="off"
          autocapitalize="off"
          :readonly="minVerificationLen === 0"
          :disabled="processing"
          class="block w-full rounded-md border-gray-300 pr-10 font-mono shadow-xs focus:border-primary focus:ring-primary sm:text-sm disabled:bg-gray-200"
          @keyup="tryAutocomplete()"
        />
        <div class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
          <CheckIcon v-if="fingerprintMatches" class="h-5 w-5 text-primary" aria-hidden="true" />
        </div>
      </div>
    </div>

    <template #footer>
      <button type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing || !fingerprintMatches" :class="primaryButton">
        {{ t('signUserKeysDialog.submit') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { CheckIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { TrustDto, UserDto } from '../common/backend';
import wot from '../common/wot';
import { primaryButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const fingerprintInput = ref<HTMLInputElement>();

const props = defineProps<{
  user: UserDto
  trust?: TrustDto
}>();

const emit = defineEmits<{
  close: []
  signed: [TrustDto]
}>();

defineExpose({
  show
});

const onSignError = ref<Error>();
const expectedFingerprint = ref<string>();
const enteredFingerprint = ref<string>('');
const minVerificationLen = ref<number>(64); // default: check all 64 hex digits of 256 bit fingerprint
const fingerprintMatches = computed(() => enteredFingerprint.value?.replaceAll(' ', '') === expectedFingerprint.value);

onMounted(fetchData);

async function fetchData() {
  try {
    expectedFingerprint.value = await wot.computeFingerprint(props.user);
    minVerificationLen.value = (await backend.settings.get()).wotIdVerifyLen;
    if (minVerificationLen.value === 0) {
      enteredFingerprint.value = expectedFingerprint.value.replace(/.{8}/g, '$&' + ' ').trim();
    }
  } catch (error) {
    console.error('Loading fingerprint failed.', error);
    onSignError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function show() {
  open.value = true;
}

async function tryAutocomplete() {
  if (enteredFingerprint.value.length >= minVerificationLen.value) {
    if (expectedFingerprint.value?.startsWith(enteredFingerprint.value)) {
      enteredFingerprint.value = expectedFingerprint.value.replace(/.{8}/g, '$&' + ' ').trim();
    }
  }
}

async function sign() {
  if (processing.value || !open.value || !fingerprintMatches.value) {
    return;
  }
  onSignError.value = undefined;
  try {
    processing.value = true;
    const newTrust = await wot.sign(props.user);
    emit('signed', newTrust);
    open.value = false;
  } catch (error) {
    console.error('Signing user keys failed.', error);
    onSignError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}

</script>
