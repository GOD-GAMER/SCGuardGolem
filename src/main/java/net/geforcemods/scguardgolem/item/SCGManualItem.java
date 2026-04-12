package net.geforcemods.scguardgolem.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
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
        Style header = Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD);
        Style body = Style.EMPTY.withColor(ChatFormatting.BLACK);
        Style cmd = Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);
        Style dim = Style.EMPTY.withColor(ChatFormatting.GRAY);

        List<Filterable<Component>> pages = List.of(
                page(Component.empty()
                        .append(Component.literal("Security Golem\nManual\n\n").withStyle(header))
                        .append(Component.literal("This book covers all\ncommands for the\nSecurity Guard Golem.\n\n").withStyle(body))
                        .append(Component.literal("Use /scgolem help\nfor a quick list.").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Getting Started\n\n").withStyle(header))
                        .append(Component.literal("1. Place an Iron Golem\n").withStyle(body))
                        .append(Component.literal("2. Hold a SC Keycard\n").withStyle(body))
                        .append(Component.literal("3. Right-click the golem\n\n").withStyle(body))
                        .append(Component.literal("The golem converts into\na Security Guard Golem\nowned by you.").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Patrol Setup\n\n").withStyle(header))
                        .append(Component.literal("/scgolem patrol start\n").withStyle(cmd))
                        .append(Component.literal("Begin patrol route\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem patrol stop\n").withStyle(cmd))
                        .append(Component.literal("Stop patrolling\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem patrol speed\n  <0.1-3.0>\n").withStyle(cmd))
                        .append(Component.literal("Set patrol speed").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Waypoints\n\n").withStyle(header))
                        .append(Component.literal("/scgolem patrol\n  waypoint addhere\n").withStyle(cmd))
                        .append(Component.literal("Add at your pos\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem patrol\n  waypoint add\n  <x> <y> <z>\n").withStyle(cmd))
                        .append(Component.literal("Add at coords\n\n").withStyle(dim))
                        .append(Component.literal("  waypoint list\n").withStyle(cmd))
                        .append(Component.literal("  waypoint remove <#>\n").withStyle(cmd))
                        .append(Component.literal("  waypoint clear").withStyle(cmd))),
                page(Component.empty()
                        .append(Component.literal("Threat Modes\n\n").withStyle(header))
                        .append(Component.literal("/scgolem threat\n  warn\n").withStyle(cmd))
                        .append(Component.literal("Warn in chat\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem threat\n  follow\n").withStyle(cmd))
                        .append(Component.literal("Follow untrusted\nplayers\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem threat\n  attack\n").withStyle(cmd))
                        .append(Component.literal("Attack untrusted\nplayers").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Upgrades (0-5)\n\n").withStyle(header))
                        .append(Component.literal("/scgolem upgrade\n  damage <lvl>\n").withStyle(cmd))
                        .append(Component.literal("+3 damage/level\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem upgrade\n  speed <lvl>\n").withStyle(cmd))
                        .append(Component.literal("+0.03 speed/level\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem upgrade\n  detection <lvl>\n").withStyle(cmd))
                        .append(Component.literal("+4 block radius\nper level").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Player Lists\n\n").withStyle(header))
                        .append(Component.literal("/scgolem list\n  ignore add <name>\n").withStyle(cmd))
                        .append(Component.literal("Never attack\n\n").withStyle(dim))
                        .append(Component.literal("  ignore remove\n   <name>\n\n").withStyle(cmd))
                        .append(Component.literal("  attack add\n   <name>\n").withStyle(cmd))
                        .append(Component.literal("Always attack\n\n").withStyle(dim))
                        .append(Component.literal("  attack remove\n   <name>\n\n").withStyle(cmd))
                        .append(Component.literal("/scgolem list show").withStyle(cmd))),
                page(Component.empty()
                        .append(Component.literal("Other Commands\n\n").withStyle(header))
                        .append(Component.literal("/scgolem status\n").withStyle(cmd))
                        .append(Component.literal("Full status report\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem setowner\n").withStyle(cmd))
                        .append(Component.literal("Claim nearest golem\n\n").withStyle(dim))
                        .append(Component.literal("Trust Priority\n").withStyle(header))
                        .append(Component.literal("1. Attack list\n2. Ignore list\n3. Owner\n4. SC owner\n5. Threat mode").withStyle(body)))
        );

        return new WrittenBookContent(
                Filterable.passThrough("Security Golem Manual"),
                "SCGuardGolem", 0, pages, true);
    }

    private static Filterable<Component> page(Component content) {
        return Filterable.passThrough(content);
    }
}
