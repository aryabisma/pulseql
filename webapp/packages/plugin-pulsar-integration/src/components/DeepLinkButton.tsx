/*
 * CloudBeaver - Pulsar Integration Plugin
 * Deep Link Button Component
 * 
 * Button component to generate and copy deep links from Pulsar to PulseQL
 */

import { observer } from 'mobx-react-lite';
import { useCallback, useState } from 'react';
import { Button } from '@cloudbeaver/core-blocks';
import { useService } from '@cloudbeaver/core-di';
import { NotificationService } from '@cloudbeaver/core-events';
import { DeepLinkingService, type DeepLinkTarget } from '../DeepLinkingService.js';

interface ValidationFields {
  connectionId?: string;
  schemaName?: string;
  tableName?: string;
  query?: string;
}

function validateRequiredFields(
  targetType: 'table' | 'schema' | 'query' | 'connection',
  fields: ValidationFields
): string | null {
  switch (targetType) {
    case 'table':
      if (!fields.connectionId || !fields.schemaName || !fields.tableName) {
        return 'Connection ID, schema name, and table name are required for table links';
      }
      break;
    case 'schema':
      if (!fields.connectionId || !fields.schemaName) {
        return 'Connection ID and schema name are required for schema links';
      }
      break;
    case 'query':
      if (!fields.query) {
        return 'Query is required for query links';
      }
      break;
    case 'connection':
      if (!fields.connectionId) {
        return 'Connection ID is required for connection links';
      }
      break;
  }
  return null;
}

interface DeepLinkButtonProps {
  targetType: 'table' | 'schema' | 'query' | 'connection';
  connectionId?: string;
  schemaName?: string;
  tableName?: string;
  query?: string;
  label?: string;
  className?: string;
  disabled?: boolean;
  mode?: 'button' | 'icon';
}

export const DeepLinkButton = observer<DeepLinkButtonProps>(function DeepLinkButton({
  targetType,
  connectionId,
  schemaName,
  tableName,
  query,
  label = 'Open in PulseQL',
  className,
  disabled = false,
  mode = 'button',
}) {
  const deepLinkingService = useService(DeepLinkingService);
  const notificationService = useService(NotificationService);
  const [isGenerating, setIsGenerating] = useState(false);

  const handleGenerateLink = useCallback(async () => {
    try {
      setIsGenerating(true);

      // Build target object
      const target: DeepLinkTarget = {
        type: targetType,
        connectionId,
        schemaName,
        tableName,
        query,
      };

      // Validate required fields based on target type
      const validationError = validateRequiredFields(targetType, { connectionId, schemaName, tableName, query });
      if (validationError) {
        notificationService.logError({
          title: 'Invalid Parameters',
          message: validationError,
        });
        return;
      }

      // Generate and copy link
      const result = await deepLinkingService.generateAndCopyLink(target);

      if (result.success) {
        notificationService.logSuccess({
          title: 'Deep Link Copied',
          message: 'Deep link has been copied to clipboard',
        });
      } else {
        notificationService.logError({
          title: 'Failed to Generate Link',
          message: result.error || 'An error occurred while generating the deep link',
        });
      }

    } catch (error) {
      console.error('Error generating deep link:', error);
      notificationService.logError({
        title: 'Error',
        message: error instanceof Error ? error.message : 'Failed to generate deep link',
      });
    } finally {
      setIsGenerating(false);
    }
  }, [
    targetType,
    connectionId,
    schemaName,
    tableName,
    query,
    deepLinkingService,
    notificationService,
  ]);

  if (mode === 'icon') {
    return (
      <Button
        className={className}
        disabled={disabled || isGenerating}
        loading={isGenerating}
        onClick={handleGenerateLink}
        title={label}
        icon="/icons/link.svg"
      />
    );
  }

  return (
    <Button
      className={className}
      disabled={disabled || isGenerating}
      loading={isGenerating}
      onClick={handleGenerateLink}
    >
      {label}
    </Button>
  );
});
