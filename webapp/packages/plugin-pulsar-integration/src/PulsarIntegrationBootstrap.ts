/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { Bootstrap, injectable } from '@cloudbeaver/core-di';
import { WorkspaceModeService, WorkspaceModeConfig } from './WorkspaceModeService.js';
import { PulsarSSOService } from './PulsarSSOService.js';

/**
 * Bootstrap for Pulsar integration plugin
 * Initializes workspace mode and applies customizations
 */
@injectable()
export class PulsarIntegrationBootstrap extends Bootstrap {
  constructor(
    private readonly workspaceModeService: WorkspaceModeService,
    private readonly ssoService: PulsarSSOService
  ) {
    super();
  }

  async load(): Promise<void> {
    try {
      // Step 1: Check for SSO token and authenticate
      const ssoAuthenticated = await this.ssoService.initializeFromURL();

      // Step 2: Initialize workspace mode from URL parameters
      this.workspaceModeService.initializeFromURL();

      // Step 3: Apply mode-specific customizations
      if (this.workspaceModeService.isPulsarMode) {
        await this.applyPulsarModeCustomizations();
      }

      // Step 4: Verify SSO for Pulsar mode
      if (this.workspaceModeService.isPulsarMode && !ssoAuthenticated) {
        console.warn(
          '[PulsarIntegrationBootstrap] Pulsar mode enabled but SSO authentication failed. ' +
            'User may need to re-authenticate.'
        );
        this.handleSSOFailure();
      }
    } catch (error) {
      console.error('[PulsarIntegrationBootstrap] Failed to initialize Pulsar integration:', error);
      throw error;
    }
  }

  private async applyPulsarModeCustomizations(): Promise<void> {
    // Apply custom branding if specified
    const branding = this.workspaceModeService.getCustomBranding();
    if (branding) {
      this.applyCustomBranding(branding);
    }

    // Apply theme if specified
    const themeName = this.workspaceModeService.getThemeName();
    if (themeName) {
      this.applyTheme(themeName);
    }

    // Apply mode attribute for CSS selectors
    this.applyModeAttribute();
  }

  private applyCustomBranding(branding: NonNullable<WorkspaceModeConfig['customBranding']>): void {
    // Sanitized in WorkspaceModeService, safe to use
    if (branding.title) {
      // Update page title
      document.title = branding.title;
    }

    if (branding.color) {
      // Apply primary color as CSS variable
      // Color is already validated as hex format
      document.documentElement.style.setProperty('--theme-primary', branding.color);
    }

    // Store return URL for "Back to Pulsar" functionality
    if (branding.returnUrl) {
      sessionStorage.setItem('pulsar_return_url', branding.returnUrl);
    }
  }

  private applyTheme(themeName: string): void {
    // Apply theme via data attribute (sanitized theme name)
    document.body.setAttribute('data-theme', themeName);
  }

  private applyModeAttribute(): void {
    // Apply mode data attribute for CSS selectors
    const mode = this.workspaceModeService.mode;
    document.body.setAttribute('data-mode', mode);
  }

  private handleSSOFailure(): void {
    // Get error message (already sanitized in PulsarSSOService)
    // Error messages are controlled strings, not user input
    const errorMessage = this.ssoService.authError || 'SSO authentication failed';

    // Create error notification element
    const notification = document.createElement('div');
    notification.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      background: #f44336;
      color: white;
      padding: 12px;
      text-align: center;
      z-index: 10000;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
    `;
    // Use textContent to prevent any XSS, even though errorMessage is controlled
    notification.textContent = `Authentication Error: ${errorMessage}`;

    document.body.appendChild(notification);

    // Auto-remove after 10 seconds
    setTimeout(() => {
      notification.remove();
    }, 10000);
  }
}
