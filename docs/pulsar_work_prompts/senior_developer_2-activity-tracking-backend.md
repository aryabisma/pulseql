# Pulsar Agent 2: Activity Tracking Backend Integration

## Objective
Implement backend endpoints in Pulsar to receive and validate activity tracking events from PulseQL for enhanced session security and monitoring.

## Context
PulseQL has implemented an Activity Tracking Service that monitors user activity to detect idle sessions and track usage patterns. Pulsar needs to receive and process these activity events for session validation and security monitoring.

## Requirements

### 1. Activity Tracking API

Create REST endpoint: `POST /api/sso/activity`

**Request Body:**
```json
{
  "session_id": "string",
  "event": {
    "type": "query" | "navigation" | "export" | "connection" | "idle" | "active",
    "timestamp": 1234567890,
    "details": {
      "query_id": "string",
      "connection_id": "string",
      "idle_duration": 900000
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

### 2. Session Validation Logic

Implement session validation service:

**SessionValidationService.java**
- Update session last_activity timestamp
- Check session expiration
- Detect suspicious activity patterns
- Trigger session renewal if needed

```java
public class SessionValidationService {
    
    public SessionValidationResult validateActivity(
        String sessionId,
        ActivityEvent event
    ) {
        // 1. Validate session exists
        // 2. Update last activity time
        // 3. Check for anomalies
        // 4. Return validation result
    }
}
```

### 3. Activity Analytics

Create activity analytics dashboard:

**Features:**
- Real-time active users count
- Query execution statistics
- Session duration metrics
- Idle timeout warnings
- Unusual activity alerts

**Database Schema:**
```sql
CREATE TABLE activity_events (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255),
    user_id VARCHAR(255),
    event_type VARCHAR(50),
    timestamp BIGINT,
    details JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_activity_session ON activity_events(session_id);
CREATE INDEX idx_activity_user ON activity_events(user_id);
CREATE INDEX idx_activity_timestamp ON activity_events(timestamp);
```

### 4. Idle Session Management

Implement idle session detection:

**Features:**
- Automatic session cleanup after 15 minutes of inactivity
- Warning notification at 13 minutes (2-minute warning)
- Grace period for session extension
- Session termination with cleanup

**IdleSessionManager.java**
```java
@Component
public class IdleSessionManager {
    
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void checkIdleSessions() {
        // Find sessions with no activity > 15 minutes
        // Send warnings for sessions > 13 minutes
        // Terminate sessions > 15 minutes
    }
}
```

### 5. Audit Logging

Implement comprehensive audit logging:

**Logged Events:**
- All activity events
- Session creation/termination
- Idle warnings issued
- Session extensions
- Suspicious activity detections

**Log Format:**
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
  "ip_address": "192.168.1.100",
  "user_agent": "Mozilla/5.0..."
}
```

## Implementation Tasks

### 1. Backend API (6-8 hours)

Create files:
- `ActivityTrackingController.java` - REST endpoint
- `SessionValidationService.java` - Validation logic
- `ActivityEvent.java` - Event model
- `ActivityEventRepository.java` - Data access

Implementation:
```java
@RestController
@RequestMapping("/api/sso")
public class ActivityTrackingController {
    
    @Autowired
    private SessionValidationService validationService;
    
    @PostMapping("/activity")
    public ResponseEntity<ActivityResponse> trackActivity(
        @RequestBody ActivityRequest request
    ) {
        // Validate session
        SessionValidationResult result = 
            validationService.validateActivity(
                request.getSessionId(),
                request.getEvent()
            );
        
        // Return response with session status
        return ResponseEntity.ok(
            new ActivityResponse(result)
        );
    }
}
```

### 2. Idle Session Management (4-5 hours)

Implementation:
- Scheduled task to check idle sessions
- Warning notification system
- Automatic session cleanup
- Session extension API

### 3. Analytics Dashboard (6-8 hours)

Create admin dashboard:
- Real-time activity metrics
- Active sessions list
- Idle session warnings
- Activity heatmap
- User activity trends

### 4. Database Schema (2-3 hours)

- Create activity_events table
- Create indexes for performance
- Setup partitioning for large datasets
- Add retention policy (30-90 days)

### 5. Testing (4-5 hours)

- Unit tests for validation service
- Integration tests for API
- E2E tests for idle detection
- Load tests for high activity volume

## Expected Deliverables

1. REST API endpoint for activity tracking
2. Session validation service
3. Idle session manager with scheduled tasks
4. Database schema and migrations
5. Analytics dashboard (admin UI)
6. Audit logging implementation
7. Comprehensive test suite

## Security Considerations

- Validate session ID before processing
- Rate limit activity tracking API (prevent DoS)
- Encrypt sensitive activity details
- Secure audit logs against tampering
- GDPR compliance for activity data

## Performance Considerations

- Batch activity events for efficiency
- Use async processing for non-critical events
- Implement caching for session lookups
- Database partitioning for large datasets
- Optimize queries with proper indexes

## Monitoring & Alerts

Setup alerts for:
- High number of idle sessions
- Unusual activity patterns
- Failed session validations
- Database write failures
- API response time > 500ms

## Configuration

Add to `application.properties`:
```properties
# Activity Tracking Configuration
pulsar.activity.idle-timeout-minutes=15
pulsar.activity.warning-minutes=13
pulsar.activity.cleanup-interval-seconds=60
pulsar.activity.retention-days=30
pulsar.activity.batch-size=100
```

## Success Criteria

- Activity events received and stored correctly
- Session validation works accurately
- Idle sessions detected and terminated
- Analytics dashboard shows real-time data
- All tests pass
- Performance meets requirements (< 100ms P95)

## Estimated Effort

- Backend API: 6-8 hours
- Session validation: 4-5 hours
- Idle management: 4-5 hours
- Analytics dashboard: 6-8 hours
- Database setup: 2-3 hours
- Testing: 4-5 hours
- **Total: 26-34 hours (3-4 days)**

## Dependencies

- Spring Boot
- PostgreSQL or MySQL
- Redis (optional, for caching)
- Spring Scheduler
- Jackson (JSON processing)
