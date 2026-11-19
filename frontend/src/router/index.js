import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import i18n from '../i18n'

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
      // 实例相关页面本身不设全局权限码：列表按 canView 过滤，操作按每实例 myPerms 控制
      { path: 'instances', name: 'instances', component: () => import('../views/InstancesView.vue') },
      { path: 'maps', name: 'maps', component: () => import('../views/MapsView.vue') },
      { path: 'mods', name: 'mods', component: () => import('../views/ModsView.vue') },
      { path: 'instance-mods', name: 'instance-mods', component: () => import('../views/InstanceModsView.vue') },
      { path: 'instance-config', name: 'instance-config', component: () => import('../views/InstanceConfigView.vue') },
      { path: 'instance-control', name: 'instance-control', component: () => import('../views/InstanceControlView.vue') },
      { path: 'jobs', name: 'jobs', component: () => import('../views/JobsView.vue'), meta: { perms: ['job:manage'] } },
      { path: 'steamcmd/:sessionId', name: 'steamcmd', component: () => import('../views/SteamCmdView.vue'), meta: { perms: ['steamcmd:use'] } },
      { path: 'users', name: 'users', component: () => import('../views/UsersView.vue'), meta: { perms: ['user:manage'] } },
      { path: 'roles', name: 'roles', component: () => import('../views/RolesView.vue'), meta: { perms: ['user:manage'] } },
      { path: 'settings', name: 'settings', component: () => import('../views/SettingsView.vue'), meta: { perms: ['system:settings'] } }
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
  // 页面级权限码校验：任一缺失则拒绝进入
  if (to.meta?.perms?.length) {
    const missing = to.meta.perms.filter((p) => !auth.hasPerm(p))
    if (missing.length) {
      ElMessage.warning(i18n.global.t('common.noPerm'))
      return { name: 'instances' }
    }
  }
})

export default router
