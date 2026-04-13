package net.geforcemods.scguardgolem.inventory;

import java.util.ArrayList;
import java.util.List;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
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

    // Tabs
    public static final int TAB_SETTINGS = 0;
    public static final int TAB_MODULES = 1;
    public static final int TAB_LOOT = 2;
    private int currentTab = TAB_SETTINGS;

    // Slot groups
    private final List<ModuleSlot> moduleSlots = new ArrayList<>();
    private final List<ToggleableSlot> lootSlots = new ArrayList<>();
    private final List<ToggleableSlot> playerSlots = new ArrayList<>();

    // Module slot layout: 2 rows x 3 cols, centered in 220px GUI
    // col spacing 50px for breathing room, starting at x=35
    private static final int MODULE_X = 35;
    private static final int MODULE_Y = 50;
    private static final int MODULE_COL_SPACING = 50;
    private static final int MODULE_ROW_SPACING = 36;

    // Loot slot layout: standard 9-wide, x=22 to center in 220px
    private static final int LOOT_X = 22;
    private static final int LOOT_Y = 30;

    // Player inventory: same x, below loot
    private static final int PLAYER_INV_X = 22;

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

        // Module slots
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                ModuleSlot ms = new ModuleSlot(moduleContainer, index,
                        MODULE_X + col * MODULE_COL_SPACING,
                        MODULE_Y + row * MODULE_ROW_SPACING);
                ms.setActiveCheck(() -> currentTab == TAB_MODULES);
                addSlot(ms);
                moduleSlots.add(ms);
            }
        }

        // Loot slots
        for (int row = 0; row < lootRows; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                if (slotIndex < lootContainer.getContainerSize()) {
                    ToggleableSlot ts = new ToggleableSlot(lootContainer, slotIndex,
                            LOOT_X + col * 18,
                            LOOT_Y + row * 18);
                    ts.setActiveCheck(() -> currentTab == TAB_LOOT);
                    addSlot(ts);
                    lootSlots.add(ts);
                }
            }
        }

        // Player inventory slots (36 slots: 27 main + 9 hotbar)
        int playerInvY = LOOT_Y + lootRows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ToggleableSlot ts = new ToggleableSlot(playerInv, col + (row + 1) * 9,
                        PLAYER_INV_X + col * 18,
                        playerInvY + row * 18);
                ts.setActiveCheck(() -> currentTab == TAB_LOOT);
                addSlot(ts);
                playerSlots.add(ts);
            }
        }
        // Hotbar
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            ToggleableSlot ts = new ToggleableSlot(playerInv, col,
                    PLAYER_INV_X + col * 18,
                    hotbarY);
            ts.setActiveCheck(() -> currentTab == TAB_LOOT);
            addSlot(ts);
            playerSlots.add(ts);
        }
    }

    public GolemMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, findGolem(playerInv, buf));
    }

    private static SecurityGolemEntity findGolem(Inventory playerInv, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        var entity = playerInv.player.level().getEntity(entityId);
        if (entity instanceof SecurityGolemEntity golem) return golem;
        throw new IllegalStateException("No SecurityGolemEntity with id " + entityId);
    }

    public void setTab(int tab) {
        this.currentTab = tab;
    }

    public int getCurrentTab() { return currentTab; }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem() || !slot.isActive()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int moduleStart = 0;
        int moduleEnd = moduleSlots.size();
        int lootStart = moduleEnd;
        int lootEnd = lootStart + lootSlots.size();
        int playerStart = lootEnd;
        int playerEnd = playerStart + playerSlots.size();

        if (currentTab == TAB_LOOT) {
            if (slotIndex >= lootStart && slotIndex < lootEnd) {
                if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            } else if (slotIndex >= playerStart && slotIndex < playerEnd) {
                if (!moveItemStackTo(stack, lootStart, lootEnd, false)) return ItemStack.EMPTY;
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

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        switch (buttonId) {
            case 0 -> {
                data.set(DATA_PATROL, data.get(DATA_PATROL) == 0 ? 1 : 0);
                return true;
            }
            case 1 -> {
                int next = (data.get(DATA_THREAT) + 1) % SecurityGolemEntity.ThreatMode.values().length;
                data.set(DATA_THREAT, next);
                return true;
            }
            case 2 -> {
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
