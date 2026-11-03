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
        Your account keys were not recognized
      </h1>
      <div class="mt-6 space-y-3 text-sm text-gray-500 text-left">
        <p>
          This browser remembers the keys of your account. The keys the server just delivered are different ones.
        </p>
        <p>
          There are two possible reasons. Either your account was reset or restored from a backup, in which case the change is expected. Or someone with access to the server is trying to make this browser use keys they control, so that vaults you create or share would be readable by them. Hub stopped before any vault key was used.
        </p>
        <p class="font-medium text-gray-700">
          If you did not reset your account and are not aware of a restore, do not continue. Contact your administrator and report what you see on this page.
        </p>
        <p>
          If the change is expected, you can re-authorize this browser with your Account Key. The key never leaves this browser, so this step is safe even if the server has been tampered with. It does not repair a tampered server; it only confirms that the keys the server delivered are really yours.
        </p>
      </div>
      <div class="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
        <router-link to="/app/logout" class="inline-flex justify-center items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-xs text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">Log out</router-link>
        <router-link to="/app/setup" class="inline-flex justify-center items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md shadow-xs text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">Re-authorize this browser…</router-link>
      </div>
    </div>
  </div>
</template>
