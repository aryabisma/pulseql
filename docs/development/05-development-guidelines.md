# Development Guidelines

**Document Version**: 1.0  
**Last Updated**: December 15, 2025

---

## 1. Core Principles

### 1.1 No Shortcuts, No Suppressions

**CRITICAL RULE**: Never take shortcut solutions that suppress or disable functionality to solve issues. Always find the root cause and implement a permanent solution.

```typescript
// ❌ NEVER DO THIS
// @ts-ignore
const result = unsafeOperation();

// ❌ NEVER DO THIS
/* eslint-disable */
someProblematicCode();

// ❌ NEVER DO THIS
try {
  riskyOperation();
} catch (e) {
  // Silently ignore error
}

// ✅ DO THIS INSTEAD
// Understand why TypeScript is complaining and fix the types
const result: ExpectedType = safeOperation();

// ✅ DO THIS INSTEAD
// Fix the linting issue properly
refactoredCleanCode();

// ✅ DO THIS INSTEAD
// Handle errors appropriately
try {
  riskyOperation();
} catch (error) {
  logger.error('Operation failed', { error: String(error) });
  notifyUser('Operation failed. Please try again.');
  throw error; // Re-throw if it should bubble up
}
```

### 1.2 No Sleep Timers for Process Waiting

**CRITICAL RULE**: Never use sleep/delay timers to wait for running processes. Use polling, events, or proper async patterns.

```typescript
// ❌ NEVER DO THIS
async function waitForServer() {
  await new Promise(resolve => setTimeout(resolve, 5000));
  // Hope the server is ready now...
}

// ✅ DO THIS INSTEAD - Polling
async function waitForServer(
  url: string,
  maxAttempts = 30,
  intervalMs = 1000
): Promise<void> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    try {
      const response = await fetch(url);
      if (response.ok) {
        return; // Server is ready
      }
    } catch {
      // Server not ready yet, continue polling
    }
    await new Promise(resolve => setTimeout(resolve, intervalMs));
  }
  throw new Error(`Server at ${url} not ready after ${maxAttempts} attempts`);
}

// ✅ DO THIS INSTEAD - Event-based
class ProcessMonitor extends EventEmitter {
  start(process: ChildProcess): void {
    process.stdout?.on('data', (data) => {
      if (data.includes('Server started')) {
        this.emit('ready');
      }
    });
    process.on('exit', (code) => {
      this.emit('exit', code);
    });
  }
}

// Usage
const monitor = new ProcessMonitor();
monitor.start(serverProcess);
await new Promise((resolve, reject) => {
  monitor.once('ready', resolve);
  monitor.once('exit', (code) => reject(new Error(`Process exited with ${code}`)));
});
```

**Special Note for Chat Agent Sessions**: When GitHub Copilot chat agents run long processes (like builds), they must NOT send additional commands to the same terminal while the process is running. Doing so interrupts the running process.

```powershell
# ❌ FORBIDDEN - Chat agent sends sleep while build is running
# Terminal 1 state: build.bat is running...
# Chat agent sends: Start-Sleep 5
# Result: build.bat process is interrupted and fails!

# ✅ CORRECT - Chat agent approach
# 1. Start build in NEW terminal with isBackground=true
# 2. Monitor using get_terminal_output tool
# 3. Never send commands to terminal with active process
```

### 1.3 Clean Code - No Spaghetti

**CRITICAL RULE**: Avoid spaghetti code. Follow industry-standard patterns and practices.

```typescript
// ❌ BAD: Spaghetti code
function processUser(data: any) {
  if (data) {
    if (data.user) {
      if (data.user.permissions) {
        if (data.user.permissions.length > 0) {
          for (let i = 0; i < data.user.permissions.length; i++) {
            if (data.user.permissions[i].active) {
              // 6 levels of nesting - impossible to maintain
            }
          }
        }
      }
    }
  }
}

// ✅ GOOD: Clean, flat code
interface UserData {
  user?: {
    permissions?: Permission[];
  };
}

function processUser(data: UserData | null): void {
  const permissions = data?.user?.permissions ?? [];
  const activePermissions = permissions.filter(p => p.active);
  
  for (const permission of activePermissions) {
    processPermission(permission);
  }
}

function processPermission(permission: Permission): void {
  // Single responsibility - just process one permission
}
```

### 1.4 The 360° Review Before Completion

**CRITICAL RULE**: Before marking any task complete, review your code from all angles.

| Aspect | Questions to Ask |
|--------|------------------|
| **Reusability** | Can this be used elsewhere? Is it too specific? |
| **Readability** | Can a new developer understand this in 5 minutes? |
| **Security** | Are there any XSS, injection, or auth vulnerabilities? |
| **Performance** | What's the time/space complexity? Any bottlenecks? |
| **Maintainability** | Will this be easy to modify in 6 months? |
| **Testability** | Can this be unit tested? Are dependencies injectable? |

### 1.5 Keep Documentation Updated

**CRITICAL RULE**: Update all relevant documentation after completing tasks.

Documents to update:
- Sprint backlog (`/docs/PMP_integration/04-sprint-backlog-current.md`)
- Daily standup log (`/docs/PMP_integration/06-daily-standup-log.md`)
- Milestone tracker (if applicable)
- Technical documentation (if architecture changed)
- User documentation (if user-facing features added)

---

## 2. Code Organization Guidelines

### 2.1 Single Responsibility

Each class/function should have one reason to change.

```typescript
// ❌ BAD: Multiple responsibilities
class UserService {
  validateUser(user: User): boolean { /* ... */ }
  saveUser(user: User): void { /* ... */ }
  sendWelcomeEmail(user: User): void { /* ... */ }
  generatePDF(user: User): Buffer { /* ... */ }
  calculateMetrics(user: User): Metrics { /* ... */ }
}

// ✅ GOOD: Single responsibility
class UserValidator {
  validate(user: User): ValidationResult { /* ... */ }
}

class UserRepository {
  save(user: User): void { /* ... */ }
  find(id: string): User | null { /* ... */ }
}

class WelcomeEmailService {
  send(user: User): void { /* ... */ }
}
```

### 2.2 Dependency Injection

Use DI for testability and flexibility.

```typescript
// ❌ BAD: Hard-coded dependencies
class PulsarSSOService {
  private http = new HttpClient(); // Can't mock in tests
  
  async validateToken(token: string): Promise<boolean> {
    return this.http.post('/validate', { token });
  }
}

// ✅ GOOD: Injected dependencies
@injectable()
class PulsarSSOService {
  constructor(
    private readonly http: HttpClient,
    private readonly logger: Logger,
  ) {}
  
  async validateToken(token: string): Promise<boolean> {
    return this.http.post('/validate', { token });
  }
}

// In tests, easily mock dependencies
const mockHttp = { post: jest.fn() };
const service = new PulsarSSOService(mockHttp, mockLogger);
```

### 2.3 Error Handling Patterns

```typescript
// Define specific error types
class AuthenticationError extends Error {
  constructor(
    message: string,
    public readonly code: AuthErrorCode,
    public readonly details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = 'AuthenticationError';
  }
}

enum AuthErrorCode {
  TOKEN_EXPIRED = 'TOKEN_EXPIRED',
  TOKEN_INVALID = 'TOKEN_INVALID',
  USER_NOT_FOUND = 'USER_NOT_FOUND',
  PERMISSION_DENIED = 'PERMISSION_DENIED',
}

// Use in code
async function authenticate(token: string): Promise<User> {
  const decoded = decodeToken(token);
  
  if (isExpired(decoded)) {
    throw new AuthenticationError(
      'Token has expired',
      AuthErrorCode.TOKEN_EXPIRED,
      { expiredAt: decoded.exp }
    );
  }
  
  const user = await findUser(decoded.sub);
  if (!user) {
    throw new AuthenticationError(
      'User not found',
      AuthErrorCode.USER_NOT_FOUND,
      { userId: decoded.sub }
    );
  }
  
  return user;
}

// Handle in caller
try {
  const user = await authenticate(token);
} catch (error) {
  if (error instanceof AuthenticationError) {
    switch (error.code) {
      case AuthErrorCode.TOKEN_EXPIRED:
        redirectToLogin('Your session has expired');
        break;
      case AuthErrorCode.PERMISSION_DENIED:
        showAccessDenied();
        break;
      default:
        showGenericError();
    }
    logger.warn('Authentication failed', { code: error.code });
  } else {
    logger.error('Unexpected error during authentication', { error });
    throw error;
  }
}
```

---

## 3. Security Guidelines

### 3.1 Input Validation

Always validate and sanitize all external input.

```typescript
// Validation utilities
const validators = {
  // Alphanumeric with limited length
  workspaceId: (value: string): string | null => {
    if (!/^[a-zA-Z0-9_-]{1,64}$/.test(value)) {
      return null;
    }
    return value;
  },
  
  // URL with protocol validation
  url: (value: string): string | null => {
    try {
      const url = new URL(value);
      if (!['http:', 'https:'].includes(url.protocol)) {
        return null;
      }
      return url.toString();
    } catch {
      return null;
    }
  },
  
  // Text with HTML encoding
  text: (value: string, maxLength = 1000): string => {
    return value
      .substring(0, maxLength)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;');
  },
  
  // Email validation
  email: (value: string): string | null => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value) || value.length > 254) {
      return null;
    }
    return value.toLowerCase();
  }
};
```

### 3.2 Secure Defaults

```typescript
// ✅ Cookies with secure defaults
const cookieOptions = {
  httpOnly: true,       // No JavaScript access
  secure: true,         // HTTPS only
  sameSite: 'strict',   // CSRF protection
  maxAge: 3600000,      // 1 hour
  path: '/',
};

// ✅ CORS with specific origins
const corsOptions = {
  origin: ['https://pulsar.example.com'],  // Not '*'
  credentials: true,
  methods: ['GET', 'POST'],
  allowedHeaders: ['Content-Type', 'Authorization'],
};

// ✅ Rate limiting
const rateLimitOptions = {
  windowMs: 60 * 1000,  // 1 minute
  max: 100,             // 100 requests per minute
  message: 'Too many requests, please try again later',
};
```

### 3.3 Secrets Management

```typescript
// ❌ NEVER commit secrets
const JWT_SECRET = 'my-super-secret-key';  // NEVER

// ✅ Use environment variables
const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  throw new Error('JWT_SECRET environment variable is required');
}

// ✅ Validate secrets exist at startup
function validateEnvironment(): void {
  const required = [
    'JWT_SECRET',
    'DATABASE_URL',
    'PULSAR_API_KEY',
  ];
  
  const missing = required.filter(key => !process.env[key]);
  
  if (missing.length > 0) {
    throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
  }
}
```

---

## 4. Performance Guidelines

### 4.1 Avoid N+1 Queries

```typescript
// ❌ BAD: N+1 queries
async function getUsers(): Promise<UserWithPermissions[]> {
  const users = await db.query('SELECT * FROM users');
  
  for (const user of users) {
    // This runs N additional queries!
    user.permissions = await db.query(
      'SELECT * FROM permissions WHERE user_id = ?',
      [user.id]
    );
  }
  
  return users;
}

// ✅ GOOD: Single query with JOIN
async function getUsers(): Promise<UserWithPermissions[]> {
  const results = await db.query(`
    SELECT u.*, p.permission_name
    FROM users u
    LEFT JOIN permissions p ON u.id = p.user_id
  `);
  
  return aggregateResults(results);
}
```

### 4.2 Memoization

```typescript
// ✅ Cache expensive computations
class PermissionService {
  private cache = new Map<string, Permission[]>();
  private cacheExpiry = new Map<string, number>();
  
  async getPermissions(userId: string): Promise<Permission[]> {
    const cached = this.getFromCache(userId);
    if (cached) {
      return cached;
    }
    
    const permissions = await this.fetchPermissions(userId);
    this.setCache(userId, permissions);
    return permissions;
  }
  
  private getFromCache(userId: string): Permission[] | null {
    const expiry = this.cacheExpiry.get(userId);
    if (!expiry || Date.now() > expiry) {
      this.cache.delete(userId);
      this.cacheExpiry.delete(userId);
      return null;
    }
    return this.cache.get(userId) ?? null;
  }
  
  private setCache(userId: string, permissions: Permission[]): void {
    this.cache.set(userId, permissions);
    this.cacheExpiry.set(userId, Date.now() + 5 * 60 * 1000); // 5 min TTL
  }
}
```

### 4.3 Lazy Loading

```typescript
// ✅ Load only when needed
class QueryEditor {
  private _autocomplete: AutocompleteEngine | null = null;
  
  get autocomplete(): AutocompleteEngine {
    if (!this._autocomplete) {
      // Initialize only on first use
      this._autocomplete = new AutocompleteEngine();
    }
    return this._autocomplete;
  }
}
```

---

## 5. Testing Guidelines

### 5.1 Test Structure

```typescript
describe('PulsarSSOService', () => {
  // Setup
  let service: PulsarSSOService;
  let mockHttp: jest.Mocked<HttpClient>;
  
  beforeEach(() => {
    mockHttp = createMockHttpClient();
    service = new PulsarSSOService(mockHttp);
  });
  
  afterEach(() => {
    jest.clearAllMocks();
  });
  
  // Group related tests
  describe('validateToken', () => {
    it('should return valid result for correct token', async () => {
      // Arrange
      const token = createValidToken();
      mockHttp.post.mockResolvedValue({ isValid: true });
      
      // Act
      const result = await service.validateToken(token);
      
      // Assert
      expect(result.isValid).toBe(true);
      expect(mockHttp.post).toHaveBeenCalledWith('/validate', { token });
    });
    
    it('should return invalid for expired token', async () => {
      // Arrange
      const token = createExpiredToken();
      
      // Act
      const result = await service.validateToken(token);
      
      // Assert
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('TOKEN_EXPIRED');
    });
    
    it('should throw on network error', async () => {
      // Arrange
      const token = createValidToken();
      mockHttp.post.mockRejectedValue(new Error('Network error'));
      
      // Act & Assert
      await expect(service.validateToken(token))
        .rejects
        .toThrow('Network error');
    });
  });
});
```

### 5.2 Test Coverage Requirements

| Code Type | Minimum Coverage |
|-----------|------------------|
| Services | 80% |
| Utilities | 90% |
| Components | 70% |
| Security-related | 100% |

### 5.3 What to Test

**Always Test**:
- Happy path
- Error conditions
- Edge cases (empty, null, max values)
- Security validations
- Permission checks

**Don't Test**:
- Framework code
- Simple getters/setters
- Third-party libraries

---

## 6. Code Review Guidelines

### 6.1 Reviewer Responsibilities

1. **Correctness**: Does the code do what it's supposed to?
2. **Security**: Are there any vulnerabilities?
3. **Performance**: Any obvious performance issues?
4. **Maintainability**: Will this be easy to maintain?
5. **Tests**: Are tests adequate and meaningful?
6. **Documentation**: Are public APIs documented?

### 6.2 Review Checklist

```markdown
## Code Review Checklist

### Logic
- [ ] Code implements the requirements correctly
- [ ] Edge cases are handled
- [ ] Error handling is appropriate

### Security
- [ ] Input is validated and sanitized
- [ ] No hardcoded secrets
- [ ] Authentication/authorization checked

### Quality
- [ ] No code duplication
- [ ] Functions are small and focused
- [ ] Variable/function names are clear

### Testing
- [ ] Unit tests cover main scenarios
- [ ] Tests are meaningful, not just for coverage

### Documentation
- [ ] Public APIs have JSDoc/JavaDoc
- [ ] Complex logic is commented
- [ ] README updated if needed
```

---

## 7. Common Patterns

### 7.1 Service Pattern

```typescript
@injectable()
export class UserService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly permissionService: PermissionService,
    private readonly logger: Logger,
  ) {}
  
  async getUser(id: string): Promise<User> {
    this.logger.debug('Fetching user', { id });
    
    const user = await this.userRepository.findById(id);
    if (!user) {
      throw new NotFoundError(`User ${id} not found`);
    }
    
    user.permissions = await this.permissionService.getForUser(id);
    
    return user;
  }
}
```

### 7.2 Repository Pattern

```typescript
interface UserRepository {
  findById(id: string): Promise<User | null>;
  findAll(filter: UserFilter): Promise<User[]>;
  save(user: User): Promise<void>;
  delete(id: string): Promise<void>;
}

class PostgresUserRepository implements UserRepository {
  constructor(private readonly db: Database) {}
  
  async findById(id: string): Promise<User | null> {
    const result = await this.db.query(
      'SELECT * FROM users WHERE id = $1',
      [id]
    );
    return result.rows[0] ?? null;
  }
  
  // ... other methods
}
```

### 7.3 Factory Pattern

```typescript
interface AuthProviderFactory {
  create(type: AuthType): AuthProvider;
}

class AuthProviderFactoryImpl implements AuthProviderFactory {
  constructor(private readonly container: DIContainer) {}
  
  create(type: AuthType): AuthProvider {
    switch (type) {
      case AuthType.SSO:
        return this.container.resolve(SSOAuthProvider);
      case AuthType.LDAP:
        return this.container.resolve(LDAPAuthProvider);
      case AuthType.LOCAL:
        return this.container.resolve(LocalAuthProvider);
      default:
        throw new Error(`Unknown auth type: ${type}`);
    }
  }
}
```

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-15 | Tech Lead | Initial guidelines |
