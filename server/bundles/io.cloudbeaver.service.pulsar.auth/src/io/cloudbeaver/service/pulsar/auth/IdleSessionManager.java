/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import io.cloudbeaver.server.CBApplication;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Idle Session Manager
 * 
 * Scheduled task to detect and cleanup idle sessions
 */
public class IdleSessionManager {
    
    private static final Log log = Log.getLog(IdleSessionManager.class);
    
    // Default configuration
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
    private static final long DEFAULT_WARNING_THRESHOLD_MS = 13 * 60 * 1000; // 13 minutes
    private static final long DEFAULT_CHECK_INTERVAL_MS = 60 * 1000; // 1 minute
    
    private final SessionValidationService validationService;
    private final ScheduledExecutorService scheduler;
    private final long idleTimeoutMs;
    private final long warningThresholdMs;
    private final long checkIntervalMs;
    
    private volatile boolean running = false;
    
    public IdleSessionManager(@NotNull SessionValidationService validationService) {
        this.validationService = validationService;
        
        // Load configuration
        CBApplication app = CBApplication.getInstance();
        
        String idleTimeoutConfig = app.getAppConfiguration()
            .getConfigurationValue("pulsar.activity.idle-timeout-minutes");
        String warningConfig = app.getAppConfiguration()
            .getConfigurationValue("pulsar.activity.warning-minutes");
        String intervalConfig = app.getAppConfiguration()
            .getConfigurationValue("pulsar.activity.cleanup-interval-seconds");
        
        this.idleTimeoutMs = CommonUtils.isEmpty(idleTimeoutConfig) 
            ? DEFAULT_IDLE_TIMEOUT_MS 
            : Long.parseLong(idleTimeoutConfig) * 60 * 1000;
            
        this.warningThresholdMs = CommonUtils.isEmpty(warningConfig) 
            ? DEFAULT_WARNING_THRESHOLD_MS 
            : Long.parseLong(warningConfig) * 60 * 1000;
            
        this.checkIntervalMs = CommonUtils.isEmpty(intervalConfig) 
            ? DEFAULT_CHECK_INTERVAL_MS 
            : Long.parseLong(intervalConfig) * 1000;
        
        // Create scheduler with daemon thread
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("Idle-Session-Cleanup");
            return thread;
        });
        
        log.info("IdleSessionManager initialized. Timeout: " + (idleTimeoutMs / 60000) + 
            " minutes, Check interval: " + (checkIntervalMs / 1000) + " seconds");
    }
    
    /**
     * Start the idle session checker
     */
    public void start() {
        if (running) {
            log.warn("IdleSessionManager already running");
            return;
        }
        
        running = true;
        
        // Schedule periodic checks
        scheduler.scheduleAtFixedRate(
            this::checkIdleSessions,
            checkIntervalMs, // Initial delay
            checkIntervalMs, // Period
            TimeUnit.MILLISECONDS
        );
        
        log.info("IdleSessionManager started");
    }
    
    /**
     * Stop the idle session checker
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("IdleSessionManager stopped");
    }
    
    /**
     * Check for idle sessions and handle warnings/cleanup
     */
    private void checkIdleSessions() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Get sessions idle for more than warning threshold
            List<SessionValidationService.SessionActivity> idleSessions = 
                validationService.getIdleSessions(warningThresholdMs);
            
            int warningsSent = 0;
            int sessionsTerminated = 0;
            
            for (SessionValidationService.SessionActivity session : idleSessions) {
                long idleTime = currentTime - session.getLastActivityTime();
                
                // Check if session should be terminated
                if (idleTime >= idleTimeoutMs) {
                    terminateSession(session);
                    sessionsTerminated++;
                }
                // Check if warning should be sent
                else if (idleTime >= warningThresholdMs && !session.isWarningSent()) {
                    sendIdleWarning(session, idleTime);
                    warningsSent++;
                }
            }
            
            if (warningsSent > 0 || sessionsTerminated > 0) {
                log.info("Idle session check completed. Warnings sent: " + warningsSent + 
                    ", Sessions terminated: " + sessionsTerminated);
            } else {
                log.debug("Idle session check completed. No actions taken.");
            }
            
        } catch (Exception e) {
            log.error("Error during idle session check", e);
        }
    }
    
    /**
     * Send idle warning for a session
     */
    private void sendIdleWarning(
        @NotNull SessionValidationService.SessionActivity session,
        long idleTime
    ) {
        try {
            long timeUntilExpiry = idleTimeoutMs - idleTime;
            long minutesUntilExpiry = timeUntilExpiry / 60000;
            
            log.info("Idle warning for session " + session.getSessionId() + 
                ": " + minutesUntilExpiry + " minute(s) until expiry");
            
            // The warning is handled by SessionValidationService.validateActivity
            // This just logs the event for monitoring
            
            logAuditEvent(
                session.getSessionId(),
                session.getUserId(),
                "IDLE_WARNING",
                "Session idle for " + (idleTime / 60000) + " minutes. " +
                "Will expire in " + minutesUntilExpiry + " minute(s)."
            );
            
        } catch (Exception e) {
            log.error("Error sending idle warning", e);
        }
    }
    
    /**
     * Terminate an idle session
     */
    private void terminateSession(@NotNull SessionValidationService.SessionActivity session) {
        try {
            log.info("Terminating idle session: " + session.getSessionId() + 
                " (user: " + session.getUserId() + ")");
            
            // Remove session from activity tracking
            validationService.removeSession(session.getSessionId());
            
            // Note: Actual session termination in CloudBeaver should be handled
            // by the session manager. This just removes from activity tracking.
            
            logAuditEvent(
                session.getSessionId(),
                session.getUserId(),
                "SESSION_TERMINATED",
                "Session terminated due to " + (idleTimeoutMs / 60000) + " minutes of inactivity"
            );
            
        } catch (Exception e) {
            log.error("Error terminating session", e);
        }
    }
    
    /**
     * Log audit event
     */
    private void logAuditEvent(
        @NotNull String sessionId,
        @Nullable String userId,
        @NotNull String eventType,
        @NotNull String message
    ) {
        // Structured audit logging for monitoring systems
        if (log.isInfoEnabled()) {
            log.info(String.format(
                "{\"timestamp\":\"%s\",\"event_type\":\"%s\",\"session_id\":\"%s\"," +
                "\"user_id\":\"%s\",\"message\":\"%s\"}",
                new java.util.Date(),
                eventType,
                sessionId,
                userId != null ? userId : "unknown",
                message.replace("\"", "\\\"")
            ));
        }
    }
    
    /**
     * Get current status
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get configuration values (for monitoring)
     */
    public long getIdleTimeoutMs() {
        return idleTimeoutMs;
    }
    
    public long getWarningThresholdMs() {
        return warningThresholdMs;
    }
    
    public long getCheckIntervalMs() {
        return checkIntervalMs;
    }
}
