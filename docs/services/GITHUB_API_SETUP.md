# GitHub API Setup Guide

This guide explains how to set up GitHub OAuth integration for the AREA platform.

## Overview

The GitHub integration allows AREA to:

**Actions (Triggers):**
- Monitor repositories for new issues
- Monitor repositories for new pull requests

**Reactions:**
- Create issues in repositories
- Create pull requests in repositories (with file commits)

## Prerequisites

- A GitHub account
- Repository access (for monitoring or creating issues/PRs)
- Admin access to create OAuth applications

## Step 1: Create a GitHub OAuth Application

1. **Navigate to GitHub Developer Settings:**
   - Go to https://github.com/settings/developers
   - Click "OAuth Apps" in the left sidebar
   - Click "New OAuth App"

2. **Configure the OAuth Application:**
   - **Application name:** `AREA Platform` (or your preferred name)
   - **Homepage URL:** `http://localhost:8080` (for development)
   - **Application description:** Optional description
   - **Authorization callback URL:** `http://localhost:8080/api/services/github/callback`

   **Important:** The callback URL must exactly match the one configured in your backend.

3. **Register the Application:**
   - Click "Register application"
   - You'll be redirected to the application details page

4. **Generate Client Secret:**
   - On the application details page, click "Generate a new client secret"
   - **Important:** Copy the client secret immediately - you won't be able to see it again!

5. **Save Credentials:**
   - Copy the **Client ID** (visible on the page)
   - Copy the **Client Secret** (just generated)

## Step 2: Configure Backend Environment Variables

Add the following to your `/server/.env` file:

```bash
# GitHub OAuth Configuration
GITHUB_CLIENT_ID=your_github_client_id_here
GITHUB_CLIENT_SECRET=your_github_client_secret_here
GITHUB_REDIRECT_URI=http://localhost:8080/api/services/github/callback
```

**For Docker deployments**, add these to your `docker-compose.yml`:

```yaml
services:
  backend:
    environment:
      - GITHUB_CLIENT_ID=your_github_client_id_here
      - GITHUB_CLIENT_SECRET=your_github_client_secret_here
      - GITHUB_REDIRECT_URI=http://localhost:8080/api/services/github/callback
```

## Step 3: Verify Configuration

1. **Start the backend:**
   ```bash
   cd server
   mvn spring-boot:run
   ```

2. **Check configuration status:**
   ```bash
   curl http://localhost:8080/api/services/github/status
   ```

   Expected response:
   ```json
   {
     "status": "success",
     "data": {
       "configured": true,
       "clientIdPresent": true,
       "clientSecretPresent": true,
       "redirectUri": "http://localhost:8080/api/services/github/callback",
       "existingConnections": 0
     }
   }
   ```

## Step 4: Connect GitHub Account (via Frontend)

1. **Navigate to Services page:**
   - Open the web UI at `http://localhost:80`
   - Go to the "Services" section

2. **Connect GitHub:**
   - Click "Connect GitHub" button
   - You'll be redirected to GitHub's authorization page
   - Review the requested permissions:
     - **repo**: Full control of private and public repositories
   - Click "Authorize application"

3. **Authorization Complete:**
   - You'll be redirected back to the AREA platform
   - A success message confirms the connection
   - The window can be closed

## OAuth Scopes Explained

The GitHub integration requests the following scope:

| Scope | Access Level | Purpose |
|-------|--------------|---------|
| `repo` | Full repository access | Required for reading issues/PRs and creating issues/PRs. Includes access to code, commits, issues, pull requests, and webhooks. |

**Why `repo` scope?**
- GitHub API requires `repo` scope to create issues and pull requests
- This scope also allows monitoring of issues and PRs in both public and private repositories
- More granular scopes like `public_repo` only work for public repositories

**Security Note:** The access token is stored securely in the database and is only used for authorized AREA workflows.

## Step 5: Test the Integration

### Test Action (Trigger): Monitor for New Issues

1. **Create an AREA workflow:**
   - **Trigger:** GitHub - New Issue Created
   - **Repository:** Choose your repository (e.g., `myusername/myrepo`)
   - **Reaction:** Discord - Send Message (or any other reaction)

2. **Create a test issue:**
   - Go to your GitHub repository
   - Create a new issue with a test title
   - Wait up to 60 seconds (polling interval)

3. **Verify trigger:**
   - Check the AREA dashboard for execution logs
   - The reaction should have been executed

### Test Reaction: Create an Issue

1. **Create an AREA workflow:**
   - **Trigger:** Gmail - New Email Received (or any trigger)
   - **Reaction:** GitHub - Create Issue
   - **Repository:** Choose target repository
   - **Issue Title:** Use template variables like `New email: {email.subject}`
   - **Issue Body:** `From: {email.from}\n\n{email.snippet}`

2. **Trigger the workflow:**
   - Send yourself an email that matches the Gmail filter
   - Wait up to 60 seconds

3. **Verify issue creation:**
   - Check your GitHub repository's Issues tab
   - A new issue should have been created with the templated content

## Template Variables

GitHub reactions support variable substitution from trigger context:

### When triggered by Gmail:
- `{email.subject}` or `{subject}` - Email subject
- `{email.from}` or `{from}` - Sender email address
- `{email.snippet}` or `{snippet}` - Email preview snippet

### When triggered by GitHub Issues:
- `{issue.number}` - Issue number
- `{issue.title}` - Issue title
- `{issue.author}` - Issue creator's username
- `{issue.url}` - Issue URL

### When triggered by GitHub PRs:
- `{pr.number}` - Pull request number
- `{pr.title}` - Pull request title
- `{pr.author}` - PR creator's username
- `{pr.url}` - Pull request URL

### Example Templates:

**Issue Title:**
```
Alert: {email.subject}
```

**Issue Body:**
```
## Email Alert

**From:** {email.from}
**Subject:** {email.subject}

**Content:**
{email.snippet}

---
*Created automatically by AREA*
```

**PR Title:**
```
Automated update from email: {email.subject}
```

## Rate Limits

GitHub API has the following rate limits:

- **Authenticated requests:** 5,000 requests per hour
- **Per-repository:** No additional limits

**AREA Platform Considerations:**
- With 60-second polling interval: ~60 polls per hour per AREA
- You can safely monitor dozens of repositories simultaneously
- Rate limit errors are automatically retried with exponential backoff

## Troubleshooting

### Issue: "GitHub OAuth not configured" error

**Solution:** Verify that `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` are set in your `.env` file and restart the backend.

### Issue: "No refresh token received" warning

**Solution:** GitHub tokens don't expire, so this warning can be ignored. Unlike Gmail OAuth, GitHub tokens are long-lived.

### Issue: OAuth callback shows "Failed to obtain access token"

**Possible causes:**
1. Client secret is incorrect - verify in `.env`
2. Redirect URI mismatch - ensure it matches the OAuth app configuration exactly
3. Network issues - check backend logs for detailed error messages

### Issue: "Repository not found" when setting up AREA

**Possible causes:**
1. Repository name is misspelled - use exact format: `owner/repo`
2. Repository is private and the authenticated user doesn't have access
3. Repository was deleted or renamed

### Issue: No issues/PRs detected despite new items

**Debugging steps:**
1. Check AREA execution logs for errors
2. Verify the repository owner and name are correct
3. Check GitHub API rate limits: `curl -H "Authorization: token YOUR_TOKEN" https://api.github.com/rate_limit`
4. Review backend logs for API errors

### Issue: Pull request creation fails

**Common reasons:**
1. Source branch already exists - use unique branch names
2. No changes to commit - ensure file content is provided
3. Insufficient permissions - verify `repo` scope is granted
4. Target branch doesn't exist - check base branch name (defaults to `main`)

## Production Deployment

For production deployments:

1. **Update OAuth App Configuration:**
   - Go to your GitHub OAuth app settings
   - Update **Homepage URL** to your production domain: `https://your-domain.com`
   - Update **Authorization callback URL** to: `https://your-domain.com/api/services/github/callback`

2. **Update Environment Variables:**
   ```bash
   GITHUB_CLIENT_ID=your_production_client_id
   GITHUB_CLIENT_SECRET=your_production_client_secret
   GITHUB_REDIRECT_URI=https://your-domain.com/api/services/github/callback
   ```

3. **Security Recommendations:**
   - Use environment variables, never commit credentials
   - Rotate client secrets periodically
   - Monitor OAuth app access logs in GitHub settings
   - Consider using separate OAuth apps for dev/staging/production

## Advanced Configuration

### Custom Polling Interval

To change the polling frequency (default: 60 seconds):

In `application.properties`:
```properties
area.polling.interval=30000  # 30 seconds (in milliseconds)
```

**Note:** Shorter intervals mean more frequent checks but higher API usage.

### Multiple Repository Monitoring

You can create multiple AREA workflows to monitor different repositories:
- Each AREA can monitor a different repository
- Each AREA maintains its own state (last processed issue/PR number)
- All areas share the same OAuth connection

## API Reference

### Backend Endpoints

**Get Authorization URL:**
```http
GET /api/services/github/auth-url
```

Response:
```json
{
  "status": "success",
  "data": {
    "authUrl": "https://github.com/login/oauth/authorize?...",
    "instructions": "Visit this URL in your browser to authorize GitHub access",
    "redirectUri": "http://localhost:8080/api/services/github/callback"
  }
}
```

**OAuth Callback:**
```http
GET /api/services/github/callback?code=...&state=...
```

Returns HTML page with success/error message.

**Configuration Status:**
```http
GET /api/services/github/status
```

Response:
```json
{
  "status": "success",
  "data": {
    "configured": true,
    "clientIdPresent": true,
    "clientSecretPresent": true,
    "redirectUri": "http://localhost:8080/api/services/github/callback",
    "existingConnections": 1
  }
}
```

## Security Best Practices

1. **Token Storage:**
   - Access tokens are encrypted at rest in the database
   - Never log or expose tokens in error messages
   - Tokens are only transmitted over HTTPS in production

2. **Scope Minimization:**
   - Only the `repo` scope is requested
   - No access to user's personal data beyond repository access
   - Tokens can be revoked at any time by the user

3. **OAuth State Parameter:**
   - State parameter prevents CSRF attacks
   - Validated during callback processing

4. **Revoke Access:**
   Users can revoke AREA's access at any time:
   - Go to https://github.com/settings/applications
   - Find "AREA Platform" (or your app name)
   - Click "Revoke access"

## Additional Resources

- [GitHub OAuth Documentation](https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps)
- [GitHub REST API Documentation](https://docs.github.com/en/rest)
- [GitHub API Rate Limits](https://docs.github.com/en/rest/overview/resources-in-the-rest-api#rate-limiting)
- [AREA Platform Developer Guide](../DEVELOPER_GUIDE.md)
- [Service Integration Guide](../service-integration-guide.md)
