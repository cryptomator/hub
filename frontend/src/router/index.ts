import { createRouter, createWebHistory, NavigationGuardWithThis, RouteLocationRaw, RouteRecordRaw } from 'vue-router';
import authPromise from '../common/auth';
import backend from '../common/backend';
import { baseURL } from '../common/config';
import userdata from '../common/userdata';
import AdminSettings from '../components/AdminSettings.vue';
import AuditLog from '../components/AuditLog.vue';
import AuthenticatedMain from '../components/AuthenticatedMain.vue';
import AuthorityList from '../components/AuthorityList.vue';
import CreateVault from '../components/CreateVault.vue';
import Forbidden from '../components/Forbidden.vue';
import InitialSetup from '../components/InitialSetup.vue';
import NotFound from '../components/NotFound.vue';
import UnlockError from '../components/UnlockError.vue';
import UnlockSuccess from '../components/UnlockSuccess.vue';
import UserProfile from '../components/UserProfile.vue';
import VaultDetails from '../components/VaultDetails.vue';
import VaultList from '../components/VaultList.vue';

import i18n, { mapToLocale } from '../i18n';

function checkRole(role: string): NavigationGuardWithThis<undefined> {
  return async (to, _) => {
    const auth = await authPromise;
    if (auth.hasRole(role)) {
      return true;
    } else {
      console.warn(`Access denied: User requires role ${role} to access ${to.fullPath}`);
      return { path: '/app/forbidden', replace: true };
    }
  };
}

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
    meta: { skipAuth: true, skipSetup: true },
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
        path: 'authority',
        component: AuthorityList,
      },
      {
        path: 'authority/user/1',
        component: () => import('../components/UserDetail.vue'),
      },
      {
        path: 'authority/group/1',
        component: () => import('../components/GroupDetail.vue'),
      },
      {
        path: 'authority/group/1/edit',
        component: () => import('../components/GroupEdit.vue'),
      },
      {
        path: 'vaults',
        component: VaultList
      },
      {
        path: 'vaults/create',
        component: CreateVault,
        props: () => ({ recover: false }),
        beforeEnter: checkRole('create-vaults'),
      },
      {
        path: 'vaults/recover',
        component: CreateVault,
        props: () => ({ recover: true }),
        beforeEnter: checkRole('create-vaults'),
      },
      {
        path: 'vaults/:id',
        component: VaultDetails,
        props: (route) => ({ vaultId: route.params.id })
      },
      {
        path: 'profile',
        component: UserProfile
      },
      {
        path: 'admin',
        beforeEnter: checkRole('admin'),
        children: [
          {
            path: '',
            redirect: '/app/admin/settings'
          },
          {
            path: 'settings',
            component: AdminSettings,
            props: (route) => ({ token: route.query.token }),
            meta: { skipSetup: true }
          },
          {
            path: 'auditlog',
            component: AuditLog,
          },
        ]
      },
    ]
  },
  {
    path: '/app/setup',
    component: InitialSetup,
    meta: { skipSetup: true }, // no setup required to run setup ;)
  },
  {
    path: '/app/unlock-success',
    component: UnlockSuccess,
    props: (route) => ({ vaultId: route.query.vault, deviceId: route.query.device }),
    meta: { skipSetup: true }
  },
  {
    path: '/app/unlock-error',
    component: UnlockError,
    meta: { skipAuth: true, skipSetup: true }
  },
  {
    path: '/app/:pathMatch(.+)', //necessary due to using history mode in router
    component: NotFound,
    meta: { skipAuth: true, skipSetup: true }
  },
  {
    path: '/app/forbidden',
    component: Forbidden,
    meta: { skipAuth: true, skipSetup: true }
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
        // secondsSinceEpoch is required for legacy reasons, as caching headers were only introduced in #255
        // as result, the redirect URI changes and caching does not break updates anymore
        const secondsSinceEpoch = Math.round(new Date().getTime() / 1000);
        const redirect: RouteLocationRaw = { query: { ...to.query, 'sync_me': secondsSinceEpoch } };
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
        await backend.users.putMe();
      }
    }).finally(() => {
      const { sync_me: _, ...remainingQuery } = to.query; // remove sync_me query parameter to avoid endless recursion
      next({ path: to.path, query: remainingQuery, replace: true });
    });
  } else {
    next();
  }
});

// THIRD check user/browser keys (requires auth)
router.beforeEach(async (to) => {
  if (to.meta.skipSetup) {
    return;
  }
  const me = await userdata.me;
  if (!me.setupCode) {
    return { path: '/app/setup' };
  }
  const browserKeys = await userdata.browserKeys;
  if (!browserKeys) {
    return { path: '/app/setup' };
  }
  const browser = await userdata.browser;
  if (!browser) {
    return { path: '/app/setup' };
  }
});

// FOURTH apply user language
router.beforeEach(async (to) => {
  if (!to.meta.skipAuth) {
    const me = await userdata.me;
    if (me.language) {
      i18n.global.locale.value = mapToLocale(me.language);
    }
  }
});

export default router;
