# Project Structure

**Document Version**: 1.0  
**Last Updated**: December 15, 2025

---

## 1. Repository Structure Overview

```
pulseql/
├── .github/                          # GitHub configuration
│   ├── agents/                       # Custom Copilot agent configurations
│   │   ├── project_manager.md
│   │   ├── senior_principal_architect.md
│   │   ├── senior_developer_1.md
│   │   ├── senior_developer_2.md
│   │   ├── senior_developer_3.md
│   │   ├── senior_developer_4.md
│   │   ├── senior_developer_5.md
│   │   ├── senior_test_engineer_1.md
│   │   ├── senior_test_engineer_2.md
│   │   └── senior_devops_engineer_1.md
│   └── workflows/                    # GitHub Actions workflows
│       ├── build.yml
│       ├── test.yml
│       └── deploy.yml
│
├── apps/                             # Standalone applications
│   └── h2-query-executor/            # H2 database query executor
│       ├── pom.xml
│       └── src/
│
├── config/                           # Configuration files
│   ├── core/
│   │   ├── cloudbeaver.conf          # Main server configuration
│   │   ├── initial-data.conf         # Initial data setup
│   │   └── logback.xml               # Logging configuration
│   └── GlobalConfiguration/          # Global settings
│
├── deploy/                           # Deployment scripts and artifacts
│   ├── build.bat                     # Windows build script
│   ├── build.sh                      # Linux build script
│   ├── cloudbeaver/                  # Deployment package
│   │   ├── conf/                     # Server configuration
│   │   ├── server/                   # Server bundles
│   │   ├── web/                      # Frontend assets
│   │   └── workspace/                # Workspace directory
│   ├── docker/                       # Docker configurations
│   │   ├── cloudbeaver-ce/
│   │   └── base-java/
│   └── scripts/                      # Utility scripts
│
├── docs/                             # Documentation
│   ├── README.md                     # Documentation index
│   ├── 01-architecture-overview.md   # Architecture documentation
│   ├── 02-sso-integration-strategy.md
│   ├── ...                           # Additional integration docs
│   ├── development/                  # Development documentation
│   │   ├── README.md
│   │   ├── 01-integration-architecture.md
│   │   ├── 02-project-structure.md   # (This file)
│   │   └── ...
│   ├── PMP_integration/              # Project management docs
│   │   ├── README.md
│   │   ├── 01-project-charter.md
│   │   ├── 02-sprint-planning.md
│   │   └── ...
│   └── pulsar_work_prompts/          # Agent work prompts
│       ├── README.md
│       └── ...
│
├── server/                           # Backend server (Java/OSGi)
│   ├── pom.xml                       # Parent POM
│   ├── bundles/                      # OSGi bundles
│   │   ├── pom.xml
│   │   ├── io.cloudbeaver.model/     # Data models
│   │   ├── io.cloudbeaver.product.ce/
│   │   ├── io.cloudbeaver.server/    # Core server
│   │   ├── io.cloudbeaver.server.ce/ # CE implementation
│   │   ├── io.cloudbeaver.service.auth/        # Auth service
│   │   ├── io.cloudbeaver.service.security/    # Security service
│   │   ├── io.cloudbeaver.service.admin/       # Admin service
│   │   └── ...
│   ├── drivers/                      # JDBC drivers
│   │   ├── pom.xml
│   │   ├── postgresql/
│   │   ├── mysql/
│   │   ├── oracle/
│   │   └── ...
│   ├── features/                     # Eclipse features
│   │   ├── pom.xml
│   │   └── ...
│   ├── product/                      # Product assembly
│   │   ├── pom.xml
│   │   ├── aggregate/
│   │   └── web-server/
│   └── test/                         # Server tests
│       └── io.cloudbeaver.test.platform/
│
├── webapp/                           # Frontend (TypeScript/React)
│   ├── package.json                  # Root package config
│   ├── eslint.config.mjs             # ESLint configuration
│   ├── vitest.config.ts              # Vitest test configuration
│   ├── common-react/                 # Shared React components
│   │   └── @dbeaver/
│   │       └── ui-kit/               # UI component library
│   ├── common-typescript/            # Shared TypeScript utilities
│   └── packages/                     # Application packages
│       ├── core-authentication/      # Authentication module
│       ├── core-connections/         # Connection management
│       ├── core-di/                  # Dependency injection
│       ├── core-routing/             # Navigation/routing
│       ├── plugin-sql-editor/        # SQL editor plugin
│       ├── plugin-navigation-tree/   # Database navigator
│       ├── plugin-data-viewer/       # Data grid viewer
│       ├── plugin-pulsar-integration/  # ⭐ PULSAR INTEGRATION
│       │   ├── package.json
│       │   ├── tsconfig.json
│       │   └── src/
│       │       ├── index.ts
│       │       ├── module.ts
│       │       ├── WorkspaceModeService.ts
│       │       ├── PulsarSSOService.ts
│       │       ├── PulsarPermissionService.ts
│       │       ├── PulsarUICustomizer.ts
│       │       ├── PulsarIntegrationBootstrap.ts
│       │       ├── components/
│       │       │   ├── BackToPulsarButton.tsx
│       │       │   ├── SSOErrorNotification.tsx
│       │       │   └── PulsarWorkspaceHeader.tsx
│       │       └── themes/
│       │           ├── pulsar-light.scss
│       │           └── pulsar-dark.scss
│       ├── product-default/          # Main product build
│       └── storybook/                # UI component documentation
│
├── copilot-instructions.md           # AI coding assistant instructions
├── lefthook.yml                      # Git hooks configuration
├── pulsarsolara-dbeaver-integration-plan.md
├── README.md                         # Project README
└── SECURITY.md                       # Security policy
```

---

## 2. Key Directories Explained

### 2.1 Server Directory (`/server`)

The backend is built on OSGi (Eclipse Equinox) with a modular bundle architecture.

```
server/
├── bundles/          # OSGi plugin bundles
│   ├── io.cloudbeaver.model/
│   │   ├── META-INF/
│   │   │   └── MANIFEST.MF        # OSGi manifest
│   │   ├── pom.xml
│   │   ├── plugin.xml             # Eclipse plugin config
│   │   └── src/
│   │       └── io/cloudbeaver/model/
│   │           ├── WebSession.java
│   │           └── ...
│   │
│   └── io.cloudbeaver.service.auth/
│       ├── schema/                 # GraphQL schema
│       │   └── auth.graphqls
│       └── src/
│           └── io/cloudbeaver/service/auth/
│               ├── DBWServiceAuth.java
│               └── ...
│
├── drivers/          # JDBC driver bundles
│   └── postgresql/
│       ├── pom.xml
│       └── lib/
│           └── postgresql-*.jar
│
└── product/          # Final product assembly
    └── web-server/
        └── target/            # Build output
```

**Important Files**:
- `MANIFEST.MF` - OSGi bundle metadata
- `plugin.xml` - Eclipse extension point definitions
- `*.graphqls` - GraphQL schema definitions
- `pom.xml` - Maven build configuration

### 2.2 Webapp Directory (`/webapp`)

The frontend uses a monorepo structure with Yarn workspaces.

```
webapp/
├── package.json              # Root workspace config
├── packages/
│   ├── core-*/               # Core framework packages
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   └── src/
│   │       ├── index.ts      # Public exports
│   │       ├── module.ts     # DI module registration
│   │       └── ...
│   │
│   ├── plugin-*/             # Feature plugins
│   │   └── ... (same structure)
│   │
│   └── product-default/      # Main application
│       ├── package.json
│       ├── index.html
│       └── src/
│           └── index.ts      # Application entry
```

**Package Naming Conventions**:
- `core-*` - Foundation services (authentication, routing, etc.)
- `plugin-*` - Feature modules (SQL editor, data viewer, etc.)
- `product-*` - Application assemblies

### 2.3 Plugin-Pulsar-Integration (`/webapp/packages/plugin-pulsar-integration`)

This is the main integration package being developed.

```
plugin-pulsar-integration/
├── package.json              # Package dependencies
├── tsconfig.json             # TypeScript configuration
├── src/
│   ├── index.ts              # Public API exports
│   ├── module.ts             # DI module registration
│   │
│   ├── WorkspaceModeService.ts     # URL parameter handling
│   ├── PulsarSSOService.ts         # JWT token validation
│   ├── PulsarPermissionService.ts  # Permission management
│   ├── PulsarUICustomizer.ts       # UI visibility control
│   ├── PulsarIntegrationBootstrap.ts # Initialization
│   │
│   ├── components/                  # React components
│   │   ├── index.ts
│   │   ├── BackToPulsarButton.tsx
│   │   ├── SSOErrorNotification.tsx
│   │   └── PulsarWorkspaceHeader.tsx
│   │
│   └── themes/                      # SCSS themes
│       ├── pulsar-light.scss
│       └── pulsar-dark.scss
│
└── __tests__/                       # Unit tests
    ├── WorkspaceModeService.test.ts
    └── PulsarSSOService.test.ts
```

---

## 3. Configuration Files

### 3.1 Root Configuration

| File | Purpose |
|------|---------|
| `package.json` | Yarn workspaces configuration |
| `eslint.config.mjs` | ESLint rules |
| `vitest.config.ts` | Test framework config |
| `lefthook.yml` | Git hooks |

### 3.2 Server Configuration

| File | Location | Purpose |
|------|----------|---------|
| `cloudbeaver.conf` | `/config/core/` | Main server settings |
| `initial-data.conf` | `/config/core/` | Initial data setup |
| `logback.xml` | `/config/core/` | Logging configuration |
| `pom.xml` | `/server/` | Maven parent config |

### 3.3 Build Configuration

| File | Location | Purpose |
|------|----------|---------|
| `build.bat` | `/deploy/` | Windows build script |
| `build.sh` | `/deploy/` | Linux build script |
| `docker-compose.yml` | `/deploy/docker/` | Docker deployment |

---

## 4. Build Artifacts

After building, artifacts are placed in:

```
deploy/cloudbeaver/
├── conf/                   # Configuration files
│   ├── cloudbeaver.conf
│   └── product.conf
├── server/                 # Java bundles
│   ├── plugins/            # OSGi plugins
│   └── features/           # Eclipse features
├── web/                    # Frontend assets
│   ├── index.html
│   ├── manifest.json
│   └── assets/
│       ├── js/
│       ├── css/
│       └── fonts/
└── workspace/              # Runtime workspace
    ├── .data/
    └── GlobalConfiguration/
```

---

## 5. Module Dependencies

### 5.1 Frontend Module Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Frontend Module Dependencies                      │
└─────────────────────────────────────────────────────────────────────┘

                    product-default
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
  plugin-sql-editor  plugin-data-viewer  plugin-pulsar-integration
        │                 │                 │
        └────────┬────────┴─────────────────┘
                 │
        ┌────────┼────────┐
        │        │        │
        ▼        ▼        ▼
  core-auth  core-di  core-routing
        │        │        │
        └────────┴────────┘
                 │
                 ▼
          @cloudbeaver/core-root
```

### 5.2 Backend Bundle Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Backend Bundle Dependencies                       │
└─────────────────────────────────────────────────────────────────────┘

             io.cloudbeaver.product.ce
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
io.cloudbeaver   io.cloudbeaver   io.cloudbeaver
  .server.ce      .service.auth    .service.security
        │               │               │
        └───────────────┼───────────────┘
                        │
                        ▼
              io.cloudbeaver.server
                        │
                        ▼
              io.cloudbeaver.model
                        │
                        ▼
              org.jkiss.dbeaver.*
```

---

## 6. Development Paths

### 6.1 When Adding a Frontend Feature

1. Create or modify package in `/webapp/packages/`
2. Update `package.json` dependencies
3. Register in `module.ts`
4. Export from `index.ts`
5. Add tests in `__tests__/`

### 6.2 When Adding a Backend Feature

1. Create or modify bundle in `/server/bundles/`
2. Update `MANIFEST.MF` for dependencies
3. Add extension in `plugin.xml`
4. Update GraphQL schema if needed
5. Add tests in `/server/test/`

### 6.3 When Adding Configuration

1. Add to appropriate config file in `/config/`
2. Update documentation
3. Add environment variable support if needed

---

## 7. Naming Conventions

### 7.1 Files

| Type | Convention | Example |
|------|------------|---------|
| TypeScript service | PascalCase + Service.ts | `WorkspaceModeService.ts` |
| React component | PascalCase.tsx | `BackToPulsarButton.tsx` |
| SCSS file | kebab-case.scss | `pulsar-light.scss` |
| Test file | *.test.ts | `PulsarSSOService.test.ts` |
| Java class | PascalCase.java | `WebSession.java` |

### 7.2 Directories

| Type | Convention | Example |
|------|------------|---------|
| Frontend package | kebab-case | `plugin-pulsar-integration` |
| Backend bundle | dot-notation | `io.cloudbeaver.service.auth` |
| Documentation | kebab-case | `PMP_integration` |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-15 | Technical Writer | Initial structure documentation |
