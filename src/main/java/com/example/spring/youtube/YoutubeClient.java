package com.example.spring.youtube;

import com.example.spring.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class YoutubeClient {

    private final String apiKey;
    private final RestClient restClient;

    public YoutubeClient(AppProperties props, RestClient youtubeRestClient) {
        this.apiKey = props.getYoutube().getApiKey();
        this.restClient = youtubeRestClient;
    }

    /**
     * - videoId가 비거나 API 키가 없으면 empty
     * - 네트워크/파싱/쿼터/타임아웃 등 어떤 문제든 empty
     */
    public YoutubeMeta fetchMetaSafe(String videoId) {
        if (videoId == null || videoId.isBlank()) return YoutubeMeta.empty();
        if (apiKey == null || apiKey.isBlank()) return YoutubeMeta.empty();

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

            @SuppressWarnings("unchecked")
            Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
            @SuppressWarnings("unchecked")
            Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");

            String videoTitle = snippet == null ? null : (String) snippet.get("title");
            String channelTitle = snippet == null ? null : (String) snippet.get("channelTitle");

            String thumb = null;
            if (snippet != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> thumbs = (Map<String, Object>) snippet.get("thumbnails");
                if (thumbs != null) {
                    // high 우선, 없으면 medium/default 순으로 폴백
                    thumb = extractThumbUrl(thumbs, "high");
                    if (thumb == null) thumb = extractThumbUrl(thumbs, "medium");
                    if (thumb == null) thumb = extractThumbUrl(thumbs, "default");
                }
            }

            int durationSec = 0;
            if (contentDetails != null) {
                String duration = (String) contentDetails.get("duration"); // ISO8601: PT#M#S
                durationSec = Iso8601Duration.toSeconds(duration);
            }

            return new YoutubeMeta(videoTitle, channelTitle, durationSec, thumb);
        } catch (Exception e) {
            return YoutubeMeta.empty();
        }
    }

    /**
     * 호환용(Optional):
     * - Service가 Optional 기반으로 작성되어 있어도 깨지지 않게 제공
     */
    public Optional<YoutubeMeta> fetchMeta(String videoId) {
        YoutubeMeta meta = fetchMetaSafe(videoId);
        if (meta == null) return Optional.empty();
        if (meta.isEmpty()) return Optional.empty();
        return Optional.of(meta);
    }

    private String extractThumbUrl(Map<String, Object> thumbs, String key) {
        Object o = thumbs.get(key);
        if (!(o instanceof Map<?, ?> m)) return null;
        Object url = m.get("url");
        return url instanceof String s ? s : null;
    }

    public record YoutubeMeta(String videoTitle, String channelTitle, int durationSec, String thumbnailUrl) {
        public static YoutubeMeta empty() {
            return new YoutubeMeta(null, null, 0, null);
        }
        public boolean isEmpty() {
            return videoTitle == null && channelTitle == null && thumbnailUrl == null && durationSec == 0;
        }
    }
}
