/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Pulsar SSO Token Payload
 * 
 * Contains validated user information from JWT token
 */
public class PulsarSSOTokenPayload {
    
    private final String userId;
    private final String displayName;
    private final String email;
    private final String authRole;
    private final List<String> permissions;
    private final List<Map<String, Object>> teams;
    private final String tokenId;
    
    public PulsarSSOTokenPayload(
        @NotNull String userId,
        @Nullable String displayName,
        @Nullable String email,
        @Nullable String authRole,
        @NotNull List<String> permissions,
        @Nullable List<Map<String, Object>> teams,
        @Nullable String tokenId
    ) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.authRole = authRole;
        this.permissions = Collections.unmodifiableList(permissions);
        this.teams = teams != null ? Collections.unmodifiableList(teams) : Collections.emptyList();
        this.tokenId = tokenId;
    }
    
    @NotNull
    public String getUserId() {
        return userId;
    }
    
    @Nullable
    public String getDisplayName() {
        return displayName;
    }
    
    @Nullable
    public String getEmail() {
        return email;
    }
    
    @Nullable
    public String getAuthRole() {
        return authRole;
    }
    
    @NotNull
    public List<String> getPermissions() {
        return permissions;
    }
    
    @NotNull
    public List<Map<String, Object>> getTeams() {
        return teams;
    }
    
    @Nullable
    public String getTokenId() {
        return tokenId;
    }
    
    public boolean hasPermission(@NotNull String permission) {
        return permissions.contains(permission);
    }
    
    public boolean hasAnyPermission(@NotNull List<String> requiredPermissions) {
        for (String permission : requiredPermissions) {
            if (permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasAllPermissions(@NotNull List<String> requiredPermissions) {
        return permissions.containsAll(requiredPermissions);
    }
    
    @Override
    public String toString() {
        return "PulsarSSOTokenPayload{" +
            "userId='" + userId + '\'' +
            ", displayName='" + displayName + '\'' +
            ", email='" + email + '\'' +
            ", authRole='" + authRole + '\'' +
            ", permissions=" + permissions.size() +
            ", teams=" + teams.size() +
            '}';
    }
}
