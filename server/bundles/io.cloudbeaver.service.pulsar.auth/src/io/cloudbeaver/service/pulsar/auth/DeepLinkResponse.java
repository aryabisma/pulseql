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

/**
 * Deep Link Response DTO
 * 
 * Response containing generated deep link
 */
public class DeepLinkResponse {
    
    private boolean success;
    private String deepLink;
    private String shortUrl;
    private String error;
    
    public DeepLinkResponse(boolean success) {
        this.success = success;
    }
    
    public static DeepLinkResponse success(@NotNull String deepLink, @Nullable String shortUrl) {
        DeepLinkResponse response = new DeepLinkResponse(true);
        response.deepLink = deepLink;
        response.shortUrl = shortUrl;
        return response;
    }
    
    public static DeepLinkResponse error(@NotNull String error) {
        DeepLinkResponse response = new DeepLinkResponse(false);
        response.error = error;
        return response;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    @Nullable
    public String getDeepLink() {
        return deepLink;
    }
    
    public void setDeepLink(@Nullable String deepLink) {
        this.deepLink = deepLink;
    }
    
    @Nullable
    public String getShortUrl() {
        return shortUrl;
    }
    
    public void setShortUrl(@Nullable String shortUrl) {
        this.shortUrl = shortUrl;
    }
    
    @Nullable
    public String getError() {
        return error;
    }
    
    public void setError(@Nullable String error) {
        this.error = error;
    }
    
    @Override
    public String toString() {
        return "DeepLinkResponse{" +
            "success=" + success +
            ", deepLink='" + deepLink + '\'' +
            ", shortUrl='" + shortUrl + '\'' +
            ", error='" + error + '\'' +
            '}';
    }
}
