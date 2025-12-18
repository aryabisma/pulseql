# Activity Tracking Backend Integration

## Overview

This document describes the Activity Tracking Backend Integration for PulseQL, which provides comprehensive session management, idle detection, and activity monitoring capabilities.

## Components

### 1. Database Schema

**Tables:**

- `CB_ACTIVITY_EVENTS` - Stores all user activity events
- `CB_SESSION_ACTIVITY` - Tracks session activity timestamps and warnings

**Schema Migration:** `cb_schema_update_27.sql`

### 2. Core Models

#### ActivityEvent
Represents a user activity event from PulseQL.

**Event Types:**
- `QUERY` - SQL query execution
- `NAVIGATION` - UI navigation
- `EXPORT` - Data export
- `CONNECTION` - Connection opened/closed
- `IDLE` - User became idle
- `ACTIVE` - User became active

**Properties:**
- `eventId` - Unique event identifier (auto-generated if not provided)
- `sessionId` - Session identifier
- `userId` - User identifier (optional)
- `type` - Event type
- `timestamp` - Event timestamp (milliseconds)
- `details` - JSON string with event details
- `ipAddress` - Client IP address
- `userAgent` - Client user agent

#### SessionValidationResult
Result of session validation including warnings and status.

**Properties:**
- `success` - Whether validation succeeded
- `sessionValid` - Whether session is still valid
- `lastActivityTime` - Timestamp of last activity
- `idleTimeMs` - How long the session has been idle
- `warnings` - List of warning messages
- `errorMessage` - Error message if validation failed

### 3. Core Services

#### SessionValidationService
Handles session validation, activity tracking, and idle detection.

**Key Methods:**
- `validateActivity(sessionId, event)` - Validate and record activity
- `getIdleSessions(idleThresholdMs)` - Get sessions exceeding idle threshold
- `removeSession(sessionId)` - Remove session from tracking

**Features:**
- In-memory caching for performance
- Automatic warning detection
- Database persistence
- Thread-safe operations

#### IdleSessionManager
Scheduled task for detecting and cleaning up idle sessions.

**Configuration:**
- `idle-timeout-minutes` - Default: 15 minutes
- `warning-minutes` - Default: 13 minutes
- `cleanup-interval-seconds` - Default: 60 seconds

**Features:**
- Automatic idle session detection
- Warning notifications at 13 minutes
- Session termination at 15 minutes
- Structured audit logging

### 4. REST API

#### POST /api/sso/activity

Track user activity events.

**Request:**
```json
{
  "session_id": "session-123",
  "user_id": "user-456",
  "event": {
    "type": "query",
    "timestamp": 1702657200000,
    "details": {
      "query_id": "q789",
      "connection_id": "conn-123"
    }
  }
}
```

**Response:**
```json
{
  "success": true,
  "session_valid": true,
  "warnings": [
    "Session will expire in 2 minutes due to inactivity"
  ]
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Session expired due to inactivity"
}
```

## Configuration

Add to `config/core/cloudbeaver.conf`:

```hocon
pulsar: {
    sso: {
        signingSecret: "${PULSAR_SSO_SECRET:default-secret-CHANGE-IN-PRODUCTION}"
    },
    activity: {
        idle-timeout-minutes: "${PULSAR_ACTIVITY_IDLE_TIMEOUT:15}",
        warning-minutes: "${PULSAR_ACTIVITY_WARNING_THRESHOLD:13}",
        cleanup-interval-seconds: "${PULSAR_ACTIVITY_CLEANUP_INTERVAL:60}",
        retention-days: "${PULSAR_ACTIVITY_RETENTION_DAYS:30}",
        batch-size: "${PULSAR_ACTIVITY_BATCH_SIZE:100}"
    }
}
```

**Environment Variables:**
- `PULSAR_SSO_SECRET` - JWT signing secret (REQUIRED in production)
- `PULSAR_ACTIVITY_IDLE_TIMEOUT` - Idle timeout in minutes
- `PULSAR_ACTIVITY_WARNING_THRESHOLD` - Warning threshold in minutes
- `PULSAR_ACTIVITY_CLEANUP_INTERVAL` - Cleanup interval in seconds
- `PULSAR_ACTIVITY_RETENTION_DAYS` - Activity data retention period
- `PULSAR_ACTIVITY_BATCH_SIZE` - Batch size for processing

## Security Features

### Input Validation
- Session ID format validation (alphanumeric, dash, underscore only)
- JSON structure validation
- SQL injection prevention via prepared statements
- XSS prevention via output escaping

### Rate Limiting
- Maximum 100 requests per minute per IP (configurable)
- Prevents DoS attacks

### Audit Logging
Structured JSON logs for all security events:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "event_type": "activity_tracked",
  "session_id": "session123",
  "user_id": "user456",
  "event_details": {
    "type": "query",
    "query_id": "q789"
  },
  "ip_address": "192.168.1.100"
}
```

## Performance Optimizations

### In-Memory Caching
- Session activity data cached in ConcurrentHashMap
- Reduces database queries for frequent operations
- Automatic cache invalidation on session removal

### Database Indexes
- `CB_ACTIVITY_EVENTS_SESSION_IDX` - Fast session lookup
- `CB_ACTIVITY_EVENTS_USER_IDX` - Fast user lookup
- `CB_ACTIVITY_EVENTS_TIMESTAMP_IDX` - Fast timestamp queries
- `CB_SESSION_ACTIVITY_LAST_ACTIVITY_IDX` - Fast idle detection

### Async Processing
- Idle session cleanup runs in background thread
- Non-blocking event processing
- Scheduled executor with proper lifecycle management

## Monitoring

### Key Metrics
- Active sessions count
- Idle sessions count
- Activity events per second
- Average response time
- Warning notifications sent
- Sessions terminated

### Alerts
Set up alerts for:
- High number of idle sessions (> 100)
- Unusual activity patterns
- Failed session validations (> 10%)
- Database write failures
- API response time > 500ms

## Testing

### Unit Tests
- `ActivityEventTest` - Tests event model and parsing
- `SessionValidationResultTest` - Tests result model and builder

### Manual Testing

1. **Test Activity Tracking:**
```bash
curl -X POST http://localhost:8978/api/sso/activity \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "test-session-123",
    "user_id": "test-user",
    "event": {
      "type": "query",
      "timestamp": 1702657200000,
      "details": {"query_id": "q123"}
    }
  }'
```

2. **Test Idle Warning:**
- Wait 13 minutes without activity
- Send activity event
- Should receive warning in response

3. **Test Session Expiration:**
- Wait 15 minutes without activity
- Send activity event
- Should receive session expired error

## Deployment

### Prerequisites
- PostgreSQL or H2 database
- Java 11 or higher
- CloudBeaver server

### Steps

1. **Apply Database Migration:**
```sql
-- Run cb_schema_update_27.sql on your database
```

2. **Configure Environment:**
```bash
export PULSAR_SSO_SECRET="your-secure-secret-key"
export PULSAR_ACTIVITY_IDLE_TIMEOUT=15
export PULSAR_ACTIVITY_WARNING_THRESHOLD=13
```

3. **Deploy Bundle:**
```bash
# Build the bundle
cd server/bundles/io.cloudbeaver.service.pulsar.auth
mvn clean install

# Bundle will be automatically deployed with CloudBeaver
```

4. **Verify Deployment:**
- Check logs for "Activity Tracking service initialized successfully"
- Test the API endpoint with curl
- Monitor for any errors

## Troubleshooting

### Common Issues

**Issue: "Activity Tracking service not initialized"**
- Check that plugin.xml is in the bundle
- Verify servlet handler is registered
- Check server logs for errors

**Issue: "Session not found in activity tracking"**
- Session may have expired and been cleaned up
- Check database for session record
- Verify session ID is correct

**Issue: "Database connection errors"**
- Check database connection pool settings
- Verify database schema migration applied
- Check database user permissions

**Issue: "Idle sessions not being cleaned up"**
- Check IdleSessionManager is started
- Verify cleanup interval configuration
- Check for errors in scheduled task logs

## Future Enhancements

### Phase 2 (Future Sprint)
- [ ] Analytics dashboard for activity metrics
- [ ] Real-time activity monitoring
- [ ] Anomaly detection for suspicious activity
- [ ] Export activity reports
- [ ] GDPR compliance features (data retention, anonymization)
- [ ] Batch processing for high-volume events
- [ ] Redis caching for distributed deployments

## References

- [Integration Architecture](../development/01-integration-architecture.md)
- [Development Workflow](../development/04-development-workflow.md)
- [Coding Standards](../development/03-coding-standards.md)
- [Senior Developer 2 Work Prompt](../pulsar_work_prompts/senior_developer_2-activity-tracking-backend.md)
