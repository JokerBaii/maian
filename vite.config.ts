import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig(({ command }) => ({
  plugins: [uni()],
  define: {
    __MAIAN_PREVIEW_PASSWORD__: JSON.stringify(command === 'serve' ? process.env.APP_PREVIEW_PASSWORD || '' : '')
  },
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_DEV_API_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        configure(proxy) {
          // 本地联调通过同源代理访问生产测试接口，避免把开发 Origin 透传给后端 CORS 校验。
          proxy.on('proxyReq', proxyRequest => proxyRequest.removeHeader('origin'))
        }
      }
    }
  }
}))
