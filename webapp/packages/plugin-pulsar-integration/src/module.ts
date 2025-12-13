/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { Bootstrap, ModuleRegistry } from '@cloudbeaver/core-di';
import { PulsarIntegrationBootstrap } from './PulsarIntegrationBootstrap.js';
import { WorkspaceModeService } from './WorkspaceModeService.js';
import { PulsarSSOService } from './PulsarSSOService.js';
import { PulsarPermissionService } from './PulsarPermissionService.js';
import { PulsarUICustomizer } from './PulsarUICustomizer.js';

/**
 * Pulsar Integration Plugin Module
 * 
 * Provides workspace mode functionality for seamless Pulsar integration:
 * - URL parameter-based mode switching
 * - SSO authentication with JWT tokens
 * - Permission-based access control (RBAC)
 * - UI customization and component visibility control
 * - Custom branding and theming
 * - Read-only connection mode
 * - Embedded workspace support
 */
export default ModuleRegistry.add({
  name: '@cloudbeaver/plugin-pulsar-integration',

  configure: serviceCollection => {
    // Register core services
    serviceCollection.addSingleton(WorkspaceModeService);
    serviceCollection.addSingleton(PulsarSSOService);
    serviceCollection.addSingleton(PulsarPermissionService);
    serviceCollection.addSingleton(PulsarUICustomizer);
    
    // Register bootstrap to initialize on app start
    serviceCollection.addSingleton(Bootstrap, PulsarIntegrationBootstrap);
  },
});
