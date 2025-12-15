# Exception Handling Strategy

## Overview

The AREA backend uses a centralized exception handling strategy with `@RestControllerAdvice` to eliminate try-catch blocks in controllers and ensure consistent error responses.

## Architecture

```
Controller Method
    ↓
throws Exception
    ↓
GlobalExceptionHandler
    ↓
@ExceptionHandler Method
    ↓
ApiResponse<?>
    ↓
HTTP Response (4xx/5xx)
```

## Exception Hierarchy

```
RuntimeException
├── ResourceNotFoundException       (404)
├── ServiceConnectionNotFoundException (404)
├── ValidationException             (400)
├── OAuthException                  (401)
├── ServiceIntegrationException     (502)
└── IllegalStateException           (409)
```

## Custom Exceptions

### 1. ResourceNotFoundException

**When to use:** Resource not found by ID

**HTTP Status:** 404 Not Found

**Example:**
```java
public Area findById(Long id) {
    return areaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Area", id));
}
```

**Response:**
```json
{
  "success": false,
  "error": "Not Found",
  "message": "Area not found with id: 123"
}
```

### 2. ServiceConnectionNotFoundException

**When to use:** Service connection not found

**HTTP Status:** 404 Not Found

**Example:**
```java
public ServiceConnection findById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ServiceConnectionNotFoundException(id));
}
```

**Response:**
```json
{
  "success": false,
  "error": "Service Connection Not Found",
  "message": "Service connection not found with id: 456"
}
```

### 3. ValidationException

**When to use:** Business logic validation failure

**HTTP Status:** 400 Bad Request

**Example:**
```java
if (botToken == null || botToken.isBlank()) {
    throw new ValidationException("botToken", "Bot token is required");
}

// Or for multiple fields:
Map<String, String> errors = new HashMap<>();
errors.put("email", "Invalid email format");
errors.put("password", "Password too short");
throw new ValidationException(errors);
```

**Response:**
```json
{
  "success": false,
  "error": "Validation Error",
  "message": "Validation failed for field 'botToken': Bot token is required"
}
```

### 4. OAuthException

**When to use:** OAuth authentication or token refresh failure

**HTTP Status:** 401 Unauthorized

**Example:**
```java
if (refreshToken == null) {
    throw new OAuthException("Gmail", "token_refresh", 
        "No refresh token available");
}
```

**Response:**
```json
{
  "success": false,
  "error": "OAuth Error",
  "message": "OAuth error for Gmail during token_refresh: No refresh token available"
}
```

### 5. ServiceIntegrationException

**When to use:** External service API call failure

**HTTP Status:** 502 Bad Gateway

**Example:**
```java
try {
    discordClient.post().uri(webhookUrl).retrieve().block();
} catch (WebClientResponseException e) {
    throw new ServiceIntegrationException("Discord", 
        "Failed to send webhook: " + e.getMessage(), e);
}
```

**Response:**
```json
{
  "success": false,
  "error": "Service Integration Error",
  "message": "Failed to send Discord webhook: 400 Bad Request"
}
```

## GlobalExceptionHandler

**Location:** `/server/src/main/java/com/area/server/exception/GlobalExceptionHandler.java`

**Handlers:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Not Found", e.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation Error", e.getMessage()));
    }

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ApiResponse<?>> handleOAuthError(OAuthException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("OAuth Error", e.getMessage()));
    }

    @ExceptionHandler(ServiceIntegrationException.class)
    public ResponseEntity<ApiResponse<?>> handleServiceIntegration(ServiceIntegrationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiResponse.error("Service Integration Error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("Conflict", e.getMessage()));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiResponse<?>> handleWebClientError(WebClientResponseException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiResponse.error("External Service Error", 
                "External service returned error: " + e.getStatusCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Validation Error", 
                "Invalid request parameters: " + errors.toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericError(Exception e) {
        logger.error("Unexpected error", e);
        String message = logger.isDebugEnabled() 
            ? e.getMessage() 
            : "An unexpected error occurred. Please try again later.";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal Server Error", message));
    }
}
```

## Controller Pattern (Before/After)

### Before Refactoring

```java
@PostMapping
public ResponseEntity<Map<String, Object>> createWorkflow(@RequestBody CreateWorkflowRequest request) {
    try {
        Workflow workflow = workflowService.create(request);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "workflow", workflow
        ));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    } catch (Exception e) {
        logger.error("Error creating workflow", e);
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "error", "Internal error"
        ));
    }
}
```

### After Refactoring

```java
@PostMapping
public ResponseEntity<ApiResponse<Workflow>> createWorkflow(@RequestBody CreateWorkflowRequest request) {
    Workflow workflow = workflowService.create(request);
    return ResponseEntity.ok(ApiResponse.success("Workflow created", workflow));
}
// GlobalExceptionHandler handles all exceptions automatically
```

## Service Layer Pattern

### Throw Specific Exceptions

```java
// GOOD ✅
public Area findById(Long id) {
    return areaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Area", id));
}

// BAD ❌
public Area findById(Long id) {
    return areaRepository.findById(id).orElse(null);  // Don't return null
}

// BAD ❌
public Area findById(Long id) {
    try {
        return areaRepository.findById(id).get();
    } catch (NoSuchElementException e) {
        return null;  // Don't catch and return null
    }
}
```

### Chain Reactive Error Handling

```java
return gmailClient.get()
    .uri("/messages")
    .retrieve()
    .bodyToMono(Response.class)
    .map(response -> processResponse(response))
    .onErrorResume(WebClientResponseException.class, e -> {
        throw new ServiceIntegrationException("Gmail", 
            "Failed to fetch messages", e);
    });
```

## Validation Strategy

### 1. Framework Validation (@Valid)

```java
public class CreateWorkflowRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotNull(message = "Trigger is required")
    private TriggerConfig trigger;
}

@PostMapping
public ResponseEntity<ApiResponse<Workflow>> createWorkflow(
        @Valid @RequestBody CreateWorkflowRequest request) {
    // Validation happens automatically
    // MethodArgumentNotValidException thrown on failure
}
```

### 2. Business Logic Validation

```java
if (actionConnection.getType() != ServiceConnection.ServiceType.GMAIL) {
    throw new ValidationException("actionConnection", 
        "Action connection must be of type GMAIL");
}
```

### 3. External Service Validation

```java
try {
    Map<String, Object> botUser = discordClient.get()
        .uri("/users/@me")
        .retrieve()
        .bodyToMono(Map.class)
        .block();
        
    if (botUser == null) {
        throw new ValidationException("botToken", "Invalid bot token");
    }
} catch (WebClientResponseException e) {
    if (e.getStatusCode().value() == 401) {
        throw new ValidationException("botToken", "Invalid or expired bot token");
    }
    throw new ServiceIntegrationException("Discord", "API error", e);
}
```

## Error Logging

### Log Levels

- **ERROR**: For exceptions that require investigation
- **WARN**: For expected errors (validation, not found)
- **DEBUG**: For detailed troubleshooting

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException e) {
    logger.warn("Resource not found: {}", e.getMessage());  // WARN, not ERROR
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error("Not Found", e.getMessage()));
}

@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<?>> handleGenericError(Exception e) {
    logger.error("Unexpected error", e);  // ERROR with stack trace
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("Internal Server Error", 
            "An unexpected error occurred"));
}
```

## Security Considerations

1. **Never expose sensitive data in error messages**
   ```java
   // BAD ❌
   throw new OAuthException("Token refresh failed: " + accessToken);
   
   // GOOD ✅
   throw new OAuthException("Gmail", "token_refresh", "Token refresh failed");
   ```

2. **Don't expose internal implementation details**
   ```java
   // BAD ❌
   throw new ServiceIntegrationException("SQL error: " + sqlException.getMessage());
   
   // GOOD ✅
   throw new ServiceIntegrationException("Database", "Failed to save record");
   ```

3. **Sanitize user input in error messages**
   ```java
   String sanitized = input.replaceAll("[^a-zA-Z0-9]", "");
   throw new ValidationException("Invalid input: " + sanitized);
   ```

## Testing Exception Handling

```java
@Test
void shouldReturn404WhenWorkflowNotFound() {
    when(workflowRepository.findById(999L)).thenReturn(Optional.empty());
    
    assertThrows(ResourceNotFoundException.class, () -> {
        workflowService.findById(999L);
    });
}

@Test
void shouldReturn400OnValidationError() {
    CreateWorkflowRequest request = new CreateWorkflowRequest();
    request.setName("");  // Invalid
    
    mockMvc.perform(post("/api/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("Validation Error"));
}
```

## Best Practices

1. **Throw specific exceptions**: Don't use generic `RuntimeException`
2. **Include context**: Pass resource type, ID, or operation name
3. **Don't swallow exceptions**: Always log or rethrow
4. **Use constructor variants**: Provide constructors with/without cause
5. **Document exceptions**: Add JavaDoc `@throws` tags
6. **Test error paths**: Write tests for exception scenarios
7. **Monitor exceptions**: Track exception rates in production
8. **Graceful degradation**: Return partial results when possible

## Common Pitfalls

### ❌ DON'T: Catch and ignore
```java
try {
    doSomething();
} catch (Exception e) {
    // Silent failure
}
```

### ✅ DO: Let it propagate
```java
doSomething();  // Let GlobalExceptionHandler handle it
```

### ❌ DON'T: Return null on error
```java
try {
    return repository.findById(id).get();
} catch (Exception e) {
    return null;
}
```

### ✅ DO: Throw exception
```java
return repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("User", id));
```

### ❌ DON'T: Mix error handling styles
```java
if (user == null) {
    return ResponseEntity.notFound().build();
}
if (user.isInvalid()) {
    throw new ValidationException("Invalid user");
}
```

### ✅ DO: Be consistent
```java
User user = userRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("User", id));
    
if (user.isInvalid()) {
    throw new ValidationException("Invalid user");
}
// Both paths throw exceptions
```

## Monitoring and Alerting

Track these metrics:
- Exception rate by type
- 4xx/5xx response rates
- Specific error messages
- Failed integration attempts
- OAuth failure rate

Set up alerts for:
- Sudden spike in exceptions
- High rate of 500 errors
- OAuth failures exceeding threshold
- External service integration failures
