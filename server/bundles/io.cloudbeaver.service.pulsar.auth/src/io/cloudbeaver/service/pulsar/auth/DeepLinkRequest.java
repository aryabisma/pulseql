/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Deep Link Request DTO
 * 
 * Request to generate a deep link for PulseQL navigation
 */
public class DeepLinkRequest {
    
    private String targetType;
    private String connectionId;
    private String schemaName;
    private String tableName;
    private String query;
    private String workspaceId;
    
    public DeepLinkRequest() {
    }
    
    @Nullable
    public String getTargetType() {
        return targetType;
    }
    
    public void setTargetType(@Nullable String targetType) {
        this.targetType = targetType;
    }
    
    @Nullable
    public String getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(@Nullable String connectionId) {
        this.connectionId = connectionId;
    }
    
    @Nullable
    public String getSchemaName() {
        return schemaName;
    }
    
    public void setSchemaName(@Nullable String schemaName) {
        this.schemaName = schemaName;
    }
    
    @Nullable
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(@Nullable String tableName) {
        this.tableName = tableName;
    }
    
    @Nullable
    public String getQuery() {
        return query;
    }
    
    public void setQuery(@Nullable String query) {
        this.query = query;
    }
    
    @Nullable
    public String getWorkspaceId() {
        return workspaceId;
    }
    
    public void setWorkspaceId(@Nullable String workspaceId) {
        this.workspaceId = workspaceId;
    }
    
    @Override
    public String toString() {
        return "DeepLinkRequest{" +
            "targetType='" + targetType + '\'' +
            ", connectionId='" + connectionId + '\'' +
            ", schemaName='" + schemaName + '\'' +
            ", tableName='" + tableName + '\'' +
            ", workspaceId='" + workspaceId + '\'' +
            ", query=[REDACTED]" +
            '}';
    }
}
