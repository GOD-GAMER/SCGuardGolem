package net.geforcemods.scguardgolem.inventory;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side menu for the Security Guard Golem configuration GUI.
 * Layout:
 *   - 6 module slots (row at top: allowlist, denylist, harming, speed, smart, storage)
 *   - Up to 27 loot slots (3 rows of 9, unlocked by storage module level)
 *   - 36 player inventory slots
 */
public class GolemMenu extends AbstractContainerMenu {

    private final SecurityGolemEntity golem;
    private final Container moduleContainer;
    private final Container lootContainer;

    // Module slot indices (in the menu's slot list)
    public static final int MODULE_SLOT_START = 0;
    public static final int MODULE_SLOT_COUNT = 6;
    // Loot slot indices
    public static final int LOOT_SLOT_START = MODULE_SLOT_COUNT;
    public static final int MAX_LOOT_SLOTS = 27;
    // Player inventory
    public static final int PLAYER_INV_START = LOOT_SLOT_START + MAX_LOOT_SLOTS;

    public GolemMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, resolveGolem(playerInv.player, buf.readInt()));
    }

    public GolemMenu(int containerId, Inventory playerInv, SecurityGolemEntity golem) {
        super(SCGContent.GOLEM_MENU.get(), containerId);
        this.golem = golem;
        this.moduleContainer = golem != null ? golem.getModuleContainer() : new SimpleContainer(MODULE_SLOT_COUNT);
        this.lootContainer = golem != null ? golem.getLootContainer() : new SimpleContainer(MAX_LOOT_SLOTS);

        // Module slots (top row, y=20)
        for (int i = 0; i < MODULE_SLOT_COUNT; i++) {
            addSlot(new ModuleSlot(moduleContainer, i, 17 + i * 26, 20));
        }

        // Loot slots (3 rows of 9, starting at y=52)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                addSlot(new LootSlot(lootContainer, index, 8 + col * 18, 52 + row * 18));
            }
        }

        // Player inventory (3 rows, starting at y=118)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }

        // Player hotbar (y=176)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 176));
        }
    }

    private static SecurityGolemEntity resolveGolem(Player player, int entityId) {
        if (player.level().getEntity(entityId) instanceof SecurityGolemEntity g) {
            return g;
        }
        return null;
    }

    public SecurityGolemEntity getGolem() {
        return golem;
    }

    public int getUnlockedLootSlots() {
        if (golem == null) return 0;
        return golem.getUnlockedLootSlots();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            copy = slotStack.copy();

            if (index < MODULE_SLOT_START + MODULE_SLOT_COUNT) {
                // Module slot -> player inventory
                if (!moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_START + 36, true))
                    return ItemStack.EMPTY;
            } else if (index < LOOT_SLOT_START + MAX_LOOT_SLOTS) {
                // Loot slot -> player inventory
                if (!moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_START + 36, true))
                    return ItemStack.EMPTY;
            } else {
                // Player inventory -> try modules first, then loot
                if (!moveItemStackTo(slotStack, MODULE_SLOT_START, MODULE_SLOT_START + MODULE_SLOT_COUNT, false)) {
                    int unlockedEnd = LOOT_SLOT_START + getUnlockedLootSlots();
                    if (unlockedEnd > LOOT_SLOT_START) {
                        if (!moveItemStackTo(slotStack, LOOT_SLOT_START, unlockedEnd, false))
                            return ItemStack.EMPTY;
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return golem != null && golem.isAlive() && golem.distanceTo(player) < 8.0;
    }

    /**
     * Slot that only accepts SC module items.
     */
    private static class ModuleSlot extends Slot {
        public ModuleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            try {
                return stack.getItem() instanceof net.geforcemods.securitycraft.items.ModuleItem;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }

        @Override
        public int getMaxStackSize() {
            return 5;
        }
    }

    /**
     * Slot for loot storage; disabled when storage module level is insufficient.
     */
    private class LootSlot extends Slot {
        public LootSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return getContainerSlot() < getUnlockedLootSlots();
        }
    }
}
