/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ActivityEvent model
 */
public class ActivityEventTest {
    
    @Test
    public void testActivityEventCreation() {
        ActivityEvent event = new ActivityEvent(
            "test-event-id",
            "test-session-id",
            "test-user-id",
            ActivityEvent.EventType.QUERY,
            System.currentTimeMillis(),
            "{\"query_id\":\"q123\"}",
            "192.168.1.100",
            "Mozilla/5.0"
        );
        
        assertNotNull(event);
        assertEquals("test-event-id", event.getEventId());
        assertEquals("test-session-id", event.getSessionId());
        assertEquals("test-user-id", event.getUserId());
        assertEquals(ActivityEvent.EventType.QUERY, event.getType());
        assertNotNull(event.getDetails());
    }
    
    @Test
    public void testActivityEventWithNullEventId() {
        ActivityEvent event = new ActivityEvent(
            null,
            "test-session-id",
            "test-user-id",
            ActivityEvent.EventType.NAVIGATION,
            System.currentTimeMillis(),
            null,
            null,
            null
        );
        
        assertNotNull(event.getEventId()); // Should be auto-generated
        assertTrue(event.getEventId().length() > 0);
    }
    
    @Test
    public void testEventTypeParsing() {
        assertEquals(ActivityEvent.EventType.QUERY, ActivityEvent.parseEventType("query"));
        assertEquals(ActivityEvent.EventType.QUERY, ActivityEvent.parseEventType("QUERY"));
        assertEquals(ActivityEvent.EventType.NAVIGATION, ActivityEvent.parseEventType("navigation"));
        assertEquals(ActivityEvent.EventType.IDLE, ActivityEvent.parseEventType("idle"));
        assertEquals(ActivityEvent.EventType.ACTIVE, ActivityEvent.parseEventType("active"));
    }
    
    @Test
    public void testEventTypeParsingInvalid() {
        // Should default to NAVIGATION for unknown types
        assertEquals(ActivityEvent.EventType.NAVIGATION, ActivityEvent.parseEventType("unknown"));
        assertEquals(ActivityEvent.EventType.NAVIGATION, ActivityEvent.parseEventType("invalid"));
    }
}
