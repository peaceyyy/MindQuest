package com.mindquest.server.util;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for network connectivity testing.
 */
public final class NetworkUtils {

    private NetworkUtils() {
        // Utility class - no instantiation
    }

    /**
     * Test HTTP/HTTPS connectivity to a URL.
     * 
     * @param urlString The URL to test
     * @param timeoutMs Connection and read timeout in milliseconds
     * @return A map containing test results (success, responseCode, elapsedMs, etc.)
     */
    public static Map<String, Object> testHttpConnection(String urlString, int timeoutMs) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", urlString);
        
        try {
            long start = System.currentTimeMillis();
            URL url = new URL(urlString);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            long elapsed = System.currentTimeMillis() - start;
            
            result.put("success", true);
            result.put("responseCode", responseCode);
            result.put("elapsedMs", elapsed);
            result.put("cipher", conn.getCipherSuite());
            
            conn.disconnect();
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            
            // Get root cause
            Throwable cause = e.getCause();
            if (cause != null) {
                result.put("cause", cause.getClass().getSimpleName() + ": " + cause.getMessage());
            }
        }
        
        return result;
    }
}
