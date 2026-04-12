package net.geforcemods.scguardgolem.entity;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.SCGuardGolem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A lightweight camera entity that follows a Security Guard Golem, providing a first-person
 * view of the golem's perspective. Compatible with SecurityCraft's Camera Monitor when
 * the camera feature is enabled on the golem.
 *
 * The camera rides the golem as a passenger, positioned at eye height.
 */
public class GolemCameraEntity extends Entity {

    private static final EntityDataAccessor<Integer> GOLEM_ID =
            SynchedEntityData.defineId(GolemCameraEntity.class, EntityDataSerializers.INT);

    public GolemCameraEntity(EntityType<? extends GolemCameraEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setInvisible(true);
    }

    public GolemCameraEntity(Level level, SecurityGolemEntity golem) {
        this(SCGContent.GOLEM_CAMERA.get(), level);
        entityData.set(GOLEM_ID, golem.getId());
        setPos(golem.getX(), golem.getY() + golem.getEyeHeight(), golem.getZ());
        setRot(golem.getYRot(), golem.getXRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GOLEM_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            Entity golem = level().getEntity(entityData.get(GOLEM_ID));

            if (golem instanceof SecurityGolemEntity sg) {
                if (!sg.isAlive() || !sg.isCameraEnabled()) {
                    discard();
                    return;
                }
                setPos(sg.getX(), sg.getY() + sg.getEyeHeight(), sg.getZ());
                setRot(sg.getYRot(), sg.getXRot());
            } else {
                discard();
            }
        }
    }

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        // Camera entities are transient — don't persist
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        // Camera entities are transient
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    public int getGolemId() {
        return entityData.get(GOLEM_ID);
    }
}
