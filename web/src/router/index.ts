import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router';
import auth from '../common/auth';
import CreateVault from '../components/CreateVault.vue';
import HelloWorld from '../components/HelloWorld.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: HelloWorld
  },
  {
    path: '/vaults/create',
    name: 'Create Vault',
    component: CreateVault
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes: routes
});
router.beforeEach((to, from, next) => {
  auth.loginIfRequired().then(() => {
    next();
  }).catch(error => {
    next(new Error("auth failed " + error));
  });
});

export default router;
