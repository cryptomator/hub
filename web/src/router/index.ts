import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router';
import auth from '../common/auth';
import CreateVault from '../components/CreateVault.vue';
import HelloWorld from '../components/HelloWorld.vue';
import LogoutComponent from '../components/Logout.vue';
import UnlockVault from '../components/UnlockVault.vue';
import VaultDetails from '../components/VaultDetails.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    end: true,
    component: HelloWorld
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
    path: '/vaults/create',
    component: CreateVault
  },
  {
    path: '/vaults/:id',
    component: VaultDetails,
    props: (route) => ({ vaultId: route.params.id })
  },
  {
    path: '/vaults/:id/unlock',
    component: UnlockVault,
    props: (route) => ({ vaultId: route.params.id, deviceId: route.query.device_id, deviceKey: route.query.device_key, redirectTo: route.query.redirect_uri })
  },
  {
    path: '/logout',
    name: 'Logout',
    component: LogoutComponent,
    meta: { skipAuth: true }
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes: routes
});
router.beforeEach((to, from, next) => {
  if (to.meta.skipAuth) {
    next();
  } else {
    const redirectUri = `${window.location.protocol}//${window.location.host}${import.meta.env.BASE_URL}#${to.fullPath}`
    auth.loginIfRequired(redirectUri).then(() => {
      next();
    }).catch(error => {
      next(new Error("auth failed " + error));
    });
  }
});

export default router;
