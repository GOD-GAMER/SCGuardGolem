package net.geforcemods.scguardgolem.inventory;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GolemMenu extends AbstractContainerMenu {

    private final SecurityGolemEntity golem;
    private final Container moduleContainer;
    private final Container lootContainer;
    private final ContainerData data;
    private final int lootRows;

    // Data slot indices
    private static final int DATA_PATROL = 0;
    private static final int DATA_THREAT = 1;
    private static final int DATA_CAMERA = 2;
    private static final int DATA_COUNT = 3;

    // Layout constants
    private static final int MODULE_START_X = 8;
    private static final int MODULE_START_Y = 18;
    private static final int LOOT_START_X = 8;
    private static final int LOOT_START_Y = 62;

    public GolemMenu(int containerId, Inventory playerInv, SecurityGolemEntity golem) {
        super(SCGContent.GOLEM_MENU.get(), containerId);
        this.golem = golem;
        this.moduleContainer = golem.getModuleInventory();
        this.lootContainer = golem.getLootInventory();
        this.lootRows = golem.getLootRows();

        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case DATA_PATROL -> golem.isPatrolling() ? 1 : 0;
                    case DATA_THREAT -> golem.getThreatMode().ordinal();
                    case DATA_CAMERA -> golem.hasCamera() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case DATA_PATROL -> golem.setPatrolling(value != 0);
                    case DATA_THREAT -> golem.setThreatMode(SecurityGolemEntity.ThreatMode.fromOrdinal(value));
                    case DATA_CAMERA -> golem.setHasCamera(value != 0);
                }
            }
            @Override public int getCount() { return DATA_COUNT; }
        };
        addDataSlots(this.data);

        // Module slots (top row: Harming, Speed, Smart; bottom row: Allowlist, Denylist, Storage)
        for (int i = 0; i < 3; i++) {
            addSlot(new ModuleSlot(moduleContainer, i, MODULE_START_X + i * 28, MODULE_START_Y));
        }
        for (int i = 0; i < 3; i++) {
            addSlot(new ModuleSlot(moduleContainer, 3 + i, MODULE_START_X + i * 28, MODULE_START_Y + 22));
        }

        // Loot inventory slots
        for (int row = 0; row < lootRows; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                if (slotIndex < lootContainer.getContainerSize()) {
                    addSlot(new Slot(lootContainer, slotIndex, LOOT_START_X + col * 18, LOOT_START_Y + row * 18));
                }
            }
        }

        // Player inventory
        int playerInvY = LOOT_START_Y + lootRows * 18 + 14;
        addStandardInventorySlots(playerInv, LOOT_START_X, playerInvY);
    }

    // Client-side constructor from network buffer
    public GolemMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, findGolem(playerInv, buf));
    }

    private static SecurityGolemEntity findGolem(Inventory playerInv, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        var entity = playerInv.player.level().getEntity(entityId);
        if (entity instanceof SecurityGolemEntity golem) return golem;
        throw new IllegalStateException("No SecurityGolemEntity with id " + entityId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int moduleSlotCount = SecurityGolemEntity.MODULE_SLOTS;
        int lootSlotCount = lootRows * 9;
        int totalContainerSlots = moduleSlotCount + lootSlotCount;

        if (slotIndex < moduleSlotCount) {
            // From module slot ? player inventory
            if (!moveItemStackTo(stack, totalContainerSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (slotIndex < totalContainerSlots) {
            // From loot slot ? player inventory
            if (!moveItemStackTo(stack, totalContainerSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // From player inventory ? try modules first, then loot
            if (!moveItemStackTo(stack, 0, moduleSlotCount, false)) {
                if (!moveItemStackTo(stack, moduleSlotCount, totalContainerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return golem.isAlive() && golem.distanceToSqr(player) < 64.0;
    }

    // Button click handler for patrol/threat/camera toggles
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        switch (buttonId) {
            case 0 -> { // Toggle patrol
                data.set(DATA_PATROL, data.get(DATA_PATROL) == 0 ? 1 : 0);
                return true;
            }
            case 1 -> { // Cycle threat mode
                int next = (data.get(DATA_THREAT) + 1) % SecurityGolemEntity.ThreatMode.values().length;
                data.set(DATA_THREAT, next);
                return true;
            }
            case 2 -> { // Toggle camera
                data.set(DATA_CAMERA, data.get(DATA_CAMERA) == 0 ? 1 : 0);
                return true;
            }
        }
        return false;
    }

    public SecurityGolemEntity getGolem() { return golem; }
    public int getLootRows() { return lootRows; }
    public ContainerData getData() { return data; }
}
