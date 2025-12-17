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
package io.cloudbeaver.service.query.sharing;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.WebAction;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.service.DBWService;
import io.cloudbeaver.service.query.sharing.dto.QueryFilter;
import io.cloudbeaver.service.query.sharing.dto.SharedQueryRequest;
import io.cloudbeaver.service.query.sharing.model.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;

/**
 * Query sharing web service API
 */
public interface DBWServiceQuerySharing extends DBWService {

    ////////////////////////////////////////////////////
    // Query Management
    ////////////////////////////////////////////////////

    @WebAction(requirePermissions = "query.create")
    @NotNull
    SharedQuery createSharedQuery(
        @NotNull WebSession webSession,
        @NotNull SharedQueryRequest request
    ) throws DBWebException;

    @WebAction
    @NotNull
    List<SharedQuery> listSharedQueries(
        @NotNull WebSession webSession,
        @Nullable QueryFilter filter
    ) throws DBWebException;

    @WebAction
    @Nullable
    SharedQuery getSharedQuery(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException;

    @WebAction(requirePermissions = "query.edit")
    @NotNull
    SharedQuery updateSharedQuery(
        @NotNull WebSession webSession,
        @NotNull String queryId,
        @NotNull SharedQueryRequest request
    ) throws DBWebException;

    @WebAction(requirePermissions = "query.delete")
    boolean deleteSharedQuery(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException;

    @WebAction
    @NotNull
    SharedQuery forkSharedQuery(
        @NotNull WebSession webSession,
        @NotNull String queryId,
        @NotNull String newName
    ) throws DBWebException;

    ////////////////////////////////////////////////////
    // Permissions
    ////////////////////////////////////////////////////

    @WebAction(requirePermissions = "query.share")
    @NotNull
    QueryPermission grantQueryPermission(
        @NotNull WebSession webSession,
        @NotNull String queryId,
        @Nullable String userId,
        @Nullable String teamId,
        @NotNull QueryPermissionType permission
    ) throws DBWebException;

    @WebAction(requirePermissions = "query.share")
    boolean revokeQueryPermission(
        @NotNull WebSession webSession,
        @NotNull String permissionId
    ) throws DBWebException;

    @WebAction
    @NotNull
    List<QueryPermission> getQueryPermissions(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException;

    ////////////////////////////////////////////////////
    // Comments
    ////////////////////////////////////////////////////

    @WebAction(requirePermissions = "query.comment")
    @NotNull
    QueryComment addQueryComment(
        @NotNull WebSession webSession,
        @NotNull String queryId,
        @NotNull String commentText,
        @Nullable String parentId
    ) throws DBWebException;

    @WebAction(requirePermissions = "query.comment")
    @NotNull
    QueryComment updateQueryComment(
        @NotNull WebSession webSession,
        @NotNull String commentId,
        @NotNull String commentText
    ) throws DBWebException;

    @WebAction(requirePermissions = "query.comment")
    boolean deleteQueryComment(
        @NotNull WebSession webSession,
        @NotNull String commentId
    ) throws DBWebException;

    @WebAction
    @NotNull
    List<QueryComment> getQueryComments(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException;

    ////////////////////////////////////////////////////
    // Version History
    ////////////////////////////////////////////////////

    @WebAction
    @NotNull
    List<QueryVersion> getQueryVersions(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException;

    @WebAction(requirePermissions = "query.edit")
    @NotNull
    SharedQuery restoreQueryVersion(
        @NotNull WebSession webSession,
        @NotNull String queryId,
        int versionNumber
    ) throws DBWebException;

    ////////////////////////////////////////////////////
    // Favorites
    ////////////////////////////////////////////////////

    @WebAction
    @NotNull
    QueryFavorite addQueryToFavorites(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException;

    @WebAction
    boolean removeQueryFromFavorites(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException;

    @WebAction
    @NotNull
    List<SharedQuery> getUserFavoriteQueries(
        @NotNull WebSession webSession
    ) throws DBWebException;

    ////////////////////////////////////////////////////
    // Analytics
    ////////////////////////////////////////////////////

    @WebAction(requirePermissions = "query.execute")
    @NotNull
    QueryExecution recordQueryExecution(
        @NotNull WebSession webSession,
        @NotNull String queryId,
        @Nullable Long executionTimeMs,
        @Nullable Integer rowCount,
        @NotNull QueryExecutionStatus status,
        @Nullable String errorMessage
    ) throws DBWebException;

    @WebAction
    @NotNull
    List<QueryExecution> getQueryExecutions(
        @NotNull WebSession webSession,
        @NotNull String queryId
    ) throws DBWebException;
}
