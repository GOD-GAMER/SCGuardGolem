package net.geforcemods.scguardgolem.inventory;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A "virtual" loot slot that occupies a fixed position in the scroll viewport
 * but dynamically maps to different container indices based on the current
 * scroll offset.  This is needed because {@code Slot.x} and {@code Slot.y}
 * are final in MC 26.1 – we cannot reposition slots after construction.
 */
public class ScrollableLootSlot extends Slot {

    private final int viewRow;
    private final int viewCol;
    private final Container lootContainer;
    private final IntSupplier scrollOffsetSupplier;
    private BooleanSupplier activeCheck = () -> true;

    public ScrollableLootSlot(Container container, int viewRow, int viewCol,
                              int x, int y, IntSupplier scrollOffset) {
        super(container, 0, x, y); // dummy index – overridden by getContainerSlot()
        this.viewRow = viewRow;
        this.viewCol = viewCol;
        this.lootContainer = container;
        this.scrollOffsetSupplier = scrollOffset;
    }

    public void setActiveCheck(BooleanSupplier check) {
        this.activeCheck = check;
    }

    /** The real container index this viewport slot currently points to. */
    private int getRealIndex() {
        return (viewRow + scrollOffsetSupplier.getAsInt()) * 9 + viewCol;
    }

    private boolean inBounds() {
        int idx = getRealIndex();
        return idx >= 0 && idx < lootContainer.getContainerSize();
    }

    // ── Slot overrides ──────────────────────────────────────────────

    @Override
    public int getContainerSlot() {
        return getRealIndex();
    }

    @Override
    public boolean isActive() {
        return activeCheck.getAsBoolean() && inBounds();
    }

    @Override
    public ItemStack getItem() {
        return inBounds() ? lootContainer.getItem(getRealIndex()) : ItemStack.EMPTY;
    }

    @Override
    public void set(ItemStack stack) {
        if (inBounds()) {
            lootContainer.setItem(getRealIndex(), stack);
            setChanged();
        }
    }

    @Override
    public ItemStack remove(int amount) {
        return inBounds() ? lootContainer.removeItem(getRealIndex(), amount) : ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem() {
        return inBounds() && !lootContainer.getItem(getRealIndex()).isEmpty();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return inBounds();
    }

    @Override
    public int getMaxStackSize() {
        return inBounds() ? lootContainer.getMaxStackSize() : 0;
    }

    @Override
    public boolean mayPickup(Player player) {
        return inBounds();
    }
}
