<template>
  <div class="profile-wrap">
    <div class="profile-header">
      <h1>Profile</h1>
      <button class="logout-btn" @click="handleLogout" :disabled="loading">
        <LogOutIcon size="18" />
        <span>Logout</span>
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="profile-loading">
      <div class="spinner"></div>
      <p>Loading profile...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="loadError" class="profile-error">
      <AlertCircleIcon size="48" />
      <p>{{ loadError }}</p>
      <button class="retry-btn" @click="loadUserProfile">Retry</button>
    </div>

    <!-- Profile Content -->
    <div v-else-if="user" class="profile-content">
      <!-- User Avatar & Basic Info -->
      <div class="profile-card">
        <div class="profile-avatar">
          <div class="avatar-circle">
            <UserIcon size="48" />
          </div>
        </div>

        <div class="profile-info">
          <h2>{{ user.fullName || user.username }}</h2>
          <p class="username">@{{ user.username }}</p>
          <div class="verification-badge" v-if="user.emailVerified">
            <ShieldCheckIcon size="16" />
            <span>Email Verified</span>
          </div>
          <div class="verification-badge unverified" v-else>
            <AlertCircleIcon size="16" />
            <span>Email Not Verified</span>
          </div>
        </div>
      </div>

      <!-- Account Details -->
      <div class="profile-section">
        <h3>Account Details</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <MailIcon size="20" class="detail-icon" />
            <div class="detail-content">
              <label>Email Address</label>
              <p>{{ user.email }}</p>
            </div>
          </div>

          <div class="detail-item">
            <UserIcon size="20" class="detail-icon" />
            <div class="detail-content">
              <label>Username</label>
              <p>{{ user.username }}</p>
            </div>
          </div>

          <div class="detail-item">
            <CalendarIcon size="20" class="detail-icon" />
            <div class="detail-content">
              <label>Member Since</label>
              <p>{{ formatDate(user.createdAt) }}</p>
            </div>
          </div>

          <div class="detail-item">
            <KeyIcon size="20" class="detail-icon" />
            <div class="detail-content">
              <label>User ID</label>
              <p>#{{ user.id }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Account Actions -->
      <div class="profile-section">
        <h3>Account Actions</h3>
        <div class="action-grid">
          <button class="action-btn" @click="handleChangePassword">
            <LockIcon size="20" />
            <span>Change Password</span>
          </button>

          <button class="action-btn" @click="handleUpdateProfile">
            <EditIcon size="20" />
            <span>Edit Profile</span>
          </button>

          <button class="action-btn" @click="handleDownloadApk">
            <DownloadIcon size="20" />
            <span>Download APK</span>
          </button>

          <button class="action-btn danger" @click="handleDeleteAccount">
            <TrashIcon size="20" />
            <span>Delete Account</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  UserIcon,
  MailIcon,
  CalendarIcon,
  KeyIcon,
  LogOutIcon,
  LockIcon,
  EditIcon,
  TrashIcon,
  AlertCircleIcon,
  ShieldCheckIcon,
  DownloadIcon
} from 'lucide-vue-next';
import { useAuthStore } from '../stores/authStore.js';
import { useModal } from '../composables/useModal.js';

const router = useRouter();
const authStore = useAuthStore();
const modal = useModal();

const user = ref(null);
const isLoading = ref(true);
const loadError = ref('');
const loading = ref(false);

// Load user profile on mount
onMounted(async () => {
  await loadUserProfile();
});

async function loadUserProfile() {
  isLoading.value = true;
  loadError.value = '';

  try {
    // Get user from auth store first
    user.value = authStore.user.value;

    // If not in store, fetch from API
    if (!user.value) {
      user.value = await authStore.getCurrentUser();
    }
  } catch (error) {
    loadError.value = error.message || 'Failed to load profile. Please try again.';
  } finally {
    isLoading.value = false;
  }
}

async function handleLogout() {
  loading.value = true;

  try {
    await authStore.logout();
    router.push({ name: 'login' });
  } catch (error) {
    await modal.alert('Logout failed. Please try again.', {
      title: 'Error',
      variant: 'error'
    });
  } finally {
    loading.value = false;
  }
}

async function handleChangePassword() {
  await modal.alert('Password change functionality coming soon!', {
    title: 'Coming Soon',
    variant: 'info'
  });
}

async function handleUpdateProfile() {
  await modal.alert('Profile editing functionality coming soon!', {
    title: 'Coming Soon',
    variant: 'info'
  });
}

function handleDownloadApk() {
  const link = document.createElement('a');
  link.href = '/shared/client.apk';
  link.download = 'client.apk';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

async function handleDeleteAccount() {
  const confirmed = await modal.confirm(
    'Are you sure you want to delete your account? This action cannot be undone.',
    {
      title: 'Delete Account',
      variant: 'danger',
      confirmText: 'Delete Account',
      cancelText: 'Cancel'
    }
  );

  if (confirmed) {
    await modal.alert('Account deletion functionality coming soon!', {
      title: 'Coming Soon',
      variant: 'info'
    });
  }
}

function formatDate(dateString) {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}
</script>

<style scoped src="../assets/ProfileView.css"></style>

<style scoped>
.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--color-danger, #ef4444);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.logout-btn:hover:not(:disabled) {
  background: var(--color-danger-hover, #dc2626);
  transform: translateY(-1px);
}

.logout-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.profile-loading,
.profile-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  color: var(--color-text-secondary, #6b7280);
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(99, 102, 241, 0.1);
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.retry-btn {
  margin-top: 16px;
  padding: 10px 24px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-card {
  background: var(--color-surface, white);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.profile-avatar {
  margin-bottom: 24px;
}

.avatar-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.profile-info h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: var(--color-text, #111827);
}

.username {
  font-size: 16px;
  color: var(--color-text-secondary, #6b7280);
  margin: 0 0 12px 0;
}

.verification-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 20px;
  color: #22c55e;
  font-size: 14px;
  font-weight: 500;
}

.verification-badge.unverified {
  background: rgba(251, 146, 60, 0.1);
  border-color: rgba(251, 146, 60, 0.3);
  color: #fb923c;
}

.profile-section {
  background: var(--color-surface, white);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.profile-section h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 20px 0;
  color: var(--color-text, #111827);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.detail-icon {
  color: var(--color-primary, #6366f1);
  flex-shrink: 0;
  margin-top: 2px;
}

.detail-content {
  flex: 1;
}

.detail-content label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--color-text-secondary, #6b7280);
  margin-bottom: 4px;
}

.detail-content p {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text, #111827);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: var(--color-surface-alt, #f3f4f6);
  color: var(--color-text, #111827);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.action-btn:hover {
  background: var(--color-surface-hover, #e5e7eb);
  transform: translateY(-1px);
}

.action-btn.danger {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.2);
}

/* Dark mode support */
[data-theme='dark'] .profile-card,
[data-theme='dark'] .profile-section {
  background: var(--color-surface-dark, #1f2937);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

[data-theme='dark'] .profile-info h2,
[data-theme='dark'] .profile-section h3,
[data-theme='dark'] .detail-content p {
  color: #f9fafb;
}

[data-theme='dark'] .action-btn {
  background: #374151;
  border-color: #4b5563;
  color: #f9fafb;
}

[data-theme='dark'] .action-btn:hover {
  background: #4b5563;
}
</style>
