package net.geforcemods.scguardgolem.entity;

import net.geforcemods.securitycraft.api.IEMPAffected;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasscodeProtected;
import net.minecraft.world.entity.Entity;

/**
 * The shared contract for every SecurityCraft guard entity (the Iron Golem guard
 * and tamed-mob guards). Aggregates the SecurityCraft interfaces the framework is
 * built on and adds the guard-specific threat predicate, so the shared goals, the
 * IMC target-check, and the taming logic can drive any guard through one type.
 *
 * <p>See {@code docs/TAMED-GUARDS-DESIGN.md}. Synced state (owner, shut-down,
 * threat mode) stays on each concrete entity because {@code EntityDataAccessor}s are
 * per-class; the bulky non-synced state (modules, passcode, loot) lives in a shared
 * {@link GuardCore}.
 */
public interface IGuardEntity extends IOwnable, IModuleInventory, IEMPAffected, IPasscodeProtected {

    /**
     * A hostile threat is any Enemy (bar Creepers) OR anything another mod registered
     * as sentry-attackable via SecurityCraft IMC — keeping guard threat logic
     * consistent with the Sentry and respecting other addons' rules.
     */
    boolean isHostileThreat(Entity entity);
}
