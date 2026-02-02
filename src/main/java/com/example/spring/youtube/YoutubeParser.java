package com.example.spring.youtube;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeParser {
    private YoutubeParser() {}

    private static final Pattern[] PATTERNS = new Pattern[] {
            Pattern.compile("v=([a-zA-Z0-9_-]{6,})"),
            Pattern.compile("youtu\\.be/([a-zA-Z0-9_-]{6,})"),
            Pattern.compile("shorts/([a-zA-Z0-9_-]{6,})"),
            Pattern.compile("^([a-zA-Z0-9_-]{6,})$") // 그냥 videoId 입력
    };

    public static String extractVideoId(String input) {
        if (input == null) return null;
        String s = input.trim();
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(s);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    public static String toWatchUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }
}
