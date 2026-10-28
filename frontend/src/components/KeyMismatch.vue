<template>
  <nav class="bg-tertiary2">
    <div class="max-w-7xl mx-auto sm:px-6 lg:px-8 h-16 flex-1 flex items-center justify-center sm:items-stretch sm:justify-start">
      <div class="shrink-0 flex items-center">
        <img src="/logo.svg" class="h-8" alt="Logo" />
        <span class="font-headline font-bold text-white ml-2 pb-px">CRYPTOMATOR HUB</span>
      </div>
    </div>
  </nav>

  <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8 flex justify-center">
    <div class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
      <div class="flex justify-center mb-3 sm:mb-5">
        <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
      </div>
      <h1 class="text-2xl leading-6 font-medium text-gray-900">
        Account keys could not be verified
      </h1>
      <p class="mt-6 text-sm text-gray-500">
        The public keys stored on this server for your account do not belong to your private keys. This can be a sign that the server has been tampered with.
      </p>
      <p class="mt-3 text-sm text-gray-500">
        Do not create, recover or share any vaults until the cause has been resolved. Please contact your administrator.
      </p>
      <div class="mt-6">
        <router-link to="/app/logout" class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-xs text-white bg-primary focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">Log out</router-link>
      </div>
    </div>
  </div>
</template>
