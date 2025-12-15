# Migration Plan - Complete Backend Refactoring

## Overview
This document outlines the step-by-step plan to complete the backend refactoring started in Phase 1 & 2.

**Current Status:**
- ✓ Phase 1: Quick Wins (100% Complete)
- ✓ Phase 2: Service Integration Framework (100% Complete)
- ⏳ Phase 3: Complete Migration (Pending)

**Estimated Total Time:** 6-8 days (1 developer)

---

## Task Breakdown

### Sprint 1: Complete Controller Refactoring (2 days)

#### Task 1.1: Refactor AreaController (Remaining Methods) - 3 hours
**Files to modify:**
- `/server/src/main/java/com/area/server/controller/AreaController.java`

**Methods to refactor:**
1. `listAreas()` - Remove try-catch, use ApiResponse
2. `updateAreaStatus()` - Remove try-catch, use ApiResponse
3. `getExecutionLogs()` - Remove try-catch, use ApiResponse
4. `getTriggerState()` - Remove try-catch, use ApiResponse

**Pattern to follow:**
```java
// Before
@GetMapping
public ResponseEntity<Map<String, Object>> listAreas(...) {
    try {
        List<Area> areas = areaService.listAreas();
        return ResponseEntity.ok(Map.of("success", true, "areas", areas));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

// After
@GetMapping
public ResponseEntity<ApiResponse<List<AreaResponse>>> listAreas(...) {
    List<Area> areas = areaService.listAreas();
    List<AreaResponse> responses = areas.stream()
        .map(this::mapToAreaResponse)
        .toList();
    return ResponseEntity.ok(ApiResponse.success(responses));
}
```

**Definition of Done:**
- [ ] All methods return `ApiResponse<T>`
- [ ] No try-catch blocks remain
- [ ] Proper generic types used
- [ ] Tests updated/passing

---

#### Task 1.2: Refactor WorkflowController - 4 hours
**Files to modify:**
- `/server/src/main/java/com/area/server/controller/WorkflowController.java`

**Methods to refactor (9 methods):**
1. `createWorkflow()`
2. `getWorkflow()`
3. `listWorkflows()`
4. `updateWorkflow()`
5. `deleteWorkflow()`
6. `executeWorkflow()`
7. `getExecutionHistory()`
8. `pauseWorkflow()`
9. `resumeWorkflow()`

**Challenges:**
- Multiple try-catch blocks (9 total)
- Complex business logic mixed with error handling
- May need new service methods

**Steps:**
1. Review each method's error handling
2. Extract business logic to service if needed
3. Apply ApiResponse pattern
4. Update return types
5. Add proper exception types in service layer

**Definition of Done:**
- [ ] All 9 methods refactored
- [ ] Service layer throws appropriate exceptions
- [ ] No try-catch in controller
- [ ] Tests updated

---

#### Task 1.3: Refactor Remaining Controllers - 3 hours
**Files to modify:**
- `ServiceConnectionController.java` (1 method)
- `DiscordTestController.java` (1 method)
- `DiscordConnectionController.java` (3 methods)
- `GmailDiscordController.java` (1 method)

**Approach:**
- Follow same pattern as AreaController
- Most are small, straightforward changes
- Focus on consistency

**Definition of Done:**
- [ ] All controllers use ApiResponse
- [ ] No try-catch blocks (except where truly needed)
- [ ] Consistent error handling

---

### Sprint 2: Service Layer & OAuth Migration (2 days)

#### Task 2.1: Migrate GmailOAuthController to Use Framework - 3 hours
**Files to modify:**
- `/server/src/main/java/com/area/server/controller/GmailOAuthController.java`

**Current Issues:**
- Duplicates logic now in BaseOAuthService
- Direct OAuth implementation instead of using framework

**Refactoring Steps:**

1. **Create GmailIntegration Service** (if not exists)
```java
@Service
public class GmailIntegration extends BaseOAuthService {
    // Implement interface methods
    // Move OAuth logic from controller
}
```

2. **Simplify Controller**
```java
@RestController
@RequestMapping("/api/services/gmail")
public class GmailOAuthController {

    private final GmailIntegration gmailIntegration;

    @GetMapping("/auth-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAuthUrl() {
        String state = UUID.randomUUID().toString();
        String authUrl = gmailIntegration.buildAuthorizationUrl(state);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "authUrl", authUrl,
            "state", state
        )));
    }

    @GetMapping("/callback")
    public Mono<ResponseEntity<String>> handleCallback(@RequestParam String code) {
        return gmailIntegration.exchangeAuthorizationCode(code)
            .map(connection -> ResponseEntity.ok(
                generateSuccessHtml(connection)
            ))
            .onErrorResume(error -> Mono.just(
                ResponseEntity.badRequest().body(
                    generateErrorHtml(error.getMessage())
                )
            ));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<StatusResponse>> getStatus() {
        // Implementation
    }
}
```

**Benefits:**
- Removes ~100 lines of duplicate OAuth code
- Consistent with new framework
- Easier to test

**Definition of Done:**
- [ ] GmailIntegration class created
- [ ] Implements ServiceIntegration interface
- [ ] Controller uses integration service
- [ ] OAuth flow works end-to-end
- [ ] Tests updated

---

#### Task 2.2: Update Service Layer Exception Handling - 3 hours
**Files to modify:**
- `/server/src/main/java/com/area/server/service/AreaService.java`
- `/server/src/main/java/com/area/server/service/WorkflowExecutionService.java`
- `/server/src/main/java/com/area/server/service/ServiceConnectionService.java`

**Current Issues:**
- Services throw generic `Exception`
- Some return null instead of throwing
- Inconsistent validation

**Refactoring Pattern:**

```java
// Before
public Area findById(Long id) {
    return areaRepository.findById(id).orElse(null);
}

// After
public Area findById(Long id) {
    return areaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Area", id));
}

// Before
public void validateConnection(ServiceConnection connection) throws Exception {
    if (connection.getAccessToken() == null) {
        throw new Exception("Token missing");
    }
}

// After
public void validateConnection(ServiceConnection connection) {
    if (connection.getAccessToken() == null) {
        throw new ServiceIntegrationException(
            connection.getType().name(),
            "Access token is missing"
        );
    }
}
```

**Changes Needed:**
1. Replace `orElse(null)` with `orElseThrow(ResourceNotFoundException)`
2. Replace generic `Exception` with specific exceptions
3. Add validation at service boundaries
4. Use `IllegalArgumentException` for validation
5. Use `IllegalStateException` for state violations
6. Use `ServiceIntegrationException` for external service errors

**Definition of Done:**
- [ ] All services throw specific exceptions
- [ ] No methods return null for "not found"
- [ ] Validation exceptions are descriptive
- [ ] Tests cover exception paths

---

#### Task 2.3: Refactor TokenRefreshService - 2 hours
**Files to modify:**
- `/server/src/main/java/com/area/server/service/TokenRefreshService.java`

**Current Issues:**
- Duplicates logic in BaseOAuthService
- Hardcoded to Gmail
- Should delegate to service integrations

**Refactoring Approach:**

**Option A: Delete and Use BaseOAuthService Directly**
- Controllers/schedulers call service integration's `refreshTokenIfNeeded()`
- Removes TokenRefreshService entirely

**Option B: Make it a Facade**
```java
@Service
public class TokenRefreshService {

    private final Map<ServiceConnection.ServiceType, OAuthServiceIntegration> integrations;

    public TokenRefreshService(List<OAuthServiceIntegration> integrations) {
        this.integrations = integrations.stream()
            .collect(Collectors.toMap(
                OAuthServiceIntegration::getType,
                Function.identity()
            ));
    }

    public Mono<ServiceConnection> refreshTokenIfNeeded(ServiceConnection connection) {
        OAuthServiceIntegration integration = integrations.get(connection.getType());

        if (integration == null) {
            return Mono.just(connection); // Not an OAuth service
        }

        return integration.refreshTokenIfNeeded(connection);
    }
}
```

**Recommendation:** Use Option B (Facade pattern)
- Maintains existing API for backward compatibility
- Delegates to appropriate service integration
- Easy to add new OAuth services

**Definition of Done:**
- [ ] Delegates to service integrations
- [ ] Works with all OAuth services
- [ ] Tests updated
- [ ] Existing code still works

---

### Sprint 3: Service Discovery & Polish (2 days)

#### Task 3.1: Create Service Registry - 3 hours
**Files to create:**
- `/server/src/main/java/com/area/server/service/integration/ServiceRegistry.java`
- `/server/src/main/java/com/area/server/controller/ServiceRegistryController.java`

**Purpose:**
- Auto-discover all service integrations
- Provide REST API to list available services
- Replace hardcoded service descriptors

**Implementation:**

```java
@Service
public class ServiceRegistry {

    private final Map<ServiceConnection.ServiceType, ServiceIntegration> services;

    public ServiceRegistry(List<ServiceIntegration> integrations) {
        this.services = integrations.stream()
            .collect(Collectors.toMap(
                ServiceIntegration::getType,
                Function.identity()
            ));
    }

    public List<ServiceIntegration> getAllServices() {
        return new ArrayList<>(services.values());
    }

    public ServiceIntegration getService(ServiceConnection.ServiceType type) {
        return Optional.ofNullable(services.get(type))
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service", type.name()
            ));
    }

    public List<ActionDefinition> getActionsForService(ServiceConnection.ServiceType type) {
        return getService(type).getActions();
    }

    public List<ReactionDefinition> getReactionsForService(ServiceConnection.ServiceType type) {
        return getService(type).getReactions();
    }
}
```

**Controller:**
```java
@RestController
@RequestMapping("/api/services")
public class ServiceRegistryController {

    private final ServiceRegistry serviceRegistry;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceDescriptorResponse>>> listServices() {
        List<ServiceDescriptorResponse> services = serviceRegistry.getAllServices()
            .stream()
            .map(this::toDescriptorResponse)
            .toList();

        return ResponseEntity.ok(ApiResponse.success(services));
    }

    @GetMapping("/{type}/actions")
    public ResponseEntity<ApiResponse<List<ActionDefinition>>> getActions(
            @PathVariable ServiceConnection.ServiceType type) {
        List<ActionDefinition> actions = serviceRegistry.getActionsForService(type);
        return ResponseEntity.ok(ApiResponse.success(actions));
    }

    @GetMapping("/{type}/reactions")
    public ResponseEntity<ApiResponse<List<ReactionDefinition>>> getReactions(
            @PathVariable ServiceConnection.ServiceType type) {
        List<ReactionDefinition> reactions = serviceRegistry.getReactionsForService(type);
        return ResponseEntity.ok(ApiResponse.success(reactions));
    }
}
```

**Benefits:**
- Services auto-register (no configuration needed)
- Dynamic service discovery
- Easy to add new services
- Single source of truth

**Definition of Done:**
- [ ] ServiceRegistry created
- [ ] Auto-discovers service integrations
- [ ] REST endpoints work
- [ ] Replaces hardcoded service lists
- [ ] Tests added

---

#### Task 3.2: Implement Gmail & Discord Integrations - 3 hours
**Files to create:**
- `/server/src/main/java/com/area/server/service/integration/GmailIntegration.java`
- `/server/src/main/java/com/area/server/service/integration/DiscordIntegration.java`

**GmailIntegration:**
```java
@Service
public class GmailIntegration extends BaseOAuthService {

    private final GmailService gmailService;

    // Implement ServiceIntegration methods
    // Delegate email operations to GmailService

    @Override
    public List<ActionDefinition> getActions() {
        return List.of(
            new ActionDefinition(
                "gmail.new_unread_email",
                "New Unread Email",
                "Triggered when Gmail receives an unread message",
                List.of(
                    new FieldDefinition("label", "Label", "string", false,
                        "Gmail label to monitor"),
                    new FieldDefinition("subjectContains", "Subject Contains",
                        "string", false, "Filter by subject"),
                    new FieldDefinition("fromAddress", "From Address",
                        "string", false, "Filter by sender")
                )
            )
        );
    }
}
```

**DiscordIntegration:**
```java
@Service
public class DiscordIntegration implements ServiceIntegration {

    private final DiscordService discordService;

    // Discord doesn't use OAuth (uses webhooks)
    // Simpler implementation

    @Override
    public boolean requiresAuthentication() {
        return false; // Webhook-based
    }

    @Override
    public List<ReactionDefinition> getReactions() {
        return List.of(
            new ReactionDefinition(
                "discord.send_message",
                "Send Message",
                "Send a message to Discord channel via webhook",
                List.of(
                    new FieldDefinition("webhookUrl", "Webhook URL",
                        "url", true, "Discord webhook URL"),
                    new FieldDefinition("messageTemplate", "Message Template",
                        "text", false, "Template with placeholders")
                )
            )
        );
    }
}
```

**Definition of Done:**
- [ ] Both integrations created
- [ ] Implement ServiceIntegration interface
- [ ] Define actions/reactions
- [ ] Auto-discovered by ServiceRegistry
- [ ] Existing functionality preserved

---

#### Task 3.3: Add Integration Tests - 3 hours
**Files to create:**
- `/server/src/test/java/com/area/server/integration/OAuthFlowTest.java`
- `/server/src/test/java/com/area/server/integration/ExceptionHandlingTest.java`
- `/server/src/test/java/com/area/server/integration/ServiceRegistryTest.java`

**Test Coverage:**
1. OAuth flow end-to-end (with mock)
2. Exception handling for all exception types
3. Service discovery and registration
4. ApiResponse format consistency

**Example:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class ExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFound_ReturnsApiResponseWith404() throws Exception {
        mockMvc.perform(get("/api/areas/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void validationError_ReturnsApiResponseWith400() throws Exception {
        mockMvc.perform(post("/api/areas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")) // Invalid empty body
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Validation Error"));
    }
}
```

**Definition of Done:**
- [ ] OAuth flow tested
- [ ] All exception handlers tested
- [ ] Service registry tested
- [ ] >80% code coverage for new code

---

## Phase 4: Documentation & Cleanup (Optional, 1-2 days)

### Task 4.1: Add OpenAPI Documentation - 2 hours
**Add Swagger/OpenAPI annotations to controllers**

```java
@Tag(name = "Areas", description = "AREA management endpoints")
@RestController
@RequestMapping("/api/areas")
public class AreaController {

    @Operation(summary = "Get area by ID",
               description = "Retrieves a single AREA by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Area found"),
        @ApiResponse(responseCode = "404", description = "Area not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AreaResponse>> getArea(
            @Parameter(description = "Area ID") @PathVariable Long id) {
        // ...
    }
}
```

---

### Task 4.2: Update README - 1 hour
**Update project README with:**
- New architecture overview
- API response format
- How to add new services
- Exception handling guide

---

### Task 4.3: Create Migration Guide for Team - 1 hour
**Document:**
- What changed and why
- How to use new patterns
- Migration checklist for features in progress
- Common pitfalls and solutions

---

## Testing Strategy

### Before Each Task:
1. Run existing tests to ensure they pass
2. Review impacted code

### During Development:
1. Write tests for new code
2. Update existing tests for refactored code
3. Test error paths explicitly

### After Each Task:
1. Run full test suite
2. Test API endpoints with Postman/cURL
3. Verify error responses
4. Check logs for errors

### Integration Testing:
1. Test OAuth flow with real Google account (dev environment)
2. Test all exception scenarios
3. Test service discovery
4. Verify backward compatibility

---

## Rollback Plan

### If Issues Arise:

1. **Git Branches:**
   - Work on feature branch: `refactor/backend-phase3`
   - Keep main stable
   - Merge only after thorough testing

2. **Incremental Merging:**
   - Merge one sprint at a time
   - Deploy to staging between sprints
   - Monitor for issues

3. **Feature Flags (Optional):**
   - Use feature flags for new endpoints
   - Gradually roll out changes
   - Easy rollback if needed

---

## Success Criteria

### Technical:
- [ ] All controllers use ApiResponse
- [ ] No try-catch blocks in controllers (except callbacks)
- [ ] All services throw specific exceptions
- [ ] Service integrations auto-register
- [ ] OAuth flow uses BaseOAuthService
- [ ] Test coverage >80%

### Quality:
- [ ] Code review approved
- [ ] No regression in existing features
- [ ] Performance maintained or improved
- [ ] Documentation updated

### Team:
- [ ] Team trained on new patterns
- [ ] Developer guide reviewed
- [ ] Common questions answered

---

## Timeline Summary

| Sprint | Tasks | Duration | Deliverables |
|--------|-------|----------|--------------|
| 1 | Controller Refactoring | 2 days | All controllers use ApiResponse |
| 2 | Service Layer & OAuth | 2 days | BaseOAuthService integrated |
| 3 | Service Discovery | 2 days | ServiceRegistry, integrations |
| 4 | Documentation (optional) | 1-2 days | Swagger, guides, README |

**Total:** 6-8 days (1 developer)

---

## Risk Assessment

### Low Risk:
- Controller refactoring (straightforward, well-defined pattern)
- Adding service integrations (optional, additive)

### Medium Risk:
- OAuth migration (affects authentication flow)
  - **Mitigation:** Test thoroughly, keep old code until verified
- Service layer exceptions (changes error behavior)
  - **Mitigation:** Comprehensive integration tests

### High Risk:
- None identified

---

## Resources Needed

- 1 backend developer (full-time, 1-2 weeks)
- Code review from senior developer
- QA testing (1-2 days)
- Staging environment for testing

---

## Getting Started

1. **Review this plan with the team**
2. **Create feature branch:** `git checkout -b refactor/backend-phase3`
3. **Start with Sprint 1, Task 1.1**
4. **Follow the patterns in `DEVELOPER_GUIDE.md`**
5. **Test incrementally**
6. **Merge when sprint is complete and tested**

---

**Questions?** Refer to:
- `REFACTORING_SUMMARY.md` - What was done in Phase 1 & 2
- `DEVELOPER_GUIDE.md` - How to use new patterns
- This document - What to do next
