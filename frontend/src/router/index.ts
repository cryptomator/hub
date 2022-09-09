import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import authPromise from '../common/auth';
import backend from '../common/backend';
import { frontendBaseURL } from '../common/config';
import AdminSettings from '../components/AdminSettings.vue';
import CreateVault from '../components/CreateVault.vue';
import DeviceList from '../components/DeviceList.vue';
import LoginComponent from '../components/Login.vue';
import LogoutComponent from '../components/Logout.vue';
import MainComponent from '../components/Main.vue';
import NotFoundComponent from '../components/NotFoundComponent.vue';
import Settings from '../components/Settings.vue';
import UnlockError from '../components/UnlockError.vue';
import UnlockSuccess from '../components/UnlockSuccess.vue';
import VaultDetails from '../components/VaultDetails.vue';
import VaultList from '../components/VaultList.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: LoginComponent,
    meta: { skipAuth: true },
    beforeEnter: async (_to, _from) => {
      const auth = await authPromise;
      if (auth.isAuthenticated()) {
        return '/vaults';  //TODO:currently not working, since silent single sign-on is missing
      }
    }
  },
  {
    path: '/logout',
    component: LogoutComponent,
    meta: { skipAuth: true },
    beforeEnter: (to, from, next) => {
      authPromise.then(async auth => {
        if (auth.isAuthenticated()) {
          const loggedOutUri = `${location.origin}${router.resolve('/').href}`;
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
        component: VaultList
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
        path: '/admin',
        component: AdminSettings,
        props: (route) => ({ token: route.query.token }),
        beforeEnter: async (_to, _from) => {
          const auth = await authPromise;
          return auth.isAdmin(); //TODO: reroute to NotFound Screen/ AccessDeniedScreen?
        }
      },
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
  },
  {
    path: '/:catchAll(.*)', //necessary due to using history mode in router
    component: NotFoundComponent,
    name: 'NotFound'
  },
];

const router = createRouter({
  history: createWebHistory(frontendBaseURL),
  routes: routes,
});

// FIRST check auth
router.beforeEach((to, from, next) => {
  if (to.meta.skipAuth) {
    next();
  } else {
    const redirectUri = `${location.origin}${router.resolve(to).href}`;
    authPromise.then(async auth => {
      await auth.loginIfRequired(redirectUri);
      next();
    });
  }
});

// SECOND update user data (requires auth)
router.beforeEach((to, from, next) => {
  if ('sync_me' in to.query) {
    authPromise.then(async auth => {
      if (auth.isAuthenticated()) {
        await backend.users.syncMe();
        delete to.query.sync_me; // remove sync_me query parameter to avoid endless recursion
        next({ path: to.path, query: to.query, params: to.params, replace: true });
      } else {
        next();
      }
    }).catch(() => {
      next();
    });
  } else {
    next();
  }
});

export default router;
