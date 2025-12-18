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
 * Query permission model
 */
public class QueryPermission {
    
    @NotNull
    private String permissionId;
    
    @NotNull
    private String queryId;
    
    @Nullable
    private String userId;
    
    @Nullable
    private String teamId;
    
    @NotNull
    private QueryPermissionType permission;
    
    @NotNull
    private String grantedBy;
    
    @NotNull
    private LocalDateTime grantedAt;

    public QueryPermission() {
    }

    public QueryPermission(@NotNull String permissionId, @NotNull String queryId, 
                          @NotNull QueryPermissionType permission, @NotNull String grantedBy) {
        this.permissionId = permissionId;
        this.queryId = queryId;
        this.permission = permission;
        this.grantedBy = grantedBy;
        this.grantedAt = LocalDateTime.now();
    }

    @NotNull
    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(@NotNull String permissionId) {
        this.permissionId = permissionId;
    }

    @NotNull
    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(@NotNull String queryId) {
        this.queryId = queryId;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }

    @Nullable
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(@Nullable String teamId) {
        this.teamId = teamId;
    }

    @NotNull
    public QueryPermissionType getPermission() {
        return permission;
    }

    public void setPermission(@NotNull QueryPermissionType permission) {
        this.permission = permission;
    }

    @NotNull
    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(@NotNull String grantedBy) {
        this.grantedBy = grantedBy;
    }

    @NotNull
    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(@NotNull LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }
}
