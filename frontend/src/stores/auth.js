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
    isLogin: (s) => !!s.token
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
