# Query Sharing Service Bundle

## Overview

The Query Sharing Service provides backend infrastructure for sharing, collaborating on, and managing SQL queries in PulseQL. This service enables users to:

- Create and share SQL queries with team members
- Control access through granular permissions
- Track query version history
- Comment on and discuss queries
- Organize queries with favorites and tags
- Monitor query execution analytics

## Architecture

### Database Schema

The service uses the following tables:

- **CB_SHARED_QUERIES**: Main table storing query metadata and SQL content
- **CB_QUERY_PERMISSIONS**: Fine-grained access control
- **CB_QUERY_COMMENTS**: Threaded comments on queries
- **CB_QUERY_VERSIONS**: Complete version history
- **CB_QUERY_FAVORITES**: User-specific favorites
- **CB_QUERY_EXECUTIONS**: Analytics and execution history

Schema is defined in `db/cb_schema_update_27.sql`

### Service Layer

- **DBWServiceQuerySharing**: Service interface defining all operations
- **WebServiceQuerySharing**: Implementation with JDBC-based data access
- **WebServiceBindingQuerySharing**: GraphQL service binding

### Models

Located in `io.cloudbeaver.service.query.sharing.model`:

- `SharedQuery`: Main query entity
- `QueryPermission`: Access control
- `QueryComment`: Comments and discussions
- `QueryVersion`: Version history
- `QueryFavorite`: User favorites
- `QueryExecution`: Execution analytics

### DTOs

Located in `io.cloudbeaver.service.query.sharing.dto`:

- `SharedQueryRequest`: Query create/update request
- `QueryFilter`: Query listing filters

## Features

### Implemented

✅ Query CRUD operations
✅ Permission-based access control
✅ Query visibility levels (PRIVATE, TEAM, PUBLIC)
✅ Version tracking for all changes
✅ Query forking
✅ Owner-based permissions

### Partially Implemented

⚠️ Permission management endpoints (interface defined, implementation needed)
⚠️ Comment management (interface defined, implementation needed)
⚠️ Favorites (interface defined, implementation needed)
⚠️ Analytics (interface defined, implementation needed)

### Planned

❌ WebSocket real-time collaboration
❌ Notification system
❌ Advanced search and filtering
❌ Query templates
❌ Team-based visibility enforcement

## API

### GraphQL Schema

Defined in `schema/service.query.sharing.graphqls`

#### Queries

```graphql
listSharedQueries(filter: QueryFilterInput): QueryList!
getSharedQuery(queryId: ID!): SharedQuery
getQueryPermissions(queryId: ID!): [QueryPermission!]!
getQueryComments(queryId: ID!): [QueryComment!]!
getQueryVersions(queryId: ID!): [QueryVersion!]!
getUserFavoriteQueries: [SharedQuery!]!
getQueryExecutions(queryId: ID!): [QueryExecution!]!
```

#### Mutations

```graphql
createSharedQuery(input: SharedQueryInput!): SharedQuery!
updateSharedQuery(queryId: ID!, input: SharedQueryInput!): SharedQuery!
deleteSharedQuery(queryId: ID!): Boolean!
forkSharedQuery(queryId: ID!, newName: String!): SharedQuery!
grantQueryPermission(queryId: ID!, permission: QueryPermissionInput!): QueryPermission!
revokeQueryPermission(permissionId: ID!): Boolean!
# ... and more
```

## Security

### Permission Model

- **query.create**: Create new shared queries
- **query.view**: View specific queries (object-level)
- **query.edit**: Edit specific queries (object-level)
- **query.delete**: Delete specific queries (object-level)
- **query.execute**: Execute specific queries (object-level)
- **query.comment**: Comment on queries (object-level)
- **query.share**: Share queries with others (object-level)

### Access Control

1. **Owner**: Full access to their own queries
2. **Explicit Permissions**: Granted via query_permissions table
3. **Visibility-based**:
   - PRIVATE: Only owner can see
   - TEAM: Team members can see (if implemented)
   - PUBLIC: All authenticated users can see

### SQL Injection Prevention

All database access uses `PreparedStatement` with parameterized queries.

## Usage Example

### Creating a Query

```javascript
mutation {
  createSharedQuery(input: {
    name: "Daily Sales Report"
    description: "Shows daily sales by region"
    sqlQuery: "SELECT region, SUM(amount) FROM sales WHERE date = CURRENT_DATE GROUP BY region"
    visibility: TEAM
    teamId: "analytics-team"
    tags: ["sales", "reporting", "daily"]
  }) {
    queryId
    name
    createdAt
  }
}
```

### Listing Queries

```javascript
query {
  listSharedQueries(filter: {
    visibility: PUBLIC
    searchText: "sales"
    limit: 20
    sortBy: "created_at"
    sortOrder: "DESC"
  }) {
    queries {
      queryId
      name
      description
      createdBy
      createdAt
    }
    totalCount
  }
}
```

### Forking a Query

```javascript
mutation {
  forkSharedQuery(
    queryId: "123e4567-e89b-12d3-a456-426614174000"
    newName: "My Custom Sales Report"
  ) {
    queryId
    name
    sqlQuery
  }
}
```

## Development

### Building

```bash
cd /path/to/pulseql/server
mvn clean install
```

### Testing

```bash
mvn test
```

### Database Migration

The schema migration (`cb_schema_update_27.sql`) will be automatically applied when the application starts, following CloudBeaver's schema versioning system.

## Future Enhancements

1. **WebSocket Integration**: Real-time collaborative editing
2. **Advanced Search**: Full-text search on query content
3. **Query Templates**: Parameterized query templates
4. **Scheduled Execution**: Cron-like query scheduling
5. **Export/Import**: Bulk query export/import
6. **Query Collections**: Organize related queries
7. **Diff Viewer**: Visual diff between versions
8. **Email Notifications**: Query share notifications
9. **Query Validation**: Syntax checking before save
10. **Execution Limits**: Resource usage controls

## Contributing

When adding new features:

1. Update the GraphQL schema
2. Add model classes if needed
3. Implement service methods
4. Add permission checks
5. Update this README
6. Add tests

## License

See main project LICENSE file.
