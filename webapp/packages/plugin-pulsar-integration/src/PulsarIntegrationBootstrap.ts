/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { Bootstrap, injectable } from '@cloudbeaver/core-di';
import { WorkspaceModeService } from './WorkspaceModeService.js';

/**
 * Bootstrap for Pulsar integration plugin
 * Initializes workspace mode and applies customizations
 */
@injectable()
export class PulsarIntegrationBootstrap extends Bootstrap {
  constructor(private readonly workspaceModeService: WorkspaceModeService) {
    super();
  }

  async load(): Promise<void> {
    // Initialize workspace mode from URL parameters
    this.workspaceModeService.initializeFromURL();

    // Apply mode-specific customizations
    if (this.workspaceModeService.isPulsarMode) {
      await this.applyPulsarModeCustomizations();
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
  }

  private applyCustomBranding(branding: any): void {
    if (branding.title) {
      // Update page title
      document.title = branding.title;
    }

    if (branding.color) {
      // Apply primary color as CSS variable
      document.documentElement.style.setProperty('--theme-primary', branding.color);
    }

    // Store return URL for "Back to Pulsar" functionality
    if (branding.returnUrl) {
      sessionStorage.setItem('pulsar_return_url', branding.returnUrl);
    }
  }

  private applyTheme(themeName: string): void {
    // Apply theme via data attribute
    document.body.setAttribute('data-theme', themeName);
  }
}
