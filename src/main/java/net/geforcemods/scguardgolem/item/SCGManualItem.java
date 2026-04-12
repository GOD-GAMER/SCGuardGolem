package net.geforcemods.scguardgolem.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;

public class SCGManualItem extends Item {

    public SCGManualItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        applyBookTags(stack);
        player.openItemGui(stack, hand);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void applyBookTags(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("pages")) return;

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", "Security Golem Manual");
        tag.putString("author", "SCGuardGolem");
        tag.putBoolean("resolved", true);

        ListTag pages = new ListTag();

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("\n\nSecurity Guard\nGolem Manual\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("v1.2.0\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("A complete guide\nto creating and\ncommanding your\nSecurity Golem.\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("by SCGuardGolem").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Getting Started\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("1. Place an Iron Golem\n\n2. Hold a SC Keycard\n\n3. Right-click the golem\n\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("The golem converts into\na Security Guard Golem\nowned by you.").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Opening the GUI\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Hold SC ").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("Wire Cutters").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("\nand right-click\nyour golem to open\nthe config GUI.\n\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("The GUI has 3 tabs:\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("- Modules\n- Patrol\n- Settings").withStyle(ChatFormatting.DARK_GREEN)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Module Upgrades\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Place SC modules in\nthe 6 upgrade slots.\nStack count = level\n(1-5 per slot).\n\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("Harming Module\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("+3 attack damage/lvl\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("Speed Module\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("+0.03 move speed/lvl").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("More Modules\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Smart Module\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("+4 block detection\nradius per level\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("Storage Module\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("+9 loot slots per\nlevel (max 27)\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("Golem auto-collects\nnearby dropped items.").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Player Lists\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Allowlist Module\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("= Don't Harm list\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("Denylist Module\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("= Always Harm list\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("Edit the SC module\nbefore placing it!").withStyle(ChatFormatting.BLACK)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Camera System\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Enable camera from\nthe Settings tab.\n\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("When enabled, a\ncamera entity rides\nthe golem.\n\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("Use the SC ").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("Camera\nMonitor").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal(" to view the\ngolem's perspective.").withStyle(ChatFormatting.BLACK)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Threat Modes\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("WARN\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Warn in chat\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("FOLLOW\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Follow untrusted\nplayers\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("ATTACK\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Attack untrusted\nplayers").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Quick Reference\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("Keycard").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal(" = Convert\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("Wire Cutters").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal(" = GUI\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("Shift+Empty").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal(" = Status\n\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("Trust Priority:\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.GOLD)))
                        .append(Component.literal("1. Denylist (attack)\n2. Allowlist (ignore)\n3. Owner (safe)\n4. Threat mode").withStyle(ChatFormatting.BLACK)))));

        tag.put("pages", pages);
    }
}
