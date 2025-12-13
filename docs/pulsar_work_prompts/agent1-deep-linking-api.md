# Pulsar Agent 1: Deep Linking API Implementation

## Objective
Implement backend API endpoints in Pulsar to generate deep links for PulseQL workspace navigation.

## Context
PulseQL has implemented a Deep Linking Service that allows users to navigate directly to specific tables, schemas, and queries from Pulsar. We need Pulsar-side endpoints to generate these links.

## Requirements

### 1. Deep Link Generation API

Create REST endpoint: `POST /api/pulseql/generate-link`

**Request Body:**
```json
{
  "target_type": "table" | "schema" | "query" | "connection",
  "connection_id": "string",
  "schema_name": "string",  // optional
  "table_name": "string",   // optional
  "query": "string",        // optional for query type
  "workspace_id": "string"  // optional
}
```

**Response:**
```json
{
  "success": true,
  "deep_link": "https://pulseql.example.com/workspace?pulsar_link=table:connId/schema/table",
  "short_url": "https://pulsar.example.com/q/abc123"  // optional short URL
}
```

### 2. Integration Points

**A. Table Context Menu**
- Add "Open in PulseQL" option to table context menu
- Generate deep link for selected table
- Open link in new tab or embedded iframe

**B. Schema Browser**
- Add "Query in PulseQL" button to schema objects
- Generate appropriate deep link based on object type

**C. Query Results**
- Add "Edit in PulseQL" button to query result panels
- Pass current SQL query as deep link

### 3. UI Components

Create React components:

**DeepLinkButton.tsx**
```typescript
interface DeepLinkButtonProps {
  targetType: 'table' | 'schema' | 'query';
  connectionId: string;
  schemaName?: string;
  tableName?: string;
  query?: string;
  label?: string;
}
```

**Features:**
- Generate and open deep link
- Copy link to clipboard
- Show success notification

### 4. Short URL Service (Optional)

Create short URL service:
- `POST /api/shorturl/create` - Create short URL
- `GET /api/shorturl/{id}` - Redirect to full URL
- Store mappings in database with expiration

### 5. Session Context

Include session context in deep links:
- Current user permissions
- Connection credentials (encrypted)
- Workspace preferences

## Implementation Tasks

1. **Backend API** (4-6 hours)
   - Create DeepLinkService.java
   - Implement generateDeepLink() method
   - Add REST endpoint
   - Add short URL service (optional)

2. **Frontend Integration** (3-4 hours)
   - Create DeepLinkButton component
   - Integrate into table context menu
   - Integrate into schema browser
   - Add to query result panels

3. **Testing** (2-3 hours)
   - Unit tests for link generation
   - Integration tests for UI components
   - E2E tests for navigation flow

## Expected Deliverables

1. Java service class: `DeepLinkService.java`
2. REST controller: `DeepLinkController.java`
3. React component: `DeepLinkButton.tsx`
4. Integration in existing UI components
5. Unit and integration tests

## API Specifications

### Generate Deep Link Endpoint

```java
@RestController
@RequestMapping("/api/pulseql")
public class DeepLinkController {
    
    @PostMapping("/generate-link")
    public ResponseEntity<DeepLinkResponse> generateLink(
        @RequestBody DeepLinkRequest request,
        @AuthenticationPrincipal User user
    ) {
        // Implementation
    }
}
```

### Request Validation

- Validate user has access to target resource
- Validate connection exists
- Validate schema/table exists (for table links)
- Sanitize SQL query (for query links)

### Security Considerations

- Include SSO token in generated link
- Validate user permissions before generating link
- Set link expiration (optional)
- Log link generation for audit trail

## Dependencies

- Spring Boot REST
- Apache Pulsar client (for accessing workspace configuration)
- PulseQL integration configuration

## Success Criteria

- Deep links generate successfully from Pulsar UI
- Links open correct target in PulseQL
- User permissions are preserved
- Short URLs work (if implemented)
- All tests pass

## Estimated Effort

- Backend: 6-8 hours
- Frontend: 4-6 hours
- Testing: 3-4 hours
- **Total: 13-18 hours (1.5-2 days)**
