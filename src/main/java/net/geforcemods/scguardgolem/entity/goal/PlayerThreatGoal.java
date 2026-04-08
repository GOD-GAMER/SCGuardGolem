package net.geforcemods.scguardgolem.entity.goal;

import java.util.EnumSet;
import java.util.List;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity.ThreatMode;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class PlayerThreatGoal extends TargetGoal {

    private final SecurityGolemEntity golem;
    private Player targetPlayer;
    private static final double FOLLOW_DISTANCE = 3.0;
    private static final int FOLLOW_RECALC_TICKS = 20;
    private int followRecalcCooldown = 0;

    public PlayerThreatGoal(SecurityGolemEntity golem) {
        super(golem, false);
        this.golem = golem;
        setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (golem.getThreatMode() == ThreatMode.WARN) return false;
        if (!golem.isScanTick()) return false;
        LivingEntity current = golem.getTarget();
        if (current instanceof Player player && player.isAlive()
                && !player.isSpectator() && !player.isCreative()) {
            targetPlayer = player;
            return true;
        }
        targetPlayer = findAlwaysAttackTarget();
        return targetPlayer != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPlayer == null || !targetPlayer.isAlive()
                || targetPlayer.isSpectator() || targetPlayer.isCreative()) return false;
        double maxDist = golem.getEffectiveDetectionRadius() * 1.5;
        if (golem.distanceToSqr(targetPlayer) > maxDist * maxDist) return false;
        String name = targetPlayer.getName().getString();
        return !golem.isOnIgnoreList(name) && !golem.isOwner(targetPlayer);
    }

    @Override
    public void start() { golem.setTarget(targetPlayer); followRecalcCooldown = 0; }

    @Override
    public void stop() { golem.setTarget(null); golem.getNavigation().stop(); targetPlayer = null; }

    @Override
    public void tick() {
        if (targetPlayer == null) return;
        if (golem.getThreatMode() == ThreatMode.FOLLOW) {
            double distSq = golem.distanceToSqr(targetPlayer);
            if (distSq > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
                if (followRecalcCooldown <= 0) {
                    golem.getNavigation().moveTo(targetPlayer, 1.0);
                    followRecalcCooldown = FOLLOW_RECALC_TICKS;
                } else { followRecalcCooldown--; }
            } else {
                golem.getNavigation().stop();
                golem.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);
            }
        }
    }

    private Player findAlwaysAttackTarget() {
        double radius = golem.getEffectiveDetectionRadius();
        AABB searchBox = golem.getBoundingBox().inflate(radius);
        List<Player> nearby = golem.level().getEntitiesOfClass(Player.class, searchBox,
                p -> !p.isSpectator() && !p.isCreative() && p.isAlive());
        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Player player : nearby) {
            if (!golem.isOnAlwaysAttackList(player.getName().getString())) continue;
            if (!golem.getSensing().hasLineOfSight(player)) continue;
            double distSq = golem.distanceToSqr(player);
            if (distSq < nearestDistSq) { nearestDistSq = distSq; nearest = player; }
        }
        return nearest;
    }
}
