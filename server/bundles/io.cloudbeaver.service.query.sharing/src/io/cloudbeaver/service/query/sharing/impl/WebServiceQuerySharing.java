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

    ////////////////////////////////////////////////////
    // Permission Management Implementation
    ////////////////////////////////////////////////////
    
    @NotNull
    @Override
    public QueryPermission grantQueryPermission(@NotNull WebSession webSession, @NotNull String queryId, 
        @Nullable String userId, @Nullable String teamId, @NotNull QueryPermissionType permission) throws DBWebException {
        try {
            String grantorId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Verify the query exists and grantor has permission to share
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                // Only owner can grant permissions
                if (!query.getCreatedBy().equals(grantorId)) {
                    throw new DBWebException("Only query owner can grant permissions");
                }
                
                // Validate at least one of userId or teamId is provided
                if (userId == null && teamId == null) {
                    throw new DBWebException("Either userId or teamId must be provided");
                }
                
                String permissionId = UUID.randomUUID().toString();
                String sql = "INSERT INTO CB_QUERY_PERMISSIONS (PERMISSION_ID, QUERY_ID, USER_ID, TEAM_ID, " +
                    "PERMISSION, GRANTED_BY, GRANTED_AT) VALUES (?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, permissionId);
                    stmt.setString(2, queryId);
                    stmt.setString(3, userId);
                    stmt.setString(4, teamId);
                    stmt.setString(5, permission.name());
                    stmt.setString(6, grantorId);
                    stmt.setTimestamp(7, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    stmt.executeUpdate();
                }
                
                QueryPermission queryPermission = new QueryPermission(permissionId, queryId, permission, grantorId);
                queryPermission.setUserId(userId);
                queryPermission.setTeamId(teamId);
                
                log.info("Granted permission " + permission + " on query " + queryId + " to " + 
                    (userId != null ? "user " + userId : "team " + teamId));
                
                return queryPermission;
            }
        } catch (Exception e) {
            throw new DBWebException("Error granting query permission", e);
        }
    }

    @Override
    public boolean revokeQueryPermission(@NotNull WebSession webSession, @NotNull String permissionId) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // First check if the permission exists and user has rights to revoke it
                String checkSql = "SELECT qp.QUERY_ID, sq.CREATED_BY FROM CB_QUERY_PERMISSIONS qp " +
                    "JOIN CB_SHARED_QUERIES sq ON qp.QUERY_ID = sq.QUERY_ID WHERE qp.PERMISSION_ID = ?";
                
                String queryOwner = null;
                try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                    stmt.setString(1, permissionId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            queryOwner = rs.getString("CREATED_BY");
                        }
                    }
                }
                
                if (queryOwner == null) {
                    return false; // Permission not found
                }
                
                // Only owner can revoke permissions
                if (!queryOwner.equals(userId)) {
                    throw new DBWebException("Only query owner can revoke permissions");
                }
                
                String sql = "DELETE FROM CB_QUERY_PERMISSIONS WHERE PERMISSION_ID = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, permissionId);
                    int rows = stmt.executeUpdate();
                    
                    if (rows > 0) {
                        log.info("Revoked permission: " + permissionId);
                    }
                    
                    return rows > 0;
                }
            }
        } catch (Exception e) {
            throw new DBWebException("Error revoking query permission", e);
        }
    }

    @NotNull
    @Override
    public List<QueryPermission> getQueryPermissions(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        try {
            List<QueryPermission> permissions = new ArrayList<>();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Verify user has access to view permissions (owner or has permission)
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                String userId = webSession.getUser().getUserId();
                if (!query.getCreatedBy().equals(userId) && !canViewQuery(connection, query, userId)) {
                    throw new DBWebException("No permission to view query permissions");
                }
                
                String sql = "SELECT * FROM CB_QUERY_PERMISSIONS WHERE QUERY_ID = ? ORDER BY GRANTED_AT DESC";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, queryId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            QueryPermission perm = new QueryPermission();
                            perm.setPermissionId(rs.getString("PERMISSION_ID"));
                            perm.setQueryId(rs.getString("QUERY_ID"));
                            perm.setUserId(rs.getString("USER_ID"));
                            perm.setTeamId(rs.getString("TEAM_ID"));
                            perm.setPermission(QueryPermissionType.valueOf(rs.getString("PERMISSION")));
                            perm.setGrantedBy(rs.getString("GRANTED_BY"));
                            perm.setGrantedAt(rs.getTimestamp("GRANTED_AT").toLocalDateTime());
                            permissions.add(perm);
                        }
                    }
                }
            }
            
            return permissions;
        } catch (Exception e) {
            throw new DBWebException("Error getting query permissions", e);
        }
    }

    ////////////////////////////////////////////////////
    // Comment Management Implementation
    ////////////////////////////////////////////////////

    @NotNull
    @Override
    public QueryComment addQueryComment(@NotNull WebSession webSession, @NotNull String queryId, 
        @NotNull String commentText, @Nullable String parentId) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Verify query exists and user has permission to view it
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                if (!canViewQuery(connection, query, userId)) {
                    throw new DBWebException("No permission to comment on this query");
                }
                
                // If parentId is provided, verify it exists
                if (parentId != null) {
                    String checkSql = "SELECT COUNT(*) FROM CB_QUERY_COMMENTS WHERE COMMENT_ID = ? AND QUERY_ID = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                        stmt.setString(1, parentId);
                        stmt.setString(2, queryId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) == 0) {
                                throw new DBWebException("Parent comment not found: " + parentId);
                            }
                        }
                    }
                }
                
                String commentId = UUID.randomUUID().toString();
                String sql = "INSERT INTO CB_QUERY_COMMENTS (COMMENT_ID, QUERY_ID, USER_ID, COMMENT_TEXT, " +
                    "PARENT_ID, CREATED_AT, UPDATED_AT, IS_DELETED) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                
                LocalDateTime now = LocalDateTime.now();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, commentId);
                    stmt.setString(2, queryId);
                    stmt.setString(3, userId);
                    stmt.setString(4, commentText);
                    stmt.setString(5, parentId);
                    stmt.setTimestamp(6, java.sql.Timestamp.valueOf(now));
                    stmt.setTimestamp(7, java.sql.Timestamp.valueOf(now));
                    stmt.setString(8, BOOLEAN_FALSE);
                    stmt.executeUpdate();
                }
                
                QueryComment comment = new QueryComment(commentId, queryId, userId, commentText);
                comment.setParentId(parentId);
                comment.setCreatedAt(now);
                comment.setUpdatedAt(now);
                
                log.info("Added comment " + commentId + " to query " + queryId);
                
                return comment;
            }
        } catch (Exception e) {
            throw new DBWebException("Error adding query comment", e);
        }
    }

    @NotNull
    @Override
    public QueryComment updateQueryComment(@NotNull WebSession webSession, @NotNull String commentId, 
        @NotNull String commentText) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Get existing comment and verify ownership
                String checkSql = "SELECT QUERY_ID, USER_ID FROM CB_QUERY_COMMENTS WHERE COMMENT_ID = ?";
                String queryId = null;
                String commentOwnerId = null;
                
                try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                    stmt.setString(1, commentId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            queryId = rs.getString("QUERY_ID");
                            commentOwnerId = rs.getString("USER_ID");
                        }
                    }
                }
                
                if (commentOwnerId == null) {
                    throw new DBWebException("Comment not found: " + commentId);
                }
                
                // Only comment owner can update
                if (!commentOwnerId.equals(userId)) {
                    throw new DBWebException("Only comment owner can update the comment");
                }
                
                String sql = "UPDATE CB_QUERY_COMMENTS SET COMMENT_TEXT = ?, UPDATED_AT = ? WHERE COMMENT_ID = ?";
                LocalDateTime now = LocalDateTime.now();
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, commentText);
                    stmt.setTimestamp(2, java.sql.Timestamp.valueOf(now));
                    stmt.setString(3, commentId);
                    stmt.executeUpdate();
                }
                
                // Fetch and return updated comment
                return getCommentById(connection, commentId);
            }
        } catch (Exception e) {
            throw new DBWebException("Error updating query comment", e);
        }
    }

    @Override
    public boolean deleteQueryComment(@NotNull WebSession webSession, @NotNull String commentId) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Get comment and verify ownership or query ownership
                String checkSql = "SELECT qc.QUERY_ID, qc.USER_ID, sq.CREATED_BY FROM CB_QUERY_COMMENTS qc " +
                    "JOIN CB_SHARED_QUERIES sq ON qc.QUERY_ID = sq.QUERY_ID WHERE qc.COMMENT_ID = ?";
                
                String commentOwnerId = null;
                String queryOwnerId = null;
                
                try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                    stmt.setString(1, commentId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            commentOwnerId = rs.getString("USER_ID");
                            queryOwnerId = rs.getString("CREATED_BY");
                        }
                    }
                }
                
                if (commentOwnerId == null) {
                    return false; // Comment not found
                }
                
                // Either comment owner or query owner can delete
                if (!commentOwnerId.equals(userId) && !queryOwnerId.equals(userId)) {
                    throw new DBWebException("Only comment owner or query owner can delete the comment");
                }
                
                // Soft delete by marking as deleted
                String sql = "UPDATE CB_QUERY_COMMENTS SET IS_DELETED = ?, UPDATED_AT = ? WHERE COMMENT_ID = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, BOOLEAN_TRUE);
                    stmt.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setString(3, commentId);
                    int rows = stmt.executeUpdate();
                    
                    if (rows > 0) {
                        log.info("Deleted comment: " + commentId);
                    }
                    
                    return rows > 0;
                }
            }
        } catch (Exception e) {
            throw new DBWebException("Error deleting query comment", e);
        }
    }

    @NotNull
    @Override
    public List<QueryComment> getQueryComments(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        try {
            List<QueryComment> comments = new ArrayList<>();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Verify user has permission to view the query
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                String userId = webSession.getUser().getUserId();
                if (!canViewQuery(connection, query, userId)) {
                    throw new DBWebException("No permission to view query comments");
                }
                
                String sql = "SELECT * FROM CB_QUERY_COMMENTS WHERE QUERY_ID = ? AND IS_DELETED = ? ORDER BY CREATED_AT ASC";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, queryId);
                    stmt.setString(2, BOOLEAN_FALSE);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            QueryComment comment = new QueryComment();
                            comment.setCommentId(rs.getString("COMMENT_ID"));
                            comment.setQueryId(rs.getString("QUERY_ID"));
                            comment.setUserId(rs.getString("USER_ID"));
                            comment.setCommentText(rs.getString("COMMENT_TEXT"));
                            comment.setParentId(rs.getString("PARENT_ID"));
                            comment.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());
                            comment.setUpdatedAt(rs.getTimestamp("UPDATED_AT").toLocalDateTime());
                            comment.setDeleted(BOOLEAN_TRUE.equals(rs.getString("IS_DELETED")));
                            comments.add(comment);
                        }
                    }
                }
            }
            
            return comments;
        } catch (Exception e) {
            throw new DBWebException("Error getting query comments", e);
        }
    }

    ////////////////////////////////////////////////////
    // Version History Implementation
    ////////////////////////////////////////////////////

    @NotNull
    @Override
    public List<QueryVersion> getQueryVersions(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        try {
            List<QueryVersion> versions = new ArrayList<>();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Verify user has permission to view the query
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                String userId = webSession.getUser().getUserId();
                if (!canViewQuery(connection, query, userId)) {
                    throw new DBWebException("No permission to view query versions");
                }
                
                String sql = "SELECT * FROM CB_QUERY_VERSIONS WHERE QUERY_ID = ? ORDER BY VERSION_NUMBER DESC";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, queryId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            QueryVersion version = new QueryVersion();
                            version.setVersionId(rs.getString("VERSION_ID"));
                            version.setQueryId(rs.getString("QUERY_ID"));
                            version.setVersionNumber(rs.getInt("VERSION_NUMBER"));
                            version.setSqlQuery(rs.getString("SQL_QUERY"));
                            version.setChangedBy(rs.getString("CHANGED_BY"));
                            version.setChangeDescription(rs.getString("CHANGE_DESCRIPTION"));
                            version.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());
                            versions.add(version);
                        }
                    }
                }
            }
            
            return versions;
        } catch (Exception e) {
            throw new DBWebException("Error getting query versions", e);
        }
    }

    @NotNull
    @Override
    public SharedQuery restoreQueryVersion(@NotNull WebSession webSession, @NotNull String queryId, 
        int versionNumber) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Get current query and verify edit permission
                SharedQuery currentQuery = getSharedQueryInternal(connection, queryId);
                if (currentQuery == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                if (!canEditQuery(connection, currentQuery, userId)) {
                    throw new DBWebException("No permission to restore query version");
                }
                
                // Get the version to restore
                String versionSql = "SELECT SQL_QUERY FROM CB_QUERY_VERSIONS WHERE QUERY_ID = ? AND VERSION_NUMBER = ?";
                String sqlToRestore = null;
                
                try (PreparedStatement stmt = connection.prepareStatement(versionSql)) {
                    stmt.setString(1, queryId);
                    stmt.setInt(2, versionNumber);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            sqlToRestore = rs.getString("SQL_QUERY");
                        }
                    }
                }
                
                if (sqlToRestore == null) {
                    throw new DBWebException("Version " + versionNumber + " not found for query " + queryId);
                }
                
                // Get current version number
                int currentVersion = getLatestVersionNumber(connection, queryId);
                
                // Update query with restored SQL
                String updateSql = "UPDATE CB_SHARED_QUERIES SET SQL_QUERY = ?, UPDATED_AT = ? WHERE QUERY_ID = ?";
                try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                    stmt.setString(1, sqlToRestore);
                    stmt.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setString(3, queryId);
                    stmt.executeUpdate();
                }
                
                // Create new version entry
                createQueryVersion(connection, queryId, currentVersion + 1, sqlToRestore, userId, 
                    "Restored from version " + versionNumber);
                
                log.info("Restored query " + queryId + " to version " + versionNumber);
                
                return getSharedQueryInternal(connection, queryId);
            }
        } catch (Exception e) {
            throw new DBWebException("Error restoring query version", e);
        }
    }

    ////////////////////////////////////////////////////
    // Favorites Implementation
    ////////////////////////////////////////////////////

    @NotNull
    @Override
    public QueryFavorite addQueryToFavorites(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Verify query exists and user has permission to view it
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                if (!canViewQuery(connection, query, userId)) {
                    throw new DBWebException("No permission to favorite this query");
                }
                
                // Check if already favorited
                String checkSql = "SELECT COUNT(*) FROM CB_QUERY_FAVORITES WHERE QUERY_ID = ? AND USER_ID = ?";
                try (PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                    stmt.setString(1, queryId);
                    stmt.setString(2, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new DBWebException("Query is already in favorites");
                        }
                    }
                }
                
                String favoriteId = UUID.randomUUID().toString();
                String sql = "INSERT INTO CB_QUERY_FAVORITES (FAVORITE_ID, QUERY_ID, USER_ID, CREATED_AT) VALUES (?, ?, ?, ?)";
                
                LocalDateTime now = LocalDateTime.now();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, favoriteId);
                    stmt.setString(2, queryId);
                    stmt.setString(3, userId);
                    stmt.setTimestamp(4, java.sql.Timestamp.valueOf(now));
                    stmt.executeUpdate();
                }
                
                QueryFavorite favorite = new QueryFavorite(favoriteId, queryId, userId);
                favorite.setCreatedAt(now);
                
                log.info("Added query " + queryId + " to favorites for user " + userId);
                
                return favorite;
            }
        } catch (Exception e) {
            throw new DBWebException("Error adding query to favorites", e);
        }
    }

    @Override
    public boolean removeQueryFromFavorites(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                String sql = "DELETE FROM CB_QUERY_FAVORITES WHERE QUERY_ID = ? AND USER_ID = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, queryId);
                    stmt.setString(2, userId);
                    int rows = stmt.executeUpdate();
                    
                    if (rows > 0) {
                        log.info("Removed query " + queryId + " from favorites for user " + userId);
                    }
                    
                    return rows > 0;
                }
            }
        } catch (Exception e) {
            throw new DBWebException("Error removing query from favorites", e);
        }
    }

    @NotNull
    @Override
    public List<SharedQuery> getUserFavoriteQueries(@NotNull WebSession webSession) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            List<SharedQuery> favoriteQueries = new ArrayList<>();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                String sql = "SELECT sq.* FROM CB_SHARED_QUERIES sq " +
                    "JOIN CB_QUERY_FAVORITES qf ON sq.QUERY_ID = qf.QUERY_ID " +
                    "WHERE qf.USER_ID = ? ORDER BY qf.CREATED_AT DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            favoriteQueries.add(mapResultSetToQuery(rs));
                        }
                    }
                }
            }
            
            return favoriteQueries;
        } catch (Exception e) {
            throw new DBWebException("Error getting favorite queries", e);
        }
    }

    ////////////////////////////////////////////////////
    // Analytics/Execution Tracking Implementation
    ////////////////////////////////////////////////////

    @NotNull
    @Override
    public QueryExecution recordQueryExecution(@NotNull WebSession webSession, @NotNull String queryId, 
        @Nullable Long executionTimeMs, @Nullable Integer rowCount, @NotNull QueryExecutionStatus status, 
        @Nullable String errorMessage) throws DBWebException {
        try {
            String userId = webSession.getUser().getUserId();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Verify query exists and user has permission to execute it
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                if (!canViewQuery(connection, query, userId)) {
                    throw new DBWebException("No permission to execute this query");
                }
                
                String executionId = UUID.randomUUID().toString();
                String sql = "INSERT INTO CB_QUERY_EXECUTIONS (EXECUTION_ID, QUERY_ID, USER_ID, " +
                    "EXECUTION_TIME_MS, ROW_COUNT, STATUS, ERROR_MESSAGE, EXECUTED_AT) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                
                LocalDateTime now = LocalDateTime.now();
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, executionId);
                    stmt.setString(2, queryId);
                    stmt.setString(3, userId);
                    
                    if (executionTimeMs != null) {
                        stmt.setLong(4, executionTimeMs);
                    } else {
                        stmt.setNull(4, java.sql.Types.BIGINT);
                    }
                    
                    if (rowCount != null) {
                        stmt.setInt(5, rowCount);
                    } else {
                        stmt.setNull(5, java.sql.Types.INTEGER);
                    }
                    
                    stmt.setString(6, status.name());
                    stmt.setString(7, errorMessage);
                    stmt.setTimestamp(8, java.sql.Timestamp.valueOf(now));
                    stmt.executeUpdate();
                }
                
                QueryExecution execution = new QueryExecution(executionId, queryId, userId, status);
                execution.setExecutionTimeMs(executionTimeMs);
                execution.setRowCount(rowCount);
                execution.setErrorMessage(errorMessage);
                execution.setExecutedAt(now);
                
                log.debug("Recorded execution " + executionId + " for query " + queryId + " with status " + status);
                
                return execution;
            }
        } catch (Exception e) {
            throw new DBWebException("Error recording query execution", e);
        }
    }

    @NotNull
    @Override
    public List<QueryExecution> getQueryExecutions(@NotNull WebSession webSession, @NotNull String queryId) throws DBWebException {
        try {
            List<QueryExecution> executions = new ArrayList<>();
            
            try (Connection connection = webSession.getSecurityController().getDatabase().openConnection()) {
                // Verify user has permission to view the query
                SharedQuery query = getSharedQueryInternal(connection, queryId);
                if (query == null) {
                    throw new DBWebException("Query not found: " + queryId);
                }
                
                String userId = webSession.getUser().getUserId();
                if (!canViewQuery(connection, query, userId)) {
                    throw new DBWebException("No permission to view query executions");
                }
                
                String sql = "SELECT * FROM CB_QUERY_EXECUTIONS WHERE QUERY_ID = ? ORDER BY EXECUTED_AT DESC LIMIT 100";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, queryId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            QueryExecution execution = new QueryExecution();
                            execution.setExecutionId(rs.getString("EXECUTION_ID"));
                            execution.setQueryId(rs.getString("QUERY_ID"));
                            execution.setUserId(rs.getString("USER_ID"));
                            
                            long execTime = rs.getLong("EXECUTION_TIME_MS");
                            if (!rs.wasNull()) {
                                execution.setExecutionTimeMs(execTime);
                            }
                            
                            int rows = rs.getInt("ROW_COUNT");
                            if (!rs.wasNull()) {
                                execution.setRowCount(rows);
                            }
                            
                            execution.setStatus(QueryExecutionStatus.valueOf(rs.getString("STATUS")));
                            execution.setErrorMessage(rs.getString("ERROR_MESSAGE"));
                            execution.setExecutedAt(rs.getTimestamp("EXECUTED_AT").toLocalDateTime());
                            executions.add(execution);
                        }
                    }
                }
            }
            
            return executions;
        } catch (Exception e) {
            throw new DBWebException("Error getting query executions", e);
        }
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
        
        // Team visibility - check if user is in the same team
        if (query.getVisibility() == QueryVisibility.TEAM && query.getTeamId() != null) {
            if (isUserInTeam(connection, userId, query.getTeamId())) {
                return true;
            }
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
    
    private QueryComment getCommentById(Connection connection, String commentId) throws SQLException {
        String sql = "SELECT * FROM CB_QUERY_COMMENTS WHERE COMMENT_ID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, commentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    QueryComment comment = new QueryComment();
                    comment.setCommentId(rs.getString("COMMENT_ID"));
                    comment.setQueryId(rs.getString("QUERY_ID"));
                    comment.setUserId(rs.getString("USER_ID"));
                    comment.setCommentText(rs.getString("COMMENT_TEXT"));
                    comment.setParentId(rs.getString("PARENT_ID"));
                    comment.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());
                    comment.setUpdatedAt(rs.getTimestamp("UPDATED_AT").toLocalDateTime());
                    comment.setDeleted(BOOLEAN_TRUE.equals(rs.getString("IS_DELETED")));
                    return comment;
                }
            }
        }
        return null;
    }
    
    private boolean isUserInTeam(Connection connection, String userId, String teamId) throws SQLException {
        // Check if user is a member of the specified team
        // This queries the team membership by checking if a user has any permissions
        // associated with the team (teams are stored as subjects in CloudBeaver)
        String sql = "SELECT COUNT(*) FROM CB_USER usr " +
            "JOIN CB_USER_TEAM ut ON usr.USER_ID = ut.USER_ID " +
            "WHERE usr.USER_ID = ? AND ut.TEAM_ID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}
