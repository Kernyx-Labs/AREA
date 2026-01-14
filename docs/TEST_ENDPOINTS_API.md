# AREA Server - Test Endpoints API Documentation

## Overview

The test endpoints allow you to individually test triggers (actions) and reactions without creating full workflows. This is useful for:

- **Development**: Testing integrations during development
- **Debugging**: Verifying trigger conditions and reaction configurations
- **Validation**: Ensuring service connections are working correctly

## Authentication

All test endpoints require authentication via JWT token in the `Authorization` header:

```
Authorization: Bearer <your-jwt-token>
```

The service connection used in tests must belong to the authenticated user.

## Base URL

```
/api/test
```

---

## Trigger Test Endpoints

Trigger endpoints check if a trigger condition would fire and return what context data would be provided to reactions. **These endpoints DO NOT execute reactions** - they only simulate the trigger check.

### 1. Test Gmail Email Received Trigger

Check for new emails matching specified filters.

**Endpoint**: `POST /api/test/trigger/gmail/email_received`

**Request Body**:
```json
{
  "serviceConnectionId": 1,
  "gmailLabel": "INBOX",
  "gmailSubjectContains": "invoice",
  "gmailFromAddress": "billing@example.com"
}
```

**Request Fields**:
- `serviceConnectionId` (required): ID of the Gmail service connection
- `gmailLabel` (optional): Gmail label to filter by (e.g., "INBOX", "IMPORTANT")
- `gmailSubjectContains` (optional): Filter emails by subject content
- `gmailFromAddress` (optional): Filter emails by sender address

**Response**:
```json
{
  "success": true,
  "message": "Gmail trigger tested successfully",
  "data": {
    "triggered": true,
    "message": "Trigger would fire! Found 3 new email(s) matching your filters.",
    "itemCount": 3,
    "contextData": {
      "messageCount": 3,
      "subject": "Invoice #12345",
      "from": "billing@example.com",
      "snippet": "Your invoice for January is ready...",
      "messageId": "18d5c6e7f8a9b0c1"
    }
  }
}
```

---

### 2. Test GitHub Issue Created Trigger

Check for new issues in a repository.

**Endpoint**: `POST /api/test/trigger/github/issue_created`

**Request Body**:
```json
{
  "serviceConnectionId": 2,
  "githubRepository": "owner/repo-name"
}
```

**Request Fields**:
- `serviceConnectionId` (required): ID of the GitHub service connection
- `githubRepository` (required): Repository in "owner/repo" format

**Response**:
```json
{
  "success": true,
  "message": "GitHub issue trigger tested successfully",
  "data": {
    "triggered": true,
    "message": "Trigger would fire! Found 2 new issue(s) in repository owner/repo-name.",
    "itemCount": 2,
    "contextData": {
      "issueCount": 2,
      "issueNumber": 42,
      "issueTitle": "Bug: Application crashes on startup",
      "issueBody": "When I start the application...",
      "issueUrl": "https://github.com/owner/repo-name/issues/42",
      "issueAuthor": "username"
    }
  }
}
```

---

### 3. Test GitHub Pull Request Created Trigger

Check for new pull requests in a repository.

**Endpoint**: `POST /api/test/trigger/github/pr_created`

**Request Body**:
```json
{
  "serviceConnectionId": 2,
  "githubRepository": "owner/repo-name"
}
```

**Request Fields**:
- `serviceConnectionId` (required): ID of the GitHub service connection
- `githubRepository` (required): Repository in "owner/repo" format

**Response**:
```json
{
  "success": true,
  "message": "GitHub PR trigger tested successfully",
  "data": {
    "triggered": true,
    "message": "Trigger would fire! Found 1 new pull request(s) in repository owner/repo-name.",
    "itemCount": 1,
    "contextData": {
      "prCount": 1,
      "prNumber": 15,
      "prTitle": "Add new feature X",
      "prBody": "This PR adds feature X...",
      "prUrl": "https://github.com/owner/repo-name/pull/15",
      "prAuthor": "contributor",
      "prSourceBranch": "feature/new-feature",
      "prTargetBranch": "main"
    }
  }
}
```

---

## Reaction Test Endpoints

Reaction endpoints **actually execute** the reaction with provided configuration and mock context data. These will send real Discord messages, create real GitHub issues/PRs, etc.

### 1. Test Discord Send Webhook Reaction

Send a message to a Discord channel.

**Endpoint**: `POST /api/test/reaction/discord/send_webhook`

**Request Body**:
```json
{
  "serviceConnectionId": 3,
  "discordChannelId": "1234567890123456789",
  "discordMessageTemplate": "New email from {from}: {subject}",
  "mockContextData": {
    "from": "test@example.com",
    "subject": "Test Subject"
  }
}
```

**Request Fields**:
- `serviceConnectionId` (required): ID of the Discord service connection
- `discordChannelId` (required): Discord channel ID where message will be sent
- `discordMessageTemplate` (optional): Message template with placeholders (e.g., `{from}`, `{subject}`)
- `mockContextData` (optional): Mock context data to test template rendering

**Response**:
```json
{
  "success": true,
  "message": "Discord reaction tested successfully",
  "data": {
    "success": true,
    "message": "Discord message sent successfully!",
    "resultData": {
      "channelId": "1234567890123456789"
    }
  }
}
```

---

### 2. Test GitHub Create Issue Reaction

Create an issue in a GitHub repository.

**Endpoint**: `POST /api/test/reaction/github/create_issue`

**Request Body**:
```json
{
  "serviceConnectionId": 2,
  "githubRepository": "owner/repo-name",
  "githubIssueTitle": "Test Issue from AREA",
  "githubIssueBody": "This is a test issue created via AREA test endpoint.",
  "githubIssueLabels": "bug,test",
  "mockContextData": {
    "subject": "Test context"
  }
}
```

**Request Fields**:
- `serviceConnectionId` (required): ID of the GitHub service connection
- `githubRepository` (required): Repository in "owner/repo" format
- `githubIssueTitle` (required): Issue title
- `githubIssueBody` (optional): Issue body/description
- `githubIssueLabels` (optional): Comma-separated list of labels
- `mockContextData` (optional): Mock context data for template rendering

**Response**:
```json
{
  "success": true,
  "message": "GitHub create_issue reaction tested successfully",
  "data": {
    "success": true,
    "message": "GitHub issue created successfully in repository owner/repo-name!",
    "resultData": {
      "repository": "owner/repo-name",
      "title": "Test Issue from AREA"
    }
  }
}
```

---

### 3. Test GitHub Create Pull Request Reaction

Create a pull request in a GitHub repository.

**Endpoint**: `POST /api/test/reaction/github/create_pr`

**Request Body**:
```json
{
  "serviceConnectionId": 2,
  "githubRepository": "owner/repo-name",
  "githubPrTitle": "Test PR from AREA",
  "githubPrBody": "This is a test pull request created via AREA test endpoint.",
  "githubSourceBranch": "test-branch",
  "githubTargetBranch": "main",
  "githubCommitMessage": "Add test file",
  "githubFilePath": "test-file.txt",
  "githubFileContent": "This is test content",
  "mockContextData": {
    "subject": "Test context"
  }
}
```

**Request Fields**:
- `serviceConnectionId` (required): ID of the GitHub service connection
- `githubRepository` (required): Repository in "owner/repo" format
- `githubPrTitle` (required): Pull request title
- `githubPrBody` (optional): Pull request body/description
- `githubSourceBranch` (required): Source branch for the PR
- `githubTargetBranch` (optional): Target branch (defaults to "main")
- `githubCommitMessage` (optional): Commit message for the changes
- `githubFilePath` (required): Path of the file to create/modify
- `githubFileContent` (optional): Content of the file
- `mockContextData` (optional): Mock context data for template rendering

**Response**:
```json
{
  "success": true,
  "message": "GitHub create_pr reaction tested successfully",
  "data": {
    "success": true,
    "message": "GitHub pull request created successfully in repository owner/repo-name!",
    "resultData": {
      "repository": "owner/repo-name",
      "title": "Test PR from AREA",
      "sourceBranch": "test-branch",
      "targetBranch": "main"
    }
  }
}
```

---

## Error Responses

All endpoints follow a consistent error response format:

```json
{
  "success": false,
  "error": "Error",
  "message": "Detailed error message"
}
```

### Common Error Scenarios

**401 Unauthorized**:
```json
{
  "success": false,
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

**400 Bad Request** - Invalid service connection:
```json
{
  "success": false,
  "error": "Error",
  "message": "Service connection must be of type GMAIL"
}
```

**404 Not Found** - Service connection not found:
```json
{
  "success": false,
  "error": "Error",
  "message": "Service connection not found"
}
```

**403 Forbidden** - Connection doesn't belong to user:
```json
{
  "success": false,
  "error": "Error",
  "message": "Service connection does not belong to the current user"
}
```

---

## Usage Examples with cURL

### Test Gmail Trigger

```bash
curl -X POST http://localhost:8080/api/test/trigger/gmail/email_received \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceConnectionId": 1,
    "gmailLabel": "INBOX",
    "gmailSubjectContains": "invoice"
  }'
```

### Test Discord Reaction

```bash
curl -X POST http://localhost:8080/api/test/reaction/discord/send_webhook \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceConnectionId": 3,
    "discordChannelId": "1234567890123456789",
    "discordMessageTemplate": "Test message from AREA!",
    "mockContextData": {}
  }'
```

### Test GitHub Issue Creation

```bash
curl -X POST http://localhost:8080/api/test/reaction/github/create_issue \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceConnectionId": 2,
    "githubRepository": "username/repo",
    "githubIssueTitle": "Test Issue",
    "githubIssueBody": "This is a test issue",
    "githubIssueLabels": "test"
  }'
```

---

## Postman Collection

### Setting Up Postman

1. **Import Environment**:
   - Create a new environment
   - Add variable `jwt_token` with your authentication token
   - Add variable `base_url` = `http://localhost:8080`

2. **Create Collection**:
   - Name: "AREA Test Endpoints"
   - Authorization: Bearer Token = `{{jwt_token}}`

3. **Add Requests**: Create requests for each endpoint documented above

### Example Postman Request

**Request Name**: Test Gmail Trigger

- **Method**: POST
- **URL**: `{{base_url}}/api/test/trigger/gmail/email_received`
- **Headers**:
  - `Content-Type`: `application/json`
- **Body** (raw JSON):
  ```json
  {
    "serviceConnectionId": 1,
    "gmailLabel": "INBOX"
  }
  ```

---

## Security Considerations

1. **Authentication Required**: All endpoints require valid JWT authentication
2. **User Isolation**: Users can only test their own service connections
3. **Rate Limiting**: Consider implementing rate limits to prevent abuse
4. **Production Warning**: Be careful when testing reactions in production - they perform real actions!

---

## Development Tips

### Testing Workflow

1. **First, test the trigger** to see if it would fire and what context data it provides
2. **Use the context data** to understand what placeholders are available for reactions
3. **Test reactions** with mock context data that matches your trigger output
4. **Verify in external services** (Discord, GitHub) that reactions executed correctly

### Debugging

- Check logs for detailed execution information
- Use `mockContextData` to test different scenarios
- Test triggers before setting up full workflows
- Validate service connection tokens are not expired

### Template Testing

For Discord messages and GitHub issues/PRs, you can use placeholders from trigger context:

**Gmail Context Placeholders**:
- `{subject}` - Email subject
- `{from}` - Sender email
- `{snippet}` - Email preview
- `{messageCount}` - Number of new emails

**GitHub Issue Context Placeholders**:
- `{issueTitle}` - Issue title
- `{issueBody}` - Issue body
- `{issueNumber}` - Issue number
- `{issueAuthor}` - Issue creator
- `{issueUrl}` - Issue URL

**GitHub PR Context Placeholders**:
- `{prTitle}` - PR title
- `{prBody}` - PR body
- `{prNumber}` - PR number
- `{prAuthor}` - PR creator
- `{prUrl}` - PR URL
- `{prSourceBranch}` - Source branch
- `{prTargetBranch}` - Target branch

---

## API Summary Table

| Endpoint | Method | Purpose | Executes Real Action? |
|----------|--------|---------|----------------------|
| `/api/test/trigger/gmail/email_received` | POST | Test Gmail email trigger | No |
| `/api/test/trigger/github/issue_created` | POST | Test GitHub issue trigger | No |
| `/api/test/trigger/github/pr_created` | POST | Test GitHub PR trigger | No |
| `/api/test/reaction/discord/send_webhook` | POST | Test Discord message | **Yes** |
| `/api/test/reaction/github/create_issue` | POST | Test GitHub issue creation | **Yes** |
| `/api/test/reaction/github/create_pr` | POST | Test GitHub PR creation | **Yes** |

---

## Notes

- Trigger endpoints are **read-only** and safe to test repeatedly
- Reaction endpoints **perform real actions** - use with caution
- All endpoints respect service connection authentication and permissions
- Context data from triggers can be used to test reaction templates
- Test endpoints do not persist to the database (no Area records created)
