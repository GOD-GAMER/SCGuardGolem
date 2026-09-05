package net.geforcemods.scguardgolem.entity.goal;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import java.util.EnumSet;

/**
 * Walks the golem's waypoint loop: TRAVEL to the current waypoint, DWELL there
 * (optionally roaming within the wander radius), advance, repeat.
 *
 * <p>Robustness rules that keep a patrol from silently deadlocking:
 * <ul>
 *   <li><b>Arrival uses the entity's real position and its width</b>, horizontally, with a
 *       lenient Y. A 1.4-wide golem's path legitimately ends up to ~a block from the exact
 *       waypoint column (doorways, walls, ledges); an integer block-distance test can fail
 *       forever and leave the golem re-pathing in place.</li>
 *   <li><b>Stuck-skip:</b> if the navigation finishes (or keeps failing) without arrival for
 *       {@link #STUCK_TICKS_TO_SKIP} ticks, the waypoint is treated as reached so an
 *       unreachable or too-tight spot cannot stall the whole route.</li>
 *   <li><b>Dwell is a state, not a distance check</b> — leaving the arrival radius (being
 *       pushed, or deliberately wandering) never resets the countdown.</li>
 *   <li><b>Every-tick updates</b>: vanilla ticks a goal on alternate ticks unless it asks
 *       otherwise, which silently doubled dwell/wander timings.</li>
 *   <li><b>Recall resumes the patrol</b> from waypoint 0 instead of parking the golem.</li>
 * </ul>
 */
public class PatrolGoal extends Goal {

    private final SecurityGolemEntity golem;

    /** Extra horizontal reach beyond the golem's half-width before a waypoint counts as reached. */
    private static final double REACH_EXTRA = 1.25;
    private static final double REACH_MAX_DY = 2.0;
    private static final int RECALC_COOLDOWN_TICKS = 40;
    private static final int STUCK_TICKS_TO_SKIP = 60;
    private static final int WANDER_REPICK_MIN_TICKS = 30;
    private static final int WANDER_REPICK_JITTER = 20;
    private static final double WANDER_SPEED_FACTOR = 0.6;

    private enum State { TRAVEL, DWELL }

    private State state = State.TRAVEL;
    private boolean pathStarted = false;
    private int recalcCooldown = 0;
    private int stuckTicks = 0;
    private int dwellCountdown = 0;
    private int wanderRepick = 0;

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
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        state = State.TRAVEL;
        pathStarted = false;
        recalcCooldown = 0;
        stuckTicks = 0;
        dwellCountdown = 0;
        wanderRepick = 0;
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
        pathStarted = false;
        state = State.TRAVEL;
    }

    @Override
    public void tick() {
        if (golem.isRecalling()) {
            tickRecall();
            return;
        }
        BlockPos target = golem.getCurrentWaypoint();
        if (target == null) return;
        if (state == State.TRAVEL) tickTravel(target);
        else tickDwell(target);
    }

    // ---- states -----------------------------------------------------------

    private void tickRecall() {
        BlockPos first = golem.getWaypoints().get(0);
        if (near(first) || gaveUp()) {
            golem.finishRecall(); // clears the flag; patrol carries on from waypoint 0
            enterDwell();
            return;
        }
        travelTowards(first);
    }

    private void tickTravel(BlockPos target) {
        if (near(target) || gaveUp()) {
            enterDwell();
            return;
        }
        travelTowards(target);
    }

    private void tickDwell(BlockPos target) {
        if (dwellCountdown > 0) {
            dwellCountdown--;
            int radius = golem.getWanderRadius();
            if (radius > 0) wander(target, radius);
            else golem.getNavigation().stop();
            return;
        }
        // Dwell finished -> next waypoint.
        golem.advanceWaypoint();
        state = State.TRAVEL;
        pathStarted = false;
        recalcCooldown = 0;
        stuckTicks = 0;
    }

    // ---- helpers ----------------------------------------------------------

    private void enterDwell() {
        golem.getNavigation().stop();
        state = State.DWELL;
        dwellCountdown = golem.getDwellTicks(); // 0 = advance on the very next tick
        wanderRepick = 0;
        stuckTicks = 0;
        pathStarted = false;
    }

    /** Path completed (or kept failing) without ever registering arrival. */
    private boolean gaveUp() {
        return pathStarted && stuckTicks >= STUCK_TICKS_TO_SKIP;
    }

    /** Horizontal distance to the waypoint column within reach, and roughly the same height. */
    private boolean near(BlockPos wp) {
        double dx = golem.getX() - (wp.getX() + 0.5);
        double dz = golem.getZ() - (wp.getZ() + 0.5);
        double reach = golem.getBbWidth() * 0.5 + REACH_EXTRA;
        return dx * dx + dz * dz <= reach * reach && Math.abs(golem.getY() - wp.getY()) <= REACH_MAX_DY;
    }

    /** One travel step: (re)issue the path when needed, with a cooldown after failures and a stuck counter. */
    private void travelTowards(BlockPos target) {
        if (recalcCooldown > 0) {
            recalcCooldown--;
            return;
        }
        PathNavigation nav = golem.getNavigation();
        if (!pathStarted || nav.isDone()) {
            if (pathStarted) stuckTicks++; // a path ended and we're still not there
            boolean ok = nav.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, golem.getPatrolSpeed());
            pathStarted = true;
            if (!ok) {
                recalcCooldown = RECALC_COOLDOWN_TICKS;
                stuckTicks += RECALC_COOLDOWN_TICKS; // no path at all counts heavily toward giving up
            }
        }
    }

    /** Roam around the waypoint while dwelling: periodically pick a random ground spot within the radius. */
    private void wander(BlockPos center, int radius) {
        PathNavigation nav = golem.getNavigation();
        // Walk to the current spot, then linger until the timer runs out before picking another.
        if (wanderRepick > 0) {
            wanderRepick--;
            return;
        }
        wanderRepick = WANDER_REPICK_MIN_TICKS + golem.getRandom().nextInt(WANDER_REPICK_JITTER);
        double angle = golem.getRandom().nextDouble() * Math.PI * 2.0;
        double dist = golem.getRandom().nextDouble() * radius;
        double x = center.getX() + 0.5 + Math.cos(angle) * dist;
        double z = center.getZ() + 0.5 + Math.sin(angle) * dist;
        nav.moveTo(x, center.getY(), z, golem.getPatrolSpeed() * WANDER_SPEED_FACTOR);
    }
}
