package net.geforcemods.scguardgolem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.geforcemods.scguardgolem.command.SCGCommands;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.securitycraft.items.KeycardItem;
import net.geforcemods.securitycraft.items.WireCuttersItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SCGuardGolem.MODID)
@EventBusSubscriber(modid = SCGuardGolem.MODID)
public class SCGuardGolem {
    public static final String MODID = "scguardgolem";
    public static final String VERSION = "1.2.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean scLoaded;

    // --- Double-crouch state (server-side, per player) ---
    private static final Map<UUID, Long> firstCrouchTick = new HashMap<>();
    private static final Map<UUID, Boolean> wasCrouching = new HashMap<>();
    /** Ticks within which a second crouch counts as a double-crouch (~1.5 s). */
    private static final int DOUBLE_CROUCH_WINDOW = 30;

    public SCGuardGolem(IEventBus modBus) {
        scLoaded = ModList.get().isLoaded("securitycraft");
        SCGContent.register(modBus);
        LOGGER.info("SecurityCraft Guard Golem addon initialized (MC 1.21.8)");
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
                    serverPlayer.openMenu(golem, buf -> {
                        buf.writeInt(golem.getId());
                        buf.writeInt(golem.getLootRows());
                        buf.writeInt(golem.getDwellTicks());
                        buf.writeInt(golem.getWaypoints().size());
                        for (int wi = 0; wi < golem.getWaypoints().size(); wi++) {
                            BlockPos wp = golem.getWaypoints().get(wi);
                            buf.writeInt(wp.getX());
                            buf.writeInt(wp.getY());
                            buf.writeInt(wp.getZ());
                            buf.writeUtf(golem.getWaypointName(wi));
                        }
                        buf.writeInt(golem.getCurrentWaypointIndex());
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
                .create(serverLevel, EntitySpawnReason.CONVERSION);
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

    // -----------------------------------------------------------------------
    //  Player tick: double-crouch with wire cutters → place waypoint
    // -----------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        UUID uid = player.getUUID();
        boolean hasWireCutters = isWireCutters(player.getMainHandItem())
                || isWireCutters(player.getOffhandItem());

        if (!hasWireCutters) {
            wasCrouching.remove(uid);
            firstCrouchTick.remove(uid);
            return;
        }

        boolean crouching = player.isCrouching();
        boolean was = wasCrouching.getOrDefault(uid, false);

        if (crouching && !was) {
            long now = serverLevel.getGameTime();
            Long first = firstCrouchTick.get(uid);
            if (first == null || (now - first) > DOUBLE_CROUCH_WINDOW) {
                firstCrouchTick.put(uid, now);
            } else {
                firstCrouchTick.remove(uid);
                placeWaypoint(player, serverLevel);
            }
        }
        wasCrouching.put(uid, crouching);
    }

    private static void placeWaypoint(Player player, ServerLevel level) {
        BlockPos pos = player.blockPosition();
        AABB searchBox = new AABB(pos).inflate(64);
        List<SecurityGolemEntity> owned = level.getEntitiesOfClass(
                SecurityGolemEntity.class, searchBox, g -> g.isOwner(player));
        if (owned.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("\u00a7c[Security Golem] No owned golem within 64 blocks."), false);
            return;
        }
        // Place on the closest owned golem
        SecurityGolemEntity golem = owned.stream()
                .min(java.util.Comparator.comparingDouble(g -> g.distanceToSqr(pos.getX(), pos.getY(), pos.getZ())))
                .orElse(owned.get(0));
        golem.addWaypoint(pos);
        player.displayClientMessage(Component.literal(
                "\u00a76[Security Golem] \u00a7fWaypoint #" + (golem.getWaypoints().size() - 1)
                + " added at " + pos.toShortString() + "."), false);
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
