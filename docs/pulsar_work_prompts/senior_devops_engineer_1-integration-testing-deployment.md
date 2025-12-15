# Pulsar Agent 4: Integration Testing & Deployment Configuration

## Objective
Create comprehensive integration tests, deployment scripts, and configuration management for the Quick Wins features (Query History, Deep Linking, Keyboard Shortcuts, Activity Tracking).

## Context
With new features being added to both PulseQL and Pulsar, we need robust integration testing to ensure everything works end-to-end, plus deployment configuration to roll out these features to production.

## Requirements

### 1. Integration Test Suite

Create end-to-end tests for all Quick Wins features:

**Test Framework Setup**
- Selenium/Cypress for UI testing
- TestContainers for database testing
- MockServer for external API mocking
- JUnit 5 for test orchestration

**Test Scenarios**

#### A. Query History & Favorites Integration
```java
@Test
public void testQueryHistoryFlowEndToEnd() {
    // 1. User executes query in PulseQL
    // 2. Query appears in history
    // 3. User favorites the query
    // 4. Favorite synced to Pulsar
    // 5. User can access favorite from both systems
    // 6. History cleaned up after 100 entries
}

@Test
public void testQuerySearchAndFilter() {
    // 1. Execute multiple queries
    // 2. Search history by text
    // 3. Filter by connection
    // 4. Verify results
}
```

#### B. Deep Linking Integration
```java
@Test
public void testDeepLinkNavigation() {
    // 1. Generate deep link from Pulsar table
    // 2. Click link to open PulseQL
    // 3. Verify SSO authentication
    // 4. Verify correct table loaded
    // 5. Verify user permissions applied
}

@Test
public void testQueryDeepLink() {
    // 1. Create query link in Pulsar
    // 2. Open link in PulseQL
    // 3. Verify query executed
    // 4. Verify results displayed
}
```

#### C. Keyboard Shortcuts Integration
```java
@Test
public void testKeyboardShortcuts() {
    // 1. Open PulseQL workspace
    // 2. Press Ctrl+Enter in SQL editor
    // 3. Verify query executed
    // 4. Press Ctrl+H
    // 5. Verify history panel opened
}

@Test
public void testShortcutCustomization() {
    // 1. Open shortcuts settings
    // 2. Change shortcut for execute query
    // 3. Save configuration
    // 4. Verify new shortcut works
}
```

#### D. Activity Tracking Integration
```java
@Test
public void testActivityTracking() {
    // 1. User logs in via SSO
    // 2. Execute queries
    // 3. Verify activity events sent to backend
    // 4. Verify session updated
    // 5. Become idle
    // 6. Verify idle warning
    // 7. Verify session timeout
}

@Test
public void testActivityAnalytics() {
    // 1. Execute multiple activities
    // 2. View analytics dashboard in Pulsar
    // 3. Verify correct statistics
    // 4. Verify activity history
}
```

### 2. Performance Testing

Create performance test suite:

**Load Tests**
```java
@Test
public void testQueryHistoryConcurrentWrites() {
    // Simulate 100 concurrent users
    // Each executing queries
    // Verify all history entries saved
    // Verify no data loss
}

@Test
public void testActivityTrackingHighVolume() {
    // Simulate 1000 activity events/second
    // Verify backend handles load
    // Verify no dropped events
    // Verify response time < 100ms
}
```

**Stress Tests**
```java
@Test
public void testDeepLinkGeneration() {
    // Generate 10,000 deep links
    // Verify all links valid
    // Verify performance acceptable
    // Verify no memory leaks
}
```

### 3. Security Testing

Implement security test suite:

**Authentication Tests**
```java
@Test
public void testUnauthorizedDeepLinkAccess() {
    // 1. Generate deep link for user A
    // 2. Try to access as user B
    // 3. Verify access denied
    // 4. Verify proper error message
}

@Test
public void testExpiredSessionActivity() {
    // 1. Create session
    // 2. Wait for expiration
    // 3. Send activity event
    // 4. Verify session invalid error
}
```

**Input Validation Tests**
```java
@Test
public void testSQLInjectionInQueryHistory() {
    // Attempt SQL injection in query text
    // Verify properly sanitized
    // Verify no database compromise
}

@Test
public void testXSSInQueryComments() {
    // Add XSS payload in comment
    // Verify sanitized on display
    // Verify no script execution
}
```

### 4. Deployment Configuration

Create deployment scripts and configuration:

**A. Feature Flags Configuration**

`feature-flags.yml`
```yaml
features:
  query-history:
    enabled: true
    max-entries: 100
    storage: localStorage
  
  deep-linking:
    enabled: true
    short-url-service: true
    base-url: https://pulseql.example.com
  
  keyboard-shortcuts:
    enabled: true
    customizable: true
    help-panel: true
  
  activity-tracking:
    enabled: true
    idle-timeout-minutes: 15
    warning-minutes: 13
    sync-interval-seconds: 30
```

**B. Database Migrations**

Create Flyway migrations:

`V1.1__add_query_history_tables.sql`
```sql
-- Add necessary tables for Quick Wins features
CREATE TABLE IF NOT EXISTS query_history...
CREATE TABLE IF NOT EXISTS shared_queries...
CREATE TABLE IF NOT EXISTS activity_events...
```

**C. nginx Configuration Update**

`nginx-quickwins.conf`
```nginx
# Deep linking route
location /workspace {
    # Enable deep link parameter parsing
    if ($arg_pulsar_link) {
        add_header X-Deep-Link $arg_pulsar_link;
    }
}

# Activity tracking endpoint
location /api/sso/activity {
    # Rate limiting for activity events
    limit_req zone=activity burst=20 nodelay;
    proxy_pass http://backend;
}

# Query sharing WebSocket
location /ws/queries {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

**D. Environment Configuration**

`.env.production`
```bash
# Quick Wins Feature Configuration
QUERY_HISTORY_MAX_ENTRIES=100
ACTIVITY_TRACKING_ENABLED=true
ACTIVITY_IDLE_TIMEOUT_MS=900000
DEEP_LINKING_ENABLED=true
KEYBOARD_SHORTCUTS_ENABLED=true

# Backend URLs
ACTIVITY_TRACKING_API=https://pulsar.example.com/api/sso/activity
QUERY_SHARING_API=https://pulsar.example.com/api/queries
DEEP_LINK_BASE_URL=https://pulseql.example.com
```

### 5. Monitoring & Observability

Setup monitoring for new features:

**Metrics to Track**
```yaml
metrics:
  - name: query_history_entries_total
    type: counter
    description: Total query history entries created
  
  - name: deep_link_clicks_total
    type: counter
    description: Total deep link navigations
    
  - name: keyboard_shortcut_usage
    type: counter
    labels: [shortcut_id]
    description: Keyboard shortcut usage by ID
    
  - name: activity_events_total
    type: counter
    labels: [event_type]
    description: Activity events by type
    
  - name: idle_sessions_total
    type: gauge
    description: Current number of idle sessions
    
  - name: activity_api_latency_ms
    type: histogram
    description: Activity API response time
```

**Logging Configuration**

`logback-quickwins.xml`
```xml
<logger name="com.cloudbeaver.pulsar.history" level="INFO"/>
<logger name="com.cloudbeaver.pulsar.deeplinking" level="INFO"/>
<logger name="com.cloudbeaver.pulsar.activity" level="DEBUG"/>
<logger name="com.cloudbeaver.pulsar.shortcuts" level="INFO"/>
```

### 6. Rollout Plan

Create phased rollout strategy:

**Phase 1: Internal Testing (Week 1)**
- Deploy to staging environment
- Internal team testing
- Performance validation
- Bug fixes

**Phase 2: Beta Users (Week 2)**
- Enable for 10% of users
- Monitor metrics and feedback
- Address issues
- Optimize performance

**Phase 3: Gradual Rollout (Week 3)**
- 25% of users
- 50% of users
- 75% of users
- Monitor at each stage

**Phase 4: Full Release (Week 4)**
- 100% of users
- Monitor for 1 week
- Document learnings
- Plan next phase enhancements

## Implementation Tasks

### 1. Test Framework Setup (4-5 hours)
- Configure Cypress/Selenium
- Setup TestContainers
- Create test utilities
- Setup CI/CD integration

### 2. Integration Tests (12-15 hours)
- Write test scenarios
- Implement test automation
- Create test data generators
- Setup test cleanup

### 3. Performance Tests (6-8 hours)
- Create load test scripts
- Setup JMeter/Gatling
- Define performance benchmarks
- Create test reports

### 4. Security Tests (4-5 hours)
- Implement security test cases
- Setup penetration testing
- Validate input sanitization
- Test authentication flows

### 5. Deployment Configuration (6-8 hours)
- Create feature flags
- Write migration scripts
- Update nginx config
- Create deployment scripts

### 6. Monitoring Setup (4-5 hours)
- Configure Prometheus metrics
- Setup Grafana dashboards
- Create alerts
- Document monitoring

### 7. Documentation (4-5 hours)
- Write deployment guide
- Create runbook
- Document rollback procedures
- Create troubleshooting guide

## Expected Deliverables

1. Comprehensive integration test suite
2. Performance test suite
3. Security test suite
4. Deployment configuration files
5. Database migration scripts
6. Monitoring dashboards
7. Deployment documentation
8. Rollout plan

## Success Criteria

- All integration tests pass
- Performance meets requirements
- Security tests pass
- Deployment scripts work correctly
- Monitoring dashboards functional
- Documentation complete
- Successful staging deployment

## Estimated Effort

- Test framework: 4-5 hours
- Integration tests: 12-15 hours
- Performance tests: 6-8 hours
- Security tests: 4-5 hours
- Deployment config: 6-8 hours
- Monitoring: 4-5 hours
- Documentation: 4-5 hours
- **Total: 40-51 hours (5-6 days)**

## Dependencies

- Cypress or Selenium
- TestContainers
- JMeter or Gatling
- Prometheus & Grafana
- Flyway (migrations)
- Feature flag system
