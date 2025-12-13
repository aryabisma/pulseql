# Pulsar Agent 3: Query Sharing & Collaboration Infrastructure

## Objective
Implement backend infrastructure in Pulsar to support query sharing, collaboration, and team knowledge sharing features in PulseQL.

## Context
PulseQL users need the ability to save, share, and collaborate on SQL queries with team members. This requires Pulsar-side database schema, APIs, and notification systems.

## Requirements

### 1. Database Schema

Create tables for query management:

```sql
-- Shared Queries
CREATE TABLE shared_queries (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sql_query TEXT NOT NULL,
    connection_id VARCHAR(255),
    schema_name VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    visibility VARCHAR(50) DEFAULT 'private', -- private, team, public
    team_id VARCHAR(255),
    is_template BOOLEAN DEFAULT FALSE,
    tags JSONB,
    metadata JSONB
);

-- Query Permissions
CREATE TABLE query_permissions (
    id UUID PRIMARY KEY,
    query_id UUID REFERENCES shared_queries(id) ON DELETE CASCADE,
    user_id VARCHAR(255),
    team_id VARCHAR(255),
    permission VARCHAR(50), -- view, edit, execute, delete
    granted_by VARCHAR(255),
    granted_at TIMESTAMP DEFAULT NOW()
);

-- Query Comments
CREATE TABLE query_comments (
    id UUID PRIMARY KEY,
    query_id UUID REFERENCES shared_queries(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    comment_text TEXT NOT NULL,
    parent_id UUID REFERENCES query_comments(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Query Versions (for history tracking)
CREATE TABLE query_versions (
    id UUID PRIMARY KEY,
    query_id UUID REFERENCES shared_queries(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    sql_query TEXT NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    change_description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Query Favorites (user-specific)
CREATE TABLE query_favorites (
    id UUID PRIMARY KEY,
    query_id UUID REFERENCES shared_queries(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(query_id, user_id)
);

-- Query Executions (analytics)
CREATE TABLE query_executions (
    id UUID PRIMARY KEY,
    query_id UUID REFERENCES shared_queries(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    execution_time_ms BIGINT,
    row_count INT,
    status VARCHAR(50), -- success, error, cancelled
    error_message TEXT,
    executed_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_shared_queries_created_by ON shared_queries(created_by);
CREATE INDEX idx_shared_queries_team ON shared_queries(team_id);
CREATE INDEX idx_shared_queries_visibility ON shared_queries(visibility);
CREATE INDEX idx_query_permissions_query ON query_permissions(query_id);
CREATE INDEX idx_query_permissions_user ON query_permissions(user_id);
CREATE INDEX idx_query_comments_query ON query_comments(query_id);
CREATE INDEX idx_query_versions_query ON query_versions(query_id);
CREATE INDEX idx_query_executions_query ON query_executions(query_id);
```

### 2. REST API Endpoints

Implement comprehensive API:

**Query Management**
```java
POST   /api/queries              - Create shared query
GET    /api/queries              - List queries (with filtering)
GET    /api/queries/{id}         - Get query details
PUT    /api/queries/{id}         - Update query
DELETE /api/queries/{id}         - Delete query
POST   /api/queries/{id}/fork    - Fork/copy query
```

**Permissions**
```java
POST   /api/queries/{id}/permissions    - Grant permission
DELETE /api/queries/{id}/permissions/{permId} - Revoke permission
GET    /api/queries/{id}/permissions    - List permissions
```

**Comments**
```java
POST   /api/queries/{id}/comments       - Add comment
GET    /api/queries/{id}/comments       - List comments
PUT    /api/queries/{id}/comments/{commentId} - Update comment
DELETE /api/queries/{id}/comments/{commentId} - Delete comment
```

**Versions**
```java
GET    /api/queries/{id}/versions       - List versions
GET    /api/queries/{id}/versions/{version} - Get specific version
POST   /api/queries/{id}/versions/{version}/restore - Restore version
```

**Favorites**
```java
POST   /api/queries/{id}/favorite       - Add to favorites
DELETE /api/queries/{id}/favorite       - Remove from favorites
GET    /api/users/me/favorites          - Get user's favorites
```

### 3. Service Layer Implementation

Create service classes:

**SharedQueryService.java**
```java
@Service
public class SharedQueryService {
    
    public SharedQuery createQuery(SharedQueryRequest request, User user) {
        // Validate permissions
        // Save query
        // Create initial version
        // Send notifications
    }
    
    public List<SharedQuery> listQueries(QueryFilter filter, User user) {
        // Apply visibility filters
        // Apply permission checks
        // Return filtered list
    }
    
    public SharedQuery updateQuery(UUID id, UpdateRequest request, User user) {
        // Check edit permission
        // Create new version
        // Update query
        // Notify collaborators
    }
    
    public void deleteQuery(UUID id, User user) {
        // Check delete permission
        // Soft delete or hard delete
        // Clean up related data
    }
}
```

**QueryPermissionService.java**
```java
@Service
public class QueryPermissionService {
    
    public boolean canView(UUID queryId, User user) {
        // Check visibility
        // Check permissions
        // Check team membership
    }
    
    public boolean canEdit(UUID queryId, User user) {
        // Check permissions
    }
    
    public void grantPermission(
        UUID queryId,
        String userId,
        Permission permission,
        User grantor
    ) {
        // Validate grantor has permission
        // Grant permission
        // Send notification
    }
}
```

### 4. Real-Time Collaboration (WebSocket)

Implement WebSocket support for real-time features:

**QueryCollaborationWebSocket.java**
```java
@Component
@ServerEndpoint("/ws/queries/{queryId}")
public class QueryCollaborationWebSocket {
    
    @OnMessage
    public void handleMessage(String message, Session session) {
        // Parse message
        // Broadcast to other collaborators
        // Update presence
    }
    
    // Broadcast events:
    // - User joined/left
    // - Cursor position changes
    // - Query text changes
    // - Comment added
}
```

### 5. Notification System

Implement notification service:

**NotificationService.java**
```java
@Service
public class NotificationService {
    
    public void notifyQueryShared(SharedQuery query, List<User> recipients) {
        // Send email notification
        // Send in-app notification
        // Log notification
    }
    
    public void notifyQueryComment(Comment comment, SharedQuery query) {
        // Notify query owner
        // Notify mentioned users
        // Notify thread participants
    }
    
    public void notifyPermissionGranted(Permission perm, User user) {
        // Notify user of new access
    }
}
```

## Implementation Tasks

### 1. Database Schema (3-4 hours)
- Create migration scripts
- Add tables and indexes
- Setup foreign keys
- Add sample data for testing

### 2. Model Classes (2-3 hours)
- SharedQuery.java
- QueryPermission.java
- QueryComment.java
- QueryVersion.java
- DTO classes for requests/responses

### 3. Repository Layer (2-3 hours)
- SharedQueryRepository.java
- QueryPermissionRepository.java
- QueryCommentRepository.java
- QueryVersionRepository.java
- Custom query methods

### 4. Service Layer (8-10 hours)
- SharedQueryService.java
- QueryPermissionService.java
- QueryCommentService.java
- QueryVersionService.java
- NotificationService.java

### 5. REST Controllers (4-5 hours)
- SharedQueryController.java
- QueryPermissionController.java
- QueryCommentController.java
- Implement all CRUD operations
- Add validation and error handling

### 6. WebSocket Implementation (4-5 hours)
- Setup WebSocket configuration
- Implement collaboration endpoint
- Handle presence tracking
- Broadcast changes

### 7. Security & Permissions (3-4 hours)
- Implement permission checks
- Add security annotations
- Validate user access
- Audit logging

### 8. Testing (6-8 hours)
- Unit tests for services
- Integration tests for APIs
- WebSocket tests
- Permission tests

## Expected Deliverables

1. Complete database schema with migrations
2. JPA entities and repositories
3. Service layer with business logic
4. REST API controllers
5. WebSocket collaboration endpoint
6. Notification service
7. Comprehensive test suite
8. API documentation (Swagger)

## Security Considerations

- Validate user permissions before all operations
- Sanitize SQL queries (prevent SQL injection)
- Encrypt sensitive query data
- Audit all query access and modifications
- Rate limit API endpoints
- Validate team membership for team queries

## Performance Considerations

- Index frequently queried columns
- Implement caching for popular queries
- Paginate query lists
- Optimize permission checks
- Use database views for complex queries
- Implement query result caching

## Success Criteria

- All CRUD operations work correctly
- Permissions enforced properly
- Real-time collaboration functional
- Notifications sent successfully
- Performance meets requirements
- All tests pass
- API documentation complete

## Estimated Effort

- Database schema: 3-4 hours
- Models & repositories: 4-6 hours
- Service layer: 8-10 hours
- REST controllers: 4-5 hours
- WebSocket: 4-5 hours
- Security: 3-4 hours
- Testing: 6-8 hours
- **Total: 32-42 hours (4-5 days)**

## Dependencies

- Spring Boot
- Spring Data JPA
- PostgreSQL
- WebSocket (Spring WebSocket)
- Email service (for notifications)
- Redis (for caching)
