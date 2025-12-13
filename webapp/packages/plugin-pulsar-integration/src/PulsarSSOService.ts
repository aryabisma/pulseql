/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { injectable } from '@cloudbeaver/core-di';
import { observable, action, makeObservable, computed } from 'mobx';

export interface JWTPayload {
  iss: string; // Issuer
  sub: string; // Subject (user ID)
  aud: string; // Audience
  exp: number; // Expiration time
  iat: number; // Issued at
  nbf: number; // Not before
  jti: string; // JWT ID
  user: {
    userId: string;
    displayName: string;
    email: string;
    authRole: string;
  };
  permissions: string[];
  teams?: Array<{
    teamId: string;
    teamName: string;
    role: string;
  }>;
  nonce?: string;
}

export interface SSOTokenValidationResult {
  valid: boolean;
  payload?: JWTPayload;
  error?: string;
  errorCode?: 'EXPIRED' | 'INVALID_SIGNATURE' | 'MALFORMED' | 'NOT_YET_VALID' | 'INVALID_AUDIENCE';
}

/**
 * Service for handling SSO token validation and user session creation
 * Implements secure JWT token validation following OWASP guidelines
 */
@injectable()
export class PulsarSSOService {
  @observable
  private currentToken: string | null = null;

  @observable
  private tokenPayload: JWTPayload | null = null;

  @observable
  private isAuthenticated = false;

  @observable
  private authenticationError: string | null = null;

  private readonly EXPECTED_ISSUER = 'pulsar';
  private readonly EXPECTED_AUDIENCE = 'pulseql';
  private readonly MAX_TOKEN_AGE_MS = 15 * 60 * 1000; // 15 minutes

  constructor() {
    makeObservable(this);
  }

  @computed
  get isUserAuthenticated(): boolean {
    return this.isAuthenticated;
  }

  @computed
  get userId(): string | null {
    return this.tokenPayload?.user?.userId || null;
  }

  @computed
  get userDisplayName(): string | null {
    return this.tokenPayload?.user?.displayName || null;
  }

  @computed
  get userPermissions(): string[] {
    return this.tokenPayload?.permissions || [];
  }

  @computed
  get authError(): string | null {
    return this.authenticationError;
  }

  /**
   * Initialize SSO from URL token parameter
   * Should be called during application bootstrap
   */
  @action
  async initializeFromURL(): Promise<boolean> {
    try {
      const params = new URLSearchParams(window.location.search);
      const token = params.get('sso_token');

      if (!token) {
        console.info('[PulsarSSOService] No SSO token found in URL');
        return false;
      }

      // Clear token from URL for security
      this.clearTokenFromURL();

      // Validate token
      const result = await this.validateToken(token);

      if (result.valid && result.payload) {
        await this.handleSuccessfulAuthentication(token, result.payload);
        return true;
      } else {
        this.handleAuthenticationFailure(result.error || 'Unknown error', result.errorCode);
        return false;
      }
    } catch (error) {
      console.error('[PulsarSSOService] SSO initialization failed:', error);
      this.handleAuthenticationFailure('SSO initialization failed', 'MALFORMED');
      return false;
    }
  }

  /**
   * Validate JWT token structure and claims (client-side validation)
   * Note: Server-side validation with signature check is still required
   */
  @action
  async validateToken(token: string): Promise<SSOTokenValidationResult> {
    try {
      // Basic format validation
      const parts = token.split('.');
      if (parts.length !== 3) {
        return {
          valid: false,
          error: 'Invalid token format',
          errorCode: 'MALFORMED',
        };
      }

      // Decode payload (client-side only - server must verify signature)
      const payloadBase64 = parts[1];
      const payloadJson = atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/'));
      const payload: JWTPayload = JSON.parse(payloadJson);

      // Validate required fields
      if (!payload.iss || !payload.sub || !payload.exp || !payload.aud) {
        return {
          valid: false,
          error: 'Missing required token claims',
          errorCode: 'MALFORMED',
        };
      }

      // Validate issuer
      if (payload.iss !== this.EXPECTED_ISSUER) {
        return {
          valid: false,
          error: `Invalid issuer: expected ${this.EXPECTED_ISSUER}, got ${payload.iss}`,
          errorCode: 'INVALID_SIGNATURE',
        };
      }

      // Validate audience
      if (payload.aud !== this.EXPECTED_AUDIENCE) {
        return {
          valid: false,
          error: `Invalid audience: expected ${this.EXPECTED_AUDIENCE}, got ${payload.aud}`,
          errorCode: 'INVALID_AUDIENCE',
        };
      }

      // Check expiration
      const now = Math.floor(Date.now() / 1000);
      if (payload.exp < now) {
        return {
          valid: false,
          error: 'Token has expired',
          errorCode: 'EXPIRED',
        };
      }

      // Check not-before time
      if (payload.nbf && payload.nbf > now) {
        return {
          valid: false,
          error: 'Token not yet valid',
          errorCode: 'NOT_YET_VALID',
        };
      }

      // Check token age (prevent old tokens)
      if (payload.iat && (now - payload.iat) > this.MAX_TOKEN_AGE_MS / 1000) {
        return {
          valid: false,
          error: 'Token too old',
          errorCode: 'EXPIRED',
        };
      }

      // Client-side validation passed - server must still verify signature
      console.info('[PulsarSSOService] Client-side token validation passed');

      return {
        valid: true,
        payload,
      };
    } catch (error) {
      console.error('[PulsarSSOService] Token validation error:', error);
      return {
        valid: false,
        error: 'Token validation failed',
        errorCode: 'MALFORMED',
      };
    }
  }

  /**
   * Handle successful authentication
   */
  @action
  private async handleSuccessfulAuthentication(token: string, payload: JWTPayload): Promise<void> {
    this.currentToken = token;
    this.tokenPayload = payload;
    this.isAuthenticated = true;
    this.authenticationError = null;

    // Store token securely (encrypted in production)
    this.storeTokenSecurely(token);

    // Log successful authentication (audit trail)
    console.info('[PulsarSSOService] User authenticated via SSO:', {
      userId: payload.user.userId,
      displayName: payload.user.displayName,
      role: payload.user.authRole,
      jti: payload.jti,
    });

    // Set up token refresh before expiration
    this.scheduleTokenRefresh(payload.exp);
  }

  /**
   * Handle authentication failure
   */
  @action
  private handleAuthenticationFailure(error: string, errorCode?: string): void {
    this.currentToken = null;
    this.tokenPayload = null;
    this.isAuthenticated = false;
    this.authenticationError = error;

    console.error('[PulsarSSOService] Authentication failed:', error, errorCode);

    // Clear any stored tokens
    sessionStorage.removeItem('pulsar_sso_token');
  }

  /**
   * Store token securely
   * In production, should use encryption or secure cookie
   */
  private storeTokenSecurely(token: string): void {
    // WARNING: sessionStorage is not encrypted
    // In production, consider:
    // 1. Storing only on server-side session
    // 2. Using HttpOnly secure cookies
    // 3. Encrypting before storage
    try {
      sessionStorage.setItem('pulsar_sso_token', token);
    } catch (error) {
      console.warn('[PulsarSSOService] Failed to store token:', error);
    }
  }

  /**
   * Clear token from URL for security
   */
  private clearTokenFromURL(): void {
    try {
      const url = new URL(window.location.href);
      url.searchParams.delete('sso_token');
      window.history.replaceState({}, '', url.toString());
    } catch (error) {
      console.warn('[PulsarSSOService] Failed to clear token from URL:', error);
    }
  }

  /**
   * Schedule token refresh before expiration
   */
  private scheduleTokenRefresh(expirationTime: number): void {
    const now = Math.floor(Date.now() / 1000);
    const timeUntilExpiry = expirationTime - now;
    
    // Refresh 1 minute before expiration
    const refreshTime = Math.max(0, (timeUntilExpiry - 60) * 1000);

    setTimeout(() => {
      this.refreshToken();
    }, refreshTime);
  }

  /**
   * Refresh token by redirecting back to Pulsar
   */
  @action
  private async refreshToken(): Promise<void> {
    console.info('[PulsarSSOService] Token expiring, initiating refresh');
    
    const returnUrl = sessionStorage.getItem('pulsar_return_url');
    if (returnUrl) {
      // Redirect to Pulsar to get new token
      window.location.href = returnUrl;
    } else {
      // Logout if no return URL
      this.logout();
    }
  }

  /**
   * Logout and clear session
   */
  @action
  logout(): void {
    this.currentToken = null;
    this.tokenPayload = null;
    this.isAuthenticated = false;
    this.authenticationError = null;

    // Clear stored tokens
    sessionStorage.removeItem('pulsar_sso_token');

    console.info('[PulsarSSOService] User logged out');
  }

  /**
   * Get current token (for API calls)
   */
  getToken(): string | null {
    return this.currentToken;
  }

  /**
   * Check if user has specific permission
   */
  hasPermission(permission: string): boolean {
    return this.userPermissions.includes(permission);
  }

  /**
   * Check if user has any of the specified permissions
   */
  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(p => this.hasPermission(p));
  }

  /**
   * Check if user has all of the specified permissions
   */
  hasAllPermissions(permissions: string[]): boolean {
    return permissions.every(p => this.hasPermission(p));
  }
}
