# Architecture Decisions

## Overview

This document explains the key architectural decisions made during the AREA backend refactoring, the problems they solve, and the trade-offs involved.

## 1. Service Integration Framework

### Decision
Implemented a plugin-based service integration framework with `ServiceIntegration`, `ActionExecutor`, and `ReactionExecutor` interfaces.

### Problem Solved
- **Hardcoded Service Coupling**: Original code had `GmailDiscordController` that tightly coupled Gmail actions to Discord reactions
- **Scalability**: Adding new services required modifying core application code
- **Maintainability**: Service-specific logic was scattered across controllers and schedulers

### Benefits
- **Separation of Concerns**: Each service is self-contained
- **Dynamic Discovery**: Services are auto-discovered via Spring dependency injection
- **Extensibility**: Add new services without modifying existing code
- **Testability**: Services can be tested in isolation
- **Flexibility**: Any action can trigger any reaction dynamically

### Trade-offs
- **Initial Complexity**: More interfaces and abstractions to understand
- **Indirection**: Action/Reaction lookup through registries adds layer of indirection
- **Learning Curve**: Developers need to understand the framework before adding services

### Implementation
```
/service/integration/
├── ServiceIntegration.java          # Base interface
├── ActionDefinition.java            # Action metadata
├── ReactionDefinition.java          # Reaction metadata
├── impl/
│   ├── GmailIntegration.java       # Gmail service
│   └── DiscordIntegration.java     # Discord service
└── executor/
    ├── ActionExecutor.java          # Action execution interface
    ├── ReactionExecutor.java        # Reaction execution interface
    ├── ActionExecutorRegistry.java  # Action registry
    ├── ReactionExecutorRegistry.java # Reaction registry
    ├── GmailActionExecutor.java     # Gmail implementation
    └── DiscordReactionExecutor.java # Discord implementation
```

## 2. Centralized Exception Handling

### Decision
Implemented `GlobalExceptionHandler` with specific exception types, eliminating try-catch blocks in controllers.

### Problem Solved
- **Duplicate Error Handling**: Every controller method had similar try-catch blocks
- **Inconsistent Responses**: Error responses varied across endpoints
- **Verbose Code**: Try-catch blocks obscured business logic
- **Maintainability**: Changes to error handling required updating every controller

### Benefits
- **DRY Principle**: Error handling logic defined once
- **Consistent API Responses**: All errors use `ApiResponse` format
- **Cleaner Controllers**: Business logic is immediately visible
- **Centralized Logging**: All errors logged in one place
- **Proper HTTP Status Codes**: Exceptions map to correct HTTP status

### Trade-offs
- **Less Control**: Controllers can't handle errors differently
- **Debugging**: Stack traces may be less obvious
- **Custom Handling**: Special cases require custom exceptions

### Exception Hierarchy
```
RuntimeException
├── ResourceNotFoundException       → 404 Not Found
├── ServiceConnectionNotFoundException → 404 Not Found
├── ValidationException              → 400 Bad Request
├── OAuthException                   → 401 Unauthorized
├── ServiceIntegrationException      → 502 Bad Gateway
└── IllegalStateException            → 409 Conflict
```

## 3. Standardized API Responses

### Decision
Created `ApiResponse<T>` wrapper for all API responses.

### Problem Solved
- **Inconsistent Response Format**: Some endpoints returned `Map<String, Object>`, others returned entities directly
- **Client Integration**: Frontend had to handle multiple response formats
- **Error Responses**: Success and error responses looked different

### Benefits
- **Predictable Structure**: Clients always expect same response shape
- **Type Safety**: Generic type `T` provides compile-time type checking
- **Consistent Metadata**: Success/error flags, messages, and data always in same place
- **API Evolution**: Easy to add new fields (e.g., pagination, timestamps) without breaking clients

### Trade-offs
- **Extra Wrapping**: Additional object layer in responses
- **Verbosity**: More fields in JSON responses
- **Migration**: Requires updating existing frontend code

### Response Format
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": 123,
    "name": "Example"
  }
}
```

## 4. OAuth Abstraction with BaseOAuthService

### Decision
Created abstract `BaseOAuthService` class for OAuth 2.0 flows.

### Problem Solved
- **Duplicate OAuth Logic**: Gmail, Slack, GitHub all need similar OAuth flows
- **Token Management**: Access token refresh logic repeated for each service
- **Configuration**: OAuth endpoints and scopes scattered across code

### Benefits
- **Code Reuse**: OAuth flow implemented once, reused for all services
- **Consistency**: All OAuth services behave identically
- **Maintainability**: Bug fixes apply to all OAuth services
- **Security**: Token refresh logic centralized and auditable

### Trade-offs
- **OAuth 2.0 Only**: Doesn't support OAuth 1.0a
- **Limited Flexibility**: Custom OAuth flows may not fit pattern
- **Inheritance**: Subclasses inherit all OAuth methods even if not needed

## 5. Executor Pattern for Actions/Reactions

### Decision
Used Strategy pattern with `ActionExecutor` and `ReactionExecutor` interfaces.

### Problem Solved
- **Hardcoded Workflows**: AreaPollingScheduler had Gmail→Discord logic hardcoded
- **Tight Coupling**: Scheduler directly called `GmailService` and `DiscordService`
- **Inflexibility**: Couldn't mix and match actions with reactions

### Benefits
- **Open/Closed Principle**: Add new executors without modifying scheduler
- **Dynamic Execution**: Runtime lookup of executors based on action/reaction type
- **Parallel Execution**: Multiple Areas can execute concurrently
- **Isolation**: Each executor is independent and testable

### Trade-offs
- **Registry Overhead**: Executor lookup adds minimal runtime cost
- **Type Safety**: String-based action/reaction types prone to typos
- **Complexity**: More classes to understand

## 6. Reactive Programming with Project Reactor

### Decision
Continued using `Mono` and `Flux` for asynchronous operations.

### Problem Solved
- **Blocking I/O**: Synchronous HTTP calls block threads
- **Resource Utilization**: One thread per request wastes resources
- **Scalability**: Limited by thread pool size

### Benefits
- **Non-Blocking**: Threads not blocked during I/O operations
- **Backpressure**: Built-in flow control for data streams
- **Composability**: Chain operations declaratively
- **Performance**: Handle more requests with fewer threads

### Trade-offs
- **Learning Curve**: Reactive programming is complex
- **Debugging**: Stack traces harder to read
- **Error Handling**: Different patterns than synchronous code

## 7. Service Layer Exception Throwing

### Decision
Services throw specific exceptions instead of returning null or Optional.

### Problem Solved
- **Null Checks**: Controllers had to check for null/empty Optional
- **Error Context**: Generic exceptions lacked context
- **Flow Control**: Difficult to distinguish between "not found" and "error"

### Benefits
- **Fail Fast**: Errors immediately propagate to exception handler
- **Rich Context**: Custom exceptions carry additional data (resource type, ID, etc.)
- **Clean Controllers**: No null checks or Optional unwrapping
- **Explicit Contracts**: Method signatures clearly indicate what can fail

### Trade-offs
- **Exception Overhead**: Creating exceptions has performance cost
- **Stack Traces**: Exceptions generate stack traces (can be expensive)

## 8. Builder Pattern for Definitions

### Decision
Used Builder pattern for `ActionDefinition`, `ReactionDefinition`, and `FieldDefinition`.

### Problem Solved
- **Immutability**: Definitions should not change after creation
- **Optional Fields**: Many fields are optional, leading to constructor overload
- **Readability**: Long constructor calls hard to read

### Benefits
- **Fluent API**: Easy to read and write
- **Immutability**: Thread-safe by default
- **Validation**: Build-time validation of required fields
- **Discoverability**: IDE autocomplete guides usage

### Trade-offs
- **Verbosity**: More code to define builders
- **Performance**: Additional object creation (negligible in practice)

## Design Principles Applied

1. **SOLID Principles**
   - Single Responsibility: Each executor handles one action/reaction
   - Open/Closed: Add services without modifying existing code
   - Liskov Substitution: All executors interchangeable
   - Interface Segregation: Separate interfaces for actions/reactions
   - Dependency Inversion: Depend on abstractions, not implementations

2. **DRY (Don't Repeat Yourself)**
   - OAuth logic in BaseOAuthService
   - Error handling in GlobalExceptionHandler
   - API response formatting in ApiResponse

3. **Separation of Concerns**
   - Controllers handle HTTP
   - Services handle business logic
   - Executors handle integration logic
   - Repositories handle data access

4. **Convention over Configuration**
   - Auto-discovery of executors
   - Standard action/reaction type naming: `service.action`
   - Consistent exception hierarchy

## Future Considerations

1. **Database-Driven Workflow Definitions**: Store action/reaction configurations in database instead of Java configs
2. **GraphQL API**: Alternative to REST for complex queries
3. **Event Sourcing**: Store workflow execution history as events
4. **CQRS**: Separate read and write models for workflows
5. **API Versioning**: Support multiple API versions concurrently
6. **Rate Limiting**: Per-service rate limiting to prevent abuse
7. **Circuit Breaker**: Automatic service degradation on failures
8. **Observability**: Distributed tracing, metrics, and structured logging

## Migration Path

For teams adopting this architecture:

1. **Phase 1**: Add new services using the framework
2. **Phase 2**: Refactor existing services incrementally
3. **Phase 3**: Migrate frontend to use new API response format
4. **Phase 4**: Remove deprecated controllers and services
5. **Phase 5**: Add advanced features (versioning, webhooks, etc.)

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Reactive Programming Guide](https://projectreactor.io/docs/core/release/reference/)
