package net.geforcemods.scguardgolem.entity;

import net.geforcemods.scguardgolem.SCGFx;
import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.items.ModuleItem;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.network.client.OpenScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
//? if >=1.21.11 {
import net.minecraft.world.entity.monster.zombie.Zombie;
//?} else
/*import net.minecraft.world.entity.monster.Zombie;*/
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.8 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

/**
 * A hostile mob tamed into an owned SecurityCraft guard. Shares the guard framework
 * with {@link SecurityGolemEntity} via {@link IGuardEntity} + {@link GuardCore}: same
 * Owner ownership (with teams), module inventory + binary effects, passcode-gated
 * loot, EMP shutdown, and IMC-consistent targeting.
 *
 * <p>Extends {@link Zombie} so it reuses the vanilla humanoid renderer across all
 * MC versions (a single stable look for every tamed mob — see
 * {@code docs/TAMED-GUARDS-DESIGN.md}); the source mob's attributes still carry over
 * for stats. Modules are installed the Sentry way — right-click with the module item.
 */
public class TamedGuardEntity extends Zombie implements MenuProvider, IGuardEntity, GuardCore.Host {

    private static final EntityDataAccessor<Owner> OWNER =
            SynchedEntityData.defineId(TamedGuardEntity.class, Owner.getSerializer());
    private static final EntityDataAccessor<Boolean> SHUT_DOWN =
            SynchedEntityData.defineId(TamedGuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> THREAT_MODE =
            SynchedEntityData.defineId(TamedGuardEntity.class, EntityDataSerializers.INT);

    public static final double DETECTION_RADIUS = 16.0;
    public static final double SMART_DETECTION_RADIUS = 32.0;
    public static final double HARMING_DAMAGE_BONUS = 6.0;
    public static final double SPEED_BONUS = 0.05;

    private final GuardCore core = new GuardCore(this);
    private int scanTimer = 0;
    private int pickupCooldown = 0;

    public TamedGuardEntity(EntityType<? extends TamedGuardEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    //? if >=1.21.1 {
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, new Owner());
        builder.define(SHUT_DOWN, false);
        builder.define(THREAT_MODE, SecurityGolemEntity.ThreatMode.ATTACK.ordinal());
    }
    //?} else {
    /*@Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER, new Owner());
        entityData.define(SHUT_DOWN, false);
        entityData.define(THREAT_MODE, SecurityGolemEntity.ThreatMode.ATTACK.ordinal());
    }
    *///?}

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        //? if >=1.21.8 {
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 10, true, false,
                (entity, level) -> isHostileThreat(entity)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                Player.class, 10, true, false,
                (entity, level) -> shouldAttackPlayer(entity)));
        //?} else {
        /*this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 10, true, false,
                entity -> isHostileThreat(entity)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                Player.class, 10, true, false,
                entity -> entity instanceof Player p && shouldAttackPlayer(p)));
        *///?}
    }

    /** A player is a target only when denied, or (in ATTACK mode) not the owner/allowed. */
    private boolean shouldAttackPlayer(Object entity) {
        if (isShutDown() || !(entity instanceof Player player)) return false;
        if (player.isSpectator() || player.isCreative()) return false;
        if (isDenied(player)) return true;
        if (isAllowed(player) || isOwnedBy(player, false)) return false;
        return getThreatMode() == SecurityGolemEntity.ThreatMode.ATTACK;
    }

    @Override
    public boolean isHostileThreat(net.minecraft.world.entity.Entity entity) {
        if (isShutDown()) return false;
        if (entity instanceof net.minecraft.world.entity.monster.Creeper) return false;
        if (entity instanceof net.minecraft.world.entity.monster.Enemy && !(entity instanceof TamedGuardEntity)) return true;
        return net.geforcemods.securitycraft.api.SecurityCraftAPI.getRegisteredSentryAttackTargetChecks().stream()
                .anyMatch(check -> check.canAttack(entity));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            scanTimer = (scanTimer + 1) % 20;
            if (pickupCooldown > 0) pickupCooldown--;
            else { core.pickupNearbyItems(getBoundingBox().inflate(2.0)); pickupCooldown = 10; }
            if (level() instanceof ServerLevel sl) {
                if (empBlastFxTimer > 0) { SCGFx.tickEmpBlast(sl, this, empBlastFxTimer); empBlastFxTimer--; }
                if (powerUpFxTimer > 0)  { SCGFx.tickPowerUp(sl, this, powerUpFxTimer);   powerUpFxTimer--; }
                if (alertFxTimer > 0)    { SCGFx.tickAlert(sl, this, alertFxTimer);       alertFxTimer--; }
                if (isShutDown() && scanTimer == 0)
                    SCGFx.burst(sl, ParticleTypes.SMOKE, getX(), getY() + getBbHeight(), getZ(), 2, 0.15, 0.01);
            }
        }
    }

    // -- Zombie quirks a guard shouldn't have --
    @Override
    public boolean isSunSensitive() { return false; }

    @Override
    protected boolean convertsInWater() { return false; }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    // -- Owner (IOwnable) --
    public void setGuardOwner(Player player) { entityData.set(OWNER, new Owner(player)); }

    @Override
    public Owner getOwner() { return entityData.get(OWNER); }

    @Override
    public void setOwner(String uuid, String name) { entityData.set(OWNER, new Owner(name, uuid)); }

    @Override
    public void onOwnerChanged(BlockState state, Level level, BlockPos pos, Player player, Owner oldOwner, Owner newOwner) {}

    public boolean isOwner(Player player) { return isOwnedBy(player, false); }

    // -- IModuleInventory (delegated to GuardCore) --
    public SimpleContainer getModuleInventory() { return core.moduleContainer(); }

    @Override
    public NonNullList<ItemStack> getInventory() { return core.moduleView(); }

    @Override
    public ModuleType[] acceptedModules() { return core.acceptedModules(); }

    @Override
    public boolean isModuleEnabled(ModuleType module) { return core.hasModule(module); }

    @Override
    public void toggleModuleState(ModuleType module, boolean shouldBeEnabled) {}

    @Override
    public Level myLevel() { return level(); }

    @Override
    public BlockPos myPos() { return blockPosition(); }

    @Override
    public void onModuleInserted(ItemStack stack, ModuleType module, boolean toggled) { onModulesChanged(); }

    @Override
    public void onModuleRemoved(ItemStack stack, ModuleType module, boolean toggled) { onModulesChanged(); }

    // -- IPasscodeProtected (delegated to GuardCore) --
    @Override public byte[] getPasscode() { return core.getPasscode(); }
    @Override public void setPasscode(byte[] passcode) { core.setPasscode(passcode); }
    @Override public java.util.UUID getSaltKey() { return core.getSaltKey(); }
    @Override public void setSaltKey(java.util.UUID saltKey) { core.setSaltKey(saltKey); }
    @Override public void setSaveSalt(boolean saveSalt) { core.setSaveSalt(saveSalt); }
    @Override public boolean shouldSaveSalt() { return core.shouldSaveSalt(); }
    @Override public long getCooldownEnd() { return core.getCooldownEnd(); }
    @Override public boolean isOnCooldown() { return core.isOnCooldown(); }
    @Override public void startCooldown() { core.startCooldown(); }
    @Override public void activate(Player player) { if (player instanceof ServerPlayer sp) core.activate(sp); }

    @Override
    public void openPasscodeGUI(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && getPasscode() != null && player instanceof ServerPlayer sp) core.openCheckScreen(sp);
    }

    @Override
    public void openSetPasscodeScreen(ServerPlayer player, BlockPos pos) { core.openSetScreen(player); }

    // -- IEMPAffected --
    // Transient FX countdowns (not saved/synced) — drive the animated set-pieces in tick().
    private int empBlastFxTimer, powerUpFxTimer, alertFxTimer;

    public void startEmpBlastFx() { empBlastFxTimer = SCGFx.EMP_BLAST_TICKS; }
    public void startPowerUpFx()  { powerUpFxTimer  = SCGFx.POWER_UP_TICKS; }
    public void startAlertFx()    { alertFxTimer    = SCGFx.ALERT_TICKS; }

    @Override
    public void shutDown() {
        setShutDown(true);
        setTarget(null);
        startEmpBlastFx();
    }

    @Override
    public void reactivate() {
        setShutDown(false);
        startPowerUpFx();
    }

    @Override
    public boolean isShutDown() { return entityData.get(SHUT_DOWN); }

    @Override
    public void setShutDown(boolean shutDown) { entityData.set(SHUT_DOWN, shutDown); }

    // -- Threat mode --
    public SecurityGolemEntity.ThreatMode getThreatMode() { return SecurityGolemEntity.ThreatMode.fromOrdinal(entityData.get(THREAT_MODE)); }
    public void setThreatMode(SecurityGolemEntity.ThreatMode m) { entityData.set(THREAT_MODE, m.ordinal()); }

    @Override
    public void setTarget(LivingEntity target) {
        if (isShutDown()) { super.setTarget(null); return; }
        boolean hadNoTarget = getTarget() == null;
        super.setTarget(target);
        if (hadNoTarget && target != null && level() instanceof ServerLevel) {
            startAlertFx();
            SCGFx.sound(level(), this, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 0.9F, 0.7F);
        }
    }

    // -- GuardCore.Host --
    @Override public int id() { return getId(); }
    @Override public void openLootMenu(ServerPlayer player) { SCGuardGolem.openContainer(player, this); }
    @Override public void onModulesChanged() { if (!level().isClientSide()) applyModuleEffects(); }

    @Override
    public void sendPasscodeScreen(ServerPlayer player, OpenScreen.DataType type) {
        OpenScreen pkt = new OpenScreen(type, getId());
        //? if forge {
        /*net.geforcemods.securitycraft.SecurityCraft.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), pkt);
        *///?} elif <1.21.1 {
        /*net.neoforged.neoforge.network.PacketDistributor.PLAYER.with(player).send(pkt);
        *///?} else {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
        //?}
    }

    private void applyModuleEffects() {
        AttributeInstance a = getAttribute(Attributes.ATTACK_DAMAGE);
        if (a != null) a.setBaseValue(baseAttackDamage + (core.hasHarmingModule() ? HARMING_DAMAGE_BONUS : 0));
        AttributeInstance s = getAttribute(Attributes.MOVEMENT_SPEED);
        if (s != null) s.setBaseValue(baseSpeed + (core.hasSpeedModule() ? SPEED_BONUS : 0));
    }

    private double baseAttackDamage = 3.0;
    private double baseSpeed = 0.23;

    /** Capture the source mob's stats at taming so modules add on top of them. */
    public void adoptStatsFrom(LivingEntity source) {
        AttributeInstance srcAtk = source.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance srcHp = source.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance srcSpd = source.getAttribute(Attributes.MOVEMENT_SPEED);
        if (srcAtk != null) baseAttackDamage = Math.max(baseAttackDamage, srcAtk.getBaseValue());
        if (srcSpd != null) baseSpeed = Math.max(0.2, srcSpd.getBaseValue());
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && srcHp != null) { hp.setBaseValue(Math.max(20.0, srcHp.getBaseValue() + 10.0)); setHealth((float) hp.getBaseValue()); }
        applyModuleEffects();
    }

    // -- Interaction: module install (Sentry-style) / reactivate / passcode loot --
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            ItemStack held = player.getItemInHand(hand);

            if (isShutDown() && held.getItem() == Items.REDSTONE) {
                if (isOwner(player)) {
                    reactivate();
                    if (!player.isCreative()) held.shrink(1);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            // Owner installs a SecurityCraft module by clicking with it.
            if (isOwner(player) && held.getItem() instanceof ModuleItem moduleItem) {
                int slot = moduleSlotFor(moduleItem.getModuleType());
                if (slot >= 0 && core.moduleContainer().getItem(slot).isEmpty()) {
                    ItemStack one = held.copy();
                    one.setCount(1);
                    core.moduleContainer().setItem(slot, one);
                    if (!player.isCreative()) held.shrink(1);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            // Bare-hand: passcode-gated loot; owner crouch-clicks to set the passcode.
            if (held.isEmpty()) {
                if (player.isCrouching() && isOwner(player)) {
                    if (player instanceof ServerPlayer sp) openSetPasscodeScreen(sp, blockPosition());
                } else if (getPasscode() == null) {
                    if (isOwner(player) && player instanceof ServerPlayer sp) SCGuardGolem.openContainer(sp, this);
                } else {
                    openPasscodeGUI(level(), blockPosition(), player);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static int moduleSlotFor(ModuleType type) {
        for (int i = 0; i < GuardCore.ACCEPTED_MODULES.length; i++)
            if (GuardCore.ACCEPTED_MODULES[i] == type) return i;
        return -1;
    }

    // -- MenuProvider: a plain chest GUI over the loot container --
    // No getDisplayName() override on purpose — it is the same method as
    // Entity.getDisplayName(), so overriding it would replace the mob's real name
    // (and any custom name tag) with a fixed string. Entity's implementation
    // satisfies MenuProvider and titles the loot chest correctly.

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        SimpleContainer loot = core.lootContainer();
        int rows = Math.max(1, loot.getContainerSize() / 9);
        MenuType<net.minecraft.world.inventory.ChestMenu> type = switch (rows) {
            case 6 -> MenuType.GENERIC_9x6;
            case 5 -> MenuType.GENERIC_9x5;
            case 4 -> MenuType.GENERIC_9x4;
            case 3 -> MenuType.GENERIC_9x3;
            case 2 -> MenuType.GENERIC_9x2;
            default -> MenuType.GENERIC_9x1;
        };
        return new net.minecraft.world.inventory.ChestMenu(type, containerId, playerInv, loot, rows);
    }

    //? if >=1.21.1 {
    @Override
    protected void dropAllDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        super.dropAllDeathLoot(level, source);
        dropContainer(core.moduleContainer());
        dropContainer(core.lootContainer());
    }
    //?} else {
    /*@Override
    protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source) {
        super.dropAllDeathLoot(source);
        dropContainer(core.moduleContainer());
        dropContainer(core.lootContainer());
    }
    *///?}

    private void dropContainer(SimpleContainer inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                //? if >=1.21.8 {
                if (level() instanceof net.minecraft.server.level.ServerLevel sl) spawnAtLocation(sl, s);
                //?} else
                /*spawnAtLocation(s);*/
            }
        }
        inv.clearContent();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) core.releaseSalt();
        super.remove(reason);
    }

    // -- Persistence (owner/shutdown/threat via inherited defaults + entity data; core for modules/loot) --
    //? if >=1.21.8 {
    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.store("GuardOwner", Owner.CODEC, getOwner());
        tag.putBoolean("ShutDown", isShutDown());
        tag.putInt("ThreatMode", getThreatMode().ordinal());
        tag.putDouble("BaseAtk", baseAttackDamage);
        tag.putDouble("BaseSpd", baseSpeed);
        savePasscodeAndSalt(tag);
        core.save(tag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER, tag.read("GuardOwner", Owner.CODEC).orElseGet(Owner::new));
        entityData.set(SHUT_DOWN, tag.getBooleanOr("ShutDown", false));
        entityData.set(THREAT_MODE, tag.getIntOr("ThreatMode", SecurityGolemEntity.ThreatMode.ATTACK.ordinal()));
        baseAttackDamage = tag.getDoubleOr("BaseAtk", baseAttackDamage);
        baseSpeed = tag.getDoubleOr("BaseSpd", baseSpeed);
        loadPasscodeAndSaltKey(tag);
        core.load(tag);
        applyModuleEffects();
    }
    //?} else {
    /*@Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag ownerTag = new CompoundTag();
        getOwner().save(ownerTag, true);
        tag.put("GuardOwner", ownerTag);
        tag.putBoolean("ShutDown", isShutDown());
        tag.putInt("ThreatMode", getThreatMode().ordinal());
        tag.putDouble("BaseAtk", baseAttackDamage);
        tag.putDouble("BaseSpd", baseSpeed);
        savePasscodeAndSalt(tag);
        core.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER, Owner.fromCompound(tag.getCompound("GuardOwner")));
        entityData.set(SHUT_DOWN, tag.getBoolean("ShutDown"));
        entityData.set(THREAT_MODE, tag.contains("ThreatMode") ? tag.getInt("ThreatMode") : SecurityGolemEntity.ThreatMode.ATTACK.ordinal());
        baseAttackDamage = tag.contains("BaseAtk") ? tag.getDouble("BaseAtk") : baseAttackDamage;
        baseSpeed = tag.contains("BaseSpd") ? tag.getDouble("BaseSpd") : baseSpeed;
        loadSaltKey(tag);
        loadPasscode(tag);
        core.load(tag);
        applyModuleEffects();
    }
    *///?}
}
