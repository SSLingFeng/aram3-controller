<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">Aram3 Controller</div>
      <el-menu :default-active="active" router background-color="#16324f" text-color="#dbe4ee" active-text-color="#f0b27a">
        <el-menu-item index="/instances">实例管理</el-menu-item>
        <el-menu-item index="/maps">地图 (mpmissions)</el-menu-item>
        <el-menu-item index="/mods">模组库</el-menu-item>
        <el-menu-item index="/instance-mods">实例模组配置</el-menu-item>
        <el-menu-item index="/instance-config">实例配置文件</el-menu-item>
        <el-menu-item index="/jobs">任务中心</el-menu-item>
        <el-menu-item index="/steamcmd">SteamCMD</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/settings">系统设置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="title">{{ title }}</div>
        <div class="user">
          <span>{{ auth.user?.displayName || auth.user?.username }}</span>
          <el-button link type="primary" @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const active = computed(() => route.path)
const title = computed(() => {
  const map = {
    '/instances': '实例管理',
    '/maps': '地图管理',
    '/mods': '模组库',
    '/instance-mods': '实例模组配置',
    '/instance-config': '实例配置文件',
    '/jobs': '任务中心',
    '/steamcmd': 'SteamCMD 控制台',
    '/users': '用户管理',
    '/settings': '系统设置'
  }
  return map[route.path] || 'Aram3'
})

function onLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #16324f;
}
.brand {
  color: #fff;
  font-weight: 700;
  padding: 18px 16px 12px;
  letter-spacing: 0.02em;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
}
.title {
  font-weight: 600;
}
.user {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
