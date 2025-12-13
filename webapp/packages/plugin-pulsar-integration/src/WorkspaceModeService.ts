/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { injectable } from '@cloudbeaver/core-di';
import { observable, computed, action, makeObservable } from 'mobx';

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
    returnUrl?: string;
  };
}

// Whitelist of navigation items that can be safely hidden
const HIDEABLE_NAV_ITEMS = [
  'admin',
  'settings',
  'connections',
  'users',
  'server',
  'drivers',
  'projects',
  'administration',
  'server-settings',
  'user-management',
  'driver-management',
  'server-config',
];

/**
 * Service to manage workspace mode configuration from URL parameters
 * Supports Pulsar integration, embedded mode, and standalone mode
 */
@injectable()
export class WorkspaceModeService {
  @observable
  private config: WorkspaceModeConfig = {
    mode: 'standalone',
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

  constructor() {
    makeObservable(this);
  }

  /**
   * Initialize workspace mode from URL parameters
   * Should be called during application bootstrap
   */
  @action
  initializeFromURL(): void {
    const params = new URLSearchParams(window.location.search);

    const hideNavItems = params.get('hide_nav')?.split(',').map(s => s.trim()) || [];
    const validHideNavItems = hideNavItems.filter(item => HIDEABLE_NAV_ITEMS.includes(item));

    // Warn about invalid nav items
    const invalidItems = hideNavItems.filter(item => !HIDEABLE_NAV_ITEMS.includes(item));
    if (invalidItems.length > 0) {
      console.warn(
        `[WorkspaceModeService] Invalid navigation items will be ignored: ${invalidItems.join(', ')}. ` +
          `Allowed items: ${HIDEABLE_NAV_ITEMS.join(', ')}`
      );
    }

    // Validate mode parameter
    const modeParam = params.get('mode');
    const validModes: WorkspaceMode[] = ['standalone', 'pulsar', 'embedded'];
    const mode = modeParam && validModes.includes(modeParam as WorkspaceMode) 
      ? (modeParam as WorkspaceMode) 
      : 'standalone';

    if (modeParam && !validModes.includes(modeParam as WorkspaceMode)) {
      console.warn(
        `[WorkspaceModeService] Invalid mode '${modeParam}'. Defaulting to 'standalone'. ` +
          `Valid modes: ${validModes.join(', ')}`
      );
    }

    // Sanitize workspace ID to prevent injection
    const workspaceId = this.sanitizeWorkspaceId(params.get('workspace_id'));

    this.config = {
      mode,
      theme: this.sanitizeThemeName(params.get('theme')),
      hideNavigation: validHideNavItems,
      readonlyConnections: params.get('readonly_connections') === 'true',
      workspaceId,
      autoConnect: params.get('auto_connect') === 'true',
      hideHeader: params.get('hide_header') === 'true',
      hideFooter: params.get('hide_footer') === 'true',
      customBranding: this.parseCustomBranding(params),
    };

    // Store in session for persistence across page loads
    if (this.isPulsarMode || this.isEmbeddedMode) {
      sessionStorage.setItem('workspace_mode_config', JSON.stringify(this.config));
    }

    // Clear URL parameters for security after reading
    this.clearSensitiveURLParameters();
  }

  /**
   * Sanitize workspace ID to prevent XSS
   */
  private sanitizeWorkspaceId(workspaceId: string | null): string | undefined {
    if (!workspaceId) {
      return undefined;
    }

    // Allow only alphanumeric, dash, and underscore
    const sanitized = workspaceId.replace(/[^a-zA-Z0-9\-_]/g, '');
    
    if (sanitized !== workspaceId) {
      console.warn(
        `[WorkspaceModeService] Workspace ID contained invalid characters and was sanitized: ` +
          `'${workspaceId}' -> '${sanitized}'`
      );
    }

    return sanitized || undefined;
  }

  /**
   * Sanitize theme name to prevent XSS
   */
  private sanitizeThemeName(theme: string | null): string | undefined {
    if (!theme) {
      return undefined;
    }

    // Allow only alphanumeric and dash
    const sanitized = theme.replace(/[^a-zA-Z0-9\-]/g, '');
    
    if (sanitized !== theme) {
      console.warn(
        `[WorkspaceModeService] Theme name contained invalid characters and was sanitized: ` +
          `'${theme}' -> '${sanitized}'`
      );
    }

    return sanitized || undefined;
  }

  /**
   * Clear sensitive URL parameters after reading
   */
  private clearSensitiveURLParameters(): void {
    try {
      const url = new URL(window.location.href);
      // Clear all search parameters
      url.search = '';
      window.history.replaceState({}, '', url.toString());
    } catch (error) {
      console.warn('[WorkspaceModeService] Failed to clear URL parameters:', error);
    }
  }

  /**
   * Parse custom branding from URL parameters
   * Validates and sanitizes all inputs to prevent XSS
   */
  private parseCustomBranding(params: URLSearchParams): WorkspaceModeConfig['customBranding'] {
    const logo = this.sanitizeURL(params.get('brand_logo'));
    const title = this.sanitizeText(params.get('brand_title'));
    const color = this.validateColor(params.get('brand_color'));
    const returnUrl = this.sanitizeURL(params.get('return_url'));

    if (!logo && !title && !color && !returnUrl) {
      return undefined;
    }

    return { logo, title, color, returnUrl };
  }

  /**
   * Validate and sanitize URL to prevent XSS
   */
  private sanitizeURL(url: string | null): string | undefined {
    if (!url) {
      return undefined;
    }

    try {
      // Parse URL to validate format
      const parsedUrl = new URL(url);

      // Allow only HTTP and HTTPS protocols
      if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
        console.warn(
          `[WorkspaceModeService] URL protocol not allowed: ${parsedUrl.protocol}. ` +
            `Only http: and https: are permitted.`
        );
        return undefined;
      }

      return parsedUrl.toString();
    } catch (error) {
      console.warn(`[WorkspaceModeService] Invalid URL format: ${url}`);
      return undefined;
    }
  }

  /**
   * Sanitize text to prevent XSS
   * Uses browser's built-in text encoding for efficiency
   */
  private sanitizeText(text: string | null): string | undefined {
    if (!text) {
      return undefined;
    }

    // Encode HTML entities to prevent XSS
    // This approach is simple and efficient for text-only content
    const encoded = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;')
      .replace(/\//g, '&#x2F;');

    // Limit length to prevent DoS
    const maxLength = 200;
    if (encoded.length > maxLength) {
      console.warn(
        `[WorkspaceModeService] Text exceeds maximum length (${maxLength} chars). Truncating.`
      );
      return encoded.substring(0, maxLength);
    }

    return encoded;
  }

  /**
   * Validate color format
   */
  private validateColor(color: string | null): string | undefined {
    if (!color) {
      return undefined;
    }

    // Basic hex color validation
    const hexColorRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/;
    if (!hexColorRegex.test(color)) {
      console.warn(
        `[WorkspaceModeService] Invalid color format: ${color}. ` +
          `Expected hex format (e.g., #1976D2). Color will be ignored.`
      );
      return undefined;
    }

    return color;
  }

  /**
   * Check if a navigation item should be hidden
   */
  shouldHideNavItem(itemId: string): boolean {
    return this.config.hideNavigation?.includes(itemId) || false;
  }

  /**
   * Get workspace/connection ID to auto-connect to
   */
  getWorkspaceId(): string | undefined {
    return this.config.workspaceId;
  }

  /**
   * Check if should auto-connect on load
   */
  shouldAutoConnect(): boolean {
    return this.config.autoConnect || false;
  }

  /**
   * Get custom branding configuration
   */
  getCustomBranding(): WorkspaceModeConfig['customBranding'] {
    return this.config.customBranding;
  }

  /**
   * Get theme name from config
   */
  getThemeName(): string | undefined {
    return this.config.theme;
  }
}
