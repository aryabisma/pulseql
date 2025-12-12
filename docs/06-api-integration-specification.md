# API Integration Specification

## Executive Summary

This document specifies the API integration between Pulsar and PulseQL, defining endpoints, data contracts, communication protocols, and integration patterns for seamless interoperability.

## API Overview

### Integration Points

```
Pulsar ←→ PulseQL Communication:

1. SSO Authentication
   Pulsar → PulseQL: JWT token for user authentication

2. Session Validation
   PulseQL → Pulsar: Validate active sessions

3. Permission Sync
   PulseQL → Pulsar: Fetch user permissions

4. Webhook Notifications
   Pulsar → PulseQL: User/permission updates

5. Connection Configuration
   Pulsar → PulseQL: Provide database connections
```

## Pulsar APIs (For PulseQL)

### 1. Session Validation API

**Endpoint**: `POST /api/session/validate`

**Purpose**: Validate that a Pulsar session is still active

**Request**:
```json
{
  "sessionId": "pulsar-session-abc123",
  "userId": "user123"
}
```

**Response** (200 OK):
```json
{
  "valid": true,
  "userId": "user123",
  "displayName": "John Doe",
  "expiresAt": "2024-12-12T18:00:00Z",
  "lastActivity": "2024-12-12T15:30:00Z"
}
```

**Response** (401 Unauthorized):
```json
{
  "valid": false,
  "reason": "SESSION_EXPIRED",
  "message": "Session has expired"
}
```

**Implementation**:
```java
// Pulsar: Session validation endpoint
@RestController
@RequestMapping("/api/session")
public class SessionValidationController {
    
    @Autowired
    private SessionManager sessionManager;
    
    @PostMapping("/validate")
    public ResponseEntity<SessionValidationResponse> validateSession(
        @RequestBody SessionValidationRequest request,
        @RequestHeader("X-API-Key") String apiKey
    ) {
        // Verify API key
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(401).build();
        }
        
        // Find session
        Session session = sessionManager.getSession(request.getSessionId());
        
        if (session == null || !session.getUserId().equals(request.getUserId())) {
            return ResponseEntity.status(401)
                .body(SessionValidationResponse.invalid("SESSION_NOT_FOUND"));
        }
        
        if (session.isExpired()) {
            return ResponseEntity.status(401)
                .body(SessionValidationResponse.invalid("SESSION_EXPIRED"));
        }
        
        // Return valid session info
        return ResponseEntity.ok(SessionValidationResponse.builder()
            .valid(true)
            .userId(session.getUserId())
            .displayName(session.getDisplayName())
            .expiresAt(session.getExpirationTime())
            .lastActivity(session.getLastActivityTime())
            .build());
    }
}
```

### 2. User Permissions API

**Endpoint**: `GET /api/user/{userId}/permissions`

**Purpose**: Fetch current permissions for a user

**Request Headers**:
```
X-API-Key: <shared-api-key>
Authorization: Bearer <jwt-token>
```

**Response** (200 OK):
```json
{
  "userId": "user123",
  "role": "ANALYST",
  "permissions": [
    "report.view",
    "report.execute",
    "report.edit",
    "data.export"
  ],
  "pulseqlPermissions": [
    "connection.view",
    "sql.execute",
    "data.view",
    "data.export",
    "sql.script.save"
  ],
  "teams": [
    {
      "teamId": "analytics",
      "teamName": "Analytics Team",
      "role": "member",
      "permissions": ["team.view", "team.query"]
    }
  ],
  "metadata": {
    "department": "Analytics",
    "location": "US"
  },
  "updatedAt": "2024-12-12T15:00:00Z"
}
```

**Implementation**:
```java
@GetMapping("/user/{userId}/permissions")
public ResponseEntity<UserPermissionsResponse> getUserPermissions(
    @PathVariable String userId,
    @RequestHeader("X-API-Key") String apiKey
) {
    if (!isValidApiKey(apiKey)) {
        return ResponseEntity.status(401).build();
    }
    
    User user = userService.getUser(userId);
    if (user == null) {
        return ResponseEntity.notFound().build();
    }
    
    return ResponseEntity.ok(UserPermissionsResponse.builder()
        .userId(user.getId())
        .role(user.getRole())
        .permissions(permissionService.getUserPermissions(user))
        .pulseqlPermissions(permissionMapper.mapToPulseQL(user.getPermissions()))
        .teams(user.getTeams())
        .metadata(user.getMetadata())
        .updatedAt(user.getPermissionsUpdatedAt())
        .build());
}
```

### 3. Connection Configuration API

**Endpoint**: `GET /api/user/{userId}/connections`

**Purpose**: Get database connections accessible to user

**Response** (200 OK):
```json
{
  "connections": [
    {
      "id": "conn-analytics-prod",
      "name": "Analytics Production",
      "driver": "postgresql",
      "host": "analytics-db.internal",
      "port": 5432,
      "database": "analytics",
      "username": "readonly_user",
      "password": "<encrypted>",
      "ssl": true,
      "readonly": true,
      "properties": {
        "schema": "public",
        "applicationName": "Pulsar-PulseQL"
      }
    },
    {
      "id": "conn-warehouse",
      "name": "Data Warehouse",
      "driver": "snowflake",
      "host": "company.snowflakecomputing.com",
      "database": "warehouse",
      "username": "pulsar_service",
      "password": "<encrypted>",
      "readonly": true,
      "properties": {
        "warehouse": "COMPUTE_WH",
        "role": "ANALYST_ROLE"
      }
    }
  ]
}
```

**Implementation**:
```java
@GetMapping("/user/{userId}/connections")
public ResponseEntity<ConnectionsResponse> getUserConnections(
    @PathVariable String userId,
    @RequestHeader("X-API-Key") String apiKey
) {
    if (!isValidApiKey(apiKey)) {
        return ResponseEntity.status(401).build();
    }
    
    List<DatabaseConnection> connections = 
        connectionService.getUserConnections(userId);
    
    // Encrypt sensitive data
    List<ConnectionDTO> connectionDTOs = connections.stream()
        .map(conn -> ConnectionDTO.builder()
            .id(conn.getId())
            .name(conn.getName())
            .driver(conn.getDriver())
            .host(conn.getHost())
            .port(conn.getPort())
            .database(conn.getDatabase())
            .username(conn.getUsername())
            .password(encryptionService.encrypt(conn.getPassword()))
            .ssl(conn.isSslEnabled())
            .readonly(true)  // Always readonly for PulseQL
            .properties(conn.getProperties())
            .build())
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(new ConnectionsResponse(connectionDTOs));
}
```

### 4. User Logout Notification

**Endpoint**: `POST /api/webhook/user-logout`

**Purpose**: Receive notifications when users log out of Pulsar

**Request**:
```json
{
  "event": "user.logout",
  "userId": "user123",
  "sessionId": "pulsar-session-abc123",
  "timestamp": "2024-12-12T16:00:00Z",
  "reason": "USER_INITIATED"
}
```

**Response** (200 OK):
```json
{
  "received": true,
  "sessionsInvalidated": 2
}
```

## PulseQL APIs (For Pulsar)

### 1. SSO Authentication GraphQL

**Endpoint**: `POST /api/gql`

**Mutation**: `authLogin`

**Request**:
```graphql
mutation PulsarSSOLogin($token: String!) {
  authLogin(
    provider: "pulsar-sso"
    credentials: { sso_token: $token }
  ) {
    authStatus
    userTokens {
      authProvider
      userId
      displayName
      loginTime
    }
  }
}
```

**Variables**:
```json
{
  "token": "<JWT_TOKEN>"
}
```

**Response**:
```json
{
  "data": {
    "authLogin": {
      "authStatus": "SUCCESS",
      "userTokens": [
        {
          "authProvider": "pulsar-sso",
          "userId": "user123",
          "displayName": "John Doe",
          "loginTime": "2024-12-12T15:30:00Z"
        }
      ]
    }
  }
}
```

### 2. Get Active User

**Query**: `activeUser`

**Request**:
```graphql
query GetActiveUser {
  activeUser {
    userId
    displayName
    authRole
    authTokens {
      authProvider
      userId
      displayName
    }
    metaParameters
    configurationParameters
    teams {
      teamId
      teamName
      teamRole
    }
  }
}
```

**Response**:
```json
{
  "data": {
    "activeUser": {
      "userId": "user123",
      "displayName": "John Doe",
      "authRole": "analyst",
      "authTokens": [
        {
          "authProvider": "pulsar-sso",
          "userId": "user123",
          "displayName": "John Doe"
        }
      ],
      "metaParameters": {
        "pulsar_role": "ANALYST",
        "pulsar_permissions": ["report.view", "data.export"],
        "department": "Analytics"
      },
      "teams": [
        {
          "teamId": "analytics",
          "teamName": "Analytics Team",
          "teamRole": "member"
        }
      ]
    }
  }
}
```

### 3. Execute Query

**Mutation**: `sqlExecute`

**Request**:
```graphql
mutation ExecuteQuery(
  $connectionId: ID!
  $contextId: ID!
  $query: String!
) {
  sqlExecuteQuery(
    connectionId: $connectionId
    contextId: $contextId
    query: $query
  ) {
    resultSetId
    updateRowCount
    duration
    statusMessage
  }
}
```

### 4. Health Check

**Endpoint**: `GET /api/health`

**Response** (200 OK):
```json
{
  "status": "UP",
  "timestamp": "2024-12-12T15:30:00Z",
  "checks": {
    "database": {
      "status": "UP",
      "responseTime": 5
    },
    "pulsarIntegration": {
      "status": "UP",
      "lastSync": "2024-12-12T15:25:00Z"
    },
    "activeSessions": {
      "count": 42,
      "pulsarSessions": 15
    }
  }
}
```

## Webhook Integration

### Pulsar → PulseQL Webhooks

**1. Permission Update Webhook**

**Endpoint**: `POST /api/pulsar/webhook/permissions`

**Request**:
```json
{
  "event": "permissions.updated",
  "userId": "user123",
  "timestamp": "2024-12-12T15:30:00Z",
  "changes": {
    "added": ["data.export"],
    "removed": ["data.edit"]
  },
  "newPermissions": [
    "report.view",
    "report.execute",
    "data.export"
  ],
  "signature": "<HMAC-SHA256-signature>"
}
```

**Implementation**:
```java
// PulseQL: Webhook handler
@RestController
@RequestMapping("/api/pulsar/webhook")
public class PulsarWebhookController {
    
    @PostMapping("/permissions")
    public ResponseEntity<WebhookResponse> handlePermissionUpdate(
        @RequestBody PermissionUpdateWebhook webhook,
        @RequestHeader("X-Webhook-Signature") String signature
    ) {
        // Verify webhook signature
        if (!webhookService.verifySignature(webhook, signature)) {
            return ResponseEntity.status(401)
                .body(WebhookResponse.error("Invalid signature"));
        }
        
        // Update user permissions in active sessions
        List<WebSession> sessions = sessionManager.getUserSessions(
            webhook.getUserId()
        );
        
        int updated = 0;
        for (WebSession session : sessions) {
            if ("pulsar-sso".equals(session.getAuthProvider())) {
                // Update session permissions
                session.setAttribute("pulsar_permissions", 
                    webhook.getNewPermissions());
                
                // Map to PulseQL permissions
                Set<String> pulseqlPerms = permissionMapper.mapPulsarPermissions(
                    webhook.getNewPermissions()
                );
                session.setAttribute("pulseql_permissions", pulseqlPerms);
                
                updated++;
            }
        }
        
        return ResponseEntity.ok(WebhookResponse.builder()
            .received(true)
            .sessionsUpdated(updated)
            .build());
    }
}
```

**2. Session Invalidation Webhook**

**Endpoint**: `POST /api/pulsar/webhook/session-invalidate`

**Request**:
```json
{
  "event": "session.invalidated",
  "userId": "user123",
  "sessionId": "pulsar-session-abc123",
  "timestamp": "2024-12-12T16:00:00Z",
  "reason": "USER_LOGOUT",
  "signature": "<HMAC-SHA256-signature>"
}
```

## Authentication Flow

### Complete SSO Flow

```
┌─────────┐                                          ┌─────────┐
│ Pulsar  │                                          │ PulseQL │
└────┬────┘                                          └────┬────┘
     │                                                     │
     │ 1. User clicks "Query Workspace"                   │
     │                                                     │
     │ 2. Generate JWT Token                              │
     │    POST /api/sso/generate-token                    │
     │    ┌────────────────────────────┐                  │
     │    │ {                          │                  │
     │    │   userId: "user123",       │                  │
     │    │   permissions: [...],      │                  │
     │    │   expiresIn: 300          │                  │
     │    │ }                          │                  │
     │    └────────────────────────────┘                  │
     │                                                     │
     │ 3. Build PulseQL URL with token                    │
     │    https://pulseql.example.com/workspace?          │
     │      sso_token=<JWT>&mode=pulsar                   │
     │                                                     │
     │ 4. Redirect user to PulseQL                        │
     ├────────────────────────────────────────────────────>│
     │                                                     │
     │                              5. Parse SSO token    │
     │                                                     │
     │                              6. GraphQL authLogin  │
     │                              mutation with token   │
     │                                                     │
     │                              7. Validate JWT:      │
     │                                 - Verify signature │
     │                                 - Check expiration │
     │                                 - Extract claims   │
     │                                                     │
     │                              8. Create session     │
     │                                                     │
     │<─────────────────────────────9. Redirect to────────│
     │                              workspace (remove     │
     │                              token from URL)       │
     │                                                     │
     │ 10. User works in PulseQL workspace                │
     │                                                     │
     │ 11. Periodic session validation                    │
     │<────────────────────────────────────────────────────│
     │     POST /api/session/validate                     │
     │     { sessionId: "...", userId: "..." }            │
     │                                                     │
     ├────────────────────────────────────────────────────>│
     │     { valid: true, expiresAt: "..." }              │
     │                                                     │
     │ 12. User logs out from Pulsar                      │
     │                                                     │
     ├────────────────────────────────────────────────────>│
     │     POST /api/pulsar/webhook/session-invalidate    │
     │                                                     │
     │<────────────────────────────────────────────────────│
     │     { received: true, sessionsInvalidated: 1 }     │
     │                                                     │
     │                              13. Close PulseQL     │
     │                              session               │
     │                                                     │
```

## Error Handling

### Error Response Format

**Standard Error Response**:
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {
      "field": "Additional context"
    },
    "timestamp": "2024-12-12T15:30:00Z",
    "requestId": "req-abc-123"
  }
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_TOKEN` | 401 | JWT token is invalid or malformed |
| `TOKEN_EXPIRED` | 401 | JWT token has expired |
| `SESSION_NOT_FOUND` | 401 | Session does not exist |
| `SESSION_EXPIRED` | 401 | Session has expired |
| `PERMISSION_DENIED` | 403 | User lacks required permission |
| `USER_NOT_FOUND` | 404 | User does not exist |
| `CONNECTION_NOT_FOUND` | 404 | Database connection not found |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `INTERNAL_ERROR` | 500 | Internal server error |
| `SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable |

### Retry Logic

```typescript
// Client-side retry logic
export class PulsarAPIClient {
  
  async callWithRetry<T>(
    fn: () => Promise<T>,
    maxRetries: number = 3,
    backoff: number = 1000
  ): Promise<T> {
    let lastError: Error;
    
    for (let i = 0; i < maxRetries; i++) {
      try {
        return await fn();
      } catch (error) {
        lastError = error as Error;
        
        // Don't retry on client errors (4xx)
        if (error.response?.status < 500) {
          throw error;
        }
        
        // Wait before retry with exponential backoff
        if (i < maxRetries - 1) {
          await this.sleep(backoff * Math.pow(2, i));
        }
      }
    }
    
    throw lastError!;
  }
  
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
```

## Rate Limiting

### API Rate Limits

```yaml
rate_limits:
  session_validation:
    limit: 100
    window: 60  # seconds
    
  user_permissions:
    limit: 50
    window: 60
    
  connections:
    limit: 20
    window: 60
    
  webhooks:
    limit: 1000
    window: 60
```

### Implementation

```java
@Component
public class RateLimitFilter implements Filter {
    
    private final RateLimiter rateLimiter;
    
    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String apiKey = httpRequest.getHeader("X-API-Key");
        String endpoint = httpRequest.getRequestURI();
        
        String key = apiKey + ":" + endpoint;
        
        if (!rateLimiter.tryAcquire(key)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.getWriter().write(
                "{\"error\": \"Rate limit exceeded\"}"
            );
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

## API Versioning

### URL-Based Versioning

```
https://pulsar.example.com/api/v1/session/validate
https://pulsar.example.com/api/v2/session/validate
```

### Header-Based Versioning

```
GET /api/session/validate
Accept: application/vnd.pulsar.v1+json
```

## API Documentation

### OpenAPI Specification

```yaml
openapi: 3.0.0
info:
  title: Pulsar API for PulseQL Integration
  version: 1.0.0
  description: API endpoints for PulseQL integration

servers:
  - url: https://pulsar.example.com/api/v1
    description: Production server

paths:
  /session/validate:
    post:
      summary: Validate user session
      security:
        - ApiKeyAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SessionValidationRequest'
      responses:
        '200':
          description: Session is valid
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SessionValidationResponse'
        '401':
          description: Session is invalid
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

components:
  schemas:
    SessionValidationRequest:
      type: object
      required:
        - sessionId
        - userId
      properties:
        sessionId:
          type: string
        userId:
          type: string
    
    SessionValidationResponse:
      type: object
      properties:
        valid:
          type: boolean
        userId:
          type: string
        displayName:
          type: string
        expiresAt:
          type: string
          format: date-time
        lastActivity:
          type: string
          format: date-time
    
    ErrorResponse:
      type: object
      properties:
        error:
          type: object
          properties:
            code:
              type: string
            message:
              type: string
            timestamp:
              type: string
              format: date-time

  securitySchemes:
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-API-Key
```

## Testing

### API Integration Tests

```java
@SpringBootTest
public class PulsarAPIIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void testSessionValidation() {
        // Create test session
        String sessionId = createTestSession("user123");
        
        // Validate session
        SessionValidationRequest request = new SessionValidationRequest(
            sessionId,
            "user123"
        );
        
        ResponseEntity<SessionValidationResponse> response = 
            restTemplate.postForEntity(
                "/api/session/validate",
                request,
                SessionValidationResponse.class
            );
        
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isValid());
    }
    
    @Test
    public void testRateLimit() {
        // Make 101 requests (exceeds limit of 100)
        for (int i = 0; i < 101; i++) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/session/validate",
                new SessionValidationRequest("session", "user"),
                String.class
            );
            
            if (i < 100) {
                assertNotEquals(429, response.getStatusCodeValue());
            } else {
                assertEquals(429, response.getStatusCodeValue());
            }
        }
    }
}
```

## Conclusion

This API specification provides:

1. **Clear Contracts**: Well-defined request/response formats
2. **Security**: API key authentication and webhook signatures
3. **Reliability**: Retry logic and rate limiting
4. **Versioning**: Support for API evolution
5. **Documentation**: OpenAPI specification for client generation
6. **Testing**: Comprehensive integration tests

These APIs enable seamless integration between Pulsar and PulseQL while maintaining security and reliability.
