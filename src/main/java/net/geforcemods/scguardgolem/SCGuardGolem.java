package net.geforcemods.scguardgolem;

import java.util.List;

import net.geforcemods.scguardgolem.command.SCGCommands;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.securitycraft.items.KeycardItem;
import net.geforcemods.securitycraft.items.WireCuttersItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SCGuardGolem.MODID)
@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID)
public class SCGuardGolem {
    public static final String MODID = "scguardgolem";
    public static final String VERSION = "1.3.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean scLoaded;

    public SCGuardGolem() {
        scLoaded = ModList.get().isLoaded("securitycraft");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SCGContent.register(modBus);
        LOGGER.info("SecurityCraft Guard Golem addon initialized (MC 1.20.1)");
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SCGCommands.register(event.getDispatcher());
    }

    /**
     * Right-click a vanilla Iron Golem with any SecurityCraft keycard to
     * convert it into a Security Guard Golem.
     * Right-click a Security Guard Golem with Wire Cutters to open the GUI.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        // Wire Cutters on a Security Golem → open configuration GUI
        if (event.getTarget() instanceof SecurityGolemEntity golem && isWireCutters(held)) {
            if (golem.isOwner(player) || player.hasPermissions(2)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, golem, buf -> {
                        buf.writeInt(golem.getId());
                        buf.writeInt(golem.getLootRows());
                    });
                }
            } else {
                player.displayClientMessage(Component.literal("\u00a7c[Security Golem] You are not the owner."), false);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        // Keycard on a vanilla Iron Golem → convert to Security Golem
        if (!(event.getTarget() instanceof IronGolem ironGolem)) return;
        if (event.getTarget() instanceof SecurityGolemEntity) return;
        if (!isKeycardItem(held)) return;

        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        SecurityGolemEntity golem = SCGContent.SECURITY_GOLEM.get()
                .create(serverLevel);
        if (golem == null) return;

        golem.setPos(ironGolem.getX(), ironGolem.getY(), ironGolem.getZ());
        golem.setYRot(ironGolem.getYRot());
        golem.setXRot(ironGolem.getXRot());
        golem.setHealth(ironGolem.getHealth());
        golem.setPlayerCreated(ironGolem.isPlayerCreated());
        golem.setGolemOwner(player);

        ironGolem.discard();
        serverLevel.addFreshEntity(golem);

        held.shrink(1);
        player.displayClientMessage(
                Component.translatable("scguardgolem.conversion.success"), false);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /**
     * Right-click a bell → recall all owned golems to their first waypoint.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BellBlock)) return;

        Player player = event.getEntity();
        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        AABB searchBox = new AABB(event.getPos()).inflate(64);
        List<SecurityGolemEntity> golems = serverLevel.getEntitiesOfClass(SecurityGolemEntity.class, searchBox,
                g -> g.isOwner(player) && !g.getWaypoints().isEmpty());
        if (!golems.isEmpty()) {
            for (SecurityGolemEntity golem : golems) golem.recallToStart();
            player.displayClientMessage(Component.literal("\u00a76[Security Golem] \u00a7f" + golems.size() + " golem(s) recalled."), false);
        }
    }

    /**
     * Crouch + left-click with a reinforced lever → add waypoint at that block pos
     * for the nearest owned golem.
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getEntity();
        if (!player.isCrouching()) return;
        ItemStack held = player.getItemInHand(event.getHand());
        if (!isReinforcedLever(held)) return;

        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        AABB searchBox = new AABB(player.blockPosition()).inflate(32);
        List<SecurityGolemEntity> golems = serverLevel.getEntitiesOfClass(SecurityGolemEntity.class, searchBox,
                g -> g.isOwner(player));
        if (golems.isEmpty()) {
            player.displayClientMessage(Component.literal("\u00a7c[Security Golem] No nearby golem found."), false);
            return;
        }
        SecurityGolemEntity nearest = golems.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
        if (nearest != null) {
            nearest.addWaypoint(event.getPos());
            player.displayClientMessage(Component.literal("\u00a76[Security Golem] \u00a7fWaypoint #"
                    + (nearest.getWaypoints().size() - 1) + " added at " + event.getPos().toShortString() + "."), false);
            event.setCanceled(true);
        }
    }

    public static boolean isReinforcedLever(ItemStack stack) {
        if (!scLoaded || stack.isEmpty()) return false;
        try {
            return stack.getItem() instanceof net.minecraft.world.item.BlockItem bi
                    && bi.getBlock() instanceof net.geforcemods.securitycraft.blocks.reinforced.ReinforcedLeverBlock;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static boolean isKeycardItem(ItemStack stack) {
        if (!scLoaded || stack.isEmpty()) return false;
        try {
            return stack.getItem() instanceof KeycardItem;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static boolean isWireCutters(ItemStack stack) {
        if (!scLoaded || stack.isEmpty()) return false;
        try {
            return stack.getItem() instanceof WireCuttersItem;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static boolean isPlayerTrustedByOwner(String playerUUID, String playerName,
                                                  String ownerUUID, String ownerName) {
        if (!scLoaded || ownerUUID == null || ownerUUID.isEmpty()) return false;
        return ownerUUID.equals(playerUUID);
    }
}
