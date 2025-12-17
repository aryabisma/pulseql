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
import io.cloudbeaver.server.graphql.GraphQLLoggerUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Pulsar SSO REST Endpoint
 * 
 * Provides REST API for SSO token validation
 * Endpoint: /api/sso/validate
 */
public class PulsarSSOServlet extends HttpServlet {
    
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
            
            if (CommonUtils.isEmpty(signingSecret) || "default-secret-CHANGE-IN-PRODUCTION".equals(signingSecret)) {
                String error = "CRITICAL: Pulsar SSO signing secret not configured! " +
                    "Set PULSAR_SSO_SECRET environment variable or pulsar.sso.signingSecret in configuration.";
                log.error(error);
                throw new ServletException(error);
            }
            
            tokenValidator = new PulsarSSOTokenValidator(signingSecret);
            authHandler = new PulsarSSOAuthHandler(tokenValidator);
            
            log.info("Pulsar SSO service initialized successfully");
            
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
            WebSession session = GraphQLLoggerUtil.getWebSession(request);
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
            WebSession session = GraphQLLoggerUtil.getWebSession(request);
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
     * Extract token from JSON request body using simple but safe parsing
     * For production, consider using Jackson or Gson
     */
    private String extractTokenFromRequest(HttpServletRequest request) throws IOException {
        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            
            String body = sb.toString().trim();
            
            // Validate basic JSON structure
            if (!body.startsWith("{") || !body.endsWith("}")) {
                return null;
            }
            
            // Simple JSON parsing for "token" field with proper escaping
            // This approach avoids most injection risks by only extracting quoted strings
            int tokenIndex = body.indexOf("\"token\"");
            if (tokenIndex == -1) {
                return null;
            }
            
            // Find the value after "token":
            int colonIndex = body.indexOf(":", tokenIndex);
            if (colonIndex == -1) {
                return null;
            }
            
            // Skip whitespace and find opening quote
            int startQuote = body.indexOf("\"", colonIndex);
            if (startQuote == -1) {
                return null;
            }
            
            // Find closing quote, handling escaped quotes
            int endQuote = startQuote + 1;
            while (endQuote < body.length()) {
                char c = body.charAt(endQuote);
                if (c == '"' && body.charAt(endQuote - 1) != '\\') {
                    break;
                }
                endQuote++;
            }
            
            if (endQuote >= body.length()) {
                return null;
            }
            
            // Extract token (between quotes)
            String token = body.substring(startQuote + 1, endQuote);
            
            // Validate token format (JWT has 3 parts separated by dots)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid token format - not a JWT");
                return null;
            }
            
            return token;
            
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
