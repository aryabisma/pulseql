/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

/**
 * Pulsar Integration Service Implementation
 * 
 * Implements service for Pulsar SSO authentication and deep linking
 */
public class WebServicePulsar implements DBWServicePulsar {
    
    public WebServicePulsar() {
    }
    
    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }
    
    @Override
    public String getServiceName() {
        return "Pulsar Integration";
    }
}
