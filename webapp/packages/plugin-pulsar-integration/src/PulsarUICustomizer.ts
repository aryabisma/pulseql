/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { injectable } from '@cloudbeaver/core-di';
import { WorkspaceModeService } from './WorkspaceModeService.js';
import { PulsarPermissionService } from './PulsarPermissionService.js';

/**
 * Service for customizing UI elements based on workspace mode and permissions
 * Provides methods to determine which UI elements should be visible/hidden
 */
@injectable()
export class PulsarUICustomizer {
  constructor(
    private readonly workspaceModeService: WorkspaceModeService,
    private readonly permissionService: PulsarPermissionService
  ) {}

  /**
   * Check if a specific UI element should be hidden
   */
  shouldHideElement(elementId: string): boolean {
    // In Pulsar mode, hide based on configuration
    if (this.workspaceModeService.isPulsarMode) {
      return this.workspaceModeService.shouldHideNavItem(elementId);
    }
    
    // In standalone mode, show everything
    return false;
  }

  /**
   * Check if connection management UI should be hidden
   */
  shouldHideConnectionManagement(): boolean {
    if (this.workspaceModeService.isPulsarMode) {
      return true; // Always hide in Pulsar mode
    }
    
    return !this.permissionService.canCreateConnection();
  }

  /**
   * Check if a connection should be read-only
   */
  isConnectionReadOnly(): boolean {
    return this.workspaceModeService.readonlyConnections;
  }

  /**
   * Check if administration menu should be hidden
   */
  shouldHideAdminMenu(): boolean {
    if (this.workspaceModeService.isPulsarMode) {
      return true; // Always hide in Pulsar mode
    }
    
    return !this.permissionService.canManageSettings();
  }

  /**
   * Check if user management should be hidden
   */
  shouldHideUserManagement(): boolean {
    if (this.workspaceModeService.isPulsarMode) {
      return true; // Always hide in Pulsar mode
    }
    
    return !this.permissionService.canManageUsers();
  }

  /**
   * Check if server settings should be hidden
   */
  shouldHideServerSettings(): boolean {
    if (this.workspaceModeService.isPulsarMode) {
      return true; // Always hide in Pulsar mode
    }
    
    return !this.permissionService.canManageSettings();
  }

  /**
   * Check if data export should be enabled
   */
  canExportData(): boolean {
    return this.permissionService.canExportData();
  }

  /**
   * Check if data editing should be enabled
   */
  canEditData(): boolean {
    return this.permissionService.canEditData();
  }

  /**
   * Check if query save should be enabled
   */
  canSaveQuery(): boolean {
    return this.permissionService.canSaveQuery();
  }

  /**
   * Get list of toolbar items to show in SQL editor
   */
  getSqlEditorToolbarItems(): string[] {
    const items: string[] = [];

    // Always show execute button
    if (this.permissionService.canExecuteQuery()) {
      items.push('execute');
    }

    // Conditional buttons based on permissions
    if (this.canSaveQuery()) {
      items.push('save');
    }

    if (this.canExportData()) {
      items.push('export');
    }

    // Show format only in standalone mode
    if (!this.workspaceModeService.isPulsarMode) {
      items.push('format');
      items.push('explain');
    }

    return items;
  }

  /**
   * Get list of result viewer actions to show
   */
  getResultViewerActions(): string[] {
    const actions: string[] = [];

    // Always show copy
    actions.push('copy');

    // Conditional actions
    if (this.canExportData()) {
      actions.push('export-csv');
      actions.push('export-json');
      actions.push('export-excel');
    }

    if (this.canEditData()) {
      actions.push('edit');
    }

    return actions;
  }

  /**
   * Check if "Back to Pulsar" button should be shown
   */
  shouldShowBackToPulsarButton(): boolean {
    return this.workspaceModeService.isPulsarMode && 
           !!this.workspaceModeService.getCustomBranding()?.returnUrl;
  }

  /**
   * Get the return URL for "Back to Pulsar" button
   * Checks session storage where URL is stored during initialization
   */
  getBackToPulsarUrl(): string | null {
    return sessionStorage.getItem('pulsar_return_url');
  }

  /**
   * Navigate back to Pulsar
   */
  navigateBackToPulsar(): void {
    const returnUrl = this.getBackToPulsarUrl();
    if (returnUrl) {
      window.location.href = returnUrl;
    }
  }

  /**
   * Get CSS classes for UI customization
   */
  getWorkspaceClasses(): string[] {
    const classes: string[] = [];

    if (this.workspaceModeService.isPulsarMode) {
      classes.push('pulsar-mode');
    }

    if (this.workspaceModeService.isEmbeddedMode) {
      classes.push('embedded-mode');
    }

    if (this.isConnectionReadOnly()) {
      classes.push('readonly-connections');
    }

    return classes;
  }
}
