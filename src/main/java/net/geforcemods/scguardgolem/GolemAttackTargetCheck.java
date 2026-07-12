package net.geforcemods.scguardgolem;

import net.geforcemods.securitycraft.api.IAttackTargetCheck;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;

/**
 * The golem's base threat rule, registered with SecurityCraft via IMC
 * ({@code registerSentryAttackTargetCheck}) so the shared sentry/golem threat
 * registry knows what the guard golem considers hostile — keeping its targeting
 * consistent with the Sentry and other addons that consult the same registry.
 */
public class GolemAttackTargetCheck implements IAttackTargetCheck {
    @Override
    public boolean canAttack(Entity potentialTarget) {
        return potentialTarget instanceof Enemy && !(potentialTarget instanceof Creeper);
    }
}
