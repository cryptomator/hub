<template>
  <div v-if="state == State.Initial">
    {{ t('common.loading') }}
  </div>

  <div v-else-if="state == State.EnterRecoveryKey">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterRecoveryKey.title') } ]"/>
    <form ref="form" novalidate @submit.prevent="validateRecoveryKey()">
      <div class="flex justify-center">
        <div class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
          <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <ArrowPathIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              {{ t('createVault.enterRecoveryKey.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                {{ t('createVault.enterRecoveryKey.description') }}
              </p>
            </div>
          </div>
          <div class="mt-5 sm:mt-6">
            <label for="recoveryKey" class="sr-only">{{ t('createVault.enterRecoveryKey.recoveryKey') }}</label>
            <textarea id="recoveryKey" v-model="recoveryKey" rows="6" name="recoveryKey" class="block w-full rounded-md border-gray-300 shadow-xs focus:border-primary focus:ring-primary sm:text-sm" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onRecoverError instanceof FormValidationFailedError }" required />
          </div>
          <div class="mt-5 sm:mt-6">
            <button type="submit" :disabled="processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
              {{ t('createVault.enterRecoveryKey.submit') }}
            </button>
            <div v-if="onRecoverError != null">
              <p v-if="(onRecoverError instanceof FormValidationFailedError)" class="text-sm text-red-900 mt-2">{{ t('createVault.error.formValidationFailed') }}</p>
              <p v-else class="text-sm text-red-900 mt-2">{{ t('createVault.error.invalidRecoveryKey') }}</p>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.EnterVaultDetails">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterVaultDetails.title') } ]"/>
    <VaultCreationProgress :state="State.EnterVaultDetails" :steps="allCreateStates" class="flex justify-center mb-4" />
    <form ref="form" class="space-y-6" novalidate @submit.prevent="validateVaultDetails()">
      <div class="flex justify-center text-center">
        <div class="bg-white shadow-sm rounded-lg overflow-hidden sm:w-full sm:max-w-lg">
          <div class="mx-auto mt-5 flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <PlusIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5 px-4 text-center">
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ t('createVault.enterVaultDetails.title') }}
            </h3>
            <p class="mt-2 text-sm text-gray-500">
              {{ t('createVault.enterVaultDetails.description') }}
            </p>
          </div>

          <div class="mt-6 px-4 space-y-6">
            <div>
              <label for="vaultName" class="block text-sm font-medium text-gray-700 text-left">{{ t('createVault.enterVaultDetails.vaultName') }}</label>
              <input id="vaultName" v-model="vaultName" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" pattern="^(?! )([^\x5C\x2F:*?\x22<>\x7C])+(?<![ \x2E])$" required />
              <p v-if="(onCreateError instanceof FormValidationFailedError)" class="text-sm text-red-900 text-left mt-2">
                {{ t('createVault.error.illegalVaultName') }} \, /, :, *, ?, ", &lt;, >, |
              </p>
            </div>

            <div>
              <label for="vaultDescription" class="block text-sm font-medium text-gray-700 text-left">
                {{ t('createVault.enterVaultDetails.vaultDescription') }}
                <span class="text-xs text-gray-500">({{ t('common.optional') }})</span>
              </label>
              <input id="vaultDescription" v-model="vaultDescription" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200"/>
            </div>
          </div>

          <div class="bg-gray-50 mt-4 px-4 py-3 sm:px-6">
            <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center sm:space-x-4">
              <div class="text-sm text-red-900 text-right sm:flex-1 sm:min-w-0">
                <template v-if="onCreateError !== null">
                  <p v-if="(onCreateError instanceof FormValidationFailedError)">
                    {{ t('createVault.error.formValidationFailed','') }} 
                  </p>
                  <p v-else>
                    {{ t('common.unexpectedError', [onCreateError.message]) }}
                  </p>
                </template>
              </div>
              <div class="flex flex-col-reverse sm:flex-row-reverse sm:space-x-reverse sm:space-x-3 flex-shrink-0 mt-4 sm:mt-0">
                <button
                  type="submit"
                  :disabled="processing"
                  class="inline-flex justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                >
                  {{ t('common.next') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>
  <div v-else-if="state == State.DefineEmergencyAccess">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterVaultDetails.title') } ]"/>
    <VaultCreationProgress :state="state" :steps="allCreateStates" class="flex justify-center mb-4" />
    <form @submit.prevent="validateVaultEmergencyAccess()">
      <div class="flex justify-center">
        <div class="bg-white shadow-sm rounded-lg sm:w-full sm:max-w-lg">
          <div class="mx-auto mt-5 flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <ArrowPathIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 mb-3 px-4 sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900 text-center">
              {{ t('createVault.emergencyAccessDetails.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500 text-center">
                {{ t('createVault.emergencyAccessDetails.description') }}
              </p>
            </div>
            <div class="relative">
              <div class="sm:grid sm:grid-cols-2 sm:items-center sm:gap-2 pt-2 pb-2">
                <label for="coundcilMembers" class="text-sm font-medium text-gray-700 flex items-center">
                  {{ t('admin.emergencyAccess.councilMembers.title') }}
                </label>
              </div>
              <MultiUserSelectInputGroup
                :selected-users="emergencyCouncilMembers"
                :on-search="searchCouncilMembers"
                :input-visible="allowChangingDefaults"
                @action="addCouncilMember"
                @remove="removeCouncilMember"
              />
            </div>

            <RequiredKeySharesInput
              v-model="requiredKeyShares"
              :allow-changing-defaults="allowChangingDefaults"
              :default-key-shares="defaultRequiredEmergencyKeyShares"
            />
            <label class="block text-sm font-medium text-gray-700 pt-4">
              {{ t('grantEmergencyAccessDialog.possibleEmergencyScenario') }}
            </label>
            <EmergencyScenarioVisualization
              :selected-users="emergencyCouncilMembers"
              :required-key-shares="requiredKeyShares"
            />
          </div>
          <div class="bg-gray-50 mt-4 px-4 py-3 sm:px-6 rounded-b-lg">
            <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center sm:space-x-4">
              <div class="text-sm text-red-900 sm:flex-1 sm:min-w-0">
                <template v-if="onCreateError !== null">
                  <p v-if="!(onCreateError instanceof PaymentRequiredError)">
                    {{ t('common.unexpectedError', [onCreateError.message]) }}
                  </p>
                </template>
              </div>
              <div class="flex flex-col-reverse sm:flex-row-reverse sm:space-x-reverse sm:space-x-3 flex-shrink-0 mt-4 sm:mt-0">
                <button
                  type="submit"
                  :disabled="isGrantButtonDisabled"
                  class="inline-flex justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                >
                  {{ t('common.next') }}
                </button>
                <button
                  type="button"
                  class="mt-3 sm:mt-0 inline-flex justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm"
                  @click="backToEnterVaultDetails()" 
                >
                  {{ t('common.previous') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>
  <div v-else-if="state == State.ShowRecoveryKey">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterVaultDetails.title') } ]"/>
    <VaultCreationProgress :state="state" :steps="allCreateStates" class="flex justify-center mb-4" />
    <form @submit.prevent="createVault()">
      <div class="flex justify-center text-center">
        <div class="bg-white shadow-sm rounded-lg overflow-hidden sm:max-w-lg">
          <div class="mx-auto mt-5 flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <KeyIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5 px-4">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              {{ t('createVault.showRecoveryKey.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                {{ t('createVault.showRecoveryKey.description') }}
              </p>
            </div>
            <div class="relative mt-5 sm:mt-6">
              <div class="overflow-hidden rounded-lg border border-gray-300 shadow-xs focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
                <label for="recoveryKey" class="sr-only">{{ t('createVault.showRecoveryKey.recoveryKey') }}</label>
                <textarea id="recoveryKey" v-model="recoveryKey" rows="6" name="recoveryKey" class="block w-full resize-none border-0 py-3 focus:ring-0 sm:text-sm" readonly />

                <!-- Spacer element to match the height of the toolbar -->
                <div class="py-2" aria-hidden="true">
                  <div class="h-9" />
                </div>
              </div>

              <div class="absolute inset-x-0 bottom-0">
                <div class="flex flex-nowrap justify-end space-x-2 py-2 px-2 sm:px-3">
                  <div class="shrink-0">
                    <button type="button" class="relative inline-flex items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 text-sm font-medium text-gray-500 hover:bg-gray-100 sm:px-3" @click="copyRecoveryKey()">
                      <ClipboardIcon class="h-5 w-5 shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
                      <span v-if="!copiedRecoveryKey" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
                      <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="relative flex items-start text-left mt-5 sm:mt-6">
              <div class="flex h-5 items-center">
                <input id="confirmRecoveryKey" v-model="confirmRecoveryKey" name="confirmRecoveryKey" type="checkbox" class="h-4 w-4 rounded-sm border-gray-300 text-primary focus:ring-primary" required>
              </div>
              <div class="ml-3 text-sm">
                <label for="confirmRecoveryKey" class="font-medium text-gray-700">{{ t('createVault.showRecoveryKey.confirmRecoveryKey') }}</label>
              </div>
            </div>
          </div>
          <div class="bg-gray-50 mt-4 px-4 py-3 sm:px-6">
            <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center sm:space-x-4">
              <div class="text-sm text-red-900 sm:flex-1 sm:min-w-0">
                <template v-if="onCreateError !== null">
                  <p v-if="!(onCreateError instanceof PaymentRequiredError)">
                    {{ t('common.unexpectedError', [onCreateError.message]) }}
                  </p>
                  <p v-else>
                    {{ t('createVault.error.paymentRequired') }}
                  </p>
                </template>
              </div>
              <div class="flex flex-col-reverse sm:flex-row-reverse sm:space-x-reverse sm:space-x-3 flex-shrink-0 mt-4 sm:mt-0">
                <button
                  type="submit"
                  :disabled="!confirmRecoveryKey || processing"
                  class="inline-flex justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                >
                  {{ t('createVault.showRecoveryKey.submit') }}
                </button>
                <button
                  type="button"
                  class="mt-3 sm:mt-0 inline-flex justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm"
                  @click="backToDefineEmergencyAccess()" 
                >
                  {{ t('common.previous') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.Finished">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterVaultDetails.title') } ]"/>
    <VaultCreationProgress :state="state" :steps="allCreateStates" class="flex justify-center mb-4" />
    <div class="flex justify-center">
      <div class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
        <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
          <CheckIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
        </div>
        <div class="mt-3 sm:mt-5">
          <h3 class="text-lg leading-6 font-medium text-gray-900">
            {{ t('createVault.success.title') }}
          </h3>
          <div class="mt-2">
            <p class="text-sm text-gray-500">
              {{ t('createVault.success.description') }}
            </p>
          </div>
        </div>
        <div class="mt-5 sm:mt-6">
          <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-xs text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="downloadVaultTemplate()">
            <ArrowDownTrayIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('createVault.success.download') }}
          </button>
          <p v-if="onDownloadTemplateError != null " class="text-sm text-red-900 mr-4">{{ t('createVault.error.downloadTemplateFailed', [onDownloadTemplateError.message]) }}</p> <!-- TODO: not beautiful-->
        </div>
        <div class="mt-2">
          <router-link to="/app/vaults" class="text-sm text-gray-500">
            {{ t('createVault.success.return') }}
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ClipboardIcon } from '@heroicons/vue/20/solid';
import { ArrowPathIcon, CheckIcon, KeyIcon, PlusIcon } from '@heroicons/vue/24/outline';
import { ArrowDownTrayIcon } from '@heroicons/vue/24/solid';
import { saveAs } from 'file-saver';
import { onMounted, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { PaymentRequiredError, ActivatedUser, didCompleteSetup } from '../common/backend';
import { VaultKeys } from '../common/crypto';
import userdata from '../common/userdata';
import { debounce } from '../common/util';
import { VaultConfig } from '../common/vaultconfig';
import BreadcrumbNav from './BreadcrumbNav.vue';
import MultiUserSelectInputGroup from './MultiUserSelectInputGroup.vue';
import RequiredKeySharesInput from './emergencyaccess/RequiredKeySharesInput.vue';
import { EmergencyAccess } from '../common/emergencyaccess';
import EmergencyScenarioVisualization from './emergencyaccess/EmergencyScenarioVisualization.vue';
import VaultCreationProgress from './VaultCreationProgress.vue';
import { wordEncoder } from '../common/util';

enum State {
  Initial,
  EnterRecoveryKey,
  EnterVaultDetails,
  DefineEmergencyAccess,
  ShowRecoveryKey,
  Finished
}

class FormValidationFailedError extends Error {
  constructor() {
    super('The form is invalid.');
  }
}

class EmptyVaultTemplateError extends Error {
  constructor() {
    super('Vault template is empty.');
  }
}

const { t } = useI18n({ useScope: 'global' });

const form = ref<HTMLFormElement>();

const onCreateError = ref<Error | null >(null);
const onRecoverError = ref<Error | null >(null);
const onDownloadTemplateError = ref<Error | null>(null);

const state = ref(State.Initial);
const processing = ref(false);
const vaultName = ref('');
const vaultDescription = ref<string | undefined>();
const copiedRecoveryKey = ref(false);
const debouncedCopyFinish = debounce(() => copiedRecoveryKey.value = false, 2000);
const confirmRecoveryKey = ref(false);
const vaultKeys = ref<VaultKeys>();
const recoveryKey = ref<string>('');
const vaultConfig = ref<VaultConfig>();

const props = defineProps<{
  recover: boolean
}>();

const emergencyCouncilMembers = computed(() =>
  [...initialEmergencyCouncilMembers.value, ...addedEmergencyCouncilMembers.value]
);
const defaultEmergencyCouncilMembers = ref<ActivatedUser[]>([]);
const defaultRequiredEmergencyKeyShares = ref<number>(0);
const allowChangingDefaults = ref<boolean>(false);
const requiredKeyShares = ref<number>(0);
const initialEmergencyCouncilMembers = ref<ActivatedUser[]>([]);
const addedEmergencyCouncilMembers = ref<ActivatedUser[]>([]);

async function searchCouncilMembers(query: string): Promise<ActivatedUser[]> {
  const existingIds = new Set(emergencyCouncilMembers.value.map(m => m.id));
  const authorities = await backend.authorities.search(query, true);
  return authorities
    .filter(a => a.type === 'USER')
    .filter(a => didCompleteSetup(a))
    .filter(a => !existingIds.has(a.id))
    .sort((a, b) => a.name.localeCompare(b.name));
}

async function addCouncilMember(authority: ActivatedUser) {
  const alreadyExists =
    initialEmergencyCouncilMembers.value.some(u => u.id === authority.id) ||
    addedEmergencyCouncilMembers.value.some(u => u.id === authority.id);
  if (!alreadyExists) {
    addedEmergencyCouncilMembers.value = [...addedEmergencyCouncilMembers.value, authority];
  }
}

function removeCouncilMember(user: ActivatedUser) {
  initialEmergencyCouncilMembers.value = initialEmergencyCouncilMembers.value.filter(u => u.id !== user.id);
  addedEmergencyCouncilMembers.value = addedEmergencyCouncilMembers.value.filter(u => u.id !== user.id);
}

async function loadDefaultEmergencyAccessSettings() {
  try {
    const settings = await backend.settings.get();
    const authorities = await backend.authorities.listSome(settings.emergencyCouncilMemberIds);
    const activatedUsers = authorities
      .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
      .sort((a, b) => a.name.localeCompare(b.name));

    defaultEmergencyCouncilMembers.value = activatedUsers;
    initialEmergencyCouncilMembers.value = [...activatedUsers];
    allowChangingDefaults.value = settings.allowChoosingEmergencyCouncil;
    defaultRequiredEmergencyKeyShares.value = settings.defaultRequiredEmergencyKeyShares;
    requiredKeyShares.value = settings.defaultRequiredEmergencyKeyShares;
  } catch (error) {
    console.error('Loading emergency council members failed:', error);
    defaultRequiredEmergencyKeyShares.value = 0;
    allowChangingDefaults.value = false;
  }
}

const isInvalidKeyShares = computed(() => {
  return requiredKeyShares.value < 1;
});

const isInvaildCouncilMembers = computed(() => {
  return emergencyCouncilMembers.value.length < 1;
});

const hasTooFewCouncilMembers = computed(() => {
  return emergencyCouncilMembers.value.length < requiredKeyShares.value;
});

const isGrantButtonDisabled = computed(() => {
  return isInvalidKeyShares.value || isInvaildCouncilMembers.value || hasTooFewCouncilMembers.value;
});

onMounted(initialize);

async function initialize() {
  if (props.recover) {
    state.value = State.EnterRecoveryKey;
  } else {
    vaultKeys.value = await VaultKeys.create();
    recoveryKey.value = await vaultKeys.value.createRecoveryKey();
    await loadDefaultEmergencyAccessSettings();
    state.value = State.EnterVaultDetails;
  }
}

async function validateRecoveryKey() {
  onRecoverError.value = null;
  if (!form.value?.checkValidity()) {
    onRecoverError.value = new FormValidationFailedError();
    return;
  }
  await recoverVault();
}

const allCreateStates = [
  State.EnterVaultDetails,
  State.DefineEmergencyAccess,
  State.ShowRecoveryKey,
  State.Finished,
];

async function recoverVault() {
  onRecoverError.value = null;
  try {
    processing.value = true;
    vaultKeys.value = await VaultKeys.recover(recoveryKey.value);
    state.value = State.EnterVaultDetails;
  } catch (error) {
    console.error('Recovering vault failed.', error);
    onRecoverError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

async function validateVaultDetails() {
  onCreateError.value = null;
  if (!form.value?.checkValidity()) {
    onCreateError.value = new FormValidationFailedError();
    return;
  }
  if (props.recover) {
    await createVault();
  } else {
    state.value = State.DefineEmergencyAccess;
  }
}

async function validateVaultEmergencyAccess(){
  await splitRecoveryKey();
  state.value = State.ShowRecoveryKey;
}

const vaultKeyShares = ref<Record<string, string> | null>(null);

async function splitRecoveryKey() {
  try {
    onCreateError.value = null;

    if (!vaultKeys.value) {
      throw new Error(t('grantEmergencyAccessDialog.error.missingVaultKeys'));
    }

    if (requiredKeyShares.value == null || requiredKeyShares.value < 1) {
      throw new Error(t('grantEmergencyAccessDialog.error.invalidKeyShares'));
    }

    if (emergencyCouncilMembers.value.length < 1) {
      throw new Error(t('grantEmergencyAccessDialog.error.invalidCouncilMemberLengt'));
    }

    if (emergencyCouncilMembers.value.length < requiredKeyShares.value) {
      throw new Error(
        t('grantEmergencyAccessDialog.error.notEnoughCouncilMembers', {
          required: requiredKeyShares.value,
          actual: emergencyCouncilMembers.value.length,
        })
      );
    }

    const recoveryKeyText = await vaultKeys.value.createRecoveryKey();
    const recoveryKeyBytes = wordEncoder.decode(recoveryKeyText); // TODO: remove encode/decode once UVF is merged

    vaultKeyShares.value = await EmergencyAccess.split(
      recoveryKeyBytes,
      requiredKeyShares.value,
      ...emergencyCouncilMembers.value
    );
  } catch (error) {
    console.error('Splitting recovery key failed.', error);
    onCreateError.value = error instanceof Error ? error : new Error('Unknown Error');
    throw error;
  }
}

function backToEnterVaultDetails(){
  state.value = State.EnterVaultDetails;
}

function backToDefineEmergencyAccess(){
  state.value = State.DefineEmergencyAccess;
}

async function createVault() {
  onCreateError.value = null;
  try {
    if (!vaultKeys.value) {
      throw new Error('Invalid state');
    }
    processing.value = true;
    const owner = await userdata.me;
    const vaultId = crypto.randomUUID();
    vaultConfig.value = await VaultConfig.create(vaultId, vaultKeys.value);
    const ownerJwe = await vaultKeys.value.encryptForUser(await userdata.ecdhPublicKey);
    await backend.vaults.createOrUpdateVault(
      vaultId, 
      vaultName.value, 
      false, 
      requiredKeyShares.value, 
      vaultKeyShares.value ?? {}, 
      vaultDescription.value);
    await backend.vaults.grantAccess(vaultId, { userId: owner.id, token: ownerJwe });
    state.value = State.Finished;
  } catch (error) {
    console.error('Creating vault failed.', error);
    onCreateError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

async function copyRecoveryKey() {
  await navigator.clipboard.writeText(recoveryKey.value);
  copiedRecoveryKey.value = true;
  debouncedCopyFinish();
}

async function downloadVaultTemplate() {
  onDownloadTemplateError.value = null;
  try {
    const blob = await vaultConfig.value?.exportTemplate();
    if (blob != null) {
      saveAs(blob, `${vaultName.value}.zip`);
    } else {
      throw new EmptyVaultTemplateError();
    }
  } catch (error) {
    console.error('Exporting vault template failed.', error);
    onDownloadTemplateError.value = error instanceof Error ? error : new Error('Unknown reason');
  }
}
</script>
