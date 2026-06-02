import { createApp } from 'vue'
import App from './App.vue'

import router from './router'
import { createPinia } from 'pinia'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'  // 引入中文语言包

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })  // 使用中文
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')