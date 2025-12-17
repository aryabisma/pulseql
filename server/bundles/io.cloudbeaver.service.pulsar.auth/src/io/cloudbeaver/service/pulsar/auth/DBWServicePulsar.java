/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.service.DBWService;

/**
 * Pulsar Integration Service Interface
 * 
 * Service for Pulsar SSO authentication and deep linking
 */
public interface DBWServicePulsar extends DBWService {
    
    String SERVICE_ID = "pulsar";
    
    /**
     * Get service ID
     */
    @Override
    default String getServiceId() {
        return SERVICE_ID;
    }
    
    /**
     * Get service name
     */
    @Override
    default String getServiceName() {
        return "Pulsar Integration";
    }
}
