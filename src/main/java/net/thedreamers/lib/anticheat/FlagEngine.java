package net.thedreamers.lib.anticheat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FlagEngine {

    private final Set<UUID> activeFlags = new HashSet<>();

    public void flagPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        activeFlags.add(uuid);
        applyPunishmentEffects(player);
    }

    public void unflagPlayer(UUID uuid) {
        activeFlags.remove(uuid);
    }

    public boolean isFlagged(UUID uuid) {
        return activeFlags.contains(uuid);
    }

    public void applyPunishmentEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 255, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 255, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 255, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 255, false, false));
    }
}