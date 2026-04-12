package net.geforcemods.scguardgolem.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.geforcemods.scguardgolem.entity.goal.BadgeCheckGoal;
import net.geforcemods.scguardgolem.entity.goal.PatrolGoal;
import net.geforcemods.scguardgolem.entity.goal.PlayerThreatGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SecurityGolemEntity extends IronGolem {

    private static final EntityDataAccessor<Boolean> PATROLLING =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER_UUID =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> THREAT_MODE =
            SynchedEntityData.defineId(SecurityGolemEntity.class, EntityDataSerializers.INT);

    private final List<BlockPos> waypoints = new ArrayList<>();
    private List<BlockPos> waypointsView;
    private int currentWaypointIndex = 0;
    private double patrolSpeed = 1.0;
    private final TreeSet<String> ignoreList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final TreeSet<String> alwaysAttackList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> ignoreListView = Collections.unmodifiableSet(ignoreList);
    private final Set<String> alwaysAttackListView = Collections.unmodifiableSet(alwaysAttackList);

    private int damageUpgrade = 0;
    private int speedUpgrade = 0;
    private int detectionUpgrade = 0;

    public static final int MAX_UPGRADE_LEVEL = 5;
    public static final double BASE_DETECTION_RADIUS = 16.0;
    public static final double DETECTION_RADIUS_PER_LEVEL = 4.0;
    public static final double DAMAGE_PER_LEVEL = 3.0;
    public static final double SPEED_PER_LEVEL = 0.03;

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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PATROLLING, false);
        builder.define(OWNER_UUID, "");
        builder.define(OWNER_NAME, "");
        builder.define(THREAT_MODE, ThreatMode.WARN.ordinal());
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
                (entity, level) -> entity instanceof Enemy && !(entity instanceof Creeper)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL_TICKS) scanTimer = 0;
        }
    }

    public boolean isScanTick() { return scanTimer == 0; }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (isOwner(player) || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                player.displayClientMessage(Component.literal("\u00a76[Security Golem] \u00a7fStatus:"), false);
                player.displayClientMessage(Component.literal("  Patrol: " + (isPatrolling() ? "\u00a7aActive" : "\u00a7cStopped")
                        + " \u00a7f(" + waypoints.size() + " waypoints)"), false);
                player.displayClientMessage(Component.literal("  Threat Mode: \u00a7e" + getThreatMode().name()), false);
                player.displayClientMessage(Component.literal("  Upgrades \u2014 DMG:" + damageUpgrade
                        + " SPD:" + speedUpgrade + " DET:" + detectionUpgrade), false);
                player.displayClientMessage(Component.literal("  Ignore list: " + ignoreList.size()
                        + " | Attack list: " + alwaysAttackList.size()), false);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.literal("\u00a7c[Security Golem] You are not the owner."), false);
            }
        }
        return InteractionResult.PASS;
    }

    // -- Owner --
    public void setGolemOwner(Player player) {
        entityData.set(OWNER_UUID, player.getGameProfile().id().toString());
        entityData.set(OWNER_NAME, player.getName().getString());
    }
    public String getOwnerUUID() { return entityData.get(OWNER_UUID); }
    public String getOwnerName() { return entityData.get(OWNER_NAME); }
    public boolean isOwner(Player player) {
        String uuid = getOwnerUUID();
        return !uuid.isEmpty() && uuid.equals(player.getGameProfile().id().toString());
    }

    // -- Patrol --
    public List<BlockPos> getWaypoints() {
        List<BlockPos> v = waypointsView;
        if (v == null || v.size() != waypoints.size()) { v = Collections.unmodifiableList(new ArrayList<>(waypoints)); waypointsView = v; }
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
    public BlockPos getCurrentWaypoint() { return waypoints.isEmpty() ? null : waypoints.get(currentWaypointIndex); }
    public void advanceWaypoint() { if (!waypoints.isEmpty()) currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size(); }
    public int getCurrentWaypointIndex() { return currentWaypointIndex; }
    public boolean isPatrolling() { return entityData.get(PATROLLING); }
    public void setPatrolling(boolean p) { entityData.set(PATROLLING, p); }
    public double getPatrolSpeed() { return patrolSpeed; }
    public void setPatrolSpeed(double s) { this.patrolSpeed = Math.max(0.1, Math.min(s, 3.0)); }

    // -- Threat Mode --
    public ThreatMode getThreatMode() { return ThreatMode.fromOrdinal(entityData.get(THREAT_MODE)); }
    public void setThreatMode(ThreatMode m) { entityData.set(THREAT_MODE, m.ordinal()); }

    // -- Player Lists --
    public Set<String> getIgnoreList() { return ignoreListView; }
    public boolean addToIgnoreList(String n) { return ignoreList.add(n); }
    public boolean removeFromIgnoreList(String n) { return ignoreList.remove(n); }
    public boolean isOnIgnoreList(String n) { return ignoreList.contains(n); }
    public Set<String> getAlwaysAttackList() { return alwaysAttackListView; }
    public boolean addToAlwaysAttackList(String n) { return alwaysAttackList.add(n); }
    public boolean removeFromAlwaysAttackList(String n) { return alwaysAttackList.remove(n); }
    public boolean isOnAlwaysAttackList(String n) { return alwaysAttackList.contains(n); }

    // -- Upgrades --
    public int getDamageUpgrade() { return damageUpgrade; }
    public int getSpeedUpgrade() { return speedUpgrade; }
    public int getDetectionUpgrade() { return detectionUpgrade; }
    public void setDamageUpgrade(int l) { damageUpgrade = Math.max(0, Math.min(l, MAX_UPGRADE_LEVEL)); applyUpgrades(); }
    public void setSpeedUpgrade(int l) { speedUpgrade = Math.max(0, Math.min(l, MAX_UPGRADE_LEVEL)); applyUpgrades(); }
    public void setDetectionUpgrade(int l) { detectionUpgrade = Math.max(0, Math.min(l, MAX_UPGRADE_LEVEL)); }
    public double getEffectiveDetectionRadius() { return BASE_DETECTION_RADIUS + detectionUpgrade * DETECTION_RADIUS_PER_LEVEL; }

    private void applyUpgrades() {
        AttributeInstance a = getAttribute(Attributes.ATTACK_DAMAGE);
        if (a != null) a.setBaseValue(15.0D + damageUpgrade * DAMAGE_PER_LEVEL);
        AttributeInstance s = getAttribute(Attributes.MOVEMENT_SPEED);
        if (s != null) s.setBaseValue(0.25D + speedUpgrade * SPEED_PER_LEVEL);
    }

    // -- Persistence --
    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("GolemOwnerUUID", getOwnerUUID());
        tag.putString("GolemOwnerName", getOwnerName());
        tag.putBoolean("Patrolling", isPatrolling());
        tag.putDouble("PatrolSpeed", patrolSpeed);
        tag.putInt("CurrentWaypointIndex", currentWaypointIndex);

        CompoundTag waypointTag = new CompoundTag();
        waypointTag.putInt("Count", waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            waypointTag.putInt("X" + i, wp.getX());
            waypointTag.putInt("Y" + i, wp.getY());
            waypointTag.putInt("Z" + i, wp.getZ());
        }
        tag.store("Waypoints", CompoundTag.CODEC, waypointTag);

        tag.putInt("ThreatMode", getThreatMode().ordinal());
        tag.putInt("DamageUpgrade", damageUpgrade);
        tag.putInt("SpeedUpgrade", speedUpgrade);
        tag.putInt("DetectionUpgrade", detectionUpgrade);

        CompoundTag listsTag = new CompoundTag();
        ListTag ignoreTag = new ListTag();
        for (String name : ignoreList) {
            CompoundTag e = new CompoundTag();
            e.putString("Name", name);
            ignoreTag.add(e);
        }
        listsTag.put("IgnoreList", ignoreTag);
        ListTag attackTag = new ListTag();
        for (String name : alwaysAttackList) {
            CompoundTag e = new CompoundTag();
            e.putString("Name", name);
            attackTag.add(e);
        }
        listsTag.put("AlwaysAttackList", attackTag);
        tag.store("PlayerLists", CompoundTag.CODEC, listsTag);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER_UUID, tag.getStringOr("GolemOwnerUUID", ""));
        entityData.set(OWNER_NAME, tag.getStringOr("GolemOwnerName", ""));
        entityData.set(PATROLLING, tag.getBooleanOr("Patrolling", false));
        patrolSpeed = tag.getDoubleOr("PatrolSpeed", 1.0);
        currentWaypointIndex = tag.getIntOr("CurrentWaypointIndex", 0);

        waypoints.clear();
        waypointsView = null;
        tag.read("Waypoints", CompoundTag.CODEC).ifPresent(wc -> {
            int count = wc.getIntOr("Count", 0);
            for (int i = 0; i < count; i++)
                waypoints.add(new BlockPos(wc.getIntOr("X" + i, 0), wc.getIntOr("Y" + i, 0), wc.getIntOr("Z" + i, 0)));
        });

        entityData.set(THREAT_MODE, tag.getIntOr("ThreatMode", ThreatMode.WARN.ordinal()));
        damageUpgrade = tag.getIntOr("DamageUpgrade", 0);
        speedUpgrade = tag.getIntOr("SpeedUpgrade", 0);
        detectionUpgrade = tag.getIntOr("DetectionUpgrade", 0);
        applyUpgrades();

        ignoreList.clear();
        alwaysAttackList.clear();
        tag.read("PlayerLists", CompoundTag.CODEC).ifPresent(lc -> {
            lc.getListOrEmpty("IgnoreList").forEach(e -> {
                if (e instanceof CompoundTag ct) {
                    String n = ct.getStringOr("Name", "");
                    if (!n.isEmpty()) ignoreList.add(n);
                }
            });
            lc.getListOrEmpty("AlwaysAttackList").forEach(e -> {
                if (e instanceof CompoundTag ct) {
                    String n = ct.getStringOr("Name", "");
                    if (!n.isEmpty()) alwaysAttackList.add(n);
                }
            });
        });

        if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
    }
}
