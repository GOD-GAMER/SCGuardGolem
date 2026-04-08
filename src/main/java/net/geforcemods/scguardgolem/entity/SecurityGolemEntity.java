package net.geforcemods.scguardgolem.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.geforcemods.scguardgolem.entity.goal.BadgeCheckGoal;
import net.geforcemods.scguardgolem.entity.goal.PatrolGoal;
import net.geforcemods.scguardgolem.entity.goal.PlayerThreatGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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
    private int currentWaypointIndex = 0;
    private double patrolSpeed = 1.0;
    private final List<String> ignoreList = new ArrayList<>();
    private final List<String> alwaysAttackList = new ArrayList<>();

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PATROLLING, false);
        this.entityData.define(OWNER_UUID, "");
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(THREAT_MODE, ThreatMode.WARN.ordinal());
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
                (entity) -> entity instanceof Enemy && !(entity instanceof Creeper)));
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
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (isOwner(player) || player.hasPermissions(2)) {
                player.displayClientMessage(Component.literal("\u00a76[Security Golem] \u00a7fStatus:"), false);
                player.displayClientMessage(Component.literal("  Patrol: " + (isPatrolling() ? "\u00a7aActive" : "\u00a7cStopped")
                        + " \u00a7f(" + waypoints.size() + " waypoints)"), false);
                player.displayClientMessage(Component.literal("  Threat Mode: \u00a7e" + getThreatMode().name()), false);
                player.displayClientMessage(Component.literal("  Upgrades - DMG:" + damageUpgrade
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

    public void setGolemOwner(Player player) {
        entityData.set(OWNER_UUID, player.getGameProfile().getId().toString());
        entityData.set(OWNER_NAME, player.getName().getString());
    }
    public String getOwnerUUID() { return entityData.get(OWNER_UUID); }
    public String getOwnerName() { return entityData.get(OWNER_NAME); }
    public boolean isOwner(Player player) {
        String uuid = getOwnerUUID();
        return !uuid.isEmpty() && uuid.equals(player.getGameProfile().getId().toString());
    }

    public List<BlockPos> getWaypoints() { return Collections.unmodifiableList(waypoints); }
    public void addWaypoint(BlockPos pos) { waypoints.add(pos); }
    public boolean removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
            if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
            return true;
        }
        return false;
    }
    public void clearWaypoints() { waypoints.clear(); currentWaypointIndex = 0; }
    public BlockPos getCurrentWaypoint() { return waypoints.isEmpty() ? null : waypoints.get(currentWaypointIndex); }
    public void advanceWaypoint() { if (!waypoints.isEmpty()) currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size(); }
    public int getCurrentWaypointIndex() { return currentWaypointIndex; }
    public boolean isPatrolling() { return entityData.get(PATROLLING); }
    public void setPatrolling(boolean p) { entityData.set(PATROLLING, p); }
    public double getPatrolSpeed() { return patrolSpeed; }
    public void setPatrolSpeed(double s) { this.patrolSpeed = Math.max(0.1, Math.min(s, 3.0)); }

    public ThreatMode getThreatMode() { return ThreatMode.fromOrdinal(entityData.get(THREAT_MODE)); }
    public void setThreatMode(ThreatMode m) { entityData.set(THREAT_MODE, m.ordinal()); }

    public List<String> getIgnoreList() { return Collections.unmodifiableList(ignoreList); }
    public boolean addToIgnoreList(String n) { if (!ignoreList.contains(n)) { ignoreList.add(n); return true; } return false; }
    public boolean removeFromIgnoreList(String n) { return ignoreList.remove(n); }
    public boolean isOnIgnoreList(String n) { return ignoreList.stream().anyMatch(n::equalsIgnoreCase); }
    public List<String> getAlwaysAttackList() { return Collections.unmodifiableList(alwaysAttackList); }
    public boolean addToAlwaysAttackList(String n) { if (!alwaysAttackList.contains(n)) { alwaysAttackList.add(n); return true; } return false; }
    public boolean removeFromAlwaysAttackList(String n) { return alwaysAttackList.remove(n); }
    public boolean isOnAlwaysAttackList(String n) { return alwaysAttackList.stream().anyMatch(n::equalsIgnoreCase); }

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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
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
        tag.put("Waypoints", waypointTag);

        tag.putInt("ThreatMode", getThreatMode().ordinal());
        tag.putInt("DamageUpgrade", damageUpgrade);
        tag.putInt("SpeedUpgrade", speedUpgrade);
        tag.putInt("DetectionUpgrade", detectionUpgrade);

        CompoundTag listsTag = new CompoundTag();
        ListTag ignoreTag = new ListTag();
        for (String name : ignoreList) { CompoundTag e = new CompoundTag(); e.putString("Name", name); ignoreTag.add(e); }
        listsTag.put("IgnoreList", ignoreTag);
        ListTag attackTag = new ListTag();
        for (String name : alwaysAttackList) { CompoundTag e = new CompoundTag(); e.putString("Name", name); attackTag.add(e); }
        listsTag.put("AlwaysAttackList", attackTag);
        tag.put("PlayerLists", listsTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER_UUID, tag.getString("GolemOwnerUUID"));
        entityData.set(OWNER_NAME, tag.getString("GolemOwnerName"));
        entityData.set(PATROLLING, tag.getBoolean("Patrolling"));
        patrolSpeed = tag.contains("PatrolSpeed") ? tag.getDouble("PatrolSpeed") : 1.0;
        currentWaypointIndex = tag.getInt("CurrentWaypointIndex");

        waypoints.clear();
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

        ignoreList.clear();
        alwaysAttackList.clear();
        if (tag.contains("PlayerLists", Tag.TAG_COMPOUND)) {
            CompoundTag lc = tag.getCompound("PlayerLists");
            ListTag il = lc.getList("IgnoreList", Tag.TAG_COMPOUND);
            for (int i = 0; i < il.size(); i++) { String n = il.getCompound(i).getString("Name"); if (!n.isEmpty()) ignoreList.add(n); }
            ListTag al = lc.getList("AlwaysAttackList", Tag.TAG_COMPOUND);
            for (int i = 0; i < al.size(); i++) { String n = al.getCompound(i).getString("Name"); if (!n.isEmpty()) alwaysAttackList.add(n); }
        }

        if (currentWaypointIndex >= waypoints.size()) currentWaypointIndex = 0;
    }
}
