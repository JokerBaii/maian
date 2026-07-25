import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      '/video-proxy/kepuchina': {
        target: 'https://pqnoss.kepuchina.cn',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/video-proxy\/kepuchina/, ''),
        secure: true
      },
      '/video-proxy/nxgov': {
        target: 'https://www.nx.gov.cn',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/video-proxy\/nxgov/, ''),
        secure: true
      },
      '/video-proxy/bcebos': {
        target: 'https://medical-cms.cdn.bcebos.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/video-proxy\/bcebos/, ''),
        secure: true
      },
      '/video-proxy/gzfuquan': {
        target: 'https://www.gzfuquan.gov.cn',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/video-proxy\/gzfuquan/, ''),
        secure: true
      }
    }
  }
});
