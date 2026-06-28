package net.thedreamers.lib.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private final String url;

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void send(String jsonPayload) {
        if (this.url == null || this.url.trim().isEmpty() || this.url.equals("YOUR_WEBHOOK_URL_HERE")) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.url.trim()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                        .build();
                CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}