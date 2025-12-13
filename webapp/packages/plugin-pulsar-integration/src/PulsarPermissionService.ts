/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { injectable } from '@cloudbeaver/core-di';
import { computed, makeObservable } from 'mobx';
import { PulsarSSOService } from './PulsarSSOService.js';

/**
 * Permission types that can be granted to Pulsar users
 */
export enum PulsarPermission {
  // Query permissions
  QUERY_EXECUTE = 'query.execute',
  QUERY_SAVE = 'query.save',
  QUERY_SHARE = 'query.share',

  // Data permissions
  DATA_VIEW = 'data.view',
  DATA_EXPORT = 'data.export',
  DATA_EDIT = 'data.edit',
  DATA_DELETE = 'data.delete',

  // Connection permissions
  CONNECTION_VIEW = 'connection.view',
  CONNECTION_CREATE = 'connection.create',
  CONNECTION_EDIT = 'connection.edit',
  CONNECTION_DELETE = 'connection.delete',

  // Schema permissions
  SCHEMA_VIEW = 'schema.view',
  SCHEMA_CREATE = 'schema.create',
  SCHEMA_EDIT = 'schema.edit',
  SCHEMA_DELETE = 'schema.delete',

  // Administration permissions
  ADMIN_USERS = 'admin.users',
  ADMIN_SETTINGS = 'admin.settings',
  ADMIN_LOGS = 'admin.logs',
}

/**
 * Service for managing Pulsar-specific permissions and RBAC
 * Integrates with PulsarSSOService for user permission checking
 */
@injectable()
export class PulsarPermissionService {
  constructor(private readonly ssoService: PulsarSSOService) {
    makeObservable(this);
  }

  @computed
  get isPulsarUser(): boolean {
    return this.ssoService.isUserAuthenticated;
  }

  /**
   * Check if current user has a specific permission
   */
  hasPermission(permission: PulsarPermission | string): boolean {
    if (!this.isPulsarUser) {
      // Non-Pulsar users have all permissions (normal CloudBeaver auth)
      return true;
    }

    return this.ssoService.hasPermission(permission);
  }

  /**
   * Check if user has any of the specified permissions
   */
  hasAnyPermission(permissions: (PulsarPermission | string)[]): boolean {
    if (!this.isPulsarUser) {
      return true;
    }

    return this.ssoService.hasAnyPermission(permissions);
  }

  /**
   * Check if user has all of the specified permissions
   */
  hasAllPermissions(permissions: (PulsarPermission | string)[]): boolean {
    if (!this.isPulsarUser) {
      return true;
    }

    return this.ssoService.hasAllPermissions(permissions);
  }

  /**
   * Query-specific permission checks
   */
  canExecuteQuery(): boolean {
    return this.hasPermission(PulsarPermission.QUERY_EXECUTE);
  }

  canSaveQuery(): boolean {
    return this.hasPermission(PulsarPermission.QUERY_SAVE);
  }

  canShareQuery(): boolean {
    return this.hasPermission(PulsarPermission.QUERY_SHARE);
  }

  /**
   * Data-specific permission checks
   */
  canViewData(): boolean {
    return this.hasPermission(PulsarPermission.DATA_VIEW);
  }

  canExportData(): boolean {
    return this.hasPermission(PulsarPermission.DATA_EXPORT);
  }

  canEditData(): boolean {
    return this.hasPermission(PulsarPermission.DATA_EDIT);
  }

  canDeleteData(): boolean {
    return this.hasPermission(PulsarPermission.DATA_DELETE);
  }

  /**
   * Connection-specific permission checks
   */
  canViewConnections(): boolean {
    return this.hasPermission(PulsarPermission.CONNECTION_VIEW);
  }

  canCreateConnection(): boolean {
    return this.hasPermission(PulsarPermission.CONNECTION_CREATE);
  }

  canEditConnection(): boolean {
    return this.hasPermission(PulsarPermission.CONNECTION_EDIT);
  }

  canDeleteConnection(): boolean {
    return this.hasPermission(PulsarPermission.CONNECTION_DELETE);
  }

  /**
   * Schema-specific permission checks
   */
  canViewSchema(): boolean {
    return this.hasPermission(PulsarPermission.SCHEMA_VIEW);
  }

  canCreateSchema(): boolean {
    return this.hasPermission(PulsarPermission.SCHEMA_CREATE);
  }

  canEditSchema(): boolean {
    return this.hasPermission(PulsarPermission.SCHEMA_EDIT);
  }

  canDeleteSchema(): boolean {
    return this.hasPermission(PulsarPermission.SCHEMA_DELETE);
  }

  /**
   * Administration permission checks
   */
  canManageUsers(): boolean {
    return this.hasPermission(PulsarPermission.ADMIN_USERS);
  }

  canManageSettings(): boolean {
    return this.hasPermission(PulsarPermission.ADMIN_SETTINGS);
  }

  canViewLogs(): boolean {
    return this.hasPermission(PulsarPermission.ADMIN_LOGS);
  }

  /**
   * Get all permissions for current user
   */
  getUserPermissions(): string[] {
    return this.ssoService.userPermissions;
  }
}
