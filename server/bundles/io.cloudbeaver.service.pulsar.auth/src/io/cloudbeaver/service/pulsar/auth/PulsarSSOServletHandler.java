/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.server.CBApplication;
import io.cloudbeaver.server.CBPlatform;
import io.cloudbeaver.server.actions.AbstractActionServletHandler;
import io.cloudbeaver.utils.ServletAppUtils;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Pulsar SSO Servlet Handler
 * 
 * Handles routing for Pulsar SSO and Activity Tracking endpoints
 * 
 * NOTE: For production use, consider implementing singleton pattern or dependency injection
 * to ensure all components share the same SessionValidationService instance for consistency.
 */
public class PulsarSSOServletHandler extends AbstractActionServletHandler {
    
    private static final Log log = Log.getLog(PulsarSSOServletHandler.class);
    
    private static final String SSO_PREFIX = "api/sso";
    
    private PulsarSSOServlet ssoServlet;
    private ActivityTrackingServlet activityServlet;
    private IdleSessionManager idleSessionManager;
    
    public PulsarSSOServletHandler() {
        try {
            ssoServlet = new PulsarSSOServlet();
            ssoServlet.init();
            
            activityServlet = new ActivityTrackingServlet();
            activityServlet.init();
            
            // Initialize and start idle session manager
            SessionValidationService validationService = new SessionValidationService(CBApplication.getInstance());
            idleSessionManager = new IdleSessionManager(validationService);
            idleSessionManager.start();
            
            log.info("PulsarSSOServletHandler initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize PulsarSSOServletHandler", e);
        }
    }
    
    @Override
    public boolean handleRequest(
        @NotNull Servlet servlet,
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response
    ) throws DBException, IOException {
        
        String servletPath = ServletAppUtils.removeSideSlashes(request.getServletPath());
        
        // Check if this is an SSO request
        if (servletPath.startsWith(SSO_PREFIX)) {
            String pathInfo = servletPath.substring(SSO_PREFIX.length());
            
            // Route to appropriate servlet
            if (pathInfo.startsWith("/validate") || pathInfo.startsWith("/logout")) {
                // Handle SSO validation/logout
                try {
                    ssoServlet.service(request, response);
                    return true;
                } catch (Exception e) {
                    log.error("Error handling SSO request", e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SSO request failed");
                    return true;
                }
            } else if (pathInfo.startsWith("/activity")) {
                // Handle activity tracking
                try {
                    activityServlet.service(request, response);
                    return true;
                } catch (Exception e) {
                    log.error("Error handling activity request", e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Activity tracking failed");
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Cleanup resources on shutdown
     */
    public void destroy() {
        if (idleSessionManager != null) {
            idleSessionManager.stop();
        }
        
        if (ssoServlet != null) {
            ssoServlet.destroy();
        }
        
        if (activityServlet != null) {
            activityServlet.destroy();
        }
        
        log.info("PulsarSSOServletHandler destroyed");
    }
    
    @Override
    protected String getActionConsole() {
        return null; // Not used for SSO/activity tracking
    }
}
