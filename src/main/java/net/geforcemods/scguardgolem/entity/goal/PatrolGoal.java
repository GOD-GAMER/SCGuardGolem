package net.geforcemods.scguardgolem.entity.goal;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class PatrolGoal extends Goal {

    private final SecurityGolemEntity golem;
    private static final double WAYPOINT_REACH_DIST_SQ = 4.0;
    private static final int RECALC_COOLDOWN_TICKS = 40;
    private int recalcCooldown = 0;
    private boolean pathStarted = false;
    /** -1 = idle (not dwelling); >0 = counting down; 0 = done, ready to advance */
    private int dwellCountdown = -1;

    public PatrolGoal(SecurityGolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !golem.isShutDown() && golem.isPatrolling() && !golem.getWaypoints().isEmpty() && golem.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void start() { pathStarted = false; recalcCooldown = 0; dwellCountdown = -1; }

    @Override
    public void stop() { golem.getNavigation().stop(); pathStarted = false; }

    @Override
    public void tick() {
        // If recalling, navigate to first waypoint only
        if (golem.isRecalling()) {
            BlockPos firstWp = golem.getWaypoints().get(0);
            double distSq = golem.blockPosition().distSqr(firstWp);
            if (distSq <= WAYPOINT_REACH_DIST_SQ) {
                golem.finishRecall();
                pathStarted = false;
                return;
            }
            if (recalcCooldown > 0) { recalcCooldown--; return; }
            if (!pathStarted || golem.getNavigation().isDone()) {
                boolean success = golem.getNavigation().moveTo(
                        firstWp.getX() + 0.5, firstWp.getY(), firstWp.getZ() + 0.5,
                        golem.getPatrolSpeed());
                pathStarted = true;
                if (!success) recalcCooldown = RECALC_COOLDOWN_TICKS;
            }
            return;
        }

        BlockPos target = golem.getCurrentWaypoint();
        if (target == null) return;

        double distSq = golem.blockPosition().distSqr(target);
        if (distSq <= WAYPOINT_REACH_DIST_SQ) {
            // Arm dwell once on arrival
            if (dwellCountdown == -1) {
                dwellCountdown = golem.getDwellTicks(); // 0 = no dwell, advance immediately
            }
            if (dwellCountdown > 0) {
                dwellCountdown--;
                golem.getNavigation().stop();
                return;
            }
            // Dwell finished (or no dwell) — advance
            dwellCountdown = -1;
            golem.advanceWaypoint();
            pathStarted = false;
            recalcCooldown = 0;
            return;
        }
        // Left waypoint range — reset dwell for next arrival
        dwellCountdown = -1;

        if (recalcCooldown > 0) { recalcCooldown--; return; }

        if (!pathStarted || golem.getNavigation().isDone()) {
            boolean success = golem.getNavigation().moveTo(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                    golem.getPatrolSpeed());
            pathStarted = true;
            if (!success) recalcCooldown = RECALC_COOLDOWN_TICKS;
        }
    }
}
