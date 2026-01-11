import { createRouter, createWebHistory } from 'vue-router'
import AreasDashboard from '@/components/AreasDashboard.vue'
import PipelineEditor from '@/components/PipelineEditor.vue'
import ServicesView from '@/components/ServicesView.vue'
import ProfileView from '@/components/ProfileView.vue'
import LogsView from '@/components/LogsView.vue'
import LoginView from '@/components/LoginView.vue'

const routes = [
  {
    path: '/login',
    component: LoginView,
    name: 'login',
    meta: { hideLayout: true, requiresAuth: false }
  },
  {
    path: '/register',
    component: () => import('@/components/RegisterView.vue'),
    name: 'register',
    meta: { hideLayout: true, requiresAuth: false }
  },
  { path: '/', redirect: '/dashboard' },
  {
    path: '/dashboard',
    component: AreasDashboard,
    name: 'dashboard',
    meta: { requiresAuth: true }
  },
  {
    path: '/editor/:areaId?',
    component: PipelineEditor,
    name: 'editor',
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/services',
    component: ServicesView,
    name: 'services',
    meta: { requiresAuth: true }
  },
  {
    path: '/logs',
    component: LogsView,
    name: 'logs',
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    component: ProfileView,
    name: 'profile',
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * Navigation guard to protect routes that require authentication
 * Redirects to login page if user is not authenticated
 */
router.beforeEach((to, from, next) => {
  const accessToken = localStorage.getItem('accessToken');
  const isAuthenticated = !!accessToken;

  // Check if route requires authentication
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false);

  if (requiresAuth && !isAuthenticated) {
    // User is not authenticated, redirect to login
    // Save the intended destination to redirect after login
    next({
      name: 'login',
      query: { redirect: to.fullPath }
    });
  } else if ((to.name === 'login' || to.name === 'register') && isAuthenticated) {
    // User is already authenticated, redirect to dashboard
    next({ name: 'dashboard' });
  } else {
    // Allow navigation
    next();
  }
});

export default router
