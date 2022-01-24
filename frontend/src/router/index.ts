import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router';
import authPromise from '../common/auth';
import backend from '../common/backend';
import config from '../common/config';
import CreateVault from '../components/CreateVault.vue';
import DeviceList from '../components/DeviceList.vue';
import LoginComponent from '../components/Login.vue';
import LogoutComponent from '../components/Logout.vue';
import MainComponent from '../components/Main.vue';
import ServiceUnavailable from '../components/ServiceUnavailable.vue';
import Settings from '../components/Settings.vue';
import SetupComponent from '../components/Setup.vue';
import UnlockError from '../components/UnlockError.vue';
import UnlockSuccess from '../components/UnlockSuccess.vue';
import VaultDetails from '../components/VaultDetails.vue';
import VaultList from '../components/VaultList.vue';
import vaultListGuard from './vaultListGuard';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: LoginComponent,
    meta: { skipAuth: true },
    beforeEnter: (to, from, next) => {
      authPromise.then(async auth => {
        if (auth.isAuthenticated()) {
          next('/vaults');
        } else {
          next();
        }
      }).catch(error => {
        next(error);
      });
    }
  },
  {
    path: '/setup',
    component: SetupComponent,
    meta: { skipAuth: true, skipSetup: true }
  },
  {
    path: '/logout',
    component: LogoutComponent,
    beforeEnter: (to, from, next) => {
      authPromise.then(async auth => {
        if (auth.isAuthenticated()) {
          const loggedOutUri = `${location.origin}/${router.resolve('/').href}`;
          await auth.logout(loggedOutUri);
        } else {
          next();
        }
      }).catch(error => {
        next(error);
      });
    }
  },
  {
    path: '/', /* required but unused */
    component: MainComponent,
    children: [
      {
        path: '/vaults',
        component: VaultList,
        beforeEnter: vaultListGuard.fetch,
        props: vaultListGuard.copy
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
        path: '/devices',
        component: DeviceList
      },
      {
        path: '/settings',
        component: Settings
      },
      {
        path: '/error',
        component: ServiceUnavailable
      }
    ]
  },
  {
    path: '/unlock-success',
    component: UnlockSuccess,
    props: (route) => ({ vaultId: route.query.vault, deviceId: route.query.device })
  },
  {
    path: '/unlock-error',
    component: UnlockError,
    meta: { skipAuth: true }
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
    const redirectUri = `${window.location.protocol}//${window.location.host}${import.meta.env.BASE_URL}#${to.fullPath}`;
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
