package net.geforcemods.scguardgolem.entity.goal;

import java.util.EnumSet;
import java.util.List;

import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity.ThreatMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class BadgeCheckGoal extends Goal {

    private final SecurityGolemEntity golem;

    public BadgeCheckGoal(SecurityGolemEntity golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return golem.isScanTick() && golem.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        double radius = golem.getEffectiveDetectionRadius();
        AABB searchBox = golem.getBoundingBox().inflate(radius);
        List<Player> nearbyPlayers = golem.level().getEntitiesOfClass(Player.class, searchBox,
                p -> !p.isSpectator() && !p.isCreative() && p.isAlive());

        for (Player player : nearbyPlayers) {
            if (!golem.getSensing().hasLineOfSight(player)) continue;

            String playerName = player.getName().getString();

            if (golem.isOnAlwaysAttackList(playerName)) { handleUntrustedPlayer(player); continue; }
            if (golem.isOnIgnoreList(playerName)) continue;
            if (golem.isOwner(player)) continue;

            if (SCGuardGolem.isPlayerTrustedByOwner(
                    player.getGameProfile().id().toString(),
                    playerName,
                    golem.getOwnerUUID(),
                    golem.getOwnerName())) continue;

            handleUntrustedPlayer(player);
        }
    }

    private void handleUntrustedPlayer(Player player) {
        ThreatMode mode = golem.getThreatMode();
        switch (mode) {
            case WARN -> player.displayClientMessage(
                    Component.translatable("scguardgolem.badge_check.halt"), false);
            case FOLLOW, ATTACK -> golem.setTarget(player);
        }
    }
}
