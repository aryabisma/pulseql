# Deep Linking Integration Examples

This document provides practical examples of integrating the Deep Linking feature into various parts of the Pulsar application.

## Table of Contents
1. [Context Menu Integration](#context-menu-integration)
2. [Schema Browser Integration](#schema-browser-integration)
3. [Query Results Integration](#query-results-integration)
4. [Custom Integration](#custom-integration)

## Context Menu Integration

### Navigation Tree Context Menu

To add "Open in PulseQL" option to table context menu:

```typescript
// In NavigationTreeService or similar
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';
import { MenuService } from '@cloudbeaver/core-view';
import { useService } from '@cloudbeaver/core-di';

// Register context menu item
menuService.addMenuItem({
  id: 'open-in-pulseql',
  label: 'Open in PulseQL',
  icon: '/icons/external-link.svg',
  isPresent: (context) => {
    // Show only for table nodes
    return context.node?.objectType === 'table';
  },
  onClick: async (context) => {
    const deepLinkingService = useService(DeepLinkingService);
    
    const result = await deepLinkingService.generateAndCopyLink({
      type: 'table',
      connectionId: context.node.connectionId,
      schemaName: context.node.schemaName,
      tableName: context.node.name,
    });

    if (result.success) {
      // Show success notification
      notificationService.logSuccess({
        title: 'Link Copied',
        message: 'Deep link copied to clipboard. Paste it in your browser to open in PulseQL.',
      });
    } else {
      // Show error
      notificationService.logError({
        title: 'Failed to Generate Link',
        message: result.error,
      });
    }
  },
});
```

### Alternative: Using DeepLinkButton Component

```typescript
// In TableContextMenu.tsx
import { observer } from 'mobx-react-lite';
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';

export const TableContextMenu = observer<{ node: NavigationNode }>(
  function TableContextMenu({ node }) {
    return (
      <ContextMenu>
        <MenuItem>View Data</MenuItem>
        <MenuItem>Edit Table</MenuItem>
        <MenuSeparator />
        
        {/* Deep Link Button */}
        <DeepLinkButton
          targetType="table"
          connectionId={node.connectionId}
          schemaName={node.schemaName}
          tableName={node.name}
          label="Open in PulseQL"
          mode="icon"
        />
      </ContextMenu>
    );
  }
);
```

## Schema Browser Integration

### Schema Object Actions

Add deep link action to schema objects:

```typescript
// In SchemaObjectActions.tsx
import { observer } from 'mobx-react-lite';
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';

export const SchemaObjectActions = observer<{
  object: SchemaObject;
}>(function SchemaObjectActions({ object }) {
  
  // Only show for tables and views
  if (object.type !== 'table' && object.type !== 'view') {
    return null;
  }

  return (
    <div className="schema-object-actions">
      <DeepLinkButton
        targetType="table"
        connectionId={object.connectionId}
        schemaName={object.schemaName}
        tableName={object.name}
        label={`Query in PulseQL`}
        mode="icon"
        className="action-button"
      />
    </div>
  );
});
```

### Schema Toolbar

Add schema-level deep link in toolbar:

```typescript
// In SchemaToolbar.tsx
import { observer } from 'mobx-react-lite';
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';
import { Toolbar } from '@cloudbeaver/core-blocks';

export const SchemaToolbar = observer<{
  connection: Connection;
  schema: Schema;
}>(function SchemaToolbar({ connection, schema }) {
  return (
    <Toolbar>
      <ToolbarButton icon="refresh" onClick={handleRefresh}>
        Refresh
      </ToolbarButton>
      
      <ToolbarSeparator />
      
      {/* Deep Link for Schema */}
      <DeepLinkButton
        targetType="schema"
        connectionId={connection.id}
        schemaName={schema.name}
        label="Explore in PulseQL"
        mode="button"
      />
    </Toolbar>
  );
});
```

## Query Results Integration

### Query Editor Toolbar

Add "Edit in PulseQL" button to query editor:

```typescript
// In QueryEditorToolbar.tsx
import { observer } from 'mobx-react-lite';
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';
import { useService } from '@cloudbeaver/core-di';
import { SQLEditorService } from '@cloudbeaver/plugin-sql-editor';

export const QueryEditorToolbar = observer(function QueryEditorToolbar() {
  const sqlEditorService = useService(SQLEditorService);
  const currentQuery = sqlEditorService.getCurrentQuery();

  return (
    <div className="query-editor-toolbar">
      <button onClick={handleExecute}>Execute</button>
      <button onClick={handleFormat}>Format</button>
      
      {/* Deep Link for Current Query */}
      {currentQuery && (
        <DeepLinkButton
          targetType="query"
          query={currentQuery}
          label="Edit in PulseQL"
          disabled={!currentQuery}
        />
      )}
    </div>
  );
});
```

### Query Results Panel

Add link to query results:

```typescript
// In QueryResultsPanel.tsx
import { observer } from 'mobx-react-lite';
import { DeepLinkButton } from '@cloudbeaver/plugin-pulsar-integration';

export const QueryResultsPanel = observer<{
  query: string;
  results: QueryResults;
}>(function QueryResultsPanel({ query, results }) {
  return (
    <div className="query-results-panel">
      <div className="results-header">
        <span>Results: {results.rowCount} rows</span>
        
        <div className="results-actions">
          <button onClick={handleExport}>Export</button>
          
          {/* Deep Link to Modify Query */}
          <DeepLinkButton
            targetType="query"
            query={query}
            label="Modify in PulseQL"
            mode="icon"
          />
        </div>
      </div>
      
      <ResultsGrid data={results.data} />
    </div>
  );
});
```

## Custom Integration

### Custom Hook for Deep Linking

Create a custom hook for common deep linking operations:

```typescript
// useDeepLink.ts
import { useService } from '@cloudbeaver/core-di';
import { DeepLinkingService, DeepLinkTarget } from '@cloudbeaver/plugin-pulsar-integration';
import { NotificationService } from '@cloudbeaver/core-events';
import { useCallback } from 'react';

export function useDeepLink() {
  const deepLinkingService = useService(DeepLinkingService);
  const notificationService = useService(NotificationService);

  const generateAndCopyLink = useCallback(
    async (target: DeepLinkTarget) => {
      const result = await deepLinkingService.generateAndCopyLink(target);

      if (result.success) {
        notificationService.logSuccess({
          title: 'Link Copied',
          message: 'Deep link has been copied to clipboard',
        });
        return true;
      } else {
        notificationService.logError({
          title: 'Failed to Generate Link',
          message: result.error || 'Unknown error',
        });
        return false;
      }
    },
    [deepLinkingService, notificationService]
  );

  const openInPulseQL = useCallback(
    async (target: DeepLinkTarget) => {
      const response = await deepLinkingService.generateDeepLinkViaAPI(target);

      if (response.success && response.deep_link) {
        // Open in new tab
        window.open(response.deep_link, '_blank');
        return true;
      } else {
        notificationService.logError({
          title: 'Failed to Open Link',
          message: response.error || 'Unknown error',
        });
        return false;
      }
    },
    [deepLinkingService, notificationService]
  );

  return {
    generateAndCopyLink,
    openInPulseQL,
  };
}
```

### Using the Custom Hook

```typescript
// In MyComponent.tsx
import { useDeepLink } from './useDeepLink';

function MyComponent({ table }) {
  const { generateAndCopyLink, openInPulseQL } = useDeepLink();

  const handleCopyLink = () => {
    generateAndCopyLink({
      type: 'table',
      connectionId: table.connectionId,
      schemaName: table.schemaName,
      tableName: table.name,
    });
  };

  const handleOpenInPulseQL = () => {
    openInPulseQL({
      type: 'table',
      connectionId: table.connectionId,
      schemaName: table.schemaName,
      tableName: table.name,
    });
  };

  return (
    <div>
      <button onClick={handleCopyLink}>Copy PulseQL Link</button>
      <button onClick={handleOpenInPulseQL}>Open in PulseQL</button>
    </div>
  );
}
```

### Batch Deep Link Generation

Generate multiple links at once:

```typescript
// In BatchDeepLinkService.ts
import { injectable } from '@cloudbeaver/core-di';
import { DeepLinkingService, DeepLinkTarget } from '@cloudbeaver/plugin-pulsar-integration';

@injectable()
export class BatchDeepLinkService {
  constructor(
    private readonly deepLinkingService: DeepLinkingService
  ) {}

  async generateMultipleLinks(targets: DeepLinkTarget[]): Promise<Map<string, string>> {
    const links = new Map<string, string>();

    for (const target of targets) {
      const response = await this.deepLinkingService.generateDeepLinkViaAPI(target);
      
      if (response.success && response.deep_link) {
        const key = this.getTargetKey(target);
        links.set(key, response.deep_link);
      }
    }

    return links;
  }

  private getTargetKey(target: DeepLinkTarget): string {
    switch (target.type) {
      case 'table':
        return `${target.connectionId}/${target.schemaName}/${target.tableName}`;
      case 'schema':
        return `${target.connectionId}/${target.schemaName}`;
      case 'query':
        return `query:${target.query?.substring(0, 20)}...`;
      default:
        return target.type;
    }
  }
}
```

## Best Practices

1. **Error Handling**: Always handle errors gracefully and show user-friendly messages
2. **Loading States**: Show loading indicators while generating links
3. **Clipboard Permissions**: Request clipboard permissions only when needed
4. **Session Validation**: Ensure user is authenticated before showing deep link options
5. **Context Awareness**: Only show deep link options for resources user has access to
6. **Accessibility**: Ensure buttons are keyboard accessible and have proper ARIA labels
7. **Performance**: Avoid generating links in loops; use batch operations when possible

## Testing

### Unit Tests

```typescript
// DeepLinkButton.test.tsx
import { render, fireEvent, waitFor } from '@testing-library/react';
import { DeepLinkButton } from './DeepLinkButton';

describe('DeepLinkButton', () => {
  it('should generate link on click', async () => {
    const { getByText } = render(
      <DeepLinkButton
        targetType="table"
        connectionId="test-conn"
        schemaName="public"
        tableName="users"
      />
    );

    const button = getByText('Open in PulseQL');
    fireEvent.click(button);

    await waitFor(() => {
      // Assert notification was shown
      expect(screen.getByText('Link Copied')).toBeInTheDocument();
    });
  });

  it('should show error for invalid parameters', async () => {
    const { getByText } = render(
      <DeepLinkButton
        targetType="table"
        // Missing required parameters
      />
    );

    const button = getByText('Open in PulseQL');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Invalid Parameters')).toBeInTheDocument();
    });
  });
});
```

### Integration Tests

```typescript
// deepLinking.integration.test.ts
describe('Deep Linking Integration', () => {
  it('should generate table link and navigate to PulseQL', async () => {
    // Setup
    const table = createMockTable();
    
    // Generate link
    const service = new DeepLinkingService();
    const response = await service.generateDeepLinkViaAPI({
      type: 'table',
      connectionId: table.connectionId,
      schemaName: table.schemaName,
      tableName: table.name,
    });

    // Verify
    expect(response.success).toBe(true);
    expect(response.deep_link).toContain('pulsar_link=table:');
    expect(response.deep_link).toContain(table.name);
  });
});
```

## Troubleshooting

### Common Issues

1. **Button doesn't appear**: Check if user has required permissions
2. **Link generation fails**: Verify backend endpoint is accessible
3. **Clipboard copy fails**: Ensure HTTPS is used (required for clipboard API)
4. **Link opens but resource not found**: Check connection ID and resource names are correct

### Debug Mode

Enable debug logging:

```typescript
// In DeepLinkingService constructor
if (process.env.NODE_ENV === 'development') {
  console.log('[DeepLink] Service initialized');
}
```
