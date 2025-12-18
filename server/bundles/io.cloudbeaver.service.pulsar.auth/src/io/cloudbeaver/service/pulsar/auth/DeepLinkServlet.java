/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.model.user.WebUser;
import io.cloudbeaver.server.graphql.GraphQLLoggerUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Deep Link Servlet
 * 
 * REST endpoint for generating deep links to PulseQL
 * Endpoint: /api/pulseql/generate-link
 */
public class DeepLinkServlet extends HttpServlet {
    
    private static final Log log = Log.getLog(DeepLinkServlet.class);
    
    private DeepLinkService deepLinkService;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            deepLinkService = new DeepLinkService();
            gson = new Gson();
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
            WebSession session = GraphQLLoggerUtil.getWebSession(request);
            if (session == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No session found");
                return;
            }
            
            // Verify user is authenticated
            WebUser user = session.getUser();
            if (user == null) {
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
            
            // Log success with null-safe user ID retrieval
            String userId = user.getUserId();
            if (userId != null) {
                log.info("Deep link generated for user: " + userId + ", type: " + deepLinkRequest.getTargetType());
            }
            
        } catch (Exception e) {
            log.error("Error generating deep link", e);
            // Use generic error message for client, detailed error is logged
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "An error occurred while generating the deep link");
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // CORS is handled by the application server configuration
        // We don't set headers here for security reasons
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    /**
     * Parse JSON request body into DeepLinkRequest using Gson
     */
    private DeepLinkRequest parseRequest(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, DeepLinkRequest.class);
        } catch (JsonSyntaxException e) {
            log.warn("Invalid JSON in request body", e);
            return null;
        }
    }
    
    /**
     * Send JSON response using Gson
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
        
        try (PrintWriter writer = response.getWriter()) {
            gson.toJson(deepLinkResponse, writer);
        }
    }
    
    /**
     * Send error response using Gson
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) 
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        
        DeepLinkResponse errorResponse = DeepLinkResponse.error(message);
        
        try (PrintWriter writer = response.getWriter()) {
            gson.toJson(errorResponse, writer);
        }
    }
}
