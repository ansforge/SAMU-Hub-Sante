package tnr;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpClientWithCache {

    private final String token;
    private final HttpClient httpClient;

    private static final Logger logger = LoggerFactory.getLogger(HttpClientWithCache.class);

    private final LoadingCache<String, String> cache;

    public HttpClientWithCache(String token) {
        this.token = token;
        this.httpClient = HttpClient.newHttpClient();

        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(100)
                .build(this::download);
    }

    public String fetch(String url) {
        logger.debug("Trying to reuse cache for {}", url);
        return cache.get(url);
    }

    private String download(String url) {
        try {
            logger.debug("Fetching uncached {}", url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 300) {
                throw new RuntimeException("HTTP error: " + response.statusCode());
            }

            return response.body();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }}
