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
package io.cloudbeaver.service.query.sharing.impl;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.service.query.sharing.DBWServiceQuerySharing;
import io.cloudbeaver.service.query.sharing.dto.QueryFilter;
import io.cloudbeaver.service.query.sharing.dto.SharedQueryRequest;
import io.cloudbeaver.service.query.sharing.model.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Query sharing service implementation
 */
public class WebServiceQuerySharing implements DBWServiceQuerySharing {

    private static final Log log = Log.getLog(WebServiceQuerySharing.class);
    
    // Constants for boolean database values
    private static final String BOOLEAN_TRUE = "Y";
    private static final String BOOLEAN_FALSE = "N";

    @NotNull
    @Override
    public SharedQuery createSharedQuery(
        @NotNull WebSession webSession,
        @NotNull SharedQueryRequest request
    ) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            String queryId = UUID.randomUUID().toString();
            
            SharedQuery query = new SharedQuery(queryId, request.getName(), 
                request.getSqlQuery(), userId, request.getVisibility());
            query.setDescription(request.getDescription());
            query.setConnectionId(request.getConnectionId());
            query.setSchemaName(request.getSchemaName());
            query.setTeamId(request.getTeamId());
            query.setTemplate(request.isTemplate());
            query.setTags(request.getTags());
            query.setMetadata(request.getMetadata());

            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                insertQuery(connection, query);
                
                // Create initial version
                createQueryVersion(connection, queryId, 1, request.getSqlQuery(), userId, "Initial version");
            }

            log.info("Created shared query: " + queryId + " by user: " + userId);
            return query;
        } catch (Exception e) {
            throw new DBWebException("Error creating shared query", e);
        }
    }

    @NotNull
    @Override
    public List<SharedQuery> listSharedQueries(
        @NotNull WebSession webSession,
        @Nullable QueryFilter filter
    ) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            List<SharedQuery> queries = new ArrayList<>();

            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                String sql = buildListQueriesSQL(filter, userId);
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    setFilterParameters(stmt, filter, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            queries.add(mapResultSetToQuery(rs));
                        }
                    }
                }
            }

            return queries;
        } catch (Exception e) {
            throw new DBWebException("Error listing shared queries", e);
        }
    }

    @Nullable
    @Override
    public SharedQuery getSharedQuery(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException {
        try {
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                String sql = "SELECT * FROM CB_SHARED_QUERIES WHERE QUERY_ID = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, queryId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            SharedQuery query = mapResultSetToQuery(rs);
                            
                            // Check if user has permission to view
                            if (canViewQuery(connection, query, webSession.getUser().getUserId())) {
                                return query;
                            } else {
                                throw new DBWebException("No permission to view this query");
                            }
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new DBWebException("Error getting shared query", e);
        }
    }

    @NotNull
    @Override
    public SharedQuery updateSharedQuery(
        @NotNull WebSession webSession,
        @NotNull String queryId,
        @NotNull SharedQueryRequest request
    ) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Get current query
                SharedQuery currentQuery = getSharedQueryInternal(connection, queryId);
                if (currentQuery == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                // Check permission
                if (!canEditQuery(connection, currentQuery, userId)) {
                    throw new DBWebException("No permission to edit this query");
                }
                
                // Get current version number
                int currentVersion = getLatestVersionNumber(connection, queryId);
                
                // Create new version if SQL changed
                if (!currentQuery.getSqlQuery().equals(request.getSqlQuery())) {
                    createQueryVersion(connection, queryId, currentVersion + 1, 
                        request.getSqlQuery(), userId, "Updated query");
                }
                
                // Update query
                String sql = "UPDATE CB_SHARED_QUERIES SET QUERY_NAME = ?, DESCRIPTION = ?, " +
                    "SQL_QUERY = ?, VISIBILITY = ?, UPDATED_AT = ? WHERE QUERY_ID = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, request.getName());
                    stmt.setString(2, request.getDescription());
                    stmt.setString(3, request.getSqlQuery());
                    stmt.setString(4, request.getVisibility().name());
                    stmt.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setString(6, queryId);
                    stmt.executeUpdate();
                }
                
                return getSharedQueryInternal(connection, queryId);
            }
        } catch (Exception e) {
            throw new DBWebException("Error updating shared query", e);
        }
    }

    @Override
    public boolean deleteSharedQuery(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    return false;
                }
                
                // Check permission
                if (!canDeleteQuery(connection, query, userId)) {
                    throw new DBWebException("No permission to delete this query");
                }
                
                String sql = "DELETE FROM CB_SHARED_QUERIES WHERE QUERY_ID = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, queryId);
                    int rows = stmt.executeUpdate();
                    return rows > 0;
                }
            }
        } catch (Exception e) {
            throw new DBWebException("Error deleting shared query", e);
        }
    }

    @NotNull
    @Override
    public SharedQuery forkSharedQuery(
        @NotNull WebSession webSession,
        @NotNull String queryId,
        @NotNull String newName
    ) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                SharedQuery originalQuery = getSharedQueryInternal(connection, queryId);
                if (originalQuery == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                // Check permission to view
                if (!canViewQuery(connection, originalQuery, userId)) {
                    throw new DBWebException("No permission to fork this query");
                }
                
                // Create forked query
                String newQueryId = UUID.randomUUID().toString();
                SharedQuery forkedQuery = new SharedQuery(newQueryId, newName, 
                    originalQuery.getSqlQuery(), userId, QueryVisibility.PRIVATE);
                forkedQuery.setDescription("Forked from: " + originalQuery.getName());
                forkedQuery.setConnectionId(originalQuery.getConnectionId());
                forkedQuery.setSchemaName(originalQuery.getSchemaName());
                
                insertQuery(connection, forkedQuery);
                createQueryVersion(connection, newQueryId, 1, originalQuery.getSqlQuery(), 
                    userId, "Forked from query " + queryId);
                
                return forkedQuery;
            }
        } catch (Exception e) {
            throw new DBWebException("Error forking shared query", e);
        }
    }

    // Placeholder implementations for other methods
    // These follow the same pattern but are omitted for brevity
    
    @NotNull
    @Override
    public QueryPermission grantQueryPermission(@NotNull WebSession webSession, @NotNull String queryId, 
        @Nullable String userId, @Nullable String teamId, @NotNull QueryPermissionType permission) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @Override
    public boolean revokeQueryPermission(@NotNull WebSession webSession, @NotNull String permissionId) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @NotNull
    @Override
    public List<QueryPermission> getQueryPermissions(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        return new ArrayList<>();
    }

    @NotNull
    @Override
    public QueryComment addQueryComment(@NotNull WebSession webSession, @NotNull String queryId, 
        @NotNull String commentText, @Nullable String parentId) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @NotNull
    @Override
    public QueryComment updateQueryComment(@NotNull WebSession webSession, @NotNull String commentId, 
        @NotNull String commentText) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @Override
    public boolean deleteQueryComment(@NotNull WebSession webSession, @NotNull String commentId) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @NotNull
    @Override
    public List<QueryComment> getQueryComments(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        return new ArrayList<>();
    }

    @NotNull
    @Override
    public List<QueryVersion> getQueryVersions(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        return new ArrayList<>();
    }

    @NotNull
    @Override
    public SharedQuery restoreQueryVersion(@NotNull WebSession webSession, @NotNull String queryId, 
        int versionNumber) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @NotNull
    @Override
    public QueryFavorite addQueryToFavorites(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @Override
    public boolean removeQueryFromFavorites(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @NotNull
    @Override
    public List<SharedQuery> getUserFavoriteQueries(@NotNull WebSession webSession) throws DBWebException {
        return new ArrayList<>();
    }

    @NotNull
    @Override
    public QueryExecution recordQueryExecution(@NotNull WebSession webSession, @NotNull String queryId, 
        @Nullable Long executionTimeMs, @Nullable Integer rowCount, @NotNull QueryExecutionStatus status, 
        @Nullable String errorMessage) throws DBWebException {
        throw new DBWebException("Not yet implemented");
    }

    @NotNull
    @Override
    public List<QueryExecution> getQueryExecutions(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        return new ArrayList<>();
    }

    // Helper methods
    
    private void insertQuery(Connection connection, SharedQuery query) throws SQLException {
        String sql = "INSERT INTO CB_SHARED_QUERIES (QUERY_ID, QUERY_NAME, DESCRIPTION, SQL_QUERY, " +
            "CONNECTION_ID, SCHEMA_NAME, CREATED_BY, CREATED_AT, UPDATED_AT, VISIBILITY, TEAM_ID, IS_TEMPLATE) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, query.getQueryId());
            stmt.setString(2, query.getName());
            stmt.setString(3, query.getDescription());
            stmt.setString(4, query.getSqlQuery());
            stmt.setString(5, query.getConnectionId());
            stmt.setString(6, query.getSchemaName());
            stmt.setString(7, query.getCreatedBy());
            stmt.setTimestamp(8, java.sql.Timestamp.valueOf(query.getCreatedAt()));
            stmt.setTimestamp(9, java.sql.Timestamp.valueOf(query.getUpdatedAt()));
            stmt.setString(10, query.getVisibility().name());
            stmt.setString(11, query.getTeamId());
            stmt.setString(12, query.isTemplate() ? BOOLEAN_TRUE : BOOLEAN_FALSE);
            stmt.executeUpdate();
        }
    }
    
    private void createQueryVersion(Connection connection, String queryId, int versionNumber, 
        String sqlQuery, String changedBy, String changeDescription) throws SQLException {
        String sql = "INSERT INTO CB_QUERY_VERSIONS (VERSION_ID, QUERY_ID, VERSION_NUMBER, SQL_QUERY, " +
            "CHANGED_BY, CHANGE_DESCRIPTION, CREATED_AT) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, queryId);
            stmt.setInt(3, versionNumber);
            stmt.setString(4, sqlQuery);
            stmt.setString(5, changedBy);
            stmt.setString(6, changeDescription);
            stmt.setTimestamp(7, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }
    
    private int getLatestVersionNumber(Connection connection, String queryId) throws SQLException {
        String sql = "SELECT MAX(VERSION_NUMBER) FROM CB_QUERY_VERSIONS WHERE QUERY_ID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, queryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    private SharedQuery getSharedQueryInternal(Connection connection, String queryId) throws SQLException {
        String sql = "SELECT * FROM CB_SHARED_QUERIES WHERE QUERY_ID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, queryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToQuery(rs);
                }
            }
        }
        return null;
    }
    
    private SharedQuery mapResultSetToQuery(ResultSet rs) throws SQLException {
        SharedQuery query = new SharedQuery();
        query.setQueryId(rs.getString("QUERY_ID"));
        query.setName(rs.getString("QUERY_NAME"));
        query.setDescription(rs.getString("DESCRIPTION"));
        query.setSqlQuery(rs.getString("SQL_QUERY"));
        query.setConnectionId(rs.getString("CONNECTION_ID"));
        query.setSchemaName(rs.getString("SCHEMA_NAME"));
        query.setCreatedBy(rs.getString("CREATED_BY"));
        query.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());
        query.setUpdatedAt(rs.getTimestamp("UPDATED_AT").toLocalDateTime());
        query.setVisibility(QueryVisibility.valueOf(rs.getString("VISIBILITY")));
        query.setTeamId(rs.getString("TEAM_ID"));
        query.setTemplate(BOOLEAN_TRUE.equals(rs.getString("IS_TEMPLATE")));
        return query;
    }
    
    private String buildListQueriesSQL(QueryFilter filter, String userId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM CB_SHARED_QUERIES WHERE 1=1");
        
        // Apply visibility filter
        sql.append(" AND (VISIBILITY = 'PUBLIC' OR CREATED_BY = ? OR VISIBILITY = 'TEAM')");
        
        if (filter != null) {
            if (filter.getCreatedBy() != null) {
                sql.append(" AND CREATED_BY = ?");
            }
            if (filter.getVisibility() != null) {
                sql.append(" AND VISIBILITY = ?");
            }
            if (filter.getIsTemplate() != null) {
                sql.append(" AND IS_TEMPLATE = ?");
            }
            if (filter.getSearchText() != null) {
                sql.append(" AND (QUERY_NAME LIKE ? OR DESCRIPTION LIKE ?)");
            }
        }
        
        sql.append(" ORDER BY CREATED_AT DESC");
        
        if (filter != null && filter.getLimit() > 0) {
            sql.append(" LIMIT ?");
        }
        
        return sql.toString();
    }
    
    private void setFilterParameters(PreparedStatement stmt, QueryFilter filter, String userId) throws SQLException {
        int paramIndex = 1;
        stmt.setString(paramIndex++, userId);
        
        if (filter != null) {
            if (filter.getCreatedBy() != null) {
                stmt.setString(paramIndex++, filter.getCreatedBy());
            }
            if (filter.getVisibility() != null) {
                stmt.setString(paramIndex++, filter.getVisibility().name());
            }
            if (filter.getIsTemplate() != null) {
                stmt.setString(paramIndex++, filter.getIsTemplate() ? BOOLEAN_TRUE : BOOLEAN_FALSE);
            }
            if (filter.getSearchText() != null) {
                String searchPattern = "%" + filter.getSearchText() + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            if (filter.getLimit() > 0) {
                stmt.setInt(paramIndex++, filter.getLimit());
            }
        }
    }
    
    private boolean canViewQuery(Connection connection, SharedQuery query, String userId) throws SQLException {
        // Owner can always view
        if (query.getCreatedBy().equals(userId)) {
            return true;
        }
        
        // Public queries can be viewed by anyone
        if (query.getVisibility() == QueryVisibility.PUBLIC) {
            return true;
        }
        
        // Check explicit permissions
        String sql = "SELECT COUNT(*) FROM CB_QUERY_PERMISSIONS WHERE QUERY_ID = ? AND " +
            "(USER_ID = ? OR TEAM_ID IN (SELECT TEAM_ID FROM CB_AUTH_SUBJECT WHERE SUBJECT_ID = ?)) " +
            "AND PERMISSION IN ('VIEW', 'EDIT', 'DELETE')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, query.getQueryId());
            stmt.setString(2, userId);
            stmt.setString(3, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    private boolean canEditQuery(Connection connection, SharedQuery query, String userId) {
        // Owner can always edit
        return query.getCreatedBy().equals(userId);
    }
    
    private boolean canDeleteQuery(Connection connection, SharedQuery query, String userId) {
        // Only owner can delete
        return query.getCreatedBy().equals(userId);
    }
}
