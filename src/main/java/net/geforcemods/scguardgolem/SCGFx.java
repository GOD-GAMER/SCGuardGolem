package net.geforcemods.scguardgolem;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * World-visible particle bursts + positional sounds, broadcast to every nearby
 * player. Both underlying calls are stable across all 8 targets and need NO
 * Stonecutter guard:
 * <ul>
 *   <li>{@code ServerLevel#sendParticles(T, x,y,z, count, dx,dy,dz, speed)} — the
 *       broadcast overload (no per-player boolean was ever added to it), unlike
 *       {@code SCGuardGolem.particles(...)} which targets a single player.</li>
 *   <li>{@code Level#playSound(null, x,y,z, SoundEvent, SoundSource, vol, pitch)} —
 *       {@code null} auto-broadcasts; the 3rd arg is a raw {@code SoundEvent} on every
 *       target (use only raw-typed {@code SoundEvents.*}, never Holder-typed ones).</li>
 * </ul>
 */
public final class SCGFx {
    private SCGFx() {}

    /** Spawn {@code count} particles centered at (x,y,z) with a symmetric spread, visible to all nearby players. */
    public static void burst(ServerLevel level, SimpleParticleType type, double x, double y, double z,
            int count, double spread, double speed) {
        level.sendParticles(type, x, y, z, count, spread, spread, spread, speed);
    }

    /** Burst centered on an entity's mid-height. */
    public static void burst(ServerLevel level, SimpleParticleType type, Entity entity,
            int count, double spread, double speed) {
        burst(level, type, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), count, spread, speed);
    }

    /** Positional sound broadcast to all nearby players. */
    public static void sound(Level level, double x, double y, double z, SoundEvent snd,
            SoundSource src, float vol, float pitch) {
        level.playSound(null, x, y, z, snd, src, vol, pitch);
    }

    /** Positional sound at an entity. */
    public static void sound(Level level, Entity entity, SoundEvent snd, SoundSource src, float vol, float pitch) {
        sound(level, entity.getX(), entity.getY(), entity.getZ(), snd, src, vol, pitch);
    }
}
