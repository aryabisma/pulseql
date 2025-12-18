/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.server.CBApplication;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session Validation Service
 * 
 * Handles session validation, activity tracking, and idle detection
 */
public class SessionValidationService {
    
    private static final Log log = Log.getLog(SessionValidationService.class);
    
    // Configuration constants
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
    private static final long DEFAULT_WARNING_THRESHOLD_MS = 13 * 60 * 1000; // 13 minutes
    
    // In-memory cache for session activity (for performance)
    private final Map<String, SessionActivity> sessionCache = new ConcurrentHashMap<>();
    
    private final long idleTimeoutMs;
    private final long warningThresholdMs;
    private final CBApplication application;
    
    public SessionValidationService(@NotNull CBApplication application) {
        this.application = application;
        
        // Load configuration from app settings
        String idleTimeoutConfig = application.getAppConfiguration()
            .getConfigurationValue("pulsar.activity.idle-timeout-minutes");
        String warningConfig = application.getAppConfiguration()
            .getConfigurationValue("pulsar.activity.warning-minutes");
        
        if (CommonUtils.isEmpty(idleTimeoutConfig)) {
            this.idleTimeoutMs = DEFAULT_IDLE_TIMEOUT_MS;
        } else {
            long idleTimeoutMinutes;
            try {
                idleTimeoutMinutes = Long.parseLong(idleTimeoutConfig);
            } catch (NumberFormatException e) {
                log.error("Invalid value for 'pulsar.activity.idle-timeout-minutes': '" + idleTimeoutConfig
                    + "'. Using default idle timeout " + (DEFAULT_IDLE_TIMEOUT_MS / 60000) + " minutes.", e);
                idleTimeoutMinutes = DEFAULT_IDLE_TIMEOUT_MS / (60 * 1000);
            }
            this.idleTimeoutMs = idleTimeoutMinutes * 60 * 1000;
        }

        if (CommonUtils.isEmpty(warningConfig)) {
            this.warningThresholdMs = DEFAULT_WARNING_THRESHOLD_MS;
        } else {
            long warningMinutes;
            try {
                warningMinutes = Long.parseLong(warningConfig);
            } catch (NumberFormatException e) {
                log.error("Invalid value for 'pulsar.activity.warning-minutes': '" + warningConfig
                    + "'. Using default warning threshold " + (DEFAULT_WARNING_THRESHOLD_MS / 60000) + " minutes.", e);
                warningMinutes = DEFAULT_WARNING_THRESHOLD_MS / (60 * 1000);
            }
            this.warningThresholdMs = warningMinutes * 60 * 1000;
        }
            
        log.info("SessionValidationService initialized. Idle timeout: " + (idleTimeoutMs / 60000) + " minutes");
    }
    
    /**
     * Validate activity event and update session
     */
    @NotNull
    public SessionValidationResult validateActivity(
        @NotNull String sessionId,
        @NotNull ActivityEvent event
    ) throws DBWebException {
        
        try {
            long currentTime = System.currentTimeMillis();
            
            // Get or create session activity
            SessionActivity activity = getOrCreateSessionActivity(sessionId, event.getUserId());
            
            // Check if session is expired
            long idleTime = currentTime - activity.lastActivityTime;
            if (idleTime > idleTimeoutMs) {
                log.warn("Session expired due to inactivity: " + sessionId);
                return SessionValidationResult.invalidSession("Session expired due to inactivity");
            }
            
            // Update activity timestamp
            updateSessionActivity(sessionId, currentTime, event.getUserId());
            
            // Build result with warnings if needed
            SessionValidationResult.Builder resultBuilder = SessionValidationResult.builder()
                .sessionValid(true)
                .lastActivityTime(currentTime)
                .idleTimeMs(idleTime); // Set actual idle time
            
            // Check if we should send a warning
            if (idleTime > warningThresholdMs && !activity.warningSent) {
                long timeUntilExpiry = idleTimeoutMs - idleTime;
                long minutesUntilExpiry = timeUntilExpiry / 60000;
                
                String warning = String.format(
                    "Session will expire in %d minute%s due to inactivity",
                    minutesUntilExpiry,
                    minutesUntilExpiry == 1 ? "" : "s"
                );
                
                resultBuilder.addWarning(warning);
                markWarningSent(sessionId, true);
            } else if (idleTime <= warningThresholdMs && activity.warningSent) {
                // Reset warning flag if activity resumed
                markWarningSent(sessionId, false);
            }
            
            // Store activity event in database
            storeActivityEvent(event);
            
            return resultBuilder.build();
            
        } catch (SQLException e) {
            log.error("Database error during activity validation", e);
            throw new DBWebException("Failed to validate activity: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error validating activity", e);
            throw new DBWebException("Activity validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Get or create session activity record
     */
    @NotNull
    private SessionActivity getOrCreateSessionActivity(
        @NotNull String sessionId,
        @Nullable String userId
    ) throws SQLException {
        
        // Check cache first
        SessionActivity cached = sessionCache.get(sessionId);
        if (cached != null) {
            return cached;
        }
        
        // Query database
        try (Connection conn = application.getDefaultDataSource().getConnection()) {
            String tablePrefix = application.getServerConfiguration().getDatabaseConfiguration().getSchema();
            if (CommonUtils.isEmpty(tablePrefix)) {
                tablePrefix = "";
            } else if (!tablePrefix.endsWith(".")) {
                tablePrefix = tablePrefix + ".";
            }
            
            String query = "SELECT SESSION_ID, USER_ID, LAST_ACTIVITY_TIME, WARNING_SENT " +
                "FROM " + tablePrefix + "CB_SESSION_ACTIVITY WHERE SESSION_ID = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, sessionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        SessionActivity activity = new SessionActivity(
                            rs.getString("SESSION_ID"),
                            rs.getString("USER_ID"),
                            rs.getLong("LAST_ACTIVITY_TIME"),
                            "Y".equals(rs.getString("WARNING_SENT"))
                        );
                        sessionCache.put(sessionId, activity);
                        return activity;
                    }
                }
            }
            
            // Create new record
            long currentTime = System.currentTimeMillis();
            String insertSql = "INSERT INTO " + tablePrefix + 
                "CB_SESSION_ACTIVITY (SESSION_ID, USER_ID, LAST_ACTIVITY_TIME, WARNING_SENT) " +
                "VALUES (?, ?, ?, 'N')";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, sessionId);
                stmt.setString(2, userId);
                stmt.setLong(3, currentTime);
                stmt.executeUpdate();
            }
            
            SessionActivity activity = new SessionActivity(sessionId, userId, currentTime, false);
            sessionCache.put(sessionId, activity);
            return activity;
        }
    }
    
    /**
     * Update session activity timestamp
     * 
     * NOTE: This method performs a database write on every activity validation.
     * For high-traffic scenarios, consider implementing:
     * - Write-through caching with periodic batch updates
     * - Async write queue to reduce blocking
     * - Conditional updates (only write if timestamp changed significantly)
     */
    private void updateSessionActivity(
        @NotNull String sessionId,
        long timestamp,
        @Nullable String userId
    ) throws SQLException {
        
        try (Connection conn = application.getDefaultDataSource().getConnection()) {
            String tablePrefix = getTablePrefix();
            
            String updateSql = "UPDATE " + tablePrefix + 
                "CB_SESSION_ACTIVITY SET LAST_ACTIVITY_TIME = ?, UPDATE_TIME = CURRENT_TIMESTAMP " +
                "WHERE SESSION_ID = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setLong(1, timestamp);
                stmt.setString(2, sessionId);
                stmt.executeUpdate();
            }
        }
        
        // Update cache with thread-safe operation
        sessionCache.computeIfPresent(sessionId, (key, activity) -> {
            activity.setLastActivityTime(timestamp);
            return activity;
        });
    }
    
    /**
     * Mark warning as sent or cleared
     */
    private void markWarningSent(@NotNull String sessionId, boolean sent) throws SQLException {
        try (Connection conn = application.getDefaultDataSource().getConnection()) {
            String tablePrefix = getTablePrefix();
            
            String updateSql = "UPDATE " + tablePrefix + 
                "CB_SESSION_ACTIVITY SET WARNING_SENT = ?, UPDATE_TIME = CURRENT_TIMESTAMP " +
                "WHERE SESSION_ID = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, sent ? "Y" : "N");
                stmt.setString(2, sessionId);
                stmt.executeUpdate();
            }
        }
        
        // Update cache with thread-safe operation
        sessionCache.computeIfPresent(sessionId, (key, activity) -> {
            activity.setWarningSent(sent);
            return activity;
        });
    }
    
    /**
     * Store activity event in database
     */
    private void storeActivityEvent(@NotNull ActivityEvent event) throws SQLException {
        try (Connection conn = application.getDefaultDataSource().getConnection()) {
            String tablePrefix = getTablePrefix();
            
            String insertSql = "INSERT INTO " + tablePrefix + 
                "CB_ACTIVITY_EVENTS (EVENT_ID, SESSION_ID, USER_ID, EVENT_TYPE, EVENT_TIMESTAMP, " +
                "DETAILS, IP_ADDRESS, USER_AGENT) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, event.getEventId());
                stmt.setString(2, event.getSessionId());
                stmt.setString(3, event.getUserId());
                stmt.setString(4, event.getType().name());
                stmt.setLong(5, event.getTimestamp());
                stmt.setString(6, truncate(event.getDetails(), 4096));
                stmt.setString(7, truncate(event.getIpAddress(), 45));
                stmt.setString(8, truncate(event.getUserAgent(), 512));
                stmt.executeUpdate();
            }
        }
        
        log.debug("Activity event stored: " + event.getType() + " for session " + event.getSessionId());
    }
    
    /**
     * Get list of idle sessions that need warnings or cleanup
     */
    @NotNull
    public List<SessionActivity> getIdleSessions(long idleThresholdMs) throws SQLException {
        List<SessionActivity> idleSessions = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - idleThresholdMs;
        
        try (Connection conn = application.getDefaultDataSource().getConnection()) {
            String tablePrefix = getTablePrefix();
            
            String query = "SELECT SESSION_ID, USER_ID, LAST_ACTIVITY_TIME, WARNING_SENT " +
                "FROM " + tablePrefix + "CB_SESSION_ACTIVITY " +
                "WHERE LAST_ACTIVITY_TIME < ? ORDER BY LAST_ACTIVITY_TIME";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setLong(1, cutoffTime);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        idleSessions.add(new SessionActivity(
                            rs.getString("SESSION_ID"),
                            rs.getString("USER_ID"),
                            rs.getLong("LAST_ACTIVITY_TIME"),
                            "Y".equals(rs.getString("WARNING_SENT"))
                        ));
                    }
                }
            }
        }
        
        return idleSessions;
    }
    
    /**
     * Remove session from activity tracking
     */
    public void removeSession(@NotNull String sessionId) throws SQLException {
        try (Connection conn = application.getDefaultDataSource().getConnection()) {
            String tablePrefix = getTablePrefix();
            
            String deleteSql = "DELETE FROM " + tablePrefix + "CB_SESSION_ACTIVITY WHERE SESSION_ID = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setString(1, sessionId);
                stmt.executeUpdate();
            }
        }
        
        sessionCache.remove(sessionId);
        log.debug("Session removed from activity tracking: " + sessionId);
    }
    
    /**
     * Get table prefix for queries
     */
    @NotNull
    private String getTablePrefix() {
        String tablePrefix = application.getServerConfiguration().getDatabaseConfiguration().getSchema();
        if (CommonUtils.isEmpty(tablePrefix)) {
            return "";
        }
        return tablePrefix.endsWith(".") ? tablePrefix : tablePrefix + ".";
    }
    
    /**
     * Truncate string to max length
     */
    @Nullable
    private String truncate(@Nullable String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
    
    /**
     * Session activity data structure with thread-safe mutable fields
     */
    public static class SessionActivity {
        public final String sessionId;
        public final String userId;
        private volatile long lastActivityTime;
        private volatile boolean warningSent;
        
        public SessionActivity(
            @NotNull String sessionId,
            @Nullable String userId,
            long lastActivityTime,
            boolean warningSent
        ) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.lastActivityTime = lastActivityTime;
            this.warningSent = warningSent;
        }
        
        @NotNull
        public String getSessionId() {
            return sessionId;
        }
        
        @Nullable
        public String getUserId() {
            return userId;
        }
        
        public synchronized long getLastActivityTime() {
            return lastActivityTime;
        }
        
        public synchronized void setLastActivityTime(long lastActivityTime) {
            this.lastActivityTime = lastActivityTime;
        }
        
        public synchronized boolean isWarningSent() {
            return warningSent;
        }
        
        public synchronized void setWarningSent(boolean warningSent) {
            this.warningSent = warningSent;
        }
    }
}
