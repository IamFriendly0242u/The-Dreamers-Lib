package net.thedreamers.lib.anticheat;

import net.thedreamers.lib.punishment.SuspensionEngine;
import net.thedreamers.lib.config.ConfigEngine;

public class AdminCommandCore {

    private final SuspensionEngine suspensionEngine;
    private final ConfigEngine configEngine;

    public AdminCommandCore(SuspensionEngine suspensionEngine, ConfigEngine configEngine) {
        this.suspensionEngine = suspensionEngine;
        this.configEngine = configEngine;
    }

    public boolean processAction(String target, String type, String reason) {
        if (target == null || target.trim().isEmpty()) return false;
        if ("BAN".equalsIgnoreCase(type)) {
            suspensionEngine.suspend(target, 4, -1, reason);
            return true;
        } else if ("KICK".equalsIgnoreCase(type)) {
            suspensionEngine.suspend(target, 1, 10, reason);
            return true;
        }
        return false;
    }

    public boolean processPardon(String target) {
        if (target == null || target.trim().isEmpty()) return false;
        suspensionEngine.pardon(target);
        return true;
    }

    public void processReload() {
        configEngine.load();
        suspensionEngine.load();
    }
}