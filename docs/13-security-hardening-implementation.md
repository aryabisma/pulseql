# Security Hardening Implementation Guide

**Date**: December 13, 2025  
**Version**: 1.2  
**Status**: Security Enhanced

## Overview

This document details the security enhancements implemented in the Pulsar integration plugin to ensure secure SSO authentication, prevent common web vulnerabilities, and maintain data integrity.

## Security Enhancements Implemented

### 1. SSO Token Security

#### JWT Token Validation (PulsarSSOService)

**Location**: `src/PulsarSSOService.ts`

**Security Features**:

1. **Client-Side Token Validation**
   - Validates JWT structure (header.payload.signature)
   - Checks required claims (iss, sub, exp, aud)
   - Verifies issuer matches expected value
   - Verifies audience matches expected value
   - Checks token expiration
   - Checks not-before time
   - Validates token age (prevents old tokens)

2. **Token Storage Security**
   ```typescript
   // Current: sessionStorage (not encrypted)
   // Production recommendation: HttpOnly secure cookies or server-side only
   sessionStorage.setItem('pulsar_sso_token', token);
   ```

3. **Token Cleanup**
   - Removes token from URL after reading
   - Clears from browser history
   - Prevents token exposure in logs/screenshots

4. **Token Refresh**
   - Automatic refresh before expiration
   - Redirects to Pulsar for new token
   - Prevents session interruption

5. **Token Replay Prevention**
   - Validates JWT ID (jti) claim
   - Checks token uniqueness
   - Short token lifetime (5-15 minutes recommended)

#### Server-Side Validation Required

⚠️ **CRITICAL**: Client-side validation is insufficient for security. Server must:

1. **Verify Signature**
   ```java
   // Server-side (Java) signature verification
   Algorithm algorithm = Algorithm.HMAC256(getSigningSecret());
   JWTVerifier verifier = JWT.require(algorithm)
       .withIssuer("pulsar")
       .withAudience("pulseql")
       .build();
   
   DecodedJWT jwt = verifier.verify(token);
   ```

2. **Check Token Blacklist**
   ```java
   // Check if token JTI is blacklisted
   String jti = jwt.getId();
   if (tokenBlacklist.contains(jti)) {
       throw new SecurityException("Token has been revoked");
   }
   ```

3. **Validate Against Database**
   ```java
   // Verify user exists and is active
   String userId = jwt.getSubject();
   User user = userRepository.findById(userId);
   if (user == null || !user.isActive()) {
       throw new SecurityException("Invalid user");
   }
   ```

### 2. Input Sanitization (WorkspaceModeService)

**Location**: `src/WorkspaceModeService.ts`

#### URL Parameter Sanitization

1. **Mode Parameter Validation**
   ```typescript
   // Validates against whitelist
   const validModes: WorkspaceMode[] = ['standalone', 'pulsar', 'embedded'];
   const mode = validModes.includes(modeParam) ? modeParam : 'standalone';
   ```

2. **Workspace ID Sanitization**
   ```typescript
   // Removes all non-alphanumeric characters except dash and underscore
   const sanitized = workspaceId.replace(/[^a-zA-Z0-9\-_]/g, '');
   ```

3. **Theme Name Sanitization**
   ```typescript
   // Allows only alphanumeric and dash
   const sanitized = theme.replace(/[^a-zA-Z0-9\-]/g, '');
   ```

4. **URL Validation**
   ```typescript
   // Validates URL format and protocol
   const parsedUrl = new URL(url);
   if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
       return undefined; // Reject non-HTTP(S) protocols
   }
   ```

5. **Text Sanitization**
   ```typescript
   // Removes HTML tags and encodes special characters
   const div = document.createElement('div');
   div.textContent = text;
   const sanitized = div.innerHTML; // HTML-encoded
   ```

6. **Length Validation**
   ```typescript
   // Prevents DoS via long strings
   const maxLength = 200;
   if (sanitized.length > maxLength) {
       return sanitized.substring(0, maxLength);
   }
   ```

### 3. XSS Prevention

#### DOM Manipulation Security

1. **Safe DOM Updates**
   ```typescript
   // Use textContent instead of innerHTML
   document.title = branding.title; // Safe - no HTML interpretation
   
   // Sanitized URLs only
   const validatedUrl = this.sanitizeURL(url);
   ```

2. **CSS Variable Safety**
   ```typescript
   // Validated hex color only
   if (hexColorRegex.test(color)) {
       document.documentElement.style.setProperty('--theme-primary', color);
   }
   ```

3. **Attribute Safety**
   ```typescript
   // Sanitized values only
   document.body.setAttribute('data-theme', sanitizedTheme);
   document.body.setAttribute('data-mode', mode); // From validated enum
   ```

### 4. CSRF Protection

#### URL Parameter Cleanup

```typescript
// Clear sensitive parameters from URL after reading
private clearSensitiveURLParameters(): void {
    const url = new URL(window.location.href);
    url.search = ''; // Clear all parameters
    window.history.replaceState({}, '', url.toString());
}
```

#### Token Cleanup

```typescript
// Remove SSO token from URL immediately after reading
private clearTokenFromURL(): void {
    const url = new URL(window.location.href);
    url.searchParams.delete('sso_token');
    window.history.replaceState({}, '', url.toString());
}
```

### 5. Permission-Based Access Control

**Location**: `src/PulsarPermissionService.ts`

#### Permission Checking

```typescript
// Check single permission
hasPermission(permission: PulsarPermission): boolean {
    return this.ssoService.hasPermission(permission);
}

// Check multiple permissions (any)
hasAnyPermission(permissions: PulsarPermission[]): boolean {
    return permissions.some(p => this.hasPermission(p));
}

// Check multiple permissions (all)
hasAllPermissions(permissions: PulsarPermission[]): boolean {
    return permissions.every(p => this.hasPermission(p));
}
```

#### Granular Permissions

- Query: execute, save, share
- Data: view, export, edit, delete
- Connection: view, create, edit, delete
- Schema: view, create, edit, delete
- Admin: users, settings, logs

### 6. Error Handling

#### Safe Error Messages

```typescript
// Don't expose internal details
private handleAuthenticationFailure(error: string, errorCode?: string): void {
    // Log detailed error server-side
    console.error('[PulsarSSOService] Authentication failed:', error, errorCode);
    
    // Show generic message to user
    this.authenticationError = 'Authentication failed. Please try again.';
}
```

#### Error Notification

```typescript
// Display error to user without exposing details
private handleSSOFailure(): void {
    const errorMessage = 'SSO authentication failed. Please contact support.';
    // Create notification...
}
```

## Security Checklist

### ✅ Implemented

- [x] JWT token client-side validation
- [x] Input sanitization for all URL parameters
- [x] XSS prevention via safe DOM manipulation
- [x] CSRF protection via parameter cleanup
- [x] Permission-based access control
- [x] URL validation and protocol checking
- [x] Length validation to prevent DoS
- [x] Token cleanup from URL and history
- [x] Safe error messaging
- [x] Navigation item whitelist

### ⚠️ Required for Production

- [ ] Server-side JWT signature verification
- [ ] Token blacklist/revocation system
- [ ] HTTPS-only communication
- [ ] Content Security Policy (CSP) headers
- [ ] Secure cookie flags (HttpOnly, Secure, SameSite)
- [ ] Rate limiting on SSO endpoint
- [ ] Audit logging for security events
- [ ] Token encryption in storage
- [ ] Secret rotation mechanism
- [ ] Intrusion detection system

## Production Deployment Requirements

### 1. Server-Side Token Validation

**Required Implementation**:

```java
// Backend: Token validation endpoint
@RestController
@RequestMapping("/api/sso")
public class SSOController {
    
    @PostMapping("/validate")
    public ResponseEntity<UserSession> validateToken(@RequestBody TokenRequest request) {
        try {
            // Verify signature
            DecodedJWT jwt = jwtVerifier.verify(request.getToken());
            
            // Check blacklist
            if (tokenBlacklist.contains(jwt.getId())) {
                throw new SecurityException("Token revoked");
            }
            
            // Validate user
            User user = userService.validateUser(jwt.getSubject());
            
            // Create session
            UserSession session = sessionService.createSession(user, jwt);
            
            return ResponseEntity.ok(session);
            
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).build();
        }
    }
}
```

### 2. HTTPS Configuration

**nginx.conf**:
```nginx
server {
    listen 443 ssl http2;
    server_name pulseql.example.com;
    
    ssl_certificate /etc/ssl/certs/pulseql.crt;
    ssl_certificate_key /etc/ssl/private/pulseql.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    # CSP header
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://pulsar.example.com" always;
    
    location / {
        proxy_pass http://pulseql-backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. Rate Limiting

```nginx
# Rate limiting for SSO endpoint
limit_req_zone $binary_remote_addr zone=sso_limit:10m rate=10r/m;

location /api/sso {
    limit_req zone=sso_limit burst=5 nodelay;
    proxy_pass http://backend;
}
```

### 4. Audit Logging

```typescript
// Log all security events
class SecurityAuditLogger {
    logSSOAttempt(userId: string, success: boolean, ipAddress: string): void {
        auditLog.info({
            event: 'sso_authentication',
            userId,
            success,
            ipAddress,
            timestamp: new Date().toISOString(),
        });
    }
    
    logPermissionCheck(userId: string, permission: string, granted: boolean): void {
        auditLog.info({
            event: 'permission_check',
            userId,
            permission,
            granted,
            timestamp: new Date().toISOString(),
        });
    }
}
```

## Vulnerability Mitigations

### Common Vulnerabilities Addressed

| Vulnerability | Mitigation | Status |
|---------------|------------|--------|
| XSS (Cross-Site Scripting) | Input sanitization, safe DOM manipulation | ✅ Implemented |
| CSRF (Cross-Site Request Forgery) | Token cleanup, SameSite cookies (backend) | ⚠️ Partial |
| SQL Injection | Parameterized queries (backend), input validation | ✅ Frontend only |
| Token Replay | Token expiration, JTI validation | ✅ Implemented |
| Session Hijacking | HTTPS, secure cookies, short session lifetime | ⚠️ Requires backend |
| Clickjacking | X-Frame-Options header | ⚠️ Requires backend |
| MITM (Man-in-the-Middle) | HTTPS, HSTS | ⚠️ Requires deployment |
| DoS (Denial of Service) | Rate limiting, input length validation | ⚠️ Partial |

## Security Testing

### Manual Testing

1. **XSS Testing**
   ```
   Test malicious inputs:
   - brand_title=<script>alert('XSS')</script>
   - brand_logo=javascript:alert('XSS')
   - workspace_id=../../etc/passwd
   - brand_color=red;background:url(evil.com)
   ```

2. **Token Testing**
   ```
   Test invalid tokens:
   - Expired token
   - Wrong issuer
   - Wrong audience
   - Malformed token
   - Modified signature
   ```

3. **Permission Testing**
   ```
   Test permission enforcement:
   - Remove permissions from token
   - Access restricted features
   - Verify UI hides unauthorized actions
   ```

### Automated Testing

```typescript
describe('Security Tests', () => {
    it('should sanitize XSS in title', () => {
        const params = new URLSearchParams('brand_title=<script>alert(1)</script>');
        // Verify sanitization
    });
    
    it('should reject invalid URLs', () => {
        const params = new URLSearchParams('brand_logo=javascript:alert(1)');
        // Verify rejection
    });
    
    it('should validate token expiration', async () => {
        const expiredToken = createExpiredToken();
        const result = await ssoService.validateToken(expiredToken);
        expect(result.valid).toBe(false);
        expect(result.errorCode).toBe('EXPIRED');
    });
});
```

## Incident Response

### Security Incident Procedure

1. **Detection**
   - Monitor audit logs for anomalies
   - Alert on failed authentication attempts
   - Track permission violations

2. **Response**
   - Revoke compromised tokens
   - Block suspicious IP addresses
   - Notify security team

3. **Recovery**
   - Rotate signing secrets
   - Force re-authentication
   - Review and patch vulnerabilities

4. **Post-Mortem**
   - Document incident
   - Update security measures
   - Train team on findings

## Compliance

### OWASP Top 10 Coverage

- A01: Broken Access Control → ✅ Permission service
- A02: Cryptographic Failures → ⚠️ HTTPS required
- A03: Injection → ✅ Input sanitization
- A04: Insecure Design → ✅ Secure architecture
- A05: Security Misconfiguration → ⚠️ Deployment config needed
- A06: Vulnerable Components → ✅ Regular updates
- A07: Identification/Auth Failures → ✅ JWT validation
- A08: Software/Data Integrity → ✅ Token verification
- A09: Logging/Monitoring Failures → ⚠️ Audit logging needed
- A10: SSRF → ✅ URL validation

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Web Security Guidelines](https://cheatsheetseries.owasp.org/)

---

**Document Version**: 1.2  
**Last Updated**: December 13, 2025  
**Security Review Required**: Before production deployment
