# Copilot Instructions - PulseQL Integration Project

**Project**: PulseQL-Pulsar Integration  
**Version**: 1.0  
**Last Updated**: December 15, 2025

---

## Project Context

This project integrates PulseQL (a CloudBeaver-based database query workspace) with the Pulsar application. The integration includes SSO authentication, RBAC permission mapping, UI customization, and various API integrations.

### Key Technologies

- **Frontend**: TypeScript, React 19, MobX, Vite 7, SCSS
- **Backend**: Java 22, OSGi (Eclipse Equinox), GraphQL, Maven
- **Database**: PostgreSQL (application data), various target databases
- **Authentication**: JWT-based SSO

---

## Critical Rules - MUST FOLLOW

### Rule 1: No Shortcut Solutions

**NEVER take shortcut solutions that suppress, disable, or bypass functionality to solve issues.**

```typescript
// ❌ FORBIDDEN - Never do these
// @ts-ignore
// @ts-expect-error
// eslint-disable
// eslint-disable-next-line
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
try { } catch (e) { /* silently ignore */ }
```

**ALWAYS find the root cause and implement a permanent solution.**

If a permanent solution is not immediately possible:
1. Document WHY a temporary workaround is needed
2. Create a ticket for the permanent fix
3. Implement a proper alternative (not suppression)

### Rule 2: No Sleep Timers for Process Waiting

**NEVER use sleep/delay timers to wait for processes to complete.**

```typescript
// ❌ FORBIDDEN
await new Promise(resolve => setTimeout(resolve, 5000));
Thread.sleep(5000);
```

**ALWAYS use one of these approaches:**

1. **Polling with condition check**:
```typescript
async function waitForCondition(check: () => boolean, maxAttempts = 30): Promise<void> {
  for (let i = 0; i < maxAttempts; i++) {
    if (check()) return;
    await new Promise(r => setTimeout(r, 1000));
  }
  throw new Error('Condition not met');
}
```

2. **Event-based waiting** (for code):
```typescript
await new Promise((resolve, reject) => {
  process.on('ready', resolve);
  process.on('error', reject);
});
```

3. **Open long-running processes in new terminal** (for chat agent sessions):

When running long processes like builds, NEVER send commands to the same terminal while a process is running. This will interrupt the running process.

```powershell
# ❌ FORBIDDEN - Interrupts the build
# Terminal 1: build.bat is running...
# You send: Start-Sleep 5  # This breaks the build!

# ✅ CORRECT - Run in new terminal, monitor from background
# Open build in NEW terminal using isBackground=true
# Monitor using get_terminal_output tool
```

### Rule 3: Clean Code - No Spaghetti

**AVOID spaghetti and smelly code. Follow industry-standard practices.**

- Maximum 3 levels of nesting
- Functions should do ONE thing
- Clear, descriptive naming
- DRY (Don't Repeat Yourself)
- SOLID principles

```typescript
// ❌ BAD
if (a) { if (b) { if (c) { if (d) { /* deep nesting */ } } } }

// ✅ GOOD
if (!a || !b || !c || !d) return;
// proceed with main logic
```

### Rule 4: 360° Review Before Completion

**Before marking ANY task as complete, evaluate from ALL angles:**

| Aspect | Check |
|--------|-------|
| **Reusability** | Can this code be reused? Is it modular? |
| **Readability** | Can a new developer understand this quickly? |
| **Security** | Any XSS, injection, or auth vulnerabilities? |
| **Performance** | Time/space complexity acceptable? |
| **Maintainability** | Will this be easy to modify later? |
| **Testability** | Can this be unit tested effectively? |

### Rule 5: Keep Documentation Updated

**ALWAYS update relevant documentation after completing tasks:**

- `/docs/PMP_integration/04-sprint-backlog-current.md` - Task status
- `/docs/PMP_integration/06-daily-standup-log.md` - Daily progress
- `/docs/PMP_integration/07-milestone-tracker.md` - Milestone progress
- Technical docs if architecture changed
- User docs if user-facing features added

---

## Development References

### Key Documents to Reference

| Document | Location | Purpose |
|----------|----------|---------|
| Integration Architecture | `/docs/development/01-integration-architecture.md` | System design |
| Project Structure | `/docs/development/02-project-structure.md` | Code organization |
| Coding Standards | `/docs/development/03-coding-standards.md` | Code style rules |
| Development Workflow | `/docs/development/04-development-workflow.md` | Git, PR process |
| Development Guidelines | `/docs/development/05-development-guidelines.md` | Best practices |
| Sprint Planning | `/docs/PMP_integration/02-sprint-planning.md` | Sprint breakdown |
| Product Backlog | `/docs/PMP_integration/03-product-backlog.md` | All user stories |

### Key Code Locations

| Component | Location |
|-----------|----------|
| Pulsar Integration Plugin | `/webapp/packages/plugin-pulsar-integration/` |
| Authentication Service | `/server/bundles/io.cloudbeaver.service.auth/` |
| Security Service | `/server/bundles/io.cloudbeaver.service.security/` |
| Configuration | `/config/core/cloudbeaver.conf` |

---

## Coding Conventions

### TypeScript/JavaScript

```typescript
// File structure
// 1. Imports (external, then internal)
// 2. Constants
// 3. Types/Interfaces
// 4. Class/Function implementation

// Naming
class PascalCaseClass {}
interface PascalCaseInterface {}
const SCREAMING_SNAKE_CONSTANT = 'value';
const camelCaseVariable = 'value';
function camelCaseFunction() {}

// Always use explicit types
function processUser(user: User): ProcessedUser { }

// Always handle errors properly
try {
  await operation();
} catch (error) {
  logger.error('Operation failed', { error: String(error) });
  throw new SpecificError('Meaningful message', error);
}
```

### Java

```java
// Package naming
package io.cloudbeaver.service.auth.pulsar;

// Class structure
public class ServiceName {
    // 1. Constants
    private static final String CONSTANT = "value";
    
    // 2. Fields
    private final Dependency dependency;
    
    // 3. Constructor
    public ServiceName(Dependency dependency) {
        this.dependency = Objects.requireNonNull(dependency);
    }
    
    // 4. Public methods
    // 5. Private methods
}

// Always use @Nonnull/@Nullable annotations
public @Nonnull String getRequired() { }
public @Nullable String getOptional() { }
```

### Git Commits

```
<type>(<scope>): <description>

[body]

[footer]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`, `security`

Example:
```
feat(sso): implement JWT token validation

- Add PulsarSSOService for token handling
- Implement token structure validation
- Add expiration checking

Closes PULSE-123
```

---

## Security Requirements

### Input Validation

Always sanitize external input:

```typescript
function sanitizeText(input: string): string {
  return input
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .substring(0, MAX_LENGTH);
}

function sanitizeURL(input: string): string | null {
  try {
    const url = new URL(input);
    if (!['http:', 'https:'].includes(url.protocol)) {
      return null;
    }
    return url.toString();
  } catch {
    return null;
  }
}
```

### Never Do These

- ❌ Hardcode secrets in code
- ❌ Disable SSL verification
- ❌ Use `eval()` or dynamic code execution
- ❌ Trust user input without validation
- ❌ Log sensitive data (passwords, tokens)
- ❌ Use `any` type in TypeScript

---

## Testing Requirements

### Minimum Coverage

- Services: 80%
- Utilities: 90%
- Security-related code: 100%

### Test Structure

```typescript
describe('ServiceName', () => {
  describe('methodName', () => {
    it('should handle happy path', () => { });
    it('should handle error condition', () => { });
    it('should handle edge case', () => { });
  });
});
```

---

## Agent-Specific Instructions

For agent-specific instructions, see:

- `/docs/.github/agents/project_manager.md`
- `/docs/.github/agents/senior_principal_architect.md`
- `/docs/.github/agents/senior_developer_1.md`
- `/docs/.github/agents/senior_developer_2.md`
- (etc.)

Each agent file contains role-specific context, responsibilities, and references.

**Current Sprint Work Prompts**:
- `/docs/pulsar_work_prompts/senior_developer_1-deep-linking-api.md`
- `/docs/pulsar_work_prompts/senior_developer_2-activity-tracking-backend.md`
- `/docs/pulsar_work_prompts/senior_developer_2-query-sharing-infrastructure.md`
- `/docs/pulsar_work_prompts/senior_devops_engineer_1-integration-testing-deployment.md`

---

## Branch Naming Convention

**CRITICAL**: All development branches MUST use `pulseql-` prefix to avoid conflicts with parent DBeaver repository.

| Branch Type | Pattern | Example |
|-------------|---------|----------|
| Main | `pulseql-main` | `pulseql-main` |
| Development | `pulseql-develop` | `pulseql-develop` |
| Feature | `pulseql-feature/PULSE-{ticket}-{desc}` | `pulseql-feature/PULSE-123-sso` |
| Bugfix | `pulseql-bugfix/PULSE-{ticket}-{desc}` | `pulseql-bugfix/PULSE-456-token` |
| Hotfix | `pulseql-hotfix/PULSE-{ticket}-{desc}` | `pulseql-hotfix/PULSE-789-patch` |
| Release | `pulseql-release/v{version}` | `pulseql-release/v1.2.0` |

**Never use**: `main`, `devel`, `develop`, `master` - these belong to DBeaver parent repo.

---

## Quick Reference

### Build Commands

```powershell
# Full build
cd deploy
.\build.bat

# Frontend only
cd webapp
yarn build

# Backend only
cd server
mvn clean install

# Run tests
yarn test          # Frontend
mvn test           # Backend
```

### Common Patterns

```typescript
// Service with DI
@injectable()
export class MyService {
  constructor(private readonly dep: Dependency) {}
}

// Error handling
throw new SpecificError('message', ErrorCode.CODE);

// Async with proper waiting
const result = await pollUntilReady(checkFn, maxAttempts);
```

---

## When in Doubt

1. Check the relevant documentation in `/docs/development/`
2. Follow existing patterns in the codebase
3. Ask for clarification rather than guessing
4. Prioritize security over convenience
5. Test your changes thoroughly

---

**Remember**: Quality over speed. A proper solution today saves hours of debugging tomorrow.
