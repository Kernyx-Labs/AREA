# Authentication System Implementation

This document describes the comprehensive authentication system implemented for the AREA frontend application.

## Overview

The authentication system provides secure user management with JWT-based authentication, protecting all routes except login and registration pages. Users are automatically redirected to the login page if not authenticated, and redirected to their intended destination after successful login.

## Architecture

### 1. Authentication Store (`/src/stores/authStore.js`)

A centralized state management solution using Vue's reactivity system.

**Features:**
- User state management (user info, tokens, authentication status)
- Persistent storage using localStorage
- Automatic token expiry handling
- Token refresh functionality

**Key Methods:**
- `login(email, password)` - Authenticate user and store tokens
- `register(userData)` - Register new user account
- `logout()` - Revoke tokens and clear user session
- `getCurrentUser()` - Fetch current user profile
- `refreshAccessToken()` - Refresh expired access token
- `ensureValidToken()` - Validate and refresh token if needed

**State Properties:**
- `user` - Current user information
- `accessToken` - JWT access token (1 hour expiry)
- `refreshToken` - JWT refresh token (7 days expiry)
- `isAuthenticated` - Boolean authentication status
- `isLoading` - Loading state for async operations
- `error` - Error messages from auth operations

### 2. API Service (`/src/services/api.js`)

Updated with comprehensive authentication endpoints and automatic token handling.

**Authentication Endpoints:**
- `POST /auth/login` - Login with email and password
- `POST /auth/register` - Register new user account
- `POST /auth/logout` - Revoke refresh token
- `GET /auth/me` - Get current user information
- `POST /auth/refresh` - Refresh access token

**Key Features:**
- `getAuthHeaders()` - Automatically includes JWT token in requests
- `authenticatedFetch()` - Wrapper for authenticated API calls
- Automatic 401 handling with redirect to login
- Token expiry detection and refresh

### 3. Router Guards (`/src/router/index.js`)

Navigation guards protect routes requiring authentication.

**Guard Logic:**
- Check if route requires authentication (`meta.requiresAuth`)
- Redirect unauthenticated users to `/login`
- Save intended destination for post-login redirect
- Prevent authenticated users from accessing login/register pages
- Automatic redirect to dashboard if already logged in

**Route Configuration:**
```javascript
{
  path: '/dashboard',
  meta: { requiresAuth: true }  // Protected route
}
{
  path: '/login',
  meta: { requiresAuth: false }  // Public route
}
```

### 4. Components

#### LoginView (`/src/components/LoginView.vue`)

**Features:**
- Email/password login form
- Form validation with error handling
- Remember me functionality
- Pre-fill email from registration redirect
- Redirect to intended destination after login
- Social login placeholders (Google, GitHub, Discord)
- Toggle between login and registration modes

**User Flow:**
1. Enter email and password
2. Submit form → calls `authStore.login()`
3. On success → redirect to intended page or dashboard
4. On error → display error message

#### RegisterView (`/src/components/RegisterView.vue`)

**Features:**
- Complete registration form (username, email, password, full name)
- Client-side validation:
  - Username: 3-50 characters, alphanumeric with hyphens/underscores
  - Email: Valid email format
  - Password: Min 8 chars, uppercase, lowercase, number, special char
  - Password confirmation match
  - Terms acceptance required
- Success message with countdown
- Automatic redirect to login with email pre-filled
- Beautiful gradient design matching LoginView

**User Flow:**
1. Fill registration form
2. Submit → calls `authStore.register()`
3. On success → show success message
4. After 2 seconds → redirect to login with email pre-filled
5. User completes login with password

#### ProfileView (`/src/components/ProfileView.vue`)

**Features:**
- Display user information:
  - Avatar with gradient background
  - Full name and username
  - Email verification status badge
  - Account creation date
  - User ID
- Logout button in header
- Account action buttons:
  - Change password (coming soon)
  - Edit profile (coming soon)
  - Delete account (coming soon)
- Loading and error states
- Automatic profile refresh from API

#### Sidebar (`/src/components/Sidebar.vue`)

**Updated Features:**
- Logout button at bottom of sidebar
- Uses `authStore.logout()` for proper cleanup
- Graceful error handling
- Redirect to login after logout

## Token Management

### Access Token
- **Expiry:** 1 hour
- **Storage:** localStorage as `accessToken`
- **Usage:** Included in Authorization header as `Bearer <token>`
- **Refresh:** Automatically refreshed using refresh token when expired

### Refresh Token
- **Expiry:** 7 days
- **Storage:** localStorage as `refreshToken`
- **Usage:** Used to obtain new access token
- **Revocation:** Revoked on logout via API call

### Token Expiry Handling
1. Store token expiry timestamp in localStorage
2. Check expiry before each authenticated request
3. Automatically refresh if expired or about to expire (5 min buffer)
4. On refresh failure → logout and redirect to login

## Security Features

### 1. Route Protection
- All routes require authentication by default
- Navigation guards prevent unauthorized access
- Automatic redirect to login for unauthenticated users

### 2. Token Security
- Tokens stored in localStorage (consider httpOnly cookies for production)
- Automatic cleanup on logout
- Expired token detection and refresh
- 401 response handling with forced logout

### 3. Password Requirements
Backend enforces strong passwords:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (@$!%*?&)

### 4. Session Management
- Remember me functionality (stores preference)
- Automatic logout on token expiration
- Refresh token revocation on logout
- Clear all authentication data on logout

## User Flows

### Registration Flow
```
1. User clicks "Sign Up" on login page
2. Redirected to /register
3. Fills registration form
4. Submits form → POST /auth/register
5. Success: Shows "Account created" message
6. After 2s: Redirects to /login?email=user@email.com
7. Email field pre-filled
8. User enters password and logs in
```

### Login Flow
```
1. User enters email and password
2. Submits form → POST /auth/login
3. Receives accessToken, refreshToken, user data
4. Tokens stored in localStorage
5. Redirected to intended destination or /dashboard
6. User is now authenticated
```

### Logout Flow
```
1. User clicks logout button (Sidebar or Profile)
2. Calls authStore.logout()
3. POST /auth/logout with refreshToken
4. Server revokes refresh token
5. Clear all tokens from localStorage
6. Redirect to /login
```

### Protected Route Access
```
1. User navigates to protected route (e.g., /dashboard)
2. Router guard checks authentication
3. If not authenticated:
   - Save intended destination
   - Redirect to /login?redirect=/dashboard
4. After login:
   - Redirect to saved destination (/dashboard)
```

### Token Refresh Flow
```
1. Access token expires (after 1 hour)
2. Before next API call, check token expiry
3. If expired or about to expire:
   - POST /auth/refresh with refreshToken
   - Receive new accessToken
   - Update localStorage
4. Continue with original API call
```

## Backend Integration

### API Endpoints

All authentication endpoints are prefixed with `/auth`:

#### POST /auth/register
**Request:**
```json
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecurePass123!",
  "fullName": "John Doe"
}
```

**Response (201):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "username": "johndoe",
      "fullName": "John Doe",
      "emailVerified": false,
      "createdAt": "2024-01-10T12:00:00.000Z"
    }
  }
}
```

#### POST /auth/login
**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (200):** Same as registration

#### POST /auth/refresh
**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "user@example.com"
    }
  }
}
```

#### POST /auth/logout
**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Logout successful"
}
```

#### GET /auth/me
**Headers:**
```
Authorization: Bearer eyJhbGc...
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "username": "johndoe",
    "fullName": "John Doe",
    "emailVerified": false,
    "createdAt": "2024-01-10T12:00:00.000Z"
  }
}
```

## Files Created/Modified

### Created Files:
1. `/web/src/stores/authStore.js` - Authentication state management
2. `/web/src/components/RegisterView.vue` - Registration page
3. `/web/AUTHENTICATION.md` - This documentation

### Modified Files:
1. `/web/src/services/api.js` - Added auth endpoints and token handling
2. `/web/src/router/index.js` - Added navigation guards and register route
3. `/web/src/components/LoginView.vue` - Integrated with auth store
4. `/web/src/components/ProfileView.vue` - Complete profile with user data
5. `/web/src/components/Sidebar.vue` - Updated logout with auth store

## Testing the Authentication System

### 1. Test Registration
```bash
# Start the backend server (port 8080)
# Start the frontend dev server (port 5173)

# Navigate to http://localhost:5173/register
# Fill the form:
# - Username: testuser
# - Email: test@example.com
# - Password: Test@1234
# - Accept terms

# Expected: Success message → Redirect to login with email pre-filled
```

### 2. Test Login
```bash
# On login page, email should be pre-filled
# Enter password: Test@1234
# Click Sign In

# Expected: Redirect to dashboard, authenticated
```

### 3. Test Protected Routes
```bash
# Logout from profile page
# Try to access http://localhost:5173/dashboard
# Expected: Redirect to /login?redirect=/dashboard

# Login
# Expected: Redirect back to /dashboard
```

### 4. Test Token Refresh
```bash
# Login and wait for token to expire (1 hour)
# Or manually set expired token in localStorage
# Try to access any protected route or API
# Expected: Automatic token refresh → Request succeeds
```

### 5. Test Logout
```bash
# Click logout in Sidebar or Profile
# Expected: Redirect to login, all tokens cleared
# Try to access protected route
# Expected: Redirect to login
```

## Best Practices Implemented

1. **Separation of Concerns**
   - Auth logic in dedicated store
   - API calls in service layer
   - Route protection in router guards

2. **User Experience**
   - Loading states for async operations
   - Clear error messages
   - Intended destination redirect after login
   - Email pre-fill after registration

3. **Security**
   - Password strength requirements
   - Token expiry handling
   - Automatic logout on 401
   - Refresh token revocation

4. **Code Quality**
   - Comprehensive documentation
   - Error handling throughout
   - Consistent naming conventions
   - Vue 3 Composition API best practices

5. **Accessibility**
   - Semantic HTML
   - Proper form labels
   - Keyboard navigation support
   - ARIA attributes where needed

## Future Enhancements

1. **Security Improvements**
   - Move tokens to httpOnly cookies (more secure than localStorage)
   - Implement CSRF protection
   - Add rate limiting on login attempts
   - Add captcha for registration

2. **Features**
   - Email verification flow
   - Password reset functionality
   - Two-factor authentication (2FA)
   - Social OAuth integration (Google, GitHub, Discord)
   - Remember me with longer-lived tokens
   - Session management (view/revoke active sessions)

3. **User Experience**
   - Profile editing
   - Password change
   - Account deletion with confirmation
   - Avatar upload
   - Notification preferences

4. **Monitoring**
   - Login attempt tracking
   - Failed login notifications
   - Unusual activity detection
   - Session analytics

## Troubleshooting

### Issue: "Session expired" on every request
**Solution:** Check that backend JWT secret matches and tokens are being properly generated

### Issue: Redirect loop on login
**Solution:** Verify router guard logic and token storage in localStorage

### Issue: 401 errors on authenticated requests
**Solution:** Check Authorization header format: `Bearer <token>`

### Issue: Register redirects but email not pre-filled
**Solution:** Verify query parameter handling in LoginView's onMounted hook

### Issue: Token refresh fails
**Solution:** Check refresh token validity (7-day expiry) and endpoint implementation

## Conclusion

The authentication system provides a robust, secure, and user-friendly authentication flow for the AREA application. It follows modern best practices for JWT-based authentication with proper token management, route protection, and error handling.

All requirements have been implemented:
- ✅ User registration with redirect to login
- ✅ Email pre-fill after registration
- ✅ Login with token management
- ✅ Logout functionality
- ✅ Route protection with authentication checks
- ✅ Automatic redirect for unauthenticated users
- ✅ Profile page with user information
- ✅ Token refresh handling
- ✅ Proper error handling and user feedback
