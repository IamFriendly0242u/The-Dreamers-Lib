package net.thedreamers.lib.anticheat;

import net.thedreamers.lib.punishment.SuspensionEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiCheatCore {

    private final SuspensionEngine suspensionEngine;
    private final FlagEngine flagEngine;
    private final Map<UUID, Integer> violationTracker = new HashMap<>();

    public AntiCheatCore(SuspensionEngine suspensionEngine) {
        this.suspensionEngine = suspensionEngine;
        this.flagEngine = new FlagEngine();
    }

    public boolean scanModHandshake(String decryptedModList, String[] blacklist) {
        for (String cheat : blacklist) {
            if (!cheat.trim().isEmpty() && decryptedModList.toLowerCase().contains(cheat.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public int triggerViolationProgress(UUID playerUuid) {
        int nextStage = violationTracker.getOrDefault(playerUuid, 0) + 1;
        if (nextStage > 4) {
            nextStage = 4;
        }
        violationTracker.put(playerUuid, nextStage);
        return nextStage;
    }

    public long calculatePunishmentDuration(int stage) {
        return switch (stage) {
            case 1 -> 20L;
            case 2 -> 120L;
            case 3 -> 360L;
            case 4 -> -1L;
            default -> 0L;
        };
    }

    public void executeEnforcement(String username, String uuid, String ip, int stage, String reason) {
        long duration = calculatePunishmentDuration(stage);
        if (stage >= 1) {
            suspensionEngine.suspend(username, stage, duration, reason);
            suspensionEngine.suspend(uuid, stage, duration, reason);
            suspensionEngine.suspend(ip, stage, duration, reason);
        }
    }

    public void clearPlayerSession(UUID playerUuid) {
        violationTracker.remove(playerUuid);
        flagEngine.unflagPlayer(playerUuid);
    }

    public int getCurrentStage(UUID playerUuid) {
        return violationTracker.getOrDefault(playerUuid, 0);
    }

    public FlagEngine getFlagEngine() {
        return flagEngine;
    }
}