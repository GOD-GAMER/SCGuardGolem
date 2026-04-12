package net.geforcemods.scguardgolem.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.goal.BadgeCheckGoal;
import net.geforcemods.scguardgolem.entity.goal.PatrolGoal;
import net.geforcemods.scguardgolem.entity.goal.PlayerThreatGoal;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public class SecurityGolemEntity extends IronGolem {

    // --- Synched Data ---
    private static final EntityDataAccessor<Boolean> PATROLLING =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER_UUID =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> THREAT_MODE =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAMERA_ENABLED =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.BOOLEAN);

    // --- Patrol ---
    private final List<BlockPos> waypoints = new ArrayList<>();
    private List<BlockPos> waypointsView;
    private int currentWaypointIndex = 0;
    private double patrolSpeed = 1.0;

    // --- Player Lists (legacy, now backed by modules when available) ---
    private final TreeSet<String> ignoreList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final TreeSet<String> alwaysAttackList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> ignoreListView = Collections.unmodifiableSet(ignoreList);
    private final Set<String> alwaysAttackListView = Collections.unmodifiableSet(alwaysAttackList);

    // --- Module Inventory (6 slots) ---
    public static final int MODULE_SLOTS = 6;
    public static final int MODULE_ALLOWLIST = 0;
    public static final int MODULE_DENYLIST = 1;
    public static final int MODULE_HARMING = 2;
    public static final int MODULE_SPEED = 3;
    public static final int MODULE_SMART = 4;
    public static final int MODULE_STORAGE = 5;

    private final SimpleContainer moduleContainer = new SimpleContainer(MODULE_SLOTS) {
        @Override
        public int getMaxStackSize() { return 5; }

        @Override
        public void setChanged() {
            super.setChanged();
            onModulesChanged();
        }
    };

    // --- Loot Inventory (up to 27 slots, unlocked by storage module) ---
    public static final int MAX_LOOT_SLOTS = 27;
    public static final int LOOT_SLOTS_PER_LEVEL = 9;
    private final SimpleContainer lootContainer = new SimpleContainer(MAX_LOOT_SLOTS);

    // --- Chest Password ---
    private String chestPassword = "";

    // --- Upgrade Constants ---
    public static final int MAX_UPGRADE_LEVEL = 5;
    public static final double BASE_DETECTION_RADIUS = 16.0;
    public static final double DETECTION_RADIUS_PER_LEVEL = 4.0;
    public static final double DAMAGE_PER_LEVEL = 3.0;
    public static final double SPEED_PER_LEVEL = 0.03;

    // --- Cached upgrade levels (derived from module stack sizes) ---
    private int damageUpgrade = 0;
    private int speedUpgrade = 0;
    private int detectionUpgrade = 0;
    private int storageLevel = 0;

    // --- Threat Mode ---
    public enum ThreatMode {
        WARN, FOLLOW, ATTACK;
        private static final ThreatMode[] VALUES = values();
        public static ThreatMode fromOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : WARN;
        }
    }

    // --- Scan Timer ---
    public static final int SCAN_INTERVAL_TICKS = 20;
    private int scanTimer = 0;

    // --- Loot Pickup ---
    private static final int LOOT_PICKUP_INTERVAL = 40;
    private int lootPickupTimer = 0;

    public SecurityGolemEntity(EntityType<? extends SecurityGolemEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PATROLLING, false);
        builder.define(OWNER_UUID, "");
        builder.define(OWNER_NAME, "");
        builder.define(THREAT_MODE, ThreatMode.WARN.ordinal());
        builder.define(CAMERA_ENABLED, false);
    }

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
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 5, false, false,
                entity -> entity instanceof Enemy && !(entity instanceof Creeper)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL_TICKS) scanTimer = 0;

            // Loot pickup
            lootPickupTimer++;
            if (lootPickupTimer >= LOOT_PICKUP_INTERVAL) {
                lootPickupTimer = 0;
                if (storageLevel > 0) pickupNearbyLoot();
            }
        }
    }

    public boolean isScanTick() { return scanTimer == 0; }

    // === Interaction ===

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);

        // Wire cutters -> open config GUI (owner only)
        if (isWireCutters(held) && isOwner(player)) {
            openConfigScreen(player);
            return InteractionResult.SUCCESS;
        }

        // Empty hand sneak -> quick status
        if (held.isEmpty() && player.isShiftKeyDown() && isOwner(player)) {
            player.displayClientMessage(Component.literal("\u00a76[Security Golem] \u00a7fStatus:"), false);
            player.displayClientMessage(Component.literal("  Mode: " + getThreatMode().name()
                    + " | Patrol: " + (isPatrolling() ? "ON" : "OFF")
                    + " | Waypoints: " + waypoints.size()), false);
            player.displayClientMessage(Component.literal("  Modules: Harm=" + getModuleLevel(MODULE_HARMING)
                    + " Spd=" + getModuleLevel(MODULE_SPEED)
                    + " Smart=" + getModuleLevel(MODULE_SMART)
                    + " Store=" + storageLevel), false);
            player.displayClientMessage(Component.literal("  Camera: " + (isCameraEnabled() ? "ON" : "OFF")
                    + " | Loot: " + countLootItems() + " items"), false);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void openConfigScreen(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("gui.scguardgolem.golem_config");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player p) {
                    return new GolemMenu(containerId, playerInv, SecurityGolemEntity.this);
                }
            }, buf -> buf.writeInt(getId()));
        }
    }

    public void openLootScreen(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("gui.scguardgolem.golem_loot");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player p) {
                    return new GolemMenu(containerId, playerInv, SecurityGolemEntity.this);
                }
            }, buf -> buf.writeInt(getId()));
        }
    }

    // === Wire Cutters Detection ===

    private static boolean isWireCutters(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try {
            return stack.getItem() instanceof net.geforcemods.securitycraft.items.WireCuttersItem;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    // === Module System ===

    public Container getModuleContainer() { return moduleContainer; }
    public Container getLootContainer() { return lootContainer; }

    public int getModuleLevel(int slot) {
        ItemStack stack = moduleContainer.getItem(slot);
        return stack.isEmpty() ? 0 : Math.min(stack.getCount(), MAX_UPGRADE_LEVEL);
    }

    public int getUnlockedLootSlots() {
        return Math.min(storageLevel * LOOT_SLOTS_PER_LEVEL, MAX_LOOT_SLOTS);
    }

    private void onModulesChanged() {
        int oldStorage = storageLevel;

        // Recalculate upgrade levels from module stack sizes
        damageUpgrade = getModuleLevel(MODULE_HARMING);
        speedUpgrade = getModuleLevel(MODULE_SPEED);
        detectionUpgrade = getModuleLevel(MODULE_SMART);
        storageLevel = getModuleLevel(MODULE_STORAGE);

        applyUpgrades();

        // If storage level decreased, drop items from now-locked slots
        if (storageLevel < oldStorage && !level().isClientSide()) {
            int newMax = getUnlockedLootSlots();
            for (int i = newMax; i < MAX_LOOT_SLOTS; i++) {
                ItemStack excess = lootContainer.getItem(i);
                if (!excess.isEmpty()) {
                    Block.popResource(level(), blockPosition(), excess);
                    lootContainer.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        // Sync allowlist/denylist from modules
        syncListsFromModules();
    }

    private void syncListsFromModules() {
        try {
            // Allowlist module -> ignoreList
            ItemStack allowStack = moduleContainer.getItem(MODULE_ALLOWLIST);
            if (!allowStack.isEmpty() && allowStack.getItem() instanceof net.geforcemods.securitycraft.items.ModuleItem) {
                var data = allowStack.get(net.geforcemods.securitycraft.SCContent.LIST_MODULE_DATA.get());
                if (data != null) {
                    ignoreList.clear();
                    data.players().forEach(ignoreList::add);
                }
            }

            // Denylist module -> alwaysAttackList
            ItemStack denyStack = moduleContainer.getItem(MODULE_DENYLIST);
            if (!denyStack.isEmpty() && denyStack.getItem() instanceof net.geforcemods.securitycraft.items.ModuleItem) {
                var data = denyStack.get(net.geforcemods.securitycraft.SCContent.LIST_MODULE_DATA.get());
                if (data != null) {
                    alwaysAttackList.clear();
                    data.players().forEach(alwaysAttackList::add);
                }
            }
        } catch (NoClassDefFoundError ignored) {
            // SC not loaded
        }
    }

    private void applyUpgrades() {
        AttributeInstance a = getAttribute(Attributes.ATTACK_DAMAGE);
        if (a != null) a.setBaseValue(15.0D + damageUpgrade * DAMAGE_PER_LEVEL);
        AttributeInstance s = getAttribute(Attributes.MOVEMENT_SPEED);
        if (s != null) s.setBaseValue(0.25D + speedUpgrade * SPEED_PER_LEVEL);
    }

    // === Loot Pickup ===

    private void pickupNearbyLoot() {
        int maxSlots = getUnlockedLootSlots();
        if (maxSlots <= 0) return;

        AABB pickupBox = getBoundingBox().inflate(3.0);
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, pickupBox);

        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;
            ItemStack stack = itemEntity.getItem();
            ItemStack remaining = insertIntoLoot(stack, maxSlots);
            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remaining);
            }
        }
    }

    private ItemStack insertIntoLoot(ItemStack stack, int maxSlots) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < maxSlots && !remaining.isEmpty(); i++) {
            ItemStack slot = lootContainer.getItem(i);
            if (slot.isEmpty()) {
                lootContainer.setItem(i, remaining.copy());
                remaining.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int toAdd = Math.min(space, remaining.getCount());
                slot.grow(toAdd);
                remaining.shrink(toAdd);
                lootContainer.setItem(i, slot);
            }
        }
        return remaining;
    }

    private int countLootItems() {
        int count = 0;
        for (int i = 0; i < MAX_LOOT_SLOTS; i++) {
            if (!lootContainer.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    // === Camera ===

    public boolean isCameraEnabled() { return entityData.get(CAMERA_ENABLED); }
    public void setCameraEnabled(boolean enabled) { entityData.set(CAMERA_ENABLED, enabled); }

    // === Chest Password ===

    public String getChestPassword() { return chestPassword; }
    public void setChestPassword(String password) { this.chestPassword = password != null ? password : ""; }
    public boolean checkChestPassword(String input) {
        return chestPassword.isEmpty() || chestPassword.equals(input);
    }

    // === Owner ===

    public void setGolemOwner(Player player) {
        entityData.set(OWNER_UUID, player.getUUID().toString());
        entityData.set(OWNER_NAME, player.getName().getString());
    }
    public String getOwnerUUID() { return entityData.get(OWNER_UUID); }
    public String getOwnerName() { return entityData.get(OWNER_NAME); }
    public boolean isOwner(Player player) {
        String uuid = getOwnerUUID();
        return !uuid.isEmpty() && uuid.equals(player.getUUID().toString());
    }

    // === Patrol ===

    public List<BlockPos> getWaypoints() {
        List<BlockPos> v = waypointsView;
        if (v == null || v.size() != waypoints.size()) {
            v = Collections.unmodifiableList(new ArrayList<>(waypoints));
            waypointsView = v;
        }
        return v;
    }
    public void addWaypoint(BlockPos pos) { waypoints.add(pos); waypointsView = null; }
    public boolean removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
            waypointsView = null;
            if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
            return true;
        }
        return false;
    }
    public void clearWaypoints() { waypoints.clear(); waypointsView = null; currentWaypointIndex = 0; }
    public BlockPos getCurrentWaypoint() {
        return waypoints.isEmpty() ? null : waypoints.get(currentWaypointIndex);
    }
    public void advanceWaypoint() {
        currentWaypointIndex = (currentWaypointIndex + 1) % Math.max(1, waypoints.size());
    }
    public int getCurrentWaypointIndex() { return currentWaypointIndex; }
    public boolean isPatrolling() { return entityData.get(PATROLLING); }
    public void setPatrolling(boolean patrolling) { entityData.set(PATROLLING, patrolling); }
    public double getPatrolSpeed() { return patrolSpeed; }
    public void setPatrolSpeed(double speed) { this.patrolSpeed = Math.max(0.1, Math.min(speed, 3.0)); }

// === Threat Mode ===

    public ThreatMode getThreatMode() { return ThreatMode.fromOrdinal(entityData.get(THREAT_MODE)); }
    public void setThreatMode(ThreatMode mode) { entityData.set(THREAT_MODE, mode.ordinal()); }

    // === Player Lists ===

    public Set<String> getIgnoreList() { return ignoreListView; }
    public Set<String> getAlwaysAttackList() { return alwaysAttackListView; }
    public boolean addToIgnoreList(String name) { return ignoreList.add(name); }
    public boolean removeFromIgnoreList(String name) { return ignoreList.remove(name); }
    public boolean addToAlwaysAttackList(String name) { return alwaysAttackList.add(name); }
    public boolean removeFromAlwaysAttackList(String name) { return alwaysAttackList.remove(name); }
    public boolean isOnIgnoreList(String name) { return ignoreList.contains(name); }
    public boolean isOnAlwaysAttackList(String name) { return alwaysAttackList.contains(name); }

    // === Legacy Upgrade Getters (for commands) ===

    public int getDamageUpgrade() { return damageUpgrade; }
    public int getSpeedUpgrade() { return speedUpgrade; }
    public int getDetectionUpgrade() { return detectionUpgrade; }
    public void setDamageUpgrade(int l) { damageUpgrade = Math.max(0, Math.min(l, MAX_UPGRADE_LEVEL)); applyUpgrades(); }
    public void setSpeedUpgrade(int l) { speedUpgrade = Math.max(0, Math.min(l, MAX_UPGRADE_LEVEL)); applyUpgrades(); }
    public void setDetectionUpgrade(int l) { detectionUpgrade = Math.max(0, Math.min(l, MAX_UPGRADE_LEVEL)); }
    public double getEffectiveDetectionRadius() { return BASE_DETECTION_RADIUS + detectionUpgrade * DETECTION_RADIUS_PER_LEVEL; }

    // === Drop loot on death ===

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            // Drop all modules
            for (int i = 0; i < MODULE_SLOTS; i++) {
                ItemStack stack = moduleContainer.getItem(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level(), blockPosition(), stack);
                }
            }

            // Drop all loot
            for (int i = 0; i < MAX_LOOT_SLOTS; i++) {
                ItemStack stack = lootContainer.getItem(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level(), blockPosition(), stack);
                }
            }
        }
        super.remove(reason);
    }

    // === Persistence ===

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("GolemOwnerUUID", getOwnerUUID());
        tag.putString("GolemOwnerName", getOwnerName());
        tag.putBoolean("Patrolling", isPatrolling());
        tag.putDouble("PatrolSpeed", patrolSpeed);
        tag.putInt("CurrentWaypointIndex", currentWaypointIndex);
        tag.putBoolean("CameraEnabled", isCameraEnabled());
        tag.putString("ChestPassword", chestPassword);

        // Waypoints
        CompoundTag waypointTag = new CompoundTag();
        waypointTag.putInt("Count", waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            waypointTag.putInt("X" + i, wp.getX());
            waypointTag.putInt("Y" + i, wp.getY());
            waypointTag.putInt("Z" + i, wp.getZ());
        }
        tag.put("Waypoints", waypointTag);

        tag.putInt("ThreatMode", getThreatMode().ordinal());

        // Player lists (legacy backup)
        CompoundTag listsTag = new CompoundTag();
        ListTag ignoreTag = new ListTag();
        for (String name : ignoreList) { CompoundTag e = new CompoundTag(); e.putString("Name", name); ignoreTag.add(e); }
        listsTag.put("IgnoreList", ignoreTag);
        ListTag attackTag = new ListTag();
        for (String name : alwaysAttackList) { CompoundTag e = new CompoundTag(); e.putString("Name", name); attackTag.add(e); }
        listsTag.put("AlwaysAttackList", attackTag);
tag.put("PlayerLists", listsTag);

// Modules
CompoundTag modulesTag = new CompoundTag();
for (int i = 0; i < MODULE_SLOTS; i++) {
    ItemStack stack = moduleContainer.getItem(i);
    if (!stack.isEmpty()) {
        modulesTag.put("Slot" + i, stack.save(registryAccess()));
    }
}
tag.put("Modules", modulesTag);

// Loot
CompoundTag lootTag = new CompoundTag();
for (int i = 0; i < MAX_LOOT_SLOTS; i++) {
    ItemStack stack = lootContainer.getItem(i);
    if (!stack.isEmpty()) {
        lootTag.put("Slot" + i, stack.save(registryAccess()));
    }
}
tag.put("LootInventory", lootTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
entityData.set(OWNER_UUID, tag.getString("GolemOwnerUUID"));
entityData.set(OWNER_NAME, tag.getString("GolemOwnerName"));
entityData.set(PATROLLING, tag.getBoolean("Patrolling"));
patrolSpeed = tag.contains("PatrolSpeed") ? tag.getDouble("PatrolSpeed") : 1.0;
currentWaypointIndex = tag.getInt("CurrentWaypointIndex");
entityData.set(CAMERA_ENABLED, tag.getBoolean("CameraEnabled"));
chestPassword = tag.contains("ChestPassword") ? tag.getString("ChestPassword") : "";

        // Waypoints
        waypoints.clear();
        waypointsView = null;
        if (tag.contains("Waypoints", Tag.TAG_COMPOUND)) {
            CompoundTag wc = tag.getCompound("Waypoints");
            int count = wc.getInt("Count");
            for (int i = 0; i < count; i++)
                waypoints.add(new BlockPos(wc.getInt("X" + i), wc.getInt("Y" + i), wc.getInt("Z" + i)));
        }

entityData.set(THREAT_MODE, tag.getInt("ThreatMode"));
damageUpgrade = tag.getInt("DamageUpgrade");
speedUpgrade = tag.getInt("SpeedUpgrade");
detectionUpgrade = tag.getInt("DetectionUpgrade");
applyUpgrades();

        // Player lists (legacy)
        ignoreList.clear();
        alwaysAttackList.clear();
        if (tag.contains("PlayerLists", Tag.TAG_COMPOUND)) {
            CompoundTag lc = tag.getCompound("PlayerLists");
            ListTag il = lc.getList("IgnoreList", Tag.TAG_COMPOUND);
            for (int i = 0; i < il.size(); i++) { String n = il.getCompound(i).getString("Name"); if (!n.isEmpty()) ignoreList.add(n); }
            ListTag al = lc.getList("AlwaysAttackList", Tag.TAG_COMPOUND);
            for (int i = 0; i < al.size(); i++) { String n = al.getCompound(i).getString("Name"); if (!n.isEmpty()) alwaysAttackList.add(n); }
        }

        // Modules
        if (tag.contains("Modules", Tag.TAG_COMPOUND)) {
            CompoundTag mc = tag.getCompound("Modules");
            for (int i = 0; i < MODULE_SLOTS; i++) {
                String key = "Slot" + i;
                if (mc.contains(key, Tag.TAG_COMPOUND)) {
                    moduleContainer.setItem(i, ItemStack.parseOptional(registryAccess(), mc.getCompound(key)));
                }
            }
        }

        // Loot
        if (tag.contains("LootInventory", Tag.TAG_COMPOUND)) {
            CompoundTag lc = tag.getCompound("LootInventory");
            for (int i = 0; i < MAX_LOOT_SLOTS; i++) {
                String key = "Slot" + i;
                if (lc.contains(key, Tag.TAG_COMPOUND)) {
                    lootContainer.setItem(i, ItemStack.parseOptional(registryAccess(), lc.getCompound(key)));
                }
            }
        }

        // Recalculate from loaded modules
        onModulesChanged();

        if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
    }
}
