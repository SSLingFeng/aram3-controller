import { defineStore } from 'pinia'
import client from '../api/http'

// 不做记住登录 / 自动登录：token 只存在内存中，刷新或重新打开页面需要重新登录
export const useAuthStore = defineStore('auth', {
  state: () => {
    // 清理历史遗留的本地登录态
    try {
      localStorage.removeItem('aram3_token')
      localStorage.removeItem('aram3_user')
    } catch (e) {
      // ignore
    }
    return {
      token: '',
      user: null
    }
  },
  getters: {
    isLogin: (s) => !!s.token,
    isSuperAdmin: (s) => !!s.user?.superAdmin,
    // 全局权限码判断：超管直接放行，普通用户看 user.perms 数组（登录/me() 时后端下发）
    hasPerm: (s) => (code) => !!s.user?.superAdmin || (s.user?.perms || []).includes(code)
  },
  actions: {
    async login(username, password) {
      const data = await client.post('/auth/login', { username, password })
      this.token = data.accessToken
      this.user = data.user
    },
    logout() {
      this.token = ''
      this.user = null
    }
  }
})
