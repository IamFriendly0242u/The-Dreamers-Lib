package net.thedreamers.lib.punishment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class SuspensionEngine {

    private final File storageFile;
    private final Map<String, String> suspensionMap = new ConcurrentHashMap<>();

    public SuspensionEngine(File storageFile) {
        this.storageFile = storageFile;
    }

    public void load() {
        if (!storageFile.exists()) {
            try {
                File parent = storageFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                storageFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        suspensionMap.clear();
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(storageFile), StandardCharsets.UTF_8)) {
            props.load(reader);
            for (String key : props.stringPropertyNames()) {
                suspensionMap.put(key.toLowerCase(), props.getProperty(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void suspend(String identifier, int stage, long durationMinutes, String reason) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return;
        }
        long expiry = durationMinutes == -1 ? -1 : System.currentTimeMillis() + (durationMinutes * 60 * 1000);
        String data = stage + "," + expiry + "," + reason;
        suspensionMap.put(identifier.trim().toLowerCase(), data);
        save();
    }

    public void pardon(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return;
        }
        if (suspensionMap.remove(identifier.trim().toLowerCase()) != null) {
            save();
        }
    }

    public boolean isSuspended(String... identifiers) {
        long now = System.currentTimeMillis();
        for (String id : identifiers) {
            if (id == null) continue;
            String data = suspensionMap.get(id.trim().toLowerCase());
            if (data != null) {
                String[] parts = data.split(",", 3);
                if (parts.length >= 2) {
                    long expiry = Long.parseLong(parts[1]);
                    if (expiry == -1 || expiry > now) {
                        return true;
                    } else {
                        suspensionMap.remove(id.trim().toLowerCase());
                        save();
                    }
                }
            }
        }
        return false;
    }

    public int getStage(String identifier) {
        if (identifier == null) return 0;
        String data = suspensionMap.get(identifier.trim().toLowerCase());
        if (data != null) {
            String[] parts = data.split(",", 3);
            try {
                return Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private void save() {
        Properties props = new Properties();
        for (Map.Entry<String, String> entry : suspensionMap.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(storageFile), StandardCharsets.UTF_8)) {
            props.store(writer, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}