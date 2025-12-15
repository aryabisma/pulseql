# Integration Architecture

**Document Version**: 1.0  
**Last Updated**: December 15, 2025  
**Author**: Principal Architect

---

## 1. High-Level Architecture

### 1.1 System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              End Users                                   │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Load Balancer (nginx)                           │
│                    - SSL Termination                                     │
│                    - Rate Limiting                                       │
│                    - Request Routing                                     │
└────────────┬──────────────────────────────────┬─────────────────────────┘
             │                                  │
             ▼                                  ▼
┌────────────────────────┐          ┌────────────────────────┐
│       Pulsar           │          │       PulseQL          │
│    Application         │◄────────►│    Application         │
│                        │   SSO    │                        │
│  ┌──────────────────┐  │  & API   │  ┌──────────────────┐  │
│  │  SSO Service     │  │          │  │  SSO Provider    │  │
│  │  - JWT Gen       │──┼─────────►│  │  - JWT Validate  │  │
│  │  - User Context  │  │          │  │  - Session Mgmt  │  │
│  └──────────────────┘  │          │  └──────────────────┘  │
│                        │          │                        │
│  ┌──────────────────┐  │          │  ┌──────────────────┐  │
│  │  Permission API  │◄─┼──────────┤  │  Permission Svc  │  │
│  │  - User Perms    │  │          │  │  - RBAC Enforce  │  │
│  └──────────────────┘  │          │  └──────────────────┘  │
│                        │          │                        │
│  ┌──────────────────┐  │          │  ┌──────────────────┐  │
│  │  Activity API    │◄─┼──────────┤  │  Activity Track  │  │
│  │  - Session Valid │  │          │  │  - User Monitor  │  │
│  └──────────────────┘  │          │  └──────────────────┘  │
│                        │          │                        │
└────────────┬───────────┘          └────────────┬───────────┘
             │                                   │
             ▼                                   ▼
┌────────────────────────┐          ┌────────────────────────┐
│     Pulsar DB          │          │     Target Databases   │
│     (PostgreSQL)       │          │  (Various RDBMS)       │
└────────────────────────┘          └────────────────────────┘
```

### 1.2 Component Interactions

```
┌──────────────────────────────────────────────────────────────────────┐
│                        SSO Authentication Flow                        │
└──────────────────────────────────────────────────────────────────────┘

User                Pulsar              PulseQL             Database
 │                    │                    │                    │
 │  1. Click Query    │                    │                    │
 │    Workspace       │                    │                    │
 │───────────────────>│                    │                    │
 │                    │                    │                    │
 │                    │  2. Generate JWT   │                    │
 │                    │    with claims     │                    │
 │                    │                    │                    │
 │  3. Redirect with  │                    │                    │
 │     JWT token      │                    │                    │
 │<───────────────────│                    │                    │
 │                    │                    │                    │
 │  4. Access PulseQL │                    │                    │
 │     with token     │                    │                    │
 │──────────────────────────────────────-->│                    │
 │                    │                    │                    │
 │                    │  5. Validate JWT   │                    │
 │                    │<───────────────────│                    │
 │                    │                    │                    │
 │                    │  6. Confirm valid  │                    │
 │                    │───────────────────>│                    │
 │                    │                    │                    │
 │                    │                    │  7. Create session │
 │                    │                    │     with perms     │
 │                    │                    │                    │
 │  8. Load Workspace │                    │                    │
 │<────────────────────────────────────────│                    │
 │                    │                    │                    │
```

---

## 2. Component Architecture

### 2.1 PulseQL Plugin Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    plugin-pulsar-integration                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │ Workspace Mode  │  │   Pulsar SSO    │  │    Pulsar       │     │
│  │    Service      │  │    Service      │  │  Permission Svc │     │
│  │                 │  │                 │  │                 │     │
│  │ - URL Parsing   │  │ - JWT Validate  │  │ - 15+ Perms     │     │
│  │ - Mode Config   │  │ - Token Refresh │  │ - Role Mapping  │     │
│  │ - Sanitization  │  │ - Session Mgmt  │  │ - UI Filtering  │     │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘     │
│           │                    │                    │               │
│           └────────────────────┼────────────────────┘               │
│                                │                                     │
│                                ▼                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                 PulsarIntegrationBootstrap                   │   │
│  │                                                              │   │
│  │  - Initialize from URL on startup                           │   │
│  │  - Apply custom branding                                    │   │
│  │  - Trigger SSO authentication                              │   │
│  │  - Handle errors gracefully                                │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────┐  ┌─────────────────┐                          │
│  │  UI Components  │  │     Themes      │                          │
│  │                 │  │                 │                          │
│  │ - BackToPulsar  │  │ - pulsar-light  │                          │
│  │ - SSOError      │  │ - pulsar-dark   │                          │
│  │ - Header        │  │                 │                          │
│  └─────────────────┘  └─────────────────┘                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Backend Service Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                  io.cloudbeaver.service.auth.pulsar                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              PulsarSSOAuthProvider                           │   │
│  │  implements DBWAuthProvider                                  │   │
│  │                                                              │   │
│  │  + authenticate(PulsarSSOCredentials): DBWSession           │   │
│  │  + validateToken(String): TokenValidationResult             │   │
│  │  + refreshToken(String): String                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              PulsarPermissionProvider                        │   │
│  │  implements DBWPermissionProvider                           │   │
│  │                                                              │   │
│  │  + mapPulsarPermissions(List<String>): Set<Permission>      │   │
│  │  + checkPermission(User, Permission): boolean               │   │
│  │  + filterByPermission(Collection, Permission): Collection   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              SessionValidationService                        │   │
│  │                                                              │   │
│  │  + validateSession(String): SessionStatus                   │   │
│  │  + refreshSession(String): void                             │   │
│  │  + terminateSession(String): void                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Data Flow Architecture

### 3.1 JWT Token Structure

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
    "jti": "unique-token-id",
    "user": {
      "userId": "user123",
      "displayName": "John Doe",
      "email": "john.doe@example.com",
      "authRole": "ANALYST"
    },
    "permissions": [
      "query.execute",
      "data.view",
      "data.export",
      "sql.script.save"
    ],
    "teams": [
      {
        "teamId": "analytics",
        "teamName": "Analytics Team",
        "role": "member"
      }
    ],
    "metadata": {
      "department": "Analytics",
      "location": "US"
    }
  },
  "signature": "..."
}
```

### 3.2 Permission Mapping

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Permission Mapping Flow                         │
└─────────────────────────────────────────────────────────────────────┘

Pulsar Roles          Pulsar Permissions        PulseQL Permissions
    │                       │                          │
    ▼                       ▼                          ▼
┌─────────┐           ┌────────────────┐        ┌─────────────────┐
│ VIEWER  │──────────>│ report.view    │───────>│ connection.view │
│         │           │ report.execute │        │ sql.execute     │
│         │           └────────────────┘        │ data.view       │
└─────────┘                                     └─────────────────┘

┌─────────┐           ┌────────────────┐        ┌─────────────────┐
│ ANALYST │──────────>│ report.view    │───────>│ connection.view │
│         │           │ report.execute │        │ sql.execute     │
│         │           │ report.edit    │        │ data.view       │
│         │           │ data.export    │        │ data.export     │
│         │           └────────────────┘        │ sql.script.save │
└─────────┘                                     └─────────────────┘

┌─────────┐           ┌────────────────┐        ┌─────────────────┐
│ ADMIN   │──────────>│ admin.access   │───────>│ admin.*         │
│         │           │ (all perms)    │        │ (all perms)     │
│         │           └────────────────┘        └─────────────────┘
└─────────┘
```

---

## 4. Security Architecture

### 4.1 Security Layers

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Security Architecture                         │
└─────────────────────────────────────────────────────────────────────┘

Layer 1: Network Security
┌─────────────────────────────────────────────────────────────────────┐
│  - TLS 1.2+ encryption                                              │
│  - HTTPS enforced                                                   │
│  - Certificate pinning                                              │
│  - IP allowlisting (optional)                                       │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
Layer 2: Application Gateway
┌─────────────────────────────────────────────────────────────────────┐
│  - Rate limiting (100 req/min per IP)                              │
│  - Request validation                                               │
│  - DDoS protection                                                  │
│  - Security headers (CSP, HSTS, X-Frame-Options)                   │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
Layer 3: Authentication
┌─────────────────────────────────────────────────────────────────────┐
│  - JWT token validation                                             │
│  - Token expiration enforcement                                     │
│  - Token blacklist checking                                         │
│  - Signature verification                                           │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
Layer 4: Authorization
┌─────────────────────────────────────────────────────────────────────┐
│  - RBAC permission enforcement                                      │
│  - Resource-level access control                                    │
│  - Team-based permissions                                           │
│  - UI element filtering                                             │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
Layer 5: Data Protection
┌─────────────────────────────────────────────────────────────────────┐
│  - Input sanitization (XSS prevention)                             │
│  - SQL injection protection                                         │
│  - Sensitive data encryption                                        │
│  - Audit logging                                                    │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Token Security Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Token Lifecycle                               │
└─────────────────────────────────────────────────────────────────────┘

1. Token Generation (Pulsar)
   ┌──────────────────────────────────────────────────────────┐
   │  - Sign with HMAC-SHA256 using shared secret            │
   │  - Set short expiration (5-15 minutes)                  │
   │  - Include unique JTI for replay protection             │
   │  - Add all required claims                              │
   └──────────────────────────────────────────────────────────┘
                              │
                              ▼
2. Token Transmission
   ┌──────────────────────────────────────────────────────────┐
   │  - Pass via HTTPS only                                  │
   │  - URL parameter for initial auth                       │
   │  - Clear from URL immediately after reading             │
   │  - Store in memory (not localStorage)                   │
   └──────────────────────────────────────────────────────────┘
                              │
                              ▼
3. Token Validation (PulseQL)
   ┌──────────────────────────────────────────────────────────┐
   │  - Verify signature                                      │
   │  - Check expiration (exp)                               │
   │  - Validate issuer (iss = "pulsar")                     │
   │  - Validate audience (aud = "pulseql")                  │
   │  - Check not-before (nbf)                               │
   │  - Verify JTI not blacklisted                           │
   └──────────────────────────────────────────────────────────┘
                              │
                              ▼
4. Token Refresh
   ┌──────────────────────────────────────────────────────────┐
   │  - Automatic refresh before expiration                  │
   │  - Call Pulsar API for new token                        │
   │  - Invalidate old token                                 │
   │  - Update session with new token                        │
   └──────────────────────────────────────────────────────────┘
```

---

## 5. API Architecture

### 5.1 API Endpoints Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Pulsar APIs (for PulseQL)                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Authentication                                                      │
│  ├── POST /api/sso/validate-token    Validate JWT token             │
│  ├── POST /api/sso/refresh-token     Refresh expired token          │
│  └── POST /api/sso/revoke-token      Revoke/blacklist token         │
│                                                                      │
│  Session Management                                                  │
│  ├── POST /api/session/validate      Validate session active        │
│  └── GET  /api/session/{id}          Get session details            │
│                                                                      │
│  Permissions                                                         │
│  ├── GET  /api/user/{id}/permissions Get user permissions           │
│  └── GET  /api/teams/{id}/members    Get team members               │
│                                                                      │
│  Activity                                                            │
│  ├── POST /api/sso/activity          Report user activity           │
│  └── GET  /api/user/{id}/activity    Get activity history           │
│                                                                      │
│  Deep Linking                                                        │
│  └── POST /api/pulseql/generate-link Generate deep link             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        PulseQL APIs                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  GraphQL API (/api/gql)                                             │
│  ├── query userInfo                  Get current user info          │
│  ├── query connections               List available connections     │
│  ├── mutation sqlExecuteQuery        Execute SQL query              │
│  └── mutation saveScript             Save SQL script                │
│                                                                      │
│  REST API                                                            │
│  └── POST /api/webhook/permissions   Receive permission updates     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 6. Deployment Architecture

### 6.1 Production Deployment

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Production Infrastructure                        │
└─────────────────────────────────────────────────────────────────────┘

                         Internet
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    DNS / CDN (CloudFlare)                           │
│                    - DDoS protection                                │
│                    - SSL/TLS termination                            │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Load Balancer (nginx)                            │
│                    - Health checks                                   │
│                    - Rate limiting                                   │
│                    - Request routing                                 │
└──────────┬──────────────────────────────────────┬───────────────────┘
           │                                      │
           ▼                                      ▼
┌───────────────────────┐              ┌───────────────────────┐
│    Pulsar Cluster     │              │    PulseQL Cluster    │
│  ┌─────────────────┐  │              │  ┌─────────────────┐  │
│  │   Instance 1    │  │              │  │   Instance 1    │  │
│  └─────────────────┘  │              │  └─────────────────┘  │
│  ┌─────────────────┐  │              │  ┌─────────────────┐  │
│  │   Instance 2    │  │              │  │   Instance 2    │  │
│  └─────────────────┘  │              │  └─────────────────┘  │
└──────────┬────────────┘              └──────────┬────────────┘
           │                                      │
           ▼                                      ▼
┌───────────────────────┐              ┌───────────────────────┐
│    Pulsar DB (RDS)    │              │  Target Databases     │
│    - Primary          │              │  - PostgreSQL         │
│    - Replica          │              │  - MySQL              │
│                       │              │  - Oracle             │
└───────────────────────┘              └───────────────────────┘
```

### 6.2 Kubernetes Deployment (Alternative)

```yaml
# Simplified K8s Architecture
┌─────────────────────────────────────────────────────────────────────┐
│                        Kubernetes Cluster                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Namespace: pulseql-integration                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                                                              │   │
│  │  Deployments:                                               │   │
│  │  ├── pulsar-deployment (replicas: 2)                       │   │
│  │  └── pulseql-deployment (replicas: 2)                      │   │
│  │                                                              │   │
│  │  Services:                                                  │   │
│  │  ├── pulsar-service (ClusterIP)                            │   │
│  │  └── pulseql-service (ClusterIP)                           │   │
│  │                                                              │   │
│  │  Ingress:                                                   │   │
│  │  └── integration-ingress (nginx-ingress)                   │   │
│  │                                                              │   │
│  │  ConfigMaps:                                                │   │
│  │  ├── pulsar-config                                         │   │
│  │  └── pulseql-config                                        │   │
│  │                                                              │   │
│  │  Secrets:                                                   │   │
│  │  ├── sso-secret (JWT signing key)                          │   │
│  │  └── db-credentials                                        │   │
│  │                                                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. Technology Stack

### 7.1 Frontend Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 19.x | UI framework |
| TypeScript | 5.x | Type safety |
| MobX | 6.x | State management |
| Vite | 7.x | Build tool |
| SCSS | - | Styling |
| Vitest | - | Unit testing |

### 7.2 Backend Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 22 | Server runtime |
| OSGi | - | Module system |
| Jetty | - | Web server |
| GraphQL | - | API layer |
| JWT (jjwt) | 0.12.x | Token handling |
| Maven/Tycho | - | Build system |

### 7.3 Infrastructure

| Technology | Purpose |
|------------|---------|
| nginx | Load balancer, reverse proxy |
| PostgreSQL | Database |
| Redis | Session cache (optional) |
| Prometheus | Monitoring |
| Grafana | Dashboards |
| GitHub Actions | CI/CD |

---

## 8. Integration Points Summary

| Integration | Direction | Protocol | Data Format |
|-------------|-----------|----------|-------------|
| SSO Token | Pulsar → PulseQL | HTTPS | JWT |
| Token Validation | PulseQL → Pulsar | HTTPS | JSON |
| Session Validation | PulseQL → Pulsar | HTTPS | JSON |
| Permission Fetch | PulseQL → Pulsar | HTTPS | JSON |
| Activity Events | PulseQL → Pulsar | HTTPS | JSON |
| Permission Webhook | Pulsar → PulseQL | HTTPS | JSON |
| Deep Link Generation | Pulsar Internal | - | URL |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-15 | Principal Architect | Initial architecture |
