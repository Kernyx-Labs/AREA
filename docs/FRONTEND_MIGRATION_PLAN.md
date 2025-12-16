# Frontend Migration Plan - API Response Format Update

## Overview

This document outlines the required changes to the Vue 3 frontend following the backend API refactoring. The backend now returns all responses in a standardized `ApiResponse<T>` wrapper format, which breaks compatibility with the current frontend implementation.

**Created**: December 2025
**Status**: ⚠️ CRITICAL - Frontend currently broken
**Estimated Total Effort**: 24 hours (3 days)

---

## Table of Contents

1. [What's Broken](#whats-broken)
2. [New Backend API Format](#new-backend-api-format)
3. [Migration Phases](#migration-phases)
4. [Detailed Task Checklist](#detailed-task-checklist)
5. [Testing Checklist](#testing-checklist)
6. [Code Examples](#code-examples)

---

## What's Broken

### Critical Issues (App Non-Functional)

#### 1. Dashboard Statistics Not Loading
**Component**: `AreasDashboard.vue`
**API Method**: `api.getDashboardStats()`
**Symptom**: Dashboard shows 0 for all KPI metrics (Total Areas, Active Areas, etc.)
**Root Cause**: Expects `data.stats` but backend returns `data.data.stats`

```javascript
// CURRENT CODE (BROKEN):
const data = await response.json();
return data.stats;

// BACKEND ACTUALLY RETURNS:
{
  "success": true,
  "data": {
    "stats": {
      "totalAreas": 5,
      "activeAreas": 3,
      ...
    }
  }
}
```

---

#### 2. Areas List Empty
**Component**: `AreasDashboard.vue`
**API Method**: `api.getAreas()`
**Symptom**: No areas displayed even when they exist in database
**Root Cause**: Expects `data.areas` but backend returns `data.data.areas`

```javascript
// CURRENT CODE (BROKEN):
const data = await response.json();
return data.areas || [];

// BACKEND ACTUALLY RETURNS:
{
  "success": true,
  "data": {
    "areas": [
      { "id": 1, "name": "Gmail to Discord", ... },
      { "id": 2, "name": "GitHub to Slack", ... }
    ]
  }
}
```

**Impact**: Users cannot see their existing automations.

---

#### 3. Area Creation Appears to Fail
**Component**: `PipelineEditor.vue`
**API Method**: `api.createArea()`
**Symptom**: Area is created in database but UI shows error message
**Root Cause**: Expects `data.area` but backend returns `data.data` (area object directly)

```javascript
// CURRENT CODE (BROKEN):
const data = await response.json();
return data.area;

// BACKEND ACTUALLY RETURNS:
{
  "success": true,
  "message": "Area created successfully",
  "data": {
    "id": 123,
    "name": "My Automation",
    "enabled": true,
    ...
  }
}
```

**Impact**: Confusing UX - area is created but user sees error.

---

#### 4. Workflows List Empty
**Component**: `AreasDashboard.vue` (workflows tab)
**API Method**: `api.getWorkflows()`
**Symptom**: Workflows tab shows no results
**Root Cause**: Expects `data.workflows` but backend returns `data.data.workflows`

---

#### 5. Service Connections Not Loading
**Component**: `ServicesView.vue` (line 114)
**API Method**: `api.getConnectedServices()`
**Symptom**: Services page shows no connected services, "Connect Service" buttons don't work
**Root Cause**: Expects array directly but gets `ApiResponse` object

```javascript
// CURRENT CODE (BROKEN):
return await response.json();

// Later in component (line 118):
if (Array.isArray(connectedServices)) {
  // This is FALSE because connectedServices is { success: true, data: [...] }
  connectedServices.forEach(conn => {
    // Never executes
  });
}

// BACKEND ACTUALLY RETURNS:
{
  "success": true,
  "data": [
    { "id": 1, "type": "GMAIL", "email": "user@gmail.com" },
    { "id": 2, "type": "DISCORD", "channelName": "general" }
  ]
}
```

**Impact**: Users cannot see or manage their service connections.

---

#### 6. Gmail OAuth Flow Broken
**Component**: `ServicesView.vue` (line 165)
**API Method**: `api.getGmailAuthUrl()`
**Symptom**: Gmail connection modal opens but authorization URL is undefined
**Root Cause**: Expects `authData.authUrl` but backend returns `authData.data.authUrl`

```javascript
// CURRENT CODE (BROKEN):
const authData = await api.getGmailAuthUrl();
const width = 600;
const height = 700;
const left = (screen.width / 2) - (width / 2);
const top = (screen.height / 2) - (height / 2);

// authData.authUrl is UNDEFINED!
const authWindow = window.open(
  authData.authUrl, // undefined
  'Google Authorization',
  `width=${width},height=${height},top=${top},left=${left}`
);

// BACKEND ACTUALLY RETURNS:
{
  "success": true,
  "data": {
    "authUrl": "https://accounts.google.com/o/oauth2/v2/auth?...",
    "state": "abc123..."
  }
}
```

**Impact**: Users cannot connect Gmail - authentication window doesn't open or shows blank page.

---

#### 7. Toggle Area Status Not Working
**API Method**: `api.toggleAreaStatus()`
**Symptom**: Enable/disable toggle doesn't update area state
**Root Cause**: Expects `data.area` but backend returns `data.data`

---

#### 8. Workflow Creation Fails
**API Method**: `api.createWorkflow()`
**Symptom**: Cannot create new workflows
**Root Cause**: Response parsing expects `data.workflow` but backend returns `data.data`

---

#### 9. Workflow Updates Don't Persist
**API Method**: `api.updateWorkflow()`, `api.updateWorkflowStatus()`
**Symptom**: Changes to workflows appear to save but don't persist
**Root Cause**: Response parsing expects nested structure

---

### High Priority Issues (Poor User Experience)

#### 10. Generic Error Messages
**All Components**
**Symptom**: Users see generic "Failed to..." messages instead of specific backend errors
**Root Cause**: Error handling expects `error.message` but backend sends both `error.error` and `error.message`

```javascript
// CURRENT ERROR HANDLING (BROKEN):
catch (error) {
  const errorData = await response.json();
  throw new Error(errorData.message || errorData.error || 'Failed...');
}

// BACKEND ERROR FORMAT:
{
  "success": false,
  "error": "Validation Error",
  "message": "Area name must be at least 3 characters long"
}

// RESULT: Shows "Validation Error" instead of helpful detailed message
```

**Impact**: Users don't know why operations failed or how to fix them.

---

#### 11. Hardcoded Service Metadata
**Components**: `ServicesView.vue` (lines 78-85), `PipelineEditor.vue` (lines 344-351)
**Symptom**: Only 6 services hardcoded (Gmail, Discord, Timer, GitHub, Dropbox, Outlook)
**Problem**: Adding new services requires frontend code changes

```javascript
// HARDCODED IN FRONTEND:
const serviceMetadata = {
  gmail: { displayName: 'Gmail', color: '#EA4335' },
  discord: { displayName: 'Discord', color: '#5865F2' },
  timer: { displayName: 'Timer', color: '#4285F4' },
  github: { displayName: 'GitHub', color: '#6e40c9' },
  dropbox: { displayName: 'Dropbox', color: '#0061FF' },
  outlook: { displayName: 'Outlook', color: '#0078D4' }
};
```

**Backend Now Provides** (via `/api/services`):
```json
{
  "success": true,
  "data": [
    {
      "type": "GMAIL",
      "name": "Gmail",
      "description": "Monitor Gmail inbox for new emails with customizable filters",
      "requiresAuthentication": true,
      "actionCount": 2,
      "reactionCount": 0,
      "actions": [...],
      "reactions": []
    }
  ]
}
```

**Impact**: Manual frontend updates needed for each new service integration.

---

#### 12. Hardcoded Actions and Reactions
**Component**: `PipelineEditor.vue` (lines 353-368)
**Problem**: Actions and reactions are hardcoded in JavaScript

```javascript
// HARDCODED:
const actionsList = [
  { service: 'gmail', name: 'New email received', desc: 'Triggers when a new email arrives' },
  { service: 'timer', name: 'At specific time', desc: 'Runs at scheduled time' },
  { service: 'github', name: 'New commit', desc: 'When code is pushed' },
  { service: 'github', name: 'New issue opened', desc: 'When someone creates issue' },
  { service: 'gmail', name: 'New email from sender', desc: 'From specific address' }
]

const reactionsList = [
  { service: 'discord', name: 'Send Message', desc: 'Posts to a channel' },
  { service: 'dropbox', name: 'Save File', desc: 'Stores in Dropbox' },
  { service: 'outlook', name: 'Create Event', desc: 'Adds calendar event' },
  { service: 'gmail', name: 'Send Email', desc: 'Sends to address' }
]
```

**Backend Provides** (via `/api/services/{type}/actions` and `/api/services/{type}/reactions`):
```json
{
  "success": true,
  "data": [
    {
      "id": "gmail.new_unread_email",
      "displayName": "New Unread Email",
      "description": "Triggers when a new unread email is received in Gmail inbox",
      "fields": [
        {
          "name": "label",
          "displayName": "Label",
          "type": "string",
          "required": false,
          "description": "Filter emails by Gmail label (e.g., INBOX, IMPORTANT)"
        }
      ]
    }
  ]
}
```

**Benefits of Using Backend Metadata**:
- Auto-discover new actions/reactions
- Field definitions for dynamic form generation
- Consistent naming between frontend and backend
- Type information (string, number, email, url)
- Required field validation
- Field descriptions for tooltips

**Impact**: Adding new trigger/action types requires updating both frontend and backend.

---

#### 13. Static Service Discovery
**API Method**: `api.getAvailableServices()`
**Problem**: Fetches from `/about.json` (static file) instead of dynamic `/api/services` endpoint

```javascript
// CURRENT (STATIC):
async getAvailableServices() {
  const response = await fetch(`${API_URL}/about.json`);
  if (!response.ok) throw new Error('Failed to fetch available services');
  const data = await response.json();
  return data.server.services || [];
}

// SHOULD USE DYNAMIC ENDPOINT:
async getAvailableServices() {
  const response = await fetch(`${API_URL}/api/services`);
  if (!response.ok) throw new Error('Failed to fetch available services');
  const result = await response.json();
  return result.data; // Extract from ApiResponse
}
```

**Impact**: Service list doesn't reflect actual backend capabilities.

---

### Medium Priority Issues (Nice to Have)

#### 14. No Dynamic Form Generation
**Component**: `PipelineEditor.vue`
**Problem**: Action/reaction configuration forms are manually coded
**Opportunity**: Backend provides `FieldDefinition` metadata with:
- Field name, display name, description
- Field type (string, number, email, url, text)
- Required/optional flags
- Validation rules

**Benefit**: Generate forms dynamically from metadata, reducing code and improving consistency.

---

#### 15. Manual Service Status Management
**Component**: `ServicesView.vue`
**Problem**: Manual logic to determine if service is connected
**Opportunity**: Backend provides `requiresAuthentication` flag and connection status

---

## New Backend API Format

### Success Response Format

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    // Actual response data here
  },
  "error": null
}
```

### Error Response Format

```json
{
  "success": false,
  "error": "Error Type",
  "message": "Detailed error description",
  "data": null
}
```

### Example Endpoints

#### GET /api/areas
```json
{
  "success": true,
  "data": {
    "areas": [
      {
        "id": 1,
        "name": "Gmail to Discord",
        "enabled": true,
        ...
      }
    ]
  }
}
```

#### GET /api/services
```json
{
  "success": true,
  "data": [
    {
      "type": "GMAIL",
      "name": "Gmail",
      "description": "Monitor Gmail inbox...",
      "requiresAuthentication": true,
      "actionCount": 2,
      "reactionCount": 0,
      "actions": [
        {
          "id": "gmail.new_unread_email",
          "displayName": "New Unread Email",
          "description": "Triggers when...",
          "fields": [
            {
              "name": "label",
              "displayName": "Label",
              "type": "string",
              "required": false,
              "description": "Filter by label..."
            }
          ]
        }
      ],
      "reactions": []
    }
  ]
}
```

#### GET /api/services/GMAIL/actions
```json
{
  "success": true,
  "data": [
    {
      "id": "gmail.new_unread_email",
      "displayName": "New Unread Email",
      "description": "Triggers when a new unread email is received",
      "fields": [
        {
          "name": "label",
          "displayName": "Label",
          "type": "string",
          "required": false,
          "description": "Filter emails by Gmail label"
        },
        {
          "name": "subjectContains",
          "displayName": "Subject Contains",
          "type": "string",
          "required": false,
          "description": "Filter by subject text"
        },
        {
          "name": "fromAddress",
          "displayName": "From Address",
          "type": "email",
          "required": false,
          "description": "Filter by sender email"
        }
      ]
    }
  ]
}
```

---

## Migration Phases

### Phase 1: Critical Fixes (IMMEDIATE - 4 hours)
**Goal**: Restore basic functionality to the application
**Priority**: CRITICAL - App is currently broken
**Success Criteria**: All existing features work again

**Tasks**:
1. Create `unwrapApiResponse()` helper function
2. Update all 12+ API methods in `api.js` to use wrapper
3. Fix error handling to use new format
4. Update component data parsing
5. Test all critical user flows

**Deliverables**:
- Working dashboard with correct stats
- Areas list displays correctly
- Workflows list displays correctly
- Service connections work
- Area creation/editing works
- OAuth flows work

---

### Phase 2: Service Discovery Integration (HIGH - 16 hours)
**Goal**: Remove hardcoded service metadata and use dynamic backend endpoints
**Priority**: HIGH - Enables extensibility
**Success Criteria**: New services can be added without frontend changes

**Tasks**:
1. Add new API methods for service discovery
2. Update `ServicesView.vue` to fetch services dynamically
3. Update `PipelineEditor.vue` to fetch actions/reactions from API
4. Remove hardcoded `serviceMetadata` object
5. Remove hardcoded `actionsList` and `reactionsList`
6. Add service statistics display (action/reaction counts)

**Deliverables**:
- Services page shows real-time service availability
- Action/reaction lists generated from backend
- New services appear automatically when backend adds them

---

### Phase 3: Dynamic Forms & UX Polish (MEDIUM - 24 hours)
**Goal**: Generate action/reaction configuration forms dynamically
**Priority**: MEDIUM - Improves maintainability and UX
**Success Criteria**: Forms generated from FieldDefinition metadata

**Tasks**:
1. Create form field components for each type (string, number, email, url, text)
2. Generate action configuration forms from FieldDefinition
3. Generate reaction configuration forms from FieldDefinition
4. Add field validation based on `required` flag
5. Add field descriptions as tooltips
6. Improve error message display
7. Add loading states and better UX

**Deliverables**:
- Dynamic form generation from metadata
- Better validation and error messages
- Improved user experience

---

## Detailed Task Checklist

### Phase 1: Critical Fixes

#### Task 1.1: Create API Response Helper (30 minutes)
**File**: `/web/src/services/api.js`

- [ ] Add `unwrapApiResponse()` function at top of file (after imports)
  ```javascript
  /**
   * Unwraps the standardized ApiResponse<T> wrapper from backend.
   * Throws error if response indicates failure.
   * @param {Object} response - The API response object
   * @returns {*} The unwrapped data
   * @throws {Error} If response.success is false
   */
  function unwrapApiResponse(response) {
    if (response.success === false) {
      // Backend error - throw with detailed message
      throw new Error(response.message || response.error || 'API request failed');
    }
    // Success - return the data payload
    return response.data;
  }
  ```

**Testing**:
- [ ] Verify function extracts data from success response
- [ ] Verify function throws error with message from error response

---

#### Task 1.2: Update Dashboard Stats Method (15 minutes)
**File**: `/web/src/services/api.js` (Line 85)

- [ ] Update `getDashboardStats()` method:
  ```javascript
  async getDashboardStats() {
    const response = await fetch(`${API_URL}/api/dashboard/stats`);
    if (!response.ok) throw new Error('Failed to fetch dashboard stats');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.stats;
  }
  ```

**Testing**:
- [ ] Open dashboard
- [ ] Verify KPI metrics show correct numbers (not 0)
- [ ] Check browser console for errors

---

#### Task 1.3: Update Get Areas Method (15 minutes)
**File**: `/web/src/services/api.js` (Line 94)

- [ ] Update `getAreas()` method:
  ```javascript
  async getAreas(activeOnly = false) {
    const url = activeOnly
      ? `${API_URL}/api/areas?activeOnly=true`
      : `${API_URL}/api/areas`;
    const response = await fetch(url);
    if (!response.ok) throw new Error('Failed to fetch areas');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.areas || [];
  }
  ```

**Testing**:
- [ ] Open dashboard "My Automations" tab
- [ ] Verify areas list displays
- [ ] Try "Show Only Active" filter

---

#### Task 1.4: Update Create Area Method (15 minutes)
**File**: `/web/src/services/api.js` (Line 111)

- [ ] Update `createArea()` method:
  ```javascript
  async createArea(areaData) {
    const response = await fetch(`${API_URL}/api/areas`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(areaData)
    });

    if (!response.ok) {
      const errorResult = await response.json();
      const error = unwrapApiResponse(errorResult); // Will throw
    }

    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // Area object is directly in data
  }
  ```

**Testing**:
- [ ] Create new area (Gmail → Discord)
- [ ] Verify success message shows
- [ ] Verify area appears in list
- [ ] Check that page redirects to dashboard

---

#### Task 1.5: Update Toggle Area Status Method (15 minutes)
**File**: `/web/src/services/api.js` (Line 129)

- [ ] Update `toggleAreaStatus()` method:
  ```javascript
  async toggleAreaStatus(areaId, enabled) {
    const response = await fetch(`${API_URL}/api/areas/${areaId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ enabled })
    });

    if (!response.ok) {
      const errorResult = await response.json();
      const error = unwrapApiResponse(errorResult);
    }

    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // Area object
  }
  ```

**Testing**:
- [ ] Toggle area on/off from dashboard
- [ ] Verify toggle state persists
- [ ] Verify area execution status updates

---

#### Task 1.6: Update Get Workflows Method (15 minutes)
**File**: `/web/src/services/api.js` (Line 138)

- [ ] Update `getWorkflows()` method:
  ```javascript
  async getWorkflows() {
    const response = await fetch(`${API_URL}/api/workflows`);
    if (!response.ok) throw new Error('Failed to fetch workflows');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflows || [];
  }
  ```

**Testing**:
- [ ] Open dashboard "Workflows" tab
- [ ] Verify workflows display
- [ ] Check workflow statistics

---

#### Task 1.7: Update Get Workflow Method (15 minutes)
**File**: `/web/src/services/api.js` (Line 145)

- [ ] Update `getWorkflow()` method:
  ```javascript
  async getWorkflow(id) {
    const response = await fetch(`${API_URL}/api/workflows/${id}`);
    if (!response.ok) throw new Error('Failed to fetch workflow');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflow;
  }
  ```

---

#### Task 1.8: Update Create Workflow Method (15 minutes)
**File**: `/web/src/services/api.js` (Line 161)

- [ ] Update `createWorkflow()` method:
  ```javascript
  async createWorkflow(workflowData) {
    const response = await fetch(`${API_URL}/api/workflows`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(workflowData)
    });

    if (!response.ok) {
      const errorResult = await response.json();
      const error = unwrapApiResponse(errorResult);
    }

    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflow;
  }
  ```

**Testing**:
- [ ] Create new workflow
- [ ] Verify success message
- [ ] Verify workflow appears in list

---

#### Task 1.9: Update Get Workflow Stats Method (15 minutes)
**File**: `/web/src/services/api.js` (Line 168)

- [ ] Update `getWorkflowStats()` method:
  ```javascript
  async getWorkflowStats(id) {
    const response = await fetch(`${API_URL}/api/workflows/${id}/stats`);
    if (!response.ok) throw new Error('Failed to fetch workflow stats');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.stats;
  }
  ```

---

#### Task 1.10: Update Update Workflow Methods (15 minutes)
**File**: `/web/src/services/api.js` (Lines 184, 197)

- [ ] Update `updateWorkflow()` method:
  ```javascript
  async updateWorkflow(id, updates) {
    const response = await fetch(`${API_URL}/api/workflows/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updates)
    });

    if (!response.ok) {
      const errorResult = await response.json();
      const error = unwrapApiResponse(errorResult);
    }

    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflow;
  }
  ```

- [ ] Update `updateWorkflowStatus()` method:
  ```javascript
  async updateWorkflowStatus(id, active) {
    const response = await fetch(`${API_URL}/api/workflows/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ active })
    });

    if (!response.ok) {
      const errorResult = await response.json();
      const error = unwrapApiResponse(errorResult);
    }

    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data.workflow;
  }
  ```

**Testing**:
- [ ] Edit existing workflow
- [ ] Toggle workflow active/inactive
- [ ] Verify changes persist

---

#### Task 1.11: Update Service Connection Methods (20 minutes)
**File**: `/web/src/services/api.js` (Lines 19, 26)

- [ ] Update `getConnectedServices()` method:
  ```javascript
  async getConnectedServices() {
    const response = await fetch(`${API_URL}/api/service-connections`);
    if (!response.ok) throw new Error('Failed to fetch connected services');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // Array of service connections
  }
  ```

- [ ] Update `getGmailAuthUrl()` method:
  ```javascript
  async getGmailAuthUrl() {
    const response = await fetch(`${API_URL}/api/services/gmail/auth-url`);
    if (!response.ok) throw new Error('Failed to get Gmail auth URL');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // { authUrl: "...", state: "..." }
  }
  ```

**Testing**:
- [ ] Open Services page
- [ ] Verify connected services display
- [ ] Click "Connect Gmail"
- [ ] Verify OAuth popup opens with correct URL

---

#### Task 1.12: Update Error Handling in All Methods (30 minutes)
**File**: `/web/src/services/api.js` (Multiple locations)

- [ ] Update error handling pattern in all methods:
  ```javascript
  // OLD PATTERN:
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.message || errorData.error || 'Failed...');
  }

  // NEW PATTERN (for POST/PUT/PATCH/DELETE):
  if (!response.ok) {
    const errorResult = await response.json();
    // unwrapApiResponse will throw error with proper message
    unwrapApiResponse(errorResult);
  }
  ```

- [ ] Update these methods:
  - [ ] `connectDiscord()` (line 42-46)
  - [ ] `testDiscordConnection()` (line 59-62)
  - [ ] `refreshServiceToken()` (line 74-77)
  - [ ] `deleteArea()` (line 106-109)
  - [ ] All other error handlers

**Testing**:
- [ ] Trigger various errors (invalid input, auth failure, etc.)
- [ ] Verify error messages are user-friendly and specific
- [ ] Check that detailed messages from backend are shown

---

#### Task 1.13: Update ServicesView Component (30 minutes)
**File**: `/web/src/components/ServicesView.vue`

- [ ] Update line 118 to handle new response format:
  ```javascript
  // OLD:
  const connectedServices = await api.getConnectedServices();
  if (Array.isArray(connectedServices)) {
    connectedServices.forEach(conn => {
      const serviceType = conn.type.toLowerCase();
      connectedServicesMap[serviceType] = conn;
    });
  }

  // NEW:
  const connectedServices = await api.getConnectedServices();
  // Now connectedServices is already an array (unwrapped by api.js)
  connectedServices.forEach(conn => {
    const serviceType = conn.type.toLowerCase();
    connectedServicesMap[serviceType] = conn;
  });
  ```

- [ ] Update line 165 to handle new OAuth response:
  ```javascript
  // OLD:
  const authData = await api.getGmailAuthUrl();
  const authWindow = window.open(authData.authUrl, ...);

  // NEW:
  const authData = await api.getGmailAuthUrl();
  // authData is already unwrapped: { authUrl: "...", state: "..." }
  const authWindow = window.open(authData.authUrl, ...);
  ```

**Testing**:
- [ ] Open Services page
- [ ] Verify all connected services show up
- [ ] Test "Connect Gmail" button
- [ ] Verify OAuth popup opens correctly
- [ ] Complete OAuth flow and verify connection appears

---

#### Task 1.14: Update DiscordConnectionModal Component (15 minutes)
**File**: `/web/src/components/DiscordConnectionModal.vue`

- [ ] Review response handling (likely already correct due to api.js changes)
- [ ] Verify error messages display correctly

**Testing**:
- [ ] Open "Connect Discord" modal
- [ ] Test with invalid webhook URL
- [ ] Test with valid webhook URL
- [ ] Verify connection appears in Services page

---

#### Task 1.15: Test All Critical Flows (1 hour)

- [ ] **Dashboard**
  - [ ] Stats display correctly
  - [ ] Areas list loads
  - [ ] Workflows list loads
  - [ ] Filters work (Active Only, etc.)

- [ ] **Areas**
  - [ ] Create new area (Gmail → Discord)
  - [ ] Edit existing area
  - [ ] Toggle area on/off
  - [ ] Delete area
  - [ ] View execution logs

- [ ] **Workflows**
  - [ ] Create new workflow
  - [ ] Edit workflow
  - [ ] Toggle workflow active/inactive
  - [ ] Delete workflow
  - [ ] Execute workflow manually

- [ ] **Services**
  - [ ] View connected services
  - [ ] Connect Gmail (OAuth flow)
  - [ ] Connect Discord (webhook)
  - [ ] Refresh token
  - [ ] Delete service connection
  - [ ] Test connection

- [ ] **Error Handling**
  - [ ] Invalid inputs show specific error messages
  - [ ] Network errors handled gracefully
  - [ ] Backend errors display properly

---

### Phase 2: Service Discovery Integration

#### Task 2.1: Add New Service Discovery API Methods (1 hour)
**File**: `/web/src/services/api.js`

- [ ] Add `getServices()` method:
  ```javascript
  /**
   * Get all available service integrations with metadata
   * @returns {Promise<Array>} Array of service descriptors
   */
  async getServices() {
    const response = await fetch(`${API_URL}/api/services`);
    if (!response.ok) throw new Error('Failed to fetch services');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // Array of service objects
  }
  ```

- [ ] Add `getService()` method:
  ```javascript
  /**
   * Get detailed metadata for a specific service
   * @param {string} serviceType - Service type (e.g., 'GMAIL', 'DISCORD')
   * @returns {Promise<Object>} Service descriptor with actions and reactions
   */
  async getService(serviceType) {
    const response = await fetch(`${API_URL}/api/services/${serviceType}`);
    if (!response.ok) throw new Error(`Failed to fetch ${serviceType} service`);
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  }
  ```

- [ ] Add `getServiceActions()` method:
  ```javascript
  /**
   * Get all available actions (triggers) for a service
   * @param {string} serviceType - Service type
   * @returns {Promise<Array>} Array of action definitions
   */
  async getServiceActions(serviceType) {
    const response = await fetch(`${API_URL}/api/services/${serviceType}/actions`);
    if (!response.ok) throw new Error(`Failed to fetch ${serviceType} actions`);
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // Array of ActionDefinition objects
  }
  ```

- [ ] Add `getServiceReactions()` method:
  ```javascript
  /**
   * Get all available reactions for a service
   * @param {string} serviceType - Service type
   * @returns {Promise<Array>} Array of reaction definitions
   */
  async getServiceReactions(serviceType) {
    const response = await fetch(`${API_URL}/api/services/${serviceType}/reactions`);
    if (!response.ok) throw new Error(`Failed to fetch ${serviceType} reactions`);
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // Array of ReactionDefinition objects
  }
  ```

- [ ] Add `getServicesWithActions()` method:
  ```javascript
  /**
   * Get all services that provide actions (triggers)
   * @returns {Promise<Array>} Array of service descriptors
   */
  async getServicesWithActions() {
    const response = await fetch(`${API_URL}/api/services/with-actions`);
    if (!response.ok) throw new Error('Failed to fetch services with actions');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  }
  ```

- [ ] Add `getServicesWithReactions()` method:
  ```javascript
  /**
   * Get all services that provide reactions
   * @returns {Promise<Array>} Array of service descriptors
   */
  async getServicesWithReactions() {
    const response = await fetch(`${API_URL}/api/services/with-reactions`);
    if (!response.ok) throw new Error('Failed to fetch services with reactions');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data;
  }
  ```

- [ ] Add `getServiceStats()` method:
  ```javascript
  /**
   * Get service registry statistics
   * @returns {Promise<Object>} Registry stats
   */
  async getServiceStats() {
    const response = await fetch(`${API_URL}/api/services/stats`);
    if (!response.ok) throw new Error('Failed to fetch service stats');
    const result = await response.json();
    const data = unwrapApiResponse(result);
    return data; // { totalServices, totalActions, totalReactions, ... }
  }
  ```

**Testing**:
- [ ] Test each method in browser console
- [ ] Verify correct data structure returned
- [ ] Verify error handling works

---

#### Task 2.2: Update ServicesView to Use Dynamic Services (3 hours)
**File**: `/web/src/components/ServicesView.vue`

- [ ] Remove hardcoded `serviceMetadata` object (lines 78-85)

- [ ] Update `loadServices()` function to fetch from API:
  ```javascript
  const loadServices = async () => {
    loading.value = true;
    error.value = null;

    try {
      // Fetch all available services from backend
      const availableServices = await api.getServices();

      // Fetch currently connected services
      const connectedServices = await api.getConnectedServices();

      // Create map of connected services by type
      const connectedServicesMap = {};
      connectedServices.forEach(conn => {
        const serviceType = conn.type.toLowerCase();
        connectedServicesMap[serviceType] = conn;
      });

      // Map services with connection status
      displayServices.value = availableServices.map(service => {
        const serviceName = service.type.toLowerCase();
        const connection = connectedServicesMap[serviceName];

        return {
          name: serviceName,
          displayName: service.name,
          description: service.description,
          color: getServiceColor(service.type), // Use default or from service
          icon: getServiceIcon(serviceName),
          requiresAuth: service.requiresAuthentication,
          actionCount: service.actionCount,
          reactionCount: service.reactionCount,
          connected: !!connection,
          connection: connection || null
        };
      });

    } catch (err) {
      console.error('Failed to load services:', err);
      error.value = err.message || 'Failed to load services. Please try again.';
    } finally {
      loading.value = false;
    }
  };
  ```

- [ ] Add helper function for service colors:
  ```javascript
  const getServiceColor = (serviceType) => {
    const colorMap = {
      'GMAIL': '#EA4335',
      'DISCORD': '#5865F2',
      'GITHUB': '#6e40c9',
      'SLACK': '#4A154B',
      'TRELLO': '#0079BF'
    };
    return colorMap[serviceType] || '#666666';
  };
  ```

- [ ] Add service statistics display in template:
  ```vue
  <div class="service-stats">
    <span v-if="service.actionCount > 0">
      {{ service.actionCount }} trigger{{ service.actionCount !== 1 ? 's' : '' }}
    </span>
    <span v-if="service.reactionCount > 0">
      {{ service.reactionCount }} action{{ service.reactionCount !== 1 ? 's' : '' }}
    </span>
  </div>
  ```

**Testing**:
- [ ] Services page loads correctly
- [ ] All backend services appear
- [ ] Service stats display correctly
- [ ] Connection status shows correctly
- [ ] Connect/disconnect buttons work

---

#### Task 2.3: Update PipelineEditor to Use Dynamic Actions/Reactions (4 hours)
**File**: `/web/src/components/PipelineEditor.vue`

- [ ] Remove hardcoded `actionsList` (line 353-363)

- [ ] Remove hardcoded `reactionsList` (line 365-368)

- [ ] Add reactive state for dynamic lists:
  ```javascript
  const actionsList = ref([]);
  const reactionsList = ref([]);
  const loadingActions = ref(false);
  const loadingReactions = ref(false);
  ```

- [ ] Add function to load actions:
  ```javascript
  const loadActions = async () => {
    loadingActions.value = true;
    try {
      // Get all services with actions
      const servicesWithActions = await api.getServicesWithActions();

      // Build actions list from all services
      const actions = [];
      for (const service of servicesWithActions) {
        for (const action of service.actions) {
          actions.push({
            service: service.type.toLowerCase(),
            serviceDisplayName: service.name,
            id: action.id,
            name: action.displayName,
            desc: action.description,
            fields: action.fields
          });
        }
      }

      actionsList.value = actions;
    } catch (err) {
      console.error('Failed to load actions:', err);
    } finally {
      loadingActions.value = false;
    }
  };
  ```

- [ ] Add function to load reactions:
  ```javascript
  const loadReactions = async () => {
    loadingReactions.value = true;
    try {
      // Get all services with reactions
      const servicesWithReactions = await api.getServicesWithReactions();

      // Build reactions list from all services
      const reactions = [];
      for (const service of servicesWithReactions) {
        for (const reaction of service.reactions) {
          reactions.push({
            service: service.type.toLowerCase(),
            serviceDisplayName: service.name,
            id: reaction.id,
            name: reaction.displayName,
            desc: reaction.description,
            fields: reaction.fields
          });
        }
      }

      reactionsList.value = reactions;
    } catch (err) {
      console.error('Failed to load reactions:', err);
    } finally {
      loadingReactions.value = false;
    }
  };
  ```

- [ ] Call load functions in `onMounted()`:
  ```javascript
  onMounted(async () => {
    await Promise.all([
      loadActions(),
      loadReactions()
    ]);
  });
  ```

- [ ] Update action/reaction selection to use `id` field:
  ```javascript
  // When user selects action:
  selectedAction.value = {
    ...action,
    actionId: action.id, // Use backend ID
    config: {} // Will be filled from fields
  };
  ```

**Testing**:
- [ ] Pipeline editor loads
- [ ] Actions list shows all available triggers
- [ ] Reactions list shows all available actions
- [ ] Selecting action/reaction works
- [ ] Creating area with new format works

---

#### Task 2.4: Update Service Available Check (1 hour)
**File**: `/web/src/services/api.js`

- [ ] Replace `getAvailableServices()` to use new endpoint:
  ```javascript
  async getAvailableServices() {
    // Use new service discovery endpoint instead of static about.json
    return await this.getServices();
  }
  ```

- [ ] Update any components that use `getAvailableServices()`

**Testing**:
- [ ] Verify all components using this method still work
- [ ] Check that new services appear automatically

---

#### Task 2.5: Integration Testing (2 hours)

- [ ] **End-to-End Service Discovery**
  - [ ] Start backend with Gmail and Discord integrations
  - [ ] Verify both services appear in frontend
  - [ ] Add new service integration to backend (e.g., Slack)
  - [ ] Verify new service appears in frontend without changes

- [ ] **Action/Reaction Discovery**
  - [ ] Verify all Gmail actions appear in pipeline editor
  - [ ] Verify all Discord reactions appear in pipeline editor
  - [ ] Create area using dynamically loaded actions/reactions
  - [ ] Verify area executes correctly

- [ ] **Service Metadata**
  - [ ] Verify service descriptions display correctly
  - [ ] Verify action/reaction counts are accurate
  - [ ] Verify authentication requirements shown correctly

---

### Phase 3: Dynamic Forms & UX Polish

#### Task 3.1: Create Form Field Components (4 hours)

- [ ] Create `StringField.vue` component:
  ```vue
  <template>
    <div class="form-field">
      <label :for="field.name">
        {{ field.displayName }}
        <span v-if="field.required" class="required">*</span>
        <span v-if="field.description" class="help-icon" :title="field.description">?</span>
      </label>
      <input
        :id="field.name"
        type="text"
        v-model="modelValue"
        :required="field.required"
        :placeholder="field.description"
        @input="$emit('update:modelValue', $event.target.value)"
      />
    </div>
  </template>

  <script setup>
  defineProps({
    field: Object,
    modelValue: String
  });
  defineEmits(['update:modelValue']);
  </script>
  ```

- [ ] Create `EmailField.vue` component (similar to StringField but with email validation)

- [ ] Create `UrlField.vue` component (with URL validation)

- [ ] Create `NumberField.vue` component (with numeric validation)

- [ ] Create `TextAreaField.vue` component (for long text)

- [ ] Create `BooleanField.vue` component (checkbox)

**Testing**:
- [ ] Test each component independently
- [ ] Verify validation works
- [ ] Verify required fields enforced
- [ ] Verify tooltips display

---

#### Task 3.2: Create Dynamic Form Generator (4 hours)

- [ ] Create `DynamicForm.vue` component:
  ```vue
  <template>
    <div class="dynamic-form">
      <component
        v-for="field in fields"
        :key="field.name"
        :is="getFieldComponent(field.type)"
        :field="field"
        v-model="formData[field.name]"
      />
    </div>
  </template>

  <script setup>
  import { ref, watch } from 'vue';
  import StringField from './fields/StringField.vue';
  import EmailField from './fields/EmailField.vue';
  import UrlField from './fields/UrlField.vue';
  import NumberField from './fields/NumberField.vue';
  import TextAreaField from './fields/TextAreaField.vue';
  import BooleanField from './fields/BooleanField.vue';

  const props = defineProps({
    fields: Array, // Array of FieldDefinition
    initialData: Object
  });

  const emit = defineEmits(['update']);

  const formData = ref(props.initialData || {});

  const getFieldComponent = (type) => {
    const components = {
      'string': StringField,
      'email': EmailField,
      'url': UrlField,
      'number': NumberField,
      'text': TextAreaField,
      'boolean': BooleanField
    };
    return components[type] || StringField;
  };

  watch(formData, (newValue) => {
    emit('update', newValue);
  }, { deep: true });
  </script>
  ```

**Testing**:
- [ ] Test with various field definitions
- [ ] Verify all field types render correctly
- [ ] Verify form data updates correctly
- [ ] Verify initial data populates fields

---

#### Task 3.3: Integrate Dynamic Forms into PipelineEditor (4 hours)

- [ ] Replace hardcoded action config form with DynamicForm
- [ ] Replace hardcoded reaction config form with DynamicForm
- [ ] Update form submission to use field metadata

**Example**:
```vue
<DynamicForm
  v-if="selectedAction"
  :fields="selectedAction.fields"
  :initialData="selectedAction.config"
  @update="selectedAction.config = $event"
/>
```

**Testing**:
- [ ] Select Gmail action
- [ ] Verify form shows label, subject, from fields
- [ ] Enter configuration values
- [ ] Create area
- [ ] Verify configuration saved correctly

---

#### Task 3.4: Add Field Validation (2 hours)

- [ ] Add validation to each field component
- [ ] Show validation errors inline
- [ ] Prevent form submission if validation fails
- [ ] Add regex validation for email/URL fields

**Testing**:
- [ ] Try to submit form with missing required fields
- [ ] Try to enter invalid email address
- [ ] Try to enter invalid URL
- [ ] Verify error messages display

---

#### Task 3.5: Improve Error Display (2 hours)

- [ ] Create `ErrorAlert.vue` component for consistent error display
- [ ] Update all components to use ErrorAlert
- [ ] Show backend error messages prominently
- [ ] Add error icons and styling

**Testing**:
- [ ] Trigger various errors
- [ ] Verify messages are clear and helpful
- [ ] Verify errors are visually prominent

---

#### Task 3.6: Add Loading States (2 hours)

- [ ] Add loading spinners to all async operations
- [ ] Disable buttons during loading
- [ ] Show skeleton loaders for lists
- [ ] Add loading indicators to forms

**Testing**:
- [ ] Test with slow network
- [ ] Verify all loading states appear
- [ ] Verify buttons disabled during operations

---

#### Task 3.7: Final Integration Testing (4 hours)

- [ ] Test complete workflow from scratch:
  - [ ] New user connects services
  - [ ] Creates areas using dynamic forms
  - [ ] Edits areas
  - [ ] Views execution logs
  - [ ] Manages service connections

- [ ] Test error scenarios:
  - [ ] Network failures
  - [ ] Invalid inputs
  - [ ] Backend errors
  - [ ] Auth failures

- [ ] Test across browsers:
  - [ ] Chrome
  - [ ] Firefox
  - [ ] Safari
  - [ ] Edge

---

## Testing Checklist

### Smoke Tests (After Phase 1)

- [ ] **Application Loads**
  - [ ] Dashboard loads without errors
  - [ ] No console errors on page load
  - [ ] All navigation links work

- [ ] **Dashboard**
  - [ ] Total Areas shows correct count
  - [ ] Active Areas shows correct count
  - [ ] Services Connected shows correct count
  - [ ] Recent activity displays
  - [ ] Charts render correctly

- [ ] **Areas Management**
  - [ ] Areas list displays
  - [ ] Can create new area
  - [ ] Can edit existing area
  - [ ] Can delete area
  - [ ] Can toggle area on/off
  - [ ] Area details display correctly

- [ ] **Services Management**
  - [ ] Services list displays
  - [ ] Connected services show correctly
  - [ ] Can connect Gmail
  - [ ] Can connect Discord
  - [ ] OAuth flow works
  - [ ] Can disconnect services

---

### Regression Tests (After Phase 2)

- [ ] **Service Discovery**
  - [ ] All backend services appear
  - [ ] Service descriptions display
  - [ ] Action/reaction counts correct
  - [ ] Authentication requirements shown

- [ ] **Dynamic Actions/Reactions**
  - [ ] Pipeline editor shows all actions
  - [ ] Pipeline editor shows all reactions
  - [ ] Can create area with any combination
  - [ ] Configuration forms work

- [ ] **Backward Compatibility**
  - [ ] Existing areas still work
  - [ ] Existing workflows still work
  - [ ] Existing service connections still work

---

### User Acceptance Tests (After Phase 3)

- [ ] **New User Journey**
  - [ ] User can register/login
  - [ ] User can connect first service
  - [ ] User can create first area
  - [ ] User understands what's happening
  - [ ] Error messages are helpful

- [ ] **Power User Journey**
  - [ ] Can manage multiple areas
  - [ ] Can manage multiple services
  - [ ] Can edit complex configurations
  - [ ] Can view execution history
  - [ ] Can troubleshoot failures

- [ ] **Error Handling**
  - [ ] Network errors handled gracefully
  - [ ] Invalid inputs prevented
  - [ ] Backend errors displayed clearly
  - [ ] User knows how to fix issues

---

## Code Examples

### Example 1: API Method Update Pattern

```javascript
// BEFORE:
async getAreas(activeOnly = false) {
  const url = activeOnly ? `${API_URL}/api/areas?activeOnly=true` : `${API_URL}/api/areas`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch areas');
  const data = await response.json();
  return data.areas || [];
}

// AFTER:
async getAreas(activeOnly = false) {
  const url = activeOnly ? `${API_URL}/api/areas?activeOnly=true` : `${API_URL}/api/areas`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch areas');
  const result = await response.json();
  const data = unwrapApiResponse(result); // Extracts data, handles errors
  return data.areas || [];
}
```

---

### Example 2: Error Handling Pattern

```javascript
// BEFORE:
catch (err) {
  console.error('Connection failed:', err);
  error.value = 'Failed to connect. Please try again.';
}

// AFTER:
catch (err) {
  console.error('Connection failed:', err);
  // err.message now contains specific backend error
  error.value = err.message || 'Failed to connect. Please try again.';
}
```

---

### Example 3: Dynamic Service Loading

```javascript
// BEFORE (HARDCODED):
const serviceMetadata = {
  gmail: { displayName: 'Gmail', color: '#EA4335' },
  discord: { displayName: 'Discord', color: '#5865F2' }
};

// AFTER (DYNAMIC):
const services = await api.getServices();
displayServices.value = services.map(service => ({
  name: service.type.toLowerCase(),
  displayName: service.name,
  description: service.description,
  color: getServiceColor(service.type),
  actionCount: service.actionCount,
  reactionCount: service.reactionCount,
  requiresAuth: service.requiresAuthentication
}));
```

---

### Example 4: Dynamic Form Generation

```vue
<!-- BEFORE (HARDCODED): -->
<div class="config-field">
  <label>Gmail Label</label>
  <input v-model="config.label" type="text" />
</div>

<!-- AFTER (DYNAMIC): -->
<DynamicForm
  :fields="selectedAction.fields"
  :initialData="config"
  @update="config = $event"
/>
```

---

## Success Criteria

### Phase 1 Success Criteria
- ✅ All critical features work (dashboard, areas, workflows, services)
- ✅ No console errors on normal operations
- ✅ Error messages display correctly
- ✅ Existing areas and workflows continue to work
- ✅ OAuth flows work (Gmail, Discord)

### Phase 2 Success Criteria
- ✅ Services list loaded from backend
- ✅ Actions/reactions loaded dynamically
- ✅ New services appear automatically when added to backend
- ✅ Service metadata displays correctly
- ✅ No hardcoded service definitions remain

### Phase 3 Success Criteria
- ✅ Configuration forms generated from metadata
- ✅ Field validation works correctly
- ✅ All field types supported (string, email, url, number, text, boolean)
- ✅ Tooltips show field descriptions
- ✅ User experience is polished and professional

---

## Rollback Plan

If issues arise during migration:

1. **Phase 1 Issues**:
   - Revert `api.js` changes
   - Keep old response parsing
   - Backend is backward compatible (returns ApiResponse wrapper)

2. **Phase 2 Issues**:
   - Keep dynamic service loading but fallback to hardcoded if API fails
   - Add try-catch around service discovery calls

3. **Phase 3 Issues**:
   - Keep legacy forms alongside dynamic forms
   - Use feature flag to switch between old/new forms

---

## Resources

- **Backend API Documentation**: `/docs/api-response-format.md`
- **Service Integration Guide**: `/docs/service-integration-guide.md`
- **Backend Migration Plan**: `/docs/MIGRATION_PLAN.md`
- **Vue 3 Documentation**: https://vuejs.org/
- **Fetch API Documentation**: https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API

---

## Questions or Issues?

If you encounter problems during migration:

1. Check browser console for errors
2. Check backend logs for API errors
3. Verify backend is running and endpoints are accessible
4. Test API endpoints directly with curl/Postman
5. Compare actual response format with expected format

---

**Last Updated**: December 2025
**Document Version**: 1.0
**Estimated Completion**: 3 days (24 hours total)
