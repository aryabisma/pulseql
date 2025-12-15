# Coding Standards

**Document Version**: 1.0  
**Last Updated**: December 15, 2025

---

## 1. General Principles

### 1.1 Code Quality Principles

1. **Readability First**: Code is read more than written. Optimize for clarity.
2. **No Shortcuts**: Never suppress warnings or disable features to "fix" issues.
3. **DRY (Don't Repeat Yourself)**: Extract common logic into reusable functions.
4. **SOLID Principles**: Follow Single Responsibility, Open-Closed, etc.
5. **Security by Design**: Consider security implications in every decision.

### 1.2 The 360° Rule

Before marking any task complete, evaluate your code from all angles:

```
┌─────────────────────────────────────────────────────────────────┐
│                      360° Code Review                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐       │
│  │ Reusability │     │ Readability │     │  Security   │       │
│  │             │     │             │     │             │       │
│  │ - Modular?  │     │ - Clear?    │     │ - XSS safe? │       │
│  │ - Generic?  │     │ - Docs?     │     │ - Injection │       │
│  │ - DRY?      │     │ - Named?    │     │   safe?     │       │
│  └─────────────┘     └─────────────┘     └─────────────┘       │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             │                                    │
│                      ┌──────▼──────┐                            │
│                      │    CODE     │                            │
│                      └──────┬──────┘                            │
│                             │                                    │
│         ┌───────────────────┼───────────────────┐               │
│         │                   │                   │               │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐       │
│  │ Performance │     │Maintainable │     │  Testable   │       │
│  │             │     │             │     │             │       │
│  │ - O(n)?     │     │ - Simple?   │     │ - Unit      │       │
│  │ - Memory?   │     │ - Coupled?  │     │   testable? │       │
│  │ - Async?    │     │ - Extend?   │     │ - Mocked?   │       │
│  └─────────────┘     └─────────────┘     └─────────────┘       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. TypeScript/JavaScript Standards

### 2.1 File Organization

```typescript
// 1. Imports (grouped and ordered)
// External dependencies
import { injectable } from '@cloudbeaver/core-di';
import { observable, computed, action } from 'mobx';

// Internal dependencies (relative imports last)
import { WorkspaceModeService } from './WorkspaceModeService';
import type { PulsarUser } from './types';

// 2. Constants
const MAX_TOKEN_AGE = 5 * 60 * 1000; // 5 minutes

// 3. Interfaces/Types
interface TokenValidationResult {
  isValid: boolean;
  user?: PulsarUser;
  error?: string;
}

// 4. Class/Function implementation
@injectable()
export class PulsarSSOService {
  // 4a. Static members first
  static readonly TOKEN_PARAM = 'sso_token';

  // 4b. Instance fields
  @observable
  private _currentUser: PulsarUser | null = null;

  // 4c. Computed properties
  @computed
  get isAuthenticated(): boolean {
    return this._currentUser !== null;
  }

  // 4d. Constructor
  constructor(
    private readonly workspaceModeService: WorkspaceModeService,
  ) {}

  // 4e. Public methods
  @action
  async authenticate(token: string): Promise<TokenValidationResult> {
    // Implementation
  }

  // 4f. Private methods
  private validateTokenStructure(token: string): boolean {
    // Implementation
  }
}
```

### 2.2 Naming Conventions

```typescript
// ✅ Good naming
class WorkspaceModeService {}           // PascalCase for classes
interface TokenValidationResult {}       // PascalCase for interfaces
type PermissionLevel = 'read' | 'write'; // PascalCase for types
const MAX_RETRIES = 3;                   // SCREAMING_SNAKE for constants
const userPermissions = [];              // camelCase for variables
function validateToken() {}              // camelCase for functions

// ❌ Bad naming
class workspace_mode_service {}          // Wrong: use PascalCase
const MaxRetries = 3;                    // Wrong: constants use SCREAMING_SNAKE
function ValidateToken() {}              // Wrong: functions use camelCase
```

### 2.3 Type Safety

```typescript
// ✅ Always use explicit types for function parameters and returns
function processUser(user: PulsarUser): ProcessedUser {
  return { ...user, processed: true };
}

// ✅ Use type guards for runtime type checking
function isValidToken(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0;
}

// ✅ Prefer interfaces over types for objects (better extensibility)
interface User {
  id: string;
  name: string;
}

// ✅ Use strict null checks
function getUser(id: string): User | null {
  // Return null explicitly, never undefined for optional values
}

// ❌ Avoid 'any' - use 'unknown' and narrow the type
function processData(data: any) {}       // Bad
function processData(data: unknown) {    // Good
  if (typeof data === 'string') {
    // Now TypeScript knows data is string
  }
}
```

### 2.4 Error Handling

```typescript
// ✅ Create specific error classes
class SSOAuthenticationError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = 'SSOAuthenticationError';
  }
}

// ✅ Use try-catch with specific error handling
async function authenticateUser(token: string): Promise<User> {
  try {
    const result = await validateToken(token);
    if (!result.isValid) {
      throw new SSOAuthenticationError(
        'Token validation failed',
        'INVALID_TOKEN',
        { reason: result.error }
      );
    }
    return result.user;
  } catch (error) {
    if (error instanceof SSOAuthenticationError) {
      // Handle known authentication errors
      logger.warn('Authentication failed', { code: error.code });
      throw error;
    }
    // Re-throw unexpected errors with context
    throw new SSOAuthenticationError(
      'Unexpected authentication error',
      'UNKNOWN_ERROR',
      { originalError: String(error) }
    );
  }
}

// ❌ Never silently swallow errors
try {
  await riskyOperation();
} catch (e) {
  // Bad: error is silently ignored
}
```

### 2.5 Async/Await Patterns

```typescript
// ✅ Use async/await over .then() chains
async function loadUserData(): Promise<UserData> {
  const user = await fetchUser();
  const permissions = await fetchPermissions(user.id);
  return { user, permissions };
}

// ✅ Use Promise.all for independent async operations
async function loadDashboard(): Promise<Dashboard> {
  const [user, settings, notifications] = await Promise.all([
    fetchUser(),
    fetchSettings(),
    fetchNotifications(),
  ]);
  return { user, settings, notifications };
}

// ❌ Never use sleep/delay to wait for processes
// Bad: Using setTimeout to wait
await new Promise(resolve => setTimeout(resolve, 5000));

// ✅ Instead, poll or use event-based waiting
async function waitForCondition(
  check: () => boolean,
  maxAttempts = 10,
  interval = 1000
): Promise<void> {
  for (let i = 0; i < maxAttempts; i++) {
    if (check()) return;
    await new Promise(resolve => setTimeout(resolve, interval));
  }
  throw new Error('Condition not met within timeout');
}
```

---

## 3. React Component Standards

### 3.1 Component Structure

```tsx
// 1. Imports
import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import styles from './BackToPulsarButton.module.scss';

// 2. Types
interface BackToPulsarButtonProps {
  className?: string;
  onClick?: () => void;
}

// 3. Component
export const BackToPulsarButton = observer(function BackToPulsarButton({
  className,
  onClick,
}: BackToPulsarButtonProps) {
  // 3a. Services/hooks
  const workspaceModeService = useService(WorkspaceModeService);
  
  // 3b. Local state (if needed)
  const [isLoading, setIsLoading] = useState(false);
  
  // 3c. Effects
  useEffect(() => {
    // Effect logic
  }, [dependency]);
  
  // 3d. Event handlers
  const handleClick = useCallback(() => {
    onClick?.();
    workspaceModeService.returnToPulsar();
  }, [onClick, workspaceModeService]);
  
  // 3e. Early returns for conditional rendering
  if (!workspaceModeService.isPulsarMode) {
    return null;
  }
  
  // 3f. Render
  return (
    <button
      className={classNames(styles.button, className)}
      onClick={handleClick}
      disabled={isLoading}
    >
      Back to Pulsar
    </button>
  );
});
```

### 3.2 Component Guidelines

```tsx
// ✅ Use functional components with hooks
export const UserProfile = observer(function UserProfile() {
  // Implementation
});

// ✅ Extract complex logic into custom hooks
function useUserPermissions(userId: string) {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    loadPermissions(userId).then(setPermissions).finally(() => setLoading(false));
  }, [userId]);
  
  return { permissions, loading };
}

// ✅ Memoize expensive computations
const sortedItems = useMemo(
  () => items.sort((a, b) => a.name.localeCompare(b.name)),
  [items]
);

// ✅ Use callback for event handlers passed to children
const handleDelete = useCallback((id: string) => {
  removeItem(id);
}, [removeItem]);

// ❌ Don't use inline functions for callbacks in renders
<Button onClick={() => handleClick(item.id)} /> // Bad: creates new function each render

// ✅ Use callback wrapper
const handleItemClick = useCallback((id: string) => () => handleClick(id), [handleClick]);
<Button onClick={handleItemClick(item.id)} />
```

---

## 4. Java Standards

### 4.1 Class Structure

```java
package io.cloudbeaver.service.auth.pulsar;

// 1. Imports (grouped: java, javax, third-party, project)
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jkiss.dbeaver.model.security.*;
import io.cloudbeaver.model.session.*;

/**
 * Service for handling Pulsar SSO authentication.
 * 
 * @since 1.0
 */
public class PulsarSSOAuthProvider implements DBWAuthProvider {
    
    // 2. Constants
    private static final String ISSUER = "pulsar";
    private static final long TOKEN_VALIDITY_MS = 5 * 60 * 1000;
    
    // 3. Static fields
    private static final Logger log = LoggerFactory.getLogger(PulsarSSOAuthProvider.class);
    
    // 4. Instance fields
    private final TokenValidator tokenValidator;
    private final SessionManager sessionManager;
    
    // 5. Constructors
    public PulsarSSOAuthProvider(
            @Nonnull TokenValidator tokenValidator,
            @Nonnull SessionManager sessionManager) {
        this.tokenValidator = Objects.requireNonNull(tokenValidator);
        this.sessionManager = Objects.requireNonNull(sessionManager);
    }
    
    // 6. Public methods
    @Override
    @Nonnull
    public AuthenticationResult authenticate(@Nonnull Credentials credentials) 
            throws AuthenticationException {
        // Implementation
    }
    
    // 7. Private methods
    private boolean validateTokenStructure(@Nonnull String token) {
        // Implementation
    }
}
```

### 4.2 Naming Conventions

```java
// ✅ Good naming
public class PulsarSSOAuthProvider {}     // PascalCase for classes
public interface AuthProvider {}          // PascalCase for interfaces
private static final int MAX_RETRIES = 3; // SCREAMING_SNAKE for constants
private String userName;                  // camelCase for fields
public void authenticateUser() {}         // camelCase for methods

// Package naming: lowercase, dot-separated
package io.cloudbeaver.service.auth.pulsar;
```

### 4.3 Null Safety

```java
// ✅ Use annotations for null safety
@Nonnull
public String getUserId() {
    return Objects.requireNonNull(userId);
}

@Nullable
public String getDisplayName() {
    return displayName; // May be null
}

// ✅ Use Optional for methods that may not return a value
public Optional<User> findUser(String id) {
    return Optional.ofNullable(userRepository.get(id));
}

// ❌ Don't return null for collections
public List<Permission> getPermissions() {
    return Collections.emptyList(); // Not null
}
```

---

## 5. CSS/SCSS Standards

### 5.1 File Organization

```scss
// 1. Variables
$primary-color: #1976D2;
$spacing-unit: 8px;

// 2. Mixins
@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

// 3. Base styles
.container {
  padding: $spacing-unit * 2;
  
  // 4. Nested elements (max 3 levels)
  &__header {
    @include flex-center;
    margin-bottom: $spacing-unit;
  }
  
  &__content {
    // Content styles
  }
  
  // 5. Modifiers
  &--compact {
    padding: $spacing-unit;
  }
  
  // 6. States
  &:hover {
    // Hover styles
  }
  
  &:disabled {
    opacity: 0.5;
  }
}
```

### 5.2 BEM Naming

```scss
// Block
.button {}

// Element (double underscore)
.button__icon {}
.button__text {}

// Modifier (double dash)
.button--primary {}
.button--large {}

// ❌ Avoid deep nesting
.nav .menu .item .link .icon {} // Bad: too specific

// ✅ Flatten with BEM
.nav-item__icon {} // Good: flat and specific
```

---

## 6. Documentation Standards

### 6.1 Code Comments

```typescript
// ✅ Document the "why", not the "what"
// We use a 5-minute token validity to balance security with UX.
// Shorter times increase security but require more frequent refreshes.
const TOKEN_VALIDITY_MS = 5 * 60 * 1000;

// ✅ Use JSDoc for public APIs
/**
 * Validates a JWT token and extracts user information.
 * 
 * @param token - The JWT token string to validate
 * @returns Validation result with user data if successful
 * @throws {SSOAuthenticationError} When token is malformed or expired
 * 
 * @example
 * const result = await ssoService.validateToken('eyJ...');
 * if (result.isValid) {
 *   console.log(result.user.displayName);
 * }
 */
async validateToken(token: string): Promise<TokenValidationResult>

// ❌ Avoid obvious comments
// Increment counter by 1
counter++; // Bad: obvious

// ✅ Explain complex logic
// Apply exponential backoff: wait 2^attempt seconds between retries
const delay = Math.pow(2, attempt) * 1000;
```

### 6.2 TODO Comments

```typescript
// ✅ Include ticket reference and owner
// TODO(PULSE-123): Implement token refresh mechanism - @developer_name

// ✅ Include context for future developers
// FIXME(PULSE-456): This is a temporary workaround for Safari's 
// localStorage quota. Remove when we migrate to IndexedDB.

// ❌ Don't leave vague TODOs
// TODO: fix this later
```

---

## 7. Testing Standards

### 7.1 Test File Organization

```typescript
describe('WorkspaceModeService', () => {
  // Group related tests
  describe('initializeFromURL', () => {
    it('should parse mode parameter correctly', () => {});
    it('should sanitize input to prevent XSS', () => {});
    it('should use default mode when parameter missing', () => {});
  });
  
  describe('isPulsarMode', () => {
    it('should return true when mode is pulsar', () => {});
    it('should return false for standalone mode', () => {});
  });
});
```

### 7.2 Test Naming

```typescript
// ✅ Use descriptive test names
it('should reject token when expiration is in the past', () => {});
it('should sanitize HTML entities in brand_title parameter', () => {});

// ❌ Avoid vague test names
it('works correctly', () => {}); // Bad
it('handles edge case', () => {}); // Bad
```

---

## 8. Security Standards

### 8.1 Input Sanitization

```typescript
// ✅ Always sanitize user input
function sanitizeText(input: string | null): string {
  if (!input) return '';
  return input
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .substring(0, MAX_LENGTH);
}

// ✅ Validate URLs
function sanitizeURL(input: string | null): string | null {
  if (!input) return null;
  try {
    const url = new URL(input);
    if (!['http:', 'https:'].includes(url.protocol)) {
      return null; // Reject javascript:, data:, etc.
    }
    return url.toString();
  } catch {
    return null;
  }
}
```

### 8.2 Never Disable Security

```typescript
// ❌ NEVER do this
// @ts-ignore
// eslint-disable-next-line security/detect-object-injection

// ❌ NEVER bypass SSL verification
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

// ✅ Find the root cause and fix properly
// If SSL is failing, fix the certificate, not the verification
```

---

## 9. Enforcement

### 9.1 Automated Checks

- **ESLint**: Enforces JavaScript/TypeScript standards
- **Prettier**: Enforces formatting
- **TypeScript**: Enforces type safety (strict mode)
- **SonarQube**: Detects code smells and vulnerabilities
- **Pre-commit hooks**: Run checks before commit

### 9.2 Code Review Checklist

- [ ] Follows naming conventions
- [ ] Properly documented
- [ ] No security vulnerabilities
- [ ] Has unit tests
- [ ] No console.log or debug code
- [ ] Error handling is complete
- [ ] No TODO without ticket reference

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-15 | Tech Lead | Initial coding standards |
