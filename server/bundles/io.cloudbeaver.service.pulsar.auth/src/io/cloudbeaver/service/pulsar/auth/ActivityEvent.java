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
import org.jkiss.utils.CommonUtils;

import java.util.UUID;

/**
 * Activity Event Model
 * 
 * Represents a user activity event from PulseQL
 */
public class ActivityEvent {
    
    /**
     * Activity event types
     */
    public enum EventType {
        QUERY,       // SQL query execution
        NAVIGATION,  // UI navigation
        EXPORT,      // Data export
        CONNECTION,  // Connection opened/closed
        IDLE,        // User became idle
        ACTIVE       // User became active
    }
    
    private final String eventId;
    private final String sessionId;
    private final String userId;
    private final EventType type;
    private final long timestamp;
    private final String details;
    private final String ipAddress;
    private final String userAgent;
    
    public ActivityEvent(
        @Nullable String eventId,
        @NotNull String sessionId,
        @Nullable String userId,
        @NotNull EventType type,
        long timestamp,
        @Nullable String details,
        @Nullable String ipAddress,
        @Nullable String userAgent
    ) {
        this.eventId = CommonUtils.isEmpty(eventId) ? UUID.randomUUID().toString() : eventId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.type = type;
        this.timestamp = timestamp;
        this.details = details;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
    
    @NotNull
    public String getEventId() {
        return eventId;
    }
    
    @NotNull
    public String getSessionId() {
        return sessionId;
    }
    
    @Nullable
    public String getUserId() {
        return userId;
    }
    
    @NotNull
    public EventType getType() {
        return type;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Nullable
    public String getDetails() {
        return details;
    }
    
    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }
    
    @Nullable
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Parse event type from string
     */
    @NotNull
    public static EventType parseEventType(@NotNull String typeStr) {
        try {
            return EventType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to NAVIGATION for unknown types
            return EventType.NAVIGATION;
        }
    }
    
    @Override
    public String toString() {
        return "ActivityEvent{" +
            "eventId='" + eventId + '\'' +
            ", sessionId='" + sessionId + '\'' +
            ", userId='" + userId + '\'' +
            ", type=" + type +
            ", timestamp=" + timestamp +
            '}';
    }
}
