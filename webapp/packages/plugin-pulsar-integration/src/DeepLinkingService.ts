/*
 * CloudBeaver - Pulsar Integration Plugin
 * Deep Linking Service
 * 
 * Enables direct navigation to specific tables, schemas, and queries from Pulsar
 */

import { injectable } from '@cloudbeaver/core-di';
import { makeObservable, observable, action } from 'mobx';

export interface DeepLinkTarget {
  type: 'table' | 'schema' | 'query' | 'connection' | 'workspace';
  connectionId?: string;
  schemaName?: string;
  tableName?: string;
  query?: string;
  workspaceId?: string;
}

export interface DeepLinkParams {
  target: string; // Format: connection/schema/table or query:base64query
  action?: 'view' | 'query' | 'edit' | 'analyze';
  highlight?: string; // Column or field to highlight
  filter?: string; // Pre-applied filter
}

export interface GenerateLinkResponse {
  success: boolean;
  deep_link?: string;
  short_url?: string;
  error?: string;
}

const DEEP_LINK_PARAM = 'pulsar_link';

@injectable()
export class DeepLinkingService {
  private currentTarget: DeepLinkTarget | null = null;
  private pendingAction: (() => void) | null = null;

  constructor() {
    makeObservable<this, 'currentTarget' | 'pendingAction'>(this, {
      currentTarget: observable,
      pendingAction: observable,
      setCurrentTarget: action,
      clearCurrentTarget: action,
    });
  }

  /**
   * Parse deep link from URL
   */
  parseDeepLink(): DeepLinkTarget | null {
    const params = new URLSearchParams(window.location.search);
    const linkParam = params.get(DEEP_LINK_PARAM);

    if (!linkParam) {
      return null;
    }

    try {
      return this.decodeDeepLink(linkParam);
    } catch (error) {
      console.error('Failed to parse deep link:', error);
      return null;
    }
  }

  /**
   * Decode deep link parameter
   */
  private decodeDeepLink(encoded: string): DeepLinkTarget {
    // Format: type:data
    // Examples:
    // - table:connectionId/schema/table
    // - query:base64(sqlquery)
    // - schema:connectionId/schema
    // - workspace:workspaceId

    const [type, data] = encoded.split(':', 2);

    switch (type) {
      case 'table': {
        const parts = data.split('/');
        if (parts.length !== 3) {
          throw new Error('Invalid table deep link format');
        }
        return {
          type: 'table',
          connectionId: decodeURIComponent(parts[0]),
          schemaName: decodeURIComponent(parts[1]),
          tableName: decodeURIComponent(parts[2]),
        };
      }

      case 'schema': {
        const parts = data.split('/');
        if (parts.length !== 2) {
          throw new Error('Invalid schema deep link format');
        }
        return {
          type: 'schema',
          connectionId: decodeURIComponent(parts[0]),
          schemaName: decodeURIComponent(parts[1]),
        };
      }

      case 'query': {
        // Decode base64 SQL query
        const query = atob(data);
        return {
          type: 'query',
          query,
        };
      }

      case 'connection': {
        return {
          type: 'connection',
          connectionId: decodeURIComponent(data),
        };
      }

      case 'workspace': {
        return {
          type: 'workspace',
          workspaceId: decodeURIComponent(data),
        };
      }

      default:
        throw new Error(`Unknown deep link type: ${type}`);
    }
  }

  /**
   * Generate deep link URL
   */
  generateDeepLink(target: DeepLinkTarget, baseUrl?: string): string {
    const encoded = this.encodeDeepLink(target);
    const url = new URL(baseUrl || window.location.origin);
    url.searchParams.set(DEEP_LINK_PARAM, encoded);
    return url.toString();
  }

  /**
   * Encode deep link target
   */
  private encodeDeepLink(target: DeepLinkTarget): string {
    switch (target.type) {
      case 'table':
        return `table:${encodeURIComponent(target.connectionId!)}/${encodeURIComponent(target.schemaName!)}/${encodeURIComponent(target.tableName!)}`;
      
      case 'schema':
        return `schema:${encodeURIComponent(target.connectionId!)}/${encodeURIComponent(target.schemaName!)}`;
      
      case 'query':
        // Encode query as base64
        return `query:${btoa(target.query!)}`;
      
      case 'connection':
        return `connection:${encodeURIComponent(target.connectionId!)}`;
      
      case 'workspace':
        return `workspace:${encodeURIComponent(target.workspaceId!)}`;
      
      default:
        throw new Error('Invalid deep link target');
    }
  }

  /**
   * Set current deep link target
   */
  setCurrentTarget(target: DeepLinkTarget | null): void {
    this.currentTarget = target;
  }

  /**
   * Get current deep link target
   */
  getCurrentTarget(): DeepLinkTarget | null {
    return this.currentTarget;
  }

  /**
   * Clear current target
   */
  clearCurrentTarget(): void {
    this.currentTarget = null;
    this.pendingAction = null;
  }

  /**
   * Execute deep link navigation
   * Returns true if navigation was handled, false otherwise
   */
  async executeDeepLink(target: DeepLinkTarget): Promise<boolean> {
    this.setCurrentTarget(target);

    // Clean up URL
    this.removeDeepLinkFromUrl();

    // Target will be handled by registered handlers
    return true;
  }

  /**
   * Register action to execute when deep link is ready
   */
  onReady(action: () => void): void {
    if (this.currentTarget) {
      action();
    } else {
      this.pendingAction = action;
    }
  }

  /**
   * Execute pending action if any
   */
  executePendingAction(): void {
    if (this.pendingAction) {
      this.pendingAction();
      this.pendingAction = null;
    }
  }

  /**
   * Remove deep link parameter from URL
   */
  private removeDeepLinkFromUrl(): void {
    const url = new URL(window.location.href);
    url.searchParams.delete(DEEP_LINK_PARAM);
    
    // Update URL without reload
    window.history.replaceState({}, '', url.toString());
  }

  /**
   * Generate shareable link for current view
   */
  generateShareLink(target: DeepLinkTarget): string {
    return this.generateDeepLink(target);
  }

  /**
   * Copy deep link to clipboard
   */
  async copyToClipboard(target: DeepLinkTarget): Promise<boolean> {
    try {
      const link = this.generateDeepLink(target);
      await navigator.clipboard.writeText(link);
      return true;
    } catch (error) {
      console.error('Failed to copy deep link to clipboard:', error);
      return false;
    }
  }

  /**
   * Generate deep link via backend API
   * This calls the Pulsar backend to generate a deep link with proper validation
   */
  async generateDeepLinkViaAPI(target: DeepLinkTarget): Promise<GenerateLinkResponse> {
    try {
      const response = await fetch('/api/pulseql/generate-link', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          target_type: target.type,
          connection_id: target.connectionId,
          schema_name: target.schemaName,
          table_name: target.tableName,
          query: target.query,
          workspace_id: target.workspaceId,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
        return {
          success: false,
          error: errorData.error || `HTTP ${response.status}: ${response.statusText}`,
        };
      }

      const data: GenerateLinkResponse = await response.json();
      return data;

    } catch (error) {
      console.error('Failed to generate deep link via API:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Failed to generate deep link',
      };
    }
  }

  /**
   * Generate and copy deep link via backend API
   */
  async generateAndCopyLink(target: DeepLinkTarget): Promise<{ success: boolean; error?: string }> {
    try {
      const response = await this.generateDeepLinkViaAPI(target);
      
      if (!response.success || !response.deep_link) {
        return {
          success: false,
          error: response.error || 'Failed to generate link',
        };
      }

      // Copy to clipboard
      await navigator.clipboard.writeText(response.deep_link);
      
      return { success: true };

    } catch (error) {
      console.error('Failed to generate and copy link:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Failed to copy link',
      };
    }
  }
}
