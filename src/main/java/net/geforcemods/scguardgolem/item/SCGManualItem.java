package net.geforcemods.scguardgolem.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import java.util.List;

public class SCGManualItem extends Item {

    public SCGManualItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.openItemGui(stack, hand);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static WrittenBookContent buildManualContent() {
        Style header = Style.EMPTY.withBold(true).withColor(ChatFormatting.DARK_AQUA);
        Style cmd = Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);
        Style body = Style.EMPTY.withColor(ChatFormatting.BLACK);
        Style dim = Style.EMPTY.withColor(ChatFormatting.GRAY);
        Style highlight = Style.EMPTY.withColor(ChatFormatting.GOLD);

        List<Filterable<Component>> pages = List.of(
                // Page 1 -- Title
                page(Component.empty()
                        .append(Component.literal("\n\n").withStyle(body))
                        .append(Component.literal("Security Guard\nGolem Manual").withStyle(header))
                        .append(Component.literal("\n\n").withStyle(body))
                        .append(Component.literal("v1.2 \u2014 Module &\nGUI Update").withStyle(highlight))
                        .append(Component.literal("\n\n").withStyle(body))
                        .append(Component.literal("A complete guide\nto your Security\nGolem.").withStyle(dim))),
                // Page 2 -- Getting Started
                page(Component.empty()
                        .append(Component.literal("Getting Started\n\n").withStyle(header))
                        .append(Component.literal("1. Build a vanilla\n   Iron Golem\n\n").withStyle(body))
                        .append(Component.literal("2. Hold any SC\n   Keycard\n\n").withStyle(body))
                        .append(Component.literal("3. Right-click the\n   Iron Golem\n\n").withStyle(body))
                        .append(Component.literal("The keycard is\nconsumed and you\nbecome its owner.").withStyle(dim))),
                // Page 3 -- Wire Cutters GUI
                page(Component.empty()
                        .append(Component.literal("Configuration GUI\n\n").withStyle(header))
                        .append(Component.literal("Use SC ").withStyle(body))
                        .append(Component.literal("Wire Cutters").withStyle(highlight))
                        .append(Component.literal("\nto right-click your\ngolem and open the\nconfiguration GUI.\n\n").withStyle(body))
                        .append(Component.literal("The GUI lets you:\n").withStyle(dim))
                        .append(Component.literal("- Insert modules\n- Toggle patrol\n- Set threat mode\n- Access loot chest").withStyle(dim))),
                // Page 4
                page(Component.empty()
                        .append(Component.literal("Module Upgrades\n\n").withStyle(header))
                        .append(Component.literal("Place SC modules in\nthe GUI slots.\nStack count = level\n(max 5).\n\n").withStyle(body))
                        .append(Component.literal("Harming Module\n").withStyle(highlight))
                        .append(Component.literal("+3 damage/level\n\n").withStyle(dim))
                        .append(Component.literal("Speed Module\n").withStyle(highlight))
                        .append(Component.literal("+0.03 speed/level\n\n").withStyle(dim))
                        .append(Component.literal("Smart Module\n").withStyle(highlight))
                        .append(Component.literal("+4 block detection\nradius per level\n\n").withStyle(dim))
                        .append(Component.literal("Storage Module\n").withStyle(highlight))
                        .append(Component.literal("Enables loot pickup\n+1 row per level").withStyle(dim))),
                // Page 5 -- Allow/Deny Lists
                page(Component.empty()
                        .append(Component.literal("Allow / Deny Lists\n\n").withStyle(header))
                        .append(Component.literal("Open the ").withStyle(body))
                        .append(Component.literal("Lists").withStyle(highlight))
                        .append(Component.literal(" tab in\nthe GUI.\n\n").withStyle(body))
                        .append(Component.literal("Look at a mob or\nplayer and click\n").withStyle(body))
                        .append(Component.literal("+ Allow").withStyle(highlight))
                        .append(Component.literal(" or ").withStyle(body))
                        .append(Component.literal("+ Deny").withStyle(highlight))
                        .append(Component.literal(".\n\n").withStyle(body))
                        .append(Component.literal("Click the ").withStyle(body))
                        .append(Component.literal("[x]").withStyle(highlight))
                        .append(Component.literal(" beside a\nname to remove it.").withStyle(dim))),
                // Page 6 -- Waypoints
                page(Component.empty()
                        .append(Component.literal("Setting Waypoints\n\n").withStyle(header))
                        .append(Component.literal("Hold a ").withStyle(body))
                        .append(Component.literal("Reinforced Lever").withStyle(highlight))
                        .append(Component.literal("\nand ").withStyle(body))
                        .append(Component.literal("crouch + left-click").withStyle(highlight))
                        .append(Component.literal("\nthe ground to add a\nwaypoint for the\nnearest golem.\n\n").withStyle(body))
                        .append(Component.literal("Use the ").withStyle(body))
                        .append(Component.literal("Clear Route").withStyle(highlight))
                        .append(Component.literal("\nbutton in the Config\ntab to delete all\nwaypoints.").withStyle(body))),
                // Page 7 -- Bell Recall
                page(Component.empty()
                        .append(Component.literal("Bell Recall\n\n").withStyle(header))
                        .append(Component.literal("Right-click a ").withStyle(body))
                        .append(Component.literal("Bell").withStyle(highlight))
                        .append(Component.literal("\nto recall all your\ngolems to their\nfirst waypoint.\n\n").withStyle(body))
                        .append(Component.literal("Recalled golems\nstop patrolling and\nwait at the start.\n\n").withStyle(dim))
                        .append(Component.literal("Use the ").withStyle(body))
                        .append(Component.literal("Patrol").withStyle(highlight))
                        .append(Component.literal(" button\nin the GUI to\nrestart patrol.").withStyle(body))),
                // Page 8 -- Patrol & Threats
                page(Component.empty()
                        .append(Component.literal("Patrol & Threats\n\n").withStyle(header))
                        .append(Component.literal("Toggle patrol and\nthreat mode in the\nGUI or via commands.\n\n").withStyle(body))
                        .append(Component.literal("/scgolem patrol\n  start | stop\n  speed <0.1-3.0>\n\n").withStyle(cmd))
                        .append(Component.literal("/scgolem threat\n  warn|follow|attack").withStyle(cmd))),
                // Page 9 -- Other Commands
                page(Component.empty()
                        .append(Component.literal("Other Commands\n\n").withStyle(header))
                        .append(Component.literal("/scgolem status\n").withStyle(cmd))
                        .append(Component.literal("Full status report\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem setowner\n").withStyle(cmd))
                        .append(Component.literal("Claim nearest golem\n\n").withStyle(dim))
                        .append(Component.literal("Trust Priority\n").withStyle(header))
                        .append(Component.literal("1. Denylist module\n2. Allowlist module\n3. Owner\n4. SC owner\n5. Threat mode").withStyle(body)))
        );

        return new WrittenBookContent(
                Filterable.passThrough("Security Golem Manual"),
                "SCGuardGolem", 0, pages, true);
    }

    private static Filterable<Component> page(Component content) {
        return Filterable.passThrough(content);
    }
}
