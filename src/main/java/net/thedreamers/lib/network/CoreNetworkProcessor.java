package net.thedreamers.lib.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.thedreamers.lib.anticheat.AntiCheatCore;
import net.thedreamers.lib.config.ConfigEngine;
import net.thedreamers.lib.punishment.QuarantineEngine;
import net.thedreamers.lib.security.SecurityUtils;
import java.io.File;

public class CoreNetworkProcessor {

    private static final AntiCheatCore ANTI_CHEAT_CORE = new AntiCheatCore(
            new net.thedreamers.lib.punishment.SuspensionEngine(new File("config/The Dreamers Guards/suspended-list.txt"))
    );

    public static void handleIncomingHandshake(MinecraftServer server, ServerPlayer player, String clientVer, String encToken, String encMods, ConfigEngine config) {
        String token = SecurityUtils.decrypt(encToken);
        String modList = SecurityUtils.decrypt(encMods);

        String name = player.getScoreboardName();
        String uuid = player.getUUID().toString();

        try {
            Class<?> webhookClass = Class.forName("net.thedreamers.guards.webhook.DiscordWebhook");
            java.lang.reflect.Method sendVerify = webhookClass.getMethod("sendVerifyAlert", MinecraftServer.class, String.class, String.class, String.class, String.class);

            String serverVer = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer("thedreamers_guards")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("1.0.0");

            if (!clientVer.equals(serverVer)) {
                player.connection.disconnect(Component.literal(String.format(config.getProperty("kick_version_mismatch_message", ""), serverVer, clientVer)));
                sendVerify.invoke(null, server, name, uuid, "FAILED", "Version Mismatch");
                return;
            }

            if ("DIRTY_CHEATER".equals(token)) {
                QuarantineEngine.startQuarantine(server, player, "Illegal Modifications", config);
                sendVerify.invoke(null, server, name, uuid, "FAILED", "Client Scanner Flagged");
                return;
            }

            String[] blacklist = config.getProperty("mod_blacklist", "").split(",");
            if (ANTI_CHEAT_CORE.scanModHandshake(modList, blacklist)) {
                QuarantineEngine.startQuarantine(server, player, "Blacklisted Modification Signature Found", config);
                sendVerify.invoke(null, server, name, uuid, "FAILED", "Blacklisted Mod Detected");
                return;
            }

            sendVerify.invoke(null, server, name, uuid, "SUCCESS", "Passed secure authentication");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}