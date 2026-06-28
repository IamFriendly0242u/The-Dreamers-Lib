package net.thedreamers.lib.punishment;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.thedreamers.lib.config.ConfigEngine;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuarantineEngine {

    private static final Map<UUID, QuarantineSession> SESSIONS = new ConcurrentHashMap<>();
    private static final File HISTORY_FILE = new File("config/TheDreamers_Guards/suspension-history.txt");
    private static final SuspensionEngine DATABASE = new SuspensionEngine(new File("config/TheDreamers_Guards/suspended-list.txt"));

    private static String cleanFormatting(String input) {
        if (input == null) return "";
        return input.replace("\\u00A7", "§").replace("&", "§").replace("\\n", "\n");
    }

    public static void startQuarantine(MinecraftServer server, ServerPlayer player, String reason, ConfigEngine config) {
        if (player == null || SESSIONS.containsKey(player.getUUID())) return;

        int timeout = Integer.parseInt(config.getProperty("total_timeout_seconds", "30"));
        SESSIONS.put(player.getUUID(), new QuarantineSession(player.getUUID(), player.getScoreboardName(), player.getIpAddress(), reason, timeout));

        String alertMsg = String.format(config.getProperty("alert_admin_message", ""), player.getScoreboardName(), reason);
        server.getPlayerList().getPlayers().stream()
                .filter(p -> server.getPlayerList().isOp(new NameAndId(p.getGameProfile().id(), p.getGameProfile().name())))
                .forEach(admin -> admin.sendSystemMessage(Component.literal(cleanFormatting(alertMsg))));
    }

    public static void tick(MinecraftServer server, ConfigEngine config, Object punishmentExecutorBridge) {
        if (SESSIONS.isEmpty()) return;

        for (UUID uuid : SESSIONS.keySet()) {
            QuarantineSession session = SESSIONS.get(uuid);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);

            if (player == null) {
                SESSIONS.remove(uuid);
                continue;
            }

            session.tickCounter++;
            if (session.tickCounter >= 20) {
                session.tickCounter = 0;
                session.remainingSeconds--;

                int remaining = session.remainingSeconds;

                if (remaining == 5) {
                    server.execute(() -> {
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 255, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 255, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 255, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 255, false, false));
                    });
                }

                if (remaining == 3) {
                    String broadcastMsg = String.format(config.getProperty("countdown_broadcast_message", ""), session.playerName, remaining);
                    server.getPlayerList().broadcastSystemMessage(Component.literal(cleanFormatting(broadcastMsg)), false);
                    server.execute(() -> {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    });
                }

                if (remaining <= 0) {
                    SESSIONS.remove(uuid);
                    server.execute(() -> executeFinalPunishment(server, player, session.reason, session.ipAddress, config));
                }
            }
        }
    }

    private static void executeFinalPunishment(MinecraftServer server, ServerPlayer player, String reason, String ip, ConfigEngine config) {
        String name = player.getScoreboardName();
        String uuidStr = player.getUUID().toString();
        int phase = getAndUpdateHistoryPhase(name);

        int durationMins;
        switch (phase) {
            case 1 -> durationMins = Integer.parseInt(config.getProperty("suspension_phase_1_mins", "20"));
            case 2 -> durationMins = Integer.parseInt(config.getProperty("suspension_phase_2_mins", "120"));
            case 3 -> durationMins = Integer.parseInt(config.getProperty("suspension_phase_3_mins", "360"));
            default -> durationMins = Integer.parseInt(config.getProperty("suspension_phase_4_mins", "-1"));
        }

        String phaseTag = durationMins == -1 ? "PHASE 4 (TERMINATED)" : "PHASE " + phase + " / 3";
        String punishmentMode = config.getProperty("punishment_mode", "KICK");
        String finalReason = reason + " [" + phaseTag + "]";

        DATABASE.load();
        DATABASE.suspend(uuidStr, phase, durationMins, finalReason);
        DATABASE.suspend(ip, phase, durationMins, finalReason);

        String action = (durationMins == -1 || "BAN".equalsIgnoreCase(punishmentMode)) ? "BAN" : "KICK";
        executeKickOrBan(server, player, finalReason, action, config);
    }

    private static int getAndUpdateHistoryPhase(String name) {
        Properties props = new Properties();
        if (HISTORY_FILE.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(HISTORY_FILE), java.nio.charset.StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) { e.printStackTrace(); }
        } else {
            HISTORY_FILE.getParentFile().mkdirs();
        }

        int nextPhase = Integer.parseInt(props.getProperty(name, "0")) + 1;
        props.setProperty(name, String.valueOf(nextPhase));

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(HISTORY_FILE), java.nio.charset.StandardCharsets.UTF_8)) {
            props.store(writer, "The Dreamers Guards Infraction History");
        } catch (IOException e) { e.printStackTrace(); }

        return nextPhase;
    }

    private static void executeKickOrBan(MinecraftServer server, ServerPlayer player, String fullReason, String action, ConfigEngine config) {
        try {
            Class<?> clazz = Class.forName("net.thedreamers.guards.punishment.PunishmentExecutor");
            java.lang.reflect.Method method = clazz.getMethod("execute", MinecraftServer.class, ServerPlayer.class, String.class, String.class);
            method.invoke(null, server, player, fullReason, action);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class QuarantineSession {
        final UUID uuid;
        final String playerName;
        final String ipAddress;
        final String reason;
        int remainingSeconds;
        int tickCounter = 0;

        QuarantineSession(UUID uuid, String name, String ip, String reason, int timeout) {
            this.uuid = uuid;
            this.playerName = name;
            this.ipAddress = ip;
            this.reason = reason;
            this.remainingSeconds = timeout;
        }
    }
}