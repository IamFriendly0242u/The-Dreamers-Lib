package net.thedreamers.lib.webhook;

import java.util.ArrayList;
import java.util.List;

public class DiscordEmbedBuilder {

    private String title;
    private String description;
    private int color;
    private String thumbnail;
    private final List<Field> fields = new ArrayList<>();

    public static class Field {
        private final String name;
        private final String value;
        private final boolean inline;

        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }

    public DiscordEmbedBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public DiscordEmbedBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public DiscordEmbedBuilder setColor(int color) {
        this.color = color;
        return this;
    }

    public DiscordEmbedBuilder setThumbnail(String url) {
        this.thumbnail = url;
        return this;
    }

    public DiscordEmbedBuilder addField(String name, String value, boolean inline) {
        this.fields.add(new Field(name, value, inline));
        return this;
    }

    public String buildPayload(String username, String avatarUrl) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        if (username != null && !username.isEmpty()) {
            json.append("\"username\":\"").append(username).append("\",");
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            json.append("\"avatar_url\":\"").append(avatarUrl).append("\",");
        }

        json.append("\"embeds\":[{");
        boolean structural = false;

        if (title != null && !title.isEmpty()) {
            json.append("\"title\":\"").append(title).append("\"");
            structural = true;
        }
        if (description != null && !description.isEmpty()) {
            if (structural) json.append(",");
            json.append("\"description\":\"").append(description).append("\"");
            structural = true;
        }
        if (color != 0) {
            if (structural) json.append(",");
            json.append("\"color\":").append(color);
            structural = true;
        }
        if (thumbnail != null && !thumbnail.isEmpty()) {
            if (structural) json.append(",");
            json.append("\"thumbnail\":{\"url\":\"").append(thumbnail).append("\"}");
            structural = true;
        }
        if (!fields.isEmpty()) {
            if (structural) json.append(",");
            json.append("\"fields\":[");
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                json.append("{");
                json.append("\"name\":\"").append(field.name).append("\",");
                json.append("\"value\":\"").append(field.value).append("\",");
                json.append("\"inline\":").append(field.inline);
                json.append("}");
                if (i < fields.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");
        }

        json.append("}]}");
        return json.toString();
    }
}