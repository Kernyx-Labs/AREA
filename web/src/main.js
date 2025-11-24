import { createApp } from 'vue'
import App from './App.vue'
import './assets/styles.css'

const savedTheme = localStorage.getItem('theme')
if (!savedTheme || savedTheme === 'dark') {
  document.documentElement.setAttribute('data-theme','dark')
} else if (savedTheme === 'light') {
  document.documentElement.removeAttribute('data-theme')
}

createApp(App).mount('#app')
