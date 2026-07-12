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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.11
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
//? if >=1.21.8
import net.minecraft.world.entity.EntitySpawnReason;
//? if >=1.21.11 {
import net.minecraft.world.entity.animal.golem.IronGolem;
//?} else
/*import net.minecraft.world.entity.animal.IronGolem;*/
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
*///?} elif <1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?}
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SCGuardGolem.MODID)
//? if forge {
/*@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID)
*///?} elif <1.21.1 {
/*@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID)
*///?} else {
@EventBusSubscriber(modid = SCGuardGolem.MODID)
//?}
public class SCGuardGolem {
    public static final String MODID = "scguardgolem";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean scLoaded;

    // --- Double-crouch state (server-side, per player) ---
    /** Game tick of the first crouch press, per player UUID. */
    private static final Map<UUID, Long> firstCrouchTick = new HashMap<>();
    /** Whether the player was crouching last tick. */
    private static final Map<UUID, Boolean> wasCrouching = new HashMap<>();
    /** Ticks within which a second crouch counts as a double-crouch (1.5 s). */
    private static final int DOUBLE_CROUCH_WINDOW = 30;
    /** How often (ticks) route particles are sent to the holding player. */
    private static final int PARTICLE_INTERVAL = 3;

    //? if forge {
    /*public SCGuardGolem() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        scLoaded = ModList.get().isLoaded("securitycraft");
        SCGContent.register(modBus);
        LOGGER.info("SecurityCraft Guard Golem addon initialized");
    }
    *///?} else {
    public SCGuardGolem(IEventBus modBus) {
        scLoaded = ModList.get().isLoaded("securitycraft");
        SCGContent.register(modBus);
        LOGGER.info("SecurityCraft Guard Golem addon initialized");
    }
    //?}

    /**
     * Sends a chat message to a player. Player#sendSystemMessage does not exist
     * on MC 1.21.10/1.21.11 (returned in 26.1); displayClientMessage covers the gap.
     */
    public static void msg(Player player, Component message) {
        //? if >=1.21.8 && <26.1 {
        /*player.displayClientMessage(message, false);
        *///?} else {
        player.sendSystemMessage(message);
        //?}
    }

    /** Owner-or-op permission check for opening the golem GUI. */
    public static boolean canConfigure(SecurityGolemEntity golem, Player player) {
        //? if >=1.21.11 {
        return golem.isOwner(player) || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //?} else
        /*return golem.isOwner(player) || player.hasPermissions(2);*/
    }

    /** Opens the golem menu with its extra client-side data on every loader. */
    public static void openGolemMenu(ServerPlayer serverPlayer, SecurityGolemEntity golem) {
        //? if forge {
        /*net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, golem, golem::writeMenuData);
        *///?} else {
        serverPlayer.openMenu(golem);
        //?}
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SCGCommands.register(event.getDispatcher());
    }

    // -----------------------------------------------------------------------
    //  Entity interact: Wire Cutters → GUI  |  Keycard → conversion
    // -----------------------------------------------------------------------
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        if (event.getTarget() instanceof SecurityGolemEntity golem && isWireCutters(held)) {
            if (canConfigure(golem, player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    openGolemMenu(serverPlayer, golem);
                }
            } else {
                msg(player, Component.literal("§c[Security Golem] You are not the owner."));
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (!(event.getTarget() instanceof IronGolem ironGolem)) return;
        if (event.getTarget() instanceof SecurityGolemEntity) return;
        if (!isKeycardItem(held)) return;

        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        //? if >=1.21.8 {
        SecurityGolemEntity golem = SCGContent.SECURITY_GOLEM.get()
                .create(serverLevel, EntitySpawnReason.CONVERSION);
        //?} else
        /*SecurityGolemEntity golem = SCGContent.SECURITY_GOLEM.get().create(serverLevel);*/
        if (golem == null) return;

        //? if >=1.21.8 {
        golem.snapTo(ironGolem.getX(), ironGolem.getY(), ironGolem.getZ(),
                ironGolem.getYRot(), ironGolem.getXRot());
        //?} else {
        /*golem.moveTo(ironGolem.getX(), ironGolem.getY(), ironGolem.getZ(),
                ironGolem.getYRot(), ironGolem.getXRot());
        *///?}
        golem.setHealth(ironGolem.getHealth());
        golem.setPlayerCreated(ironGolem.isPlayerCreated());
        golem.setGolemOwner(player);

        ironGolem.discard();
        serverLevel.addFreshEntity(golem);

        held.shrink(1);
        msg(player, Component.translatable("scguardgolem.conversion.success"));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    // -----------------------------------------------------------------------
    //  Bell right-click → recall owned golems
    // -----------------------------------------------------------------------
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
            msg(player, Component.literal("§6[Security Golem] §f" + golems.size() + " golem(s) recalled."));
        }
    }

    // -----------------------------------------------------------------------
    //  Player tick: double-crouch waypoint placement + route particles
    // -----------------------------------------------------------------------
    //? if forge || <1.21.1 {
    /*@SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        handlePlayerTick(event.player);
    }
    *///?} else {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        handlePlayerTick(event.getEntity());
    }
    //?}

    private static void handlePlayerTick(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        ItemStack held = player.getMainHandItem();
        UUID uid = player.getUUID();

        // Only active when holding Wire Cutters
        if (!isWireCutters(held)) {
            wasCrouching.remove(uid);
            firstCrouchTick.remove(uid);
            return;
        }

        // --- Double-crouch detection ---
        boolean crouching = player.isCrouching();
        boolean was = wasCrouching.getOrDefault(uid, false);

        if (crouching && !was) {
            // Rising edge: player just started crouching
            long now = serverLevel.getGameTime();
            Long first = firstCrouchTick.get(uid);

            if (first == null || (now - first) > DOUBLE_CROUCH_WINDOW) {
                // First crouch of the pair
                firstCrouchTick.put(uid, now);
            } else {
                // Second crouch within window → place waypoint
                firstCrouchTick.remove(uid);
                placeWaypoint(player, serverLevel);
            }
        }
        wasCrouching.put(uid, crouching);

        // --- Route particles (every PARTICLE_INTERVAL ticks) ---
        if (serverLevel.getGameTime() % PARTICLE_INTERVAL == 0) {
            showRouteParticles(player, serverLevel);
        }
    }

    // -----------------------------------------------------------------------
    //  Waypoint placement helper
    // -----------------------------------------------------------------------
    private static void placeWaypoint(Player player, ServerLevel level) {
        AABB box = new AABB(player.blockPosition()).inflate(64);
        SecurityGolemEntity nearest = level.getEntitiesOfClass(SecurityGolemEntity.class, box,
                        g -> g.isOwner(player))
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);

        if (nearest == null) {
            msg(player, Component.literal("§c[Route] No nearby Security Golem found (within 64 blocks)."));
            return;
        }

        BlockPos pos = player.blockPosition();
        nearest.addWaypoint(pos);
        int num = nearest.getWaypoints().size();
        msg(player, Component.literal(
                "§6[Route] §aWaypoint #" + num + " placed at §f" + pos.toShortString()));

        // Burst of happy-villager particles visible to the placing player
        if (player instanceof ServerPlayer sp) {
            particles(level, sp, ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    12, 0.3, 0.3, 0.3, 0.0);
        }
    }

    // -----------------------------------------------------------------------
    //  Route particle renderer (server → single player)
    // -----------------------------------------------------------------------
    private static void showRouteParticles(Player player, ServerLevel level) {
        if (!(player instanceof ServerPlayer sp)) return;

        AABB box = new AABB(player.blockPosition()).inflate(64);
        SecurityGolemEntity nearest = level.getEntitiesOfClass(SecurityGolemEntity.class, box,
                        g -> g.isOwner(player))
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);

        if (nearest == null) return;

        List<BlockPos> waypoints = nearest.getWaypoints();
        if (waypoints.isEmpty()) return;

        int currentIdx = nearest.getCurrentWaypointIndex();

        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos wp = waypoints.get(i);
            double wx = wp.getX() + 0.5;
            double wy = wp.getY() + 1.15;
            double wz = wp.getZ() + 0.5;

            // Node marker: flame = current target, end_rod = others
            if (i == currentIdx) {
                particles(level, sp, ParticleTypes.FLAME, wx, wy, wz, 3, 0.1, 0.1, 0.1, 0.0);
            } else {
                particles(level, sp, ParticleTypes.END_ROD, wx, wy, wz, 2, 0.08, 0.08, 0.08, 0.0);
            }

            // Line from this waypoint to the next (wraps around)
            if (waypoints.size() > 1) {
                BlockPos next = waypoints.get((i + 1) % waypoints.size());
                Vec3 from = new Vec3(wx, wy, wz);
                Vec3 to   = new Vec3(next.getX() + 0.5, next.getY() + 1.15, next.getZ() + 0.5);
                double dist = from.distanceTo(to);
                int steps = Math.max(1, Math.min((int) dist, 48));
                for (int s = 1; s < steps; s++) {
                    double t  = (double) s / steps;
                    double lx = from.x + (to.x - from.x) * t;
                    double ly = from.y + (to.y - from.y) * t;
                    double lz = from.z + (to.z - from.z) * t;
                    particles(level, sp, ParticleTypes.END_ROD, lx, ly, lz, 1, 0, 0, 0, 0.0);
                }
            }
        }

        // Preview dot at the player's own feet
        BlockPos pp = player.blockPosition();
        particles(level, sp, ParticleTypes.CRIT,
                pp.getX() + 0.5, pp.getY() + 0.3, pp.getZ() + 0.5,
                1, 0.15, 0.0, 0.15, 0.0);
    }


    /** ServerLevel#sendParticles gained a second boolean (alwaysShow) in 1.21.2. */
    private static void particles(ServerLevel level, ServerPlayer sp, net.minecraft.core.particles.SimpleParticleType type,
            double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        //? if >=1.21.8 {
        level.sendParticles(sp, type, true, true, x, y, z, count, dx, dy, dz, speed);
        //?} else
        /*level.sendParticles(sp, type, true, x, y, z, count, dx, dy, dz, speed);*/
    }

    // -----------------------------------------------------------------------
    //  Item helpers
    // -----------------------------------------------------------------------
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
