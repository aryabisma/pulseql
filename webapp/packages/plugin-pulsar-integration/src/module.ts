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

/**
 * Pulsar Integration Plugin Module
 * 
 * Provides workspace mode functionality for seamless Pulsar integration:
 * - URL parameter-based mode switching
 * - Custom branding and theming
 * - Read-only connection mode
 * - Embedded workspace support
 */
export default ModuleRegistry.add({
  name: '@cloudbeaver/plugin-pulsar-integration',

  configure: serviceCollection => {
    // Register services
    serviceCollection.addSingleton(WorkspaceModeService);
    
    // Register bootstrap to initialize on app start
    serviceCollection.addSingleton(Bootstrap, PulsarIntegrationBootstrap);
  },
});
