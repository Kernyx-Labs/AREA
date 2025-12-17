# Phase 1 Testing Checklist

Quick reference for verifying all critical flows work after migration.

---

## Prerequisites

```bash
# Start all services
docker compose up -d

# Verify containers running
docker compose ps

# Should see:
# - area-postgres (port 8088)
# - area-server (port 8080)
# - area-web (port 80)
```

---

## 1. Dashboard Tests

### Open Dashboard
- Navigate to: http://localhost:80
- Should redirect to: http://localhost:80/dashboard

### Verify KPI Cards Display
- [ ] Total Areas shows correct count (not 0)
- [ ] Active Areas shows correct count
- [ ] Connected Services shows correct count (should be 2 if Gmail and Discord connected)
- [ ] Executions (24h) shows count
- [ ] Success Rate shows percentage

### Verify Recent Activity
- [ ] Recent activity list shows recent executions
- [ ] Each activity shows success/failure icon
- [ ] Activity messages display
- [ ] Timestamps format correctly

### Check Console
- [ ] Open browser DevTools (F12)
- [ ] Check Console tab
- [ ] Should have NO red errors
- [ ] May have info/debug messages (OK)

---

## 2. Areas List Tests

### View Areas
- [ ] Areas list displays on dashboard
- [ ] Each area card shows:
  - Area name
  - Action type (Gmail)
  - Reaction type (Discord)
  - Enabled/disabled toggle
  - Last execution time

### Area Filters
- [ ] "Show Only Active" checkbox works
- [ ] Area count updates when filtered

### Area Actions
- [ ] Click toggle to disable area
- [ ] Status updates immediately
- [ ] Refresh page - status persists
- [ ] Toggle back to enable
- [ ] Click delete button
- [ ] Confirm deletion
- [ ] Area removed from list

---

## 3. Service Connection Tests

### Open Services Page
- Navigate to: http://localhost:80/services

### View Connected Services
- [ ] Gmail card shows connection status
- [ ] Discord card shows connection status
- [ ] If connected, shows "Connected" status
- [ ] If token expired, shows "Expired" status
- [ ] Expiration time displays for Gmail

### Connect Gmail (if not connected)
- [ ] Click "Connect Gmail" button
- [ ] OAuth popup opens
- [ ] Google authorization page loads
- [ ] Sign in with Google account
- [ ] Grant permissions
- [ ] Popup closes automatically
- [ ] Services page shows Gmail as "Connected"
- [ ] Refresh page - connection persists

### Connect Discord (if not connected)
- [ ] Click "Connect Discord" button
- [ ] Modal opens with instructions
- [ ] Enter Discord bot token
- [ ] Enter Discord channel ID
- [ ] Click "Test Connection"
- [ ] Success message shows
- [ ] Check Discord channel - test message received
- [ ] Click "Connect"
- [ ] Success message shows
- [ ] Modal closes
- [ ] Services page shows Discord as "Connected"

### Refresh Token (Gmail only)
- [ ] Wait for token to expire (or manually expire in DB)
- [ ] Status shows "Expired"
- [ ] Click "Refresh" button
- [ ] Alert shows "Token refreshed successfully"
- [ ] Status changes to "Connected"
- [ ] New expiration time displays

### Delete Connection
- [ ] Click "Delete" button on any service
- [ ] Confirm deletion
- [ ] Connection removed from list
- [ ] Refresh page - still deleted

---

## 4. Area Creation Tests

### Open Pipeline Editor
- [ ] Click "Create New Area" button
- [ ] Pipeline editor opens

### Configure Gmail Action
- [ ] Click on Gmail trigger card
- [ ] Action configuration form appears
- [ ] Enter Gmail label (e.g., "INBOX")
- [ ] Enter subject filter (optional)
- [ ] Enter from address filter (optional)
- [ ] Configuration saves

### Configure Discord Reaction
- [ ] Click on Discord action card
- [ ] Reaction configuration form appears
- [ ] Webhook URL auto-populated from connection
- [ ] Channel name shows
- [ ] Enter custom message template
- [ ] Use variables: {{subject}}, {{from}}, {{body}}
- [ ] Configuration saves

### Create Area
- [ ] Enter area name
- [ ] Click "Save and Activate" button
- [ ] Success message displays
- [ ] Redirects to dashboard
- [ ] New area appears in areas list
- [ ] Area shows as "Active"

---

## 5. Workflow Tests

### View Workflows
- [ ] Click "Workflows" tab on dashboard
- [ ] Workflows list displays (if any exist)
- [ ] Each workflow shows:
  - Workflow name
  - Trigger type
  - Action type
  - Active/inactive status

### Create Workflow
- [ ] Click "Create Workflow" button
- [ ] Pipeline editor opens
- [ ] Select trigger (e.g., Timer)
- [ ] Configure trigger
- [ ] Select action (e.g., Send Email)
- [ ] Configure action
- [ ] Enter workflow name
- [ ] Click "Save and Activate"
- [ ] Success message displays
- [ ] Workflow appears in list

### Toggle Workflow Status
- [ ] Click toggle on workflow
- [ ] Status updates immediately
- [ ] Refresh page - status persists

### Execute Workflow Manually
- [ ] Click "Execute" button on workflow
- [ ] Execution starts
- [ ] Success/failure message displays
- [ ] Check workflow logs for execution record

---

## 6. Error Handling Tests

### Invalid Area Creation
- [ ] Try to create area without connections
- [ ] Error message displays (specific reason)
- [ ] Form doesn't submit
- [ ] User can fix and retry

### Invalid Discord Connection
- [ ] Enter invalid bot token
- [ ] Click "Test Connection"
- [ ] Error message displays
- [ ] Message explains what's wrong
- [ ] Can correct and retry

### Network Error Simulation
- [ ] Stop backend: `docker compose stop server`
- [ ] Try to load dashboard
- [ ] Error message displays
- [ ] Restart backend: `docker compose start server`
- [ ] Refresh page - works again

### Missing Gmail Connection
- [ ] Delete Gmail connection
- [ ] Try to create Gmail → Discord area
- [ ] Error message displays
- [ ] Message says "Gmail not connected"
- [ ] Provides guidance to connect

---

## 7. End-to-End Flow Test

This test verifies the complete user journey:

### 1. Fresh Start
```bash
# Reset database (optional - deletes all data!)
docker compose down -v
docker compose up -d
```

### 2. Connect Services
- [ ] Open http://localhost:80/services
- [ ] Connect Gmail via OAuth
- [ ] Connect Discord with bot token
- [ ] Both show "Connected" status

### 3. Create Area
- [ ] Go to dashboard
- [ ] Click "Create New Area"
- [ ] Configure Gmail trigger (INBOX)
- [ ] Configure Discord action with custom message
- [ ] Name it "Email to Discord"
- [ ] Save and activate
- [ ] Area appears in dashboard

### 4. Trigger Area
- [ ] Send test email to Gmail account
- [ ] Wait up to 60 seconds (polling interval)
- [ ] Check Discord channel
- [ ] Message appears with email details

### 5. Monitor Execution
- [ ] Go to dashboard
- [ ] Check "Recent Activity" section
- [ ] New execution appears
- [ ] Status shows "SUCCESS"
- [ ] Timestamp is recent

### 6. Manage Area
- [ ] Toggle area off
- [ ] Send another email
- [ ] Wait 60 seconds
- [ ] No Discord message (area disabled)
- [ ] Toggle area back on
- [ ] Send email
- [ ] Discord message appears

---

## 8. Browser Compatibility Tests

### Chrome/Chromium
- [ ] All tests pass
- [ ] No console errors

### Firefox
- [ ] All tests pass
- [ ] OAuth popup works
- [ ] No console errors

### Safari
- [ ] All tests pass
- [ ] OAuth popup works
- [ ] No console errors

### Edge
- [ ] All tests pass
- [ ] No console errors

---

## 9. Performance Tests

### Page Load Time
- [ ] Dashboard loads in < 2 seconds
- [ ] Services page loads in < 1 second
- [ ] Pipeline editor opens instantly

### API Response Time
```bash
# Test dashboard stats
time curl -s http://localhost:8080/api/dashboard/stats > /dev/null

# Should complete in < 100ms
```

### Build Time
```bash
# Rebuild web container
time docker compose build web

# Should complete in < 30 seconds
```

---

## 10. Regression Tests

Ensure nothing broke from changes:

### Existing Functionality
- [ ] Area toggle still works
- [ ] Area deletion still works
- [ ] OAuth flow unchanged
- [ ] Discord modal unchanged
- [ ] Webhook validation unchanged

### Data Persistence
- [ ] Restart containers: `docker compose restart`
- [ ] All data persists
- [ ] Areas still active
- [ ] Connections still valid
- [ ] Settings unchanged

---

## Common Issues & Solutions

### Issue: Dashboard shows 0s
**Solution**: Backend not fully started. Wait 10 seconds and refresh.

### Issue: OAuth popup blocked
**Solution**: Allow popups for localhost in browser settings.

### Issue: Discord test message fails
**Solution**: Verify bot has permissions in channel and channel ID is correct.

### Issue: "Failed to fetch" errors
**Solution**: Backend container stopped. Run `docker compose start server`.

### Issue: Containers not starting
**Solution**:
```bash
docker compose down
docker compose up -d
docker compose logs --tail 50
```

---

## Success Criteria

Phase 1 is successful if:

- [ ] All 9 test sections pass
- [ ] Dashboard loads without errors
- [ ] Areas can be created and executed
- [ ] Services can be connected
- [ ] Error messages are clear and helpful
- [ ] No console errors during normal operation
- [ ] Build completes without warnings

---

## Test Results Log

**Date**: _______________
**Tester**: _______________
**Environment**: Docker / Local / Production

| Test Section | Pass | Fail | Notes |
|--------------|------|------|-------|
| 1. Dashboard | ☐ | ☐ | |
| 2. Areas List | ☐ | ☐ | |
| 3. Services | ☐ | ☐ | |
| 4. Area Creation | ☐ | ☐ | |
| 5. Workflows | ☐ | ☐ | |
| 6. Error Handling | ☐ | ☐ | |
| 7. End-to-End | ☐ | ☐ | |
| 8. Browsers | ☐ | ☐ | |
| 9. Performance | ☐ | ☐ | |
| 10. Regression | ☐ | ☐ | |

**Overall Result**: PASS / FAIL

**Issues Found**:

1. _________________________________________________
2. _________________________________________________
3. _________________________________________________

**Notes**:
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________

---

**Last Updated**: December 17, 2025
