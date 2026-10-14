import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// Nginx 反代友好：
//   VITE_BASE     页面与静态资源前缀，默认 /
//   VITE_API_BASE 接口前缀，默认 /api/v1
// 子路径示例：VITE_BASE=/aram3/  VITE_API_BASE=/aram3/api/v1
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, '')
  const base = env.VITE_BASE || '/'
  const apiBase = env.VITE_API_BASE || '/api/v1'
  const apiProxyPath = apiBase.replace(/\/v\d+\/?$/, '') || '/api'

  return {
    plugins: [vue()],
    base,
    build: {
      outDir: path.resolve(__dirname, '../backend/src/main/resources/static'),
      emptyOutDir: true,
      assetsDir: 'assets'
    },
    server: {
      port: 5173,
      proxy: {
        [apiProxyPath]: {
          target: env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true
        }
      }
    }
  }
})
