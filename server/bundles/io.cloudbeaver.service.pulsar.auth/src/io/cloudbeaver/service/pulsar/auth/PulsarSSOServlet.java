/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.model.user.WebUser;
import io.cloudbeaver.server.CBApplication;
import io.cloudbeaver.service.DBWServiceBindingServlet;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Pulsar SSO REST Endpoint
 * 
 * Provides REST API for SSO token validation
 * Endpoint: /api/sso/validate
 */
public class PulsarSSOServlet extends DBWServiceBindingServlet {
    
    private static final Log log = Log.getLog(PulsarSSOServlet.class);
    
    private PulsarSSOTokenValidator tokenValidator;
    private PulsarSSOAuthHandler authHandler;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            // Get signing secret from configuration
            String signingSecret = CBApplication.getInstance().getAppConfiguration()
                .getConfigurationValue("pulsar.sso.signingSecret");
            
            if (CommonUtils.isEmpty(signingSecret)) {
                log.warn("Pulsar SSO signing secret not configured");
                signingSecret = "default-secret-change-in-production";
            }
            
            tokenValidator = new PulsarSSOTokenValidator(signingSecret);
            authHandler = new PulsarSSOAuthHandler(tokenValidator);
            
            log.info("Pulsar SSO service initialized");
            
        } catch (Exception e) {
            log.error("Failed to initialize Pulsar SSO service", e);
            throw new ServletException("Failed to initialize Pulsar SSO service", e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        if ("/validate".equals(pathInfo)) {
            handleTokenValidation(request, response);
        } else if ("/logout".equals(pathInfo)) {
            handleLogout(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint");
        }
    }
    
    /**
     * Handle token validation request
     * POST /api/sso/validate
     * Body: { "token": "<JWT>" }
     */
    private void handleTokenValidation(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Get session
            WebSession session = getWebSession(request);
            if (session == null) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "No session found");
                return;
            }
            
            // Parse request body
            String token = extractTokenFromRequest(request);
            if (CommonUtils.isEmpty(token)) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Token missing");
                return;
            }
            
            // Authenticate with SSO
            WebUser user = authHandler.authenticateWithSSO(session, token);
            
            // Send success response
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            
            PrintWriter writer = response.getWriter();
            writer.write("{");
            writer.write("\"success\":true,");
            writer.write("\"userId\":\"" + escapeJson(user.getUserId()) + "\",");
            writer.write("\"displayName\":\"" + escapeJson(user.getDisplayName()) + "\"");
            writer.write("}");
            writer.flush();
            
            log.info("SSO validation successful for user: " + user.getUserId());
            
        } catch (DBWebException e) {
            log.error("SSO validation failed", e);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            log.error("SSO validation error", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Validation failed");
        }
    }
    
    /**
     * Handle logout request
     * POST /api/sso/logout
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            WebSession session = getWebSession(request);
            if (session != null) {
                authHandler.logout(session);
                session.close();
            }
            
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true}");
            
        } catch (Exception e) {
            log.error("Logout error", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Logout failed");
        }
    }
    
    /**
     * Extract token from request body
     */
    private String extractTokenFromRequest(HttpServletRequest request) throws IOException {
        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            
            String body = sb.toString();
            
            // Simple JSON parsing (for token field)
            if (body.contains("\"token\"")) {
                int start = body.indexOf("\"token\"") + 8;
                start = body.indexOf("\"", start) + 1;
                int end = body.indexOf("\"", start);
                if (start > 0 && end > start) {
                    return body.substring(start, end);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Failed to extract token from request", e);
            return null;
        }
    }
    
    /**
     * Send error response
     */
    private void sendError(HttpServletResponse response, int status, String message) 
            throws IOException {
        
        response.setContentType("application/json");
        response.setStatus(status);
        
        PrintWriter writer = response.getWriter();
        writer.write("{");
        writer.write("\"success\":false,");
        writer.write("\"error\":\"" + escapeJson(message) + "\"");
        writer.write("}");
        writer.flush();
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
