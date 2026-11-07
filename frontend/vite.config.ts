import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      open: false,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:10010',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          // 去掉上游转发里的 Expect: 100-continue，避免 Node http-parser
          // 与 WebFlux chunked 响应的互操作怪癖（Parse Error: Data after Connection: close）
          configure: (proxy) => {
            // 剥掉上游转发里的 Expect: 100-continue —— 网关（WebFlux/Netty）对
            //   Expect 返回的 "100 Continue" 中间响应带 connection: close，
            //   Node http-parser 视为连接结束、随后拒绝 200 OK（Parse Error:
            //   Data after Connection: close）。仅 .NET/curl 等非浏览器客户端
            //   会带此头；浏览器/axios 从未发送，不受影响。
            proxy.on('proxyReq', (proxyReq, req) => {
              if (req.headers?.expect) {
                proxyReq.removeHeader('expect')
                proxyReq.removeHeader('Expect')
              }
            })
          }
        }
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          manualChunks: {
            'element-plus': ['element-plus'],
            'echarts': ['echarts'],
            'vue-vendor': ['vue', 'vue-router', 'pinia']
          }
        }
      }
    }
  }
})