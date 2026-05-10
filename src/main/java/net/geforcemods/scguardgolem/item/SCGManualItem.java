package net.geforcemods.scguardgolem.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.openItemGui(stack, hand);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    public static WrittenBookContent buildManualContent() {
        Style header = Style.EMPTY.withBold(true).withColor(ChatFormatting.DARK_AQUA);
        Style cmd = Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);
        Style body = Style.EMPTY.withColor(ChatFormatting.BLACK);
        Style dim = Style.EMPTY.withColor(ChatFormatting.GRAY);
        Style highlight = Style.EMPTY.withColor(ChatFormatting.GOLD);

        List<Filterable<Component>> pages = List.of(
                // Page 1 — Title
                page(Component.empty()
                        .append(Component.literal("\n\n").withStyle(body))
                        .append(Component.literal("Security Guard\nGolem Manual").withStyle(header))
                        .append(Component.literal("\n\n").withStyle(body))
                        .append(Component.literal("v1.4 — Route &\nDwell Update").withStyle(highlight))
                        .append(Component.literal("\n\n").withStyle(body))
                        .append(Component.literal("A complete guide\nto your Security\nGolem.").withStyle(dim))),
                // Page 2 — Getting Started
                page(Component.empty()
                        .append(Component.literal("Getting Started\n\n").withStyle(header))
                        .append(Component.literal("1. Build a vanilla\n   Iron Golem\n\n").withStyle(body))
                        .append(Component.literal("2. Hold any SC\n   Keycard\n\n").withStyle(body))
                        .append(Component.literal("3. Right-click the\n   Iron Golem\n\n").withStyle(body))
                        .append(Component.literal("The keycard is\nconsumed and you\nbecome its owner.").withStyle(dim))),
                // Page 3 — 4-Tab GUI
                page(Component.empty()
                        .append(Component.literal("Configuration GUI\n\n").withStyle(header))
                        .append(Component.literal("Right-click your\ngolem with SC ").withStyle(body))
                        .append(Component.literal("Wire\nCutters").withStyle(highlight))
                        .append(Component.literal(" to open\nthe 4-tab GUI:\n\n").withStyle(body))
                        .append(Component.literal("Config").withStyle(highlight))
                        .append(Component.literal(" - Modules,\n  patrol, threat\n").withStyle(dim))
                        .append(Component.literal("Loot").withStyle(highlight))
                        .append(Component.literal(" - Loot inventory\n").withStyle(dim))
                        .append(Component.literal("Lists").withStyle(highlight))
                        .append(Component.literal(" - Allow/Deny\n").withStyle(dim))
                        .append(Component.literal("Route").withStyle(highlight))
                        .append(Component.literal(" - Waypoints &\n  dwell time").withStyle(dim))),
                // Page 4 — Module Upgrades
                page(Component.empty()
                        .append(Component.literal("Module Upgrades\n\n").withStyle(header))
                        .append(Component.literal("Place SC modules in\nthe GUI slots.\nStack count = level\n(max 5).\n\n").withStyle(body))
                        .append(Component.literal("Harming  ").withStyle(highlight))
                        .append(Component.literal("+3 dmg/lvl\n").withStyle(dim))
                        .append(Component.literal("Speed    ").withStyle(highlight))
                        .append(Component.literal("+0.03 spd/lvl\n").withStyle(dim))
                        .append(Component.literal("Smart    ").withStyle(highlight))
                        .append(Component.literal("+4 range/lvl\n").withStyle(dim))
                        .append(Component.literal("Storage  ").withStyle(highlight))
                        .append(Component.literal("+1 loot row/lvl\n").withStyle(dim))
                        .append(Component.literal("Allowlist ").withStyle(highlight))
                        .append(Component.literal("Allow list\n").withStyle(dim))
                        .append(Component.literal("Denylist  ").withStyle(highlight))
                        .append(Component.literal("Deny list").withStyle(dim))),
                // Page 5 — Setting Waypoints
                page(Component.empty()
                        .append(Component.literal("Setting Waypoints\n\n").withStyle(header))
                        .append(Component.literal("Hold SC ").withStyle(body))
                        .append(Component.literal("Wire Cutters").withStyle(highlight))
                        .append(Component.literal(".\n\n").withStyle(body))
                        .append(Component.literal("Crouch twice").withStyle(highlight))
                        .append(Component.literal(" within\n1.5 seconds to\nplace a waypoint\nat your feet.\n\n").withStyle(body))
                        .append(Component.literal("A particle burst\nconfirms placement.\nThe golem must be\nwithin 64 blocks.").withStyle(dim))),
                // Page 6 — Route Tab & Dwell
                page(Component.empty()
                        .append(Component.literal("Route Tab\n\n").withStyle(header))
                        .append(Component.literal("Open the ").withStyle(body))
                        .append(Component.literal("Route").withStyle(highlight))
                        .append(Component.literal(" tab to:\n\n").withStyle(body))
                        .append(Component.literal("- View all waypoints\n  with coordinates\n").withStyle(body))
                        .append(Component.literal("- Remove any entry\n  with the [x] button\n").withStyle(body))
                        .append(Component.literal("- Set ").withStyle(body))
                        .append(Component.literal("Dwell Time").withStyle(highlight))
                        .append(Component.literal(":\n  seconds to pause\n  at each waypoint\n  (").withStyle(body))
                        .append(Component.literal("[-]").withStyle(highlight))
                        .append(Component.literal(" / ").withStyle(body))
                        .append(Component.literal("[+]").withStyle(highlight))
                        .append(Component.literal(" buttons)").withStyle(body))),
                // Page 7 — Route Particle Preview
                page(Component.empty()
                        .append(Component.literal("Route Visualizer\n\n").withStyle(header))
                        .append(Component.literal("While holding ").withStyle(body))
                        .append(Component.literal("Wire\nCutters").withStyle(highlight))
                        .append(Component.literal(", a live\nparticle trail shows\nyour nearest golem's\nroute:\n\n").withStyle(body))
                        .append(Component.literal("Orange flame").withStyle(highlight))
                        .append(Component.literal(" =\n  current target\n").withStyle(dim))
                        .append(Component.literal("White rods").withStyle(highlight))
                        .append(Component.literal(" =\n  other waypoints\n").withStyle(dim))
                        .append(Component.literal("Crit dot").withStyle(highlight))
                        .append(Component.literal(" = your\n  next drop point").withStyle(dim))),
                // Page 8 — Allow/Deny Lists
                page(Component.empty()
                        .append(Component.literal("Allow / Deny Lists\n\n").withStyle(header))
                        .append(Component.literal("Open the ").withStyle(body))
                        .append(Component.literal("Lists").withStyle(highlight))
                        .append(Component.literal(" tab in\nthe GUI.\n\n").withStyle(body))
                        .append(Component.literal("Select an entity\nfrom the picker,\nthen click ").withStyle(body))
                        .append(Component.literal("+ Allow").withStyle(highlight))
                        .append(Component.literal("\nor ").withStyle(body))
                        .append(Component.literal("+ Deny").withStyle(highlight))
                        .append(Component.literal(".\n\n").withStyle(body))
                        .append(Component.literal("Click ").withStyle(body))
                        .append(Component.literal("[x]").withStyle(highlight))
                        .append(Component.literal(" beside a\nname to remove it.").withStyle(dim))),
                // Page 9 — Bell Recall
                page(Component.empty()
                        .append(Component.literal("Bell Recall\n\n").withStyle(header))
                        .append(Component.literal("Right-click any ").withStyle(body))
                        .append(Component.literal("Bell").withStyle(highlight))
                        .append(Component.literal("\nwithin 64 blocks to\nrecall all owned\ngolems to their\nfirst waypoint.\n\n").withStyle(body))
                        .append(Component.literal("Golems stop patrol\nand wait at start.\nUse the ").withStyle(dim))
                        .append(Component.literal("Patrol").withStyle(highlight))
                        .append(Component.literal("\nbutton to restart.").withStyle(dim))),
                // Page 10 — Commands
                page(Component.empty()
                        .append(Component.literal("Commands\n\n").withStyle(header))
                        .append(Component.literal("/scgolem status\n").withStyle(cmd))
                        .append(Component.literal("Full status report\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem setowner\n").withStyle(cmd))
                        .append(Component.literal("Claim nearest golem\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem patrol\n  start|stop\n  speed <0.1-3.0>\n").withStyle(cmd))
                        .append(Component.literal("\n/scgolem threat\n  warn|follow|attack\n").withStyle(cmd))
                        .append(Component.literal("\n/scgolem waypoint\n  add|remove|clear").withStyle(cmd)))
        );

        return new WrittenBookContent(
                Filterable.passThrough("Security Golem Manual"),
                "SCGuardGolem", 0, pages, true);
    }

    private static Filterable<Component> page(Component content) {
        return Filterable.passThrough(content);
    }
}
