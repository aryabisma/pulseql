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

/**
 * Query version model for tracking query history
 */
public class QueryVersion {
    
    @NotNull
    private String versionId;
    
    @NotNull
    private String queryId;
    
    private int versionNumber;
    
    @NotNull
    private String sqlQuery;
    
    @NotNull
    private String changedBy;
    
    @Nullable
    private String changeDescription;
    
    @NotNull
    private LocalDateTime createdAt;

    public QueryVersion() {
    }

    public QueryVersion(@NotNull String versionId, @NotNull String queryId, 
                       int versionNumber, @NotNull String sqlQuery, @NotNull String changedBy) {
        this.versionId = versionId;
        this.queryId = queryId;
        this.versionNumber = versionNumber;
        this.sqlQuery = sqlQuery;
        this.changedBy = changedBy;
        this.createdAt = LocalDateTime.now();
    }

    @NotNull
    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(@NotNull String versionId) {
        this.versionId = versionId;
    }

    @NotNull
    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(@NotNull String queryId) {
        this.queryId = queryId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    @NotNull
    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(@NotNull String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    @NotNull
    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(@NotNull String changedBy) {
        this.changedBy = changedBy;
    }

    @Nullable
    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(@Nullable String changeDescription) {
        this.changeDescription = changeDescription;
    }

    @NotNull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NotNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
