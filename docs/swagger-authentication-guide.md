# Swagger UI Authentication Guide

## Overview

The Video Metadata Service API uses JWT (JSON Web Token) authentication. This guide explains how to authenticate and use the API through Swagger UI.

## Authentication Flow

### 1. Access Swagger UI
- Navigate to: `http://localhost:8080/swagger-ui.html`
- You'll see the API documentation with an "Authorize" button at the top

### 2. Get a JWT Token
1. **Find the Login Endpoint**: Look for the `/auth/login` endpoint under the "Authentication" section
2. **Click on the endpoint** to expand it
3. **Click "Try it out"**
4. **Enter login credentials**:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
5. **Click "Execute"**
6. **Copy the token**: From the response, copy the `access_token` value

### 3. Authorize in Swagger UI
1. **Click the "Authorize" button** at the top of the page
2. **Enter the token**: In the "Value" field, enter: `Bearer <your-token>`
   - Example: `Bearer eyJhbGciOiJIUzI1NiJ9...`
3. **Click "Authorize"**
4. **Close the dialog**

### 4. Use Protected Endpoints
Now you can use all the protected endpoints:
- **Import Videos** (`POST /videos/import`) - Requires ADMIN role
- **Get Videos** (`GET /videos`) - Requires USER or ADMIN role
- **Get Video by ID** (`GET /videos/{id}`) - Requires USER or ADMIN role
- **Get Statistics** (`GET /videos/stats`) - Requires USER or ADMIN role

## Available Users

| Username | Password | Roles | Description |
|----------|----------|-------|-------------|
| `admin`  | `admin123` | ADMIN, USER | Full access to all endpoints |
| `user`   | `user123`  | USER | Read-only access to videos and statistics |

## Example Usage

### Step 1: Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Step 2: Use the Token
```bash
# Copy the access_token from the login response
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# Use the token in subsequent requests
curl -X POST http://localhost:8080/videos/import \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "source": "MOCK",
    "videoIds": ["video1", "video2", "video3"],
    "batchSize": 10
  }'
```

## API Parameters

### Video Source Values
When using the API, use these exact values for the `source` parameter:
- **`MOCK`** - For mock video service (development/testing)

**Note**: The API expects uppercase `MOCK`, not `Mock` or `mock`.

## Troubleshooting

### Common Issues

1. **"Unauthorized" Error**
   - Make sure you've clicked "Authorize" in Swagger UI
   - Verify the token format: `Bearer <token>`
   - Check if the token has expired (tokens expire after 24 hours)

2. **"Forbidden" Error**
   - The endpoint requires a specific role
   - Use the `admin` user for full access
   - Use the `user` user for read-only access

3. **Token Not Working**
   - Get a fresh token by logging in again
   - Make sure you're using the correct username/password
   - Check that the application is running

4. **Parameter Conversion Error**
   - Make sure you're using `MOCK` (uppercase) for the source parameter
   - The API expects exact enum values, not display names

### Token Format
- **Correct**: `Bearer eyJhbGciOiJIUzI1NiJ9...`
- **Incorrect**: `eyJhbGciOiJIUzI1NiJ9...` (missing "Bearer ")

## Security Notes

- JWT tokens expire after 24 hours
- Tokens are stateless - no server-side storage
- Use HTTPS in production
- Keep tokens secure and don't share them
- Logout by clearing the authorization in Swagger UI

## API Endpoints Summary

| Endpoint | Method | Authentication | Roles | Description |
|----------|--------|----------------|-------|-------------|
| `/auth/login` | POST | None | None | Get JWT token |
| `/videos/import` | POST | Required | ADMIN | Import videos |
| `/videos` | GET | Required | USER, ADMIN | List videos |
| `/videos/{id}` | GET | Required | USER, ADMIN | Get video by ID |
| `/videos/stats` | GET | Required | USER, ADMIN | Get statistics | 