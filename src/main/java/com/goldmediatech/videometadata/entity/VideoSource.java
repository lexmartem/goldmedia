package com.goldmediatech.videometadata.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the source platforms for video metadata.
 * 
 * This enum defines the supported video platforms that can be integrated
 * with the Video Metadata Service. Currently only Mock service is supported
 * for development and testing purposes.
 */
public enum VideoSource {
    MOCK("Mock");

    private final String displayName;

    VideoSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonValue
    public String getValue() {
        return name(); // Use the enum name (MOCK) for JSON serialization
    }

    @Override
    public String toString() {
        return name(); // Use the enum name (MOCK) for string conversion
    }
} 