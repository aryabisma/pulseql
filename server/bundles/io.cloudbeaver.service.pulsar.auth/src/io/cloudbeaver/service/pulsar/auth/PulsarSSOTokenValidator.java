/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.cloudbeaver.DBWebException;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.service.security.SMUtils;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pulsar SSO Token Validator
 * 
 * Validates JWT tokens from Pulsar application
 * Implements token blacklist and signature verification
 */
public class PulsarSSOTokenValidator {
    
    private static final Log log = Log.getLog(PulsarSSOTokenValidator.class);
    
    private static final String EXPECTED_ISSUER = "pulsar";
    private static final String EXPECTED_AUDIENCE = "pulseql";
    private static final long MAX_TOKEN_AGE_MS = 15 * 60 * 1000; // 15 minutes
    
    // Token blacklist - stores revoked token IDs
    private static final Map<String, Long> TOKEN_BLACKLIST = new ConcurrentHashMap<>();
    
    private final String signingSecret;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    
    public PulsarSSOTokenValidator(@NotNull String signingSecret) {
        this.signingSecret = signingSecret;
        this.algorithm = Algorithm.HMAC256(signingSecret);
        this.verifier = JWT.require(algorithm)
            .withIssuer(EXPECTED_ISSUER)
            .withAudience(EXPECTED_AUDIENCE)
            .build();
            
        // Start cleanup thread for expired blacklist entries
        startBlacklistCleanup();
    }
    
    /**
     * Validate JWT token and extract user information
     */
    @NotNull
    public PulsarSSOTokenPayload validateToken(@NotNull String token) throws DBWebException {
        try {
            // Verify signature and decode token
            DecodedJWT jwt = verifier.verify(token);
            
            // Check if token is blacklisted
            String jti = jwt.getId();
            if (jti != null && TOKEN_BLACKLIST.containsKey(jti)) {
                throw new DBWebException("Token has been revoked");
            }
            
            // Extract and validate claims
            String userId = jwt.getSubject();
            if (CommonUtils.isEmpty(userId)) {
                throw new DBWebException("Token missing user ID (sub claim)");
            }
            
            // Validate token age
            Date issuedAt = jwt.getIssuedAt();
            if (issuedAt != null) {
                long tokenAge = System.currentTimeMillis() - issuedAt.getTime();
                if (tokenAge > MAX_TOKEN_AGE_MS) {
                    throw new DBWebException("Token too old");
                }
            }
            
            // Extract user information from claims
            Map<String, Object> userClaim = jwt.getClaim("user").asMap();
            if (userClaim == null) {
                throw new DBWebException("Token missing user information");
            }
            
            String displayName = (String) userClaim.get("displayName");
            String email = (String) userClaim.get("email");
            String authRole = (String) userClaim.get("authRole");
            
            // Extract permissions
            List<String> permissions = jwt.getClaim("permissions").asList(String.class);
            if (permissions == null) {
                permissions = Collections.emptyList();
            }
            
            // Extract teams (optional)
            List<Map<String, Object>> teams = jwt.getClaim("teams").asList(Map.class);
            
            // Create and return payload
            PulsarSSOTokenPayload payload = new PulsarSSOTokenPayload(
                userId,
                displayName,
                email,
                authRole,
                permissions,
                teams,
                jti
            );
            
            log.debug("SSO token validated successfully for user: " + userId);
            
            return payload;
            
        } catch (JWTVerificationException e) {
            log.error("JWT verification failed", e);
            throw new DBWebException("Invalid SSO token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Token validation error", e);
            throw new DBWebException("Token validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Revoke a token by adding it to blacklist
     */
    public void revokeToken(@NotNull String jti) {
        TOKEN_BLACKLIST.put(jti, System.currentTimeMillis() + MAX_TOKEN_AGE_MS);
        log.info("Token revoked: " + jti);
    }
    
    /**
     * Check if token is revoked
     */
    public boolean isTokenRevoked(@NotNull String jti) {
        return TOKEN_BLACKLIST.containsKey(jti);
    }
    
    /**
     * Start background thread to cleanup expired blacklist entries
     */
    private void startBlacklistCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 60 * 1000); // Run every 5 minutes
                    
                    long now = System.currentTimeMillis();
                    TOKEN_BLACKLIST.entrySet().removeIf(entry -> entry.getValue() < now);
                    
                    log.debug("Blacklist cleanup completed. Size: " + TOKEN_BLACKLIST.size());
                    
                } catch (InterruptedException e) {
                    log.debug("Blacklist cleanup thread interrupted");
                    break;
                } catch (Exception e) {
                    log.error("Error in blacklist cleanup", e);
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("Pulsar-SSO-Blacklist-Cleanup");
        cleanupThread.start();
    }
    
    /**
     * Get current blacklist size (for monitoring)
     */
    public int getBlacklistSize() {
        return TOKEN_BLACKLIST.size();
    }
}
