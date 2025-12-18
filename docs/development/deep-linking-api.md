# Deep Linking API Documentation

## Overview

The Deep Linking API allows Pulsar to generate deep links that navigate users directly to specific resources in PulseQL workspace, such as tables, schemas, queries, or connections.

## Backend API

### Endpoint

```
POST /api/pulseql/generate-link
```

### Request

**Headers:**
- `Content-Type: application/json`
- Session cookie (automatically included by browser)

**Body:**

```json
{
  "target_type": "table|schema|query|connection|workspace",
  "connection_id": "string",     // Required for table, schema, connection
  "schema_name": "string",       // Required for table, schema
  "table_name": "string",        // Required for table
  "query": "string",             // Required for query (SQL text)
  "workspace_id": "string"       // Required for workspace
}
```

### Response

**Success (200 OK):**

```json
{
  "success": true,
  "deep_link": "https://pulseql.example.com/workspace?pulsar_link=table:conn123/public/users",
  "short_url": null
}
```

**Error (400 Bad Request / 401 Unauthorized):**

```json
{
  "success": false,
  "error": "Error message describing the issue"
}
```

### Examples

**Note**: These endpoints require a valid user session. The curl examples below will fail without proper session authentication. In a browser context, the session cookie is automatically included. For API testing, you would need to first authenticate and include the session cookie in your request.

#### Generate Table Link

```bash
# This example requires a valid session cookie
curl -X POST https://pulsar.example.com/api/pulseql/generate-link \
  -H "Content-Type: application/json" \
  -b "session_cookie=your_session_id" \
  -d '{
    "target_type": "table",
    "connection_id": "postgres-prod",
    "schema_name": "public",
    "table_name": "users"
  }'
```

#### Generate Query Link

```bash
# This example requires a valid session cookie
curl -X POST https://pulsar.example.com/api/pulseql/generate-link \
  -H "Content-Type: application/json" \
  -b "session_cookie=your_session_id" \
  -d '{
    "target_type": "query",
    "query": "SELECT * FROM users WHERE status = '\''active'\''"
  }'
```

## Frontend Integration

### Using DeepLinkButton Component

```tsx
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';

// In a table context menu
<DeepLinkButton
  targetType="table"
  connectionId={connection.id}
  schemaName={schema.name}
  tableName={table.name}
  label="Open in PulseQL"
/>

// In a schema browser
<DeepLinkButton
  targetType="schema"
  connectionId={connection.id}
  schemaName={schema.name}
  label="Query in PulseQL"
  mode="icon"
/>

// In a query result panel
<DeepLinkButton
  targetType="query"
  query={sqlQuery}
  label="Edit in PulseQL"
/>
```

### Using DeepLinkingService Directly

```typescript
import { useService } from '@cloudbeaver/core-di';
import { DeepLinkingService } from '@cloudbeaver/plugin-pulsar-integration';

function MyComponent() {
  const deepLinkingService = useService(DeepLinkingService);

  const handleGenerateLink = async () => {
    const result = await deepLinkingService.generateDeepLinkViaAPI({
      type: 'table',
      connectionId: 'my-connection',
      schemaName: 'public',
      tableName: 'users',
    });

    if (result.success) {
      console.log('Deep link:', result.deep_link);
    } else {
      console.error('Error:', result.error);
    }
  };

  return <button onClick={handleGenerateLink}>Generate Link</button>;
}
```

## Deep Link Format

Deep links use the `pulsar_link` URL parameter with the following format:

### Table Link
```
?pulsar_link=table:connectionId/schemaName/tableName
```

Example:
```
https://pulseql.example.com/workspace?pulsar_link=table:postgres-prod/public/users
```

### Schema Link
```
?pulsar_link=schema:connectionId/schemaName
```

Example:
```
https://pulseql.example.com/workspace?pulsar_link=schema:postgres-prod/public
```

### Query Link
```
?pulsar_link=query:base64EncodedSQL
```

Example:
```
https://pulseql.example.com/workspace?pulsar_link=query:U0VMRUNUICogRlJPTSB1c2Vycw==
```

### Connection Link
```
?pulsar_link=connection:connectionId
```

Example:
```
https://pulseql.example.com/workspace?pulsar_link=connection:postgres-prod
```

### Workspace Link
```
?pulsar_link=workspace:workspaceId
```

Example:
```
https://pulseql.example.com/workspace?pulsar_link=workspace:my-workspace-123
```

## Security Considerations

### Input Validation

All inputs are validated on the backend:

1. **Identifier Length**: Connection IDs, schema names, and table names are limited to 200 characters
2. **Query Length**: SQL queries are limited to 50KB
3. **XSS Prevention**: Inputs are checked for script tags and JavaScript URIs
4. **SQL Injection Prevention**: Basic checks for common SQL injection patterns

### Authentication

- Users must be authenticated with a valid session
- Session is validated before generating links
- User permissions are checked for resource access

### URL Encoding

- All identifiers are URL-encoded to prevent injection
- SQL queries are base64-encoded for safe transmission
- Special characters are properly escaped

## Configuration

### Backend Configuration

Add to `cloudbeaver.conf`:

```properties
# PulseQL base URL for deep link generation
pulsar.pulseql.baseUrl=https://pulseql.example.com/workspace
```

### Environment Variable

```bash
export PULSEQL_BASE_URL=https://pulseql.example.com/workspace
```

## Error Handling

### Common Errors

| Error Message | Cause | Solution |
|---------------|-------|----------|
| "User not authenticated" | No valid session | Ensure user is logged in |
| "PulseQL base URL not configured" | Missing configuration | Set `pulsar.pulseql.baseUrl` |
| "Target type is required" | Missing target_type | Include target_type in request |
| "connectionId and schemaName are required" | Missing required fields | Include all required fields |
| "Query exceeds maximum length" | SQL query too long | Reduce query size |
| "Invalid characters in connectionId" | Potential injection attempt | Use only alphanumeric and safe characters |

## Integration Examples

### Table Context Menu Integration

```tsx
// In NavigationTreeContextMenuService.ts
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';

const tableActions = [
  {
    id: 'open-in-pulseql',
    label: 'Open in PulseQL',
    icon: '/icons/link.svg',
    handler: () => {
      // Render DeepLinkButton or call service directly
    },
  },
];
```

### Schema Browser Integration

```tsx
// In SchemaBrowser.tsx
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';

function SchemaObjectActions({ object }) {
  if (object.type === 'table') {
    return (
      <DeepLinkButton
        targetType="table"
        connectionId={object.connectionId}
        schemaName={object.schemaName}
        tableName={object.name}
        mode="icon"
      />
    );
  }
  
  return null;
}
```

### Query Results Integration

```tsx
// In QueryResultsPanel.tsx
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';

function QueryResultsToolbar({ query }) {
  return (
    <div className="toolbar">
      <DeepLinkButton
        targetType="query"
        query={query}
        label="Edit in PulseQL"
      />
    </div>
  );
}
```

## Testing

### Manual Testing

1. **Test Table Link:**
   - Navigate to a table in Pulsar
   - Click "Open in PulseQL" button
   - Verify link is copied to clipboard
   - Paste link in browser
   - Verify PulseQL opens with the correct table

2. **Test Query Link:**
   - Execute a query in Pulsar
   - Click "Edit in PulseQL" button
   - Verify link opens PulseQL with query pre-loaded

3. **Test Permission Validation:**
   - Login as user with limited permissions
   - Try to generate link for restricted resource
   - Verify appropriate error message

### Automated Testing

```typescript
describe('DeepLinkingService', () => {
  it('should generate table link via API', async () => {
    const service = new DeepLinkingService();
    const result = await service.generateDeepLinkViaAPI({
      type: 'table',
      connectionId: 'test-conn',
      schemaName: 'public',
      tableName: 'users',
    });

    expect(result.success).toBe(true);
    expect(result.deep_link).toContain('pulsar_link=table:');
  });

  it('should handle errors gracefully', async () => {
    const service = new DeepLinkingService();
    const result = await service.generateDeepLinkViaAPI({
      type: 'table',
      // Missing required fields
    });

    expect(result.success).toBe(false);
    expect(result.error).toBeDefined();
  });
});
```

## Troubleshooting

### Link Not Working

1. Check PulseQL base URL configuration
2. Verify user has access to target resource
3. Check browser console for errors
4. Verify session is still valid

### Clipboard Copy Fails

1. Check browser permissions for clipboard access
2. Try using HTTPS (required for clipboard API)
3. Check if user clicked the button (clipboard requires user interaction)

### Session Timeout

1. Links generated are tied to user session
2. If session expires, user must re-authenticate
3. Consider implementing link expiration/refresh logic

## Future Enhancements

- **Short URL Service**: Generate compact URLs for sharing
- **Link Expiration**: Time-limited links for security
- **Link Analytics**: Track link usage and navigation patterns
- **Custom Actions**: Additional actions on deep link (e.g., auto-execute query)
- **Bookmarks**: Save frequently used deep links
