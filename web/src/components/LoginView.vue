<template>
  <div class="login-container">
    <!-- Left Half - Login/Create Account Form -->
    <div class="login-form-section">
      <div class="form-wrapper">
        <!-- Logo -->
        <div class="login-logo">
          <img src="/AREA.png" alt="AREA logo" class="logo-image" />
          <h1>AREA</h1>
        </div>

        <!-- Welcome Message -->
        <div class="welcome-message">
          <h2>{{ isLogin ? 'Welcome Back' : 'Create Account' }}</h2>
          <p>{{ isLogin ? 'Sign in to continue to your automations' : 'Join us to start automating your workflows' }}</p>
        </div>

        <!-- Form -->
        <form class="login-form" @submit.prevent="handleSubmit">
          <!-- Name Field (only for signup) -->
          <div class="form-group" v-if="!isLogin">
            <label for="name">Full Name</label>
            <div class="input-wrapper">
              <UserIcon size="20" class="input-icon" />
              <input
                type="text"
                id="name"
                v-model="formData.name"
                placeholder="Enter your full name"
                required
              />
            </div>
          </div>

          <!-- Email Field -->
          <div class="form-group">
            <label for="email">Email Address</label>
            <div class="input-wrapper">
              <MailIcon size="20" class="input-icon" />
              <input
                type="email"
                id="email"
                v-model="formData.email"
                placeholder="Enter your email"
                required
              />
            </div>
          </div>

          <!-- Password Field -->
          <div class="form-group">
            <label for="password">Password</label>
            <div class="input-wrapper">
              <LockIcon size="20" class="input-icon" />
              <input
                :type="showPassword ? 'text' : 'password'"
                id="password"
                v-model="formData.password"
                placeholder="Enter your password"
                required
              />
              <button
                type="button"
                class="toggle-password"
                @click="showPassword = !showPassword"
                tabindex="-1"
              >
                <EyeIcon v-if="!showPassword" size="20" />
                <EyeOffIcon v-else size="20" />
              </button>
            </div>
          </div>

          <!-- Confirm Password Field (only for signup) -->
          <div class="form-group" v-if="!isLogin">
            <label for="confirmPassword">Confirm Password</label>
            <div class="input-wrapper">
              <LockIcon size="20" class="input-icon" />
              <input
                :type="showConfirmPassword ? 'text' : 'password'"
                id="confirmPassword"
                v-model="formData.confirmPassword"
                placeholder="Confirm your password"
                required
              />
              <button
                type="button"
                class="toggle-password"
                @click="showConfirmPassword = !showConfirmPassword"
                tabindex="-1"
              >
                <EyeIcon v-if="!showConfirmPassword" size="20" />
                <EyeOffIcon v-else size="20" />
              </button>
            </div>
          </div>

          <!-- Remember Me / Forgot Password -->
          <div class="form-options" v-if="isLogin">
            <label class="checkbox-label">
              <input type="checkbox" v-model="rememberMe" />
              <span>Remember me</span>
            </label>
            <a href="#" class="forgot-password" @click.prevent="handleForgotPassword">
              Forgot password?
            </a>
          </div>

          <!-- Terms and Conditions (only for signup) -->
          <div class="form-group" v-if="!isLogin">
            <label class="checkbox-label">
              <input type="checkbox" v-model="acceptTerms" required />
              <span>I agree to the <a href="#" @click.prevent>Terms of Service</a> and <a href="#" @click.prevent>Privacy Policy</a></span>
            </label>
          </div>

          <!-- Error Message -->
          <div class="error-message" v-if="error">
            <AlertCircleIcon size="20" />
            <span>{{ error }}</span>
          </div>

          <!-- Submit Button -->
          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="!loading">{{ isLogin ? 'Sign In' : 'Create Account' }}</span>
            <span v-else class="loading-spinner">
              <div class="spinner-small"></div>
              {{ isLogin ? 'Signing In...' : 'Creating Account...' }}
            </span>
          </button>
        </form>

        <!-- Toggle Login/Signup -->
        <div class="form-toggle">
          <p>
            {{ isLogin ? "Don't have an account?" : 'Already have an account?' }}
            <a href="#" @click.prevent="toggleMode">
              {{ isLogin ? 'Sign Up' : 'Sign In' }}
            </a>
          </p>
        </div>

        <!-- Social Login -->
        <div class="social-divider">
          <span>or continue with</span>
        </div>

        <div class="social-buttons">
          <button class="social-btn google" @click="handleSocialLogin('google')">
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Google
          </button>
          <button class="social-btn github" @click="handleSocialLogin('github')">
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path fill="currentColor" d="M12 2C6.477 2 2 6.477 2 12c0 4.42 2.865 8.17 6.839 9.49.5.092.682-.217.682-.482 0-.237-.008-.866-.013-1.7-2.782.603-3.369-1.34-3.369-1.34-.454-1.156-1.11-1.463-1.11-1.463-.908-.62.069-.608.069-.608 1.003.07 1.531 1.03 1.531 1.03.892 1.529 2.341 1.087 2.91.831.092-.646.35-1.086.636-1.336-2.22-.253-4.555-1.11-4.555-4.943 0-1.091.39-1.984 1.029-2.683-.103-.253-.446-1.27.098-2.647 0 0 .84-.269 2.75 1.025A9.578 9.578 0 0112 6.836c.85.004 1.705.114 2.504.336 1.909-1.294 2.747-1.025 2.747-1.025.546 1.377.203 2.394.1 2.647.64.699 1.028 1.592 1.028 2.683 0 3.842-2.339 4.687-4.566 4.935.359.309.678.919.678 1.852 0 1.336-.012 2.415-.012 2.743 0 .267.18.578.688.48C19.138 20.167 22 16.418 22 12c0-5.523-4.477-10-10-10z"/>
            </svg>
            GitHub
          </button>
          <button class="social-btn discord" @click="handleSocialLogin('discord')">
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path fill="currentColor" d="M20.317 4.37a19.791 19.791 0 00-4.885-1.515.074.074 0 00-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 00-5.487 0 12.64 12.64 0 00-.617-1.25.077.077 0 00-.079-.037A19.736 19.736 0 003.677 4.37a.07.07 0 00-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 00.031.057 19.9 19.9 0 005.993 3.03.078.078 0 00.084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 00-.041-.106 13.107 13.107 0 01-1.872-.892.077.077 0 01-.008-.128 10.2 10.2 0 00.372-.292.074.074 0 01.077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 01.078.01c.12.098.246.198.373.292a.077.077 0 01-.006.127 12.299 12.299 0 01-1.873.892.077.077 0 00-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 00.084.028 19.839 19.839 0 006.002-3.03.077.077 0 00.032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 00-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"/>
            </svg>
            Discord
          </button>
        </div>
      </div>
    </div>

    <!-- Right Half - Abstract Design -->
    <div class="abstract-section">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
      <div class="gradient-mesh"></div>

      <div class="abstract-content">
        <div class="feature-showcase">
          <div class="feature-item" v-for="(feature, index) in features" :key="index">
            <div class="feature-icon">
              <component :is="feature.icon" size="32" />
            </div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import {
  UserIcon,
  MailIcon,
  LockIcon,
  EyeIcon,
  EyeOffIcon,
  AlertCircleIcon,
  ZapIcon,
  LinkIcon,
  ShieldIcon
} from 'lucide-vue-next';
import { useModal } from '../composables/useModal.js';

const router = useRouter();
const modal = useModal();

// State
const isLogin = ref(true);
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const rememberMe = ref(false);
const acceptTerms = ref(false);
const loading = ref(false);
const error = ref('');

const formData = reactive({
  name: '',
  email: '',
  password: '',
  confirmPassword: ''
});

const features = [
  {
    icon: ZapIcon,
    title: 'Powerful Automation',
    description: 'Connect your favorite services and create powerful workflows'
  },
  {
    icon: LinkIcon,
    title: 'Seamless Integration',
    description: 'Integrate with Gmail, Discord, and many more services'
  },
  {
    icon: ShieldIcon,
    title: 'Secure & Reliable',
    description: 'Your data is encrypted and protected with industry standards'
  }
];

// Methods
function toggleMode() {
  isLogin.value = !isLogin.value;
  error.value = '';
  resetForm();
}

function resetForm() {
  formData.name = '';
  formData.email = '';
  formData.password = '';
  formData.confirmPassword = '';
  acceptTerms.value = false;
}

async function handleSubmit() {
  // DEMO MODE: Skip validation and API calls
  // Just accept any email/password and navigate to dashboard
  error.value = '';
  loading.value = true;

  // Simulate a brief loading state for better UX
  setTimeout(() => {
    // Store demo auth token
    localStorage.setItem('authToken', 'demo-token');

    // Store remember me preference if checked
    if (rememberMe.value) {
      localStorage.setItem('rememberMe', 'true');
    }

    loading.value = false;

    // Navigate to dashboard
    router.push({ name: 'dashboard' });
  }, 500);
}

async function handleForgotPassword() {
  await modal.alert('Password reset functionality coming soon!', {
    title: 'Coming Soon',
    variant: 'info'
  });
}

function handleSocialLogin(provider) {
  // DEMO MODE: Social login also navigates to dashboard
  loading.value = true;
  setTimeout(() => {
    localStorage.setItem('authToken', `demo-token-${provider}`);
    loading.value = false;
    router.push({ name: 'dashboard' });
  }, 500);
}
</script>

<style scoped src="../assets/LoginView.css"></style>
