# AREA Backend Developer Guide

## Quick Reference for New Patterns

### 1. Using ApiResponse in Controllers

**Old Pattern (Don't Use):**
```java
@GetMapping("/{id}")
public ResponseEntity<Map<String, Object>> getArea(@PathVariable Long id) {
    try {
        Area area = areaService.findById(id);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "area", area
        ));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    }
}
```

**New Pattern (Use This):**
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<AreaResponse>> getArea(@PathVariable Long id) {
    Area area = areaService.findById(id);
    AreaResponse response = mapToAreaResponse(area);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**Key Points:**
- No try-catch needed - GlobalExceptionHandler handles errors
- Type-safe with generics: `ApiResponse<YourDataType>`
- Service throws exceptions, controller doesn't catch them
- Consistent response format automatically

### 2. Exception Handling

**Available Exceptions:**

```java
// Resource not found (404)
throw new ResourceNotFoundException("Area", id);

// Service integration error (502)
throw new ServiceIntegrationException("Gmail", "Token expired");

// Validation error (400) - Spring handles this automatically
// Just use @Valid annotation

// Business logic validation (400)
throw new IllegalArgumentException("Invalid configuration");

// Invalid state (409)
throw new IllegalStateException("Cannot delete active area");
```

**GlobalExceptionHandler automatically converts these to:**
```json
{
  "success": false,
  "error": "Not Found",
  "message": "Area not found with id: 123"
}
```

### 3. Adding a New OAuth Service

**Step 1: Create Integration Class**

```java
package com.area.server.service.integration;

import com.area.server.service.integration.oauth.BaseOAuthService;
import org.springframework.stereotype.Service;

@Service
public class SlackIntegration extends BaseOAuthService {

    @Value("${slack.oauth.client-id}")
    private String clientId;

    @Value("${slack.oauth.client-secret}")
    private String clientSecret;

    @Value("${slack.oauth.redirect-uri}")
    private String redirectUri;

    public SlackIntegration(WebClient.Builder webClientBuilder,
                           ServiceConnectionRepository repository,
                           ObjectMapper objectMapper) {
        super(webClientBuilder, repository, objectMapper);
    }

    @Override
    public ServiceConnection.ServiceType getType() {
        return ServiceConnection.ServiceType.SLACK;
    }

    @Override
    public String getName() {
        return "Slack";
    }

    @Override
    public String getDescription() {
        return "Team communication and collaboration platform";
    }

    @Override
    public OAuthConfig getOAuthConfig() {
        return new OAuthConfig(
            clientId,
            clientSecret,
            "https://slack.com/oauth/v2/authorize",
            "https://slack.com/api/oauth.v2.access",
            redirectUri,
            List.of("chat:write", "channels:read")
        );
    }

    @Override
    public List<ActionDefinition> getActions() {
        return List.of(
            new ActionDefinition(
                "slack.new_message",
                "New Message in Channel",
                "Triggered when a new message is posted to a monitored channel",
                List.of(
                    new FieldDefinition("channelId", "Channel ID", "string", true,
                        "The Slack channel ID to monitor")
                )
            )
        );
    }

    @Override
    public List<ReactionDefinition> getReactions() {
        return List.of(
            new ReactionDefinition(
                "slack.send_message",
                "Send Message",
                "Post a message to a Slack channel",
                List.of(
                    new FieldDefinition("channelId", "Channel ID", "string", true,
                        "The channel to post to"),
                    new FieldDefinition("message", "Message", "text", true,
                        "The message content")
                )
            )
        );
    }

    @Override
    protected Mono<ServiceConnection> enrichConnection(
            ServiceConnection connection, String accessToken) {
        // Optional: Fetch workspace info, user details, etc.
        return webClient.get()
            .uri("https://slack.com/api/auth.test")
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                // Parse response and set metadata
                connection.setMetadata(response);
                return connection;
            })
            .onErrorReturn(connection); // Graceful fallback
    }
}
```

**Step 2: Add to ServiceType Enum**

```java
// In ServiceConnection.java
public enum ServiceType {
    GMAIL,
    DISCORD,
    SLACK  // Add new type
}
```

**Step 3: Create Controller (Optional)**

```java
@RestController
@RequestMapping("/api/services/slack")
public class SlackOAuthController {

    private final SlackIntegration slackIntegration;

    @GetMapping("/auth-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAuthUrl() {
        String state = UUID.randomUUID().toString();
        String authUrl = slackIntegration.buildAuthorizationUrl(state);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "authUrl", authUrl,
            "state", state
        )));
    }

    @GetMapping("/callback")
    public Mono<ResponseEntity<String>> handleCallback(@RequestParam String code) {
        return slackIntegration.exchangeAuthorizationCode(code)
            .map(connection -> ResponseEntity.ok(
                generateSuccessHtml("Slack connected successfully!")
            ))
            .onErrorResume(error -> Mono.just(
                ResponseEntity.badRequest().body(
                    generateErrorHtml(error.getMessage())
                )
            ));
    }
}
```

**That's it!** BaseOAuthService handles all the OAuth complexity.

### 4. Creating Service Actions/Reactions

**Define Available Actions:**
```java
@Override
public List<ActionDefinition> getActions() {
    return List.of(
        new ActionDefinition(
            "service.action_id",      // Unique ID
            "User-Friendly Name",      // Display name
            "Description of what triggers this",
            List.of(                   // Configuration fields
                new FieldDefinition(
                    "fieldName",       // Internal name
                    "Field Label",     // UI label
                    "string",          // Type: string, number, boolean, text, url
                    true,              // Required?
                    "Help text for users"
                )
            )
        )
    );
}
```

**Define Available Reactions:**
```java
@Override
public List<ReactionDefinition> getReactions() {
    return List.of(
        new ReactionDefinition(
            "service.reaction_id",
            "Send Notification",
            "Sends a notification to the service",
            List.of(
                new FieldDefinition("message", "Message", "text", true,
                    "The message to send"),
                new FieldDefinition("priority", "Priority", "string", false,
                    "Message priority: low, normal, high")
            )
        )
    );
}
```

### 5. Service Method Best Practices

**Service Layer:**
```java
@Service
public class AreaService {

    // Good: Throw specific exceptions, let controller handle
    public Area findById(Long id) {
        return areaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Area", id));
    }

    // Good: Validate and throw exceptions
    public Area createArea(CreateAreaRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Area name is required");
        }

        // Business logic here
        Area area = new Area();
        // ...
        return areaRepository.save(area);
    }

    // Good: Use reactive types for external calls
    public Mono<ValidationResult> validateConnection(Long connectionId) {
        ServiceConnection connection = findConnection(connectionId);

        return webClient.get()
            .uri("/api/validate")
            .headers(h -> h.setBearerAuth(connection.getAccessToken()))
            .retrieve()
            .bodyToMono(ValidationResult.class)
            .onErrorMap(error -> new ServiceIntegrationException(
                connection.getType().name(),
                "Validation failed: " + error.getMessage(),
                error
            ));
    }
}
```

### 6. Controller Best Practices

**Do:**
```java
@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;

    // ✓ Use ApiResponse with generic type
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AreaResponse>> getArea(@PathVariable Long id) {
        Area area = areaService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(area)));
    }

    // ✓ Use @Valid for automatic validation
    @PostMapping
    public ResponseEntity<ApiResponse<AreaResponse>> createArea(
            @Valid @RequestBody CreateAreaRequest request) {
        Area area = areaService.createArea(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Area created", toResponse(area)));
    }

    // ✓ Let exceptions bubble up to GlobalExceptionHandler
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArea(@PathVariable Long id) {
        areaService.deleteArea(id);  // Throws ResourceNotFoundException if not found
        return ResponseEntity.ok(ApiResponse.success("Area deleted"));
    }
}
```

**Don't:**
```java
// ✗ Don't use try-catch for business logic
@GetMapping("/{id}")
public ResponseEntity<?> getArea(@PathVariable Long id) {
    try {
        Area area = areaService.findById(id);
        return ResponseEntity.ok(area);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

// ✗ Don't return raw Map
@GetMapping
public ResponseEntity<Map<String, Object>> listAreas() {
    return ResponseEntity.ok(Map.of("areas", areaService.findAll()));
}

// ✗ Don't put business logic in controllers
@PostMapping
public ResponseEntity<?> createArea(@RequestBody CreateAreaRequest request) {
    // ✗ Validation should be in service or use @Valid
    if (request.getName() == null) {
        return ResponseEntity.badRequest().body("Name required");
    }

    // ✗ Database operations should be in service
    Area area = areaRepository.save(new Area());
    return ResponseEntity.ok(area);
}
```

### 7. Testing the New Pattern

**Controller Tests:**
```java
@WebMvcTest(AreaController.class)
class AreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AreaService areaService;

    @Test
    void getArea_WhenFound_ReturnsApiResponse() throws Exception {
        Area area = new Area();
        area.setId(1L);

        when(areaService.findById(1L)).thenReturn(area);

        mockMvc.perform(get("/api/areas/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getArea_WhenNotFound_Returns404() throws Exception {
        when(areaService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Area", 999L));

        mockMvc.perform(get("/api/areas/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
```

**Service Tests:**
```java
@SpringBootTest
class AreaServiceTest {

    @Autowired
    private AreaService areaService;

    @Test
    void findById_WhenNotFound_ThrowsException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            areaService.findById(999L);
        });
    }

    @Test
    void createArea_WithInvalidData_ThrowsException() {
        CreateAreaRequest request = new CreateAreaRequest();
        // Missing required fields

        assertThrows(IllegalArgumentException.class, () -> {
            areaService.createArea(request);
        });
    }
}
```

### 8. API Response Examples

**Success Response:**
```json
{
  "success": true,
  "message": "Area created successfully",
  "data": {
    "id": 1,
    "name": "Gmail to Discord",
    "active": true
  }
}
```

**Error Response (Validation):**
```json
{
  "success": false,
  "error": "Validation Error",
  "message": "Area name is required"
}
```

**Error Response (Not Found):**
```json
{
  "success": false,
  "error": "Not Found",
  "message": "Area not found with id: 999"
}
```

**Error Response (Service Integration):**
```json
{
  "success": false,
  "error": "Service Integration Error",
  "message": "Gmail integration error: Token expired"
}
```

---

## Cheat Sheet

### Import Statements You'll Need

```java
// ApiResponse
import com.area.server.dto.response.ApiResponse;

// Exceptions
import com.area.server.exception.ResourceNotFoundException;
import com.area.server.exception.ServiceIntegrationException;

// Service Integration Framework
import com.area.server.service.integration.ServiceIntegration;
import com.area.server.service.integration.ActionDefinition;
import com.area.server.service.integration.ReactionDefinition;
import com.area.server.service.integration.FieldDefinition;
import com.area.server.service.integration.oauth.OAuthServiceIntegration;
import com.area.server.service.integration.oauth.BaseOAuthService;
import com.area.server.service.integration.oauth.OAuthConfig;
```

### Common Patterns

**Return success with data:**
```java
return ResponseEntity.ok(ApiResponse.success(data));
```

**Return success with message:**
```java
return ResponseEntity.ok(ApiResponse.success("Operation completed"));
```

**Return success with message and data:**
```java
return ResponseEntity.ok(ApiResponse.success("Area created", areaData));
```

**Return created:**
```java
return ResponseEntity.status(HttpStatus.CREATED)
    .body(ApiResponse.success("Created", data));
```

**Throw not found:**
```java
throw new ResourceNotFoundException("Area", id);
```

**Throw validation error:**
```java
throw new IllegalArgumentException("Invalid input: " + reason);
```

**Throw service error:**
```java
throw new ServiceIntegrationException("Gmail", "Token refresh failed");
```

---

## Migration Checklist

When refactoring an existing controller:

- [ ] Add `ApiResponse` import
- [ ] Add exception imports
- [ ] Change return type from `Map<String, Object>` to `ApiResponse<YourType>`
- [ ] Remove try-catch blocks
- [ ] Replace `ResponseEntity.ok(Map.of(...))` with `ApiResponse.success(...)`
- [ ] Let exceptions bubble up (don't catch them)
- [ ] Update method signatures with proper generic types
- [ ] Test error cases to ensure GlobalExceptionHandler works

---

## Questions?

Check the full refactoring summary in `REFACTORING_SUMMARY.md` for:
- Complete list of changes
- Architecture decisions
- Remaining work
- Code review findings

For OAuth-related questions, see `BaseOAuthService.java` documentation.
