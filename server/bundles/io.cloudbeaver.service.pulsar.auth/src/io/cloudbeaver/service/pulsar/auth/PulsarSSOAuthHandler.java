/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.model.user.WebUser;
import io.cloudbeaver.server.CBApplication;
import io.cloudbeaver.service.security.SMUtils;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.security.SMAdminController;
import org.jkiss.dbeaver.model.security.SMAuthStatus;
import org.jkiss.dbeaver.model.security.user.SMUser;
import org.jkiss.utils.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Pulsar SSO Authentication Handler
 * 
 * Handles SSO authentication flow for Pulsar integration
 */
public class PulsarSSOAuthHandler {
    
    private static final Log log = Log.getLog(PulsarSSOAuthHandler.class);
    
    private final PulsarSSOTokenValidator tokenValidator;
    private final SMAdminController securityController;
    
    public PulsarSSOAuthHandler(@NotNull PulsarSSOTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
        this.securityController = CBApplication.getInstance().getSecurityController();
    }
    
    /**
     * Authenticate user via SSO token
     * 
     * @param session Current web session
     * @param token SSO token from URL parameter
     * @return WebUser object if authentication successful
     * @throws DBWebException if authentication fails
     */
    @NotNull
    public WebUser authenticateWithSSO(
        @NotNull WebSession session,
        @NotNull String token
    ) throws DBWebException {
        
        try {
            // Validate token and extract payload
            PulsarSSOTokenPayload payload = tokenValidator.validateToken(token);
            
            log.info("SSO authentication attempt for user: " + payload.getUserId());
            
            // Get or create user
            SMUser smUser = getOrCreateUser(payload);
            
            // Update user permissions
            updateUserPermissions(smUser, payload);
            
            // Create web session user
            WebUser webUser = new WebUser(smUser);
            
            // Set session authentication
            session.setUser(webUser);
            session.setAuthStatus(SMAuthStatus.SUCCESS);
            
            // Store SSO metadata in session
            session.setAttribute("pulsar_sso_user_id", payload.getUserId());
            session.setAttribute("pulsar_sso_role", payload.getAuthRole());
            session.setAttribute("pulsar_sso_token_id", payload.getTokenId());
            
            log.info("SSO authentication successful for user: " + payload.getUserId());
            
            return webUser;
            
        } catch (DBWebException e) {
            log.error("SSO authentication failed", e);
            throw e;
        } catch (Exception e) {
            log.error("SSO authentication error", e);
            throw new DBWebException("SSO authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Get existing user or create new one
     */
    @NotNull
    private SMUser getOrCreateUser(@NotNull PulsarSSOTokenPayload payload) throws DBWebException {
        try {
            String userId = payload.getUserId();
            
            // Try to find existing user
            SMUser user = securityController.getUserById(userId);
            
            if (user == null) {
                // Create new user
                log.info("Creating new user from SSO: " + userId);
                
                user = securityController.createUser(
                    userId,
                    Collections.emptyMap()
                );
                
                // Update user profile
                if (!CommonUtils.isEmpty(payload.getDisplayName())) {
                    securityController.setUserMetaParameter(
                        userId,
                        "displayName",
                        payload.getDisplayName()
                    );
                }
                
                if (!CommonUtils.isEmpty(payload.getEmail())) {
                    securityController.setUserMetaParameter(
                        userId,
                        "email",
                        payload.getEmail()
                    );
                }
            } else {
                log.debug("Found existing user: " + userId);
                
                // Update user info if changed
                updateUserInfo(user, payload);
            }
            
            // Ensure user is enabled
            if (!user.isEnabled()) {
                securityController.enableUser(userId, true);
            }
            
            return user;
            
        } catch (Exception e) {
            throw new DBWebException("Failed to get or create user: " + e.getMessage());
        }
    }
    
    /**
     * Update user information from SSO payload
     */
    private void updateUserInfo(@NotNull SMUser user, @NotNull PulsarSSOTokenPayload payload) {
        try {
            String userId = user.getUserId();
            
            if (!CommonUtils.isEmpty(payload.getDisplayName())) {
                String currentName = securityController.getUserMetaParameter(userId, "displayName");
                if (!payload.getDisplayName().equals(currentName)) {
                    securityController.setUserMetaParameter(userId, "displayName", payload.getDisplayName());
                }
            }
            
            if (!CommonUtils.isEmpty(payload.getEmail())) {
                String currentEmail = securityController.getUserMetaParameter(userId, "email");
                if (!payload.getEmail().equals(currentEmail)) {
                    securityController.setUserMetaParameter(userId, "email", payload.getEmail());
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to update user info", e);
        }
    }
    
    /**
     * Update user permissions based on SSO token
     */
    private void updateUserPermissions(@NotNull SMUser user, @NotNull PulsarSSOTokenPayload payload) {
        try {
            // Store permissions in user metadata for application to use
            String permissionsJson = String.join(",", payload.getPermissions());
            securityController.setUserMetaParameter(
                user.getUserId(),
                "pulsar_permissions",
                permissionsJson
            );
            
            // Store role
            if (!CommonUtils.isEmpty(payload.getAuthRole())) {
                securityController.setUserMetaParameter(
                    user.getUserId(),
                    "pulsar_role",
                    payload.getAuthRole()
                );
            }
            
            log.debug("Updated permissions for user: " + user.getUserId());
            
        } catch (Exception e) {
            log.warn("Failed to update user permissions", e);
        }
    }
    
    /**
     * Logout and revoke SSO token
     */
    public void logout(@NotNull WebSession session) {
        try {
            String tokenId = (String) session.getAttribute("pulsar_sso_token_id");
            if (!CommonUtils.isEmpty(tokenId)) {
                tokenValidator.revokeToken(tokenId);
                log.info("SSO token revoked: " + tokenId);
            }
            
            session.removeAttribute("pulsar_sso_user_id");
            session.removeAttribute("pulsar_sso_role");
            session.removeAttribute("pulsar_sso_token_id");
            
        } catch (Exception e) {
            log.error("Error during SSO logout", e);
        }
    }
}
