package com.example.spring.youtube;

import com.example.spring.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class YoutubeClient {

    private final String apiKey;
    private final RestClient restClient = RestClient.create();

    public YoutubeClient(AppProperties props) {
        this.apiKey = props.getYoutube().getApiKey();
    }

    /** 메타 조회 실패해도 예외 X, empty 반환(발표 안정형) */
    public YoutubeMeta fetchMetaSafe(String videoId) {
        if (videoId == null || videoId.isBlank()) return YoutubeMeta.empty();
        if (apiKey == null || apiKey.isBlank()) return YoutubeMeta.empty(); // 키 없으면 스킵

        try {
            String url = "https://www.googleapis.com/youtube/v3/videos"
                    + "?part=snippet,contentDetails"
                    + "&id=" + videoId
                    + "&key=" + apiKey;

            Map<?, ?> res = restClient.get().uri(url).retrieve().body(Map.class);
            if (res == null) return YoutubeMeta.empty();

            Object itemsObj = res.get("items");
            if (!(itemsObj instanceof List<?> items) || items.isEmpty()) return YoutubeMeta.empty();

            Object firstObj = items.get(0);
            if (!(firstObj instanceof Map<?, ?> item)) return YoutubeMeta.empty();

            Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
            Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");

            String channelTitle = snippet == null ? null : (String) snippet.get("channelTitle");

            String thumb = null;
            if (snippet != null) {
                Map<String, Object> thumbs = (Map<String, Object>) snippet.get("thumbnails");
                if (thumbs != null) {
                    Map<String, Object> high = (Map<String, Object>) thumbs.get("high");
                    if (high != null) thumb = (String) high.get("url");
                }
            }

            int durationSec = 0;
            if (contentDetails != null) {
                String duration = (String) contentDetails.get("duration"); // ISO8601: PT#M#S
                durationSec = Iso8601Duration.toSeconds(duration);
            }

            return new YoutubeMeta(channelTitle, durationSec, thumb);
        } catch (Exception e) {
            // 어떤 문제가 나도 발표 중 끊기지 않게 empty로 처리
            return YoutubeMeta.empty();
        }
    }

    public record YoutubeMeta(String channelTitle, int durationSec, String thumbnailUrl) {
        public static YoutubeMeta empty() {
            return new YoutubeMeta(null, 0, null);
        }
    }
}
