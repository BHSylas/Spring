package com.example.spring.youtube;

public class Iso8601Duration {
    private Iso8601Duration() {}

    // "PT1H2M3S" -> seconds
    public static int toSeconds(String iso) {
        if (iso == null || iso.isBlank()) return 0;
        try {
            java.time.Duration d = java.time.Duration.parse(iso);
            long s = d.getSeconds();
            if (s < 0) return 0;
            if (s > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            return (int) s;
        } catch (Exception e) {
            return 0;
        }
    }
}
