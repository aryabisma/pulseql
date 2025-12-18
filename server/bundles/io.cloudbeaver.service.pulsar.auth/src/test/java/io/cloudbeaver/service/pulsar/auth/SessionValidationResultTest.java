/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
package io.cloudbeaver.service.pulsar.auth;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/**
 * Unit tests for SessionValidationResult model
 */
public class SessionValidationResultTest {
    
    @Test
    public void testSuccessResult() {
        SessionValidationResult result = SessionValidationResult.success(
            System.currentTimeMillis(),
            5000L,
            Arrays.asList("Warning: Session will expire soon")
        );
        
        assertTrue(result.isSuccess());
        assertTrue(result.isSessionValid());
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getWarnings().size());
    }
    
    @Test
    public void testInvalidSessionResult() {
        SessionValidationResult result = SessionValidationResult.invalidSession(
            "Session expired"
        );
        
        assertFalse(result.isSuccess());
        assertFalse(result.isSessionValid());
        assertEquals("Session expired", result.getErrorMessage());
    }
    
    @Test
    public void testFailureResult() {
        SessionValidationResult result = SessionValidationResult.failure(
            "Database error"
        );
        
        assertFalse(result.isSuccess());
        assertFalse(result.isSessionValid());
        assertEquals("Database error", result.getErrorMessage());
    }
    
    @Test
    public void testBuilderPattern() {
        SessionValidationResult result = SessionValidationResult.builder()
            .success(true)
            .sessionValid(true)
            .lastActivityTime(System.currentTimeMillis())
            .idleTimeMs(1000L)
            .addWarning("Warning 1")
            .addWarning("Warning 2")
            .build();
        
        assertTrue(result.isSuccess());
        assertTrue(result.isSessionValid());
        assertEquals(2, result.getWarnings().size());
    }
    
    @Test
    public void testBuilderWithError() {
        SessionValidationResult result = SessionValidationResult.builder()
            .success(false)
            .sessionValid(false)
            .errorMessage("Test error")
            .build();
        
        assertFalse(result.isSuccess());
        assertFalse(result.isSessionValid());
        assertEquals("Test error", result.getErrorMessage());
    }
}
