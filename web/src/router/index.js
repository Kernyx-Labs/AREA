import { createRouter, createWebHistory } from 'vue-router'
import AreasDashboard from '@/components/AreasDashboard.vue'
import PipelineEditor from '@/components/PipelineEditor.vue'
import ServicesView from '@/components/ServicesView.vue'
import ProfileView from '@/components/ProfileView.vue'
import LogsView from '@/components/LogsView.vue'
import LoginView from '@/components/LoginView.vue'

const routes = [
  { path: '/login', component: LoginView, name: 'login', meta: { hideLayout: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: AreasDashboard, name: 'dashboard' },
  { path: '/editor/:areaId?', component: PipelineEditor, name: 'editor', props: true },
  { path: '/services', component: ServicesView, name: 'services' },
  { path: '/logs', component: LogsView, name: 'logs' },
  { path: '/profile', component: ProfileView, name: 'profile' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
