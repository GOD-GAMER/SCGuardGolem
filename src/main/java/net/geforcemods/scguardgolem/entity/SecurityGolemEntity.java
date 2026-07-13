package net.geforcemods.scguardgolem.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.goal.BadgeCheckGoal;
import net.geforcemods.scguardgolem.entity.goal.PatrolGoal;
import net.geforcemods.scguardgolem.entity.goal.PlayerThreatGoal;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import java.util.UUID;

import net.geforcemods.securitycraft.api.IEMPAffected;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasscodeProtected;
import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.api.SecurityCraftAPI;
import net.geforcemods.securitycraft.items.ModuleItem;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.SaltData;
import net.geforcemods.securitycraft.network.client.OpenScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
//? if >=1.21.11 {
import net.minecraft.world.entity.animal.golem.IronGolem;
//?} else
/*import net.minecraft.world.entity.animal.IronGolem;*/
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.8 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}
//? if >=1.21.1
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.AABB;

public class SecurityGolemEntity extends IronGolem implements MenuProvider, IOwnable, IModuleInventory, IEMPAffected, IPasscodeProtected {

    private static final EntityDataAccessor<Boolean> PATROLLING =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHUT_DOWN =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.BOOLEAN);
    // SecurityCraft Owner, synced to the client via its own EntityDataSerializer
    // (present on every SC version). Replaces the old owner-UUID/name string pair.
    private static final EntityDataAccessor<Owner> OWNER =
            SynchedEntityData.defineId(SecurityGolemEntity.class, Owner.getSerializer());
    private static final EntityDataAccessor<Integer> THREAT_MODE =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.INT);
    // SecurityCraft module inventory (api.IModuleInventory). Modules are BINARY —
    // present = full effect; SC modules don't stack.
    public static final ModuleType[] ACCEPTED_MODULES = {
        ModuleType.HARMING, ModuleType.SPEED, ModuleType.SMART, ModuleType.STORAGE,
        ModuleType.ALLOWLIST, ModuleType.DENYLIST
    };

    public static final int MODULE_SLOTS = ACCEPTED_MODULES.length;

    public static final int MAX_LOOT_ROWS = 6;
    public static final int BASE_LOOT_SLOTS = 9;
    public static final double BASE_DETECTION_RADIUS = 16.0;
    public static final double SMART_DETECTION_RADIUS = 32.0;
    public static final double BASE_ATTACK_DAMAGE = 15.0;
    public static final double HARMING_ATTACK_DAMAGE = 25.0;
    public static final double BASE_SPEED = 0.25;
    public static final double FAST_SPEED = 0.35;

    private final SimpleContainer moduleInventory = new SimpleContainer(MODULE_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            onModulesChanged();
        }
        //? if >=1.21.1 {
        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
        //?} else {
        /*@Override
        public int getMaxStackSize() {
            return 1;
        }
        *///?}
    };
    private SimpleContainer lootInventory = new SimpleContainer(BASE_LOOT_SLOTS);

    private final List<BlockPos> waypoints = new ArrayList<>();
    private final List<String> waypointNames = new ArrayList<>();
    private List<BlockPos> waypointsView;
    private int currentWaypointIndex = 0;
    private double patrolSpeed = 1.0;
    private int dwellTicks = 0;

    // Loot item filter (item registry names)
    private final LinkedHashSet<String> lootFilter = new LinkedHashSet<>();


    // Bell recall state
    private boolean recalling = false;

    // Patrol resume: index saved when combat starts
    private int savedWaypointIndex = 0;
    private boolean hadTarget = false;

    // SecurityCraft passcode protection for the loot (api.IPasscodeProtected)
    private byte[] passcode;
    private UUID saltKey;
    private boolean saveSalt = true;
    private long cooldownEnd = 0;
    private static final long PASSCODE_COOLDOWN_MS = 5000L;

    private int pickupCooldown = 0;

    public enum ThreatMode {
        WARN, FOLLOW, ATTACK;
        private static final ThreatMode[] VALUES = values();
        public static ThreatMode fromOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : WARN;
        }
    }

    public static final int SCAN_INTERVAL_TICKS = 20;
    private int scanTimer = 0;

    public SecurityGolemEntity(EntityType<? extends SecurityGolemEntity> type, Level level) {
        super(type, level);
    }

    //? if >=1.21.1 {
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PATROLLING, false);
        builder.define(SHUT_DOWN, false);
        builder.define(OWNER, new Owner());
        builder.define(THREAT_MODE, ThreatMode.WARN.ordinal());
    }
    //?} else {
    /*@Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PATROLLING, false);
        entityData.define(SHUT_DOWN, false);
        entityData.define(OWNER, new Owner());
        entityData.define(THREAT_MODE, ThreatMode.WARN.ordinal());
    }
    *///?}

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(3, new PatrolGoal(this));
        this.goalSelector.addGoal(4, new BadgeCheckGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new PlayerThreatGoal(this));
        //? if >=1.21.8 {
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 5, false, false,
                (entity, level) -> isHostileThreat(entity)));
        //?} else {
        /*this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 5, false, false,
                entity -> isHostileThreat(entity)));
        *///?}
    }

    /**
     * A hostile threat is any Enemy (bar Creepers) OR anything another mod registered
     * as sentry-attackable via SecurityCraft IMC — so the golem's threat logic stays
     * consistent with the Sentry and respects other addons' rules.
     */
    public boolean isHostileThreat(net.minecraft.world.entity.Entity entity) {
        if (isShutDown()) return false;
        if (entity instanceof Creeper) return false;
        if (entity instanceof Enemy) return true;
        return SecurityCraftAPI.getRegisteredSentryAttackTargetChecks().stream()
                .anyMatch(check -> check.canAttack(entity));
    }

    // -- IEMPAffected (responds to SecurityCraft EMP like the Sentry) --
    @Override
    public void shutDown() {
        IEMPAffected.super.shutDown();
        setTarget(null);
    }

    @Override
    public boolean isShutDown() { return entityData.get(SHUT_DOWN); }

    @Override
    public void setShutDown(boolean shutDown) { entityData.set(SHUT_DOWN, shutDown); }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL_TICKS) scanTimer = 0;
            if (pickupCooldown > 0) pickupCooldown--;
            else pickupNearbyItems();
        }
    }

    public boolean isScanTick() { return scanTimer == 0; }

    /** Track target acquisition so PatrolGoal can resume at saved waypoint after combat. */
    @Override
    public void setTarget(net.minecraft.world.entity.LivingEntity target) {
        if (isShutDown()) { super.setTarget(null); return; } // EMP'd golems don't fight
        boolean hadNoTarget = this.getTarget() == null;
        super.setTarget(target);
        if (!level().isClientSide()) {
            if (hadNoTarget && target != null) {
                // Save current waypoint when combat starts
                savedWaypointIndex = currentWaypointIndex;
                hadTarget = true;
            } else if (target == null && hadTarget) {
                // Combat ended — restore waypoint
                currentWaypointIndex = savedWaypointIndex;
                hadTarget = false;
            }
        }
    }

    // -- MenuProvider --
    @Override
    public Component getDisplayName() {
        return Component.translatable("scguardgolem.gui.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new GolemMenu(containerId, playerInv, this);
    }

    /** Extra menu data; used by writeClientSideData (NeoForge) / NetworkHooks (Forge 1.20.1). */
    public void writeMenuData(FriendlyByteBuf buf) {
        buf.writeInt(this.getId());
        buf.writeInt(getLootRows());
        // Waypoints
        buf.writeInt(waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            buf.writeInt(wp.getX());
            buf.writeInt(wp.getY());
            buf.writeInt(wp.getZ());
            buf.writeUtf(i < waypointNames.size() ? waypointNames.get(i) : "");
        }
        buf.writeInt(currentWaypointIndex);
    }

    //? if forge {
    /*// Forge 1.20.1: extra data goes through NetworkHooks.openScreen(…, this::writeMenuData)
    *///?} elif <1.21.1 {
    /*@Override
    public void writeClientSideData(AbstractContainerMenu menu, FriendlyByteBuf buf) {
        writeMenuData(buf);
    }
    *///?} else {
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        writeMenuData(buf);
    }
    //?}

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            // Reactivate an EMP-disabled golem with redstone (the Sentry pattern).
            if (isShutDown() && player.getItemInHand(hand).getItem() == net.minecraft.world.item.Items.REDSTONE) {
                if (SCGuardGolem.canConfigure(this, player)) {
                    reactivate();
                    if (!player.isCreative()) player.getItemInHand(hand).shrink(1);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
            // Bare-hand: passcode-gated loot access. Owner crouch-clicks to set the passcode.
            if (player.getItemInHand(hand).isEmpty()) {
                if (player.isCrouching() && isOwner(player)) promptSetPasscode(player);
                else openLoot(player);
                return InteractionResult.SUCCESS;
            }
            // Owner with any other item: open the full config menu.
            if (SCGuardGolem.canConfigure(this, player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    SCGuardGolem.openGolemMenu(serverPlayer, this);
                }
                return InteractionResult.SUCCESS;
            } else {
                SCGuardGolem.msg(player, Component.literal("§c[Security Golem] You are not the owner."));
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && getSaltKey() != null) SaltData.removeSalt(getSaltKey());
        super.remove(reason);
    }

    // -- Owner (SecurityCraft api.IOwnable) --
    public void setGolemOwner(Player player) {
        entityData.set(OWNER, new Owner(player));
    }

    @Override
    public Owner getOwner() {
        return entityData.get(OWNER);
    }

    @Override
    public void setOwner(String uuid, String name) {
        entityData.set(OWNER, new Owner(name, uuid));
    }

    // The IOwnable default casts `this` to BlockEntity; this is an entity, so no-op (Sentry does the same).
    @Override
    public void onOwnerChanged(BlockState state, Level level, BlockPos pos, Player player, Owner oldOwner, Owner newOwner) {}

    public String getOwnerUUID() { return getOwner().getUUID(); }
    public String getOwnerName() { return getOwner().getName(); }

    /** Owner check that also honours SecurityCraft teams (via IOwnable#isOwnedBy). */
    public boolean isOwner(Player player) {
        return isOwnedBy(player, false);
    }

    // -- Module Inventory (api.IModuleInventory) --
    /** The backing SimpleContainer (GUI slots + save/load use this; getInventory() is a view of it). */
    public SimpleContainer getModuleInventory() { return moduleInventory; }

    @Override
    public NonNullList<ItemStack> getInventory() {
        // Read-only view for SecurityCraft. Nothing writes through getInventory():
        // the GUI edits the SimpleContainer via slots; getModule/isAllowed only read.
        NonNullList<ItemStack> view = NonNullList.withSize(moduleInventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < moduleInventory.getContainerSize(); i++)
            view.set(i, moduleInventory.getItem(i));
        return view;
    }

    @Override
    public ModuleType[] acceptedModules() { return ACCEPTED_MODULES; }

    @Override
    public boolean isModuleEnabled(ModuleType module) { return !getModule(module).isEmpty(); }

    @Override
    public void toggleModuleState(ModuleType module, boolean shouldBeEnabled) {}

    @Override
    public Level myLevel() { return level(); }

    @Override
    public BlockPos myPos() { return blockPosition(); }

    // IModuleInventory's insert/remove defaults dirty a BlockEntity (no-op on an entity),
    // so reapply effects here instead.
    @Override
    public void onModuleInserted(ItemStack stack, ModuleType module, boolean toggled) { onModulesChanged(); }

    @Override
    public void onModuleRemoved(ItemStack stack, ModuleType module, boolean toggled) { onModulesChanged(); }

    /** A config slot accepts a ModuleItem whose type matches acceptedModules()[index]. */
    public static boolean isValidModuleForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!(stack.getItem() instanceof ModuleItem moduleItem)) return false;
        return slot >= 0 && slot < ACCEPTED_MODULES.length && moduleItem.getModuleType() == ACCEPTED_MODULES[slot];
    }

    private void onModulesChanged() {
        if (level() != null) {
            if (!level().isClientSide()) {
                applyUpgrades();
            }
            resizeLootInventory();
        }
    }

    // -- Module-driven effects (binary: present = full effect) --
    public boolean hasHarmingModule() { return isModuleEnabled(ModuleType.HARMING); }
    public boolean hasSpeedModule()   { return isModuleEnabled(ModuleType.SPEED); }
    public boolean hasSmartModule()   { return isModuleEnabled(ModuleType.SMART); }
    public boolean hasStorageModule() { return isModuleEnabled(ModuleType.STORAGE); }
    public double getEffectiveDetectionRadius() { return hasSmartModule() ? SMART_DETECTION_RADIUS : BASE_DETECTION_RADIUS; }

    private void applyUpgrades() {
        AttributeInstance a = getAttribute(Attributes.ATTACK_DAMAGE);
        if (a != null) a.setBaseValue(hasHarmingModule() ? HARMING_ATTACK_DAMAGE : BASE_ATTACK_DAMAGE);
        AttributeInstance s = getAttribute(Attributes.MOVEMENT_SPEED);
        if (s != null) s.setBaseValue(hasSpeedModule() ? FAST_SPEED : BASE_SPEED);
    }

    // -- Allow/deny via SecurityCraft ALLOWLIST/DENYLIST modules (api.IModuleInventory) --
    // isAllowed(Entity) / isDenied(Entity) are inherited defaults, backed by the list
    // module data (ListModuleData on 1.21.1+, ModuleItem NBT on 1.20.x — absorbed by SC).
    public boolean hasAllowlistModule() { return isModuleEnabled(ModuleType.ALLOWLIST); }
    public boolean hasDenylistModule()  { return isModuleEnabled(ModuleType.DENYLIST); }

    // -- Loot Inventory --
    public SimpleContainer getLootInventory() { return lootInventory; }
    public void setLootInventory(SimpleContainer inv) { this.lootInventory = inv; }

    public int getLootSlotCount() {
        int rows = hasStorageModule() ? MAX_LOOT_ROWS : 1;
        return rows * 9;
    }

    public int getLootRows() {
        return getLootSlotCount() / 9;
    }

    private void resizeLootInventory() {
        int needed = getLootSlotCount();
        if (lootInventory.getContainerSize() == needed) return;
        SimpleContainer newInv = new SimpleContainer(needed);
        for (int i = 0; i < Math.min(lootInventory.getContainerSize(), needed); i++) {
            newInv.setItem(i, lootInventory.getItem(i));
        }
        if (level() != null && !level().isClientSide()) {
            for (int i = needed; i < lootInventory.getContainerSize(); i++) {
                ItemStack overflow = lootInventory.getItem(i);
                if (!overflow.isEmpty() && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    //? if >=1.21.8 {
                    spawnAtLocation(serverLevel, overflow);
                    //?} else
                    /*spawnAtLocation(overflow);*/
                }
            }
        }
        lootInventory = newInv;
    }

    private int getLootItemCount() {
        int count = 0;
        for (int i = 0; i < lootInventory.getContainerSize(); i++)
            if (!lootInventory.getItem(i).isEmpty()) count++;
        return count;
    }

    // -- Loot Password --
    // -- Loot passcode (api.IPasscodeProtected) --
    @Override
    public byte[] getPasscode() { return passcode == null || passcode.length == 0 ? null : passcode; }

    @Override
    public void setPasscode(byte[] passcode) { this.passcode = passcode; }

    @Override
    public UUID getSaltKey() { return saltKey; }

    @Override
    public void setSaltKey(UUID saltKey) { this.saltKey = saltKey; }

    @Override
    public void setSaveSalt(boolean saveSalt) { this.saveSalt = saveSalt; }

    @Override
    public boolean shouldSaveSalt() { return saveSalt; }

    @Override
    public void startCooldown() {
        if (!isOnCooldown()) cooldownEnd = System.currentTimeMillis() + PASSCODE_COOLDOWN_MS;
    }

    @Override
    public long getCooldownEnd() { return cooldownEnd; }

    @Override
    public boolean isOnCooldown() { return System.currentTimeMillis() < cooldownEnd; }

    /** Called by SecurityCraft's CheckPasscode packet on a correct code — open the loot. */
    @Override
    public void activate(Player player) {
        if (player instanceof ServerPlayer serverPlayer) SCGuardGolem.openGolemMenu(serverPlayer, this);
    }

    // The passcode screens are pos-based in the API; an entity keys them on its id instead.
    @Override
    public void openPasscodeGUI(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && getPasscode() != null && player instanceof ServerPlayer serverPlayer)
            sendPasscodeScreen(serverPlayer, OpenScreen.DataType.CHECK_PASSCODE_FOR_ENTITY);
    }

    @Override
    public void openSetPasscodeScreen(ServerPlayer player, BlockPos pos) {
        sendPasscodeScreen(player, OpenScreen.DataType.SET_PASSCODE_FOR_ENTITY);
    }

    private void sendPasscodeScreen(ServerPlayer player, OpenScreen.DataType type) {
        OpenScreen pkt = new OpenScreen(type, getId());
        //? if forge {
        /*net.geforcemods.securitycraft.SecurityCraft.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), pkt);
        *///?} elif <1.21.1 {
        /*net.neoforged.neoforge.network.PacketDistributor.PLAYER.with(player).send(pkt);
        *///?} else {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, pkt);
        //?}
    }

    /** Owner opens loot directly (or sets a passcode); others must enter the passcode if one is set. */
    public void openLoot(Player player) {
        if (getPasscode() == null) {
            if (isOwner(player)) {
                if (player instanceof ServerPlayer serverPlayer) SCGuardGolem.openGolemMenu(serverPlayer, this);
            }
        } else {
            openPasscodeGUI(level(), blockPosition(), player);
        }
    }

    /** Owner-triggered: open the set-passcode screen for the loot lock. */
    public void promptSetPasscode(Player player) {
        if (isOwner(player) && player instanceof ServerPlayer serverPlayer)
            openSetPasscodeScreen(serverPlayer, blockPosition());
    }

    // -- Item Pickup --
    private void pickupNearbyItems() {
        if (!hasStorageModule()) return;
        AABB pickupBox = getBoundingBox().inflate(2.0);
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, pickupBox);
        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;
            ItemStack stack = itemEntity.getItem();
            // If a filter is active, only collect matching items
            if (!lootFilter.isEmpty() && !isFiltered(stack)) continue;
            ItemStack remainder = lootInventory.addItem(stack.copy());
            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remainder);
            }
        }
        pickupCooldown = 10;
    }

    // -- Bell Recall --
    public boolean isRecalling() { return recalling; }
    public void setRecalling(boolean r) { this.recalling = r; }

    public void recallToStart() {
        if (!waypoints.isEmpty()) {
            currentWaypointIndex = 0;
            recalling = true;
            setPatrolling(true);
        }
    }

    public void finishRecall() {
        recalling = false;
        setPatrolling(false);
    }

    // -- Patrol --
    public List<BlockPos> getWaypoints() {
        List<BlockPos> v = waypointsView;
        if (v == null || v.size() != waypoints.size()) { v = Collections.unmodifiableList(new ArrayList<>(waypoints)); waypointsView = v; }
        return v;
    }
    public void addWaypoint(BlockPos pos) { addWaypoint(pos, ""); }
    public void addWaypoint(BlockPos pos, String name) {
        waypoints.add(pos);
        waypointNames.add(name != null ? name : "");
        waypointsView = null;
    }
    public String getWaypointName(int index) {
        if (index < 0 || index >= waypointNames.size()) return "";
        return waypointNames.get(index);
    }
    public void setWaypointName(int index, String name) {
        if (index >= 0 && index < waypointNames.size())
            waypointNames.set(index, name != null ? name : "");
    }
    public boolean removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
            if (index < waypointNames.size()) waypointNames.remove(index);
            waypointsView = null;
            if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
            return true;
        }
        return false;
    }
    public void clearWaypoints() { waypoints.clear(); waypointNames.clear(); waypointsView = null; currentWaypointIndex = 0; }
    public BlockPos getCurrentWaypoint() { return waypoints.isEmpty() ? null : waypoints.get(currentWaypointIndex); }
    public void advanceWaypoint() { if (!waypoints.isEmpty()) currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size(); }
    public int getCurrentWaypointIndex() { return currentWaypointIndex; }
    public void setCurrentWaypointIndex(int idx) { currentWaypointIndex = (waypoints.isEmpty() ? 0 : Math.max(0, Math.min(idx, waypoints.size() - 1))); }
    public boolean isPatrolling() { return entityData.get(PATROLLING); }
    public void setPatrolling(boolean p) { entityData.set(PATROLLING, p); }
    public double getPatrolSpeed() { return patrolSpeed; }
    public void setPatrolSpeed(double s) { this.patrolSpeed = Math.max(0.1, Math.min(s, 3.0)); }

    // -- Dwell time --
    public int getDwellTicks() { return dwellTicks; }
    public void setDwellTicks(int ticks) { this.dwellTicks = Math.max(0, Math.min(ticks, 200)); }

    // -- Loot filter --
    public Set<String> getLootFilter() { return Collections.unmodifiableSet(lootFilter); }
    public void addLootFilter(String itemId) { if (itemId != null && !itemId.isBlank()) lootFilter.add(itemId); }
    public void removeLootFilter(String itemId) { lootFilter.remove(itemId); }
    public void clearLootFilter() { lootFilter.clear(); }
    public boolean isFiltered(ItemStack stack) {
        if (lootFilter.isEmpty() || stack.isEmpty()) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return lootFilter.contains(id);
    }

    // -- Threat Mode --
    public ThreatMode getThreatMode() { return ThreatMode.fromOrdinal(entityData.get(THREAT_MODE)); }
    public void setThreatMode(ThreatMode m) { entityData.set(THREAT_MODE, m.ordinal()); }

    // -- Drop everything on death --
    //? if >=1.21.1 {
    @Override
    protected void dropAllDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        super.dropAllDeathLoot(level, source);
        for (int i = 0; i < moduleInventory.getContainerSize(); i++) {
            ItemStack stack = moduleInventory.getItem(i);
            if (!stack.isEmpty()) dropStack(stack);
        }
        moduleInventory.clearContent();
        for (int i = 0; i < lootInventory.getContainerSize(); i++) {
            ItemStack stack = lootInventory.getItem(i);
            if (!stack.isEmpty()) dropStack(stack);
        }
        lootInventory.clearContent();
    }
    //?} else {
    /*@Override
    protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source) {
        super.dropAllDeathLoot(source);
        for (int i = 0; i < moduleInventory.getContainerSize(); i++) {
            ItemStack stack = moduleInventory.getItem(i);
            if (!stack.isEmpty()) dropStack(stack);
        }
        moduleInventory.clearContent();
        for (int i = 0; i < lootInventory.getContainerSize(); i++) {
            ItemStack stack = lootInventory.getItem(i);
            if (!stack.isEmpty()) dropStack(stack);
        }
        lootInventory.clearContent();
    }
    *///?}

    /** spawnAtLocation across the 1.21.2 ServerLevel-parameter change. */
    private void dropStack(ItemStack stack) {
        //? if >=1.21.8 {
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
            spawnAtLocation(serverLevel, stack);
        //?} else
        /*spawnAtLocation(stack);*/
    }

    // -- Persistence --
    //? if >=1.21.8 {
    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.store("GolemOwner", Owner.CODEC, getOwner());
        tag.putBoolean("Patrolling", isPatrolling());
        tag.putBoolean("ShutDown", isShutDown());
        tag.putDouble("PatrolSpeed", patrolSpeed);
        tag.putInt("CurrentWaypointIndex", currentWaypointIndex);
        tag.putInt("ThreatMode", getThreatMode().ordinal());
        savePasscodeAndSalt(tag);
        tag.putBoolean("Recalling", recalling);
        tag.putInt("DwellTicks", dwellTicks);
        tag.putInt("SavedWaypointIndex", savedWaypointIndex);

        // Loot filter
        CompoundTag filterTag = new CompoundTag();
        filterTag.putInt("Count", lootFilter.size());
        int fi = 0;
        for (String id : lootFilter) filterTag.putString("Filter" + fi++, id);
        tag.store("LootFilter", CompoundTag.CODEC, filterTag);

        CompoundTag waypointTag = new CompoundTag();
        waypointTag.putInt("Count", waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            waypointTag.putInt("X" + i, wp.getX());
            waypointTag.putInt("Y" + i, wp.getY());
            waypointTag.putInt("Z" + i, wp.getZ());
            waypointTag.putString("Name" + i, i < waypointNames.size() ? waypointNames.get(i) : "");
        }
        tag.store("Waypoints", CompoundTag.CODEC, waypointTag);

        moduleInventory.storeAsItemList(tag.list("Modules", ItemStack.CODEC));
        lootInventory.storeAsItemList(tag.list("LootItems", ItemStack.CODEC));
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        Owner golemOwner = tag.read("GolemOwner", Owner.CODEC).orElse(null);
        if (golemOwner == null) { // migrate legacy owner-string save data
            String legacyUuid = tag.getStringOr("GolemOwnerUUID", "");
            golemOwner = legacyUuid.isEmpty() ? new Owner()
                    : new Owner(tag.getStringOr("GolemOwnerName", ""), legacyUuid);
        }
        entityData.set(OWNER, golemOwner);
        entityData.set(PATROLLING, tag.getBooleanOr("Patrolling", false));
        entityData.set(SHUT_DOWN, tag.getBooleanOr("ShutDown", false));
        patrolSpeed = tag.getDoubleOr("PatrolSpeed", 1.0);
        currentWaypointIndex = tag.getIntOr("CurrentWaypointIndex", 0);
        entityData.set(THREAT_MODE, tag.getIntOr("ThreatMode", ThreatMode.WARN.ordinal()));
        loadPasscodeAndSaltKey(tag);
        String legacyPw = tag.getStringOr("LootPassword", "");
        if (!legacyPw.isEmpty() && getPasscode() == null) hashAndSetPasscode(legacyPw);
        recalling = tag.getBooleanOr("Recalling", false);
        dwellTicks = tag.getIntOr("DwellTicks", 0);
        savedWaypointIndex = tag.getIntOr("SavedWaypointIndex", 0);

        // Loot filter
        lootFilter.clear();
        tag.read("LootFilter", CompoundTag.CODEC).ifPresent(ft -> {
            int fc = ft.getIntOr("Count", 0);
            for (int i = 0; i < fc; i++) { String id = ft.getStringOr("Filter" + i, ""); if (!id.isEmpty()) lootFilter.add(id); }
        });

        waypoints.clear();
        waypointsView = null;
        tag.read("Waypoints", CompoundTag.CODEC).ifPresent(wc -> {
            int count = wc.getIntOr("Count", 0);
            for (int i = 0; i < count; i++) {
                waypoints.add(new BlockPos(wc.getIntOr("X" + i, 0), wc.getIntOr("Y" + i, 0), wc.getIntOr("Z" + i, 0)));
                waypointNames.add(wc.getStringOr("Name" + i, ""));
            }
        });

        moduleInventory.clearContent();
        moduleInventory.fromItemList(tag.listOrEmpty("Modules", ItemStack.CODEC));

        applyUpgrades();
        resizeLootInventory();

        lootInventory.clearContent();
        lootInventory.fromItemList(tag.listOrEmpty("LootItems", ItemStack.CODEC));

        if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
    }
    //?} elif >=1.21.1 {
    /*@Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag golemOwnerTag = new CompoundTag();
        getOwner().save(golemOwnerTag, true);
        tag.put("GolemOwner", golemOwnerTag);
        tag.putBoolean("Patrolling", isPatrolling());
        tag.putBoolean("ShutDown", isShutDown());
        tag.putDouble("PatrolSpeed", patrolSpeed);
        tag.putInt("CurrentWaypointIndex", currentWaypointIndex);
        tag.putInt("ThreatMode", getThreatMode().ordinal());
        savePasscodeAndSalt(tag);
        tag.putBoolean("Recalling", recalling);
        tag.putInt("DwellTicks", dwellTicks);
        tag.putInt("SavedWaypointIndex", savedWaypointIndex);

        CompoundTag filterTag = new CompoundTag();
        filterTag.putInt("Count", lootFilter.size());
        int fi = 0;
        for (String id : lootFilter) filterTag.putString("Filter" + fi++, id);
        tag.put("LootFilter", filterTag);

        CompoundTag waypointTag = new CompoundTag();
        waypointTag.putInt("Count", waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            waypointTag.putInt("X" + i, wp.getX());
            waypointTag.putInt("Y" + i, wp.getY());
            waypointTag.putInt("Z" + i, wp.getZ());
            waypointTag.putString("Name" + i, i < waypointNames.size() ? waypointNames.get(i) : "");
        }
        tag.put("Waypoints", waypointTag);

        net.minecraft.nbt.ListTag moduleList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < moduleInventory.getContainerSize(); i++) {
            ItemStack s = moduleInventory.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag it = (CompoundTag) s.save(registryAccess());
                it.putInt("Slot", i);
                moduleList.add(it);
            }
        }
        tag.put("Modules", moduleList);

        net.minecraft.nbt.ListTag lootList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < lootInventory.getContainerSize(); i++) {
            ItemStack s = lootInventory.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag it = (CompoundTag) s.save(registryAccess());
                it.putInt("Slot", i);
                lootList.add(it);
            }
        }
        tag.put("LootItems", lootList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readCommonTag(tag);

        moduleInventory.clearContent();
        if (tag.contains("Modules")) {
            net.minecraft.nbt.ListTag ml = tag.getList("Modules", 10);
            for (int i = 0; i < ml.size(); i++) {
                CompoundTag it = ml.getCompound(i);
                final int slot = it.getInt("Slot");
                ItemStack.parse(registryAccess(), it).ifPresent(s -> { if (slot >= 0 && slot < moduleInventory.getContainerSize()) moduleInventory.setItem(slot, s); });
            }
        }

        applyUpgrades();
        resizeLootInventory();

        lootInventory.clearContent();
        if (tag.contains("LootItems")) {
            net.minecraft.nbt.ListTag ll = tag.getList("LootItems", 10);
            for (int i = 0; i < ll.size(); i++) {
                CompoundTag it = ll.getCompound(i);
                final int slot = it.getInt("Slot");
                ItemStack.parse(registryAccess(), it).ifPresent(s -> { if (slot >= 0 && slot < lootInventory.getContainerSize()) lootInventory.setItem(slot, s); });
            }
        }

        if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
    }
    *///?} else {
    /*@Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag golemOwnerTag = new CompoundTag();
        getOwner().save(golemOwnerTag, true);
        tag.put("GolemOwner", golemOwnerTag);
        tag.putBoolean("Patrolling", isPatrolling());
        tag.putBoolean("ShutDown", isShutDown());
        tag.putDouble("PatrolSpeed", patrolSpeed);
        tag.putInt("CurrentWaypointIndex", currentWaypointIndex);
        tag.putInt("ThreatMode", getThreatMode().ordinal());
        savePasscodeAndSalt(tag);
        tag.putBoolean("Recalling", recalling);
        tag.putInt("DwellTicks", dwellTicks);
        tag.putInt("SavedWaypointIndex", savedWaypointIndex);

        CompoundTag filterTag = new CompoundTag();
        filterTag.putInt("Count", lootFilter.size());
        int fi = 0;
        for (String id : lootFilter) filterTag.putString("Filter" + fi++, id);
        tag.put("LootFilter", filterTag);

        CompoundTag waypointTag = new CompoundTag();
        waypointTag.putInt("Count", waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            waypointTag.putInt("X" + i, wp.getX());
            waypointTag.putInt("Y" + i, wp.getY());
            waypointTag.putInt("Z" + i, wp.getZ());
            waypointTag.putString("Name" + i, i < waypointNames.size() ? waypointNames.get(i) : "");
        }
        tag.put("Waypoints", waypointTag);

        net.minecraft.nbt.ListTag moduleList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < moduleInventory.getContainerSize(); i++) {
            ItemStack s = moduleInventory.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag it = s.save(new CompoundTag());
                it.putInt("Slot", i);
                moduleList.add(it);
            }
        }
        tag.put("Modules", moduleList);

        net.minecraft.nbt.ListTag lootList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < lootInventory.getContainerSize(); i++) {
            ItemStack s = lootInventory.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag it = s.save(new CompoundTag());
                it.putInt("Slot", i);
                lootList.add(it);
            }
        }
        tag.put("LootItems", lootList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readCommonTag(tag);

        moduleInventory.clearContent();
        if (tag.contains("Modules")) {
            net.minecraft.nbt.ListTag ml = tag.getList("Modules", 10);
            for (int i = 0; i < ml.size(); i++) {
                CompoundTag it = ml.getCompound(i);
                int slot = it.getInt("Slot");
                ItemStack s = ItemStack.of(it);
                if (!s.isEmpty() && slot >= 0 && slot < moduleInventory.getContainerSize()) moduleInventory.setItem(slot, s);
            }
        }

        applyUpgrades();
        resizeLootInventory();

        lootInventory.clearContent();
        if (tag.contains("LootItems")) {
            net.minecraft.nbt.ListTag ll = tag.getList("LootItems", 10);
            for (int i = 0; i < ll.size(); i++) {
                CompoundTag it = ll.getCompound(i);
                int slot = it.getInt("Slot");
                ItemStack s = ItemStack.of(it);
                if (!s.isEmpty() && slot >= 0 && slot < lootInventory.getContainerSize()) lootInventory.setItem(slot, s);
            }
        }

        if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
    }
    *///?}

    /** Shared CompoundTag reads for the pre-1.21.8 load paths. */
    //? if <1.21.8 {
    /*private void readCommonTag(CompoundTag tag) {
        CompoundTag golemOwnerTag = tag.getCompound("GolemOwner");
        if (!golemOwnerTag.isEmpty()) entityData.set(OWNER, Owner.fromCompound(golemOwnerTag));
        else { // migrate legacy owner-string save data
            String legacyUuid = tag.getString("GolemOwnerUUID");
            entityData.set(OWNER, legacyUuid.isEmpty() ? new Owner()
                    : new Owner(tag.getString("GolemOwnerName"), legacyUuid));
        }
        entityData.set(PATROLLING, tag.getBoolean("Patrolling"));
        entityData.set(SHUT_DOWN, tag.getBoolean("ShutDown"));
        patrolSpeed = tag.contains("PatrolSpeed") ? tag.getDouble("PatrolSpeed") : 1.0;
        currentWaypointIndex = tag.getInt("CurrentWaypointIndex");
        entityData.set(THREAT_MODE, tag.contains("ThreatMode") ? tag.getInt("ThreatMode") : ThreatMode.WARN.ordinal());
        loadSaltKey(tag);
        loadPasscode(tag);
        String legacyPw = tag.getString("LootPassword");
        if (!legacyPw.isEmpty() && getPasscode() == null) hashAndSetPasscode(legacyPw);
        recalling = tag.getBoolean("Recalling");
        dwellTicks = tag.getInt("DwellTicks");
        savedWaypointIndex = tag.getInt("SavedWaypointIndex");

        lootFilter.clear();
        if (tag.contains("LootFilter")) {
            CompoundTag ft = tag.getCompound("LootFilter");
            int fc = ft.getInt("Count");
            for (int i = 0; i < fc; i++) { String id = ft.getString("Filter" + i); if (!id.isEmpty()) lootFilter.add(id); }
        }

        waypoints.clear();
        waypointNames.clear();
        waypointsView = null;
        if (tag.contains("Waypoints")) {
            CompoundTag wc = tag.getCompound("Waypoints");
            int count = wc.getInt("Count");
            for (int i = 0; i < count; i++) {
                waypoints.add(new BlockPos(wc.getInt("X" + i), wc.getInt("Y" + i), wc.getInt("Z" + i)));
                waypointNames.add(wc.getString("Name" + i));
            }
        }
    }
    *///?}
}
