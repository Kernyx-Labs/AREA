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

#### 2. Areas List Empty
**Component**: `AreasDashboard.vue`
**API Method**: `api.getAreas()`
**Symptom**: No areas displayed even when they exist in database
**Root Cause**: Expects `data.areas` but backend returns `data.data.areas`
**Impact**: Users cannot see their existing automations.

#### 3. Area Creation Appears to Fail
**Component**: `PipelineEditor.vue`
**API Method**: `api.createArea()`
**Symptom**: Area is created in database but UI shows error message
**Root Cause**: Expects `data.area` but backend returns `data.data` (area object directly)
**Impact**: Confusing UX - area is created but user sees error.

#### 4. Workflows List Empty
**Component**: `AreasDashboard.vue` (workflows tab)
**API Method**: `api.getWorkflows()`
**Symptom**: Workflows tab shows no results
**Root Cause**: Expects `data.workflows` but backend returns `data.data.workflows`

#### 5. Service Connections Not Loading
**Component**: `ServicesView.vue` (line 114)
**API Method**: `api.getConnectedServices()`
**Symptom**: Services page shows no connected services, "Connect Service" buttons don't work
**Root Cause**: Expects array directly but gets `ApiResponse` object
**Impact**: Users cannot see or manage their service connections.

#### 6. Gmail OAuth Flow Broken
**Component**: `ServicesView.vue` (line 165)
**API Method**: `api.getGmailAuthUrl()`
**Symptom**: Gmail connection modal opens but authorization URL is undefined
**Root Cause**: Expects `authData.authUrl` but backend returns `authData.data.authUrl`
**Impact**: Users cannot connect Gmail - authentication window doesn't open or shows blank page.

#### 7. Toggle Area Status Not Working
**API Method**: `api.toggleAreaStatus()`
**Symptom**: Enable/disable toggle doesn't update area state
**Root Cause**: Expects `data.area` but backend returns `data.data`

#### 8. Workflow Creation Fails
**API Method**: `api.createWorkflow()`
**Symptom**: Cannot create new workflows
**Root Cause**: Response parsing expects `data.workflow` but backend returns `data.data`

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
**Impact**: Users don't know why operations failed or how to fix them.

#### 11. Hardcoded Service Metadata
**Components**: `ServicesView.vue` (lines 78-85), `PipelineEditor.vue` (lines 344-351)
**Symptom**: Only 6 services hardcoded (Gmail, Discord, Timer, GitHub, Dropbox, Outlook)
**Problem**: Adding new services requires frontend code changes
**Impact**: Manual frontend updates needed for each new service integration.
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

---

#### 12. Hardcoded Actions and Reactions
**Component**: `PipelineEditor.vue` (lines 353-368)
**Problem**: Actions and reactions are hardcoded in JavaScript
**Impact**: Adding new trigger/action types requires updating both frontend and backend.
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

#### 13. Static Service Discovery
**API Method**: `api.getAvailableServices()`
**Problem**: Fetches from `/about.json` (static file) instead of dynamic `/api/services` endpoint
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

**Phase 1 is completed, need testing see [Phase 1 Testing Checklist](TESTING_CHECKLIST_PHASE1.md)**

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

**All task finished but not tested. See [Phase 1 Testing Checklist](TESTING_CHECKLIST_PHASE1.md) for testing details.**

---

### Phase 2: Service Discovery Integration

#### Task 2.1: Add New Service Discovery API Methods (1 hour)
**File**: `/web/src/services/api.js`

- [ ] Add `getServices()` method:
- [ ] Add `getService()` method:
- [ ] Add `getServiceActions()` method:
- [ ] Add `getServiceReactions()` method:
- [ ] Add `getServicesWithActions()` method:
- [ ] Add `getServicesWithReactions()` method:
- [ ] Add `getServiceStats()` method:

**Testing**:
- [ ] Test each method in browser console
- [ ] Verify correct data structure returned
- [ ] Verify error handling works

---

#### Task 2.2: Update ServicesView to Use Dynamic Services (3 hours)
**File**: `/web/src/components/ServicesView.vue`

- [ ] Remove hardcoded `serviceMetadata` object (lines 78-85)

- [ ] Update `loadServices()` function to fetch from API:
- [ ] Add helper function for service colors:
- [ ] Add service statistics display in template:

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
- [ ] Add function to load actions:
- [ ] Add function to load reactions:
- [ ] Call load functions in `onMounted()`:
- [ ] Update action/reaction selection to use `id` field:

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

**Last Updated**: December 2025
**Document Version**: 1.0
**Estimated Completion**: 3 days (24 hours total)
