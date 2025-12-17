/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cloudbeaver.service.query.sharing.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Shared query model
 */
public class SharedQuery {
    
    @NotNull
    private String queryId;
    
    @NotNull
    private String name;
    
    @Nullable
    private String description;
    
    @NotNull
    private String sqlQuery;
    
    @Nullable
    private String connectionId;
    
    @Nullable
    private String schemaName;
    
    @NotNull
    private String createdBy;
    
    @NotNull
    private LocalDateTime createdAt;
    
    @NotNull
    private LocalDateTime updatedAt;
    
    @NotNull
    private QueryVisibility visibility;
    
    @Nullable
    private String teamId;
    
    private boolean isTemplate;
    
    @Nullable
    private List<String> tags;
    
    @Nullable
    private Map<String, Object> metadata;

    public SharedQuery() {
    }

    public SharedQuery(@NotNull String queryId, @NotNull String name, @NotNull String sqlQuery, 
                      @NotNull String createdBy, @NotNull QueryVisibility visibility) {
        this.queryId = queryId;
        this.name = name;
        this.sqlQuery = sqlQuery;
        this.createdBy = createdBy;
        this.visibility = visibility;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @NotNull
    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(@NotNull String queryId) {
        this.queryId = queryId;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NotNull
    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(@NotNull String sqlQuery) {
        this.sqlQuery = sqlQuery;
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

    @NotNull
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(@NotNull String createdBy) {
        this.createdBy = createdBy;
    }

    @NotNull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NotNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @NotNull
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NotNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @NotNull
    public QueryVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(@NotNull QueryVisibility visibility) {
        this.visibility = visibility;
    }

    @Nullable
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(@Nullable String teamId) {
        this.teamId = teamId;
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean template) {
        isTemplate = template;
    }

    @Nullable
    public List<String> getTags() {
        return tags;
    }

    public void setTags(@Nullable List<String> tags) {
        this.tags = tags;
    }

    @Nullable
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(@Nullable Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
