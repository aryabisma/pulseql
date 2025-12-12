# Security Considerations for Pulsar-PulseQL Integration

## Executive Summary

This document provides a comprehensive security analysis of the Pulsar-PulseQL integration, identifying potential security risks, mitigation strategies, and best practices to ensure a secure implementation.

## Security Architecture Overview

### Defense in Depth Strategy

```
┌─────────────────────────────────────────────────────┐
│              Layer 1: Network Security              │
│  - Firewall rules                                   │
│  - VPN/Private network                              │
│  - DDoS protection                                  │
└─────────────────────────────────────────────────────┘
                         │
┌─────────────────────────────────────────────────────┐
│         Layer 2: Application Security (Edge)        │
│  - WAF (Web Application Firewall)                   │
│  - SSL/TLS termination                              │
│  - Rate limiting                                    │
│  - CORS policy                                      │
└─────────────────────────────────────────────────────┘
                         │
┌─────────────────────────────────────────────────────┐
│       Layer 3: Authentication & Authorization       │
│  - SSO via JWT                                      │
│  - Token validation                                 │
│  - Permission checks                                │
│  - Session management                               │
└─────────────────────────────────────────────────────┘
                         │
┌─────────────────────────────────────────────────────┐
│         Layer 4: Application Security               │
│  - Input validation                                 │
│  - SQL injection prevention                         │
│  - XSS prevention                                   │
│  - CSRF protection                                  │
└─────────────────────────────────────────────────────┘
                         │
┌─────────────────────────────────────────────────────┐
│            Layer 5: Data Security                   │
│  - Encryption at rest                               │
│  - Encryption in transit                            │
│  - Credential encryption                            │
│  - Audit logging                                    │
└─────────────────────────────────────────────────────┘
```

## Authentication Security

### JWT Token Security

#### Token Generation (Pulsar)

**Security Requirements**:

1. **Strong Signing Algorithm**:
   ```java
   // Use RS256 (RSA with SHA-256) for production
   Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
   
   // Alternative: HS256 with strong secret (minimum 256 bits)
   String secret = generateSecureSecret(256); // 256-bit secret
   Algorithm algorithm = Algorithm.HMAC256(secret);
   ```

2. **Short Token Lifetime**:
   ```java
   // Maximum 5-15 minutes
   long expirationTime = System.currentTimeMillis() + (5 * 60 * 1000);
   
   jwt.withExpiresAt(new Date(expirationTime))
      .withIssuedAt(new Date())
      .withNotBefore(new Date());
   ```

3. **Token Audience Validation**:
   ```java
   jwt.withAudience("pulseql.example.com")
      .withIssuer("pulsar.example.com");
   ```

4. **Prevent Token Reuse**:
   ```java
   // Include unique JTI (JWT ID)
   String jti = UUID.randomUUID().toString();
   jwt.withJWTId(jti);
   
   // Track used tokens in cache
   tokenCache.put(jti, true, 15, TimeUnit.MINUTES);
   ```

**Complete Secure Token Generation**:

```java
public class SecureSSOTokenGenerator {
    
    private final String issuer = "pulsar.example.com";
    private final String audience = "pulseql.example.com";
    private final long tokenValidityMs = 5 * 60 * 1000; // 5 minutes
    private final Cache<String, Boolean> usedTokens;
    
    public String generateToken(User user) {
        try {
            // Generate unique token ID
            String jti = UUID.randomUUID().toString();
            
            // Get signing key from secure storage (Vault, KMS, etc.)
            String secret = getSigningSecret();
            Algorithm algorithm = Algorithm.HMAC256(secret);
            
            // Generate token
            String token = JWT.create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject(user.getId())
                .withJWTId(jti)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + tokenValidityMs))
                .withNotBefore(new Date())
                .withClaim("user", buildUserClaims(user))
                .withClaim("permissions", getUserPermissions(user))
                .withClaim("nonce", generateNonce()) // Additional entropy
                .sign(algorithm);
            
            // Log token generation (not the token itself!)
            auditLog.info("SSO token generated - userId: {}, jti: {}", 
                user.getId(), jti);
            
            return token;
            
        } catch (JWTCreationException e) {
            throw new SecurityException("Failed to generate SSO token", e);
        }
    }
    
    private String generateNonce() {
        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
    }
    
    private String getSigningSecret() {
        // Fetch from secure storage, not from config file!
        return vaultClient.getSecret("pulsar/sso-signing-key");
    }
}
```

#### Token Validation (PulseQL)

**Security Requirements**:

```java
public class SecureTokenValidator {
    
    private final String issuer = "pulsar.example.com";
    private final String audience = "pulseql.example.com";
    private final Cache<String, Boolean> usedTokens;
    
    public DecodedJWT validateToken(String token) throws SecurityException {
        try {
            // Get verification key
            String secret = getSigningSecret();
            Algorithm algorithm = Algorithm.HMAC256(secret);
            
            // Build verifier with strict validation
            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .acceptLeeway(0) // No clock skew tolerance
                .build();
            
            // Verify and decode
            DecodedJWT jwt = verifier.verify(token);
            
            // Check for token reuse
            String jti = jwt.getId();
            if (jti != null && usedTokens.getIfPresent(jti) != null) {
                auditLog.warn("Token replay attempt detected - jti: {}", jti);
                throw new SecurityException("Token has already been used");
            }
            
            // Mark token as used
            if (jti != null) {
                long expiresIn = jwt.getExpiresAt().getTime() - System.currentTimeMillis();
                usedTokens.put(jti, true, expiresIn, TimeUnit.MILLISECONDS);
            }
            
            // Additional validation
            validateTokenClaims(jwt);
            
            auditLog.info("Token validated successfully - sub: {}, jti: {}", 
                jwt.getSubject(), jti);
            
            return jwt;
            
        } catch (JWTVerificationException e) {
            auditLog.warn("Token validation failed: {}", e.getMessage());
            throw new SecurityException("Invalid SSO token", e);
        }
    }
    
    private void validateTokenClaims(DecodedJWT jwt) {
        // Ensure required claims are present
        if (jwt.getSubject() == null || jwt.getSubject().isEmpty()) {
            throw new SecurityException("Token missing subject claim");
        }
        
        // Validate custom claims
        Map<String, Object> userClaims = jwt.getClaim("user").asMap();
        if (userClaims == null || userClaims.isEmpty()) {
            throw new SecurityException("Token missing user claims");
        }
        
        // Check for suspicious patterns
        String userId = jwt.getSubject();
        if (!isValidUserId(userId)) {
            auditLog.warn("Suspicious user ID in token: {}", userId);
            throw new SecurityException("Invalid user ID format");
        }
    }
    
    private boolean isValidUserId(String userId) {
        // Validate user ID format (e.g., alphanumeric, length constraints)
        return userId.matches("^[a-zA-Z0-9_-]{3,50}$");
    }
}
```

### Token Transmission Security

**1. HTTPS Only**:
```java
// Enforce HTTPS in production
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.requiresChannel()
            .anyRequest()
            .requiresSecure(); // Force HTTPS
        
        return http.build();
    }
}
```

**2. Token Removal from URL**:
```typescript
// Remove token from URL after authentication
async function authenticateWithSSO(token: string) {
    try {
        // Authenticate
        await authService.authenticate(token);
        
        // Remove token from URL immediately
        const url = new URL(window.location.href);
        url.searchParams.delete('sso_token');
        
        // Replace URL without adding to history
        window.history.replaceState({}, '', url.toString());
        
    } catch (error) {
        handleAuthError(error);
    }
}
```

**3. No Token Logging**:
```java
// Never log tokens!
// ❌ BAD
logger.info("Received token: {}", token);

// ✅ GOOD
String tokenHash = DigestUtils.sha256Hex(token);
logger.info("Received token with hash: {}", tokenHash.substring(0, 8));
```

## Session Security

### Session Management

**Secure Session Configuration**:

```java
@Configuration
public class SessionConfig {
    
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        
        // Use secure cookies in production
        serializer.setUseSecureCookie(true);
        
        // HTTP-only to prevent XSS
        serializer.setUseHttpOnlyCookie(true);
        
        // SameSite to prevent CSRF
        serializer.setSameSite("Strict");
        
        // Reasonable cookie name
        serializer.setCookieName("PULSEQL_SESSION");
        
        // Limit cookie path
        serializer.setCookiePath("/");
        
        return serializer;
    }
    
    @Bean
    public SessionRepository<Session> sessionRepository() {
        // Use Redis for distributed sessions
        RedisIndexedSessionRepository repository = 
            new RedisIndexedSessionRepository(redisConnectionFactory);
        
        // Set session timeout (30 minutes)
        repository.setDefaultMaxInactiveInterval(Duration.ofMinutes(30));
        
        return repository;
    }
}
```

### Session Validation

**Periodic Validation**:

```java
@Scheduled(fixedDelay = 60000) // Every minute
public void validateActiveSessions() {
    List<WebSession> pulsarSessions = sessionManager.getSessionsByAuthProvider("pulsar-sso");
    
    for (WebSession session : pulsarSessions) {
        try {
            String pulsarSessionId = (String) session.getAttribute("pulsar_session_id");
            
            // Validate with Pulsar
            boolean valid = pulsarApiClient.validateSession(pulsarSessionId);
            
            if (!valid) {
                auditLog.warn("Pulsar session invalid, terminating PulseQL session: {}", 
                    session.getId());
                session.close();
            }
            
        } catch (Exception e) {
            logger.error("Session validation failed for session: {}", 
                session.getId(), e);
        }
    }
}
```

### Session Hijacking Prevention

**1. Session Fingerprinting**:

```java
public class SessionFingerprint {
    
    public String generateFingerprint(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();
        
        fingerprint.append(request.getRemoteAddr());
        fingerprint.append("|");
        fingerprint.append(request.getHeader("User-Agent"));
        fingerprint.append("|");
        fingerprint.append(request.getHeader("Accept-Language"));
        
        return DigestUtils.sha256Hex(fingerprint.toString());
    }
    
    public boolean validateFingerprint(
        HttpServletRequest request, 
        WebSession session
    ) {
        String expectedFingerprint = (String) session.getAttribute("fingerprint");
        String actualFingerprint = generateFingerprint(request);
        
        if (!actualFingerprint.equals(expectedFingerprint)) {
            auditLog.warn(
                "Session fingerprint mismatch - possible hijacking attempt. " +
                "Session: {}, Expected: {}, Actual: {}", 
                session.getId(), 
                expectedFingerprint, 
                actualFingerprint
            );
            return false;
        }
        
        return true;
    }
}
```

**2. Session Rotation**:

```java
// Rotate session ID after authentication
@Component
public class SessionRotationFilter implements Filter {
    
    @Override
    public void doFilter(
        ServletRequest request, 
        ServletResponse response, 
        FilterChain chain
    ) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        
        if (session != null && isNewlyAuthenticated(session)) {
            // Migrate to new session
            HttpSession newSession = httpRequest.getSession(true);
            copySessionAttributes(session, newSession);
            session.invalidate();
            
            auditLog.info("Session rotated after authentication");
        }
        
        chain.doFilter(request, response);
    }
}
```

## Authorization Security

### Permission Enforcement

**Backend Permission Checks**:

```java
@Aspect
@Component
public class PermissionEnforcementAspect {
    
    @Around("@annotation(requiresPermission)")
    public Object enforcePermission(
        ProceedingJoinPoint joinPoint,
        RequiresPermission requiresPermission
    ) throws Throwable {
        
        // Get current session
        WebSession session = getCurrentSession();
        
        if (session == null || !session.isAuthenticated()) {
            auditLog.warn("Unauthenticated access attempt to: {}", 
                joinPoint.getSignature());
            throw new UnauthorizedException("Authentication required");
        }
        
        // Check permission
        String permission = requiresPermission.value();
        boolean hasPermission = permissionProvider.hasPermission(
            session,
            session.getUserId(),
            permission
        );
        
        if (!hasPermission) {
            auditLog.warn(
                "Permission denied - user: {}, permission: {}, method: {}", 
                session.getUserId(),
                permission,
                joinPoint.getSignature()
            );
            throw new ForbiddenException("Permission denied: " + permission);
        }
        
        // Log successful permission check
        auditLog.debug("Permission granted - user: {}, permission: {}", 
            session.getUserId(), permission);
        
        return joinPoint.proceed();
    }
}
```

**Frontend Permission Enforcement**:

```typescript
// Permission-based action execution
export class SecureActionService {
  
  async executeAction(
    actionId: string,
    requiredPermission: string,
    action: () => Promise<void>
  ): Promise<void> {
    // Check permission
    if (!this.permissionService.hasPermission(requiredPermission)) {
      this.notificationService.showError(
        `Permission denied: ${requiredPermission}`
      );
      
      // Log attempt
      this.auditService.logUnauthorizedAction(
        actionId,
        requiredPermission
      );
      
      return;
    }
    
    try {
      // Execute action
      await action();
      
      // Log successful action
      this.auditService.logAction(actionId);
      
    } catch (error) {
      this.handleError(error);
    }
  }
}
```

### Privilege Escalation Prevention

**1. Immutable Permissions**:

```java
// Make permissions immutable after loading
public class ImmutablePermissionSet {
    
    private final Set<String> permissions;
    
    public ImmutablePermissionSet(Set<String> permissions) {
        this.permissions = Collections.unmodifiableSet(
            new HashSet<>(permissions)
        );
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    public Set<String> getPermissions() {
        // Return defensive copy
        return new HashSet<>(permissions);
    }
}
```

**2. Permission Re-validation**:

```java
// Re-validate permissions for critical operations
@RequiresPermission("admin.users.delete")
public void deleteUser(String userId) {
    // Double-check permission before critical operation
    if (!revalidatePermission("admin.users.delete")) {
        throw new ForbiddenException("Permission re-validation failed");
    }
    
    // Perform deletion
    userRepository.delete(userId);
    
    auditLog.info("User deleted: {}", userId);
}

private boolean revalidatePermission(String permission) {
    // Fetch fresh permissions from Pulsar
    UserPermissions freshPermissions = pulsarApiClient.getUserPermissions(
        getCurrentUserId()
    );
    
    return freshPermissions.getPermissions().contains(permission);
}
```

## Data Security

### Encryption

**1. Encryption at Rest**:

```java
@Configuration
public class DatabaseEncryptionConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Enable database encryption
        config.setJdbcUrl(
            "jdbc:postgresql://localhost:5432/pulseql?ssl=true&sslmode=require"
        );
        
        // Use encrypted credentials
        config.setUsername(decryptCredential("db.username"));
        config.setPassword(decryptCredential("db.password"));
        
        return new HikariDataSource(config);
    }
    
    private String decryptCredential(String key) {
        // Fetch encrypted value from config
        String encrypted = env.getProperty(key);
        
        // Decrypt using KMS or Vault
        return encryptionService.decrypt(encrypted);
    }
}
```

**2. Encryption in Transit**:

```java
// Enforce TLS for database connections
public class SecureDatabaseConnection {
    
    public Connection createConnection(DatabaseConfig config) {
        Properties props = new Properties();
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());
        
        // Enforce SSL/TLS
        props.setProperty("ssl", "true");
        props.setProperty("sslmode", "verify-full");
        
        // Certificate validation
        props.setProperty("sslrootcert", "/path/to/root.crt");
        props.setProperty("sslcert", "/path/to/client.crt");
        props.setProperty("sslkey", "/path/to/client.key");
        
        return DriverManager.getConnection(
            config.getJdbcUrl(),
            props
        );
    }
}
```

**3. Sensitive Data Handling**:

```java
@Entity
public class DatabaseConnection {
    
    @Id
    private String id;
    
    private String name;
    private String host;
    private int port;
    
    // Encrypt sensitive fields
    @Convert(converter = PasswordEncryptor.class)
    private String password;
    
    @Convert(converter = PasswordEncryptor.class)
    private String sshPassword;
}

@Converter
public class PasswordEncryptor implements AttributeConverter<String, String> {
    
    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        return encryptionService.encrypt(plaintext);
    }
    
    @Override
    public String convertToEntityAttribute(String encrypted) {
        if (encrypted == null) return null;
        return encryptionService.decrypt(encrypted);
    }
}
```

### SQL Injection Prevention

**1. Parameterized Queries**:

```java
// ✅ GOOD - Parameterized query
String query = "SELECT * FROM users WHERE username = ? AND status = ?";
PreparedStatement stmt = connection.prepareStatement(query);
stmt.setString(1, username);
stmt.setString(2, status);
ResultSet rs = stmt.executeQuery();

// ❌ BAD - String concatenation
String query = "SELECT * FROM users WHERE username = '" + username + "'";
```

**2. Input Validation**:

```java
public class SQLInputValidator {
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('.+(--|;|\\||\\*|%|@|#))|" +
        "(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|DECLARE)\\b)",
        Pattern.CASE_INSENSITIVE
    );
    
    public static boolean isSafe(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        
        return !SQL_INJECTION_PATTERN.matcher(input).find();
    }
    
    public static String sanitize(String input) {
        if (!isSafe(input)) {
            throw new SecurityException("Potentially malicious input detected");
        }
        return input;
    }
}
```

**3. Query Permissions**:

```java
// Restrict query types for Pulsar users
public class QueryPermissionValidator {
    
    private static final Set<String> ALLOWED_STATEMENTS = Set.of(
        "SELECT"
    );
    
    public void validateQuery(String query, WebSession session) {
        // For Pulsar users, only allow SELECT queries
        if (isPulsarUser(session)) {
            String normalizedQuery = query.trim().toUpperCase();
            
            boolean isAllowed = ALLOWED_STATEMENTS.stream()
                .anyMatch(normalizedQuery::startsWith);
            
            if (!isAllowed) {
                auditLog.warn(
                    "Blocked non-SELECT query from Pulsar user: {}", 
                    session.getUserId()
                );
                throw new ForbiddenException(
                    "Only SELECT queries are allowed for Pulsar users"
                );
            }
            
            // Additional validation for dangerous keywords
            if (containsDangerousKeywords(query)) {
                throw new ForbiddenException("Query contains restricted keywords");
            }
        }
    }
    
    private boolean containsDangerousKeywords(String query) {
        String upper = query.toUpperCase();
        return upper.contains("DROP") || 
               upper.contains("DELETE") ||
               upper.contains("TRUNCATE") ||
               upper.contains("ALTER") ||
               upper.contains("EXEC");
    }
}
```

## Cross-Site Scripting (XSS) Prevention

### Output Encoding

```typescript
// Sanitize user input before rendering
export class XSSProtection {
  
  static sanitizeHTML(html: string): string {
    // Use DOMPurify or similar library
    return DOMPurify.sanitize(html, {
      ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'code'],
      ALLOWED_ATTR: []
    });
  }
  
  static escapeHTML(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
  
  static sanitizeURL(url: string): string {
    // Only allow safe protocols
    const safeProtocols = ['http:', 'https:', 'mailto:'];
    
    try {
      const parsed = new URL(url);
      if (!safeProtocols.includes(parsed.protocol)) {
        throw new Error('Unsafe protocol');
      }
      return url;
    } catch {
      return '#';
    }
  }
}
```

### Content Security Policy

```nginx
# nginx configuration
add_header Content-Security-Policy "
    default-src 'self';
    script-src 'self' 'unsafe-inline' 'unsafe-eval';
    style-src 'self' 'unsafe-inline';
    img-src 'self' data: https:;
    font-src 'self' data:;
    connect-src 'self' https://pulsar.example.com;
    frame-ancestors 'self' https://pulsar.example.com;
    base-uri 'self';
    form-action 'self';
" always;
```

## CSRF Protection

### Implementation

```java
@Configuration
public class CSRFConfig {
    
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = 
            new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }
}
```

```typescript
// Frontend: Include CSRF token in requests
export class APIClient {
  
  private async getCSRFToken(): Promise<string> {
    const meta = document.querySelector('meta[name="csrf-token"]');
    return meta ? meta.getAttribute('content') : '';
  }
  
  async post(url: string, data: any): Promise<Response> {
    const csrfToken = await this.getCSRFToken();
    
    return fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      },
      body: JSON.stringify(data)
    });
  }
}
```

## Audit Logging

### Comprehensive Audit Trail

```java
@Component
public class AuditLogger {
    
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    
    public void logAuthentication(String userId, String sessionId, boolean success) {
        AuditEvent event = AuditEvent.builder()
            .eventType("AUTHENTICATION")
            .userId(userId)
            .sessionId(sessionId)
            .success(success)
            .timestamp(Instant.now())
            .ipAddress(getClientIP())
            .userAgent(getUserAgent())
            .build();
        
        auditLog.info(toJSON(event));
    }
    
    public void logPermissionCheck(
        String userId, 
        String permission, 
        boolean granted
    ) {
        AuditEvent event = AuditEvent.builder()
            .eventType("PERMISSION_CHECK")
            .userId(userId)
            .permission(permission)
            .granted(granted)
            .timestamp(Instant.now())
            .build();
        
        auditLog.info(toJSON(event));
    }
    
    public void logQueryExecution(
        String userId,
        String query,
        boolean success,
        long durationMs
    ) {
        AuditEvent event = AuditEvent.builder()
            .eventType("QUERY_EXECUTION")
            .userId(userId)
            .queryHash(hashQuery(query))  // Don't log actual query
            .success(success)
            .durationMs(durationMs)
            .timestamp(Instant.now())
            .build();
        
        auditLog.info(toJSON(event));
    }
    
    public void logDataExport(
        String userId,
        String connectionId,
        int rowCount,
        String format
    ) {
        AuditEvent event = AuditEvent.builder()
            .eventType("DATA_EXPORT")
            .userId(userId)
            .connectionId(connectionId)
            .rowCount(rowCount)
            .format(format)
            .timestamp(Instant.now())
            .build();
        
        auditLog.info(toJSON(event));
    }
    
    public void logSecurityEvent(
        String eventType,
        String description,
        Map<String, Object> details
    ) {
        AuditEvent event = AuditEvent.builder()
            .eventType(eventType)
            .description(description)
            .details(details)
            .timestamp(Instant.now())
            .build();
        
        auditLog.warn(toJSON(event));
    }
}
```

## Security Monitoring

### Intrusion Detection

```java
@Component
public class IntrusionDetectionService {
    
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    
    public void recordFailedAuthentication(String userId, String ipAddress) {
        String key = userId + ":" + ipAddress;
        
        int attempts = failedAttempts.computeIfAbsent(
            key, 
            k -> new AtomicInteger(0)
        ).incrementAndGet();
        
        if (attempts >= 5) {
            auditLogger.logSecurityEvent(
                "BRUTE_FORCE_DETECTED",
                "Multiple failed authentication attempts",
                Map.of(
                    "userId", userId,
                    "ipAddress", ipAddress,
                    "attempts", attempts
                )
            );
            
            // Block IP temporarily
            blockIP(ipAddress, Duration.ofHours(1));
        }
    }
    
    public void recordSuspiciousActivity(
        String userId,
        String activityType,
        Map<String, Object> details
    ) {
        auditLogger.logSecurityEvent(
            "SUSPICIOUS_ACTIVITY",
            activityType,
            Map.of(
                "userId", userId,
                "details", details
            )
        );
        
        // Alert security team
        alertSecurityTeam(userId, activityType, details);
    }
}
```

## Secret Management

### Using Vault for Secrets

```java
@Configuration
public class VaultConfig {
    
    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.create("vault.example.com", 8200);
        
        // Use AppRole authentication
        VaultTokenSupplier tokenSupplier = () -> {
            AppRoleAuthentication auth = new AppRoleAuthentication(
                AppRoleAuthenticationOptions.builder()
                    .roleId(getRoleId())
                    .secretId(getSecretId())
                    .build()
            );
            
            return auth.login().getToken();
        };
        
        return new VaultTemplate(endpoint, tokenSupplier);
    }
    
    @Bean
    public String getSSOSigningKey() {
        VaultResponse response = vaultTemplate.read("secret/pulsar/sso-signing-key");
        return (String) response.getData().get("value");
    }
}
```

## Security Checklist

### Pre-Deployment Security Review

- [ ] All secrets stored in Vault/KMS, not in config files
- [ ] JWT tokens use strong signing algorithm (RS256/HS256 with 256-bit key)
- [ ] Token expiration set to maximum 15 minutes
- [ ] Token replay prevention implemented
- [ ] HTTPS enforced for all endpoints
- [ ] Session cookies are Secure and HttpOnly
- [ ] CSRF protection enabled
- [ ] Content Security Policy configured
- [ ] SQL injection prevention verified
- [ ] XSS protection implemented
- [ ] Input validation on all user inputs
- [ ] Output encoding on all dynamic content
- [ ] Permission checks on all sensitive operations
- [ ] Audit logging for all security events
- [ ] Rate limiting configured
- [ ] Intrusion detection active
- [ ] Database encryption at rest
- [ ] TLS for all database connections
- [ ] Sensitive data encrypted in database
- [ ] Security headers configured
- [ ] Dependency vulnerability scanning
- [ ] Penetration testing completed
- [ ] Security audit by external team

## Incident Response Plan

### Security Incident Response

1. **Detection**:
   - Monitor security alerts
   - Review audit logs
   - Analyze anomalies

2. **Containment**:
   - Block malicious IPs
   - Revoke compromised tokens
   - Disable affected accounts
   - Isolate affected systems

3. **Eradication**:
   - Identify root cause
   - Remove malicious code
   - Patch vulnerabilities
   - Update security rules

4. **Recovery**:
   - Restore from backups
   - Verify system integrity
   - Re-enable services
   - Monitor for recurrence

5. **Post-Incident**:
   - Document incident
   - Update procedures
   - Conduct retrospective
   - Implement improvements

## Compliance Considerations

### GDPR Compliance

- User consent for data processing
- Data minimization
- Right to erasure
- Data portability
- Audit trail of data access

### SOC 2 Compliance

- Access controls
- Encryption
- Monitoring
- Incident response
- Change management

## Conclusion

Security is paramount in the Pulsar-PulseQL integration. This document outlines:

1. **Authentication Security**: Secure JWT implementation
2. **Authorization Security**: Robust permission enforcement
3. **Data Security**: Encryption and protection
4. **Application Security**: Protection against common vulnerabilities
5. **Monitoring**: Comprehensive audit logging
6. **Incident Response**: Procedures for security events

By following these security practices, the integration will maintain a strong security posture while providing seamless user experience.

**Remember**: Security is not a one-time effort but an ongoing process that requires continuous monitoring, testing, and improvement.
