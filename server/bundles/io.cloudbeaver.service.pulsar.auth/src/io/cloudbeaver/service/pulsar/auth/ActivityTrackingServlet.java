/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.server.CBApplication;
import io.cloudbeaver.service.DBWServiceBindingServlet;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity Tracking REST Endpoint
 * 
 * Handles POST /api/sso/activity for activity tracking from PulseQL
 */
public class ActivityTrackingServlet extends DBWServiceBindingServlet {
    
    private static final Log log = Log.getLog(ActivityTrackingServlet.class);
    
    // Rate limiting (simple implementation - per IP)
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,128}$");
    
    private SessionValidationService validationService;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        try {
            validationService = new SessionValidationService(CBApplication.getInstance());
            log.info("Activity Tracking service initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize Activity Tracking service", e);
            throw new ServletException("Failed to initialize Activity Tracking service", e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Handle activity tracking endpoint
        handleActivityTracking(request, response);
    }
    
    /**
     * Handle activity tracking request
     * POST /api/sso/activity
     */
    private void handleActivityTracking(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse request body
            ActivityRequest activityRequest = parseActivityRequest(request);
            
            // Validate session ID format
            if (!isValidSessionId(activityRequest.sessionId)) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid session ID format");
                return;
            }
            
            // Create activity event
            ActivityEvent event = new ActivityEvent(
                null, // Event ID will be auto-generated
                activityRequest.sessionId,
                activityRequest.userId,
                ActivityEvent.parseEventType(activityRequest.eventType),
                activityRequest.timestamp,
                activityRequest.details,
                getClientIpAddress(request),
                request.getHeader("User-Agent")
            );
            
            // Validate and process activity
            SessionValidationResult result = validationService.validateActivity(
                activityRequest.sessionId,
                event
            );
            
            // Send response
            sendActivityResponse(response, result);
            
            // Audit log
            if (log.isDebugEnabled()) {
                log.debug("Activity tracked: " + event.getType() + 
                    " for session " + event.getSessionId() +
                    " (warnings: " + result.getWarnings().size() + ")");
            }
            
        } catch (DBWebException e) {
            log.error("Activity tracking failed", e);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Activity tracking error", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Activity tracking failed");
        }
    }
    
    /**
     * Parse activity request from JSON body
     */
    @NotNull
    private ActivityRequest parseActivityRequest(@NotNull HttpServletRequest request) 
            throws IOException, DBWebException {
        
        // Read request body
        StringBuilder sb = new StringBuilder();
        String line;
        
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        
        String body = sb.toString().trim();
        
        // Validate JSON structure
        if (!body.startsWith("{") || !body.endsWith("}")) {
            throw new DBWebException("Invalid JSON request");
        }
        
        // Parse fields (simple JSON parsing for security)
        ActivityRequest req = new ActivityRequest();
        
        req.sessionId = extractJsonString(body, "session_id");
        if (CommonUtils.isEmpty(req.sessionId)) {
            throw new DBWebException("Missing session_id");
        }
        
        // Parse event object
        String eventJson = extractJsonObject(body, "event");
        if (CommonUtils.isEmpty(eventJson)) {
            throw new DBWebException("Missing event object");
        }
        
        req.eventType = extractJsonString(eventJson, "type");
        if (CommonUtils.isEmpty(req.eventType)) {
            throw new DBWebException("Missing event type");
        }
        
        String timestampStr = extractJsonNumber(eventJson, "timestamp");
        if (CommonUtils.isEmpty(timestampStr)) {
            req.timestamp = System.currentTimeMillis();
        } else {
            try {
                req.timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                throw new DBWebException("Invalid event timestamp: " + timestampStr, e);
            }
        }
        
        // Optional fields
        req.userId = extractJsonString(body, "user_id");
        
        // Extract details if present
        String detailsJson = extractJsonObject(eventJson, "details");
        req.details = detailsJson;
        
        return req;
    }
    
    /**
     * Extract string value from JSON
     */
    @NotNull
    private String extractJsonString(@NotNull String json, @NotNull String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        
        return "";
    }
    
    /**
     * Extract number value from JSON
     */
    @NotNull
    private String extractJsonNumber(@NotNull String json, @NotNull String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "";
    }
    
    /**
     * Extract object value from JSON
     * 
     * NOTE: This is a simple regex-based parser suitable for basic JSON structures.
     * For production use with complex nested objects, consider using Jackson or Gson.
     */
    @NotNull
    private String extractJsonObject(@NotNull String json, @NotNull String key) {
        // Simple pattern that works for non-nested objects
        // Will not work correctly for nested objects or complex structures
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\{[^}]*\\})");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "";
    }
    
    /**
     * Unescape JSON string with comprehensive escape sequence handling
     */
    @NotNull
    private String unescapeJson(@NotNull String str) {
        StringBuilder sb = new StringBuilder(str.length());
        int i = 0;
        int len = str.length();
        while (i < len) {
            char c = str.charAt(i++);
            if (c == '\\' && i < len) {
                char esc = str.charAt(i++);
                switch (esc) {
                    case '"':
                        sb.append('\"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 4 <= len) {
                            int codePoint = 0;
                            for (int j = 0; j < 4; j++) {
                                char h = str.charAt(i + j);
                                int digit = Character.digit(h, 16);
                                if (digit == -1) {
                                    // Invalid hex digit; append literally
                                    sb.append('\\').append('u');
                                    sb.append(str, i, i + 4);
                                    codePoint = -1;
                                    break;
                                }
                                codePoint = (codePoint << 4) + digit;
                            }
                            if (codePoint >= 0) {
                                sb.append((char) codePoint);
                            }
                            i += 4;
                        } else {
                            // Truncated unicode escape, append literally
                            sb.append('\\').append('u');
                            sb.append(str.substring(i));
                            i = len;
                        }
                        break;
                    default:
                        // Unknown escape sequence, keep as-is
                        sb.append('\\').append(esc);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * Validate session ID format
     */
    private boolean isValidSessionId(@NotNull String sessionId) {
        return SESSION_ID_PATTERN.matcher(sessionId).matches();
    }
    
    /**
     * Get client IP address (handling proxies)
     */
    @NotNull
    private String getClientIpAddress(@NotNull HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (!CommonUtils.isEmpty(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String remoteAddr = request.getRemoteAddr();
        return CommonUtils.isEmpty(remoteAddr) ? "unknown" : remoteAddr;
    }
    
    /**
     * Send activity response
     */
    private void sendActivityResponse(
        @NotNull HttpServletResponse response,
        @NotNull SessionValidationResult result
    ) throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        PrintWriter writer = response.getWriter();
        writer.write("{");
        writer.write("\"success\":" + result.isSuccess() + ",");
        writer.write("\"session_valid\":" + result.isSessionValid());
        
        if (result.hasWarnings()) {
            writer.write(",\"warnings\":[");
            boolean first = true;
            for (String warning : result.getWarnings()) {
                if (!first) {
                    writer.write(",");
                }
                writer.write("\"" + escapeJson(warning) + "\"");
                first = false;
            }
            writer.write("]");
        }
        
        writer.write("}");
        writer.flush();
    }
    
    /**
     * Send error response
     */
    private void sendError(
        @NotNull HttpServletResponse response,
        int status,
        @NotNull String message
    ) throws IOException {
        
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
    @NotNull
    private String escapeJson(@Nullable String str) {
        if (str == null) {
            return "";
        }
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("/", "\\/");
    }
    
    /**
     * Activity request data structure
     */
    private static class ActivityRequest {
        String sessionId;
        String userId;
        String eventType;
        long timestamp;
        String details;
    }
}
