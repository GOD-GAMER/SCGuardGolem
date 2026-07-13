package net.geforcemods.scguardgolem.item;

import net.minecraft.world.InteractionHand;
//? if >=1.21.8
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A small "open the guide" item. The guard golem's documentation is a native
 * {@link net.geforcemods.securitycraft.misc.SCManualPage} inside SecurityCraft's
 * own Manual (registered by {@code SCGManualPages}); using this item just opens
 * that Manual, mirroring SecurityCraft's own manual item. No hardcoded book.
 */
public class SCGManualItem extends Item {

    public SCGManualItem(Item.Properties properties) {
        super(properties);
    }

    //? if >=1.21.8 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide())
            net.geforcemods.securitycraft.ClientHandler.displaySCManualScreen();
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide())
            net.geforcemods.securitycraft.ClientHandler.displaySCManualScreen();
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    *///?}
}
