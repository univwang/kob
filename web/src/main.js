import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'


import Global from './global.js'

// createApp(App).use(store).use(router).mount('#app')

const app = createApp(App);

// 使用插件
app.use(store).use(router);

// 在根组件中提供全局变量
app.provide('global', Global);

// 挂载应用
app.mount('#app');