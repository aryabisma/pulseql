# Pulsar Integration Plugin - Implementation Documentation

**Date**: December 13, 2025  
**Version**: 1.0  
**Status**: Initial Implementation

## Overview

This document tracks the implementation of the Pulsar Integration Plugin for PulseQL, which enables seamless integration with the Pulsar application through workspace mode customization, custom theming, and UI adjustments.

## Implementation Status

### Phase 1: Foundation ✅ COMPLETED

#### Plugin Structure Created
- [x] Created `plugin-pulsar-integration` package at `webapp/packages/plugin-pulsar-integration/`
- [x] Set up package.json with proper dependencies
- [x] Configured TypeScript compilation with tsconfig.json
- [x] Added .gitignore for build artifacts

**Location**: `/webapp/packages/plugin-pulsar-integration/`

**Key Files**:
- `package.json` - Package configuration and dependencies
- `tsconfig.json` - TypeScript compilation settings
- `.gitignore` - Git ignore patterns

### Phase 2: Core Services ✅ COMPLETED

#### WorkspaceModeService
Manages workspace mode configuration from URL parameters.

**Location**: `webapp/packages/plugin-pulsar-integration/src/WorkspaceModeService.ts`

**Features**:
- URL parameter parsing for mode configuration
- Support for three modes: `standalone`, `pulsar`, `embedded`
- Configuration options:
  - `mode` - Workspace mode (standalone/pulsar/embedded)
  - `theme` - Theme name (pulsar-light/pulsar-dark)
  - `hide_nav` - Comma-separated list of navigation items to hide
  - `readonly_connections` - Make connections read-only
  - `workspace_id` - Auto-connect to specific workspace
  - `auto_connect` - Enable auto-connection
  - `hide_header` - Hide application header
  - `hide_footer` - Hide application footer
  - `brand_logo` - Custom logo URL
  - `brand_title` - Custom title
  - `brand_color` - Custom primary color
  - `return_url` - URL to return to Pulsar

**URL Example**:
```
https://pulseql.example.com/workspace?
  mode=pulsar&
  theme=pulsar-light&
  hide_nav=admin,settings,connections&
  readonly_connections=true&
  brand_title=Pulsar%20Query%20Workspace&
  brand_color=%231976D2&
  return_url=https://pulsar.example.com
```

**API**:
- `initializeFromURL()` - Parse and apply URL parameters
- `isPulsarMode` - Check if in Pulsar mode
- `isEmbeddedMode` - Check if in embedded mode
- `readonlyConnections` - Check if connections are read-only
- `shouldHideNavItem(itemId)` - Check if nav item should be hidden
- `getWorkspaceId()` - Get workspace ID to connect to
- `shouldAutoConnect()` - Check if should auto-connect
- `getCustomBranding()` - Get custom branding config
- `getThemeName()` - Get theme name

#### PulsarIntegrationBootstrap
Bootstrap service that initializes the plugin on application startup.

**Location**: `webapp/packages/plugin-pulsar-integration/src/PulsarIntegrationBootstrap.ts`

**Features**:
- Initializes WorkspaceModeService from URL parameters
- Applies custom branding (title, colors, logo)
- Applies theme via data attributes
- Stores return URL for "Back to Pulsar" functionality

**Initialization Flow**:
1. Parse URL parameters
2. Apply Pulsar mode customizations if in Pulsar mode
3. Set document title from branding
4. Apply primary color as CSS variable
5. Store return URL in session storage
6. Apply theme to document body

#### Module Registration
Registers the plugin with CloudBeaver's module system.

**Location**: `webapp/packages/plugin-pulsar-integration/src/module.ts`

**Configuration**:
- Registers `WorkspaceModeService` as singleton
- Registers `PulsarIntegrationBootstrap` as Bootstrap
- Named `@cloudbeaver/plugin-pulsar-integration`

### Phase 3: Theme Implementation ✅ COMPLETED

#### Pulsar Light Theme
Light mode theme matching Pulsar's visual style.

**Location**: `webapp/packages/plugin-pulsar-integration/src/themes/pulsar-light.scss`

**Colors**:
- Primary: `#1976D2` (Pulsar blue)
- Secondary: `#f2f2f2` (Light gray)
- Background: `#f5f5f5` (Very light gray)
- Surface: `#ffffff` (White)
- Positive: `#388E3C` (Green - Pulsar secondary)
- Negative: `#e73e52` (Red)
- Status: `#ff9900` (Orange)

**Typography**:
- Based on CloudBeaver defaults
- Consistent with Pulsar application

#### Pulsar Dark Theme
Dark mode theme matching Pulsar's visual style.

**Location**: `webapp/packages/plugin-pulsar-integration/src/themes/pulsar-dark.scss`

**Colors**:
- Primary: `#1976D2` (Same Pulsar blue for consistency)
- Secondary: `#2D2D2D` (Dark gray)
- Background: `#121212` (Very dark)
- Surface: `#1E1E1E` (Dark surface)
- Positive: `#4CAF50` (Brighter green for dark mode)
- Negative: `#F44336` (Brighter red)
- Link: `#BBDEFB` (Light blue for visibility)

## Architecture

### Plugin Architecture

The plugin follows CloudBeaver's modular architecture:

```
plugin-pulsar-integration/
├── package.json          # Package configuration
├── tsconfig.json         # TypeScript config
├── .gitignore           # Git ignore patterns
└── src/
    ├── index.ts         # Public exports
    ├── module.ts        # Module registration
    ├── WorkspaceModeService.ts      # Mode management service
    ├── PulsarIntegrationBootstrap.ts # Bootstrap service
    └── themes/
        ├── pulsar-light.scss        # Light theme
        └── pulsar-dark.scss         # Dark theme
```

### Service Dependencies

```
PulsarIntegrationBootstrap
  └─> WorkspaceModeService
```

### Integration Points

The plugin integrates with CloudBeaver through:

1. **Dependency Injection**: Uses `@cloudbeaver/core-di` for service registration
2. **Bootstrap System**: Hooks into app initialization via Bootstrap class
3. **MobX State**: Observable state management for reactive UI updates
4. **Theme System**: Extends `@cloudbeaver/core-theming` with Pulsar themes

## Usage

### Basic Usage

To use the plugin in Pulsar mode:

```
https://pulseql.example.com/workspace?mode=pulsar&theme=pulsar-light
```

### With Custom Branding

```
https://pulseql.example.com/workspace?
  mode=pulsar&
  theme=pulsar-light&
  brand_title=Pulsar%20Query%20Workspace&
  brand_color=%231976D2&
  return_url=https://pulsar.example.com/dashboard
```

### Embedded Mode

```
https://pulseql.example.com/workspace?
  mode=embedded&
  hide_header=true&
  hide_footer=true&
  readonly_connections=true
```

### With Auto-Connect

```
https://pulseql.example.com/workspace?
  mode=pulsar&
  workspace_id=conn-123&
  auto_connect=true
```

## Technical Design

### URL Parameter Design

The plugin uses URL parameters for configuration because:
- **Stateless**: No server-side configuration needed
- **Flexible**: Easy to generate from Pulsar
- **Cacheable**: Can be bookmarked or shared
- **Secure**: Can be validated via SSO token
- **Simple**: No complex API integration needed

### State Management

- **MobX Observables**: For reactive state updates
- **Session Storage**: For persistence across page loads
- **URL Parameters**: As source of truth

### Theme Application

Themes are applied through:
1. **SCSS Variables**: Compile-time theme definitions
2. **CSS Variables**: Runtime color customization
3. **Data Attributes**: Theme switching via `data-theme` attribute

## Next Steps

### Pending Implementation

1. **UI Customization Components**
   - [ ] Create UI customization service
   - [ ] Hide/show navigation items based on mode
   - [ ] Customize SQL editor toolbar
   - [ ] Customize result viewer actions
   - [ ] Add "Back to Pulsar" button

2. **SSO Integration**
   - [ ] JWT token handling
   - [ ] Session synchronization
   - [ ] Permission integration

3. **Testing**
   - [ ] Unit tests for services
   - [ ] Integration tests for URL parsing
   - [ ] Theme switching tests
   - [ ] UI customization tests

4. **Documentation**
   - [ ] User guide for Pulsar users
   - [ ] Developer guide for customization
   - [ ] API documentation
   - [ ] Deployment guide

## Configuration Reference

### URL Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `mode` | string | `standalone` | Workspace mode: `standalone`, `pulsar`, `embedded` |
| `theme` | string | - | Theme name: `pulsar-light`, `pulsar-dark` |
| `hide_nav` | string | - | Comma-separated list of nav items to hide |
| `readonly_connections` | boolean | `false` | Make connections read-only |
| `workspace_id` | string | - | Auto-connect to specific workspace |
| `auto_connect` | boolean | `false` | Enable auto-connection |
| `hide_header` | boolean | `false` | Hide application header |
| `hide_footer` | boolean | `false` | Hide application footer |
| `brand_logo` | string | - | Custom logo URL |
| `brand_title` | string | - | Custom page title |
| `brand_color` | string | - | Custom primary color (hex) |
| `return_url` | string | - | URL to return to Pulsar |

### Navigation Items That Can Be Hidden

- `admin` - Administration menu
- `settings` - Settings menu
- `connections` - Connection management
- `users` - User management
- `server` - Server configuration
- `drivers` - Driver management

## References

- [04-ui-customization-strategy.md](./04-ui-customization-strategy.md) - UI customization strategy
- [07-implementation-roadmap.md](./07-implementation-roadmap.md) - Implementation roadmap
- [02-sso-integration-strategy.md](./02-sso-integration-strategy.md) - SSO integration strategy

## Change Log

### 2025-12-13 - Initial Implementation
- Created plugin structure
- Implemented WorkspaceModeService
- Implemented PulsarIntegrationBootstrap
- Created Pulsar Light and Dark themes
- Added comprehensive documentation

## License

Apache License 2.0 - See LICENSE file in repository root
