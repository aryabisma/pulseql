/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.service.DBWServiceBindingServlet;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Deep Link Servlet
 * 
 * REST endpoint for generating deep links to PulseQL
 * Endpoint: /api/pulseql/generate-link
 */
public class DeepLinkServlet extends DBWServiceBindingServlet {
    
    private static final Log log = Log.getLog(DeepLinkServlet.class);
    
    private DeepLinkService deepLinkService;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            deepLinkService = new DeepLinkService();
            log.info("Deep Link service initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize Deep Link service", e);
            throw new ServletException("Failed to initialize Deep Link service", e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        try {
            // Get session
            WebSession session = getWebSession(request);
            if (session == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No session found");
                return;
            }
            
            // Verify user is authenticated
            if (session.getUser() == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
                return;
            }
            
            // Parse request body
            DeepLinkRequest deepLinkRequest = parseRequest(request);
            if (deepLinkRequest == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
                return;
            }
            
            // Generate deep link
            DeepLinkResponse deepLinkResponse = deepLinkService.generateDeepLink(session, deepLinkRequest);
            
            // Send response
            sendJsonResponse(response, deepLinkResponse);
            
            log.info("Deep link generated for user: " + session.getUser().getUserId() + 
                ", type: " + deepLinkRequest.getTargetType());
            
        } catch (Exception e) {
            log.error("Error generating deep link", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Failed to generate deep link: " + e.getMessage());
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Support CORS preflight requests
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    /**
     * Parse JSON request body into DeepLinkRequest
     */
    private DeepLinkRequest parseRequest(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        
        String body = sb.toString().trim();
        
        // Validate basic JSON structure
        if (!body.startsWith("{") || !body.endsWith("}")) {
            log.warn("Invalid JSON structure in request body");
            return null;
        }
        
        try {
            DeepLinkRequest req = new DeepLinkRequest();
            
            // Parse JSON manually (simple approach for security)
            req.setTargetType(extractJsonString(body, "target_type"));
            req.setConnectionId(extractJsonString(body, "connection_id"));
            req.setSchemaName(extractJsonString(body, "schema_name"));
            req.setTableName(extractJsonString(body, "table_name"));
            req.setQuery(extractJsonString(body, "query"));
            req.setWorkspaceId(extractJsonString(body, "workspace_id"));
            
            return req;
            
        } catch (Exception e) {
            log.warn("Failed to parse request body", e);
            return null;
        }
    }
    
    /**
     * Extract string value from JSON
     */
    private String extractJsonString(String json, String fieldName) {
        try {
            // Find field name in quotes
            String searchPattern = "\"" + fieldName + "\"";
            int fieldIndex = json.indexOf(searchPattern);
            if (fieldIndex == -1) {
                return null;
            }
            
            // Find colon after field name
            int colonIndex = json.indexOf(":", fieldIndex);
            if (colonIndex == -1) {
                return null;
            }
            
            // Skip whitespace and find opening quote
            int startQuote = json.indexOf("\"", colonIndex);
            if (startQuote == -1) {
                return null;
            }
            
            // Find closing quote, handling escaped quotes
            int endQuote = startQuote + 1;
            while (endQuote < json.length()) {
                char c = json.charAt(endQuote);
                if (c == '"' && json.charAt(endQuote - 1) != '\\') {
                    break;
                }
                endQuote++;
            }
            
            if (endQuote >= json.length()) {
                return null;
            }
            
            // Extract value
            String value = json.substring(startQuote + 1, endQuote);
            
            // Unescape common escape sequences
            value = value.replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t");
            
            return value;
            
        } catch (Exception e) {
            log.debug("Failed to extract field: " + fieldName, e);
            return null;
        }
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpServletResponse response, DeepLinkResponse deepLinkResponse) 
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        if (deepLinkResponse.isSuccess()) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        
        PrintWriter writer = response.getWriter();
        writer.write("{");
        writer.write("\"success\":" + deepLinkResponse.isSuccess());
        
        if (deepLinkResponse.isSuccess()) {
            writer.write(",\"deep_link\":\"" + escapeJson(deepLinkResponse.getDeepLink()) + "\"");
            if (!CommonUtils.isEmpty(deepLinkResponse.getShortUrl())) {
                writer.write(",\"short_url\":\"" + escapeJson(deepLinkResponse.getShortUrl()) + "\"");
            }
        } else {
            writer.write(",\"error\":\"" + escapeJson(deepLinkResponse.getError()) + "\"");
        }
        
        writer.write("}");
        writer.flush();
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) 
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\f", "\\f");
    }
}
