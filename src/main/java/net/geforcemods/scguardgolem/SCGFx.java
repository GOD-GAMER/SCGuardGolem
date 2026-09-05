package net.geforcemods.scguardgolem;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * World-visible particle + sound FX. Everything is broadcast to nearby players
 * and needs NO Stonecutter guard: the broadcast {@code sendParticles(T,x,y,z,count,dx,dy,dz,speed)}
 * overload and positional {@code playSound(null,...)} are stable on all 8 targets,
 * and every particle used is a {@link SimpleParticleType} present since 1.20.1
 * (no custom particles, no {@code DustParticleOptions} version traps).
 *
 * <p>The "epic" set-pieces (EMP blast, power-up, alert) are CHOREOGRAPHED: an
 * entity holds a countdown timer and calls the matching {@code tick*} method each
 * server tick, so the effect animates frame-by-frame (expanding shockwave rings,
 * a rising energy helix, a sonic-boom detonation) instead of a single puff.
 */
public final class SCGFx {
    private SCGFx() {}

    // Total tick lengths of the choreographed sequences.
    public static final int EMP_BLAST_TICKS = 22;
    public static final int POWER_UP_TICKS = 20;
    public static final int ALERT_TICKS = 10;

    // ---- primitives -------------------------------------------------------

    /** count particles on a horizontal circle; vOut>0 gives them outward velocity (a shockwave). */
    public static void ring(ServerLevel level, SimpleParticleType type, double cx, double cy, double cz,
            double radius, int count, double vOut) {
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2.0 * i) / count;
            double x = cx + Math.cos(a) * radius;
            double z = cz + Math.sin(a) * radius;
            if (vOut > 0) level.sendParticles(type, x, cy, z, 0, Math.cos(a), 0.02, Math.sin(a), vOut);
            else level.sendParticles(type, x, cy, z, 1, 0, 0, 0, 0);
        }
    }

    /** A straight line of particles from a to b (used for the EMP beam). */
    public static void beam(ServerLevel level, SimpleParticleType type,
            double ax, double ay, double az, double bx, double by, double bz, int steps) {
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            level.sendParticles(type, ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t, 1, 0, 0, 0, 0);
        }
    }

    /** count particles on a sphere surface (Fibonacci distribution), optional outward velocity. */
    public static void sphere(ServerLevel level, SimpleParticleType type, double cx, double cy, double cz,
            double radius, int count, double vOut) {
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < count; i++) {
            double yy = 1.0 - (i / (double) (count - 1)) * 2.0;
            double r = Math.sqrt(1.0 - yy * yy);
            double th = golden * i;
            double dx = Math.cos(th) * r, dy = yy, dz = Math.sin(th) * r;
            double x = cx + dx * radius, y = cy + dy * radius, z = cz + dz * radius;
            if (vOut > 0) level.sendParticles(type, x, y, z, 0, dx, dy, dz, vOut);
            else level.sendParticles(type, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    // ---- one-shot helpers (kept for callers that don't animate) -----------

    public static void burst(ServerLevel level, SimpleParticleType type, double x, double y, double z,
            int count, double spread, double speed) {
        level.sendParticles(type, x, y, z, count, spread, spread, spread, speed);
    }

    public static void burst(ServerLevel level, SimpleParticleType type, Entity entity,
            int count, double spread, double speed) {
        burst(level, type, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), count, spread, speed);
    }

    public static void sound(Level level, double x, double y, double z, SoundEvent snd, SoundSource src, float vol, float pitch) {
        level.playSound(null, x, y, z, snd, src, vol, pitch);
    }

    public static void sound(Level level, Entity entity, SoundEvent snd, SoundSource src, float vol, float pitch) {
        sound(level, entity.getX(), entity.getY(), entity.getZ(), snd, src, vol, pitch);
    }

    // ---- choreographed set-pieces (one frame per call) --------------------

    /** EMP detonation: implosion -> sonic-boom flash -> expanding shockwave -> smouldering aftermath. */
    public static void tickEmpBlast(ServerLevel level, Entity e, int remaining) {
        double cx = e.getX(), cy = e.getY() + e.getBbHeight() * 0.5, cz = e.getZ();
        int t = EMP_BLAST_TICKS - remaining; // 0..EMP_BLAST_TICKS-1
        if (t < 6) {
            // implosion: sparks spiral inward before the blast
            double r = 2.2 - t * 0.3;
            double spin = t * 0.8;
            for (int k = 0; k < 6; k++) {
                double a = spin + (Math.PI * 2 * k) / 6;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx + Math.cos(a) * r, cy + Math.sin(a * 1.3) * 0.4, cz + Math.sin(a) * r,
                        0, -Math.cos(a) * 0.15, 0, -Math.sin(a) * 0.15, 1);
            }
        } else if (t == 6) {
            // DETONATION
            level.sendParticles(ParticleTypes.SONIC_BOOM, cx, cy, cz, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 1, 0, 0, 0, 0);
            sphere(level, ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 0.4, 30, 0.7);
            sound(level, e, net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.2F, 0.6F);
        } else if (t < 16) {
            // expanding double shockwave ring on the ground + at mid-height
            double r = (t - 6) * 0.45;
            ring(level, ParticleTypes.ELECTRIC_SPARK, cx, e.getY() + 0.1, cz, r, 22, 0);
            ring(level, ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, r * 0.7, 12, 0);
        } else {
            // aftermath: smoke rises, sparks fizzle
            level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.3, cz, 2, 0.25, 0.35, 0.25, 0.02);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 2, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /** Power-up: a rising twin energy helix that culminates in a flash + totem burst + pillar. */
    public static void tickPowerUp(ServerLevel level, Entity e, int remaining) {
        double cx = e.getX(), cz = e.getZ();
        double h = e.getBbHeight() + 0.5;
        int t = POWER_UP_TICKS - remaining;
        double frac = t / (double) POWER_UP_TICKS;
        double y = e.getY() + frac * h;
        double r = 0.15 + (1.0 - frac) * 0.6;
        double angle = t * 0.9;
        for (int k = 0; k < 2; k++) {
            double a = angle + k * Math.PI;
            level.sendParticles(ParticleTypes.END_ROD, cx + Math.cos(a) * r, y, cz + Math.sin(a) * r, 1, 0, 0.02, 0, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx + Math.cos(a + 0.5) * r, y, cz + Math.sin(a + 0.5) * r, 1, 0, 0, 0, 0);
        }
        if (remaining == 1) {
            double my = e.getY() + h * 0.5;
            level.sendParticles(ParticleTypes.EXPLOSION, cx, my, cz, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, my, cz, 40, 0.3, 0.6, 0.3, 0.25);
            for (int p = 0; p < 8; p++) level.sendParticles(ParticleTypes.END_ROD, cx, e.getY() + p * 0.22, cz, 1, 0, 0.12, 0, 0.04);
            sound(level, e, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.9F, 1.3F);
        }
    }

    /** Threat alert: a quick flash + a pulsing expanding warning ring above the head. */
    public static void tickAlert(ServerLevel level, Entity e, int remaining) {
        double cx = e.getX(), cy = e.getY() + e.getBbHeight() + 0.35, cz = e.getZ();
        int t = ALERT_TICKS - remaining;
        if (t == 0) level.sendParticles(ParticleTypes.CRIT, cx, cy, cz, 6, 0.1, 0.1, 0.1, 0.2);
        double r = 0.25 + t * 0.13;
        ring(level, ParticleTypes.ANGRY_VILLAGER, cx, cy, cz, r, 6, 0);
        if (t % 2 == 0) level.sendParticles(ParticleTypes.CRIT, cx, cy - 0.2, cz, 3, 0.2, 0.1, 0.2, 0.1);
    }
}
