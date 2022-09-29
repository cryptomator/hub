<template>
  <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">
    <div class="pt-8 pb-4 shrink-0 flex items-center">
      <img src="/logo.svg" class="h-8" alt="Logo"/>
      <span class="font-headline font-bold text-primary ml-2 pb-px">CRYPTOMATOR HUB</span>
    </div>

    <div class="relative shadow-xl sm:rounded-2xl sm:overflow-hidden">
      <div class="absolute inset-0">
        <div class="absolute inset-0 bg-gradient-to-r from-primary-l1 to-primary mix-blend-multiply" />
      </div>
      <div class="relative px-4 py-16 sm:px-6 sm:py-24 lg:py-32 lg:px-8">
        <h1 class="text-center text-4xl font-extrabold tracking-tight sm:text-5xl lg:text-6xl text-white">
          Welcome!
        </h1>
        <p class="mt-6 max-w-lg mx-auto text-center text-xl text-primary-l2 sm:max-w-3xl">
          Please sign in to continue.
        </p>
        <div class="mt-10 max-w-sm mx-auto sm:max-w-none sm:flex sm:justify-center">
          <div class="space-y-4 sm:space-y-0 sm:mx-auto">
            <router-link to="/app/vaults" class="flex items-center justify-center px-4 py-3 border border-transparent text-base font-medium rounded-md shadow-sm text-primary-d1 bg-white hover:bg-primary-l2 sm:px-8">
              Continue to Login
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
