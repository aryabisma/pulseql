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
package io.cloudbeaver.service.query.sharing.dto;

import io.cloudbeaver.service.query.sharing.model.QueryVisibility;
import org.jkiss.code.Nullable;

/**
 * Filter for querying shared queries
 */
public class QueryFilter {
    
    @Nullable
    private String createdBy;
    
    @Nullable
    private String teamId;
    
    @Nullable
    private QueryVisibility visibility;
    
    @Nullable
    private Boolean isTemplate;
    
    @Nullable
    private String searchText;
    
    @Nullable
    private String[] tags;
    
    private int limit = 50;
    
    private int offset = 0;
    
    @Nullable
    private String sortBy;
    
    @Nullable
    private String sortOrder;

    @Nullable
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(@Nullable String createdBy) {
        this.createdBy = createdBy;
    }

    @Nullable
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(@Nullable String teamId) {
        this.teamId = teamId;
    }

    @Nullable
    public QueryVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(@Nullable QueryVisibility visibility) {
        this.visibility = visibility;
    }

    @Nullable
    public Boolean getIsTemplate() {
        return isTemplate;
    }

    public void setIsTemplate(@Nullable Boolean template) {
        isTemplate = template;
    }

    @Nullable
    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(@Nullable String searchText) {
        this.searchText = searchText;
    }

    @Nullable
    public String[] getTags() {
        return tags;
    }

    public void setTags(@Nullable String[] tags) {
        this.tags = tags;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Nullable
    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(@Nullable String sortBy) {
        this.sortBy = sortBy;
    }

    @Nullable
    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(@Nullable String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
