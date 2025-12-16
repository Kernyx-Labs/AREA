# Service Integration Guide

## Overview

This guide explains how to add new services to the AREA platform using the service integration framework. The framework provides a standardized way to define actions (triggers) and reactions (outputs) that can be dynamically combined.

## Architecture

The service integration framework consists of several key components:

### 1. Service Integration Interface

Every service must implement the `ServiceIntegration` interface:

```java
public interface ServiceIntegration {
    String getServiceId();              // e.g., "gmail", "discord"
    String getServiceName();            // e.g., "Gmail", "Discord"
    String getServiceDescription();     // Brief description
    List<ActionDefinition> getActions(); // Actions this service provides
    List<ReactionDefinition> getReactions(); // Reactions this service provides
    boolean requiresOAuth();            // OAuth required?
    boolean supportsWebhooks();         // Webhook support?
}
```

### 2. Action and Reaction Definitions

Actions and reactions are defined using builder patterns:

```java
ActionDefinition.builder()
    .name("email_received")
    .displayName("New Email Received")
    .description("Triggered when an email is received")
    .fields(List.of(
        FieldDefinition.builder()
            .name("fromAddress")
            .type("string")
            .description("Filter by sender email")
            .required(false)
            .build()
    ))
    .build()
```

### 3. Executors

Executors implement the actual logic:

- **ActionExecutor**: Checks if action conditions are met
- **ReactionExecutor**: Performs the reaction operation

## Adding a New Service

### Step 1: Create Service Integration Class

Create a class implementing `ServiceIntegration`:

```java
@Service
public class SlackIntegration implements ServiceIntegration {
    
    @Override
    public String getServiceId() {
        return "slack";
    }
    
    @Override
    public String getServiceName() {
        return "Slack";
    }
    
    @Override
    public String getServiceDescription() {
        return "Slack messaging integration";
    }
    
    @Override
    public List<ActionDefinition> getActions() {
        return List.of(
            ActionDefinition.builder()
                .name("message_received")
                .displayName("New Message")
                .description("Triggered when a message is received")
                .fields(List.of(
                    FieldDefinition.builder()
                        .name("channel")
                        .type("string")
                        .description("Channel ID to monitor")
                        .required(true)
                        .build()
                ))
                .build()
        );
    }
    
    @Override
    public List<ReactionDefinition> getReactions() {
        return List.of(
            ReactionDefinition.builder()
                .name("send_message")
                .displayName("Send Message")
                .description("Send a message to a Slack channel")
                .fields(List.of(
                    FieldDefinition.builder()
                        .name("channel")
                        .type("string")
                        .description("Channel ID")
                        .required(true)
                        .build(),
                    FieldDefinition.builder()
                        .name("message")
                        .type("text")
                        .description("Message content")
                        .required(true)
                        .build()
                ))
                .build()
        );
    }
    
    @Override
    public boolean requiresOAuth() {
        return true;
    }
}
```

### Step 2: Create Action Executor (if providing actions)

```java
@Component
public class SlackActionExecutor implements ActionExecutor {
    
    private final SlackService slackService;
    
    @Override
    public String getActionType() {
        return "slack.message_received";  // service.action
    }
    
    @Override
    public Mono<Boolean> isTriggered(Area area) {
        return getTriggerContext(area)
            .map(context -> context.getInteger("messageCount") > 0);
    }
    
    @Override
    public Mono<TriggerContext> getTriggerContext(Area area) {
        return slackService.fetchNewMessages(area.getSlackConfig())
            .map(messages -> {
                TriggerContext context = new TriggerContext();
                context.put("messageCount", messages.size());
                context.put("messages", messages);
                return context;
            });
    }
}
```

### Step 3: Create Reaction Executor (if providing reactions)

```java
@Component
public class SlackReactionExecutor implements ReactionExecutor {
    
    private final SlackService slackService;
    
    @Override
    public String getReactionType() {
        return "slack.send_message";  // service.reaction
    }
    
    @Override
    public Mono<Void> execute(Area area, TriggerContext context) {
        String message = formatMessage(area, context);
        return slackService.sendMessage(area.getSlackConfig(), message);
    }
    
    private String formatMessage(Area area, TriggerContext context) {
        String template = area.getSlackConfig().getMessageTemplate();
        // Replace {{placeholders}} with context data
        // ... template logic
        return template;
    }
}
```

### Step 4: OAuth Integration (if required)

For OAuth services, extend `BaseOAuthService`:

```java
@Service
public class SlackIntegration extends BaseOAuthService {
    
    @Value("${slack.oauth.client-id}")
    private String clientId;
    
    @Value("${slack.oauth.client-secret}")
    private String clientSecret;
    
    @Value("${slack.oauth.redirect-uri}")
    private String redirectUri;
    
    @Override
    public OAuthConfig getOAuthConfig() {
        return OAuthConfig.builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .redirectUri(redirectUri)
            .authorizationUrl("https://slack.com/oauth/v2/authorize")
            .tokenUrl("https://slack.com/api/oauth.v2.access")
            .scopes(List.of("chat:write", "channels:read"))
            .build();
    }
    
    @Override
    public ServiceConnection.ServiceType getType() {
        return ServiceConnection.ServiceType.SLACK;  // Add to enum
    }
}
```

## Field Types

Supported field types:
- `string` - Short text input
- `text` - Long text/multiline input
- `number` - Numeric input
- `boolean` - True/false checkbox
- `select` - Dropdown selection
- `url` - URL input with validation

## Trigger Context

The `TriggerContext` object passes data from actions to reactions:

```java
TriggerContext context = new TriggerContext();
context.put("subject", email.getSubject());
context.put("from", email.getFrom());
context.put("messageCount", 5);

// In reaction:
String subject = context.getString("subject");
Integer count = context.getInteger("messageCount");
```

## Best Practices

1. **Error Handling**: Throw specific exceptions (`ServiceIntegrationException`, `OAuthException`, etc.)
2. **Logging**: Use SLF4J logger for debugging
3. **Validation**: Validate all required fields before execution
4. **Retry Logic**: Use reactive retry mechanisms for transient failures
5. **Rate Limiting**: Respect external API rate limits
6. **Security**: Never log sensitive tokens or credentials
7. **Testing**: Create unit and integration tests for executors

## Registry Auto-Discovery

Executors are automatically registered via Spring dependency injection:

```java
@Component
public class ActionExecutorRegistry {
    public ActionExecutorRegistry(List<ActionExecutor> actionExecutors) {
        // Auto-discovers all @Component ActionExecutor beans
        for (ActionExecutor executor : actionExecutors) {
            register(executor);
        }
    }
}
```

## Examples

See existing integrations:
- `/server/src/main/java/com/area/server/service/integration/impl/GmailIntegration.java`
- `/server/src/main/java/com/area/server/service/integration/impl/DiscordIntegration.java`
- `/server/src/main/java/com/area/server/service/integration/executor/GmailActionExecutor.java`
- `/server/src/main/java/com/area/server/service/integration/executor/DiscordReactionExecutor.java`

## Troubleshooting

**Q: My executor isn't being discovered**  
A: Ensure the class is annotated with `@Component` and in a package scanned by Spring

**Q: OAuth flow fails**  
A: Check that `OAuthConfig` is properly configured with valid credentials

**Q: Action never triggers**  
A: Verify `getTriggerContext()` returns proper data and `isTriggered()` logic is correct

**Q: Reaction doesn't execute**  
A: Check logs for exceptions and verify the service connection has valid credentials
