package net.geforcemods.scguardgolem.inventory;

import java.util.function.Supplier;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A container proxy that always delegates to the current container
 * returned by the supplier. This prevents stale references when the
 * underlying container is replaced (e.g., loot inventory resize).
 */
public class DelegatingContainer implements Container {

    private final Supplier<? extends Container> delegate;

    public DelegatingContainer(Supplier<? extends Container> delegate) {
        this.delegate = delegate;
    }

    private Container get() { return delegate.get(); }

    @Override public int getContainerSize() { return get().getContainerSize(); }
    @Override public boolean isEmpty() { return get().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return get().getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return get().removeItem(slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return get().removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { get().setItem(slot, stack); }
    @Override public void setChanged() { get().setChanged(); }
    @Override public boolean stillValid(Player player) { return get().stillValid(player); }
    @Override public void clearContent() { get().clearContent(); }
}
