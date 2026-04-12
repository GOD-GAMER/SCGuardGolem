package net.geforcemods.scguardgolem.inventory;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModuleSlot extends Slot {

    private final int moduleSlotIndex;

    public ModuleSlot(Container container, int slotIndex, int x, int y) {
        super(container, slotIndex, x, y);
        this.moduleSlotIndex = slotIndex;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return SecurityGolemEntity.isValidModuleForSlot(moduleSlotIndex, stack);
    }

    @Override
    public int getMaxStackSize() {
        return SecurityGolemEntity.MAX_UPGRADE_LEVEL;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return SecurityGolemEntity.MAX_UPGRADE_LEVEL;
    }
}
