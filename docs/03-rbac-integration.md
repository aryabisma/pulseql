# RBAC Integration: Pulsar Permissions to PulseQL

## Executive Summary

This document defines the strategy for integrating Pulsar's Role-Based Access Control (RBAC) system with PulseQL, ensuring that users accessing the query workspace have appropriate permissions and feature visibility based on their Pulsar roles.

## Pulsar RBAC Model (Assumptions)

### Assumed Pulsar Permission Structure

Based on typical enterprise applications, we assume Pulsar has:

**User Roles**:
- `ADMIN`: Full system access
- `ANALYST`: Report viewing and query execution
- `DEVELOPER`: Report creation and editing
- `VIEWER`: Read-only access
- `AUDITOR`: Audit log access

**Permissions**:
- `report.view`: View existing reports
- `report.create`: Create new reports
- `report.edit`: Modify reports
- `report.delete`: Delete reports
- `report.execute`: Execute report queries
- `data.export`: Export data to files
- `connection.view`: View database connections
- `connection.create`: Create connections (admin only)
- `connection.edit`: Modify connections (admin only)
- `admin.access`: Administrative functions

### User Context Structure

```java
public class PulsarUserContext {
    private String userId;
    private String displayName;
    private String email;
    private String primaryRole;
    private Set<String> permissions;
    private List<TeamMembership> teams;
    private Map<String, Object> metadata;
}

public class TeamMembership {
    private String teamId;
    private String teamName;
    private String role; // team-specific role
    private Set<String> teamPermissions;
}
```

## PulseQL Permission Model

### Current PulseQL Permissions

PulseQL uses permission-based access control:

**Connection Permissions**:
- `connection.view`: View connections
- `connection.edit`: Create/edit connections
- `connection.delete`: Delete connections

**Query Permissions**:
- `sql.execute`: Execute SQL queries
- `sql.script.save`: Save SQL scripts
- `sql.script.share`: Share scripts with others

**Data Permissions**:
- `data.view`: View query results
- `data.edit`: Edit data in result sets
- `data.export`: Export data to files
- `data.import`: Import data from files

**Administrative Permissions**:
- `admin.users`: User management
- `admin.teams`: Team management
- `admin.server`: Server configuration
- `admin.drivers`: Driver management

**Resource Permissions**:
- `resource.view`: View resources
- `resource.edit`: Create/edit resources
- `resource.delete`: Delete resources
- `resource.share`: Share resources

## Permission Mapping Strategy

### Mapping Table

| Pulsar Permission | PulseQL Permissions | Description |
|-------------------|---------------------|-------------|
| `report.view` | `connection.view`, `sql.execute`, `data.view` | Basic query execution |
| `report.execute` | `connection.view`, `sql.execute`, `data.view` | Execute queries |
| `report.edit` | `connection.view`, `sql.execute`, `data.view`, `sql.script.save` | Save queries |
| `data.export` | `data.export` | Export query results |
| `connection.view` | `connection.view` | View connection details |
| `admin.access` | `admin.*` | Administrative access |

### Role-Based Default Permissions

| Pulsar Role | PulseQL Permissions | UI Restrictions |
|-------------|---------------------|-----------------|
| `VIEWER` | `connection.view`, `sql.execute`, `data.view` | Read-only mode, no export |
| `ANALYST` | `connection.view`, `sql.execute`, `data.view`, `data.export`, `sql.script.save` | Can save and export |
| `DEVELOPER` | `connection.view`, `sql.execute`, `data.view`, `data.edit`, `data.export`, `sql.script.save`, `sql.script.share`, `resource.*` | Full query features |
| `ADMIN` | All permissions | Full access including configuration |
| `AUDITOR` | `connection.view`, `sql.execute`, `data.view`, `data.export` | Query + export only |

## Implementation Approach

### 1. Permission Transfer via SSO Token

**JWT Token with Permissions**:

```json
{
  "iss": "pulsar",
  "sub": "user123",
  "exp": 1702660800,
  "user": {
    "userId": "user123",
    "displayName": "John Doe",
    "email": "john.doe@example.com",
    "role": "ANALYST"
  },
  "pulsar_permissions": [
    "report.view",
    "report.execute",
    "report.edit",
    "data.export"
  ],
  "pulseql_permissions": [
    "connection.view",
    "sql.execute",
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
  ]
}
```

### 2. Backend Permission Enforcement

**Custom Permission Provider**:

```java
package io.cloudbeaver.service.auth.pulsar;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.service.security.SMPermission;
import io.cloudbeaver.service.security.SMPermissionProvider;
import org.jkiss.code.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Permission provider that reads permissions from Pulsar SSO token
 */
public class PulsarPermissionProvider implements SMPermissionProvider {
    
    public static final String PROVIDER_ID = "pulsar";
    
    private final Map<String, Set<String>> rolePermissionMap;
    
    public PulsarPermissionProvider() {
        this.rolePermissionMap = initializeRolePermissionMap();
    }
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public Set<String> getUserPermissions(
        @NotNull WebSession session,
        @NotNull String userId
    ) throws DBWebException {
        
        // Get permissions from session attributes (set during SSO auth)
        Set<String> pulsarPermissions = getSessionAttribute(
            session, 
            "pulsar_permissions", 
            Set.class
        );
        
        Set<String> pulseqlPermissions = getSessionAttribute(
            session,
            "pulseql_permissions",
            Set.class
        );
        
        // Combine and return
        Set<String> allPermissions = new HashSet<>();
        
        if (pulseqlPermissions != null) {
            allPermissions.addAll(pulseqlPermissions);
        }
        
        // Map Pulsar permissions if not explicitly provided
        if (pulsarPermissions != null && pulseqlPermissions == null) {
            allPermissions.addAll(mapPulsarPermissions(pulsarPermissions));
        }
        
        // Add role-based permissions
        String role = getSessionAttribute(session, "pulsar_role", String.class);
        if (role != null && rolePermissionMap.containsKey(role)) {
            allPermissions.addAll(rolePermissionMap.get(role));
        }
        
        return allPermissions;
    }
    
    @Override
    public boolean hasPermission(
        @NotNull WebSession session,
        @NotNull String userId,
        @NotNull String permission
    ) throws DBWebException {
        
        Set<String> permissions = getUserPermissions(session, userId);
        
        // Check exact match
        if (permissions.contains(permission)) {
            return true;
        }
        
        // Check wildcard permissions (e.g., "admin.*" matches "admin.users")
        return permissions.stream()
            .filter(p -> p.endsWith(".*"))
            .anyMatch(p -> {
                String prefix = p.substring(0, p.length() - 2);
                return permission.startsWith(prefix + ".");
            });
    }
    
    private Set<String> mapPulsarPermissions(Set<String> pulsarPermissions) {
        return pulsarPermissions.stream()
            .flatMap(p -> getPulseQLPermissionsFor(p).stream())
            .collect(Collectors.toSet());
    }
    
    private Set<String> getPulseQLPermissionsFor(String pulsarPermission) {
        // Permission mapping logic
        switch (pulsarPermission) {
            case "report.view":
            case "report.execute":
                return Set.of("connection.view", "sql.execute", "data.view");
            
            case "report.edit":
                return Set.of("connection.view", "sql.execute", "data.view", 
                             "sql.script.save", "resource.edit");
            
            case "data.export":
                return Set.of("data.export");
            
            case "connection.view":
                return Set.of("connection.view");
            
            case "admin.access":
                return Set.of("admin.*");
            
            default:
                return Set.of();
        }
    }
    
    private Map<String, Set<String>> initializeRolePermissionMap() {
        Map<String, Set<String>> map = new HashMap<>();
        
        // VIEWER role
        map.put("VIEWER", Set.of(
            "connection.view",
            "sql.execute",
            "data.view"
        ));
        
        // ANALYST role
        map.put("ANALYST", Set.of(
            "connection.view",
            "sql.execute",
            "data.view",
            "data.export",
            "sql.script.save",
            "resource.view"
        ));
        
        // DEVELOPER role
        map.put("DEVELOPER", Set.of(
            "connection.view",
            "sql.execute",
            "data.view",
            "data.edit",
            "data.export",
            "data.import",
            "sql.script.save",
            "sql.script.share",
            "resource.view",
            "resource.edit",
            "resource.delete",
            "resource.share"
        ));
        
        // ADMIN role
        map.put("ADMIN", Set.of(
            "admin.*",
            "connection.*",
            "sql.*",
            "data.*",
            "resource.*"
        ));
        
        // AUDITOR role
        map.put("AUDITOR", Set.of(
            "connection.view",
            "sql.execute",
            "data.view",
            "data.export"
        ));
        
        return map;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getSessionAttribute(WebSession session, String key, Class<T> type) {
        Object value = session.getAttribute(key);
        return type.isInstance(value) ? (T) value : null;
    }
}
```

**Permission Registration**:

```xml
<!-- plugin.xml for Pulsar integration plugin -->
<extension point="io.cloudbeaver.security.permissionProvider">
    <permissionProvider
        id="pulsar"
        class="io.cloudbeaver.service.auth.pulsar.PulsarPermissionProvider"
        label="Pulsar Permission Provider"
        description="Permission provider for Pulsar SSO users"/>
</extension>
```

**Store Permissions During Authentication**:

```java
// In PulsarSSOAuthProvider.validateAuthentication()

// Extract permissions from JWT
List<String> pulsarPermissions = jwt.getClaim("pulsar_permissions")
    .asList(String.class);
List<String> pulseqlPermissions = jwt.getClaim("pulseql_permissions")
    .asList(String.class);
String role = jwt.getClaim("user").asMap().get("role").toString();

// Store in session
session.setAttribute("pulsar_permissions", new HashSet<>(pulsarPermissions));
session.setAttribute("pulseql_permissions", new HashSet<>(pulseqlPermissions));
session.setAttribute("pulsar_role", role);
session.setAttribute("permission_provider", "pulsar");
```

### 3. Frontend Permission Enforcement

**Permission Service**:

```typescript
// PulseQL: Permission checking service
import { injectable } from '@cloudbeaver/core-di';
import { UserInfoResource } from '@cloudbeaver/core-authentication';

@injectable()
export class PulsarPermissionService {
  
  constructor(
    private readonly userInfoResource: UserInfoResource
  ) {}
  
  /**
   * Check if user has a specific permission
   */
  hasPermission(permission: string): boolean {
    const userInfo = this.userInfoResource.data;
    
    if (!userInfo) {
      return false;
    }
    
    // Get permissions from user meta parameters
    const permissions = this.getPermissions();
    
    // Check exact match
    if (permissions.includes(permission)) {
      return true;
    }
    
    // Check wildcard permissions
    return permissions.some(p => {
      if (p.endsWith('.*')) {
        const prefix = p.substring(0, p.length - 2);
        return permission.startsWith(prefix + '.');
      }
      return false;
    });
  }
  
  /**
   * Check if user has any of the specified permissions
   */
  hasAnyPermission(...permissions: string[]): boolean {
    return permissions.some(p => this.hasPermission(p));
  }
  
  /**
   * Check if user has all of the specified permissions
   */
  hasAllPermissions(...permissions: string[]): boolean {
    return permissions.every(p => this.hasPermission(p));
  }
  
  /**
   * Get all user permissions
   */
  getPermissions(): string[] {
    const userInfo = this.userInfoResource.data;
    
    if (!userInfo) {
      return [];
    }
    
    return userInfo.metaParameters['pulseql_permissions'] || [];
  }
  
  /**
   * Get user's Pulsar role
   */
  getPulsarRole(): string | null {
    const userInfo = this.userInfoResource.data;
    
    if (!userInfo) {
      return null;
    }
    
    return userInfo.metaParameters['pulsar_role'] || null;
  }
  
  /**
   * Check if user is from Pulsar (SSO)
   */
  isPulsarUser(): boolean {
    const userInfo = this.userInfoResource.data;
    
    if (!userInfo || !userInfo.authTokens) {
      return false;
    }
    
    return userInfo.authTokens.some(
      token => token.authProvider === 'pulsar-sso'
    );
  }
  
  // Feature-specific permission checks
  
  canExecuteQuery(): boolean {
    return this.hasPermission('sql.execute');
  }
  
  canSaveScript(): boolean {
    return this.hasPermission('sql.script.save');
  }
  
  canExportData(): boolean {
    return this.hasPermission('data.export');
  }
  
  canImportData(): boolean {
    return this.hasPermission('data.import');
  }
  
  canEditData(): boolean {
    return this.hasPermission('data.edit');
  }
  
  canCreateConnection(): boolean {
    // Pulsar users should never create connections
    if (this.isPulsarUser()) {
      return false;
    }
    return this.hasPermission('connection.edit');
  }
  
  canViewConnections(): boolean {
    return this.hasPermission('connection.view');
  }
  
  canShareResources(): boolean {
    return this.hasPermission('resource.share');
  }
  
  canAccessAdmin(): boolean {
    return this.hasAnyPermission('admin.*', 'admin.users', 'admin.server');
  }
}
```

**React Component Permission Wrapper**:

```typescript
// Permission-based component wrapper
import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import type { ReactNode } from 'react';

interface RequirePermissionProps {
  permission?: string;
  permissions?: string[];
  requireAll?: boolean;
  fallback?: ReactNode;
  children: ReactNode;
}

export const RequirePermission = observer<RequirePermissionProps>(
  function RequirePermission({
    permission,
    permissions,
    requireAll = false,
    fallback = null,
    children
  }) {
    const permissionService = useService(PulsarPermissionService);
    
    let hasAccess = false;
    
    if (permission) {
      hasAccess = permissionService.hasPermission(permission);
    } else if (permissions) {
      hasAccess = requireAll
        ? permissionService.hasAllPermissions(...permissions)
        : permissionService.hasAnyPermission(...permissions);
    }
    
    if (!hasAccess) {
      return <>{fallback}</>;
    }
    
    return <>{children}</>;
  }
);
```

**Usage in Components**:

```typescript
// Example: Conditional rendering based on permissions
export const QueryToolbar = observer(function QueryToolbar() {
  const permissionService = useService(PulsarPermissionService);
  
  return (
    <div className="query-toolbar">
      {/* Execute button - always shown if can execute */}
      <RequirePermission permission="sql.execute">
        <Button onClick={executeQuery}>Execute</Button>
      </RequirePermission>
      
      {/* Save button - only for users who can save */}
      <RequirePermission permission="sql.script.save">
        <Button onClick={saveScript}>Save</Button>
      </RequirePermission>
      
      {/* Export button - only for users who can export */}
      <RequirePermission permission="data.export">
        <Button onClick={exportData}>Export</Button>
      </RequirePermission>
      
      {/* Edit data button - only for users who can edit */}
      <RequirePermission permission="data.edit">
        <Button onClick={editData}>Edit Data</Button>
      </RequirePermission>
    </div>
  );
});
```

### 4. GraphQL Permission Checks

**Backend Permission Validation**:

```java
// GraphQL resolver with permission check
@GraphQLQuery
public void executeQuery(
    @GraphQLContext WebSession session,
    @GraphQLArgument String connectionId,
    @GraphQLArgument String query
) throws DBWebException {
    
    // Check permission
    if (!hasPermission(session, "sql.execute")) {
        throw new DBWebException(
            "Permission denied: sql.execute permission required"
        );
    }
    
    // Execute query...
}

@GraphQLMutation
public void saveScript(
    @GraphQLContext WebSession session,
    @GraphQLArgument String scriptId,
    @GraphQLArgument String content
) throws DBWebException {
    
    // Check permission
    if (!hasPermission(session, "sql.script.save")) {
        throw new DBWebException(
            "Permission denied: sql.script.save permission required"
        );
    }
    
    // Save script...
}

private boolean hasPermission(WebSession session, String permission) {
    try {
        SMPermissionProvider provider = getPermissionProvider(session);
        return provider.hasPermission(
            session,
            session.getUserId(),
            permission
        );
    } catch (Exception e) {
        return false;
    }
}
```

## UI Restrictions for Pulsar Users

### Restricted Features

The following features should be hidden or disabled for Pulsar users:

1. **Connection Management**:
   - Hide "New Connection" button
   - Disable connection edit/delete actions
   - Show connections as read-only

2. **User Administration**:
   - Hide user management menu
   - Disable team management
   - Hide admin settings

3. **Configuration**:
   - Hide server settings
   - Disable driver management
   - Hide global preferences that affect all users

4. **Limited Navigation**:
   - Focus on query workspace
   - Hide irrelevant navigation items

### UI Customization Service

```typescript
// Service to customize UI for Pulsar users
@injectable()
export class PulsarUICustomizationService {
  
  constructor(
    private readonly permissionService: PulsarPermissionService,
    private readonly navigationTreeService: NavigationTreeService,
    private readonly topMenuService: TopMenuService
  ) {}
  
  /**
   * Apply UI customizations for Pulsar users
   */
  applyPulsarUICustomizations(): void {
    if (!this.permissionService.isPulsarUser()) {
      return;
    }
    
    // Hide connection management actions
    this.hideConnectionManagement();
    
    // Customize navigation tree
    this.customizeNavigationTree();
    
    // Remove admin menu items
    this.removeAdminMenuItems();
    
    // Customize toolbar
    this.customizeToolbar();
  }
  
  private hideConnectionManagement(): void {
    // Remove "New Connection" action
    const connectionActions = this.navigationTreeService.getConnectionActions();
    connectionActions.forEach(action => {
      if (action.id === 'connection.create') {
        action.hidden = true;
      }
      if (action.id === 'connection.edit' || action.id === 'connection.delete') {
        action.disabled = true;
      }
    });
  }
  
  private customizeNavigationTree(): void {
    // Show only database objects, hide projects/resources
    this.navigationTreeService.setFilter({
      showConnections: true,
      showDatabases: true,
      showSchemas: true,
      showTables: true,
      showColumns: true,
      showProjects: false,  // Hide for Pulsar users
      showResources: this.permissionService.hasPermission('resource.view')
    });
  }
  
  private removeAdminMenuItems(): void {
    if (!this.permissionService.canAccessAdmin()) {
      this.topMenuService.removeMenuItem('administration');
      this.topMenuService.removeMenuItem('server-settings');
      this.topMenuService.removeMenuItem('user-management');
    }
  }
  
  private customizeToolbar(): void {
    // Customize based on permissions
    const toolbarItems = this.topMenuService.getToolbarItems();
    
    toolbarItems.forEach(item => {
      // Hide based on permissions
      if (item.id === 'import-data' && 
          !this.permissionService.canImportData()) {
        item.hidden = true;
      }
      
      if (item.id === 'export-data' && 
          !this.permissionService.canExportData()) {
        item.hidden = true;
      }
    });
  }
}
```

**Bootstrap Integration**:

```typescript
// Apply customizations on app startup
@injectable()
export class PulsarUIBootstrap extends Bootstrap {
  
  constructor(
    private readonly uiCustomizationService: PulsarUICustomizationService
  ) {
    super();
  }
  
  async load(): Promise<void> {
    // Wait for user authentication
    await this.appAuthService.authUser();
    
    // Apply UI customizations
    this.uiCustomizationService.applyPulsarUICustomizations();
  }
}
```

## Permission Synchronization

### Real-time Permission Updates

**Permission Change Webhook**:

```java
// Pulsar: Notify PulseQL of permission changes
@PostMapping("/api/user/{userId}/permissions/updated")
public void notifyPermissionChange(@PathVariable String userId) {
    // Notify PulseQL
    pulseQLWebhookService.notifyPermissionChange(userId);
}

// PulseQL: Webhook endpoint
@PostMapping("/api/pulsar/webhook/permissions")
public void handlePermissionChange(
    @RequestBody PermissionChangeNotification notification
) {
    // Verify webhook signature
    if (!verifyWebhookSignature(notification)) {
        throw new UnauthorizedException();
    }
    
    // Update user permissions in active sessions
    List<WebSession> sessions = sessionManager.getUserSessions(
        notification.getUserId()
    );
    
    for (WebSession session : sessions) {
        // Refresh permissions from Pulsar
        refreshUserPermissions(session, notification.getUserId());
    }
}

private void refreshUserPermissions(WebSession session, String userId) {
    // Call Pulsar API to get updated permissions
    UserPermissions permissions = pulsarApiClient.getUserPermissions(userId);
    
    // Update session attributes
    session.setAttribute("pulsar_permissions", permissions.getPulsarPermissions());
    session.setAttribute("pulseql_permissions", permissions.getPulseqlPermissions());
    session.setAttribute("pulsar_role", permissions.getRole());
}
```

### Periodic Permission Refresh

```java
@Scheduled(fixedDelay = 300000) // Every 5 minutes
public void refreshAllActivePulsarUserPermissions() {
    List<WebSession> pulsarSessions = sessionManager.getSessionsByAuthProvider("pulsar-sso");
    
    for (WebSession session : pulsarSessions) {
        try {
            String userId = session.getUserId();
            refreshUserPermissions(session, userId);
        } catch (Exception e) {
            logger.warn("Failed to refresh permissions for session: {}", 
                session.getSessionId(), e);
        }
    }
}
```

## Audit Logging

### Permission Check Logging

```java
@Aspect
@Component
public class PermissionAuditAspect {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("PERMISSION_AUDIT");
    
    @Around("@annotation(requiresPermission)")
    public Object auditPermissionCheck(
        ProceedingJoinPoint joinPoint,
        RequiresPermission requiresPermission
    ) throws Throwable {
        
        WebSession session = getSessionFromArgs(joinPoint.getArgs());
        String userId = session != null ? session.getUserId() : "anonymous";
        String permission = requiresPermission.value();
        
        boolean hasPermission = checkPermission(session, permission);
        
        // Log the permission check
        auditLogger.info(
            "Permission check - user: {}, permission: {}, granted: {}, method: {}",
            userId,
            permission,
            hasPermission,
            joinPoint.getSignature().toShortString()
        );
        
        if (!hasPermission) {
            throw new DBWebException("Permission denied: " + permission);
        }
        
        return joinPoint.proceed();
    }
}
```

### Audit Log Format

```json
{
  "timestamp": "2024-12-12T14:30:00Z",
  "event": "permission_check",
  "user_id": "user123",
  "pulsar_role": "ANALYST",
  "permission": "sql.execute",
  "granted": true,
  "method": "executeQuery",
  "session_id": "session-abc-123"
}
```

## Testing Strategy

### Unit Tests

```java
@Test
public void testPermissionMapping() {
    Set<String> pulsarPermissions = Set.of("report.view", "data.export");
    Set<String> pulseqlPermissions = permissionMapper.mapPulsarPermissions(
        pulsarPermissions
    );
    
    assertTrue(pulseqlPermissions.contains("connection.view"));
    assertTrue(pulseqlPermissions.contains("sql.execute"));
    assertTrue(pulseqlPermissions.contains("data.view"));
    assertTrue(pulseqlPermissions.contains("data.export"));
}

@Test
public void testRoleBasedPermissions() {
    String role = "ANALYST";
    Set<String> permissions = permissionProvider.getPermissionsForRole(role);
    
    assertTrue(permissions.contains("sql.execute"));
    assertTrue(permissions.contains("data.export"));
    assertFalse(permissions.contains("admin.users"));
}
```

### Integration Tests

```typescript
describe('Permission-based UI', () => {
  it('should hide connection create button for viewer role', async () => {
    // Login as viewer
    await authenticateWithRole('VIEWER');
    
    // Navigate to connections
    await navigateTo('/connections');
    
    // Check that create button is hidden
    const createButton = screen.queryByRole('button', { name: /new connection/i });
    expect(createButton).not.toBeInTheDocument();
  });
  
  it('should show export button for analyst role', async () => {
    await authenticateWithRole('ANALYST');
    await executeQuery('SELECT * FROM users');
    
    const exportButton = screen.getByRole('button', { name: /export/i });
    expect(exportButton).toBeInTheDocument();
    expect(exportButton).toBeEnabled();
  });
});
```

## Configuration

### Permission Configuration File

```yaml
# pulsar-permissions.yml
permission_mapping:
  # Pulsar → PulseQL permission mappings
  report.view:
    - connection.view
    - sql.execute
    - data.view
  
  report.edit:
    - connection.view
    - sql.execute
    - data.view
    - sql.script.save
    - resource.edit
  
  data.export:
    - data.export

role_permissions:
  VIEWER:
    - connection.view
    - sql.execute
    - data.view
  
  ANALYST:
    - connection.view
    - sql.execute
    - data.view
    - data.export
    - sql.script.save
  
  DEVELOPER:
    - connection.view
    - sql.execute
    - data.view
    - data.edit
    - data.export
    - data.import
    - sql.script.save
    - sql.script.share
    - resource.view
    - resource.edit
  
  ADMIN:
    - admin.*
    - connection.*
    - sql.*
    - data.*
    - resource.*

ui_restrictions:
  pulsar_users:
    hide_features:
      - connection.create
      - connection.edit
      - connection.delete
      - admin.menu
      - server.settings
    
    readonly_features:
      - connection.view
    
    workspace_mode:
      show_navigation_tree: true
      show_query_editor: true
      show_result_viewer: true
      show_admin_panel: false
      show_user_management: false
```

## Conclusion

The RBAC integration strategy ensures that:

1. **Permissions are accurately transferred** from Pulsar to PulseQL via SSO token
2. **Backend enforcement** prevents unauthorized API access
3. **Frontend restrictions** provide appropriate UI based on permissions
4. **Real-time synchronization** keeps permissions up-to-date
5. **Audit logging** tracks all permission checks
6. **Flexible configuration** allows easy permission mapping adjustments

This approach provides defense-in-depth with both frontend and backend permission enforcement while maintaining a seamless user experience.
