/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.model.app.ServletApplication;
import io.cloudbeaver.server.CBApplication;
import io.cloudbeaver.service.DBWBindingContext;
import io.cloudbeaver.service.DBWServiceBindingServlet;
import io.cloudbeaver.service.DBWServletContext;
import io.cloudbeaver.service.WebServiceBindingBase;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

/**
 * Pulsar Integration Service Binding
 * 
 * Registers servlets for Pulsar SSO and Deep Linking
 */
public class WebServiceBindingPulsar extends WebServiceBindingBase<DBWServicePulsar> 
    implements DBWServiceBindingServlet<ServletApplication> {
    
    private static final Log log = Log.getLog(WebServiceBindingPulsar.class);
    
    public WebServiceBindingPulsar() {
        super(DBWServicePulsar.class, new WebServicePulsar(), null);
    }
    
    @Override
    public void bindWiring(DBWBindingContext model) throws DBWebException {
        // No GraphQL bindings for this service
        // All functionality exposed via REST endpoints
    }
    
    @Override
    public void addServlets(ServletApplication application, DBWServletContext servletContext) throws DBException {
        try {
            // Register SSO validation servlet
            servletContext.addServlet(
                "pulsarSSO",
                new PulsarSSOServlet(),
                application.getServicesURI() + "sso/*"
            );
            
            log.info("Registered Pulsar SSO servlet at: " + application.getServicesURI() + "sso/*");
            
            // Register Deep Link generation servlet
            servletContext.addServlet(
                "pulsarDeepLink",
                new DeepLinkServlet(),
                application.getServicesURI() + "pulseql/generate-link"
            );
            
            log.info("Registered Deep Link servlet at: " + application.getServicesURI() + "pulseql/generate-link");
            
        } catch (Exception e) {
            log.error("Failed to register Pulsar servlets", e);
            throw new DBException("Failed to register Pulsar servlets", e);
        }
    }
    
    @Override
    public boolean isApplicable(ServletApplication application) {
        // Always applicable
        return true;
    }
}
