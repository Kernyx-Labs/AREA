# API Response Format Documentation

## Overview

All AREA API endpoints return responses in a standardized format using the `ApiResponse<T>` wrapper. This ensures consistent client integration and predictable error handling.

## Response Structure

### Success Response

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    // Response payload
  },
  "error": null
}
```

### Error Response

```json
{
  "success": false,
  "message": "Detailed error description",
  "data": null,
  "error": "Error Type"
}
```

## Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `success` | boolean | Indicates if the operation succeeded |
| `message` | string | Human-readable message (optional) |
| `data` | T | Response payload (null on error) |
| `error` | string | Error type/category (null on success) |

## Common Response Patterns

### 1. Simple Success

```json
{
  "success": true,
  "data": {
    "id": 123,
    "name": "Example Workflow",
    "active": true
  }
}
```

### 2. Success with Message

```json
{
  "success": true,
  "message": "Workflow created successfully",
  "data": {
    "id": 456
  }
}
```

### 3. Success with No Data

```json
{
  "success": true,
  "message": "Workflow deleted successfully"
}
```

### 4. List Response

```json
{
  "success": true,
  "data": {
    "workflows": [
      {"id": 1, "name": "Workflow 1"},
      {"id": 2, "name": "Workflow 2"}
    ],
    "total": 2
  }
}
```

## HTTP Status Codes

### Success Codes

| Status Code | Meaning | Usage |
|-------------|---------|-------|
| 200 OK | Success | Standard successful response |
| 201 Created | Created | Resource successfully created |
| 204 No Content | No Content | Successful deletion or update with no response body |

### Client Error Codes

| Status Code | Error Type | Description |
|-------------|------------|-------------|
| 400 Bad Request | Validation Error | Invalid request parameters |
| 401 Unauthorized | OAuth Error | Authentication required or failed |
| 404 Not Found | Not Found | Resource doesn't exist |
| 409 Conflict | Conflict | Resource state conflict |

### Server Error Codes

| Status Code | Error Type | Description |
|-------------|------------|-------------|
| 500 Internal Server Error | Internal Server Error | Unexpected server error |
| 502 Bad Gateway | Service Integration Error | External service error |

## Exception to Response Mapping

```java
@ExceptionHandler(ResourceNotFoundException.class)
→ 404 Not Found
{
  "success": false,
  "error": "Not Found",
  "message": "Workflow not found with id: 123"
}

@ExceptionHandler(ValidationException.class)
→ 400 Bad Request
{
  "success": false,
  "error": "Validation Error",
  "message": "Invalid email format"
}

@ExceptionHandler(OAuthException.class)
→ 401 Unauthorized
{
  "success": false,
  "error": "OAuth Error",
  "message": "Access token expired"
}

@ExceptionHandler(ServiceIntegrationException.class)
→ 502 Bad Gateway
{
  "success": false,
  "error": "Service Integration Error",
  "message": "Discord API returned error"
}
```

## Examples by Endpoint

### GET /api/workflows

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "workflows": [
      {
        "id": 1,
        "name": "Gmail to Discord",
        "description": "Trigger: gmail → Action: discord",
        "active": true,
        "createdAt": "2025-12-15T10:00:00Z",
        "updatedAt": "2025-12-15T10:00:00Z",
        "workflowData": {
          "trigger": {...},
          "action": {...}
        }
      }
    ],
    "total": 1
  }
}
```

### POST /api/workflows

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Workflow created successfully",
  "data": {
    "id": 123,
    "name": "New Workflow",
    "active": true
  }
}
```

**Validation Error (400 Bad Request):**
```json
{
  "success": false,
  "error": "Validation Error",
  "message": "Workflow name is required"
}
```

### GET /api/workflows/{id}

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "name": "Example Workflow",
    "description": "Trigger: gmail → Action: discord",
    "active": true
  }
}
```

**Not Found Error (404 Not Found):**
```json
{
  "success": false,
  "error": "Not Found",
  "message": "Workflow not found with id: 999"
}
```

### DELETE /api/workflows/{id}

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Workflow deleted successfully"
}
```

### POST /api/services/discord/connect

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Discord connected successfully",
  "data": {
    "connectionId": 456,
    "botUsername": "MyBot",
    "channelName": "general",
    "channelId": "123456789"
  }
}
```

**Validation Error (400 Bad Request):**
```json
{
  "success": false,
  "error": "Validation Error",
  "message": "Validation failed for field 'botToken': Bot token is required"
}
```

**Integration Error (502 Bad Gateway):**
```json
{
  "success": false,
  "error": "Service Integration Error",
  "message": "OAuth error for Discord during token exchange"
}
```

## Client Implementation

### JavaScript/TypeScript

```typescript
interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: string;
}

async function fetchWorkflows(): Promise<Workflow[]> {
  const response = await fetch('/api/workflows');
  const result: ApiResponse<{ workflows: Workflow[], total: number }> = await response.json();
  
  if (!result.success) {
    throw new Error(result.message || result.error);
  }
  
  return result.data.workflows;
}
```

### Error Handling

```typescript
try {
  const workflows = await fetchWorkflows();
  console.log('Workflows:', workflows);
} catch (error) {
  if (error.message.includes('Not Found')) {
    // Handle 404
  } else if (error.message.includes('Validation')) {
    // Handle validation error
  } else {
    // Handle generic error
  }
}
```

### React Hook

```typescript
function useWorkflows() {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    fetch('/api/workflows')
      .then(res => res.json())
      .then((result: ApiResponse<{workflows: Workflow[]}>) => {
        if (result.success) {
          setWorkflows(result.data.workflows);
        } else {
          setError(result.message || result.error);
        }
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, []);
  
  return { workflows, loading, error };
}
```

## Best Practices

1. **Always check `success` field**: Never assume a 200 status means success
2. **Handle null data**: On error, `data` will be null
3. **Display user-friendly messages**: Use `message` field for user feedback
4. **Log errors**: Use `error` field for error categorization
5. **Type safety**: Use TypeScript interfaces for response types
6. **Retry logic**: Implement retries for 502/503 errors
7. **Timeout handling**: Set reasonable request timeouts

## Backward Compatibility

**Old Format (Deprecated):**
```json
{
  "success": true,
  "workflow": {...}
}
```

**New Format:**
```json
{
  "success": true,
  "data": {
    "id": 123,
    ...
  }
}
```

Clients should migrate to the new format. The old format will be removed in a future version.

## Pagination (Future)

Future pagination will follow this format:

```json
{
  "success": true,
  "data": {
    "items": [...],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 100,
      "totalPages": 5
    }
  }
}
```

## Versioning (Future)

API versioning will be indicated in the URL:

```
/api/v1/workflows
/api/v2/workflows
```

Each version maintains the same response format but may have different data structures.
