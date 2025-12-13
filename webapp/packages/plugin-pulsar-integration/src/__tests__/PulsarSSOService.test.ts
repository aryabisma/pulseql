/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { PulsarSSOService } from '../PulsarSSOService';

describe('PulsarSSOService', () => {
  let service: PulsarSSOService;

  beforeEach(() => {
    service = new PulsarSSOService();
  });

  describe('Token Validation', () => {
    it('should reject malformed token', async () => {
      const result = await service.validateToken('invalid.token');
      expect(result.valid).toBe(false);
      expect(result.errorCode).toBe('MALFORMED');
    });

    it('should validate token structure', async () => {
      // Create a simple token (header.payload.signature)
      const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
      const payload = btoa(JSON.stringify({
        iss: 'pulsar',
        sub: 'user123',
        aud: 'pulseql',
        exp: Math.floor(Date.now() / 1000) + 3600,
        iat: Math.floor(Date.now() / 1000),
        nbf: Math.floor(Date.now() / 1000),
      }));
      const signature = 'fake-signature';
      const token = `${header}.${payload}.${signature}`;

      const result = await service.validateToken(token);
      expect(result.valid).toBe(true);
    });

    it('should reject expired token', async () => {
      const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
      const payload = btoa(JSON.stringify({
        iss: 'pulsar',
        sub: 'user123',
        aud: 'pulseql',
        exp: Math.floor(Date.now() / 1000) - 3600, // Expired
        iat: Math.floor(Date.now() / 1000) - 7200,
        nbf: Math.floor(Date.now() / 1000) - 7200,
      }));
      const token = `${header}.${payload}.fake`;

      const result = await service.validateToken(token);
      expect(result.valid).toBe(false);
      expect(result.errorCode).toBe('EXPIRED');
    });

    it('should reject token with wrong issuer', async () => {
      const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
      const payload = btoa(JSON.stringify({
        iss: 'wrong-issuer',
        sub: 'user123',
        aud: 'pulseql',
        exp: Math.floor(Date.now() / 1000) + 3600,
        iat: Math.floor(Date.now() / 1000),
        nbf: Math.floor(Date.now() / 1000),
      }));
      const token = `${header}.${payload}.fake`;

      const result = await service.validateToken(token);
      expect(result.valid).toBe(false);
      expect(result.errorCode).toBe('INVALID_SIGNATURE');
    });

    it('should reject token with wrong audience', async () => {
      const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
      const payload = btoa(JSON.stringify({
        iss: 'pulsar',
        sub: 'user123',
        aud: 'wrong-audience',
        exp: Math.floor(Date.now() / 1000) + 3600,
        iat: Math.floor(Date.now() / 1000),
        nbf: Math.floor(Date.now() / 1000),
      }));
      const token = `${header}.${payload}.fake`;

      const result = await service.validateToken(token);
      expect(result.valid).toBe(false);
      expect(result.errorCode).toBe('INVALID_AUDIENCE');
    });
  });

  describe('Permission Checking', () => {
    beforeEach(async () => {
      // Create valid token with permissions
      const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
      const payload = btoa(JSON.stringify({
        iss: 'pulsar',
        sub: 'user123',
        aud: 'pulseql',
        exp: Math.floor(Date.now() / 1000) + 3600,
        iat: Math.floor(Date.now() / 1000),
        nbf: Math.floor(Date.now() / 1000),
        jti: 'test-token-id',
        user: {
          userId: 'user123',
          displayName: 'Test User',
          email: 'test@example.com',
          authRole: 'analyst',
        },
        permissions: ['query.execute', 'data.view', 'data.export'],
      }));
      const token = `${header}.${payload}.fake`;

      await service.validateToken(token);
      (service as any).handleSuccessfulAuthentication(token, JSON.parse(atob(payload)));
    });

    it('should check if user has specific permission', () => {
      expect(service.hasPermission('query.execute')).toBe(true);
      expect(service.hasPermission('data.view')).toBe(true);
      expect(service.hasPermission('admin.users')).toBe(false);
    });

    it('should check if user has any of specified permissions', () => {
      expect(service.hasAnyPermission(['query.execute', 'admin.users'])).toBe(true);
      expect(service.hasAnyPermission(['admin.users', 'admin.settings'])).toBe(false);
    });

    it('should check if user has all specified permissions', () => {
      expect(service.hasAllPermissions(['query.execute', 'data.view'])).toBe(true);
      expect(service.hasAllPermissions(['query.execute', 'admin.users'])).toBe(false);
    });
  });

  describe('User Information', () => {
    it('should return null when not authenticated', () => {
      expect(service.userId).toBeNull();
      expect(service.userDisplayName).toBeNull();
      expect(service.isUserAuthenticated).toBe(false);
    });
  });
});
