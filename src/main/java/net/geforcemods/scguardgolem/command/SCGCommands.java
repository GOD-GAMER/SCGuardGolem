package net.geforcemods.scguardgolem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity.ThreatMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SCGCommands {

    private static final double SEARCH_RANGE = 32.0;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scgolem").requires(src -> src.hasPermission(2))
                .then(Commands.literal("patrol")
                        .then(Commands.literal("start").executes(SCGCommands::patrolStart))
                        .then(Commands.literal("stop").executes(SCGCommands::patrolStop))
                        .then(Commands.literal("speed")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 3.0))
                                        .executes(SCGCommands::patrolSpeed)))
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
                .then(Commands.literal("upgrade")
                        .then(Commands.literal("damage")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, SecurityGolemEntity.MAX_UPGRADE_LEVEL))
                                        .executes(ctx -> setUpgrade(ctx, "damage"))))
                        .then(Commands.literal("speed")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, SecurityGolemEntity.MAX_UPGRADE_LEVEL))
                                        .executes(ctx -> setUpgrade(ctx, "speed"))))
                        .then(Commands.literal("detection")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, SecurityGolemEntity.MAX_UPGRADE_LEVEL))
                                        .executes(ctx -> setUpgrade(ctx, "detection")))))
                .then(Commands.literal("list")
                        .then(Commands.literal("ignore")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> listModify(ctx, "ignore", true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> listModify(ctx, "ignore", false)))))
                        .then(Commands.literal("attack")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> listModify(ctx, "attack", true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> listModify(ctx, "attack", false)))))
                        .then(Commands.literal("show").executes(SCGCommands::listShow)))
                .then(Commands.literal("status").executes(SCGCommands::showStatus))
                .then(Commands.literal("setowner").executes(SCGCommands::setOwner)));
    }

    private static SecurityGolemEntity requireGolem(CommandContext<CommandSourceStack> ctx) {
        Vec3 pos = ctx.getSource().getPosition();
        AABB box = new AABB(pos.x - SEARCH_RANGE, pos.y - SEARCH_RANGE, pos.z - SEARCH_RANGE,
                pos.x + SEARCH_RANGE, pos.y + SEARCH_RANGE, pos.z + SEARCH_RANGE);
        List<SecurityGolemEntity> golems = ctx.getSource().getLevel()
                .getEntitiesOfClass(SecurityGolemEntity.class, box);
        if (golems.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("scguardgolem.command.no_golem"));
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

    private static void msg(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal("\u00a76[Security Golem] \u00a7f" + text), false);
    }

    private static int patrolStart(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        if (g.getWaypoints().isEmpty()) { ctx.getSource().sendFailure(Component.literal("\u00a7cAdd waypoints first.")); return 0; }
        g.setPatrolling(true); msg(ctx, "Patrol \u00a7astarted\u00a7f."); return 1;
    }

    private static int patrolStop(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        g.setPatrolling(false); msg(ctx, "Patrol \u00a7cstopped\u00a7f."); return 1;
    }

    private static int patrolSpeed(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        double speed = DoubleArgumentType.getDouble(ctx, "value");
        g.setPatrolSpeed(speed); msg(ctx, "Patrol speed set to \u00a7e" + String.format("%.2f", speed) + "\u00a7f."); return 1;
    }

    private static int waypointAdd(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        BlockPos pos = new BlockPos(IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"), IntegerArgumentType.getInteger(ctx, "z"));
        g.addWaypoint(pos); msg(ctx, "Waypoint #" + (g.getWaypoints().size() - 1) + " added at " + pos.toShortString() + "."); return 1;
    }

    private static int waypointAddHere(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        BlockPos bp = BlockPos.containing(ctx.getSource().getPosition());
        g.addWaypoint(bp); msg(ctx, "Waypoint #" + (g.getWaypoints().size() - 1) + " added at " + bp.toShortString() + "."); return 1;
    }

    private static int waypointRemove(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        int index = IntegerArgumentType.getInteger(ctx, "index");
        if (g.removeWaypoint(index)) msg(ctx, "Waypoint #" + index + " removed.");
        else ctx.getSource().sendFailure(Component.literal("\u00a7cInvalid waypoint index.")); return 1;
    }

    private static int waypointClear(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        g.clearWaypoints(); g.setPatrolling(false); msg(ctx, "All waypoints cleared. Patrol stopped."); return 1;
    }

    private static int waypointList(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        List<BlockPos> wps = g.getWaypoints();
        if (wps.isEmpty()) { msg(ctx, "No waypoints set."); }
        else {
            msg(ctx, "Waypoints (" + wps.size() + "):");
            for (int i = 0; i < wps.size(); i++) {
                final int idx = i;
                String marker = (i == g.getCurrentWaypointIndex()) ? " \u00a7a<- current" : "";
                ctx.getSource().sendSuccess(() -> Component.literal("  \u00a77#" + idx + " \u00a7f" + wps.get(idx).toShortString() + marker), false);
            }
        }
        return 1;
    }

    private static int setThreatMode(CommandContext<CommandSourceStack> ctx, ThreatMode mode) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        g.setThreatMode(mode); msg(ctx, "Threat mode set to \u00a7e" + mode.name() + "\u00a7f."); return 1;
    }

    private static int setUpgrade(CommandContext<CommandSourceStack> ctx, String type) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        int level = IntegerArgumentType.getInteger(ctx, "level");
        switch (type) {
            case "damage" -> g.setDamageUpgrade(level);
            case "speed" -> g.setSpeedUpgrade(level);
            case "detection" -> g.setDetectionUpgrade(level);
        }
        msg(ctx, "Upgrade \u00a7e" + type + "\u00a7f set to level \u00a7e" + level + "\u00a7f."); return 1;
    }

    private static int listModify(CommandContext<CommandSourceStack> ctx, String listType, boolean add) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        String name = StringArgumentType.getString(ctx, "name");
        boolean success = "ignore".equals(listType) ? (add ? g.addToIgnoreList(name) : g.removeFromIgnoreList(name)) : (add ? g.addToAlwaysAttackList(name) : g.removeFromAlwaysAttackList(name));
        if (success) msg(ctx, (add ? "Added" : "Removed") + " \u00a7e" + name + "\u00a7f " + (add ? "to" : "from") + " " + listType + " list.");
        else ctx.getSource().sendFailure(Component.literal("\u00a7cPlayer " + name + " " + (add ? "already on" : "not on") + " " + listType + " list.")); return 1;
    }

    private static int listShow(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        msg(ctx, "\u00a7eIgnore list:\u00a7f " + (g.getIgnoreList().isEmpty() ? "(empty)" : String.join(", ", g.getIgnoreList())));
        msg(ctx, "\u00a7eAttack list:\u00a7f " + (g.getAlwaysAttackList().isEmpty() ? "(empty)" : String.join(", ", g.getAlwaysAttackList())));
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        msg(ctx, "\u00a76--- Security Golem Status ---");
        msg(ctx, "Owner: \u00a7e" + (g.getOwnerName().isEmpty() ? "(none)" : g.getOwnerName()));
        msg(ctx, "Health: \u00a7e" + String.format("%.1f", g.getHealth()) + "\u00a7f / \u00a7e" + String.format("%.1f", g.getMaxHealth()));
        msg(ctx, "Patrol: " + (g.isPatrolling() ? "\u00a7aActive" : "\u00a7cStopped") + " \u00a7f| Waypoints: \u00a7e" + g.getWaypoints().size() + " \u00a7f| Speed: \u00a7e" + String.format("%.2f", g.getPatrolSpeed()));
        msg(ctx, "Threat Mode: \u00a7e" + g.getThreatMode().name());
        msg(ctx, "Detection Radius: \u00a7e" + String.format("%.1f", g.getEffectiveDetectionRadius()) + " blocks");
        msg(ctx, "Upgrades - DMG:\u00a7e" + g.getDamageUpgrade() + "\u00a7f SPD:\u00a7e" + g.getSpeedUpgrade() + "\u00a7f DET:\u00a7e" + g.getDetectionUpgrade());
        return 1;
    }

    private static int setOwner(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        SecurityGolemEntity g = requireGolem(ctx); if (g == null) return 0;
        var player = ctx.getSource().getPlayerOrException();
        g.setGolemOwner(player); msg(ctx, "You are now the owner of this Security Golem."); return 1;
    }
}
