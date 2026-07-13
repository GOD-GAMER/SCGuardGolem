package net.geforcemods.scguardgolem.item;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.SCGFx;
import net.geforcemods.securitycraft.api.IEMPAffected;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.8 {
import net.minecraft.world.InteractionResult;
//?} else
/*import net.minecraft.world.InteractionResultHolder;*/

/**
 * An EMP gun: a raycast device that shuts down any {@link IEMPAffected} target
 * (the guard golem, a tamed guard, SecurityCraft's Sentry) and lightly
 * incapacitates ordinary living entities with taser-style debuffs. It exists so
 * the golem's IEMPAffected shutdown/redstone-reactivate path is actually
 * exercisable in-world — SecurityCraft ships no EMP of its own.
 *
 * <p>Modelled on SecurityCraft's TaserItem (MIT); see CREDITS.md. Two states:
 * a base gun and a redstone-charged "powered" burst that reverts to the base
 * after one shot, mirroring the taser.
 */
public class EmpGunItem extends Item {
    private static final int RANGE = 11;
    private static final int SHOT_COST = 1;

    private final boolean powered;

    public EmpGunItem(Item.Properties properties, boolean powered) {
        super(properties);
        this.powered = powered;
    }

    //? if >=1.21.8 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        int code = fireOrCharge(level, player, hand);
        return code == 2 ? InteractionResult.CONSUME : code == 1 ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        int code = fireOrCharge(level, player, hand);
        ItemStack stack = player.getItemInHand(hand);
        return code == 2 ? InteractionResultHolder.consume(stack) : code == 1 ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
    }*/
    //?}

    /**
     * Shared logic. Returns a loader-neutral code the version-guarded {@code use}
     * wrapper maps to the era's result type: 0 = pass, 1 = charged (success,
     * no shot), 2 = fired (consume).
     */
    private int fireOrCharge(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Crouch = charge the base gun into the powered variant (creative toggles
        // freely; survival spends one redstone), matching the taser's charge step.
        if (player.isCrouching() && (player.isCreative() || !powered)) {
            boolean charged = false;
            if (player.isCreative()) {
                boolean base = stack.getItem() == SCGContent.EMP_GUN.get();
                player.setItemInHand(hand, new ItemStack(base ? SCGContent.EMP_GUN_POWERED.get() : SCGContent.EMP_GUN.get()));
                charged = true;
            } else if (consumeOneRedstone(player)) {
                player.setItemInHand(hand, new ItemStack(SCGContent.EMP_GUN_POWERED.get()));
                charged = true;
            }
            if (charged) {
                if (level instanceof ServerLevel sl) {
                    net.minecraft.world.phys.Vec3 e = player.getEyePosition(1.0F);
                    SCGFx.burst(sl, ParticleTypes.ELECTRIC_SPARK, e.x, e.y, e.z, 10, 0.25, 0.05);
                    SCGFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.4F);
                }
                return 1;
            }
            return 0;
        }

        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).scale(RANGE);
        Vec3 end = start.add(look);
        AABB box = player.getBoundingBox().expandTowards(look).inflate(1, 1, 1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box, LivingEntity.class::isInstance, RANGE * RANGE);

        // Muzzle flash + a spark beam traced from the barrel to the impact point.
        if (level instanceof ServerLevel sl) {
            SCGFx.burst(sl, ParticleTypes.ELECTRIC_SPARK, start.x, start.y, start.z, 6, 0.05, 0.02);
            SCGFx.burst(sl, ParticleTypes.SMOKE, start.x, start.y, start.z, 2, 0.05, 0.01);
            Vec3 tip = hit != null ? hit.getLocation() : end;
            int steps = Math.max(1, Math.min((int) start.distanceTo(tip), RANGE));
            for (int s = 1; s < steps; s++) {
                double t = (double) s / steps;
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        start.x + (tip.x - start.x) * t, start.y + (tip.y - start.y) * t, start.z + (tip.z - start.z) * t,
                        1, 0, 0, 0, 0);
            }
            SCGFx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.6F);
        }

        if (hit != null) {
            LivingEntity target = (LivingEntity) hit.getEntity();
            if (target instanceof IEMPAffected affected) {
                affected.shutDown(); // the target's own shutDown() plays the EMP burst + power-down sound
            } else {
                applyTaseEffects(target);
                if (level instanceof ServerLevel sl) {
                    SCGFx.burst(sl, ParticleTypes.CRIT, target, 8, 0.3, 0.1);
                    SCGFx.burst(sl, ParticleTypes.ELECTRIC_SPARK, target, 6, 0.3, 0.1);
                    SCGFx.sound(level, target, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F, 1.8F);
                }
            }
        }

        // Consume: the powered burst reverts to the base gun after one shot; the
        // base gun spends durability (mirrors the taser).
        if (!player.isCreative()) {
            if (powered) {
                ItemStack base = new ItemStack(SCGContent.EMP_GUN.get());
                hurtGun(base, player, hand);
                player.setItemInHand(hand, base);
            }
            else
                hurtGun(stack, player, hand);
        }
        return 2;
    }

    private void applyTaseEffects(LivingEntity target) {
        int duration = powered ? 400 : 200;
        int amplifier = powered ? 2 : 1;
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier));
        // Slowness + Nausea were renamed from MOVEMENT_SLOWDOWN / CONFUSION at 1.21.8.
        //? if >=1.21.8 {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier));
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration, 0));
        //?} else {
        /*target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));*/
        //?}
    }

    private void hurtGun(ItemStack stack, Player player, InteractionHand hand) {
        // Durability-damage slot arg: Consumer form pre-1.21.1, getSlotForHand on
        // 1.21.1-1.21.8, InteractionHand.asEquipmentSlot from 1.21.10 on.
        //? if >=1.21.10 {
        stack.hurtAndBreak(SHOT_COST, player, hand.asEquipmentSlot());
        //?} elif >=1.21.1 {
        /*stack.hurtAndBreak(SHOT_COST, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));*/
        //?} else
        /*stack.hurtAndBreak(SHOT_COST, player, p -> p.broadcastBreakEvent(hand));*/
    }

    private static boolean consumeOneRedstone(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && slot.getItem() == Items.REDSTONE) {
                slot.shrink(1);
                return true;
            }
        }
        return false;
    }
}
