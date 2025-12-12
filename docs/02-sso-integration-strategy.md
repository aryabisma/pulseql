# SSO Integration Strategy: Pulsar to PulseQL

## Executive Summary

This document details the technical strategy for implementing Single Sign-On (SSO) between Pulsar and PulseQL, allowing Pulsar users to access PulseQL's query workspace without re-authentication while maintaining security and session integrity.

## Integration Requirements

### Functional Requirements

1. **Seamless Authentication**:
   - Pulsar users click "Query Workspace" link in Pulsar
   - PulseQL opens without login prompt
   - User session automatically created/validated
   - User identity synchronized from Pulsar

2. **User Context Transfer**:
   - User ID and profile information
   - Display name and metadata
   - Role and permission data
   - Team/group memberships

3. **Session Management**:
   - Maintain separate but linked sessions
   - Synchronized session expiration
   - Logout propagation
   - Session refresh handling

4. **Security Requirements**:
   - Secure token transmission
   - Token validation and expiration
   - No credential exposure
   - Protection against token replay attacks

### Non-Functional Requirements

1. **Performance**: SSO handshake < 500ms
2. **Security**: Industry-standard token security
3. **Reliability**: 99.9% authentication success rate
4. **Scalability**: Support concurrent user sessions

## Recommended SSO Approach

### Option 1: JWT Token-Based SSO (Recommended)

#### Architecture

```
┌─────────────────┐                    ┌──────────────────┐
│     Pulsar      │                    │     PulseQL      │
│   Application   │                    │   Application    │
└────────┬────────┘                    └────────┬─────────┘
         │                                      │
         │  1. User clicks "Query Workspace"    │
         │                                      │
         │  2. Generate JWT token              │
         │     - User ID                       │
         │     - Roles/Permissions             │
         │     - Expiration time               │
         │     - Signature (shared secret)     │
         │                                      │
         │  3. Redirect to PulseQL with token  │
         ├─────────────────────────────────────>│
         │     /pulseql?sso_token=<JWT>        │
         │                                      │
         │  4. Validate JWT                    │
         │     - Verify signature              │
         │     - Check expiration              │
         │     - Extract user data             │
         │                                      │
         │  5. Create/update user session      │
         │                                      │
         │  6. Display workspace               │
         │<─────────────────────────────────────┤
```

#### JWT Token Structure

**Token Payload**:
```json
{
  "iss": "pulsar",
  "sub": "user123",
  "exp": 1702660800,
  "iat": 1702657200,
  "nbf": 1702657200,
  "user": {
    "userId": "user123",
    "displayName": "John Doe",
    "email": "john.doe@example.com",
    "authRole": "analyst"
  },
  "permissions": [
    "query.execute",
    "connection.view",
    "data.export"
  ],
  "teams": [
    {
      "teamId": "analytics",
      "teamName": "Analytics Team",
      "teamRole": "member"
    }
  ],
  "metadata": {
    "department": "Analytics",
    "location": "US"
  }
}
```

#### Implementation Steps

**1. Pulsar-Side Implementation**:

```java
// Pulsar: Generate SSO token
public class PulsarSSOTokenGenerator {
    private static final String SECRET_KEY = "shared-secret-key"; // From config
    private static final long TOKEN_VALIDITY = 300000; // 5 minutes
    
    public String generateSSOToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        
        return JWT.create()
            .withIssuer("pulsar")
            .withSubject(user.getId())
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_VALIDITY))
            .withClaim("user", convertUserToClaims(user))
            .withClaim("permissions", getUserPermissions(user))
            .withClaim("teams", getUserTeams(user))
            .withClaim("metadata", getUserMetadata(user))
            .sign(algorithm);
    }
    
    public String buildPulseQLURL(String token, String mode) {
        return String.format(
            "%s/workspace?sso_token=%s&mode=%s",
            pulseQLBaseUrl,
            URLEncoder.encode(token, StandardCharsets.UTF_8),
            mode
        );
    }
}
```

**2. PulseQL-Side Implementation**:

Create custom authentication provider:

```java
// PulseQL: Custom SSO Authentication Provider
package io.cloudbeaver.service.auth.pulsar;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.service.auth.SMAuthProvider;
import org.jkiss.dbeaver.model.auth.SMAuthCredentials;

public class PulsarSSOAuthProvider implements SMAuthProvider<SMAuthCredentials> {
    
    public static final String PROVIDER_ID = "pulsar-sso";
    private static final String SECRET_KEY = "shared-secret-key"; // From config
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public SMAuthCredentials validateAuthentication(
        WebSession session,
        Map<String, Object> credentials
    ) throws DBWebException {
        String token = (String) credentials.get("sso_token");
        
        if (token == null || token.isEmpty()) {
            throw new DBWebException("SSO token is required");
        }
        
        try {
            // Validate JWT token
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("pulsar")
                .build();
            
            DecodedJWT jwt = verifier.verify(token);
            
            // Extract user information
            Map<String, Object> userClaims = jwt.getClaim("user").asMap();
            List<String> permissions = jwt.getClaim("permissions").asList(String.class);
            List<Map<String, Object>> teams = jwt.getClaim("teams").asList(Map.class);
            Map<String, Object> metadata = jwt.getClaim("metadata").asMap();
            
            // Create or update user in PulseQL
            String userId = (String) userClaims.get("userId");
            String displayName = (String) userClaims.get("displayName");
            String email = (String) userClaims.get("email");
            String authRole = (String) userClaims.get("authRole");
            
            // Return credentials
            return new PulsarSSOCredentials(
                userId,
                displayName,
                email,
                authRole,
                permissions,
                teams,
                metadata
            );
            
        } catch (JWTVerificationException e) {
            throw new DBWebException("Invalid SSO token", e);
        }
    }
    
    @Override
    public void endUserSession(WebSession session) {
        // Clean up session
    }
}
```

**3. Frontend Integration**:

```typescript
// PulseQL: SSO authentication handler
export class PulsarSSOService {
    
    async authenticateWithSSO(token: string): Promise<UserInfo> {
        // Call GraphQL mutation to authenticate
        const result = await this.graphQLService.sdk.authLogin({
            provider: 'pulsar-sso',
            credentials: {
                sso_token: token
            }
        });
        
        // Update user info resource
        await this.userInfoResource.load();
        
        return this.userInfoResource.data!;
    }
    
    // Parse SSO token from URL
    parseSSOTokenFromURL(): string | null {
        const params = new URLSearchParams(window.location.search);
        return params.get('sso_token');
    }
    
    // Handle SSO authentication on page load
    async handleSSOAuthentication(): Promise<boolean> {
        const token = this.parseSSOTokenFromURL();
        
        if (!token) {
            return false;
        }
        
        try {
            await this.authenticateWithSSO(token);
            
            // Remove token from URL for security
            const url = new URL(window.location.href);
            url.searchParams.delete('sso_token');
            window.history.replaceState({}, '', url.toString());
            
            return true;
        } catch (error) {
            console.error('SSO authentication failed:', error);
            return false;
        }
    }
}
```

**4. Bootstrap Integration**:

```typescript
// PulseQL: Application bootstrap
export class PulsarIntegrationBootstrap extends Bootstrap {
    
    async load(): Promise<void> {
        const ssoService = this.injector.getServiceByClass(PulsarSSOService);
        
        // Check for SSO token
        const authenticated = await ssoService.handleSSOAuthentication();
        
        if (authenticated) {
            console.log('User authenticated via Pulsar SSO');
        }
    }
}
```

### Option 2: SAML-Based SSO (Alternative)

#### Overview

Leverage PulseQL's existing federated authentication support with SAML protocol.

**Pros**:
- Uses existing PulseQL infrastructure
- Industry-standard protocol
- No custom code in PulseQL

**Cons**:
- More complex setup
- Requires SAML IdP in Pulsar
- Redirect-based flow (less seamless)

**Implementation**:
1. Configure Pulsar as SAML Identity Provider (IdP)
2. Configure PulseQL as Service Provider (SP)
3. Establish trust relationship (metadata exchange)
4. Implement SAML assertion generation in Pulsar
5. Configure attribute mapping

### Option 3: Session Cookie Sharing (Not Recommended)

**Why Not Recommended**:
- Security risks with shared cookies
- Same-domain requirement
- Session synchronization complexity
- CSRF vulnerabilities
- Browser security restrictions

## User Session Synchronization

### Session Lifecycle Management

**1. Session Creation**:
```
User logs into Pulsar
    ↓
Pulsar creates user session
    ↓
User accesses Query Workspace
    ↓
Generate SSO token with session ID
    ↓
PulseQL validates token
    ↓
PulseQL creates linked session
    ↓
Store Pulsar session ID in PulseQL session
```

**2. Session Validation**:
```typescript
// Periodic session validation
interface SessionValidationRequest {
    pulsarSessionId: string;
    pulseqlSessionId: string;
}

// Pulsar endpoint
@PostMapping("/api/session/validate")
public SessionValidationResponse validateSession(
    @RequestBody SessionValidationRequest request
) {
    Session session = sessionManager.getSession(request.getPulsarSessionId());
    
    return SessionValidationResponse.builder()
        .valid(session != null && session.isActive())
        .expiresAt(session != null ? session.getExpirationTime() : null)
        .build();
}
```

**3. Session Refresh**:
```java
// PulseQL: Periodic session refresh
public class PulsarSessionRefreshTask implements Runnable {
    
    @Override
    public void run() {
        // Get all active sessions linked to Pulsar
        List<WebSession> sessions = getActivePulsarLinkedSessions();
        
        for (WebSession session : sessions) {
            String pulsarSessionId = session.getAttribute("pulsar_session_id");
            
            // Validate with Pulsar
            boolean valid = validateWithPulsar(pulsarSessionId);
            
            if (!valid) {
                // Terminate PulseQL session
                session.close();
            }
        }
    }
    
    private boolean validateWithPulsar(String sessionId) {
        // Call Pulsar API to validate session
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(pulsarBaseUrl + "/api/session/validate"))
            .POST(/* session validation request */)
            .build();
            
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
            
        return response.statusCode() == 200 && 
               parseValidationResponse(response.body()).isValid();
    }
}
```

**4. Logout Synchronization**:

**Pulsar-initiated logout**:
```java
@PostMapping("/logout")
public ResponseEntity<?> logout(HttpSession session) {
    String userId = getCurrentUserId(session);
    
    // Invalidate Pulsar session
    session.invalidate();
    
    // Notify PulseQL to invalidate linked sessions
    pulseQLSessionService.invalidateUserSessions(userId);
    
    return ResponseEntity.ok().build();
}
```

**PulseQL webhook endpoint**:
```java
@PostMapping("/api/pulsar/session/invalidate")
public void invalidateUserSessions(@RequestBody SessionInvalidationRequest request) {
    // Verify request signature
    if (!verifyRequestSignature(request)) {
        throw new UnauthorizedException();
    }
    
    // Find and close all sessions for user
    List<WebSession> sessions = sessionManager.getUserSessions(request.getUserId());
    
    for (WebSession session : sessions) {
        if ("pulsar-sso".equals(session.getAuthProvider())) {
            session.close();
        }
    }
}
```

## Permission Synchronization

### RBAC Mapping Strategy

**1. Permission Model Mapping**:

```java
// Pulsar permissions → PulseQL permissions mapping
public class PermissionMapper {
    
    private static final Map<String, List<String>> PERMISSION_MAP = Map.of(
        // Pulsar permission → PulseQL permissions
        "pulsar.report.view", List.of(
            "connection.view",
            "sql.execute",
            "data.view"
        ),
        "pulsar.report.edit", List.of(
            "connection.view",
            "sql.execute",
            "data.view",
            "data.edit"
        ),
        "pulsar.report.export", List.of(
            "connection.view",
            "sql.execute",
            "data.view",
            "data.export"
        ),
        "pulsar.admin", List.of(
            "connection.view",
            "connection.edit",
            "sql.execute",
            "data.view",
            "data.edit",
            "data.export",
            "admin"
        )
    );
    
    public Set<String> mapPulsarPermissions(Set<String> pulsarPermissions) {
        return pulsarPermissions.stream()
            .flatMap(p -> PERMISSION_MAP.getOrDefault(p, List.of()).stream())
            .collect(Collectors.toSet());
    }
}
```

**2. Dynamic Permission Enforcement**:

```typescript
// PulseQL: Permission-based UI control
export class PulsarPermissionService {
    
    hasPermission(permission: string): boolean {
        const userInfo = this.userInfoResource.data;
        
        if (!userInfo) {
            return false;
        }
        
        // Check if user has permission from Pulsar
        const pulsarPermissions = userInfo.metaParameters['pulsar_permissions'] || [];
        return pulsarPermissions.includes(permission);
    }
    
    // Hide UI elements based on permissions
    canCreateConnection(): boolean {
        return false; // Always false for Pulsar users
    }
    
    canExecuteQuery(): boolean {
        return this.hasPermission('query.execute');
    }
    
    canExportData(): boolean {
        return this.hasPermission('data.export');
    }
}
```

## Security Considerations

### Token Security

**1. Token Signing**:
- Use HMAC-SHA256 or RSA-SHA256
- Rotate signing keys periodically
- Store secret keys securely (e.g., vault)

**2. Token Validation**:
- Verify signature
- Check expiration (exp claim)
- Validate issuer (iss claim)
- Verify not-before (nbf claim)
- Check audience if applicable

**3. Token Transmission**:
- Use HTTPS only
- Short expiration time (5-15 minutes)
- One-time use tokens (optional)
- Remove from URL after use

**4. Token Storage**:
- Never store in localStorage
- Remove from browser history
- Don't log tokens
- Use secure session storage

### Replay Attack Prevention

```java
// Token replay prevention
public class TokenReplayPrevention {
    
    private final Cache<String, Boolean> usedTokens = CacheBuilder.newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build();
    
    public void validateToken(String token) throws DBWebException {
        // Check if token was already used
        if (usedTokens.getIfPresent(token) != null) {
            throw new DBWebException("Token already used");
        }
        
        // Mark token as used
        usedTokens.put(token, true);
        
        // Verify JWT...
    }
}
```

### CORS Configuration

```java
// PulseQL: CORS configuration for Pulsar integration
@Configuration
public class PulsarCORSConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow Pulsar origin
        config.addAllowedOrigin(pulsarBaseUrl);
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        
        UrlBasedCorsConfigurationSource source = 
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        return new CorsFilter(source);
    }
}
```

## Configuration Management

### Shared Configuration

**Configuration File** (`pulsar-integration.conf`):
```yaml
pulsar:
  integration:
    enabled: true
    base_url: "https://pulsar.example.com"
    
sso:
  enabled: true
  provider: "pulsar-sso"
  jwt:
    secret_key: "${PULSAR_SSO_SECRET}"
    algorithm: "HS256"
    token_expiration: 300  # 5 minutes
    
  session:
    sync_interval: 60  # seconds
    validation_url: "${PULSAR_BASE_URL}/api/session/validate"
    
permissions:
  sync_enabled: true
  mapping_file: "pulsar-permission-mapping.json"
  
security:
  replay_prevention: true
  token_cache_size: 10000
  require_https: true
```

### Environment Variables

```bash
# Pulsar
PULSEQL_BASE_URL=https://pulseql.example.com
PULSEQL_SSO_SECRET=<shared-secret>

# PulseQL
PULSAR_BASE_URL=https://pulsar.example.com
PULSAR_SSO_SECRET=<shared-secret>
PULSAR_SSO_ENABLED=true
```

## Testing Strategy

### Unit Tests

```java
@Test
public void testSSOTokenGeneration() {
    User user = createTestUser();
    String token = tokenGenerator.generateSSOToken(user);
    
    assertNotNull(token);
    
    DecodedJWT jwt = JWT.decode(token);
    assertEquals("pulsar", jwt.getIssuer());
    assertEquals(user.getId(), jwt.getSubject());
}

@Test
public void testSSOTokenValidation() {
    String token = generateValidToken();
    
    SMAuthCredentials credentials = authProvider.validateAuthentication(
        session,
        Map.of("sso_token", token)
    );
    
    assertNotNull(credentials);
    assertEquals("user123", credentials.getUserId());
}

@Test
public void testExpiredTokenRejection() {
    String expiredToken = generateExpiredToken();
    
    assertThrows(DBWebException.class, () -> {
        authProvider.validateAuthentication(
            session,
            Map.of("sso_token", expiredToken)
        );
    });
}
```

### Integration Tests

```java
@Test
public void testEndToEndSSOFlow() {
    // 1. User logs into Pulsar
    PulsarSession pulsarSession = pulsarApp.login("user123", "password");
    
    // 2. Generate SSO token
    String token = pulsarSession.generatePulseQLToken();
    
    // 3. Open PulseQL with token
    String pulseqlUrl = pulseqlApp.buildSSOUrl(token);
    WebDriver driver = new ChromeDriver();
    driver.get(pulseqlUrl);
    
    // 4. Verify user is authenticated
    assertTrue(pulseqlApp.isUserAuthenticated(driver));
    assertEquals("user123", pulseqlApp.getCurrentUserId(driver));
    
    // 5. Verify permissions are applied
    assertFalse(pulseqlApp.canCreateConnection(driver));
    assertTrue(pulseqlApp.canExecuteQuery(driver));
}
```

## Performance Optimization

### Token Caching

```java
public class TokenCache {
    
    private final Cache<String, UserInfo> tokenCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();
    
    public UserInfo validateAndCache(String token) {
        return tokenCache.get(token, () -> {
            // Expensive validation
            return validateToken(token);
        });
    }
}
```

### Connection Pooling

```java
// HTTP client for Pulsar API calls
private final HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .executor(Executors.newFixedThreadPool(10))
    .build();
```

## Error Handling

### Error Scenarios

1. **Invalid Token**: Show login page with error message
2. **Expired Token**: Redirect back to Pulsar
3. **Network Error**: Retry with exponential backoff
4. **Session Expired**: Notify user and redirect to Pulsar
5. **Permission Denied**: Show appropriate error message

### Error Response Format

```json
{
  "error": {
    "code": "SSO_TOKEN_INVALID",
    "message": "The SSO token is invalid or expired",
    "timestamp": "2024-12-12T14:30:00Z",
    "details": {
      "reason": "Token signature verification failed"
    }
  }
}
```

## Monitoring and Logging

### Logging Requirements

```java
// Log SSO authentication attempts
logger.info("SSO authentication attempt - userId: {}, issuer: {}", 
    userId, tokenIssuer);

// Log successful authentication
logger.info("SSO authentication successful - userId: {}, sessionId: {}", 
    userId, sessionId);

// Log failed authentication
logger.warn("SSO authentication failed - reason: {}, token_hash: {}", 
    reason, tokenHash);

// Log session synchronization
logger.debug("Session sync - pulsarSessionId: {}, pulseqlSessionId: {}, valid: {}", 
    pulsarSessionId, pulseqlSessionId, isValid);
```

### Metrics

- SSO authentication success rate
- Average authentication time
- Token validation failures
- Session sync failures
- Active linked sessions count

## Migration Path

### Phase 1: Development
1. Implement JWT token generation in Pulsar
2. Create custom auth provider in PulseQL
3. Develop frontend SSO handler
4. Unit and integration testing

### Phase 2: Testing
1. Deploy to test environment
2. User acceptance testing
3. Performance testing
4. Security testing

### Phase 3: Production
1. Configure production secrets
2. Deploy to production
3. Monitor metrics
4. Gradual rollout

## Conclusion

The JWT token-based SSO approach provides the most flexible and secure integration between Pulsar and PulseQL. It allows for:

- Seamless user experience
- Strong security with industry-standard practices
- Flexible permission mapping
- Independent session management
- Easy monitoring and debugging

The implementation requires coordination between both applications but provides a clean, maintainable solution that can evolve with changing requirements.
