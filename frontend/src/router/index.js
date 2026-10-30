import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

// BASE_URL 与 Vite base 一致，便于 Nginx 子路径部署
const base = import.meta.env.BASE_URL || '/'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    redirect: '/instances',
    children: [
      { path: 'instances', name: 'instances', component: () => import('../views/InstancesView.vue') },
      { path: 'maps', name: 'maps', component: () => import('../views/MapsView.vue') },
      { path: 'mods', name: 'mods', component: () => import('../views/ModsView.vue') },
      { path: 'instance-mods', name: 'instance-mods', component: () => import('../views/InstanceModsView.vue') },
      { path: 'instance-config', name: 'instance-config', component: () => import('../views/InstanceConfigView.vue') },
      { path: 'instance-control', name: 'instance-control', component: () => import('../views/InstanceControlView.vue') },
      { path: 'jobs', name: 'jobs', component: () => import('../views/JobsView.vue') },
      { path: 'steamcmd/:sessionId', name: 'steamcmd', component: () => import('../views/SteamCmdView.vue') },
      { path: 'users', name: 'users', component: () => import('../views/UsersView.vue') },
      { path: 'settings', name: 'settings', component: () => import('../views/SettingsView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(base),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.name !== 'login' && !auth.isLogin) {
    return { name: 'login' }
  }
  if (to.name === 'login' && auth.isLogin) {
    return { name: 'instances' }
  }
})

export default router
