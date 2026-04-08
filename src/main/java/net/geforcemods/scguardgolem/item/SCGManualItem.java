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
                        .append(Component.literal("\n\nSecurity Guard\nGolem Manual\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.DARK_AQUA)))
                        .append(Component.literal("A complete guide\nto creating and\ncommanding your\nSecurity Golem.\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("by SCGuardGolem").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Getting Started\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.DARK_AQUA)))
                        .append(Component.literal("1. Build a vanilla\n   Iron Golem\n\n2. Hold any SC\n   Keycard\n\n3. Right-click the\n   Iron Golem\n\n").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("The keycard is\nconsumed and you\nbecome its owner.").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Patrol System\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.DARK_AQUA)))
                        .append(Component.literal("/scgolem patrol\n  start\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Begin patrol loop\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/scgolem patrol\n  stop\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Stop patrolling\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/scgolem patrol\n  speed <0.1-3.0>\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Set patrol speed").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Waypoints\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.DARK_AQUA)))
                        .append(Component.literal("/scgolem patrol\n  waypoint addhere\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Add at your pos\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/scgolem patrol\n  waypoint add\n  <x> <y> <z>\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Add at coords\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("  waypoint list\n  waypoint remove <#>\n  waypoint clear").withStyle(ChatFormatting.DARK_GREEN)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Threat Modes\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.DARK_AQUA)))
                        .append(Component.literal("/scgolem threat warn\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Warn in chat\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/scgolem threat follow\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Follow untrusted\nplayers\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/scgolem threat attack\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Attack untrusted\nplayers").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Upgrades (0-5)\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.DARK_AQUA)))
                        .append(Component.literal("/scgolem upgrade\n  damage <lvl>\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("+3 damage/level\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/scgolem upgrade\n  speed <lvl>\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("+0.03 speed/level\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/scgolem upgrade\n  detection <lvl>\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("+4 block radius/lvl").withStyle(ChatFormatting.GRAY)))));

        pages.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.empty()
                        .append(Component.literal("Player Lists\n\n").withStyle(s -> s.withBold(true).withColor(ChatFormatting.DARK_AQUA)))
                        .append(Component.literal("/scgolem list\n  ignore add <name>\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Never attack\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("  attack add <name>\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("Always attack\n\n").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/scgolem list show\n").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("/scgolem status\n/scgolem setowner").withStyle(ChatFormatting.DARK_GREEN)))));

        tag.put("pages", pages);
    }
}
