# SSO Integration - Implementation Guide

**Date**: December 13, 2025  
**Version**: 1.2  
**Status**: SSO Integrated

## Overview

This document provides a complete guide for the SSO integration between Pulsar and PulseQL, including JWT token handling, permission management, and secure authentication flow.

## Architecture

### SSO Flow Diagram

```
┌─────────────┐                           ┌──────────────┐
│   Pulsar    │                           │   PulseQL    │
│ Application │                           │ Application  │
└──────┬──────┘                           └──────┬───────┘
       │                                         │
       │ 1. User clicks "Query Workspace"        │
       │                                         │
       │ 2. Generate JWT Token                   │
       │    - User ID, roles, permissions        │
       │    - Expiration: 5-15 minutes           │
       │    - Signature with secret key          │
       │                                         │
       │ 3. Redirect with token in URL           │
       ├────────────────────────────────────────>│
       │ /workspace?mode=pulsar&sso_token=<JWT>  │
       │                                         │
       │                                         │ 4. PulsarSSOService
       │                                         │    - Parse token from URL
       │                                         │    - Validate structure
       │                                         │    - Check expiration
       │                                         │    - Verify issuer/audience
       │                                         │
       │                                         │ 5. Backend Validation
       │                                         │    - Verify signature
       │                                         │    - Check user in DB
       │                                         │    - Create session
       │                                         │
       │                                         │ 6. WorkspaceModeService
       │                                         │    - Apply workspace mode
       │                                         │    - Apply branding
       │                                         │    - Apply theme
       │                                         │
       │                                         │ 7. Display Workspace
       │<────────────────────────────────────────┤
       │                                         │
```

## Implementation Components

### 1. PulsarSSOService

**Location**: `src/PulsarSSOService.ts`

**Responsibilities**:
- JWT token validation (client-side)
- User session management
- Token refresh mechanism
- Permission storage and retrieval

**Key Methods**:

```typescript
// Initialize SSO from URL token
async initializeFromURL(): Promise<boolean>

// Validate JWT token structure and claims
async validateToken(token: string): Promise<SSOTokenValidationResult>

// Check user permissions
hasPermission(permission: string): boolean
hasAnyPermission(permissions: string[]): boolean
hasAllPermissions(permissions: string[]): boolean

// Get user information
get userId(): string | null
get userDisplayName(): string | null
get userPermissions(): string[]

// Logout and cleanup
logout(): void
```

**Security Features**:
- Validates JWT structure (header.payload.signature)
- Checks required claims: iss, sub, exp, aud, jti
- Verifies issuer and audience
- Validates token expiration and not-before time
- Checks token age to prevent old tokens
- Automatically clears token from URL
- Schedules token refresh before expiration
- Secure error handling without exposing details

### 2. PulsarPermissionService

**Location**: `src/PulsarPermissionService.ts`

**Responsibilities**:
- Permission-based access control (RBAC)
- Granular permission checking
- Integration with SSO service

**Permission Categories**:

1. **Query Permissions**
   - `query.execute` - Execute SQL queries
   - `query.save` - Save queries
   - `query.share` - Share queries with team

2. **Data Permissions**
   - `data.view` - View query results
   - `data.export` - Export data to files
   - `data.edit` - Modify data (INSERT/UPDATE)
   - `data.delete` - Delete data

3. **Connection Permissions**
   - `connection.view` - View database connections
   - `connection.create` - Create new connections
   - `connection.edit` - Modify connections
   - `connection.delete` - Delete connections

4. **Schema Permissions**
   - `schema.view` - View database schemas
   - `schema.create` - Create schemas/tables
   - `schema.edit` - Modify schema objects
   - `schema.delete` - Drop schema objects

5. **Administration Permissions**
   - `admin.users` - Manage users
   - `admin.settings` - Manage settings
   - `admin.logs` - View audit logs

**Usage Example**:

```typescript
// In component
const permissionService = useService(PulsarPermissionService);

// Check single permission
if (permissionService.canExportData()) {
    // Show export button
}

// Check multiple permissions
if (permissionService.hasAnyPermission(['data.edit', 'data.delete'])) {
    // Show data modification UI
}
```

### 3. Enhanced WorkspaceModeService

**New Security Features**:

1. **Input Sanitization**
   - Workspace ID: Only alphanumeric, dash, underscore
   - Theme name: Only alphanumeric and dash
   - URL validation: Only HTTP/HTTPS protocols
   - Text sanitization: HTML encoding
   - Length limits: Prevent DoS attacks

2. **Mode Validation**
   - Validates against whitelist
   - Defaults to 'standalone' for invalid values
   - Logs warnings for invalid inputs

3. **URL Cleanup**
   - Clears sensitive parameters after reading
   - Removes from browser history
   - Prevents token exposure

### 4. Enhanced PulsarIntegrationBootstrap

**New Features**:

1. **SSO Integration**
   - Checks for SSO token during initialization
   - Validates authentication before applying customizations
   - Handles SSO failures gracefully

2. **Error Handling**
   - Displays user-friendly error messages
   - Logs detailed errors for debugging
   - Auto-dismisses error notifications

3. **Mode Application**
   - Sets data-mode attribute on body
   - Enables CSS-based hiding/showing

## JWT Token Specification

### Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "iss": "pulsar",
    "sub": "user123",
    "aud": "pulseql",
    "exp": 1702660800,
    "iat": 1702657200,
    "nbf": 1702657200,
    "jti": "unique-token-id-123",
    "user": {
      "userId": "user123",
      "displayName": "John Doe",
      "email": "john.doe@example.com",
      "authRole": "analyst"
    },
    "permissions": [
      "query.execute",
      "data.view",
      "data.export",
      "connection.view"
    ],
    "teams": [
      {
        "teamId": "analytics",
        "teamName": "Analytics Team",
        "role": "member"
      }
    ],
    "nonce": "random-entropy"
  }
}
```

### Required Claims

| Claim | Type | Description | Required |
|-------|------|-------------|----------|
| `iss` | string | Issuer (must be "pulsar") | Yes |
| `sub` | string | Subject (user ID) | Yes |
| `aud` | string | Audience (must be "pulseql") | Yes |
| `exp` | number | Expiration time (Unix timestamp) | Yes |
| `iat` | number | Issued at (Unix timestamp) | Yes |
| `nbf` | number | Not before (Unix timestamp) | Yes |
| `jti` | string | JWT ID (unique identifier) | Yes |
| `user` | object | User information | Yes |
| `permissions` | array | User permissions | Yes |
| `teams` | array | Team memberships | No |
| `nonce` | string | Random entropy | No |

## Security Considerations

### Client-Side Validation

⚠️ **Important**: Client-side validation is NOT sufficient for security. It only provides:
- Quick feedback to users
- Input validation
- Basic sanity checks

### Server-Side Requirements

✅ **Required for Production**:

1. **Signature Verification**
   - Verify JWT signature with shared secret
   - Use strong algorithm (RS256 or HS256 with 256-bit key)
   - Reject tampered tokens

2. **Token Blacklist**
   - Maintain list of revoked token IDs (jti)
   - Check against blacklist on each validation
   - Expire blacklist entries after token expiration

3. **User Validation**
   - Verify user exists in database
   - Check user is active/enabled
   - Validate user permissions match token

4. **Rate Limiting**
   - Limit SSO authentication attempts
   - Prevent brute force attacks
   - Monitor for suspicious patterns

### Best Practices

1. **Token Lifetime**
   - Keep tokens short-lived (5-15 minutes)
   - Implement refresh mechanism
   - Use refresh tokens for longer sessions

2. **Token Storage**
   - **Current**: sessionStorage (not encrypted)
   - **Production**: HttpOnly secure cookies or server-side only
   - Never store in localStorage (XSS risk)

3. **Token Transmission**
   - Use HTTPS only
   - Clear from URL immediately
   - Don't log tokens

4. **Error Handling**
   - Don't expose internal details
   - Log security events
   - Monitor failed attempts

## Integration Steps

### Pulsar Side (Token Generation)

1. **Install JWT Library**
   ```java
   // Maven dependency
   <dependency>
       <groupId>com.auth0</groupId>
       <artifactId>java-jwt</artifactId>
       <version>4.4.0</version>
   </dependency>
   ```

2. **Create Token Generator**
   ```java
   public class PulsarSSOTokenGenerator {
       private final String secret = getSigningSecret(); // From Vault
       private final Algorithm algorithm = Algorithm.HMAC256(secret);
       
       public String generateToken(User user) {
           return JWT.create()
               .withIssuer("pulsar")
               .withAudience("pulseql")
               .withSubject(user.getId())
               .withJWTId(UUID.randomUUID().toString())
               .withExpiresAt(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
               .withIssuedAt(new Date())
               .withNotBefore(new Date())
               .withClaim("user", buildUserClaims(user))
               .withClaim("permissions", getUserPermissions(user))
               .sign(algorithm);
       }
   }
   ```

3. **Create SSO Endpoint**
   ```java
   @GetMapping("/sso/pulseql")
   public RedirectView redirectToPulseQL(@AuthenticationPrincipal User user) {
       String token = tokenGenerator.generateToken(user);
       String url = String.format(
           "https://pulseql.example.com/workspace?mode=pulsar&sso_token=%s",
           token
       );
       return new RedirectView(url);
   }
   ```

### PulseQL Side (Token Validation)

1. **Client-Side** (Already Implemented)
   - PulsarSSOService validates token structure
   - Checks claims and expiration
   - Extracts user information

2. **Server-Side** (To Be Implemented)
   ```java
   @PostMapping("/api/sso/validate")
   public ResponseEntity<UserSession> validateToken(@RequestBody TokenRequest request) {
       try {
           // Verify signature
           String secret = getSigningSecret();
           Algorithm algorithm = Algorithm.HMAC256(secret);
           JWTVerifier verifier = JWT.require(algorithm)
               .withIssuer("pulsar")
               .withAudience("pulseql")
               .build();
           
           DecodedJWT jwt = verifier.verify(request.getToken());
           
           // Check blacklist
           if (tokenBlacklist.contains(jwt.getId())) {
               return ResponseEntity.status(401).body(null);
           }
           
           // Validate user
           String userId = jwt.getSubject();
           User user = userRepository.findById(userId)
               .orElseThrow(() -> new SecurityException("User not found"));
           
           // Create session
           UserSession session = sessionService.createSession(user, jwt);
           
           return ResponseEntity.ok(session);
           
       } catch (JWTVerificationException e) {
           return ResponseEntity.status(401).body(null);
       }
   }
   ```

## Testing

### Unit Tests

```typescript
describe('PulsarSSOService', () => {
    it('should validate valid token', async () => {
        const validToken = createValidToken();
        const result = await ssoService.validateToken(validToken);
        expect(result.valid).toBe(true);
        expect(result.payload?.sub).toBe('user123');
    });
    
    it('should reject expired token', async () => {
        const expiredToken = createExpiredToken();
        const result = await ssoService.validateToken(expiredToken);
        expect(result.valid).toBe(false);
        expect(result.errorCode).toBe('EXPIRED');
    });
    
    it('should reject wrong issuer', async () => {
        const wrongIssuer = createTokenWithIssuer('wrong');
        const result = await ssoService.validateToken(wrongIssuer);
        expect(result.valid).toBe(false);
        expect(result.errorCode).toBe('INVALID_SIGNATURE');
    });
});

describe('PulsarPermissionService', () => {
    it('should check permissions correctly', () => {
        expect(permissionService.canExecuteQuery()).toBe(true);
        expect(permissionService.canDeleteSchema()).toBe(false);
    });
});
```

### Integration Tests

```typescript
describe('SSO Integration', () => {
    it('should authenticate with valid token', async () => {
        const token = generateValidToken();
        await navigateTo(`/workspace?mode=pulsar&sso_token=${token}`);
        
        expect(ssoService.isUserAuthenticated).toBe(true);
        expect(ssoService.userId).toBe('user123');
    });
    
    it('should show error for invalid token', async () => {
        await navigateTo('/workspace?mode=pulsar&sso_token=invalid');
        
        expect(ssoService.isUserAuthenticated).toBe(false);
        expect(ssoService.authError).toBeDefined();
    });
});
```

## Troubleshooting

### Common Issues

1. **Token Validation Fails**
   - Check token expiration
   - Verify issuer/audience match
   - Ensure shared secret matches

2. **Permissions Not Working**
   - Verify token contains permissions array
   - Check permission names match exactly
   - Ensure PulsarPermissionService is injected

3. **Token Not Cleared from URL**
   - Check browser history API support
   - Verify no JavaScript errors
   - Check console for warnings

### Debug Mode

Enable debug logging:
```typescript
// In browser console
localStorage.setItem('debug', 'pulsar:*');
```

## Monitoring

### Key Metrics

1. **Authentication Success Rate**
   - Target: > 99.5%
   - Alert if < 95%

2. **Token Validation Time**
   - Target: < 100ms (p95)
   - Alert if > 500ms

3. **Failed Authentication Attempts**
   - Monitor for suspicious patterns
   - Alert on 5+ failures from same IP

### Audit Logging

Log these events:
- SSO authentication attempts (success/failure)
- Permission checks
- Token refresh
- Logout events

## Future Enhancements

1. **Token Refresh**
   - Implement silent token refresh
   - Use refresh tokens for longer sessions

2. **Multi-Factor Authentication**
   - Add MFA for sensitive operations
   - Integrate with Pulsar MFA

3. **Session Management**
   - Server-side session storage
   - Session timeout warnings
   - Active session monitoring

4. **Advanced Permissions**
   - Resource-level permissions
   - Time-based permissions
   - Context-aware permissions

## References

- [JWT Best Practices (RFC 8725)](https://tools.ietf.org/html/rfc8725)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OAuth 2.0 Security Best Practices](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)

---

**Document Version**: 1.2  
**Last Updated**: December 13, 2025  
**Review Required**: Security team approval before production
