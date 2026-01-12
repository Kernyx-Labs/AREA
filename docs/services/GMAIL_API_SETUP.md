# Gmail API Setup Guide

This guide will help you set up Gmail API credentials for the AREA project.

## Prerequisites
- A Google account (create one at https://accounts.google.com/signup if needed)
- Access to Google Cloud Console

## Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click on the project dropdown at the top
3. Click "New Project"
4. Enter project details:
   - **Project name**: `AREA-Project` (or your preferred name)
   - **Organization**: Leave as default
5. Click "Create"

## Step 2: Enable Gmail API

1. In the Google Cloud Console, make sure your new project is selected
2. Go to **APIs & Services** > **Library**
3. Search for "Gmail API"
4. Click on "Gmail API"
5. Click the "Enable" button

## Step 3: Configure OAuth Consent Screen

1. Go to **APIs & Services** > **OAuth consent screen**
2. Select **External** user type (unless you have a Google Workspace organization)
3. Click "Create"
4. Fill in the required fields:
   - **App name**: `AREA Application`
   - **User support email**: Your email address
   - **Developer contact information**: Your email address
5. Click "Save and Continue"
6. On the **Scopes** page:
   - Click "Add or Remove Scopes"
   - Search for and add these scopes:
     - `https://www.googleapis.com/auth/gmail.readonly`
     - `https://www.googleapis.com/auth/gmail.send`
     - `https://www.googleapis.com/auth/gmail.modify`
   - Click "Update" then "Save and Continue"
7. On the **Test users** page:
   - Click "Add Users"
   - Add your Gmail address (the one you'll use for testing)
   - Click "Save and Continue"
8. Review and click "Back to Dashboard"

## Step 4: Create OAuth 2.0 Credentials

1. Go to **APIs & Services** > **Credentials**
2. Click "Create Credentials" > "OAuth client ID"
3. Configure the OAuth client:
   - **Application type**: Web application
   - **Name**: `AREA Gmail Integration`
   - **Authorized JavaScript origins**: (optional for now)
   - **Authorized redirect URIs**: Add these:
     - `http://localhost:8080/api/services/gmail/callback`
     - `http://localhost:8080/login/oauth2/code/google` (if using Spring Security OAuth2)
4. Click "Create"
5. **IMPORTANT**: A dialog will appear with your credentials:
   - **Client ID**: Copy this value
   - **Client Secret**: Copy this value
   - Download the JSON file for backup

## Step 5: Update Your .env File

Update the `/home/pandor/Delivery/AREA/server/.env` file with your credentials:

```bash
# Gmail OAuth
GOOGLE_CLIENT_ID=your-actual-client-id-here.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-actual-client-secret-here
GOOGLE_REDIRECT_URI=http://localhost:8080/api/services/gmail/callback
```

## Step 6: Test Account Setup

### Recommended Test Gmail Account
You have two options:

1. **Use your existing Gmail account** for testing
2. **Create a dedicated test account**:
   - Go to https://accounts.google.com/signup
   - Suggested username: `area.test.project@gmail.com` (or similar)
   - Complete the signup process
   - Add this account as a test user in the OAuth consent screen (Step 3.7 above)

### For Testing
Since your app is in "Testing" mode, only test users you explicitly add can authenticate. Make sure to:
- Add any Gmail accounts you want to test with as "Test users" in the OAuth consent screen
- Keep the app in Testing mode during development

## Step 7: Verify Configuration

Your credentials should look like this:
```
GOOGLE_CLIENT_ID=123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-aBcDeFgHiJkLmNoPqRsTuVwXyZ
```

## Security Notes

⚠️ **IMPORTANT**:
- Never commit your actual credentials to Git
- The `.env` file should be in `.gitignore`
- Use `.env.example` as a template with placeholder values
- For production, use environment variables or a secure secret manager

## Testing the Integration

Once configured, you can test the Gmail integration:

1. Start your Spring Boot server
2. Navigate to: `http://localhost:8080/api/services/gmail/auth`
3. You should be redirected to Google's OAuth consent page
4. Sign in with your test Gmail account
5. Grant the requested permissions
6. You should be redirected back to your callback URL

## Troubleshooting

### Error: "redirect_uri_mismatch"
- Verify the redirect URI in your `.env` matches exactly what's configured in Google Cloud Console
- Check for trailing slashes or http vs https

### Error: "Access blocked: This app's request is invalid"
- Make sure you've added test users in the OAuth consent screen
- Verify the Gmail API is enabled

### Error: "invalid_client"
- Double-check your CLIENT_ID and CLIENT_SECRET are correct
- Ensure there are no extra spaces or quotes in your `.env` file

## Additional Resources

- [Gmail API Documentation](https://developers.google.com/gmail/api)
- [Google OAuth 2.0 Guide](https://developers.google.com/identity/protocols/oauth2)
- [Spring Boot OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
