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

    this.config = {
      mode: (params.get('mode') as WorkspaceMode) || 'standalone',
      theme: params.get('theme') || undefined,
      hideNavigation: validHideNavItems,
      readonlyConnections: params.get('readonly_connections') === 'true',
      workspaceId: params.get('workspace_id') || undefined,
      autoConnect: params.get('auto_connect') === 'true',
      hideHeader: params.get('hide_header') === 'true',
      hideFooter: params.get('hide_footer') === 'true',
      customBranding: this.parseCustomBranding(params),
    };

    // Store in session for persistence across page loads
    if (this.isPulsarMode || this.isEmbeddedMode) {
      sessionStorage.setItem('workspace_mode_config', JSON.stringify(this.config));
    }
  }

  /**
   * Parse custom branding from URL parameters
   * Validates color parameter to prevent invalid CSS
   */
  private parseCustomBranding(params: URLSearchParams): WorkspaceModeConfig['customBranding'] {
    const logo = params.get('brand_logo');
    const title = params.get('brand_title');
    const color = params.get('brand_color');
    const returnUrl = params.get('return_url');

    if (!logo && !title && !color && !returnUrl) {
      return undefined;
    }

    // Validate color if provided
    let validatedColor = color;
    if (color) {
      // Basic hex color validation
      const hexColorRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/;
      if (!hexColorRegex.test(color)) {
        console.warn(
          `[WorkspaceModeService] Invalid color format: ${color}. ` +
            `Expected hex format (e.g., #1976D2). Color will be ignored.`
        );
        validatedColor = undefined;
      }
    }

    return { logo, title, color: validatedColor, returnUrl };
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
