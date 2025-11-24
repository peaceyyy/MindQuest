package com.mindquest.loader.source;

/**
 * Small helper to record whether the last Gemini attempt used a fallback
 * (cached JSON or hardcoded questions). This is intentionally minimal
 * and thread-safe for simple diagnostic display in the UI.
 */
public class GeminiFallbackStatus {
    private static volatile boolean fallbackUsed = false;
    private static volatile String fallbackSource = "";

    public static synchronized void setFallback(String source) {
        fallbackUsed = true;
        fallbackSource = source == null ? "" : source;
    }

    public static synchronized void clear() {
        fallbackUsed = false;
        fallbackSource = "";
    }

    public static boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public static String getFallbackSource() {
        return fallbackSource;
    }
}
