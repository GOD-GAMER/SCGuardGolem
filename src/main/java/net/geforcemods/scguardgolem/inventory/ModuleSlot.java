package net.geforcemods.scguardgolem.inventory;

import java.util.function.BooleanSupplier;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModuleSlot extends Slot {

    private final int moduleSlotIndex;
    private BooleanSupplier activeCheck = () -> true;

    public ModuleSlot(Container container, int slotIndex, int x, int y) {
        super(container, slotIndex, x, y);
        this.moduleSlotIndex = slotIndex;
    }

    public void setActiveCheck(BooleanSupplier check) {
        this.activeCheck = check;
    }

    @Override
    public boolean isActive() {
        return activeCheck.getAsBoolean();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return SecurityGolemEntity.isValidModuleForSlot(moduleSlotIndex, stack);
    }

    // SecurityCraft modules are binary — one per slot, no stacking.
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
