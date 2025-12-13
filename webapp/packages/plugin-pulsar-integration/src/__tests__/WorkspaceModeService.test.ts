/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { WorkspaceModeService } from '../WorkspaceModeService';

describe('WorkspaceModeService', () => {
  let service: WorkspaceModeService;

  beforeEach(() => {
    service = new WorkspaceModeService();
    // Clear URL parameters
    window.history.replaceState({}, '', window.location.pathname);
  });

  describe('Mode Detection', () => {
    it('should default to standalone mode', () => {
      service.initializeFromURL();
      expect(service.mode).toBe('standalone');
      expect(service.isPulsarMode).toBe(false);
      expect(service.isEmbeddedMode).toBe(false);
    });

    it('should detect pulsar mode from URL', () => {
      window.history.replaceState({}, '', '?mode=pulsar');
      service.initializeFromURL();
      expect(service.mode).toBe('pulsar');
      expect(service.isPulsarMode).toBe(true);
      expect(service.isEmbeddedMode).toBe(true);
    });

    it('should detect embedded mode from URL', () => {
      window.history.replaceState({}, '', '?mode=embedded');
      service.initializeFromURL();
      expect(service.mode).toBe('embedded');
      expect(service.isPulsarMode).toBe(false);
      expect(service.isEmbeddedMode).toBe(true);
    });

    it('should reject invalid mode and default to standalone', () => {
      window.history.replaceState({}, '', '?mode=invalid');
      service.initializeFromURL();
      expect(service.mode).toBe('standalone');
    });
  });

  describe('URL Parameter Sanitization', () => {
    it('should sanitize workspace ID', () => {
      window.history.replaceState({}, '', '?workspace_id=valid-id_123');
      service.initializeFromURL();
      expect(service.getWorkspaceId()).toBe('valid-id_123');
    });

    it('should remove invalid characters from workspace ID', () => {
      window.history.replaceState({}, '', '?workspace_id=<script>alert(1)</script>');
      service.initializeFromURL();
      expect(service.getWorkspaceId()).not.toContain('<');
      expect(service.getWorkspaceId()).not.toContain('>');
    });

    it('should sanitize theme name', () => {
      window.history.replaceState({}, '', '?theme=pulsar-light');
      service.initializeFromURL();
      expect(service.getThemeName()).toBe('pulsar-light');
    });

    it('should remove invalid characters from theme name', () => {
      window.history.replaceState({}, '', '?theme=<script>');
      service.initializeFromURL();
      expect(service.getThemeName()).not.toContain('<');
    });
  });

  describe('Navigation Hiding', () => {
    it('should hide navigation items from comma-separated list', () => {
      window.history.replaceState({}, '', '?hide_nav=admin,settings,users');
      service.initializeFromURL();
      expect(service.shouldHideNavItem('admin')).toBe(true);
      expect(service.shouldHideNavItem('settings')).toBe(true);
      expect(service.shouldHideNavItem('users')).toBe(true);
      expect(service.shouldHideNavItem('other')).toBe(false);
    });

    it('should validate navigation items against whitelist', () => {
      window.history.replaceState({}, '', '?hide_nav=admin,invalid_item');
      service.initializeFromURL();
      expect(service.shouldHideNavItem('admin')).toBe(true);
      expect(service.shouldHideNavItem('invalid_item')).toBe(false);
    });
  });

  describe('Custom Branding', () => {
    it('should parse custom branding from URL', () => {
      window.history.replaceState({}, '', '?brand_title=My%20App&brand_color=%23FF0000');
      service.initializeFromURL();
      const branding = service.getCustomBranding();
      expect(branding?.title).toContain('My');
      expect(branding?.color).toBe('#FF0000');
    });

    it('should validate hex color format', () => {
      window.history.replaceState({}, '', '?brand_color=red');
      service.initializeFromURL();
      const branding = service.getCustomBranding();
      expect(branding?.color).toBeUndefined();
    });

    it('should validate URL protocol for logo', () => {
      window.history.replaceState({}, '', '?brand_logo=https://example.com/logo.png');
      service.initializeFromURL();
      const branding = service.getCustomBranding();
      expect(branding?.logo).toContain('https');
    });

    it('should reject javascript: protocol in logo URL', () => {
      window.history.replaceState({}, '', '?brand_logo=javascript:alert(1)');
      service.initializeFromURL();
      const branding = service.getCustomBranding();
      expect(branding?.logo).toBeUndefined();
    });
  });

  describe('Readonly Connections', () => {
    it('should respect readonly_connections parameter', () => {
      window.history.replaceState({}, '', '?readonly_connections=true');
      service.initializeFromURL();
      expect(service.readonlyConnections).toBe(true);
    });

    it('should make connections readonly in Pulsar mode', () => {
      window.history.replaceState({}, '', '?mode=pulsar');
      service.initializeFromURL();
      expect(service.readonlyConnections).toBe(true);
    });

    it('should not make connections readonly in standalone mode by default', () => {
      window.history.replaceState({}, '', '?mode=standalone');
      service.initializeFromURL();
      expect(service.readonlyConnections).toBe(false);
    });
  });

  describe('Header and Footer Visibility', () => {
    it('should hide header when parameter is true', () => {
      window.history.replaceState({}, '', '?hide_header=true');
      service.initializeFromURL();
      expect(service.hideHeader).toBe(true);
    });

    it('should hide footer when parameter is true', () => {
      window.history.replaceState({}, '', '?hide_footer=true');
      service.initializeFromURL();
      expect(service.hideFooter).toBe(true);
    });

    it('should show header and footer by default', () => {
      service.initializeFromURL();
      expect(service.hideHeader).toBe(false);
      expect(service.hideFooter).toBe(false);
    });
  });
});
