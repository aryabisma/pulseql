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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Session Validation Result
 * 
 * Contains the result of session validation including warnings and status
 */
public class SessionValidationResult {
    
    private final boolean success;
    private final boolean sessionValid;
    private final long lastActivityTime;
    private final long idleTimeMs;
    private final List<String> warnings;
    private final String errorMessage;
    
    private SessionValidationResult(
        boolean success,
        boolean sessionValid,
        long lastActivityTime,
        long idleTimeMs,
        @Nullable List<String> warnings,
        @Nullable String errorMessage
    ) {
        this.success = success;
        this.sessionValid = sessionValid;
        this.lastActivityTime = lastActivityTime;
        this.idleTimeMs = idleTimeMs;
        this.warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
        this.errorMessage = errorMessage;
    }
    
    /**
     * Create a success result
     */
    @NotNull
    public static SessionValidationResult success(
        long lastActivityTime,
        long idleTimeMs,
        @Nullable List<String> warnings
    ) {
        return new SessionValidationResult(
            true,
            true,
            lastActivityTime,
            idleTimeMs,
            warnings,
            null
        );
    }
    
    /**
     * Create an invalid session result
     */
    @NotNull
    public static SessionValidationResult invalidSession(@NotNull String errorMessage) {
        return new SessionValidationResult(
            false,
            false,
            0,
            0,
            null,
            errorMessage
        );
    }
    
    /**
     * Create a failure result
     */
    @NotNull
    public static SessionValidationResult failure(@NotNull String errorMessage) {
        return new SessionValidationResult(
            false,
            false,
            0,
            0,
            null,
            errorMessage
        );
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isSessionValid() {
        return sessionValid;
    }
    
    public long getLastActivityTime() {
        return lastActivityTime;
    }
    
    public long getIdleTimeMs() {
        return idleTimeMs;
    }
    
    @NotNull
    public List<String> getWarnings() {
        return warnings;
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Add a warning to the result
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for SessionValidationResult
     */
    public static class Builder {
        private boolean success = true;
        private boolean sessionValid = true;
        private long lastActivityTime = System.currentTimeMillis();
        private long idleTimeMs = 0;
        private final List<String> warnings = new ArrayList<>();
        private String errorMessage = null;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder sessionValid(boolean sessionValid) {
            this.sessionValid = sessionValid;
            return this;
        }
        
        public Builder lastActivityTime(long lastActivityTime) {
            this.lastActivityTime = lastActivityTime;
            return this;
        }
        
        public Builder idleTimeMs(long idleTimeMs) {
            this.idleTimeMs = idleTimeMs;
            return this;
        }
        
        public Builder addWarning(@NotNull String warning) {
            this.warnings.add(warning);
            return this;
        }
        
        public Builder errorMessage(@Nullable String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        @NotNull
        public SessionValidationResult build() {
            return new SessionValidationResult(
                success,
                sessionValid,
                lastActivityTime,
                idleTimeMs,
                warnings,
                errorMessage
            );
        }
    }
    
    @Override
    public String toString() {
        return "SessionValidationResult{" +
            "success=" + success +
            ", sessionValid=" + sessionValid +
            ", lastActivityTime=" + lastActivityTime +
            ", idleTimeMs=" + idleTimeMs +
            ", warnings=" + warnings.size() +
            ", errorMessage='" + errorMessage + '\'' +
            '}';
    }
}
