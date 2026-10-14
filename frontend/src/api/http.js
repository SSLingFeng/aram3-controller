import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

// 可通过 VITE_API_BASE 适配 Nginx 反代/子路径，默认 /api/v1
const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1'

const client = axios.create({
  baseURL: API_BASE,
  timeout: 60000
})

client.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

client.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return body
  },
  async (error) => {
    const auth = useAuthStore()
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message || '网络错误'
    if (status === 401 || status === 403) {
      const isAuthApi = (error.config?.url || '').includes('/auth/login')
      if (!isAuthApi) {
        if (status === 401) {
          auth.logout()
          if (router.currentRoute.value.name !== 'login') {
            router.push({ name: 'login' })
          }
        }
        return Promise.reject(new Error(status === 401 ? '未登录或登录已过期，请重新登录' : msg))
      }
    }
    return Promise.reject(new Error(msg))
  }
)

export default client
