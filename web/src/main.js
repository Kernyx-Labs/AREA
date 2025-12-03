import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/styles.css'

const savedTheme = localStorage.getItem('theme')
if (!savedTheme || savedTheme === 'dark') {
  document.documentElement.setAttribute('data-theme','dark')
} else if (savedTheme === 'light') {
  document.documentElement.removeAttribute('data-theme')
}

const app = createApp(App)
app.use(router)
app.mount('#app')
