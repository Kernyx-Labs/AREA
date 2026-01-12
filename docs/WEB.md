# AREA Web Client Documentation

## 🌐 Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Prerequisites](#prerequisites)
4. [Installation & Setup](#installation--setup)
5. [Project Structure](#project-structure)
6. [Architecture](#architecture)
7. [Build & Run](#build--run)
8. [Development Guide](#development-guide)
9. [Testing](#testing)
10. [Deployment](#deployment)
11. [Troubleshooting](#troubleshooting)

---

## Overview

The AREA web client is a modern single-page application (SPA) built with Vue.js 3 that provides a browser-based interface for managing automation workflows. It features a responsive design, real-time updates, and an intuitive drag-and-drop pipeline editor.

### Key Features

- **Responsive Dashboard**: Visual overview of all AREAs with real-time status
- **Pipeline Editor**: Interactive drag-and-drop workflow builder
- **Service Management**: Connect and configure external services
- **User Profile**: Manage account settings and connected services
- **Modern UI/UX**: Clean, intuitive interface with custom styling
- **Single Page Application**: Fast navigation without page reloads
- **OAuth2 Integration**: Secure authentication with external providers

### Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

---

## Technology Stack

### Core Framework
- **Vue.js**: 3.5.22
- **Vue Router**: 4.4.5

### Build Tools
- **Vite**: 7.1.11 - Fast build tool and dev server
- **@vitejs/plugin-vue**: 6.0.1 - Vue 3 plugin for Vite

### UI Libraries
- **lucide-vue-next**: 0.454.0 - Icon library

### Dev Tools
- **vite-plugin-vue-devtools**: 8.0.3 - Enhanced debugging

### Runtime Requirements
- **Node.js**: 20.19.0+ or 22.12.0+
- **npm**: 10+

---

## Prerequisites

### Required Software

1. **Node.js and npm**:
   ```bash
   # Check versions
   node --version  # Should be 20.19+ or 22.12+
   npm --version   # Should be 10+

   # Install from https://nodejs.org/
   ```

2. **Git**:
   ```bash
   git --version
   ```

### Recommended Tools

- **VS Code** with extensions:
  - Volar (Vue Language Features)
  - ESLint
  - Prettier
- **Vue DevTools** browser extension
- **Postman** or similar for API testing

---

## Installation & Setup

### 1. Navigate to Web Directory

```bash
cd AREA/web
```

### 2. Install Dependencies

```bash
# Install all dependencies from package.json
npm install

# Or using Yarn
yarn install

# Or using pnpm
pnpm install
```

### 3. Configure Environment

Create `.env.local` file for local development:

```bash
# .env.local
VITE_API_URL=http://localhost:8080
VITE_APP_TITLE=AREA Platform
VITE_OAUTH_GOOGLE_CLIENT_ID=your_google_client_id
VITE_OAUTH_GITHUB_CLIENT_ID=your_github_client_id
```

**Environment Variables Explained**:
- `VITE_API_URL`: Backend API base URL
- `VITE_APP_TITLE`: Application title
- `VITE_OAUTH_*`: OAuth2 client IDs for authentication

### 4. Verify Setup

```bash
# Start development server
npm run dev

# Should see output:
# VITE vx.x.x  ready in xxx ms
# ➜  Local:   http://localhost:5173/
# ➜  Network: use --host to expose
```

---

## Project Structure

```
web/
├── public/                       # Static assets (served as-is)
│   └── (favicon, images, etc.)
│
├── src/                          # Source code
│   ├── main.js                  # Application entry point
│   ├── App.vue                  # Root component
│   │
│   ├── assets/                  # Stylesheets and images
│   │   ├── AreasDashboard.css       # Dashboard styles
│   │   ├── DashboardGrid.css        # Grid component styles
│   │   ├── DashboardLayout.css      # Layout styles
│   │   ├── NavBar.css               # Navigation styles
│   │   ├── PipelineEditor.css       # Editor styles
│   │   ├── ProfileView.css          # Profile styles
│   │   ├── ServicesView.css         # Services styles
│   │   ├── Sidebar.css              # Sidebar styles
│   │   ├── StatCard.css             # Statistics card styles
│   │   └── styles.css               # Global styles
│   │
│   ├── components/              # Vue components
│   │   ├── AreasDashboard.vue       # Dashboard main view
│   │   ├── DashboardGrid.vue        # Areas grid display
│   │   ├── DashboardLayout.vue      # Main layout wrapper
│   │   ├── DiscordConnectionModal.vue # Discord OAuth modal
│   │   ├── NavBar.vue               # Top navigation bar
│   │   ├── PipelineEditor.vue       # Visual workflow editor
│   │   ├── ProfileView.vue          # User profile page
│   │   ├── ServicesView.vue         # Service management
│   │   ├── Sidebar.vue              # Side navigation
│   │   └── StatCard.vue             # Dashboard statistics
│   │
│   ├── composables/             # Composition API logic
│   │   └── useDashboardData.js      # Dashboard data management
│   │
│   ├── router/                  # Vue Router configuration
│   │   └── index.js                 # Route definitions
│   │
│   └── services/                # API and external services
│       └── api.js                   # HTTP client and API calls
│
├── .gitignore                   # Git ignore rules
├── Dockerfile                   # Docker build configuration
├── index.html                   # HTML entry point
├── jsconfig.json                # JavaScript configuration
├── nginx.conf                   # Nginx configuration for production
├── package.json                 # Dependencies and scripts
├── package-lock.json            # Locked dependency versions
├── README.md                    # Web-specific README
├── sonar-project.properties     # SonarQube configuration
├── vite.config.js               # Vite build configuration
└── Jenkinsfile                  # CI/CD pipeline
```

### Key Files Explained

- **`main.js`**: Initializes Vue app, router, and global plugins
- **`App.vue`**: Root component with router-view
- **`vite.config.js`**: Build configuration, plugins, proxy settings
- **`package.json`**: Project metadata, dependencies, and npm scripts
- **`index.html`**: HTML shell for SPA
- **`nginx.conf`**: Production server configuration

---

## Architecture

### Application Architecture

```
┌─────────────────────────────────────────┐
│           View Layer (Vue Components)    │
│     Dashboard │ Editor │ Profile │ etc. │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Composition API / State           │
│    (Composables, Reactive Data)         │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           Router (Vue Router)           │
│      (Navigation & Route Guards)        │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│        Service Layer (API Client)       │
│    (HTTP Requests, Error Handling)      │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Backend REST API                │
└─────────────────────────────────────────┘
```

### Component Hierarchy

```
App.vue
├── DashboardLayout.vue
│   ├── NavBar.vue
│   ├── Sidebar.vue
│   └── RouterView
│       ├── AreasDashboard.vue
│       │   ├── StatCard.vue
│       │   └── DashboardGrid.vue
│       ├── PipelineEditor.vue
│       ├── ServicesView.vue
│       │   └── DiscordConnectionModal.vue
│       └── ProfileView.vue
```

### Data Flow

1. **User Interaction** → Component Event Handler
2. **Event Handler** → Composable/State Update
3. **State Update** → API Service Call
4. **API Call** → Backend Server
5. **Response** → State Update
6. **Reactive State** → Component Re-render

### Routing Strategy

```javascript
// router/index.js structure
{
  '/': 'DashboardLayout',
  '/dashboard': 'AreasDashboard',
  '/editor': 'PipelineEditor',
  '/services': 'ServicesView',
  '/profile': 'ProfileView',
  '/login': 'LoginView',
  '/register': 'RegisterView'
}
```

---

## Build & Run

### Development Mode

```bash
# Start development server with hot reload
npm run dev

# Access at http://localhost:5173
# - Hot Module Replacement (HMR) enabled
# - Source maps for debugging
# - Fast refresh on file changes
```

**Development Server Features**:
- Instant HMR (Hot Module Replacement)
- Automatic browser refresh
- Error overlay in browser
- Source maps for debugging
- Fast rebuild times

### Preview Production Build

```bash
# Build for production
npm run build

# Preview the production build locally
npm run preview

# Access at http://localhost:4173
```

### Build for Production

```bash
# Build optimized production bundle
npm run build

# Output directory: dist/
# Contains:
# - index.html
# - assets/
#   - *.js (minified JavaScript)
#   - *.css (minified CSS)
#   - images and other assets
```

**Build Optimizations**:
- Code splitting
- Tree shaking
- Minification
- Gzip compression
- Asset optimization

### Docker Build

```bash
# Build Docker image
docker build -t area-web:latest .

# Run container
docker run -p 80:80 area-web:latest

# Or use docker-compose from root
cd ..
docker-compose up web
```

---

## Development Guide

### Creating a New Component

```vue
<!-- src/components/MyComponent.vue -->
<template>
  <div class="my-component">
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <button @click="handleClick">Click Me</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// Props
const props = defineProps({
  title: {
    type: String,
    required: true
  },
  description: String
})

// State
const count = ref(0)

// Methods
const handleClick = () => {
  count.value++
  console.log('Clicked!', count.value)
}

// Lifecycle
import { onMounted } from 'vue'
onMounted(() => {
  console.log('Component mounted')
})
</script>

<style scoped>
.my-component {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

button {
  background-color: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  background-color: #45a049;
}
</style>
```

### Adding a New Route

```javascript
// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import MyComponent from '../components/MyComponent.vue'

const routes = [
  // ... existing routes
  {
    path: '/my-page',
    name: 'MyPage',
    component: MyComponent,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const isAuthenticated = !!localStorage.getItem('token')

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

### Creating a Composable

```javascript
// src/composables/useAuth.js
import { ref, computed } from 'vue'
import api from '../services/api'

export function useAuth() {
  const user = ref(null)
  const token = ref(localStorage.getItem('token'))
  const isAuthenticated = computed(() => !!token.value)

  const login = async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password })
      token.value = response.data.token
      user.value = response.data.user
      localStorage.setItem('token', token.value)
      return true
    } catch (error) {
      console.error('Login failed:', error)
      return false
    }
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
  }

  return {
    user,
    token,
    isAuthenticated,
    login,
    logout
  }
}
```

### API Service Integration

```javascript
// src/services/api.js
import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Redirect to login
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// API methods
export default {
  // Auth
  login(credentials) {
    return apiClient.post('/api/auth/login', credentials)
  },

  register(userData) {
    return apiClient.post('/api/auth/register', userData)
  },

  // Areas
  getAreas() {
    return apiClient.get('/api/areas')
  },

  createArea(areaData) {
    return apiClient.post('/api/areas', areaData)
  },

  updateArea(id, areaData) {
    return apiClient.put(`/api/areas/${id}`, areaData)
  },

  deleteArea(id) {
    return apiClient.delete(`/api/areas/${id}`)
  },

  // Services
  getServices() {
    return apiClient.get('/api/services')
  },

  connectService(serviceId, credentials) {
    return apiClient.post(`/api/services/${serviceId}/connect`, credentials)
  }
}
```

### State Management Pattern

```javascript
// src/composables/useStore.js
import { reactive, readonly } from 'vue'

const state = reactive({
  user: null,
  areas: [],
  services: [],
  loading: false,
  error: null
})

const actions = {
  setUser(user) {
    state.user = user
  },

  setAreas(areas) {
    state.areas = areas
  },

  addArea(area) {
    state.areas.push(area)
  },

  removeArea(id) {
    const index = state.areas.findIndex(a => a.id === id)
    if (index !== -1) {
      state.areas.splice(index, 1)
    }
  },

  setLoading(loading) {
    state.loading = loading
  },

  setError(error) {
    state.error = error
  }
}

export function useStore() {
  return {
    state: readonly(state),
    ...actions
  }
}
```

### Styling Guidelines

1. **Use Scoped Styles**:
```vue
<style scoped>
/* Styles only apply to this component */
.my-class { }
</style>
```

2. **CSS Variables** (in assets/styles.css):
```css
:root {
  --primary-color: #4CAF50;
  --secondary-color: #2196F3;
  --background-color: #f5f5f5;
  --text-color: #333;
  --border-radius: 8px;
}

.button-primary {
  background-color: var(--primary-color);
  border-radius: var(--border-radius);
}
```

3. **Responsive Design**:
```css
/* Mobile First */
.container {
  padding: 10px;
}

/* Tablet */
@media (min-width: 768px) {
  .container {
    padding: 20px;
  }
}

/* Desktop */
@media (min-width: 1024px) {
  .container {
    padding: 40px;
  }
}
```

---

## Testing

### Unit Testing Setup

```bash
# Install testing dependencies
npm install --save-dev vitest @vue/test-utils jsdom
```

Update `package.json`:
```json
{
  "scripts": {
    "test": "vitest",
    "test:ui": "vitest --ui",
    "coverage": "vitest --coverage"
  }
}
```

### Writing Tests

```javascript
// src/components/__tests__/StatCard.spec.js
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatCard from '../StatCard.vue'

describe('StatCard', () => {
  it('renders properly', () => {
    const wrapper = mount(StatCard, {
      props: {
        title: 'Total Areas',
        value: 42,
        icon: 'activity'
      }
    })

    expect(wrapper.text()).toContain('Total Areas')
    expect(wrapper.text()).toContain('42')
  })

  it('emits click event', async () => {
    const wrapper = mount(StatCard, {
      props: { title: 'Test', value: 1 }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted()).toHaveProperty('click')
  })
})
```

### Component Testing

```javascript
// src/composables/__tests__/useAuth.spec.js
import { describe, it, expect, beforeEach } from 'vitest'
import { useAuth } from '../useAuth'

describe('useAuth', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('initializes with no user', () => {
    const { user, isAuthenticated } = useAuth()
    expect(user.value).toBeNull()
    expect(isAuthenticated.value).toBe(false)
  })

  it('logs in successfully', async () => {
    const { login, isAuthenticated } = useAuth()
    // Mock API call
    const result = await login('test@example.com', 'password')
    expect(result).toBe(true)
    expect(isAuthenticated.value).toBe(true)
  })
})
```

### Running Tests

```bash
# Run tests
npm run test

# Run tests in watch mode
npm run test -- --watch

# Run with coverage
npm run coverage

# Run specific test file
npm run test src/components/__tests__/StatCard.spec.js
```

---

## Deployment

### Production Build

```bash
# Build for production
npm run build

# Output: dist/ directory
```

### Deploy with Nginx

1. **Copy build files**:
```bash
sudo cp -r dist/* /var/www/html/area/
```

2. **Nginx configuration** (`/etc/nginx/sites-available/area`):
```nginx
server {
    listen 80;
    server_name area.yourdomain.com;
    root /var/www/html/area;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # Cache static assets
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

3. **Enable and restart**:
```bash
sudo ln -s /etc/nginx/sites-available/area /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### Docker Deployment

```bash
# Build image
docker build -t area-web:1.0 .

# Run container
docker run -d -p 80:80 --name area-web area-web:1.0

# Or use docker-compose
docker-compose up -d web
```

### Environment-Specific Builds

```bash
# Development
VITE_API_URL=http://localhost:8080 npm run build

# Staging
VITE_API_URL=https://staging-api.area.com npm run build

# Production
VITE_API_URL=https://api.area.com npm run build
```

### Performance Optimization

1. **Code Splitting**:
```javascript
// Lazy load routes
const PipelineEditor = () => import('../components/PipelineEditor.vue')
```

2. **Asset Optimization**:
   - Compress images
   - Use WebP format
   - Lazy load images

3. **Enable Compression**:
   - Gzip/Brotli in nginx
   - Pre-compress assets during build

---

## Troubleshooting

### Common Issues

#### 1. Vite Dev Server Won't Start

**Error**: `Port 5173 is already in use`

**Solution**:
```bash
# Kill process using port
lsof -ti:5173 | xargs kill -9

# Or use different port
npm run dev -- --port 3000
```

#### 2. Cannot Connect to Backend

**Error**: CORS errors or network errors

**Solution**:
- Check `VITE_API_URL` environment variable
- Verify backend CORS configuration allows frontend origin
- Use Vite proxy in `vite.config.js`:
```javascript
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

#### 3. Build Fails

**Error**: `Module not found` or dependency errors

**Solution**:
```bash
# Clean install
rm -rf node_modules package-lock.json
npm install

# Clear cache
npm cache clean --force
```

#### 4. Hot Reload Not Working

**Solution**:
- Check file watcher limits (Linux):
```bash
echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

#### 5. Production Build Issues

**Solution**:
```bash
# Check build output
npm run build -- --debug

# Preview locally
npm run preview
```

### Performance Issues

1. **Slow Build Times**:
```bash
# Use faster dependency resolution
npm install --legacy-peer-deps

# Enable build cache
# Update vite.config.js
export default {
  cacheDir: '.vite'
}
```

2. **Large Bundle Size**:
   - Analyze bundle:
```bash
npm install --save-dev rollup-plugin-visualizer
```
   - Add to vite.config.js:
```javascript
import { visualizer } from 'rollup-plugin-visualizer'

export default {
  plugins: [
    visualizer({ open: true })
  ]
}
```

### Debug Mode

```bash
# Run with verbose logging
DEBUG=vite:* npm run dev

# Check source maps
npm run build -- --sourcemap
```

---

## Best Practices

1. **Component Design**:
   - Keep components small and focused
   - Use props for data down, events for data up
   - Prefer composition API over options API

2. **Performance**:
   - Use `v-memo` for expensive renders
   - Lazy load routes and components
   - Implement virtual scrolling for long lists

3. **Code Organization**:
   - Group related files together
   - Use consistent naming conventions
   - Create reusable composables

4. **Security**:
   - Never store sensitive data in localStorage
   - Validate all user input
   - Use HTTPS in production
   - Implement CSRF protection

5. **Accessibility**:
   - Use semantic HTML
   - Add ARIA labels where needed
   - Test with keyboard navigation
   - Ensure proper color contrast

---

## Additional Resources

- **Vue.js Documentation**: https://vuejs.org/
- **Vue Router**: https://router.vuejs.org/
- **Vite Documentation**: https://vitejs.dev/
- **Vue Composition API**: https://vuejs.org/guide/extras/composition-api-faq.html
- **Vue DevTools**: https://devtools.vuejs.org/

---

*Last Updated: December 2025*
