# PulseQL Architecture Overview

## Executive Summary

This document provides a comprehensive architectural overview of PulseQL (CloudBeaver Community Edition) to facilitate its integration with the Pulsar application. PulseQL is a web-based database management tool that can serve as a query workspace for Pulsar users.

## Current PulseQL Architecture

### Technology Stack

#### Backend (Java-based)
- **Framework**: OSGi-based modular architecture
- **Web Server**: Eclipse Jetty
- **API Layer**: GraphQL API (schema-based)
- **Authentication Service**: `io.cloudbeaver.service.auth`
- **Security Service**: `io.cloudbeaver.service.security`
- **Session Management**: Built-in session handling with WebSession
- **Database Access**: DBeaver core libraries for database connectivity

#### Frontend (TypeScript/React)
- **Framework**: React 19
- **State Management**: MobX
- **Build Tool**: Vite 7
- **Module System**: Monorepo with Yarn workspaces
- **UI Architecture**: Plugin-based modular system
- **Key Packages**:
  - `core-authentication`: Authentication and user management
  - `core-connections`: Database connection management
  - `plugin-sql-editor`: SQL editor functionality
  - `plugin-navigation-tree`: Database object explorer
  - `plugin-data-viewer`: Query result visualization
  - `product-default`: Main application product

### Core Components

#### 1. Server Architecture (`/server`)

**Main Bundles**:
- `io.cloudbeaver.server`: Core server infrastructure
- `io.cloudbeaver.server.ce`: Community Edition implementation
- `io.cloudbeaver.product.ce`: Product definition
- `io.cloudbeaver.model`: Data models and DTOs
- `io.cloudbeaver.service.auth`: Authentication services
- `io.cloudbeaver.service.security`: Security and authorization
- `io.cloudbeaver.service.admin`: Administration services
- `io.cloudbeaver.service.rm`: Resource management

**Key Classes**:
- `CBApplication`: Main application entry point
- `CBJettyServer`: Jetty web server configuration
- `DBWServiceAuth`: Authentication service interface
- `WebSession`: User session management

#### 2. Frontend Architecture (`/webapp`)

**Package Structure**:
- **Core Packages**: Foundation services (DI, routing, resources, etc.)
- **Plugin Packages**: Feature modules (authentication, connections, SQL editor)
- **Product Packages**: Application assembly and configuration

**Key Modules**:
- `core-authentication`: User authentication and session management
- `core-di`: Dependency injection container
- `core-routing`: Navigation and routing
- `plugin-sql-editor`: SQL editing and execution
- `plugin-navigation-tree`: Database object explorer
- `plugin-connections`: Connection management

### Authentication & Authorization

#### Current Authentication Model

**Authentication Providers**:
- Local authentication (username/password)
- LDAP authentication
- Federated authentication (SSO via SAML/OAuth)

**Authentication Flow**:
1. User provides credentials via login form
2. `DBWServiceAuth.authLogin()` processes authentication
3. Session created with `WebSession`
4. User info stored in `UserInfoResource`
5. Auth tokens tracked via `UserAuthToken`

**Authorization Model**:
- Permission-based access control
- User roles and teams
- Resource-level permissions
- Object-level access control

**Key GraphQL Types**:
```graphql
type UserInfo {
    userId: ID!
    displayName: String
    authRole: ID
    authTokens: [UserAuthToken!]!
    linkedAuthProviders: [String!]!
    metaParameters: Object!
    configurationParameters: Object!
    teams: [UserTeamInfo!]!
    isAnonymous: Boolean!
}

type UserAuthToken {
    authProvider: ID!
    authConfiguration: ID
    loginTime: DateTime!
    userId: String!
    displayName: String!
    message: String
    origin: ObjectOrigin!
}
```

### Session Management

**WebSession Features**:
- HTTP session binding
- User authentication state
- Connection context management
- Resource lifecycle management
- Session expiration handling

**Frontend Session**:
- `UserInfoResource`: Cached user information
- `AppAuthService`: Authentication state management
- Automatic session refresh
- Session expiration detection

### Database Connectivity

**Connection Architecture**:
- Multi-database support (via DBeaver drivers)
- Connection pooling
- Execution context management
- Transaction management
- Connection credentials storage

**Connection Execution Context**:
- Isolated query execution environments
- Connection state management
- Active connection tracking
- Resource cleanup

## Deployment Architecture

### Current Deployment Options

1. **Standalone Server**
   - Java application with embedded Jetty
   - Serves both backend API and frontend assets
   - Single deployment unit

2. **Docker Container**
   - Official Docker image available
   - Pre-configured with drivers
   - Environment-based configuration

3. **Embedded Mode**
   - Can be embedded in other Java applications
   - Configurable via OSGi bundles

### Configuration

**Server Configuration** (`CBServerConfig`):
- Server port and host
- Authentication settings
- Database access controls
- Session configuration
- Resource limits

**Web Configuration** (`CBWebServerConfig`):
- Frontend settings
- CORS configuration
- Static resource serving
- API endpoint configuration

## Security Model

### Current Security Features

1. **Authentication**:
   - Multiple authentication providers
   - Federated authentication support
   - Session-based authentication
   - Token-based API access

2. **Authorization**:
   - Role-based access control (RBAC)
   - Permission-based feature access
   - Connection-level permissions
   - Resource-level permissions

3. **Session Security**:
   - HTTP session management
   - Session timeout
   - Secure session cookies
   - CSRF protection (via GraphQL patterns)

4. **Data Security**:
   - Encrypted credential storage
   - Secure parameter transmission
   - SQL injection prevention
   - Query result access control

## UI Component Structure

### Main UI Areas

1. **Top App Bar** (`plugin-top-app-bar`)
   - Application branding
   - User profile menu
   - Settings access
   - Navigation controls

2. **Navigation Tree** (`plugin-navigation-tree`)
   - Database object explorer
   - Connection listing
   - Hierarchical object navigation
   - Context menus

3. **SQL Editor** (`plugin-sql-editor`)
   - Code editor with syntax highlighting
   - Query execution
   - Multiple query tabs
   - Auto-completion

4. **Data Viewer** (`plugin-data-viewer`)
   - Result set display
   - Data editing
   - Export functionality
   - Filtering and sorting

5. **Connection Management** (`plugin-connections`)
   - Connection creation
   - Connection editing
   - Connection testing
   - Driver management

### Screen Components

**SQL Editor Screen** (`plugin-sql-editor-screen`):
- Standalone SQL editor view
- Execution context binding
- Query result display
- Database object exploration

## Integration Points for Pulsar

### Potential Integration Layers

1. **Authentication Layer**
   - Custom authentication provider
   - Session synchronization
   - User profile mapping

2. **Authorization Layer**
   - RBAC integration
   - Permission synchronization
   - Access control enforcement

3. **UI Embedding Layer**
   - iFrame integration
   - URL parameter passing
   - Cross-origin communication
   - UI customization

4. **API Layer**
   - GraphQL API access
   - Custom API endpoints
   - Webhook integration
   - Event notifications

5. **Configuration Layer**
   - Dynamic configuration
   - Feature toggling
   - UI customization
   - Branding customization

## Technical Constraints

### Current Limitations

1. **Session Management**:
   - Uses standard HTTP sessions
   - No built-in SSO token validation
   - Session state server-side

2. **UI Customization**:
   - Plugin-based architecture
   - Requires rebuild for deep customization
   - Limited runtime configuration

3. **Authentication**:
   - Federated auth requires redirect flow
   - No direct token validation API
   - Provider-based authentication model

4. **Deployment**:
   - Java-based backend required
   - Cannot run as pure SPA
   - Requires server infrastructure

### Advantages for Integration

1. **Modular Architecture**:
   - Plugin-based system
   - OSGi modularity
   - Clear separation of concerns

2. **GraphQL API**:
   - Well-defined schema
   - Strongly typed
   - Extensible

3. **Authentication Flexibility**:
   - Multiple provider support
   - Federated auth capability
   - Customizable auth flow

4. **UI Framework**:
   - Modern React architecture
   - Component-based
   - Customizable themes

## Next Steps

The following integration analysis documents will address:

1. **SSO Integration Strategy**: Technical approach for seamless authentication
2. **RBAC Integration**: Mapping Pulsar permissions to PulseQL
3. **UI Customization Strategy**: Hiding/showing UI elements based on context
4. **Deployment Architecture**: Infrastructure and deployment models
5. **API Integration Specification**: Communication protocols and APIs
6. **Implementation Roadmap**: Phased implementation approach
7. **Security Considerations**: Security review and hardening

## References

- CloudBeaver GitHub: https://github.com/dbeaver/cloudbeaver
- CloudBeaver Wiki: https://github.com/dbeaver/cloudbeaver/wiki
- GraphQL API Schema: `/server/bundles/*/schema/*.graphqls`
- Authentication Service: `/server/bundles/io.cloudbeaver.service.auth`
- Frontend Core: `/webapp/packages/core-*`
