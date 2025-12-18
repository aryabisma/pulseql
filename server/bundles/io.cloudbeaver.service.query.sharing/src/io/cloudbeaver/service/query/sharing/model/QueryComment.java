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
 * Query comment model
 */
public class QueryComment {
    
    @NotNull
    private String commentId;
    
    @NotNull
    private String queryId;
    
    @NotNull
    private String userId;
    
    @NotNull
    private String commentText;
    
    @Nullable
    private String parentId;
    
    @NotNull
    private LocalDateTime createdAt;
    
    @NotNull
    private LocalDateTime updatedAt;
    
    private boolean deleted;

    public QueryComment() {
    }

    public QueryComment(@NotNull String commentId, @NotNull String queryId, 
                       @NotNull String userId, @NotNull String commentText) {
        this.commentId = commentId;
        this.queryId = queryId;
        this.userId = userId;
        this.commentText = commentText;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.deleted = false;
    }

    @NotNull
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(@NotNull String commentId) {
        this.commentId = commentId;
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
    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(@NotNull String commentText) {
        this.commentText = commentText;
    }

    @Nullable
    public String getParentId() {
        return parentId;
    }

    public void setParentId(@Nullable String parentId) {
        this.parentId = parentId;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
