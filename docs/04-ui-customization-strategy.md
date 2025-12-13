# UI Customization Strategy for Pulsar Integration

## Executive Summary

This document outlines the strategy for customizing PulseQL's user interface when accessed from Pulsar, focusing on creating a streamlined query workspace that shows only relevant features while hiding connection management and administrative functions.

## UI Customization Requirements

### Workspace Mode Requirements

When users access PulseQL from Pulsar, the interface should:

1. **Show Only Query Workspace**:
   - SQL editor with syntax highlighting
   - Query execution controls
   - Result viewer with data grid
   - Database object explorer (navigation tree)
   - Multiple query tab support

2. **Hide Unnecessary Features**:
   - Connection creation/editing UI
   - User administration interface
   - Server configuration panels
   - Global settings that don't apply
   - Welcome/login screens

3. **Customize Branding**:
   - Show Pulsar branding (optional)
   - Customize color scheme to match Pulsar
   - Update navigation labels

4. **Responsive Layout**:
   - Optimized for embedded iFrame usage
   - Adjustable panel sizes
   - Full-screen capable

## Implementation Approaches

### Approach 1: URL Parameter-Based Mode (Recommended)

This approach uses URL parameters to trigger workspace mode without requiring code changes.

#### URL Format

```
https://pulseql.example.com/workspace?
  sso_token=<JWT_TOKEN>&
  mode=pulsar&
  theme=pulsar-light&
  hide_nav=admin,settings,connections&
  readonly_connections=true
```

#### URL Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `sso_token` | string | JWT token for SSO authentication |
| `mode` | string | UI mode: `pulsar`, `embedded`, `standalone` |
| `theme` | string | Theme name: `pulsar-light`, `pulsar-dark` |
| `hide_nav` | string | Comma-separated list of nav items to hide |
| `readonly_connections` | boolean | Make connections read-only |
| `workspace_id` | string | Pre-select specific workspace/connection |
| `auto_connect` | boolean | Auto-connect to default connection |
| `hide_header` | boolean | Hide top application bar |
| `hide_footer` | boolean | Hide footer/status bar |

#### Implementation

**Frontend Mode Service**:

```typescript
// PulseQL: Workspace mode service
import { injectable } from '@cloudbeaver/core-di';
import { observable, computed, action } from 'mobx';

export type WorkspaceMode = 'standalone' | 'pulsar' | 'embedded';

export interface WorkspaceModeConfig {
  mode: WorkspaceMode;
  theme?: string;
  hideNavigation?: string[];
  readonlyConnections?: boolean;
  workspaceId?: string;
  autoConnect?: boolean;
  hideHeader?: boolean;
  hideFooter?: boolean;
  customBranding?: {
    logo?: string;
    title?: string;
    color?: string;
  };
}

@injectable()
export class WorkspaceModeService {
  
  @observable
  private config: WorkspaceModeConfig = {
    mode: 'standalone'
  };
  
  @computed
  get mode(): WorkspaceMode {
    return this.config.mode;
  }
  
  @computed
  get isPulsarMode(): boolean {
    return this.config.mode === 'pulsar';
  }
  
  @computed
  get isEmbeddedMode(): boolean {
    return this.config.mode === 'embedded' || this.config.mode === 'pulsar';
  }
  
  @computed
  get readonlyConnections(): boolean {
    return this.config.readonlyConnections || this.isPulsarMode;
  }
  
  @computed
  get hideHeader(): boolean {
    return this.config.hideHeader || false;
  }
  
  @computed
  get hideFooter(): boolean {
    return this.config.hideFooter || false;
  }
  
  @action
  initializeFromURL(): void {
    const params = new URLSearchParams(window.location.search);
    
    this.config = {
      mode: (params.get('mode') as WorkspaceMode) || 'standalone',
      theme: params.get('theme') || undefined,
      hideNavigation: params.get('hide_nav')?.split(',') || [],
      readonlyConnections: params.get('readonly_connections') === 'true',
      workspaceId: params.get('workspace_id') || undefined,
      autoConnect: params.get('auto_connect') === 'true',
      hideHeader: params.get('hide_header') === 'true',
      hideFooter: params.get('hide_footer') === 'true',
      customBranding: this.parseCustomBranding(params)
    };
    
    // Apply theme
    if (this.config.theme) {
      this.applyTheme(this.config.theme);
    }
  }
  
  private parseCustomBranding(params: URLSearchParams): WorkspaceModeConfig['customBranding'] {
    const logo = params.get('brand_logo');
    const title = params.get('brand_title');
    const color = params.get('brand_color');
    
    if (!logo && !title && !color) {
      return undefined;
    }
    
    return { logo, title, color };
  }
  
  private applyTheme(themeName: string): void {
    document.body.setAttribute('data-theme', themeName);
  }
  
  shouldHideNavItem(itemId: string): boolean {
    return this.config.hideNavigation?.includes(itemId) || false;
  }
  
  getWorkspaceId(): string | undefined {
    return this.config.workspaceId;
  }
  
  shouldAutoConnect(): boolean {
    return this.config.autoConnect || false;
  }
  
  getCustomBranding(): WorkspaceModeConfig['customBranding'] {
    return this.config.customBranding;
  }
}
```

**Bootstrap Integration**:

```typescript
// Initialize workspace mode on app startup
@injectable()
export class WorkspaceModeBootstrap extends Bootstrap {
  
  constructor(
    private readonly workspaceModeService: WorkspaceModeService,
    private readonly navigationService: NavigationService,
    private readonly themeService: ThemeService
  ) {
    super();
  }
  
  async load(): Promise<void> {
    // Initialize from URL parameters
    this.workspaceModeService.initializeFromURL();
    
    // Apply mode-specific customizations
    if (this.workspaceModeService.isPulsarMode) {
      await this.applyPulsarModeCustomizations();
    }
  }
  
  private async applyPulsarModeCustomizations(): Promise<void> {
    // Auto-connect if specified
    if (this.workspaceModeService.shouldAutoConnect()) {
      await this.autoConnectToWorkspace();
    }
    
    // Apply custom branding
    const branding = this.workspaceModeService.getCustomBranding();
    if (branding) {
      this.applyCustomBranding(branding);
    }
  }
  
  private async autoConnectToWorkspace(): Promise<void> {
    const workspaceId = this.workspaceModeService.getWorkspaceId();
    
    if (workspaceId) {
      // Navigate to specific workspace
      await this.navigationService.navigateTo(`/workspace/${workspaceId}`);
    }
  }
  
  private applyCustomBranding(branding: any): void {
    if (branding.logo) {
      // Update logo
      this.themeService.setLogo(branding.logo);
    }
    
    if (branding.title) {
      // Update page title
      document.title = branding.title;
    }
    
    if (branding.color) {
      // Update primary color
      this.themeService.setPrimaryColor(branding.color);
    }
  }
}
```

### Approach 2: Plugin-Based Customization

Create a dedicated Pulsar integration plugin that customizes the UI.

**Plugin Structure**:

```
webapp/packages/plugin-pulsar-integration/
├── src/
│   ├── PulsarIntegrationService.ts
│   ├── PulsarUICustomizer.ts
│   ├── PulsarWorkspaceScreen.tsx
│   ├── PulsarTheme.ts
│   ├── locales/
│   └── module.ts
├── package.json
└── README.md
```

**Plugin Implementation**:

```typescript
// plugin-pulsar-integration/src/PulsarUICustomizer.ts
import { injectable } from '@cloudbeaver/core-di';
import { NavigationTreeService } from '@cloudbeaver/plugin-navigation-tree';
import { TopMenuService } from '@cloudbeaver/plugin-top-app-bar';
import { ConnectionsService } from '@cloudbeaver/core-connections';

@injectable()
export class PulsarUICustomizer {
  
  constructor(
    private readonly navigationTreeService: NavigationTreeService,
    private readonly topMenuService: TopMenuService,
    private readonly connectionsService: ConnectionsService
  ) {}
  
  applyCustomizations(): void {
    this.customizeTopMenu();
    this.customizeNavigationTree();
    this.customizeConnectionActions();
    this.customizeToolbar();
  }
  
  private customizeTopMenu(): void {
    // Remove unwanted menu items
    this.topMenuService.removeMenuItem('administration');
    this.topMenuService.removeMenuItem('server-settings');
    this.topMenuService.removeMenuItem('user-management');
    
    // Update menu labels
    this.topMenuService.updateMenuItem('workspace', {
      label: 'Query Workspace',
      icon: 'query'
    });
    
    // Add Pulsar-specific menu items
    this.topMenuService.addMenuItem({
      id: 'back-to-pulsar',
      label: 'Back to Pulsar',
      icon: 'arrow-back',
      position: 'left',
      onClick: () => this.navigateBackToPulsar()
    });
  }
  
  private customizeNavigationTree(): void {
    // Configure navigation tree filter
    this.navigationTreeService.setFilter({
      // Show database objects
      showConnections: true,
      showDatabases: true,
      showSchemas: true,
      showTables: true,
      showViews: true,
      showColumns: true,
      showIndexes: true,
      
      // Hide other features
      showProjects: false,
      showResources: false,
      showScripts: false
    });
    
    // Customize node renderers
    this.navigationTreeService.registerNodeRenderer({
      nodeType: 'connection',
      render: (node) => this.renderReadOnlyConnection(node)
    });
  }
  
  private customizeConnectionActions(): void {
    // Remove connection creation/editing actions
    this.connectionsService.removeAction('connection.create');
    this.connectionsService.removeAction('connection.edit');
    this.connectionsService.removeAction('connection.delete');
    
    // Make connection actions read-only
    this.connectionsService.setActionMode('readonly');
  }
  
  private customizeToolbar(): void {
    // Hide unnecessary toolbar items
    const toolbarCustomizations = {
      'import-data': { hidden: true },
      'manage-drivers': { hidden: true },
      'server-logs': { hidden: true }
    };
    
    Object.entries(toolbarCustomizations).forEach(([id, config]) => {
      this.topMenuService.updateToolbarItem(id, config);
    });
  }
  
  private navigateBackToPulsar(): void {
    // Get Pulsar URL from configuration or session
    const pulsarUrl = this.getPulsarUrl();
    
    if (pulsarUrl) {
      window.location.href = pulsarUrl;
    }
  }
  
  private getPulsarUrl(): string | null {
    // Try to get from session storage (set during SSO)
    return sessionStorage.getItem('pulsar_return_url');
  }
  
  private renderReadOnlyConnection(node: any): React.ReactElement {
    // Custom renderer for read-only connections
    return (
      <div className="readonly-connection">
        <Icon name="database-lock" />
        <span>{node.name}</span>
        <Badge>Read-only</Badge>
      </div>
    );
  }
}
```

**Workspace Screen Component**:

```typescript
// plugin-pulsar-integration/src/PulsarWorkspaceScreen.tsx
import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import { SqlEditorScreen } from '@cloudbeaver/plugin-sql-editor-screen';
import { NavigationTree } from '@cloudbeaver/plugin-navigation-tree';
import { WorkspaceModeService } from './WorkspaceModeService';

export const PulsarWorkspaceScreen = observer(function PulsarWorkspaceScreen() {
  const workspaceModeService = useService(WorkspaceModeService);
  
  return (
    <div className="pulsar-workspace">
      {/* Conditional header */}
      {!workspaceModeService.hideHeader && (
        <div className="workspace-header">
          <PulsarWorkspaceHeader />
        </div>
      )}
      
      {/* Main workspace area */}
      <div className="workspace-main">
        {/* Left panel - Database explorer */}
        <div className="workspace-sidebar">
          <NavigationTree readonly={workspaceModeService.readonlyConnections} />
        </div>
        
        {/* Right panel - SQL editor and results */}
        <div className="workspace-content">
          <SqlEditorScreen />
        </div>
      </div>
      
      {/* Conditional footer */}
      {!workspaceModeService.hideFooter && (
        <div className="workspace-footer">
          <PulsarWorkspaceFooter />
        </div>
      )}
    </div>
  );
});

const PulsarWorkspaceHeader = observer(function PulsarWorkspaceHeader() {
  const workspaceModeService = useService(WorkspaceModeService);
  const branding = workspaceModeService.getCustomBranding();
  
  return (
    <div className="pulsar-header">
      {branding?.logo && <img src={branding.logo} alt="Logo" />}
      {branding?.title && <h1>{branding.title}</h1>}
      
      <div className="header-actions">
        <Button onClick={() => window.parent.postMessage({ type: 'close' }, '*')}>
          Back to Pulsar
        </Button>
      </div>
    </div>
  );
});

const PulsarWorkspaceFooter = observer(function PulsarWorkspaceFooter() {
  return (
    <div className="pulsar-footer">
      <span>Powered by PulseQL</span>
    </div>
  );
});
```

## UI Component Customization

### Navigation Tree Customization

**Hide Connection Management Actions**:

```typescript
// Customize navigation tree context menu
export class PulsarNavigationTreeCustomizer {
  
  customizeContextMenu(): void {
    // Get navigation tree context menu service
    const contextMenuService = this.injector.getServiceByClass(
      NavigationTreeContextMenuService
    );
    
    // Remove connection management actions for Pulsar users
    if (this.permissionService.isPulsarUser()) {
      contextMenuService.removeAction('connection.create');
      contextMenuService.removeAction('connection.edit');
      contextMenuService.removeAction('connection.delete');
      contextMenuService.removeAction('connection.duplicate');
    }
    
    // Keep only view actions
    const allowedActions = [
      'connection.view',
      'connection.select',
      'object.view',
      'object.explore'
    ];
    
    contextMenuService.filterActions((action) => 
      allowedActions.includes(action.id)
    );
  }
}
```

**Read-Only Connection Indicator**:

```typescript
// Add visual indicator for read-only connections
export const ConnectionNodeRenderer = observer<{ node: NavNode }>(
  function ConnectionNodeRenderer({ node }) {
    const permissionService = useService(PulsarPermissionService);
    const readonly = permissionService.isPulsarUser();
    
    return (
      <div className="connection-node">
        <Icon name={node.icon} />
        <span className="connection-name">{node.name}</span>
        
        {readonly && (
          <Tooltip title="Connection is read-only in Pulsar mode">
            <Icon name="lock" className="readonly-icon" />
          </Tooltip>
        )}
        
        <ConnectionStatus status={node.connectionStatus} />
      </div>
    );
  }
);
```

### SQL Editor Customization

**Simplified Toolbar**:

```typescript
// Customize SQL editor toolbar for Pulsar mode
export const PulsarSqlEditorToolbar = observer(function PulsarSqlEditorToolbar() {
  const permissionService = useService(PulsarPermissionService);
  
  return (
    <div className="sql-editor-toolbar">
      {/* Always show execute button */}
      <Button 
        icon="play" 
        onClick={executeQuery}
        disabled={!permissionService.canExecuteQuery()}
      >
        Execute
      </Button>
      
      {/* Conditional buttons based on permissions */}
      {permissionService.canSaveScript() && (
        <Button icon="save" onClick={saveScript}>
          Save
        </Button>
      )}
      
      {permissionService.canExportData() && (
        <Button icon="download" onClick={exportResults}>
          Export
        </Button>
      )}
      
      {/* Hide advanced features */}
      {!permissionService.isPulsarUser() && (
        <>
          <Button icon="format" onClick={formatSQL}>
            Format
          </Button>
          <Button icon="explain" onClick={explainQuery}>
            Explain
          </Button>
        </>
      )}
    </div>
  );
});
```

**Query Tab Management**:

```typescript
// Allow multiple query tabs with restrictions
export class PulsarQueryTabService {
  
  canCreateNewTab(): boolean {
    // Allow new tabs for query editing
    return this.permissionService.canExecuteQuery();
  }
  
  canCloseTab(tabId: string): boolean {
    // Always allow closing tabs
    return true;
  }
  
  canRenameTab(tabId: string): boolean {
    // Only if user can save scripts
    return this.permissionService.canSaveScript();
  }
  
  createNewQueryTab(): void {
    if (!this.canCreateNewTab()) {
      this.notificationService.showError('Permission denied');
      return;
    }
    
    // Create new tab with default connection
    const tab = this.sqlEditorTabService.createTab({
      name: 'New Query',
      connectionId: this.getDefaultConnectionId(),
      script: '-- Write your query here\n'
    });
    
    this.sqlEditorTabService.activateTab(tab.id);
  }
  
  private getDefaultConnectionId(): string | undefined {
    // Use workspace connection if specified
    const workspaceId = this.workspaceModeService.getWorkspaceId();
    
    if (workspaceId) {
      return workspaceId;
    }
    
    // Otherwise, use first available connection
    const connections = this.connectionsService.getConnections();
    return connections[0]?.id;
  }
}
```

### Result Viewer Customization

**Data Export Controls**:

```typescript
// Customize data export based on permissions
export const PulsarResultViewerActions = observer(
  function PulsarResultViewerActions({ resultSetId }) {
    const permissionService = useService(PulsarPermissionService);
    
    return (
      <div className="result-actions">
        {/* Copy to clipboard - always available */}
        <Button icon="copy" onClick={copyToClipboard}>
          Copy
        </Button>
        
        {/* Export - permission-based */}
        {permissionService.canExportData() && (
          <DropdownMenu>
            <DropdownMenuItem onClick={exportAsCSV}>
              Export as CSV
            </DropdownMenuItem>
            <DropdownMenuItem onClick={exportAsJSON}>
              Export as JSON
            </DropdownMenuItem>
            <DropdownMenuItem onClick={exportAsExcel}>
              Export as Excel
            </DropdownMenuItem>
          </DropdownMenu>
        )}
        
        {/* Data editing - permission-based */}
        {permissionService.canEditData() && (
          <Button icon="edit" onClick={enableEditMode}>
            Edit Data
          </Button>
        )}
      </div>
    );
  }
);
```

## Theme Customization

### Pulsar Theme Definition

**Theme Configuration**:

```typescript
// Pulsar-specific theme
export const pulsarLightTheme: Theme = {
  id: 'pulsar-light',
  name: 'Pulsar Light',
  type: 'light',
  
  colors: {
    // Primary colors - match Pulsar branding
    primary: '#1976D2',
    primaryDark: '#0D47A1',
    primaryLight: '#BBDEFB',
    
    // Secondary colors
    secondary: '#388E3C',
    secondaryDark: '#1B5E20',
    secondaryLight: '#C8E6C9',
    
    // Background colors
    background: '#FFFFFF',
    backgroundSecondary: '#F5F5F5',
    backgroundTertiary: '#EEEEEE',
    
    // Text colors
    text: '#212121',
    textSecondary: '#757575',
    textDisabled: '#BDBDBD',
    
    // UI element colors
    border: '#E0E0E0',
    divider: '#BDBDBD',
    hover: '#F5F5F5',
    
    // Status colors
    success: '#4CAF50',
    warning: '#FF9800',
    error: '#F44336',
    info: '#2196F3'
  },
  
  typography: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif',
    fontSize: {
      small: '12px',
      normal: '14px',
      large: '16px',
      xlarge: '20px'
    },
    fontWeight: {
      normal: 400,
      medium: 500,
      bold: 700
    }
  },
  
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '24px',
    xl: '32px'
  },
  
  borderRadius: {
    small: '2px',
    medium: '4px',
    large: '8px'
  },
  
  shadows: {
    small: '0 1px 3px rgba(0, 0, 0, 0.12)',
    medium: '0 2px 6px rgba(0, 0, 0, 0.16)',
    large: '0 4px 12px rgba(0, 0, 0, 0.20)'
  }
};

export const pulsarDarkTheme: Theme = {
  ...pulsarLightTheme,
  id: 'pulsar-dark',
  name: 'Pulsar Dark',
  type: 'dark',
  
  colors: {
    ...pulsarLightTheme.colors,
    
    // Override for dark theme
    background: '#1E1E1E',
    backgroundSecondary: '#252525',
    backgroundTertiary: '#2D2D2D',
    
    text: '#E0E0E0',
    textSecondary: '#AAAAAA',
    textDisabled: '#666666',
    
    border: '#3D3D3D',
    divider: '#4D4D4D',
    hover: '#2D2D2D'
  }
};
```

**Theme Registration**:

```typescript
// Register Pulsar themes
export class PulsarThemeService {
  
  constructor(
    private readonly themeService: ThemeService
  ) {}
  
  registerThemes(): void {
    this.themeService.registerTheme(pulsarLightTheme);
    this.themeService.registerTheme(pulsarDarkTheme);
  }
  
  applyPulsarTheme(themeName?: string): void {
    const theme = themeName || 'pulsar-light';
    this.themeService.setTheme(theme);
  }
}
```

### CSS Customization

**Custom Styles**:

```scss
// pulsar-workspace.scss
.pulsar-workspace {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--theme-background);
  
  .workspace-header {
    height: 60px;
    background: var(--theme-primary);
    color: white;
    display: flex;
    align-items: center;
    padding: 0 var(--spacing-md);
    box-shadow: var(--shadow-medium);
    
    img {
      height: 40px;
      margin-right: var(--spacing-md);
    }
    
    h1 {
      font-size: var(--font-size-large);
      font-weight: var(--font-weight-medium);
      margin: 0;
      flex: 1;
    }
  }
  
  .workspace-main {
    flex: 1;
    display: flex;
    overflow: hidden;
    
    .workspace-sidebar {
      width: 300px;
      background: var(--theme-background-secondary);
      border-right: 1px solid var(--theme-border);
      overflow-y: auto;
    }
    
    .workspace-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
  }
  
  .workspace-footer {
    height: 30px;
    background: var(--theme-background-secondary);
    border-top: 1px solid var(--theme-border);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--font-size-small);
    color: var(--theme-text-secondary);
  }
  
  // Read-only connection indicator
  .readonly-icon {
    color: var(--theme-warning);
    margin-left: var(--spacing-xs);
  }
  
  // Disabled UI elements for Pulsar mode
  &[data-mode="pulsar"] {
    .connection-create-button,
    .connection-edit-button,
    .connection-delete-button {
      display: none !important;
    }
    
    .admin-menu,
    .server-settings-menu {
      display: none !important;
    }
  }
}
```

## Layout Modes

### Full-Screen Mode

```typescript
// Full-screen workspace layout
export const FullScreenWorkspace = observer(function FullScreenWorkspace() {
  return (
    <div className="fullscreen-workspace">
      {/* No header or footer */}
      <div className="workspace-container">
        <NavigationTree />
        <SqlEditorScreen />
      </div>
    </div>
  );
});
```

### Embedded iFrame Mode

```typescript
// Optimized for iFrame embedding
export class EmbeddedModeService {
  
  setupEmbeddedMode(): void {
    // Remove unnecessary chrome
    this.hideApplicationChrome();
    
    // Setup message communication with parent
    this.setupParentCommunication();
    
    // Adjust layout for iFrame
    this.adjustLayoutForEmbedding();
  }
  
  private hideApplicationChrome(): void {
    // Hide elements not needed in embedded mode
    document.body.classList.add('embedded-mode');
  }
  
  private setupParentCommunication(): void {
    // Listen for messages from parent (Pulsar)
    window.addEventListener('message', (event) => {
      // Verify origin
      if (event.origin !== this.pulsarOrigin) {
        return;
      }
      
      // Handle messages
      switch (event.data.type) {
        case 'execute-query':
          this.executeQuery(event.data.query);
          break;
        
        case 'change-connection':
          this.changeConnection(event.data.connectionId);
          break;
        
        case 'close':
          this.handleClose();
          break;
      }
    });
    
    // Send ready message to parent
    window.parent.postMessage({ type: 'ready' }, this.pulsarOrigin);
  }
  
  private adjustLayoutForEmbedding(): void {
    // Remove fixed heights/widths
    // Adjust to fill parent container
    document.body.style.height = '100%';
    document.body.style.overflow = 'hidden';
  }
  
  // Send messages to parent
  sendToParent(message: any): void {
    window.parent.postMessage(message, this.pulsarOrigin);
  }
  
  // Notify parent of query execution
  notifyQueryExecuted(resultCount: number): void {
    this.sendToParent({
      type: 'query-executed',
      resultCount
    });
  }
  
  // Notify parent of errors
  notifyError(error: string): void {
    this.sendToParent({
      type: 'error',
      error
    });
  }
}
```

### Split-Panel Mode

```typescript
// Adjustable split panels
export const SplitPanelWorkspace = observer(function SplitPanelWorkspace() {
  const [sidebarWidth, setSidebarWidth] = useState(300);
  const [editorHeight, setEditorHeight] = useState(60); // Percentage
  
  return (
    <div className="split-panel-workspace">
      <ResizablePanel
        direction="horizontal"
        minSize={200}
        maxSize={500}
        size={sidebarWidth}
        onResize={setSidebarWidth}
      >
        <NavigationTree />
      </ResizablePanel>
      
      <div className="main-panel">
        <ResizablePanel
          direction="vertical"
          minSize={30}
          maxSize={80}
          size={editorHeight}
          onResize={setEditorHeight}
        >
          <SqlEditor />
        </ResizablePanel>
        
        <ResultViewer />
      </div>
    </div>
  );
});
```

## Configuration

### UI Customization Config File

```yaml
# pulsar-ui-customization.yml
workspace_mode:
  default_mode: standalone
  allowed_modes:
    - standalone
    - pulsar
    - embedded

pulsar_mode:
  # Header configuration
  header:
    visible: true
    logo: /assets/pulsar-logo.png
    title: Pulsar Query Workspace
    background_color: '#1976D2'
  
  # Footer configuration
  footer:
    visible: true
    text: Powered by PulseQL
  
  # Navigation customization
  navigation:
    hide_items:
      - administration
      - server-settings
      - user-management
      - connection-management
    
    readonly_connections: true
    show_connection_status: true
  
  # Editor customization
  editor:
    toolbar_items:
      - execute
      - save
      - export
      - format
    
    hide_items:
      - explain-plan
      - query-history
      - templates
  
  # Theme
  theme:
    default: pulsar-light
    available:
      - pulsar-light
      - pulsar-dark
  
  # Branding
  branding:
    primary_color: '#1976D2'
    secondary_color: '#388E3C'
    logo_url: /assets/pulsar-logo.png
    favicon_url: /assets/pulsar-favicon.ico

embedded_mode:
  remove_chrome: true
  parent_origin: https://pulsar.example.com
  auto_resize: true
  messaging_enabled: true
```

## Testing UI Customizations

### Visual Regression Testing

```typescript
// Test UI customizations
describe('Pulsar UI Customization', () => {
  it('should hide connection management in Pulsar mode', async () => {
    await page.goto('/workspace?mode=pulsar&sso_token=<token>');
    
    // Check that create connection button is hidden
    const createButton = await page.$('.connection-create-button');
    expect(createButton).toBeNull();
    
    // Check that navigation tree is visible
    const navTree = await page.$('.navigation-tree');
    expect(navTree).not.toBeNull();
  });
  
  it('should apply Pulsar theme', async () => {
    await page.goto('/workspace?mode=pulsar&theme=pulsar-light');
    
    // Check primary color
    const primaryColor = await page.evaluate(() => {
      return getComputedStyle(document.body)
        .getPropertyValue('--theme-primary');
    });
    
    expect(primaryColor).toBe('#1976D2');
  });
  
  it('should show custom branding', async () => {
    await page.goto('/workspace?mode=pulsar&brand_title=Pulsar Workspace');
    
    const title = await page.title();
    expect(title).toBe('Pulsar Workspace');
  });
});
```

## Conclusion

The UI customization strategy provides:

1. **Flexible Customization**: Multiple approaches to customize UI
2. **URL Parameter Control**: Easy configuration via URL parameters
3. **Plugin Architecture**: Extensible design for custom features
4. **Permission Integration**: UI adapts to user permissions
5. **Theme Support**: Consistent branding with Pulsar
6. **Embedded Mode**: Optimized for iFrame integration
7. **Responsive Design**: Adapts to different screen sizes
8. **Maintainability**: Clear separation of customization code

This strategy ensures that Pulsar users have a streamlined, focused query workspace experience while maintaining the full power of PulseQL's query capabilities.
