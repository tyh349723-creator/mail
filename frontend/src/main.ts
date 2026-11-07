import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import pinia from './store'
import { setupPermission } from './router/permission'

import './styles/base.css' // 移动端 H5 基础样式（先）
import './styles/index.scss' // 管理端全局样式（后，同名选择器以本文件为准）

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 路由守卫（角色权限控制）
setupPermission(router)

app.mount('#app')