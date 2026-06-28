package net.thedreamers.lib.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.thedreamers.lib.security.SecurityUtils;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigEngine {

    private final File rootDir;
    private final File backupDir;
    private final File embedsDir;
    private final File languageDir;
    private final File configFile;
    private File langFile;
    private final Properties configProps = new Properties();
    private final JsonObject langJson = new JsonObject();

    public ConfigEngine(String baseDirPath) {
        this.rootDir = new File(baseDirPath, "TheDreamers_Guards");
        this.backupDir = new File(rootDir, "backups");
        this.embedsDir = new File(rootDir, "embeds");
        this.languageDir = new File(rootDir, "language");
        this.configFile = new File(rootDir, "thedreamers_guards.toml");
    }

    public String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("thedreamers_guards")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("2.0.1-Release+26.1.2");
    }

    public void load() {
        try {
            if (!rootDir.exists()) rootDir.mkdirs();
            if (!backupDir.exists()) backupDir.mkdirs();
            if (!embedsDir.exists()) embedsDir.mkdirs();
            if (!languageDir.exists()) languageDir.mkdirs();

            String oldVersion = null;
            if (configFile.exists()) {
                Properties testProps = new Properties();
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                    testProps.load(reader);
                    oldVersion = testProps.getProperty("configVersion");
                    if (oldVersion != null && oldVersion.startsWith("\"") && oldVersion.endsWith("\"")) {
                        oldVersion = oldVersion.substring(1, oldVersion.length() - 1);
                    }
                }
            }

            if (configFile.exists()) {
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                    configProps.load(reader);
                    sanitizeProperties();
                }
            }

            String currentVersion = getModVersion();
            if (oldVersion != null && !oldVersion.equals(currentVersion)) {
                triggerIndexedBackup();
                if (oldVersion.startsWith("1.") && currentVersion.startsWith("2.")) {
                    System.out.println("[The Dreamers Guards] CRITICAL CONFIG MIGRATION NOTICE: Server administrators must delete the old 'thedreamers_guards.toml' file to apply the major structural update correctly.");
                }
            }

            String langFileName = configProps.getProperty("language", "en_us");
            if (!langFileName.endsWith(".json")) langFileName += ".json";
            this.langFile = new File(languageDir, langFileName);

            initDefaultFiles();

            if (oldVersion != null && oldVersion.startsWith("1.") && currentVersion.startsWith("2.")) {
                File targetEmbed = new File(embedsDir, "threat_alert.json");
                if (targetEmbed.exists()) {
                    System.out.println("[The Dreamers Guards] CRITICAL EMBEDS MIGRATION NOTICE: Major structural shift detected. Server administrators should review or recreate files inside the 'embeds' directory.");
                }
            }

            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                configProps.load(reader);
                sanitizeProperties();
            }
            handleWebhookEncryption();

            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
                JsonObject parsed = JsonParser.parseReader(reader).getAsJsonObject();
                langJson.entrySet().clear();
                parsed.entrySet().forEach(entry -> langJson.add(entry.getKey(), entry.getValue()));
            }
        } catch (Exception e) {
            if ("true".equals(configProps.getProperty("debugging"))) {
                e.printStackTrace();
            }
        }
    }

    private void triggerIndexedBackup() {
        File backupFile = null;
        int availableIndex = -1;
        for (int i = 1; i <= 10; i++) {
            File f = new File(backupDir, "thedreamers_guards" + i + ".old");
            if (!f.exists()) {
                availableIndex = i;
                backupFile = f;
                break;
            }
        }
        if (availableIndex == -1) {
            File first = new File(backupDir, "thedreamers_guards1.old");
            if (first.exists()) first.delete();
            for (int i = 2; i <= 10; i++) {
                File current = new File(backupDir, "thedreamers_guards" + i + ".old");
                File target = new File(backupDir, "thedreamers_guards" + (i - 1) + ".old");
                current.renameTo(target);
            }
            backupFile = new File(backupDir, "thedreamers_guards10.old");
        }
        copyFile(configFile, backupFile);
    }

    private void sanitizeProperties() {
        for (String key : configProps.stringPropertyNames()) {
            String val = configProps.getProperty(key);
            if (val != null && val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                configProps.setProperty(key, val.substring(1, val.length() - 1));
            }
        }
    }

    private void handleWebhookEncryption() {
        String webhookUrl = configProps.getProperty("webhookUrl", "");
        if (webhookUrl.startsWith("http://") || webhookUrl.startsWith("https://")) {
            String encryptedUrl = SecurityUtils.encrypt(webhookUrl);
            configProps.setProperty("webhookUrl", encryptedUrl);
            saveProperties();
        }
    }

    public String getProperty(String key, String defaultValue) {
        String value = configProps.getProperty(key);
        if (value == null) {
            configProps.setProperty(key, defaultValue);
            saveProperties();
            return defaultValue;
        }
        if ("webhookUrl".equals(key) && !value.startsWith("http://") && !value.startsWith("https://") && !value.equals("YOUR_WEBHOOK_URL_HERE") && !value.equals("Change Me!")) {
            return SecurityUtils.decrypt(value);
        }
        return value;
    }

    public String getLanguageString(String key, String defaultValue) {
        if (!langJson.has(key)) {
            langJson.addProperty(key, defaultValue);
            saveJson(langJson, langFile);
            return defaultValue;
        }
        return langJson.get(key).getAsString();
    }

    private void saveProperties() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            writer.write("# =========================================================================\n");
            writer.write("#\n");
            writer.write("#                     THE DREAMERS GUARDS ANTI-CHEAT SUITE\n");
            writer.write("#                        ENTERPRISE CORE CONFIGURATION\n");
            writer.write("#\n");
            writer.write("# =========================================================================\n\n");
            writer.write("# =======================================================================\n");
            writer.write("#                      SYSTEM DOCUMENTATION & TUTORIAL                   \n");
            writer.write("# =======================================================================\n");
            writer.write("# IN-GAME COLOR CODES (§):\n");
            writer.write("#  §0 - Black\n");
            writer.write("#  §1 - Dark Blue\n");
            writer.write("#  §2 - Dark Green\n");
            writer.write("#  §3 - Dark Aqua\n");
            writer.write("#  §4 - Dark Red\n");
            writer.write("#  §5 - Purple\n");
            writer.write("#  §6 - Gold\n");
            writer.write("#  §7 - Gray\n");
            writer.write("#  §8 - Dark Gray\n");
            writer.write("#  §9 - Blue\n");
            writer.write("#  §a - Green\n");
            writer.write("#  §b - Aqua\n");
            writer.write("#  §c - Red\n");
            writer.write("#  §d - Pink\n");
            writer.write("#  §e - Yellow\n");
            writer.write("#  §f - White\n");
            writer.write("\n");
            writer.write("# IN-GAME FORMATTING CODES:\n");
            writer.write("#  §l : Bold\n");
            writer.write("#  §m : Strikethrough\n");
            writer.write("#  §n : Underline\n");
            writer.write("#  §o : Italic\n");
            writer.write("#  §r : Reset All Component Formats\n");
            writer.write("\n");
            writer.write("# =======================================================================\n\n");
            writer.write("# # General Mod Parameters\n");
            writer.write("[general]\n");
            writer.write("\tenabled = " + configProps.getProperty("enabled", "true") + "\n");
            writer.write("\tdebugging = " + configProps.getProperty("debugging", "false") + "\n");
            writer.write("\tenforce_version_match = " + configProps.getProperty("enforce_version_match", "true") + "\n");
            writer.write("\tlanguage = \"" + configProps.getProperty("language", "en_us") + "\"\n");
            writer.write("\tconfigVersion = \"" + getModVersion() + "\"\n\n");
            writer.write("# # Core Master Server Profile Mapping\n");
            writer.write("[server]\n");
            writer.write("\tserverName = \"" + configProps.getProperty("serverName", "Change Me!") + "\"\n");
            writer.write("\tenableAdminPing = " + configProps.getProperty("enableAdminPing", "false") + "\n");
            writer.write("\tadminRoleId = \"" + configProps.getProperty("adminRoleId", "Change Me!") + "\"\n\n");
            writer.write("# # Remote Data Pipelines & Webhook Security Integrations\n");
            writer.write("[webhook]\n");
            writer.write("\twebhookUrl = \"" + configProps.getProperty("webhookUrl", "Change Me!") + "\"\n");
            writer.write("\twebhookAvatarUrl = \"" + configProps.getProperty("webhookAvatarUrl", "DEFAULT") + "\"\n");
            writer.write("\tmodBlacklist = \"" + configProps.getProperty("modBlacklist", "wurst,meteor,liquidbounce,bleachhack,aristois,kami,rusherhack,future,salhack,phobos,kami-blue,konas,inertia,mathax,vector,danielfrominternet,seppuku,coffee,lambda,abyss,w+3,w+2,gopro,earthhack,bleach,pixel,liquid,ares,novoline,flux,rise,tenacity,vape,astolfo,zeroday") + "\"\n\n");
            writer.write("# # Punishment Framework Escalations and Expiration Lifecycles\n");
            writer.write("[punishment]\n");
            writer.write("\tpunishment_mode = \"" + configProps.getProperty("punishment_mode", "KICK") + "\"\n");
            writer.write("\tvalidation_delay_ms = " + configProps.getProperty("validation_delay_ms", "10000") + "\n");
            writer.write("\ttotal_timeout_seconds = " + configProps.getProperty("total_timeout_seconds", "30") + "\n");
            writer.write("\tcountdown_start_seconds = " + configProps.getProperty("countdown_start_seconds", "3") + "\n");
            writer.write("\tsuspension_phase_1_mins = " + configProps.getProperty("suspension_phase_1_mins", "20") + "\n");
            writer.write("\tsuspension_phase_2_mins = " + configProps.getProperty("suspension_phase_2_mins", "120") + "\n");
            writer.write("\tsuspension_phase_3_mins = " + configProps.getProperty("suspension_phase_3_mins", "360") + "\n");
            writer.write("\tsuspension_phase_4_mins = " + configProps.getProperty("suspension_phase_4_mins", "-1") + "\n");
            writer.write("\tappeal_link = \"" + configProps.getProperty("appeal_link", "No appeal system linked. Please contact the Server Administration directly.") + "\"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveJson(JsonObject json, File file) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(json.toString());
        } catch (IOException e) {
            if ("true".equals(configProps.getProperty("debugging"))) e.printStackTrace();
        }
    }

    private void copyFile(File source, File dest) {
        try (FileChannel srcChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } catch (IOException e) {
            if ("true".equals(configProps.getProperty("debugging"))) e.printStackTrace();
        }
    }

    private void initDefaultFiles() throws IOException {
        if (!configFile.exists()) {
            configProps.setProperty("enabled", "true");
            configProps.setProperty("debugging", "false");
            configProps.setProperty("enforce_version_match", "true");
            configProps.setProperty("language", "en_us");
            configProps.setProperty("serverName", "Change Me!");
            configProps.setProperty("enableAdminPing", "false");
            configProps.setProperty("adminRoleId", "Change Me!");
            configProps.setProperty("webhookUrl", "Change Me!");
            configProps.setProperty("webhookAvatarUrl", "DEFAULT");
            configProps.setProperty("modBlacklist", "wurst,meteor,liquidbounce,bleachhack,aristois,kami,rusherhack,future,salhack,phobos,kami-blue,konas,inertia,mathax,vector,danielfrominternet,seppuku,coffee,lambda,abyss,w+3,w+2,gopro,earthhack,bleach,pixel,liquid,ares,novoline,flux,rise,tenacity,vape,astolfo,zeroday");
            configProps.setProperty("punishment_mode", "KICK");
            configProps.setProperty("validation_delay_ms", "10000");
            configProps.setProperty("total_timeout_seconds", "30");
            configProps.setProperty("countdown_start_seconds", "3");
            configProps.setProperty("suspension_phase_1_mins", "20");
            configProps.setProperty("suspension_phase_2_mins", "120");
            configProps.setProperty("suspension_phase_3_mins", "360");
            configProps.setProperty("suspension_phase_4_mins", "-1");
            configProps.setProperty("appeal_link", "No appeal system linked. Please contact the Server Administration directly.");
            saveProperties();
        }
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(langFile), StandardCharsets.UTF_8))) {
                writer.write("{\n" +
                        "  \"system.load_success\": \"§b[The Dreamers Guards] §aCore security framework initialized successfully and securely!\",\n" +
                        "  \"admin.alert_message\": \"§d[GUARDS ALERT] §e%s §7flagged for §c%s (Client-Side Checked).\\n§8Yo admin, use §7/guards action §8to execute this cheater or §7/guards trust §8to let them stay.\",\n" +
                        "  \"admin.trust_permitted\": \"§c[The Dreamers Guards] §aYou have been permitted to stay in this server for a while.\",\n" +
                        "  \"admin.trust_success\": \"§a[Guards] Countdown halted. %s is permitted to stay temporarily.\",\n" +
                        "  \"admin.action_success\": \"§a[Guards] Guards Action Successfully: %s the %s\",\n" +
                        "  \"admin.action_success_reason\": \"§a[Guards] Guards Action Successfully: %s the %s for %s\",\n" +
                        "  \"admin.pardon_success\": \"§a[Guards] Guards Action Successfully: PARDON (%s) the %s\",\n" +
                        "  \"admin.pardon_failed_permanent\": \"§c[Guards] Action Failed: Cannot use KEEP mode for a Phase 4 permanently suspended player. Please utilize REMOVE mode to completely purge the record!\",\n" +
                        "  \"admin.trust_log\": \"§a[Guards] Successfully added %s to the trusted bypass list.\",\n" +
                        "  \"admin.not_op\": \"§c[Guards] Nice try, but this command is for admins only!\",\n" +
                        "  \"admin.player_offline\": \"§c[Guards] Player is not online or active!\",\n" +
                        "  \"kick.caught_message\": \"§c[The Dreamers Guards]\\n§7Nice try, but you got caught: §eIllegal Modifications (Client-Side Checked)\",\n" +
                        "  \"kick.version_mismatch\": \"§c[The Dreamers Guards]\\n\\n§6■ SECURITY NOTICE: VERSION MISMATCH ■\\n§7Your client mod version does not match the server requirements.\\n\\n§7Required Server Version: §e%s\\n§7Your Client Version: §c%s\\n\\n§7Please install the exact matching mod version before trying to reconnect.\",\n" +
                        "  \"kick.action_kick\": \"§c[The Dreamers Guards]\\n\\n§6■ PROTECTION SYSTEM ALERT ■\\n§7You have been disconnected from the dreamscape realm.\\n\\n§6» Status: §eAutomated Security Sweep Enforcement\",\n" +
                        "  \"kick.action_kick_reason\": \"§c[The Dreamers Guards]\\n\\n§6■ ADMINISTRATIVE KICK DROP ■\\n§7Enforced by order of the server security board.\\n\\n§6» Reason: §f%s\",\n" +
                        "  \"kick.action_ban\": \"§c[The Dreamers Guards]\\n\\n§4■ CRITICAL BANISHMENT PROCLAMATION ■\\n§7Your access rights to this realm have been permanently severed.\\n\\n§4» Status: §eThe Core Matrix Integrity Protection Violation\",\n" +
                        "  \"kick.action_ban_reason\": \"§c[The Dreamers Guards]\\n\\n§4■ PERMANENT TERMINATION RECORD ■\\n§7Enforced by operational command authority staff.\\n\\n§4» Reason: §f%s\",\n" +
                        "  \"suspension.temporary_header\": \"§c[The Dreamers Guards]\\n\\n§6■ SECURITY PROTOCOL: CLIENT ISOLATION ■\\n§7An infraction was flagged. Access temporarily restricted.\\n\\n\",\n" +
                        "  \"suspension.temporary_phase\": \"§7Current Phase: §e%s / 3\\n\",\n" +
                        "  \"suspension.temporary_penalty\": \"§7Remaining Penalty: §f%sh %sm %ss\\n\\n\",\n" +
                        "  \"suspension.temporary_footer\": \"§7Please completely purge any illegal modifications from your game directory before trying to reconnect.\",\n" +
                        "  \"suspension.permanent_ban\": \"§c[The Dreamers Guards]\\n\\n§c■ CRITICAL SYSTEM SECURITY BREACH ■\\n§fYour client has been §cPERMANENTLY ISOLATED §ffrom the server.\\n\\n§7Reason: §e%1$s\\n§7Strikes: §c3 / 3 Exceeded\\n§7Status: §cTERMINATED\\n\\n§7Appeal Resolution:\\n§b%2$s\",\n" +
                        "  \"webhook.embed.title\": \"ANTI-CHEAT THREAT DETECTION\",\n" +
                        "  \"webhook.embed.footer\": \"System Integration\",\n" +
                        "  \"webhook.stage.terminated\": \"PHASE 4 (TERMINATED)\",\n" +
                        "  \"webhook.stage.warn\": \"PHASE %s / 3\",\n" +
                        "  \"webhook.field.player\": \"Target Player\",\n" +
                        "  \"webhook.field.action\": \"Action Taken\",\n" +
                        "  \"webhook.field.stage\": \"Infraction Stage\",\n" +
                        "  \"webhook.field.reason\": \"Violation Reason\",\n" +
                        "  \"webhook.field.uuid\": \"Player UUID\",\n" +
                        "  \"webhook.field.ip\": \"Network IP Address\",\n" +
                        "  \"kick_no_mod_message\": \"§c[The Dreamers Guards]\\n§7Grab our mod first if you want to play here, mate!\",\n" +
                        "  \"kick_cheater_message\": \"§c[The Dreamers Guards]\\n§7Nice try, but you got caught: §e%s\",\n" +
                        "  \"kick_version_mismatch_message\": \"§c[The Dreamers Guards]\\n§7Whoops! Your mod version doesn't match the server requirement.\\n§7Server: §e%s §7| Yours: §c%s\",\n" +
                        "  \"alert_admin_message\": \"§d§l[GUARDS ALERT] §e%s §7flagged for §c%s.\\n§7Yo admin, use /guards action to resolve this!\",\n" +
                        "  \"countdown_broadcast_message\": \"§c§l[WARN] §e%s §cgets dropped in §l%ss!\",\n" +
                        "  \"broadcast_ban_message\": \"§3[BANNED] §e%s §7got caught cheating. See ya never!\",\n" +
                        "  \"broadcast_kick_message\": \"§a[KICKED] §e%s §7was removed from the game. Trash cleared.\",\n" +
                        "  \"console_log_alert\": \"[The Dreamers Guards] ALERT: %s detected using illegal modifications (%s).\",\n" +
                        "  \"console_log_failed\": \"§c[WARN] FAILED! §7Failed to execute action on %s.\"\n" +
                        "}");
            }
        }
        File langTutorialFile = new File(languageDir, "tutorial.json");
        if (!langTutorialFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(langTutorialFile), StandardCharsets.UTF_8))) {
                writer.write("{\n" +
                        "  \"__NOTICE__\": \"THIS FILE IS A COMPREHENSIVE LANGUAGE CONFIGURATION MANUAL AND FORMATTING TUTORIAL. IT IS NOT USED BY SYSTEM RUNTIMES.\",\n" +
                        "  \"__MINECRAFT_COLOR_CODES__\": [\n" +
                        "    \"§0 = Black\",\n" +
                        "    \"§1 = Dark Blue\",\n" +
                        "    \"§2 = Dark Green\",\n" +
                        "    \"§3 = Dark Aqua\",\n" +
                        "    \"§4 = Dark Red\",\n" +
                        "    \"§5 = Purple\",\n" +
                        "    \"§6 = Gold\",\n" +
                        "    \"§7 = Gray\",\n" +
                        "    \"§8 = Dark Gray\",\n" +
                        "    \"§9 = Blue\",\n" +
                        "    \"§a = Green\",\n" +
                        "    \"§b = Aqua\",\n" +
                        "    \"§c = Red\",\n" +
                        "    \"§d = Pink\",\n" +
                        "    \"§e = Yellow\",\n" +
                        "    \"§f = White\"\n" +
                        "  ],\n" +
                        "  \"__MINECRAFT_FORMATTING_CODES__\": [\n" +
                        "    \"§l = Bold\",\n" +
                        "    \"§m = Strikethrough\",\n" +
                        "    \"§n = Underline\",\n" +
                        "    \"§o = Italic\",\n" +
                        "    \"§r = Reset All Component Formats\"\n" +
                        "  ],\n" +
                        "  \"__ESCAPE_CHARACTERS_GUIDE__\": [\n" +
                        "    \"\\\\n = Newline adjustment layout break (creates a multi-line kick or broadcast screen)\",\n" +
                        "    \"& or \\\\u00A7 = Automatically processed into section (§) structural color codes by internal regex parsing engines\"\n" +
                        "  ],\n" +
                        "  \"__STRING_PLACEHOLDERS_DICTIONARY__\": {\n" +
                        "    \"admin.alert_message\": \"Requires 2 positional flags: %1$s = Player Name, %2$s = Violation Detection Reason Tag.\",\n" +
                        "    \"admin.trust_success\": \"Requires 1 flag: %s = Player Name verified by operators.\",\n" +
                        "    \"admin.action_success\": \"Requires 2 flags: %1$s = Enforcement Punishment Type, %2$s = Target Profile Username.\",\n" +
                        "    \"admin.action_success_reason\": \"Requires 3 flags: %1$s = Punishment Type, %2$s = Username, %3$s = Custom Board Reason String.\",\n" +
                        "    \"admin.pardon_success\": \"Requires 2 flags: %1$s = Operational Clear Mode, %2$s = Target Identity Name.\",\n" +
                        "    \"kick.version_mismatch\": \"Requires 2 flags: %1$s = Expected Server Metadata Version, %2$s = Detected Client Core Version.\",\n" +
                        "    \"suspension.temporary_phase\": \"Requires 1 flag: %s = Current Infraction Strike Escalation Count.\",\n" +
                        "    \"suspension.temporary_penalty\": \"Requires 3 flags for clock countdown data mapping: %1$s = Remaining Hours, %2$s = Minutes, %3$s = Seconds.\",\n" +
                        "    \"suspension.permanent_ban\": \"Requires 2 flags: %1$s = Reason from data file, %2$s = Dynamic custom appeal target link mapped directly from the TOML parameters setup.\"\n" +
                        "  },\n" +
                        "  \"__SYSTEM_KEYS_DICTIONARY__\": {\n" +
                        "    \"system.load_success\": \"Console logging trace triggered upon validated clean subsystem bootstrap initialization.\",\n" +
                        "    \"admin.not_op\": \"Error block received by non-privileged client trying to bypass operational permission gates.\",\n" +
                        "    \"admin.player_offline\": \"Error component returned when an operator triggers commands targeting an unmapped or cached connection handle.\",\n" +
                        "    \"kick.caught_message\": \"Immediate telemetry penatly intercept screen triggered upon confirmed malicious mod execution reports.\",\n" +
                        "    \"suspension.temporary_header\": \"Quarantine block introduction banner header layout prefix context.\",\n" +
                        "    \"suspension.temporary_footer\": \"Postscript closure statement advising the user to purge files from local directories before joining.\"\n" +
                        "  }\n" +
                        "}");
            }
        }
        writeEmbedIfMissing("threat_alert.json", "{\n" +
                "  \"username\": \"The Dreamers Guards\",\n" +
                "  \"avatar_url\": \"DEFAULT\",\n" +
                "  \"content\": \"Yo, an illegal modification threat has been flagged! Please check this out ASAP!\",\n" +
                "  \"embeds\": [\n" +
                "    {\n" +
                "      \"title\": \"ANTI-CHEAT THREAT DETECTION\",\n" +
                "      \"color\": 16733525,\n" +
                "      \"author\": {\n" +
                "        \"name\": \"Target Player: %player_name%\",\n" +
                "        \"icon_url\": \"%player_avatar%\"\n" +
                "      },\n" +
                "      \"thumbnail\": {\n" +
                "        \"url\": \"Change Me!\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"text\": \"System Integration | %server_name%\"\n" +
                "      },\n" +
                "      \"fields\": [\n" +
                "        { \"name\": \"Target Player:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                "        { \"name\": \"Action Taken:\", \"value\": \"%action_type%\", \"inline\": true },\n" +
                "        { \"name\": \"Infraction Stage:\", \"value\": \"%phase_text%\", \"inline\": false },\n" +
                "        { \"name\": \"Violation Reason:\", \"value\": \"%violation_reason%\", \"inline\": false },\n" +
                "        { \"name\": \"Player UUID:\", \"value\": \"%player_uuid%\", \"inline\": false },\n" +
                "        { \"name\": \"Network IP Address:\", \"value\": \"%player_ip%\", \"inline\": false }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        writeEmbedIfMissing("action_alert.json", "{\n" +
                "  \"username\": \"The Dreamers Guards\",\n" +
                "  \"avatar_url\": \"DEFAULT\",\n" +
                "  \"embeds\": [\n" +
                "    {\n" +
                "      \"title\": \"ADMIN COMMAND EXECUTION\",\n" +
                "      \"description\": \"Admin %admin_name% executed forced punishment action on target player.\",\n" +
                "      \"color\": 16755200,\n" +
                "      \"thumbnail\": {\n" +
                "        \"url\": \"Change Me!\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"text\": \"Admin Enforcement | %server_name%\"\n" +
                "      },\n" +
                "      \"fields\": [\n" +
                "        { \"name\": \"Administrator:\", \"value\": \"%admin_name%\", \"inline\": true },\n" +
                "        { \"name\": \"Target Player:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                "        { \"name\": \"Action Type:\", \"value\": \"%action_type%\", \"inline\": false },\n" +
                "        { \"name\": \"Enforcement Reason:\", \"value\": \"%reason%\", \"inline\": false }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        writeEmbedIfMissing("pardon_alert.json", "{\n" +
                "  \"username\": \"The Dreamers Guards\",\n" +
                "  \"avatar_url\": \"DEFAULT\",\n" +
                "  \"embeds\": [\n" +
                "    {\n" +
                "      \"title\": \"ADMIN PARDON EXECUTION\",\n" +
                "      \"description\": \"Admin %admin_name% pardoned and removed target player from suspension list.\",\n" +
                "      \"color\": 5635925,\n" +
                "      \"thumbnail\": {\n" +
                "        \"url\": \"Change Me!\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"text\": \"Pardon Resolution | %server_name%\"\n" +
                "      },\n" +
                "      \"fields\": [\n" +
                "        { \"name\": \"Administrator:\", \"value\": \"%admin_name%\", \"inline\": true },\n" +
                "        { \"name\": \"Target Player:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                "        { \"name\": \"Pardon Mode:\", \"value\": \"%pardon_mode%\", \"inline\": false }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        writeEmbedIfMissing("trust_alert.json", "{\n" +
                "  \"username\": \"The Dreamers Guards\",\n" +
                "  \"avatar_url\": \"DEFAULT\",\n" +
                "  \"embeds\": [\n" +
                "    {\n" +
                "      \"title\": \"ADMIN TRUST EXEMPTION\",\n" +
                "      \"description\": \"Admin %admin_name% granted an official trust bypass exemption protocol to target player.\",\n" +
                "      \"color\": 5636095,\n" +
                "      \"thumbnail\": {\n" +
                "        \"url\": \"Change Me!\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"text\": \"Trust Protocol Mapped | %server_name%\"\n" +
                "      },\n" +
                "      \"fields\": [\n" +
                "        { \"name\": \"Administrator:\", \"value\": \"%admin_name%\", \"inline\": true },\n" +
                "        { \"name\": \"Target Player:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                "        { \"name\": \"Action Type:\", \"value\": \"TRUST EXEMPTION\", \"inline\": false },\n" +
                "        { \"name\": \"Enforcement Notes:\", \"value\": \"%reason%\", \"inline\": false }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        writeEmbedIfMissing("verify_alert.json", "{\n" +
                "  \"username\": \"The Dreamers Guards\",\n" +
                "  \"avatar_url\": \"DEFAULT\",\n" +
                "  \"embeds\": [\n" +
                "    {\n" +
                "      \"title\": \"PLAYER VERIFICATION MONITOR\",\n" +
                "      \"description\": \"Join network authentication scanner processed entry details.\",\n" +
                "      \"color\": 5592575,\n" +
                "      \"author\": {\n" +
                "        \"name\": \"Target Player: %player_name%\",\n" +
                "        \"icon_url\": \"%player_avatar%\"\n" +
                "      },\n" +
                "      \"thumbnail\": {\n" +
                "        \"url\": \"Change Me!\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"text\": \"Gate Verification | %server_name%\"\n" +
                "      },\n" +
                "      \"fields\": [\n" +
                "        { \"name\": \"Player Name:\", \"value\": \"%player_name%\", \"inline\": true },\n" +
                "        { \"name\": \"Player UUID:\", \"value\": \"%player_uuid%\", \"inline\": true },\n" +
                "        { \"name\": \"Result Status:\", \"value\": \"%violation_reason%\", \"inline\": false },\n" +
                "        { \"name\": \"Detailed Notes:\", \"value\": \"%details%\", \"inline\": false }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        writeEmbedIfMissing("reload_alert.json", "{\n" +
                "  \"username\": \"The Dreamers Guards\",\n" +
                "  \"avatar_url\": \"DEFAULT\",\n" +
                "  \"embeds\": [\n" +
                "    {\n" +
                "      \"title\": \"SYSTEM CONFIGURATION RELOAD\",\n" +
                "      \"description\": \"Admin %admin_name% reloaded the anti-cheat core security configuration variables.\",\n" +
                "      \"color\": 5636095,\n" +
                "      \"thumbnail\": {\n" +
                "        \"url\": \"Change Me!\"\n" +
                "      },\n" +
                "      \"footer\": {\n" +
                "        \"text\": \"System Maintenance | %server_name%\"\n" +
                "      },\n" +
                "      \"fields\": [\n" +
                "        { \"name\": \"Administrator:\", \"value\": \"%admin_name%\", \"inline\": true }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        writeEmbedIfMissing("tutorial.json", "{\n" +
                "  \"__NOTICE__\": \"THIS FILE IS PURELY AN ADMINISTRATIVE LOGICAL MANUAL. IT IS NEVER USED BY CORE PIPELINES.\",\n" +
                "  \"__PLACEHOLDERS_TUTORIAL__\": [\n" +
                "    \"%player_name%       -> Displays target user display account name\",\n" +
                "    \"%player_uuid%       -> Mapped account identity index token format\",\n" +
                "    \"%player_avatar%     -> Automated live render link stream tracking avatar\",\n" +
                "    \"%admin_name%        -> Executor staff user name or alternative SYSTEM tag\",\n" +
                "    \"%admin_avatar%      -> Live 3D cube head asset rendering stream link for administrator\",\n" +
                "    \"%action_type%       -> Active response penalty applied (KICK or BAN)\",\n" +
                "    \"%violation_reason%  -> Summary description string of the blacklisted client hack signature\",\n" +
                "    \"%phase_text%        -> Escalation stage tracker string data\",\n" +
                "    \"%server_name%       -> Server name fetched from master TOML parameters\"\n" +
                "  ]\n" +
                "}");
    }

    private void writeEmbedIfMissing(String fileName, String content) throws IOException {
        File target = new File(embedsDir, fileName);
        if (!target.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8))) {
                writer.write(content);
            }
        }
    }
}