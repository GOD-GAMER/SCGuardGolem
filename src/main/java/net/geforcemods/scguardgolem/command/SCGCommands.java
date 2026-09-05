package net.geforcemods.scguardgolem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity.ThreatMode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
//? if >=1.21.11
import net.minecraft.server.permissions.Permissions;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SCGCommands {

    private static final double SEARCH_RANGE = 32.0;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scgolem")
                //? if >=1.21.11 {
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                //?} else
                /*.requires(src -> src.hasPermission(2))*/
                .then(Commands.literal("patrol")
                        .then(Commands.literal("start").executes(SCGCommands::patrolStart))
                        .then(Commands.literal("stop").executes(SCGCommands::patrolStop))
                        .then(Commands.literal("speed")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 3.0))
                                        .executes(SCGCommands::patrolSpeed)))
                        .then(Commands.literal("wander")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 8))
                                        .executes(SCGCommands::patrolWander)))
                        .then(Commands.literal("waypoint")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                .executes(SCGCommands::waypointAdd)))))
                                .then(Commands.literal("addhere").executes(SCGCommands::waypointAddHere))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                .executes(SCGCommands::waypointRemove)))
                                .then(Commands.literal("clear").executes(SCGCommands::waypointClear))
                                .then(Commands.literal("list").executes(SCGCommands::waypointList))))
                .then(Commands.literal("threat")
                        .then(Commands.literal("warn").executes(ctx -> setThreatMode(ctx, ThreatMode.WARN)))
                        .then(Commands.literal("follow").executes(ctx -> setThreatMode(ctx, ThreatMode.FOLLOW)))
                        .then(Commands.literal("attack").executes(ctx -> setThreatMode(ctx, ThreatMode.ATTACK))))
                .then(Commands.literal("status").executes(SCGCommands::showStatus))
                .then(Commands.literal("setowner").executes(SCGCommands::setOwner)));
    }

    private static SecurityGolemEntity requireGolem(CommandContext<CommandSourceStack> ctx) {
        Vec3 pos = ctx.getSource().getPosition();
        AABB box = new AABB(pos.x - SEARCH_RANGE, pos.y - SEARCH_RANGE, pos.z - SEARCH_RANGE,
                pos.x + SEARCH_RANGE, pos.y + SEARCH_RANGE, pos.z + SEARCH_RANGE);
        List<SecurityGolemEntity> golems = ctx.getSource().getLevel()
                .getEntitiesOfClass(SecurityGolemEntity.class, box, e -> true);
        if (golems.isEmpty()) {
            fail(ctx, "scguardgolem.command.no_golem");
            return null;
        }
        SecurityGolemEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (SecurityGolemEntity g : golems) {
            double d = g.distanceToSqr(pos);
            if (d < nearestDist) { nearestDist = d; nearest = g; }
        }
        return nearest;
    }

    /** Prefixed, coloured success line built from a translatable key + args. */
    private static void msg(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        Component body = Component.translatable(key, args).withStyle(ChatFormatting.WHITE);
        Component line = Component.translatable("scguardgolem.cmd.prefix").withStyle(ChatFormatting.GOLD).append(body);
        ctx.getSource().sendSuccess(() -> line, false);
    }

    private static void fail(CommandContext<CommandSourceStack> ctx, String key) {
        ctx.getSource().sendFailure(Component.translatable(key).withStyle(ChatFormatting.RED));
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value ? "scguardgolem.cmd.yes" : "scguardgolem.cmd.no");
    }

    private static int patrolStart(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        if (g.getWaypoints().isEmpty()) {
            fail(ctx, "scguardgolem.cmd.add_waypoints_first");
            return 0;
        }
        g.setPatrolling(true);
        msg(ctx, "scguardgolem.cmd.patrol_started");
        return 1;
    }

    private static int patrolStop(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        g.setPatrolling(false);
        msg(ctx, "scguardgolem.cmd.patrol_stopped");
        return 1;
    }

    private static int patrolSpeed(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        double speed = DoubleArgumentType.getDouble(ctx, "value");
        g.setPatrolSpeed(speed);
        msg(ctx, "scguardgolem.cmd.patrol_speed", String.format("%.2f", speed));
        return 1;
    }

    private static int patrolWander(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        g.setWanderRadius(radius);
        msg(ctx, "scguardgolem.cmd.wander_set", radius);
        return 1;
    }

    private static int waypointAdd(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        BlockPos pos = new BlockPos(
                IntegerArgumentType.getInteger(ctx, "x"),
                IntegerArgumentType.getInteger(ctx, "y"),
                IntegerArgumentType.getInteger(ctx, "z"));
        g.addWaypoint(pos);
        msg(ctx, "scguardgolem.cmd.waypoint_added", g.getWaypoints().size() - 1, pos.toShortString());
        return 1;
    }

    private static int waypointAddHere(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        BlockPos bp = BlockPos.containing(ctx.getSource().getPosition());
        g.addWaypoint(bp);
        msg(ctx, "scguardgolem.cmd.waypoint_added", g.getWaypoints().size() - 1, bp.toShortString());
        return 1;
    }

    private static int waypointRemove(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        int index = IntegerArgumentType.getInteger(ctx, "index");
        if (g.removeWaypoint(index)) msg(ctx, "scguardgolem.cmd.waypoint_removed", index);
        else fail(ctx, "scguardgolem.cmd.invalid_waypoint");
        return 1;
    }

    private static int waypointClear(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        g.clearWaypoints();
        g.setPatrolling(false);
        msg(ctx, "scguardgolem.cmd.waypoints_cleared");
        return 1;
    }

    private static int waypointList(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        List<BlockPos> wps = g.getWaypoints();
        if (wps.isEmpty()) {
            msg(ctx, "scguardgolem.cmd.no_waypoints");
        } else {
            msg(ctx, "scguardgolem.cmd.waypoints_header", wps.size());
            for (int i = 0; i < wps.size(); i++) {
                boolean current = (i == g.getCurrentWaypointIndex());
                Component entry = Component.translatable("scguardgolem.cmd.waypoint_entry", i, wps.get(i).toShortString())
                        .withStyle(current ? ChatFormatting.GREEN : ChatFormatting.GRAY);
                ctx.getSource().sendSuccess(() -> entry, false);
            }
        }
        return 1;
    }

    private static int setThreatMode(CommandContext<CommandSourceStack> ctx, ThreatMode mode) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        g.setThreatMode(mode);
        msg(ctx, "scguardgolem.cmd.threat_set", Component.translatable("scguardgolem.gui.threat." + mode.name()));
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        msg(ctx, "scguardgolem.cmd.status_header");
        msg(ctx, "scguardgolem.cmd.status_owner",
                g.getOwnerName().isEmpty() ? Component.translatable("scguardgolem.cmd.owner_none") : g.getOwnerName());
        msg(ctx, "scguardgolem.cmd.status_health",
                String.format("%.1f", g.getHealth()), String.format("%.1f", g.getMaxHealth()));
        msg(ctx, "scguardgolem.cmd.status_patrol",
                Component.translatable(g.isPatrolling() ? "scguardgolem.cmd.active" : "scguardgolem.cmd.stopped"),
                g.getWaypoints().size(), String.format("%.2f", g.getPatrolSpeed()));
        msg(ctx, "scguardgolem.cmd.status_threat", Component.translatable("scguardgolem.gui.threat." + g.getThreatMode().name()));
        msg(ctx, "scguardgolem.cmd.status_detection", String.format("%.1f", g.getEffectiveDetectionRadius()));
        msg(ctx, "scguardgolem.cmd.status_modules",
                yesNo(g.hasHarmingModule()), yesNo(g.hasSpeedModule()), yesNo(g.hasSmartModule()), yesNo(g.hasStorageModule()));
        msg(ctx, "scguardgolem.cmd.status_lists",
                Component.translatable(g.hasAllowlistModule() ? "scguardgolem.gui.lists.installed" : "scguardgolem.gui.lists.none"),
                Component.translatable(g.hasDenylistModule() ? "scguardgolem.gui.lists.installed" : "scguardgolem.gui.lists.none"));
        return 1;
    }

    private static int setOwner(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        SecurityGolemEntity g = requireGolem(ctx);
        if (g == null) return 0;
        var player = ctx.getSource().getPlayerOrException();
        g.setGolemOwner(player);
        msg(ctx, "scguardgolem.cmd.now_owner");
        return 1;
    }
}
