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

import java.time.LocalDateTime;

/**
 * Query favorite model
 */
public class QueryFavorite {
    
    @NotNull
    private String favoriteId;
    
    @NotNull
    private String queryId;
    
    @NotNull
    private String userId;
    
    @NotNull
    private LocalDateTime createdAt;

    public QueryFavorite() {
    }

    public QueryFavorite(@NotNull String favoriteId, @NotNull String queryId, @NotNull String userId) {
        this.favoriteId = favoriteId;
        this.queryId = queryId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    @NotNull
    public String getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(@NotNull String favoriteId) {
        this.favoriteId = favoriteId;
    }

    @NotNull
    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(@NotNull String queryId) {
        this.queryId = queryId;
    }

    @NotNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NotNull String userId) {
        this.userId = userId;
    }

    @NotNull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NotNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
