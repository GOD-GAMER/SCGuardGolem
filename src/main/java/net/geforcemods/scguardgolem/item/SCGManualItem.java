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
        Style accent = Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD);

        List<Filterable<Component>> pages = List.of(
                page(Component.empty()
                        .append(Component.literal("\n\n").withStyle(body))
                        .append(Component.literal("Security Guard\nGolem Manual").withStyle(header))
                        .append(Component.literal("\n\nv1.2.0\n\n").withStyle(accent))
                        .append(Component.literal("A complete guide\nto creating and\ncommanding your\nSecurity Golem.").withStyle(dim))
                        .append(Component.literal("\n\n").withStyle(body))
                        .append(Component.literal("by SCGuardGolem").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Getting Started\n\n").withStyle(header))
                        .append(Component.literal("1. Build a vanilla\n   Iron Golem\n\n").withStyle(body))
                        .append(Component.literal("2. Hold any SC\n   Keycard\n\n").withStyle(body))
                        .append(Component.literal("3. Right-click the\n   Iron Golem\n\n").withStyle(body))
                        .append(Component.literal("The keycard is\nconsumed and you\nbecome its owner.").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Opening the GUI\n\n").withStyle(header))
                        .append(Component.literal("Hold SC ").withStyle(body))
                        .append(Component.literal("Wire Cutters").withStyle(accent))
                        .append(Component.literal("\nand right-click\nyour golem to open\nthe config GUI.\n\n").withStyle(body))
                        .append(Component.literal("The GUI has 3 tabs:\n").withStyle(body))
                        .append(Component.literal("- Modules\n").withStyle(cmd))
                        .append(Component.literal("- Patrol\n").withStyle(cmd))
                        .append(Component.literal("- Settings").withStyle(cmd))),
                page(Component.empty()
                        .append(Component.literal("Module Upgrades\n\n").withStyle(header))
                        .append(Component.literal("Place SC modules in\nthe 6 upgrade slots.\n").withStyle(body))
                        .append(Component.literal("Stack count = level\n(1-5 per slot).\n\n").withStyle(dim))
                        .append(Component.literal("Harming Module\n").withStyle(accent))
                        .append(Component.literal("+3 attack damage\nper level\n\n").withStyle(dim))
                        .append(Component.literal("Speed Module\n").withStyle(accent))
                        .append(Component.literal("+0.03 move speed\nper level").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("More Modules\n\n").withStyle(header))
                        .append(Component.literal("Smart Module\n").withStyle(accent))
                        .append(Component.literal("+4 block detection\nradius per level\n\n").withStyle(dim))
                        .append(Component.literal("Storage Module\n").withStyle(accent))
                        .append(Component.literal("+9 loot slots per\nlevel (max 27)\n\n").withStyle(dim))
                        .append(Component.literal("Golem auto-collects\nnearby dropped items\ninto its chest when\nstorage is unlocked.").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Player Lists\n\n").withStyle(header))
                        .append(Component.literal("Allowlist Module\n").withStyle(accent))
                        .append(Component.literal("= Don't Harm list\nPlayers on this list\nare never attacked.\n\n").withStyle(dim))
                        .append(Component.literal("Denylist Module\n").withStyle(accent))
                        .append(Component.literal("= Always Harm list\nPlayers on this list\nare always attacked.\n\n").withStyle(dim))
                        .append(Component.literal("Edit the SC module\nbefore placing it!").withStyle(body))),
                page(Component.empty()
                        .append(Component.literal("Loot Storage\n\n").withStyle(header))
                        .append(Component.literal("The Modules tab\nshows loot slots\nunlocked by the\nStorage Module.\n\n").withStyle(body))
                        .append(Component.literal("Password protect\nyour golem's chest\nfrom Settings tab.\n\n").withStyle(body))
                        .append(Component.literal("On death, all loot\nand modules drop.").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Camera System\n\n").withStyle(header))
                        .append(Component.literal("Enable camera from\nthe Settings tab.\n\n").withStyle(body))
                        .append(Component.literal("When enabled, a\ncamera entity rides\nthe golem at eye\nheight.\n\n").withStyle(body))
                        .append(Component.literal("Use the SC ").withStyle(body))
                        .append(Component.literal("Camera\nMonitor").withStyle(accent))
                        .append(Component.literal(" to view the\ngolem's perspective.").withStyle(body))),
                page(Component.empty()
                        .append(Component.literal("Patrol System\n\n").withStyle(header))
                        .append(Component.literal("Use the Patrol tab\nto add waypoints\nand toggle patrol.\n\n").withStyle(body))
                        .append(Component.literal("\"+ Add Here\" adds\nyour current pos.\n\n").withStyle(dim))
                        .append(Component.literal("Patrol speed and\non/off toggle are\nin the Settings tab.").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Threat Modes\n\n").withStyle(header))
                        .append(Component.literal("Set from Settings:\n\n").withStyle(body))
                        .append(Component.literal("WARN\n").withStyle(accent))
                        .append(Component.literal("Warn in chat\n\n").withStyle(dim))
                        .append(Component.literal("FOLLOW\n").withStyle(accent))
                        .append(Component.literal("Follow untrusted\nplayers\n\n").withStyle(dim))
                        .append(Component.literal("ATTACK\n").withStyle(accent))
                        .append(Component.literal("Attack untrusted\nplayers").withStyle(dim))),
                page(Component.empty()
                        .append(Component.literal("Commands\n\n").withStyle(header))
                        .append(Component.literal("Commands still work\nas an alternative\nto the GUI:\n\n").withStyle(dim))
                        .append(Component.literal("/scgolem status\n").withStyle(cmd))
                        .append(Component.literal("/scgolem setowner\n").withStyle(cmd))
                        .append(Component.literal("/scgolem patrol ...\n").withStyle(cmd))
                        .append(Component.literal("/scgolem threat ...\n").withStyle(cmd))
                        .append(Component.literal("/scgolem upgrade ...\n").withStyle(cmd))
                        .append(Component.literal("/scgolem list ...").withStyle(cmd))),
                page(Component.empty()
                        .append(Component.literal("Quick Reference\n\n").withStyle(header))
                        .append(Component.literal("Keycard").withStyle(accent))
                        .append(Component.literal(" = Convert\n").withStyle(body))
                        .append(Component.literal("Wire Cutters").withStyle(accent))
                        .append(Component.literal(" = GUI\n").withStyle(body))
                        .append(Component.literal("Shift+Empty").withStyle(accent))
                        .append(Component.literal(" = Status\n\n").withStyle(body))
                        .append(Component.literal("Trust Priority:\n").withStyle(header))
                        .append(Component.literal("1. Denylist (attack)\n2. Allowlist (ignore)\n3. Owner (safe)\n4. Threat mode").withStyle(body)))
        );

        return new WrittenBookContent(
                Filterable.passThrough("Security Golem Manual"),
                "SCGuardGolem", 0, pages, true);
    }

    private static Filterable<Component> page(Component content) {
        return Filterable.passThrough(content);
    }
}
