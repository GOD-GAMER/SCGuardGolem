package net.geforcemods.scguardgolem.inventory;

import java.util.function.BooleanSupplier;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class ToggleableSlot extends Slot {

    private BooleanSupplier activeCheck = () -> true;

    public ToggleableSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public void setActiveCheck(BooleanSupplier check) {
        this.activeCheck = check;
    }

    @Override
    public boolean isActive() {
        return activeCheck.getAsBoolean();
    }
}
