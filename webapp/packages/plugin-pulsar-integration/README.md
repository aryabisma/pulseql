# Pulsar Integration Plugin

CloudBeaver plugin for seamless integration with Pulsar application.

## Overview

This plugin enables PulseQL (CloudBeaver Community Edition) to be integrated with the Pulsar application through:

- **Workspace Mode**: URL parameter-based mode switching between standalone, Pulsar, and embedded modes
- **Custom Theming**: Pulsar-specific light and dark themes matching Pulsar's visual style
- **Custom Branding**: Logo, title, and color customization via URL parameters
- **Read-only Connections**: Support for read-only connection mode
- **Navigation Customization**: Hide/show navigation items based on mode

## Installation

This plugin is part of the PulseQL monorepo. To build:

```bash
cd /path/to/pulseql/webapp/packages/plugin-pulsar-integration
yarn build
```

## Usage

### Basic Pulsar Mode

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
  return_url=https://pulsar.example.com
```

### Embedded Mode

```
https://pulseql.example.com/workspace?
  mode=embedded&
  hide_header=true&
  hide_footer=true&
  readonly_connections=true
```

## URL Parameters

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

## Architecture

### Services

#### WorkspaceModeService

Manages workspace mode configuration from URL parameters.

**Key Methods**:
- `initializeFromURL()` - Parse and apply URL parameters
- `isPulsarMode` - Check if in Pulsar mode
- `isEmbeddedMode` - Check if in embedded mode
- `shouldHideNavItem(itemId)` - Check if nav item should be hidden
- `getCustomBranding()` - Get custom branding config

#### PulsarIntegrationBootstrap

Bootstrap service that initializes the plugin on application startup.

**Responsibilities**:
- Initialize WorkspaceModeService
- Apply custom branding
- Apply theme
- Store configuration in session

### Themes

#### Pulsar Light Theme (`pulsar-light`)

Light mode theme matching Pulsar's visual style:
- Primary: `#1976D2` (Pulsar blue)
- Secondary: `#f2f2f2` (Light gray)
- Background: `#f5f5f5` (Very light gray)

#### Pulsar Dark Theme (`pulsar-dark`)

Dark mode theme matching Pulsar's visual style:
- Primary: `#1976D2` (Same Pulsar blue)
- Background: `#121212` (Very dark)
- Surface: `#1E1E1E` (Dark surface)

## Development

### Building

```bash
yarn build
```

### Linting

```bash
yarn lint
```

### Clean Build Artifacts

```bash
yarn clean
```

## Documentation

For detailed implementation documentation, see:
- [Implementation Documentation](/docs/09-pulsar-plugin-implementation.md)
- [UI Customization Strategy](/docs/04-ui-customization-strategy.md)
- [Integration Roadmap](/docs/07-implementation-roadmap.md)

## License

Apache License 2.0 - See LICENSE file in repository root

## Contributing

This plugin follows CloudBeaver's architecture patterns and coding standards. See the main repository README for contribution guidelines.
