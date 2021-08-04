import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import HelloWorld from '../components/HelloWorld.vue';
import Other from '../components/Other.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: HelloWorld
  },
  {
    path: '/other',
    name: 'Other',
    component: Other
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes: routes
});

export default router;
