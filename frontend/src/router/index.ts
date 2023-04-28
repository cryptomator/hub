import { createRouter, createWebHistory, RouteLocationRaw, RouteRecordRaw } from 'vue-router';
import authPromise from '../common/auth';
import backend from '../common/backend';
import { baseURL } from '../common/config';
import AdminSettings from '../components/AdminSettings.vue';
import AuthenticatedMain from '../components/AuthenticatedMain.vue';
import CreateVault from '../components/CreateVault.vue';
import DeviceList from '../components/DeviceList.vue';
import NotFound from '../components/NotFound.vue';
import UnlockError from '../components/UnlockError.vue';
import UnlockSuccess from '../components/UnlockSuccess.vue';
import UserSettings from '../components/UserSettings.vue';
import VaultDetails from '../components/VaultDetails.vue';
import VaultList from '../components/VaultList.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/app'
  },
  {
    path: '/app',
    redirect: '/app/vaults'
  },
  {
    path: '/app/logout',
    component: AuthenticatedMain, // any component will do
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
    path: '/app', /* required but unused */
    component: AuthenticatedMain,
    children: [
      {
        path: 'vaults',
        component: VaultList
      },
      {
        path: 'vaults/create',
        component: CreateVault,
        props: () => ({ recover: false })
      },
      {
        path: 'vaults/recover',
        component: CreateVault,
        props: () => ({ recover: true })
      },
      {
        path: 'vaults/:id',
        component: VaultDetails,
        props: (route) => ({ vaultId: route.params.id })
      },
      {
        path: 'devices',
        component: DeviceList
      },
      {
        path: 'settings',
        component: UserSettings
      },
      {
        path: 'admin',
        component: AdminSettings,
        props: (route) => ({ token: route.query.token }),
        beforeEnter: async () => {
          const auth = await authPromise;
          return auth.isAdmin(); //TODO: reroute to NotFound Screen/ AccessDeniedScreen?
        }
      },
    ]
  },
  {
    path: '/app/unlock-success',
    component: UnlockSuccess,
    props: (route) => ({ vaultId: route.query.vault, deviceId: route.query.device })
  },
  {
    path: '/app/unlock-error',
    component: UnlockError,
    meta: { skipAuth: true }
  },
  {
    path: '/app/:pathMatch(.+)', //necessary due to using history mode in router
    component: NotFound,
    meta: { skipAuth: true }
  },
];

const router = createRouter({
  history: createWebHistory(baseURL),
  routes: routes,
});

// FIRST check auth
router.beforeEach((to, from, next) => {
  if (to.meta.skipAuth) {
    next();
  } else {
    authPromise.then(async auth => {
      if (auth.isAuthenticated()) {
        next();
      } else {
        const redirect: RouteLocationRaw = { query: { ...to.query, 'sync_me': null } };
        const redirectUri = `${location.origin}${router.resolve(redirect, to).href}`;
        auth.login(redirectUri);
      }
    });
  }
});

// SECOND update user data (requires auth)
router.beforeEach((to, from, next) => {
  if ('sync_me' in to.query) {
    authPromise.then(async auth => {
      if (auth.isAuthenticated()) {
        await backend.users.syncMe();
      }
    }).finally(async () => {
      let me = await backend.users.me();
      if (!me.publicKey) {
        // redirect to user setup
        next({ path: '/app/settings' });
      } else {
        // remove sync_me query parameter to avoid endless recursion
        delete to.query.sync_me;
        next({ path: to.path, query: to.query, params: to.params, replace: true });
      }
    });
  } else {
    next();
  }
});

export default router;
