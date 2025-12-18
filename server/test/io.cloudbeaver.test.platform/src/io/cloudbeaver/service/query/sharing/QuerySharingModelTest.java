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

import io.cloudbeaver.service.query.sharing.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic tests for Query Sharing models
 */
public class QuerySharingModelTest {

    @Test
    public void testSharedQueryCreation() {
        String queryId = "test-query-id";
        String name = "Test Query";
        String sql = "SELECT * FROM users";
        String userId = "user1";
        QueryVisibility visibility = QueryVisibility.PRIVATE;

        SharedQuery query = new SharedQuery(queryId, name, sql, userId, visibility);
        
        Assert.assertEquals(queryId, query.getQueryId());
        Assert.assertEquals(name, query.getName());
        Assert.assertEquals(sql, query.getSqlQuery());
        Assert.assertEquals(userId, query.getCreatedBy());
        Assert.assertEquals(visibility, query.getVisibility());
        Assert.assertNotNull(query.getCreatedAt());
        Assert.assertNotNull(query.getUpdatedAt());
        Assert.assertFalse(query.isTemplate());
    }

    @Test
    public void testSharedQueryWithMetadata() {
        SharedQuery query = new SharedQuery("id1", "Query 1", "SELECT 1", "user1", QueryVisibility.PUBLIC);
        
        // Set metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "reporting");
        metadata.put("priority", 1);
        query.setMetadata(metadata);
        
        // Set tags
        query.setTags(Arrays.asList("sales", "monthly", "report"));
        
        Assert.assertNotNull(query.getMetadata());
        Assert.assertEquals("reporting", query.getMetadata().get("category"));
        Assert.assertEquals(3, query.getTags().size());
        Assert.assertTrue(query.getTags().contains("sales"));
    }

    @Test
    public void testQueryVisibility() {
        Assert.assertEquals(3, QueryVisibility.values().length);
        Assert.assertEquals(QueryVisibility.PRIVATE, QueryVisibility.valueOf("PRIVATE"));
        Assert.assertEquals(QueryVisibility.TEAM, QueryVisibility.valueOf("TEAM"));
        Assert.assertEquals(QueryVisibility.PUBLIC, QueryVisibility.valueOf("PUBLIC"));
    }

    @Test
    public void testQueryPermissionTypes() {
        Assert.assertEquals(4, QueryPermissionType.values().length);
        Assert.assertEquals(QueryPermissionType.VIEW, QueryPermissionType.valueOf("VIEW"));
        Assert.assertEquals(QueryPermissionType.EDIT, QueryPermissionType.valueOf("EDIT"));
        Assert.assertEquals(QueryPermissionType.EXECUTE, QueryPermissionType.valueOf("EXECUTE"));
        Assert.assertEquals(QueryPermissionType.DELETE, QueryPermissionType.valueOf("DELETE"));
    }

    @Test
    public void testQueryPermissionCreation() {
        String permId = "perm1";
        String queryId = "query1";
        QueryPermissionType permType = QueryPermissionType.VIEW;
        String grantedBy = "admin";

        QueryPermission perm = new QueryPermission(permId, queryId, permType, grantedBy);
        
        Assert.assertEquals(permId, perm.getPermissionId());
        Assert.assertEquals(queryId, perm.getQueryId());
        Assert.assertEquals(permType, perm.getPermission());
        Assert.assertEquals(grantedBy, perm.getGrantedBy());
        Assert.assertNotNull(perm.getGrantedAt());
    }

    @Test
    public void testQueryCommentThreading() {
        QueryComment parent = new QueryComment("c1", "q1", "u1", "Parent comment");
        QueryComment child = new QueryComment("c2", "q1", "u2", "Child comment");
        child.setParentId(parent.getCommentId());
        
        Assert.assertNull(parent.getParentId());
        Assert.assertNotNull(child.getParentId());
        Assert.assertEquals(parent.getCommentId(), child.getParentId());
    }

    @Test
    public void testQueryVersionCreation() {
        String versionId = "v1";
        String queryId = "q1";
        int versionNumber = 2;
        String sql = "SELECT * FROM users WHERE active = true";
        String changedBy = "user1";

        QueryVersion version = new QueryVersion(versionId, queryId, versionNumber, sql, changedBy);
        
        Assert.assertEquals(versionId, version.getVersionId());
        Assert.assertEquals(queryId, version.getQueryId());
        Assert.assertEquals(versionNumber, version.getVersionNumber());
        Assert.assertEquals(sql, version.getSqlQuery());
        Assert.assertEquals(changedBy, version.getChangedBy());
        Assert.assertNotNull(version.getCreatedAt());
    }

    @Test
    public void testQueryExecutionStatus() {
        Assert.assertEquals(3, QueryExecutionStatus.values().length);
        Assert.assertEquals(QueryExecutionStatus.SUCCESS, QueryExecutionStatus.valueOf("SUCCESS"));
        Assert.assertEquals(QueryExecutionStatus.ERROR, QueryExecutionStatus.valueOf("ERROR"));
        Assert.assertEquals(QueryExecutionStatus.CANCELLED, QueryExecutionStatus.valueOf("CANCELLED"));
    }

    @Test
    public void testQueryExecutionWithError() {
        QueryExecution exec = new QueryExecution("e1", "q1", "u1", QueryExecutionStatus.ERROR);
        
        String errorMsg = "Syntax error near 'SELCT'";
        exec.setErrorMessage(errorMsg);
        
        Assert.assertEquals(QueryExecutionStatus.ERROR, exec.getStatus());
        Assert.assertEquals(errorMsg, exec.getErrorMessage());
    }

    @Test
    public void testSharedQueryTemplate() {
        SharedQuery query = new SharedQuery("id1", "Template Query", "SELECT * FROM {{table}}", "user1", QueryVisibility.PUBLIC);
        query.setTemplate(true);
        
        Assert.assertTrue(query.isTemplate());
        Assert.assertEquals(QueryVisibility.PUBLIC, query.getVisibility());
    }

    @Test
    public void testTeamQuery() {
        SharedQuery query = new SharedQuery("id1", "Team Query", "SELECT 1", "user1", QueryVisibility.TEAM);
        query.setTeamId("analytics-team");
        
        Assert.assertEquals(QueryVisibility.TEAM, query.getVisibility());
        Assert.assertEquals("analytics-team", query.getTeamId());
    }
}
