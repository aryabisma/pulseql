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
import io.cloudbeaver.server.CBApplication;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Deep Link Service
 * 
 * Generates deep links for PulseQL workspace navigation
 */
public class DeepLinkService {
    
    private static final Log log = Log.getLog(DeepLinkService.class);
    
    private static final String DEEP_LINK_PARAM = "pulsar_link";
    private static final int MAX_QUERY_LENGTH = 50000; // 50KB max query size
    private static final int MAX_IDENTIFIER_LENGTH = 200;
    
    /**
     * Generate deep link for table
     */
    @NotNull
    public String generateTableLink(
        @NotNull String connectionId,
        @NotNull String schemaName,
        @NotNull String tableName,
        @Nullable String baseUrl
    ) throws DBException {
        validateIdentifier(connectionId, "connectionId");
        validateIdentifier(schemaName, "schemaName");
        validateIdentifier(tableName, "tableName");
        
        String encoded = String.format("table:%s/%s/%s",
            urlEncode(connectionId),
            urlEncode(schemaName),
            urlEncode(tableName)
        );
        
        return buildDeepLinkUrl(encoded, baseUrl);
    }
    
    /**
     * Generate deep link for schema
     */
    @NotNull
    public String generateSchemaLink(
        @NotNull String connectionId,
        @NotNull String schemaName,
        @Nullable String baseUrl
    ) throws DBException {
        validateIdentifier(connectionId, "connectionId");
        validateIdentifier(schemaName, "schemaName");
        
        String encoded = String.format("schema:%s/%s",
            urlEncode(connectionId),
            urlEncode(schemaName)
        );
        
        return buildDeepLinkUrl(encoded, baseUrl);
    }
    
    /**
     * Generate deep link for query
     */
    @NotNull
    public String generateQueryLink(
        @NotNull String query,
        @Nullable String baseUrl
    ) throws DBException {
        validateQuery(query);
        
        // Encode query as base64
        String base64Query = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(query.getBytes(StandardCharsets.UTF_8));
        
        String encoded = "query:" + base64Query;
        
        return buildDeepLinkUrl(encoded, baseUrl);
    }
    
    /**
     * Generate deep link for connection
     */
    @NotNull
    public String generateConnectionLink(
        @NotNull String connectionId,
        @Nullable String baseUrl
    ) throws DBException {
        validateIdentifier(connectionId, "connectionId");
        
        String encoded = "connection:" + urlEncode(connectionId);
        
        return buildDeepLinkUrl(encoded, baseUrl);
    }
    
    /**
     * Generate deep link for workspace
     */
    @NotNull
    public String generateWorkspaceLink(
        @NotNull String workspaceId,
        @Nullable String baseUrl
    ) throws DBException {
        validateIdentifier(workspaceId, "workspaceId");
        
        String encoded = "workspace:" + urlEncode(workspaceId);
        
        return buildDeepLinkUrl(encoded, baseUrl);
    }
    
    /**
     * Generate deep link from request
     */
    @NotNull
    public DeepLinkResponse generateDeepLink(
        @NotNull WebSession session,
        @NotNull DeepLinkRequest request
    ) throws DBWebException {
        try {
            // Validate user session
            if (session.getUser() == null) {
                return DeepLinkResponse.error("User not authenticated");
            }
            
            // Get base URL from configuration
            String baseUrl = getPulseQLBaseUrl();
            if (CommonUtils.isEmpty(baseUrl)) {
                log.warn("PulseQL base URL not configured");
                return DeepLinkResponse.error("PulseQL base URL not configured");
            }
            
            String deepLink;
            
            // Generate deep link based on target type
            String targetType = request.getTargetType();
            if (CommonUtils.isEmpty(targetType)) {
                return DeepLinkResponse.error("Target type is required");
            }
            
            switch (targetType.toLowerCase()) {
                case "table":
                    if (CommonUtils.isEmpty(request.getConnectionId()) ||
                        CommonUtils.isEmpty(request.getSchemaName()) ||
                        CommonUtils.isEmpty(request.getTableName())) {
                        return DeepLinkResponse.error("connectionId, schemaName, and tableName are required for table links");
                    }
                    deepLink = generateTableLink(
                        request.getConnectionId(),
                        request.getSchemaName(),
                        request.getTableName(),
                        baseUrl
                    );
                    break;
                    
                case "schema":
                    if (CommonUtils.isEmpty(request.getConnectionId()) ||
                        CommonUtils.isEmpty(request.getSchemaName())) {
                        return DeepLinkResponse.error("connectionId and schemaName are required for schema links");
                    }
                    deepLink = generateSchemaLink(
                        request.getConnectionId(),
                        request.getSchemaName(),
                        baseUrl
                    );
                    break;
                    
                case "query":
                    if (CommonUtils.isEmpty(request.getQuery())) {
                        return DeepLinkResponse.error("query is required for query links");
                    }
                    deepLink = generateQueryLink(request.getQuery(), baseUrl);
                    break;
                    
                case "connection":
                    if (CommonUtils.isEmpty(request.getConnectionId())) {
                        return DeepLinkResponse.error("connectionId is required for connection links");
                    }
                    deepLink = generateConnectionLink(request.getConnectionId(), baseUrl);
                    break;
                    
                case "workspace":
                    if (CommonUtils.isEmpty(request.getWorkspaceId())) {
                        return DeepLinkResponse.error("workspaceId is required for workspace links");
                    }
                    deepLink = generateWorkspaceLink(request.getWorkspaceId(), baseUrl);
                    break;
                    
                default:
                    return DeepLinkResponse.error("Unknown target type: " + targetType);
            }
            
            log.debug("Generated deep link for user " + session.getUser().getUserId() + ": " + targetType);
            
            return DeepLinkResponse.success(deepLink, null);
            
        } catch (DBException e) {
            log.error("Failed to generate deep link", e);
            return DeepLinkResponse.error("Failed to generate deep link: " + e.getMessage());
        }
    }
    
    /**
     * Build deep link URL with parameter
     */
    @NotNull
    private String buildDeepLinkUrl(@NotNull String encodedTarget, @Nullable String baseUrl) {
        String url = CommonUtils.isEmpty(baseUrl) ? getPulseQLBaseUrl() : baseUrl;
        
        // Ensure URL ends with /
        if (!url.endsWith("/")) {
            url += "/";
        }
        
        // Build URL with deep link parameter
        return url + "?" + DEEP_LINK_PARAM + "=" + encodedTarget;
    }
    
    /**
     * Get PulseQL base URL from configuration
     */
    @NotNull
    private String getPulseQLBaseUrl() {
        String baseUrl = CBApplication.getInstance().getAppConfiguration()
            .getConfigurationValue("pulsar.pulseql.baseUrl");
        
        if (CommonUtils.isEmpty(baseUrl)) {
            // Fallback to default
            baseUrl = "http://localhost:8080/workspace";
            log.warn("Using default PulseQL base URL: " + baseUrl);
        }
        
        return baseUrl;
    }
    
    /**
     * Validate identifier (connection ID, schema name, table name, etc.)
     * 
     * Note: These are basic defense-in-depth checks. The receiving PulseQL application
     * must perform its own validation as these identifiers are URL-encoded during transmission.
     */
    private void validateIdentifier(@NotNull String identifier, @NotNull String fieldName) throws DBException {
        if (CommonUtils.isEmpty(identifier)) {
            throw new DBException(fieldName + " cannot be empty");
        }
        
        if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
            throw new DBException(fieldName + " exceeds maximum length of " + MAX_IDENTIFIER_LENGTH);
        }
        
        // Basic defense-in-depth checks for common attack patterns
        // Note: These checks may have false positives. URL encoding provides primary protection.
        String lower = identifier.toLowerCase();
        if (lower.contains("<script") || lower.contains("javascript:") ||
            lower.contains("' or ") || lower.contains("\" or ") ||
            lower.contains("; drop ") || lower.contains("union select")) {
            throw new DBException("Invalid characters in " + fieldName);
        }
    }
    
    /**
     * Validate SQL query
     * 
     * Note: Query is Base64-encoded before URL transmission, which prevents XSS.
     * The receiving PulseQL application is responsible for validating query content.
     */
    private void validateQuery(@NotNull String query) throws DBException {
        if (CommonUtils.isEmpty(query)) {
            throw new DBException("Query cannot be empty");
        }
        
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new DBException("Query exceeds maximum length of " + MAX_QUERY_LENGTH);
        }
    }
    
    /**
     * URL encode string
     */
    @NotNull
    private String urlEncode(@NotNull String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
