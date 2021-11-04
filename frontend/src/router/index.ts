import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router';
import authPromise from '../common/auth';
import backend from '../common/backend';
import config from '../common/config';
import AddDevice from '../components/AddDevice.vue';
import CreateVault from '../components/CreateVault.vue';
import HelloWorld from '../components/HelloWorld.vue';
import LogoutComponent from '../components/Logout.vue';
import SetupComponent from "../components/Setup.vue";
import UnlockError from '../components/UnlockError.vue';
import UnlockSuccess from '../components/UnlockSuccess.vue';
import UserDetails from '../components/UserDetails.vue';
import VaultDetails from '../components/VaultDetails.vue';
import VaultList from "../components/VaultList.vue";

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    end: true,
    //component: VaultList,
    components: {
      default: VaultList,

      end: HelloWorld
    },
  },
  /*{
    path: '/vaults',
    component: Vaults,
    children: [
      {
        path: 'create',
        component: CreateVault
      },
      {
        path: ':uuid/unlock',
        component: UnlockVault
      }
    ]
  },*/
  {
    path: '/devices/add',
    component: AddDevice,
    props: (route) => ({ deviceId: route.query.device_id, deviceKey: route.query.device_key, verificationHash: route.query.verification_hash })
  },
  {
    path: '/user',
    component: UserDetails,
    props: (route) => ({ vaultId: route.params.id })
  },
  {
    path: '/vaults/create',
    component: CreateVault
  },
  {
    path: '/vaults/:id',
    component: VaultDetails,
    props: (route) => ({ vaultId: route.params.id })
  },
  {
    path: '/unlock-success',
    component: UnlockSuccess
  },
  {
    path: '/unlock-error',
    component: UnlockError,
    meta: { skipAuth: true }
  },
  {
    path: '/logout',
    component: LogoutComponent,
    meta: { skipAuth: true }
  },
  {
    path: '/setup',
    component: SetupComponent,
    meta: { skipAuth: true, skipSetup: true }
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes: routes
});

// FIRST we must check whether the setup wizard ran
router.beforeEach((to, from, next) => {
  if (to.meta.skipSetup) {
    next();
  } else if (config.get().setupCompleted) {
    next();
  } else {
    next({ path: '/setup' });
  }
});

// SECOND check auth (requires setup)
router.beforeEach((to, from, next) => {
  if (to.meta.skipAuth) {
    next();
  } else {
    const redirectUri = `${window.location.protocol}//${window.location.host}${import.meta.env.BASE_URL}#${to.fullPath}`
    authPromise.then(async auth => {
      await auth.loginIfRequired(redirectUri);
      next();
    });
  }
});

// THIRD update user data (requires auth)
router.beforeEach((to, from, next) => {
  if ('login' in to.query) {
    authPromise.then(async auth => {
      if (auth.isAuthenticated()) {
        await backend.users.syncMe();
      }
    }).finally(() => {
      next();
    });
  } else {
    next();
  }
});

export default router;
