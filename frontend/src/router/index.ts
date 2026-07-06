import { createRouter, createWebHistory, NavigationGuardWithThis, RouteLocationNormalized, RouteLocationRaw, RouteRecordRaw } from 'vue-router';
import authPromise from '../common/auth';
import backend from '../common/backend';
import { baseURL } from '../common/config';
import userdata from '../common/userdata';
import AdminSettings from '../components/AdminSettings.vue';
import AuditLog from '../components/AuditLog.vue';
import AuthenticatedMain from '../components/AuthenticatedMain.vue';
import GroupDetail from '../components/authority/GroupDetail.vue';
import GroupEditCreate from '../components/authority/GroupEditCreate.vue';
import GroupList from '../components/authority/GroupList.vue';
import UserDetail from '../components/authority/UserDetail.vue';
import UserEditCreate from '../components/authority/UserEditCreate.vue';
import UserList from '../components/authority/UserList.vue';
import CreateVault from '../components/CreateVault.vue';
import EmergencyAccessVaultList from '../components/emergencyaccess/EmergencyAccessVaultList.vue';
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
    beforeEnter: async () => {
      const auth = await authPromise;
      if (auth.isAuthenticated()) {
        const loggedOutUri = `${location.origin}${router.resolve('/').href}`;
        await auth.logout(loggedOutUri);
        return false; // prevent navigation, as logout will cause a full page reload
      } else {
        return true;
      }
    }
  },
  {
    path: '/app', /* required but unused */
    component: AuthenticatedMain,
    children: [
      {
        path: 'emergency-access',
        component: EmergencyAccessVaultList
      },
      {
        path: 'users',
        beforeEnter: checkRole('admin'),
        children: [
          {
            path: '',
            component: UserList
          },
          {
            path: 'create',
            component: UserEditCreate,
            props: { id: undefined, mode: 'CREATE' },
          },
          {
            path: ':id',
            component: UserDetail,
            props: (route) => ({ id: route.params.id as string }),
          },
          {
            path: ':id/edit',
            component: UserEditCreate,
            props: (route) => ({ id: route.params.id as string, mode: 'EDIT' }),
          },
        ]
      },
      {
        path: 'groups',
        beforeEnter: checkRole('admin'),
        children: [
          {
            path: '',
            component: GroupList
          },
          {
            path: 'create',
            component: GroupEditCreate,
            props: { id: undefined, mode: 'CREATE' },
          },
          {
            path: ':id',
            component: GroupDetail,
            props: (route) => ({ id: route.params.id as string }),
          },
          {
            path: ':id/edit',
            component: GroupEditCreate,
            props: (route) => ({ id: route.params.id as string, mode: 'EDIT' }),
          },
        ]
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
            props: (route) => {
              return { token: route.query.token, session: route.query.session };
            },
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
router.beforeEach(async (to) => {
  if (to.meta.skipAuth) {
    return true;
  }

  const auth = await authPromise;
  if (auth.isAuthenticated()) {
    return true;
  } else {
    const redirectUri = buildRedirectSyncMeUri(to);
    auth.login(redirectUri);
    return false;
  }
});

// SECOND update user data (requires auth)
router.beforeEach(async (to) => {
  if (!('sync_me' in to.query)) {
    return true;
  }

  const auth = await authPromise;
  if (auth.isAuthenticated()) {
    await backend.users.putMe();
  }

  const { sync_me: _, ...remainingQuery } = to.query; // remove sync_me query parameter to avoid endless recursion
  return { path: to.path, query: remainingQuery, replace: true };
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
  // Users created before Hub 1.4.0 may lack ECDSA key pair. Unlocking the user keys with
  // the (already registered) browser device backfills and persists ECDSA key pair
  if (!me.ecdsaPublicKey) {
    try {
      await userdata.decryptUserKeysWithBrowser(browserKeys, browser);
    } catch (error) {
      console.error('Backfilling the missing ECDSA user key failed.', error);
    }
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

export function buildRedirectSyncMeUri(route?: RouteLocationNormalized): string {
  const targetRoute = route ?? router.currentRoute.value;
  // secondsSinceEpoch is required for legacy reasons, as caching headers were only introduced in #255
  const secondsSinceEpoch = Math.round(Date.now() / 1000);
  const redirect: RouteLocationRaw = {
    query: {
      ...targetRoute.query,
      sync_me: secondsSinceEpoch,
    }
  };
  return `${location.origin}${router.resolve(redirect, targetRoute).href}`;
}

export default router;
