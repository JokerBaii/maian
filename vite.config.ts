import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig(({ command }) => ({
  plugins: [uni()],
  define: {
    __MAIAN_PUBLIC_DEMO__: JSON.stringify(process.env.VITE_PUBLIC_DEMO === 'true'),
    __MAIAN_PREVIEW_PASSWORD__: JSON.stringify(command === 'serve' ? process.env.APP_PREVIEW_PASSWORD || '' : process.env.VITE_PUBLIC_DEMO === 'true' ? 'MaianTest2026!' : ''),
    __MAIAN_PREVIEW_LOCATION__: JSON.stringify(process.env.VITE_PUBLIC_DEMO === 'true' || (command === 'serve' && process.env.APP_PREVIEW_PASSWORD) ? {
      longitude: 120.1505, latitude: 30.2805, accuracy: 15, address: '杭州市'
    } : null)
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
