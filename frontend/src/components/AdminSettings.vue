<template>
  <div v-if="admin == null || version == null || wotMaxDepth == null || wotIdVerifyLen == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else>
    <div class="pb-5 border-b border-gray-200">
      <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
        {{ t('admin.title') }}
      </h2>
    </div>

    <div class="space-y-6 mt-5">
      <section class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
        <h3 class="text-lg font-medium leading-6 text-gray-900">
          {{ t('admin.serverInfo.title') }}
        </h3>
        <p class="mt-1 text-sm text-gray-500 w-full">
          {{ t('admin.serverInfo.description') }}
        </p>
        <hr class="my-4 pb-6 border-gray-200"/>
        <form class="space-y-6 md:gap-6" novalidate>
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="hubId" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.serverInfo.hubId.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="hubId" v-model="admin.hubId" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="hubVersion" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.serverInfo.hubVersion.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="hubVersion" v-model="version.hubVersion" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              <p v-if="errorOnFetchingUpdates" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.serverInfo.hubVersion.description.fetchingUpdatesFailed') }}
              </p>
              <p v-else-if="!stableUpdateExists && !betaUpdateExists" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.serverInfo.hubVersion.description.upToDate') }}
              </p>
              <p v-else-if="stableUpdateExists" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.serverInfo.hubVersion.description.updateExists', [latestVersion?.stable]) }}
              </p>
              <p v-else-if="betaUpdateExists && isBeta" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.serverInfo.hubVersion.description.updateExists', [latestVersion?.beta]) }}
              </p>
              <p v-else-if="betaUpdateExists && !isBeta" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                <InformationCircleIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.serverInfo.hubVersion.description.updateExists', [latestVersion?.beta]) }}
              </p>
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="keycloakVersion" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.serverInfo.keycloakVersion.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="keycloakVersion" v-model="version.keycloakVersion" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              <p id="keycloakAdminRealmURL" class="inline-flex mt-2 text-sm">
                <LinkIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                <a :href="keycloakAdminRealmURL" target="_blank" class="underline text-gray-500 hover:text-gray-900">{{ $t('admin.serverInfo.keycloakVersion.description') }}</a>
              </p>
            </div>
          </div>
        </form>
      </section>

      <section v-if="admin.hasLicense && remainingSeats != null" class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
        <h3 class="text-lg font-medium leading-6 text-gray-900">
          {{ t('admin.licenseInfo.title') }}
        </h3>
        <p class="mt-1 text-sm text-gray-500 w-full">
          {{ t('admin.licenseInfo.description') }}
        </p>
        <hr class="my-4 pb-6 border-gray-200"/>
        <form class="space-y-6 md:gap-6" novalidate>
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="email" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.licenseInfo.email.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="email" v-model="admin.email" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="seats" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.licenseInfo.seats.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="seats" v-model="admin.licensedSeats" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="seats-description" readonly />
              <p v-if="remainingSeats > 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.seats.description.enoughSeats', [remainingSeats]) }}
              </p>
              <p v-else-if="remainingSeats == 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.seats.description.zeroSeats') }}
              </p>
              <p v-else id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                <XMarkIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.seats.description.undercutSeats', [numberOfExceededSeats]) }}
              </p>
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="issuedAt" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.licenseInfo.issuedAt.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="issuedAt" :value="d(admin.issuedAt, 'short')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="expiresAt" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.licenseInfo.expiresAt.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="expiresAt" :value="d(admin.expiresAt, 'short')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="expiresAt-description" readonly />
              <p v-if="now < admin.expiresAt" id="expiresAt-description" class="inline-flex mt-2 text-sm text-gray-500">
                <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.expiresAt.description.valid') }}
              </p>
              <p v-else id="expiresAt-description" class="inline-flex mt-2 text-sm text-gray-500">
                <XMarkIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.expiresAt.description.expired') }}
              </p>
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-start-2">
              <button type="button" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="manageSubscription()">
                <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.manageSubscription') }}
              </button>
            </div>
          </div>
        </form>
      </section>

      <section v-if="!admin.hasLicense && remainingSeats != null" class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
        <h3 class="text-lg font-medium leading-6 text-gray-900">
          {{ t('admin.licenseInfo.title') }}
        </h3>
        <p v-if="!admin.managedInstance" class="mt-1 text-sm text-gray-500 w-full">
          {{ t('admin.licenseInfo.selfHostedNoLicense.description') }}
        </p>
        <p v-else class="mt-1 text-sm text-gray-500 w-full">
          {{ t('admin.licenseInfo.managedNoLicense.description') }}
        </p>
        <hr class="my-4 pb-6 border-gray-200"/>
        <form class="space-y-6 md:gap-6" novalidate>
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="licenseType" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.licenseInfo.type.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input v-if="!admin.managedInstance" id="licenseType" value="Community License" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              <input v-else id="licenseType" value="Managed" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="seats" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">{{ t('admin.licenseInfo.seats.title') }}</label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input id="seats" v-model="admin.licensedSeats" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="seats-description" readonly />
              <p v-if="remainingSeats > 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.seats.description.enoughSeats', [remainingSeats]) }}
              </p>
              <p v-else-if="remainingSeats == 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.seats.description.zeroSeats') }}
              </p>
              <p v-else id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                <XMarkIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.seats.description.undercutSeats', [numberOfExceededSeats]) }}
              </p>
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-start-2">
              <button type="button" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="manageSubscription()">
                <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
                {{ t('admin.licenseInfo.getLicense') }}
              </button>
            </div>
          </div>
        </form>
      </section>

      <section class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
        <h3 class="text-lg font-medium leading-6 text-gray-900">
          {{ t('admin.webOfTrust.title') }}
        </h3>
        <p class="mt-1 text-sm text-gray-500 w-full">
          {{ t('admin.webOfTrust.description') }}
        </p>
        <hr class="my-4 pb-6 border-gray-200"/>
        <form ref="form" class="space-y-6 md:gap-6" novalidate @submit.prevent="saveWebOfTrust()">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="wotMaxDepth" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('admin.webOfTrust.wotMaxDepth.title') }}
            </label>
            <div class="mt-1 md:mt-0 relative md:col-span-2 lg:col-span-1">
              <input id="wotMaxDepth" v-model="wotMaxDepth" type="number" min="0" max="9" step="1" class="focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': wotMaxDepthError instanceof FormValidationFailedError }"/>
              <div v-if="wotMaxDepthError" class="absolute left-1/2 -translate-x-1/2 -top-2 transform translate-y-[-100%] w-5/6">
                <div class="bg-red-50 border border-red-300 text-red-900 px-2 py-1 rounded shadow-sm text-sm hyphens-auto">
                  {{ t('admin.webOfTrust.wotMaxDepth.error') }}
                  <div class="absolute bottom-0 left-1/2 transform translate-y-1/2 rotate-45 w-2 h-2 bg-red-50 border-r border-b border-red-300"></div>
                </div>
              </div>
              <p class="mt-2 text-sm text-gray-500">
                {{ t('admin.webOfTrust.wotMaxDepth.description') }}
                <a href="https://docs.cryptomator.org/hub/admin/#web-of-trust" target="_blank" class="inline-flex items-center text-primary underline hover:text-primary-darker">
                  {{ t('admin.webOfTrust.information') }}
                  <ArrowRightIcon class="ml-1 h-4 w-4" aria-hidden="true" />
                </a>
              </p>
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="wotIdVerifyLen" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('admin.webOfTrust.wotIdVerifyLen.title') }}
            </label>
            <div class="mt-1 md:mt-0 relative md:col-span-2 lg:col-span-1">
              <input id="wotIdVerifyLen" v-model="wotIdVerifyLen" type="number" min="0" max="9" step="1" class="focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': wotIdVerifyLenError instanceof FormValidationFailedError }"/>
              <div v-if="wotIdVerifyLenError" class="absolute left-1/2 -translate-x-1/2 -top-2 transform translate-y-[-100%] w-5/6">
                <div class="bg-red-50 border border-red-300 text-red-900 px-2 py-1 rounded shadow-sm text-sm hyphens-auto">
                  {{ t('admin.webOfTrust.wotIdVerifyLen.error') }}
                  <div class="absolute bottom-0 left-1/2 transform translate-y-1/2 rotate-45 w-2 h-2 bg-red-50 border-r border-b border-red-300"></div>
                </div>
              </div>
              <p class="mt-2 text-sm text-gray-500">
                {{ t('admin.webOfTrust.wotIdVerifyLen.description') }}
                <a href="https://docs.cryptomator.org/hub/admin/#web-of-trust" target="_blank" class="inline-flex items-center text-primary underline hover:text-primary-darker">
                  {{ t('admin.webOfTrust.information') }}
                  <ArrowRightIcon class="ml-1 h-4 w-4" aria-hidden="true" />
                </a>
              </p>
            </div>
          </div>

          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-start-2">
              <button type="submit" :disabled="processingWot" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                <span v-if="!wotUpdated">{{ t('admin.webOfTrust.save') }}</span>
                <span v-else>{{ t('admin.webOfTrust.saved') }}</span>
              </button>
              <p v-if="onSaveErrorWot != null && !(onSaveErrorWot instanceof FormValidationFailedError)" class="mt-2 text-sm text-red-900">
                {{ t('common.unexpectedError', [onSaveErrorWot.message]) }}
              </p>
            </div>
          </div>
        </form>
      </section>

      <section class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
        <h3 class="text-lg font-medium leading-6 text-gray-900">
          {{ t('admin.emergencyAccess.title') }}
        </h3>
        <p class="mt-1 text-sm text-gray-500 w-full">
          {{ t('admin.emergencyAccess.description') }}
        </p>
        <hr class="my-4 pb-6 border-gray-200"/>

        <form ref="recoveryForm" class="space-y-6 md:gap-6" novalidate @submit.prevent="saveRecoverySettings()">
          <!-- User Selection -->
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('admin.emergencyAccess.councilMembers.title') }}
            </label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <div class="relative">
                <input v-model="userQuery" type="text" class="w-full rounded-md border border-gray-300 shadow-sm text-sm focus:ring-primary focus:border-primary" :placeholder="t('admin.emergencyAccess.councilMembers.placeholder')"/>
                <ul v-if="userQuery && searchResults.length > 0" class="absolute z-10 mt-1 max-h-60 w-full overflow-auto bg-white border border-gray-200 shadow-md rounded-md text-sm" >
                  <li v-for="user in searchResults" :key="user.id" class="flex items-center px-3 py-2 hover:bg-primary hover:text-white cursor-pointer" @click="selectUser(user)">
                    <img :src="user.pictureUrl" class="w-6 h-6 rounded-full mr-2" />
                    <span class="truncate">{{ user.name }} ({{ user.email }})</span>
                  </li>
                </ul>

                <div class="flex flex-wrap gap-2 mt-2">
                  <span v-for="user in selectedUsers" :key="user.id" class="inline-flex items-center bg-green-100 text-green-800 text-xs font-medium px-2.5 py-0.5 rounded" >
                    <img :src="user.pictureUrl" class="w-4 h-4 rounded-full mr-1"/>
                    {{ user.name }}
                    <button type="button" class="ml-1 text-green-700 hover:text-red-500" :aria-label="t('admin.recover.userFilter.remove', { name: user.name })" @click="removeUser(user)" >
                      &times;
                    </button>
                  </span>
                </div>
                <p class="mt-2 text-sm text-gray-500">
                  {{ t('admin.emergencyAccess.councilMembers.description') }}
                </p>
              </div>
            </div>
          </div>

          <!-- Default Shares -->
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('admin.emergencyAccess.defaultShares.title') }}
            </label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <input v-model="defaultRequiredEmergencyKeyShares" type="number" min="0" class="block w-full rounded-md border-gray-300 shadow-sm sm:text-sm focus:ring-primary focus:border-primary" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': defaultRequiredEmergencyKeySharesError instanceof FormValidationFailedError }" />
              <p class="mt-2 text-sm text-gray-500">
                {{ t('admin.emergencyAccess.defaultShares.description') }}
              </p>
            </div>
          </div>

          <!-- Allow Choosing Council -->
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('admin.emergencyAccess.allowChoosingCouncil.title') }}
            </label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1 flex items-center">
              <input v-model="allowChoosingEmergencyCouncil" type="checkbox" class="h-4 w-4 text-primary focus:ring-primary border-gray-300 rounded"/>
              <span class="ml-2 text-sm text-gray-500">
                {{ t('admin.emergencyAccess.allowChoosingCouncil.description') }}
              </span>
            </div>
          </div>

          <!-- Save Button -->
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-start-2">
              <button type="submit" :disabled="processingRecovery" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed">
                <span v-if="!recoveryUpdated">{{ t('common.save') }}</span>
                <span v-else>{{ t('common.saved') }}</span>
              </button>
              <p v-if="onSaveErrorRecovery && !(onSaveErrorRecovery instanceof FormValidationFailedError)" class="mt-2 text-sm text-red-900">
                {{ t('common.unexpectedError', [onSaveErrorRecovery.message]) }}
              </p>
            </div>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowRightIcon, ArrowTopRightOnSquareIcon, CheckIcon, ExclamationTriangleIcon, InformationCircleIcon, LinkIcon, XMarkIcon } from '@heroicons/vue/20/solid';
import semver from 'semver';
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { BillingDto, VersionDto, UserDto } from '../common/backend';
import config, { absFrontendBaseURL } from '../common/config';
import { FetchUpdateError, LatestVersionDto, updateChecker } from '../common/updatecheck';
import { debounce } from '../common/util';
import { Locale } from '../i18n/index';
import FetchError from './FetchError.vue';

const { t, d, locale, fallbackLocale } = useI18n({ useScope: 'global' });

const props = defineProps<{
  token?: string
}>();

const userQuery = ref('');
const allUsers = ref<UserDto[]>([]);
const searchResults = ref<UserDto[]>([]);
const selectedUsers = ref<UserDto[]>([]);

const recoveryUpdated = ref(false);
const debouncedRecoveryUpdated = debounce(() => recoveryUpdated.value = false, 2000);

const version = ref<VersionDto>();
const latestVersion = ref<LatestVersionDto>();
const admin = ref<BillingDto>();
const now = ref<Date>(new Date());
const keycloakAdminRealmURL = ref<string>();
const wotMaxDepth = ref<number>();
const wotIdVerifyLen = ref<number>();
const defaultRequiredEmergencyKeyShares = ref<number>();
const allowChoosingEmergencyCouncil = ref<boolean>();
const wotUpdated = ref(false);
const debouncedWotUpdated = debounce(() => wotUpdated.value = false, 2000);
const form = ref<HTMLFormElement>();
const onFetchError = ref<Error | null>(null);
const processingWot = ref(false);
const processingRecovery = ref(false);
const onSaveErrorWot = ref<Error | null>(null);
const onSaveErrorRecovery = ref<Error | null>(null);
const errorOnFetchingUpdates = ref<boolean>(false);
const wotMaxDepthError = ref<Error | null >(null);
const wotIdVerifyLenError = ref<Error | null >(null);
const defaultRequiredEmergencyKeySharesError = ref<Error | null>(null);

class FormValidationFailedError extends Error {
  constructor() {
    super('The form is invalid.');
  }
}

const isBeta = computed(() => {
  if (version.value && semver.valid(version.value.hubVersion)) {
    return semver.prerelease(version.value.hubVersion ?? '0.1.0') != null;
  }
  return false;
});
const stableUpdateExists = computed(() => {
  if (version.value && semver.valid(version.value.hubVersion) && latestVersion.value?.stable) {
    return semver.lt(version.value.hubVersion, latestVersion.value.stable ?? '0.1.0');
  }
  return false;
});
const betaUpdateExists = computed(() => {
  if (version.value && semver.valid(version.value.hubVersion) && latestVersion.value?.beta) {
    return semver.lt(version.value.hubVersion, latestVersion.value.beta ?? '0.1.0-beta1');
  }
  return false;
});

const remainingSeats = computed(() => admin.value ? admin.value.licensedSeats - admin.value.usedSeats : undefined);
const numberOfExceededSeats = computed(() => {
  if (remainingSeats.value === undefined) {
    return undefined;
  }
  return remainingSeats.value < 0 ? Math.abs(remainingSeats.value) : 0;
});

onMounted(async () => {
  const cfg = config.get();
  keycloakAdminRealmURL.value = `${cfg.keycloakUrl}/admin/${cfg.keycloakRealm}/console`;
  if (props.token) {
    await setToken(props.token);
  }
  await fetchData();
});

async function setToken(token: string) {
  try {
    await backend.billing.setToken(token);
  } catch (error) {
    console.error('Setting token failed.', error);
  }
}

async function fetchData() {
  try {
    const versionDto = backend.version.get();
    const versionAvailable = versionDto.then(versionDto => updateChecker.get(versionDto.hubVersion));
    admin.value = await backend.billing.get();
    version.value = await versionDto;
    latestVersion.value = await versionAvailable;
    allUsers.value = await backend.users.listAll();
    const settings = await backend.settings.get();
    selectedUsers.value = settings.emergencyCouncilMemberIds
      .map(id => allUsers.value.find(u => u.id === id))
      .filter((u): u is UserDto => u != null);
    wotMaxDepth.value = settings.wotMaxDepth;
    wotIdVerifyLen.value = settings.wotIdVerifyLen;
    defaultRequiredEmergencyKeyShares.value = settings.defaultRequiredEmergencyKeyShares;
    allowChoosingEmergencyCouncil.value = settings.allowChoosingEmergencyCouncil;
  } catch (error) {
    if (error instanceof FetchUpdateError) {
      errorOnFetchingUpdates.value = true;
    } else {
      console.error('Retrieving server information failed.', error);
      onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
}

watch(userQuery, (newQuery) => {
  if (newQuery && newQuery.trim().length > 0) {
    searchAuthority(newQuery.trim());
  } else {
    searchResults.value = [];
  }
});

function searchAuthority(query: string) {
  searchResults.value = allUsers.value
    .filter(user =>
      user.name.toLowerCase().includes(query.toLowerCase()) &&
      !selectedUsers.value.some(su => su.id === user.id)
    )
    .sort((a, b) => a.name.localeCompare(b.name));
}

function selectUser(user: UserDto) {
  selectedUsers.value.push(user);
  userQuery.value = '';
  searchResults.value = [];
}

function removeUser(user: UserDto) {
  selectedUsers.value = selectedUsers.value.filter(u => u.id !== user.id);
}

function manageSubscription() {
  const returnUrl = `${absFrontendBaseURL}admin`;
  const supportedLanguages = [Locale.EN, Locale.DE];
  const supportedLanguagePathComponents = Object.fromEntries(supportedLanguages.map(lang => [lang, lang == Locale.EN ? '' : `${lang}/`]));
  const languagePathComponent = supportedLanguagePathComponents[(locale.value as string).split('-')[0]] ?? supportedLanguagePathComponents[fallbackLocale.value as string] ?? '';
  window.open(`https://cryptomator.org/${languagePathComponent}hub/billing/?hub_id=${admin.value?.hubId}&return_url=${encodeURIComponent(returnUrl)}`, '_self');
}

async function saveWebOfTrust() {
  onSaveErrorWot.value = null;
  wotMaxDepthError.value = null;
  wotIdVerifyLenError.value = null;

  if (admin.value == null || wotMaxDepth.value == null || wotIdVerifyLen.value == null) {
    throw new Error('No data available.');
  }
  if (!form.value?.checkValidity()) {
    if (wotMaxDepth.value < 0 || wotMaxDepth.value > 9) {
      wotMaxDepthError.value = new FormValidationFailedError();
    }
    if (wotIdVerifyLen.value < 0) {
      wotIdVerifyLenError.value = new FormValidationFailedError();
    }
    return;
  }

  try {
    processingWot.value = true;
    const settings = {
      wotMaxDepth: wotMaxDepth.value,
      wotIdVerifyLen: wotIdVerifyLen.value,
      hubId: admin.value.hubId
    };
    await backend.settings.update(settings);
    wotUpdated.value = true;
    debouncedWotUpdated();
  } catch (error) {
    console.error('Failed to save settings:', error);
    onSaveErrorWot.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processingWot.value = false;
  }
}

async function saveRecoverySettings() {
  onSaveErrorRecovery.value = null;
  defaultRequiredEmergencyKeySharesError.value = null;

  if (admin.value == null || defaultRequiredEmergencyKeyShares.value == null || allowChoosingEmergencyCouncil.value == null) {
    throw new Error('No data available.');
  }

  if (defaultRequiredEmergencyKeyShares.value < 0) {
    defaultRequiredEmergencyKeySharesError.value = new FormValidationFailedError();
    return;
  }

  try {
    processingRecovery.value = true;
    const settings = {
      hubId: admin.value.hubId,
      defaultRequiredEmergencyKeyShares: defaultRequiredEmergencyKeyShares.value,
      allowChoosingEmergencyCouncil: allowChoosingEmergencyCouncil.value,
      emergencyCouncilMemberIds: selectedUsers.value.map(u => u.id)
    };
    await backend.settings.update(settings);
    recoveryUpdated.value = true;
    debouncedRecoveryUpdated();
  } catch (error) {
    console.error('Failed to save recovery settings:', error);
    onSaveErrorRecovery.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processingRecovery.value = false;
  }
}
</script>
