<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">{{ $t('layout.brand') }}</div>
      <el-menu :default-active="active" router background-color="#16324f" text-color="#dbe4ee" active-text-color="#f0b27a">
        <el-menu-item index="/instances">{{ $t('layout.menu.instances') }}</el-menu-item>
        <el-menu-item index="/maps">{{ $t('layout.menu.maps') }}</el-menu-item>
        <el-menu-item index="/mods">{{ $t('layout.menu.mods') }}</el-menu-item>
        <el-menu-item index="/instance-mods">{{ $t('layout.menu.instanceMods') }}</el-menu-item>
        <el-menu-item index="/instance-config">{{ $t('layout.menu.instanceConfig') }}</el-menu-item>
        <el-menu-item index="/instance-control">{{ $t('layout.menu.instanceControl') }}</el-menu-item>
        <el-menu-item index="/jobs">{{ $t('layout.menu.jobs') }}</el-menu-item>
        <el-menu-item index="/steamcmd">{{ $t('layout.menu.steamcmd') }}</el-menu-item>
        <el-menu-item index="/users">{{ $t('layout.menu.users') }}</el-menu-item>
        <el-menu-item index="/settings">{{ $t('layout.menu.settings') }}</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="title">{{ title }}</div>
        <div class="user">
          <LanguageSwitcher />
          <span>{{ auth.user?.displayName || auth.user?.username }}</span>
          <el-button link type="primary" @click="onLogout">{{ $t('layout.logout') }}</el-button>
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
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { t } = useI18n({ useScope: 'global' })

const active = computed(() => route.path)

const pathKeys = {
  '/instances': 'instances',
  '/maps': 'maps',
  '/mods': 'mods',
  '/instance-mods': 'instanceMods',
  '/instance-config': 'instanceConfig',
  '/instance-control': 'instanceControl',
  '/jobs': 'jobs',
  '/steamcmd': 'steamcmd',
  '/users': 'users',
  '/settings': 'settings'
}

const title = computed(() => {
  const key = pathKeys[route.path]
  return key ? t('layout.titles.' + key) : t('layout.app')
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
