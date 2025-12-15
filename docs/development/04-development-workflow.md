# Development Workflow

**Document Version**: 1.0  
**Last Updated**: December 15, 2025

---

## 1. Git Workflow

### 1.1 Branch Strategy

**CRITICAL**: All PulseQL development branches MUST use `pulseql-` prefix to avoid conflicts with the parent DBeaver repository.

We use a modified GitFlow strategy with the following branches:

```
pulseql-main (production)
│
├── pulseql-develop (development)
│   │
│   ├── pulseql-feature/PULSE-123-sso-implementation
│   ├── pulseql-feature/PULSE-124-permission-mapping
│   ├── pulseql-bugfix/PULSE-125-token-expiration
│   └── ...
│
├── pulseql-release/v1.0.0
│   └── pulseql-hotfix/PULSE-999-critical-fix
│
└── pulseql-hotfix/PULSE-999-production-fix
```

**Never use**: `main`, `devel`, `develop`, `master` - these are reserved for the DBeaver parent repository.

### 1.2 Branch Naming

**All branches MUST start with `pulseql-` prefix.**

| Type | Pattern | Example |
|------|---------|----------|
| Main | `pulseql-main` | `pulseql-main` |
| Development | `pulseql-develop` | `pulseql-develop` |
| Feature | `pulseql-feature/PULSE-{ticket}-{short-description}` | `pulseql-feature/PULSE-123-sso-implementation` |
| Bug Fix | `pulseql-bugfix/PULSE-{ticket}-{short-description}` | `pulseql-bugfix/PULSE-456-token-validation` |
| Hotfix | `pulseql-hotfix/PULSE-{ticket}-{short-description}` | `pulseql-hotfix/PULSE-789-security-patch` |
| Release | `pulseql-release/v{major}.{minor}.{patch}` | `pulseql-release/v1.2.0` |

### 1.3 Commit Messages

Follow Conventional Commits format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Formatting, no code change
- `refactor`: Code restructuring
- `perf`: Performance improvement
- `test`: Adding tests
- `chore`: Maintenance tasks
- `ci`: CI/CD changes
- `security`: Security fixes

**Examples**:

```
feat(sso): implement JWT token validation

- Add PulsarSSOService for token handling
- Implement token structure validation
- Add expiration checking

Closes PULSE-123
```

```
fix(permissions): correct ANALYST role mapping

The ANALYST role was incorrectly mapped to viewer permissions.
Updated PermissionMapper to include data.export permission.

Fixes PULSE-456
```

```
security(auth): add token blacklist checking

SECURITY: Tokens are now checked against blacklist before acceptance.
This prevents use of revoked tokens.

Closes PULSE-789
```

---

## 2. Development Flow

### 2.1 Starting New Work

```bash
# 1. Update local develop branch
git checkout pulseql-develop
git pull origin pulseql-develop

# 2. Create feature branch (note: pulseql- prefix required)
git checkout -b pulseql-feature/PULSE-123-sso-implementation

# 3. Make changes with small, focused commits
git add .
git commit -m "feat(sso): add JWT token validation service"

# 4. Push branch to remote
git push -u origin pulseql-feature/PULSE-123-sso-implementation
```

### 2.2 During Development

```bash
# Keep branch updated with develop
git fetch origin
git rebase origin/pulseql-develop

# If conflicts occur, resolve them:
# 1. Fix conflicts in files
# 2. git add <resolved-files>
# 3. git rebase --continue

# Push updates (force needed after rebase)
git push --force-with-lease
```

### 2.3 Completing Work

```bash
# 1. Ensure all tests pass locally
yarn test
yarn lint

# 2. Squash commits if needed (optional)
git rebase -i HEAD~N  # where N is number of commits

# 3. Push final changes
git push

# 4. Create Pull Request in GitHub
```

---

## 3. Pull Request Process

### 3.1 PR Requirements

Before creating a PR:

- [ ] All tests pass locally
- [ ] Code follows coding standards
- [ ] New tests added for new functionality
- [ ] Documentation updated if needed
- [ ] No merge conflicts with pulseql-develop
- [ ] Self-reviewed the changes

### 3.2 PR Template

```markdown
## Description
Brief description of the changes.

## Type of Change
- [ ] Bug fix (non-breaking change fixing an issue)
- [ ] New feature (non-breaking change adding functionality)
- [ ] Breaking change (fix or feature causing existing functionality to change)
- [ ] Documentation update

## Related Issues
Closes PULSE-123

## Testing Done
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist
- [ ] Code follows project coding standards
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings introduced
- [ ] Tests pass locally
```

### 3.3 Code Review Guidelines

**As a Reviewer**:

1. **Be respectful and constructive**
2. **Focus on**:
   - Logic errors
   - Security vulnerabilities
   - Performance issues
   - Code maintainability
   - Test coverage
3. **Use suggestion feature** for small changes
4. **Request changes** only for blocking issues
5. **Approve** when ready to merge

**Review Checklist**:

- [ ] Code is readable and well-structured
- [ ] No security vulnerabilities
- [ ] Error handling is adequate
- [ ] Tests cover the changes
- [ ] No unnecessary complexity
- [ ] Follows established patterns

### 3.4 Merging

After approval:

```bash
# 1. Squash and merge (preferred for features)
# - Keeps history clean
# - Single commit per feature

# 2. Rebase and merge (for small, clean PRs)
# - Preserves commit history
# - Only if commits are well-organized

# 3. Never use regular merge commits
```

---

## 4. Build Process

### 4.1 Local Build

**Backend (Java/Maven)**:

```powershell
# Navigate to server directory
cd server

# Clean build
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests
```

**Frontend (TypeScript/Yarn)**:

```powershell
# Navigate to webapp directory
cd webapp

# Install dependencies
yarn install

# Build all packages
yarn build

# Run in development mode
yarn dev
```

**Full Build**:

```powershell
# From deploy directory
cd deploy
.\build.bat   # Windows
./build.sh    # Linux/Mac
```

### 4.2 CI/CD Pipeline

```yaml
# Simplified pipeline flow
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Trigger   │────>│    Build    │────>│    Test     │
│ (PR/Push)   │     │  (Maven +   │     │  (Unit +    │
│             │     │   Yarn)     │     │  Integration)│
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                         ┌─────────────────────┘
                         │
                         ▼
                    ┌─────────────┐     ┌─────────────┐
                    │   Analyze   │────>│   Deploy    │
                    │  (ESLint,   │     │  (Staging/  │
                    │   Sonar)    │     │   Prod)     │
                    └─────────────┘     └─────────────┘
```

---

## 5. Testing Workflow

### 5.1 Running Tests

**Unit Tests**:

```powershell
# Frontend tests
cd webapp
yarn test              # Run all tests
yarn test:watch        # Watch mode
yarn test:coverage     # With coverage

# Backend tests
cd server
mvn test               # Run all tests
mvn test -Dtest=ClassNameTest  # Single class
```

**Integration Tests**:

```powershell
# Run integration tests
yarn test:integration

# With specific browser
yarn test:e2e --browser=chrome
```

### 5.2 Test Requirements

| Change Type | Required Tests |
|-------------|----------------|
| New feature | Unit + Integration |
| Bug fix | Unit (reproducing bug) |
| Refactor | Existing tests must pass |
| Security fix | Security-focused tests |

---

## 6. Documentation Workflow

### 6.1 When to Update Documentation

- Adding new features → Update user guide
- API changes → Update API reference
- Configuration changes → Update deployment guide
- Architecture changes → Update architecture docs
- Sprint completion → Update progress documents

### 6.2 Documentation Location

| Type | Location |
|------|----------|
| Architecture | `/docs/development/01-integration-architecture.md` |
| User Guide | `/docs/11-user-guide.md` |
| API Reference | `/docs/06-api-integration-specification.md` |
| Sprint Progress | `/docs/PMP_integration/06-daily-standup-log.md` |
| Task Status | `/docs/PMP_integration/04-sprint-backlog-current.md` |

---

## 7. Task Completion Checklist

Before marking a task as complete:

### 7.1 Code Checklist

- [ ] Code compiles without errors
- [ ] No ESLint/SonarQube warnings
- [ ] Unit tests written and passing
- [ ] Integration tests updated if needed
- [ ] No hardcoded values (use constants/config)
- [ ] Error handling is complete
- [ ] Logging added for debugging
- [ ] Security considerations addressed

### 7.2 Documentation Checklist

- [ ] Code comments added where needed
- [ ] JSDoc/JavaDoc for public APIs
- [ ] README updated if setup changed
- [ ] User documentation updated
- [ ] API documentation updated

### 7.3 Project Tracking Checklist

- [ ] Sprint backlog updated
- [ ] Daily standup log updated
- [ ] Milestone tracker updated (if applicable)
- [ ] Task moved to "Done" in board
- [ ] Time logged accurately

---

## 8. Release Process

### 8.1 Release Preparation

```bash
# 1. Create release branch from devel
git checkout devel
git pull origin devel
git checkout -b release/v1.2.0

# 2. Update version numbers
# - package.json (webapp)
# - pom.xml (server)
# - CHANGELOG.md

# 3. Run full test suite
yarn test
mvn test

# 4. Create release PR to main
```

### 8.2 Release Checklist

- [ ] All features for release complete
- [ ] All tests passing
- [ ] Security scan passed
- [ ] Performance benchmarks acceptable
- [ ] Documentation complete
- [ ] Release notes written
- [ ] Stakeholder approval obtained

### 8.3 Post-Release

```bash
# 1. Tag the release
git tag -a v1.2.0 -m "Release v1.2.0"
git push origin v1.2.0

# 2. Merge release to devel
git checkout devel
git merge release/v1.2.0
git push origin devel

# 3. Delete release branch
git branch -d release/v1.2.0
git push origin --delete release/v1.2.0
```

---

## 9. Emergency Procedures

### 9.1 Hotfix Process

```bash
# 1. Create hotfix from main
git checkout main
git pull origin main
git checkout -b hotfix/PULSE-999-critical-fix

# 2. Make minimal fix
# ... code changes ...

# 3. Test thoroughly
yarn test
mvn test

# 4. Create PR to main (expedited review)
# 5. After merge, also merge to devel
```

### 9.2 Rollback Procedure

```bash
# If deployment fails
# 1. Identify last good version
git log --oneline

# 2. Revert to previous release
git revert HEAD   # Revert last commit
# OR
git checkout v1.1.0  # Checkout previous tag

# 3. Deploy previous version
```

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-15 | Tech Lead | Initial workflow documentation |
