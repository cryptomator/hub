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
              <button type="submit" :disabled="processing" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                <span v-if="!wotUpdated">{{ t('admin.webOfTrust.save') }}</span>
                <span v-else>{{ t('admin.webOfTrust.saved') }}</span>
              </button>
              <p v-if="onSaveError != null && !(onSaveError instanceof FormValidationFailedError)" class="mt-2 text-sm text-red-900">
                {{ t('common.unexpectedError', [onSaveError.message]) }}
              </p>
            </div>
          </div>
        </form>
      </section>

      <section class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
        <h3 class="text-lg font-medium leading-6 text-gray-900">
          {{ t('admin.recover.title') }}
        </h3>
        <p class="mt-1 text-sm text-gray-500 w-full">
          {{ t('admin.recover.description') }}
        </p>
        <hr class="my-4 pb-6 border-gray-200"/>
        <form ref="form" class="space-y-6 md:gap-6" novalidate @submit.prevent="saveWebOfTrust()">    
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="userFilter" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('admin.recover.title') }}
            </label>
            <div class="mt-1 md:mt-0 md:col-span-2 lg:col-span-1">
              <div class="relative">
                <input v-model="userQuery" type="text" class="w-full rounded-md border border-gray-300 shadow-sm text-sm focus:ring-primary focus:border-primary" :placeholder="t('admin.recover.userFilter.placeholder')" />
                <ul v-if="userQuery && filteredUsers.length > 0" class="absolute z-10 mt-1 max-h-60 w-full overflow-auto bg-white border border-gray-200 shadow-md rounded-md text-sm">
                  <li v-for="user in filteredUsers" :key="user.id" class="flex items-center px-3 py-2 hover:bg-primary hover:text-white cursor-pointer" @click="selectUser(user)">
                    <img :src="user.userPicture" class="w-6 h-6 rounded-full mr-2" />
                    <span class="truncate">{{ user.name }} ({{ user.username }})</span>
                  </li>
                </ul>

                <div class="flex flex-wrap gap-2 mt-2">
                  <span v-for="user in selectedUsers" :key="user.id" class="inline-flex items-center bg-green-100 text-green-800 text-xs font-medium px-2.5 py-0.5 rounded">
                    <img :src="user.userPicture" class="w-4 h-4 rounded-full mr-1" />
                    {{ user.name }}
                    <button type="button" class="ml-1 text-green-700 hover:text-red-500" :aria-label="t('admin.recover.userFilter.remove', { name: user.name })" @click="removeUser(user)">
                      &times;
                    </button>
                  </span>
                </div>
                <p class="mt-2 text-sm text-gray-500">{{ t('admin.recover.userFilter.description') }}</p>
              </div>
            </div>
          </div>
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <label for="wotIdVerifyLen" class="block text-sm font-medium text-gray-700 md:text-right md:pr-4 md:mt-2">
              {{ t('admin.recover.amount.title') }}
            </label>
            <div class="mt-1 md:mt-0 relative md:col-span-2 lg:col-span-1">
              <input id="wotIdVerifyLen" v-model="wotIdVerifyLen" type="number" min="0" max="9" step="1" class="focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': wotIdVerifyLenError instanceof FormValidationFailedError }"/>
              <div v-if="wotIdVerifyLenError" class="absolute left-1/2 -translate-x-1/2 -top-2 transform translate-y-[-100%] w-5/6">
                <div class="bg-red-50 border border-red-300 text-red-900 px-2 py-1 rounded shadow-sm text-sm hyphens-auto">
                  {{ t('admin.recover.amount.error') }}
                  <div class="absolute bottom-0 left-1/2 transform translate-y-1/2 rotate-45 w-2 h-2 bg-red-50 border-r border-b border-red-300"></div>
                </div>
              </div>
              <p class="mt-2 text-sm text-gray-500">
                {{ t('admin.recover.amount.description') }}
                <a href="https://docs.cryptomator.org/en/latest/security/hub/#web-of-trust" target="_blank" class="inline-flex items-center text-primary underline hover:text-primary-darker">
                  {{ t('admin.webOfTrust.information') }}
                  <ArrowRightIcon class="ml-1 h-4 w-4" aria-hidden="true" />
                </a>
              </p>
            </div>
          </div>    
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-start-2">
              <button type="submit" :disabled="true" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                <span v-if="!wotUpdated">{{ t('admin.webOfTrust.save') }}</span>
                <span v-else>{{ t('admin.webOfTrust.saved') }}</span>
              </button>
              <p v-if="onSaveError != null && !(onSaveError instanceof FormValidationFailedError)" class="mt-2 text-sm text-red-900">
                {{ t('common.unexpectedError', [onSaveError.message]) }}
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
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { BillingDto, VersionDto } from '../common/backend';
import config, { absFrontendBaseURL } from '../common/config';
import { FetchUpdateError, LatestVersionDto, updateChecker } from '../common/updatecheck';
import { debounce } from '../common/util';
import { Locale } from '../i18n/index';
import FetchError from './FetchError.vue';

const { t, d, locale, fallbackLocale } = useI18n({ useScope: 'global' });

const props = defineProps<{
  token?: string
}>();
type RecoverUser = { id: string; name: string; username: string; userPicture: string };

const dummyUsers = ref<RecoverUser[]>([
  { id: 'u1', name: 'Alice Admin', username: 'alice', userPicture: 'https://i.pravatar.cc/50?u=alice' },
  { id: 'u2', name: 'Bob Benutzer', username: 'bob', userPicture: 'https://i.pravatar.cc/50?u=bob' },
  { id: 'u3', name: 'Clara Crypt', username: 'clara', userPicture: 'https://i.pravatar.cc/50?u=clara' },
  { id: 'u4', name: 'David Dev', username: 'david', userPicture: 'https://i.pravatar.cc/50?u=david' },
  { id: 'u5', name: 'Ella Engineer', username: 'ella', userPicture: 'https://i.pravatar.cc/50?u=ella' },
  { id: 'u6', name: 'Felix Frontend', username: 'felix', userPicture: 'https://i.pravatar.cc/50?u=felix' },
  { id: 'u7', name: 'Greta Guest', username: 'greta', userPicture: 'https://i.pravatar.cc/50?u=greta' },
  { id: 'u8', name: 'Hannes Hacker', username: 'hannes', userPicture: 'https://i.pravatar.cc/50?u=hannes' },
  { id: 'u9', name: 'Ines Integrator', username: 'ines', userPicture: 'https://i.pravatar.cc/50?u=ines' },
  { id: 'u10', name: 'Jonas JSON', username: 'jonas', userPicture: 'https://i.pravatar.cc/50?u=jonas' },
  { id: 'u11', name: 'Karla Keycloak', username: 'karla', userPicture: 'https://i.pravatar.cc/50?u=karla' },
  { id: 'u12', name: 'Lars Logger', username: 'lars', userPicture: 'https://i.pravatar.cc/50?u=lars' },
  { id: 'u13', name: 'Mona Maintainer', username: 'mona', userPicture: 'https://i.pravatar.cc/50?u=mona' },
  { id: 'u14', name: 'Nico Network', username: 'nico', userPicture: 'https://i.pravatar.cc/50?u=nico' },
  { id: 'u15', name: 'Olivia Operator', username: 'olivia', userPicture: 'https://i.pravatar.cc/50?u=olivia' },
  { id: 'u16', name: 'Paul Parser', username: 'paul', userPicture: 'https://i.pravatar.cc/50?u=paul' },
  { id: 'u17', name: 'Quentin Query', username: 'quentin', userPicture: 'https://i.pravatar.cc/50?u=quentin' },
  { id: 'u18', name: 'Rita Refactor', username: 'rita', userPicture: 'https://i.pravatar.cc/50?u=rita' },
  { id: 'u19', name: 'Sam Security', username: 'sam', userPicture: 'https://i.pravatar.cc/50?u=sam' },
  { id: 'u20', name: 'Tina Tester', username: 'tina', userPicture: 'https://i.pravatar.cc/50?u=tina' },
  { id: 'u21', name: 'Uwe UI', username: 'uwe', userPicture: 'https://i.pravatar.cc/50?u=uwe' },
  { id: 'u22', name: 'Vera Validator', username: 'vera', userPicture: 'https://i.pravatar.cc/50?u=vera' },
  { id: 'u23', name: 'Willy Worker', username: 'willy', userPicture: 'https://i.pravatar.cc/50?u=willy' },
  { id: 'u24', name: 'Xenia XML', username: 'xenia', userPicture: 'https://i.pravatar.cc/50?u=xenia' },
  { id: 'u25', name: 'Yann YAML', username: 'yann', userPicture: 'https://i.pravatar.cc/50?u=yann' },
  { id: 'u26', name: 'Zoe Zero', username: 'zoe', userPicture: 'https://i.pravatar.cc/50?u=zoe' },
  { id: 'u27', name: 'Andreas Admin', username: 'andreas', userPicture: 'https://i.pravatar.cc/50?u=andreas' },
  { id: 'u28', name: 'Bina Backend', username: 'bina', userPicture: 'https://i.pravatar.cc/50?u=bina' },
  { id: 'u29', name: 'Chris Crypt', username: 'chris', userPicture: 'https://i.pravatar.cc/50?u=chris' },
  { id: 'u30', name: 'Doro DevOps', username: 'doro', userPicture: 'https://i.pravatar.cc/50?u=doro' },
  { id: 'u31', name: 'Emil Email', username: 'emil', userPicture: 'https://i.pravatar.cc/50?u=emil' },
  { id: 'u32', name: 'Fiona Front', username: 'fiona', userPicture: 'https://i.pravatar.cc/50?u=fiona' },
  { id: 'u33', name: 'Gustav Git', username: 'gustav', userPicture: 'https://i.pravatar.cc/50?u=gustav' },
  { id: 'u34', name: 'Heidi Helper', username: 'heidi', userPicture: 'https://i.pravatar.cc/50?u=heidi' },
  { id: 'u35', name: 'Isa Inspector', username: 'isa', userPicture: 'https://i.pravatar.cc/50?u=isa' },
  { id: 'u36', name: 'Jakob JSON', username: 'jakob', userPicture: 'https://i.pravatar.cc/50?u=jakob' },
  { id: 'u37', name: 'Kim Kotlin', username: 'kim', userPicture: 'https://i.pravatar.cc/50?u=kim' },
  { id: 'u38', name: 'Lea Linter', username: 'lea', userPicture: 'https://i.pravatar.cc/50?u=lea' },
  { id: 'u39', name: 'Marc Markdown', username: 'marc', userPicture: 'https://i.pravatar.cc/50?u=marc' },
  { id: 'u40', name: 'Nadine Node', username: 'nadine', userPicture: 'https://i.pravatar.cc/50?u=nadine' },
  { id: 'u41', name: 'Omar OAuth', username: 'omar', userPicture: 'https://i.pravatar.cc/50?u=omar' },
  { id: 'u42', name: 'Petra Post', username: 'petra', userPicture: 'https://i.pravatar.cc/50?u=petra' },
  { id: 'u43', name: 'Quinn Queue', username: 'quinn', userPicture: 'https://i.pravatar.cc/50?u=quinn' },
  { id: 'u44', name: 'Rolf REST', username: 'rolf', userPicture: 'https://i.pravatar.cc/50?u=rolf' },
  { id: 'u45', name: 'Sina Socket', username: 'sina', userPicture: 'https://i.pravatar.cc/50?u=sina' },
  { id: 'u46', name: 'Tim Token', username: 'tim', userPicture: 'https://i.pravatar.cc/50?u=tim' },
  { id: 'u47', name: 'Uli Upload', username: 'uli', userPicture: 'https://i.pravatar.cc/50?u=uli' },
  { id: 'u48', name: 'Viktor Vault', username: 'viktor', userPicture: 'https://i.pravatar.cc/50?u=viktor' },
  { id: 'u49', name: 'Wanda Web', username: 'wanda', userPicture: 'https://i.pravatar.cc/50?u=wanda' },
  { id: 'u50', name: 'Xaver XML', username: 'xaver', userPicture: 'https://i.pravatar.cc/50?u=xaver' }
]);

const selectedUsers = ref<typeof dummyUsers.value>([]);

function selectUser(user: RecoverUser) {
  if (!selectedUsers.value.find(u => u.id === user.id)) {
    selectedUsers.value.push(user);
  }
  userQuery.value = '';
}

function removeUser(user: RecoverUser) {
  selectedUsers.value = selectedUsers.value.filter(u => u.id !== user.id);
}

const userQuery = ref('');
const filteredUsers = computed(() => {
  const q = userQuery.value.toLowerCase();
  return dummyUsers.value.filter(
    u =>
      !selectedUsers.value.some(su => su.id === u.id) &&
      (u.name.toLowerCase().includes(q) || u.username.toLowerCase().includes(q))
  );
});

const version = ref<VersionDto>();
const latestVersion = ref<LatestVersionDto>();
const admin = ref<BillingDto>();
const now = ref<Date>(new Date());
const keycloakAdminRealmURL = ref<string>();
const wotMaxDepth = ref<number>();
const wotIdVerifyLen = ref<number>();
const wotUpdated = ref(false);
const debouncedWotUpdated = debounce(() => wotUpdated.value = false, 2000);
const form = ref<HTMLFormElement>();
const processing = ref(false);
const onFetchError = ref<Error | null>();
const errorOnFetchingUpdates = ref<boolean>(false);
const onSaveError = ref<Error | null>(null);
const wotMaxDepthError = ref<Error | null >(null);
const wotIdVerifyLenError = ref<Error | null >(null);

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
    
    const settings = await backend.settings.get();
    wotMaxDepth.value = settings.wotMaxDepth;
    wotIdVerifyLen.value = settings.wotIdVerifyLen;
  } catch (error) {
    if (error instanceof FetchUpdateError) {
      errorOnFetchingUpdates.value = true;
    } else {
      console.error('Retrieving server information failed.', error);
      onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
}

function manageSubscription() {
  const returnUrl = `${absFrontendBaseURL}admin`;
  const supportedLanguages = [Locale.EN, Locale.DE];
  const supportedLanguagePathComponents = Object.fromEntries(supportedLanguages.map(lang => [lang, lang == Locale.EN ? '' : `${lang}/`]));
  const languagePathComponent = supportedLanguagePathComponents[(locale.value as string).split('-')[0]] ?? supportedLanguagePathComponents[fallbackLocale.value as string] ?? '';
  window.open(`https://cryptomator.org/${languagePathComponent}hub/billing/?hub_id=${admin.value?.hubId}&return_url=${encodeURIComponent(returnUrl)}`, '_self');
}

async function saveWebOfTrust() {
  onSaveError.value = null;
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
    processing.value = true;
    const settings = {
      wotMaxDepth: wotMaxDepth.value,
      wotIdVerifyLen: wotIdVerifyLen.value,
      hubId: admin.value.hubId
    };
    await backend.settings.put(settings);
    wotUpdated.value = true;
    debouncedWotUpdated();
  } catch (error) {
    console.error('Failed to save settings:', error);
    onSaveError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

</script>
