package net.geforcemods.scguardgolem.inventory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
//?} else
/*import net.minecraft.network.FriendlyByteBuf;*/
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

    private static final int DATA_PATROL = 0;
    private static final int DATA_THREAT = 1;
    private static final int DATA_DWELL  = 2;
    private static final int DATA_COUNT  = 3;

    public static final int TAB_CONFIG = 0;
    public static final int TAB_LOOT = 1;
    public static final int TAB_LISTS = 2;
    public static final int TAB_WAYPOINTS = 3;
    private int currentTab = TAB_CONFIG;

    /** Maximum rows shown in the loot viewport before scrolling kicks in. */
    public static final int VISIBLE_LOOT_ROWS = 3;

    private final List<ModuleSlot> moduleSlots = new ArrayList<>();
    private final List<ScrollableLootSlot> lootSlots = new ArrayList<>();
    private final List<ToggleableSlot> playerSlots = new ArrayList<>();

    private int scrollOffset = 0;

    // ?? Config tab slot positions (relative to GUI top-left) ??
    public static final int MOD_X = 8;
    public static final int MOD_Y = 20;
    public static final int MOD_COL = 50;  // legacy 2-col spacing (kept for screen refs)
    public static final int MOD_COL_TIGHT = 54; // 3-col spacing for 6 module slots
    public static final int MOD_ROW = 28;  // 18px slot + 10px gap for label below

    // Config content bottom (modules + buttons + status)
    private static final int CONFIG_BOTTOM = MOD_Y + 2 * MOD_ROW + 58;

    // ?? Loot tab slot positions ??
    public static final int LOOT_X = 8;
    public static final int LOOT_Y = 18;
    public static final int PLAYER_INV_X = 8;

    // ?? Computed player inv Y (below both tabs' content) ??
    private final int playerInvY;

    public GolemMenu(int containerId, Inventory playerInv, SecurityGolemEntity golem) {
        super(SCGContent.GOLEM_MENU.get(), containerId);
        this.golem = golem;
        this.moduleContainer = golem.getModuleInventory();
        this.lootContainer = new DelegatingContainer(golem::getLootInventory);

        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case DATA_PATROL -> golem.isPatrolling() ? 1 : 0;
                    case DATA_THREAT -> golem.getThreatMode().ordinal();
                    case DATA_DWELL  -> golem.getDwellTicks();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case DATA_PATROL -> golem.setPatrolling(value != 0);
                    case DATA_THREAT -> golem.setThreatMode(SecurityGolemEntity.ThreatMode.fromOrdinal(value));
                    case DATA_DWELL  -> golem.setDwellTicks(Math.max(0, value));
                }
            }
            @Override public int getCount() { return DATA_COUNT; }
        };
        addDataSlots(this.data);

        // Module slots (visible on CONFIG tab) — 3x2 grid for the 6 SC module types
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                if (index >= moduleContainer.getContainerSize()) continue;
                ModuleSlot ms = new ModuleSlot(moduleContainer, index,
                        MOD_X + col * MOD_COL_TIGHT,
                        MOD_Y + row * MOD_ROW);
                ms.setActiveCheck(() -> currentTab == TAB_CONFIG);
                addSlot(ms);
                moduleSlots.add(ms);
            }
        }

        // ?? Scrollable loot viewport (visible on LOOT tab) ??
        for (int row = 0; row < VISIBLE_LOOT_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                final int vRow = row;
                final int vCol = col;
                ScrollableLootSlot sl = new ScrollableLootSlot(
                        lootContainer, vRow, vCol,
                        LOOT_X + col * 18,
                        LOOT_Y + row * 18,
                        () -> scrollOffset);
                sl.setActiveCheck(() -> currentTab == TAB_LOOT);
                addSlot(sl);
                lootSlots.add(sl);
            }
        }

        // ?? Compute player inv Y: below all tabs' content ??
        int lootViewportBottom = LOOT_Y + VISIBLE_LOOT_ROWS * 18 + 4;
        this.playerInvY = Math.max(CONFIG_BOTTOM, lootViewportBottom) + 14;

        // ?? Player inventory (visible on BOTH tabs) ??
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ToggleableSlot ts = new ToggleableSlot(playerInv, col + (row + 1) * 9,
                        PLAYER_INV_X + col * 18,
                        playerInvY + row * 18);
                addSlot(ts);
                playerSlots.add(ts);
            }
        }
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            ToggleableSlot ts = new ToggleableSlot(playerInv, col,
                    PLAYER_INV_X + col * 18, hotbarY);
            addSlot(ts);
            playerSlots.add(ts);
        }
    }

    /** Client-side factory — reads entity ID + lootRows from the network buffer. */
    //? if >=1.20.5 {
    public GolemMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, prepareGolemFromBuf(playerInv, buf));
    }
    //?} else {
    /*public GolemMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, prepareGolemFromBuf(playerInv, buf));
    }
    *///?}

    /**
     * Reads the buffer written by {@code SecurityGolemEntity.writeClientSideData},
     * finds the entity on the client, and ensures its loot container is correctly
     * sized (the client-side entity hasn't run {@code resizeLootInventory}).
     */
    //? if >=1.20.5
    private static SecurityGolemEntity prepareGolemFromBuf(Inventory playerInv, RegistryFriendlyByteBuf buf) {
    //? if <1.20.5
    /*private static SecurityGolemEntity prepareGolemFromBuf(Inventory playerInv, FriendlyByteBuf buf) {*/
        int entityId = buf.readInt();
        int serverLootRows = buf.readInt();
        // Waypoints
        int wpCount = buf.readInt();
        List<BlockPos> wpPositions = new ArrayList<>(wpCount);
        List<String> wpNames = new ArrayList<>(wpCount);
        for (int i = 0; i < wpCount; i++) {
            wpPositions.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
            wpNames.add(buf.readUtf());
        }
        int currentWpIndex = buf.readInt();

        var entity = playerInv.player.level().getEntity(entityId);
        if (!(entity instanceof SecurityGolemEntity g))
            throw new IllegalStateException("No SecurityGolemEntity with id " + entityId);

        // Resize client loot container to match server capacity
        int needed = serverLootRows * 9;
        SimpleContainer current = g.getLootInventory();
        if (current.getContainerSize() != needed) {
            SimpleContainer resized = new SimpleContainer(needed);
            for (int i = 0; i < Math.min(current.getContainerSize(), needed); i++)
                resized.setItem(i, current.getItem(i));
            g.setLootInventory(resized);
        }

        // Inject waypoints from server into client-side entity
        g.clearWaypoints();
        for (int i = 0; i < wpPositions.size(); i++) {
            g.addWaypoint(wpPositions.get(i), wpNames.get(i));
        }
        g.setCurrentWaypointIndex(currentWpIndex);

        return g;
    }

    public void setTab(int tab) { this.currentTab = tab; }
    public int getCurrentTab() { return currentTab; }

    // ?? Scroll helpers ??

    public int getScrollOffset() { return scrollOffset; }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, getMaxScroll()));
    }

    public int getMaxScroll() {
        return Math.max(0, getLootRows() - VISIBLE_LOOT_ROWS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem() || !slot.isActive()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int moduleEnd = moduleSlots.size();
        int lootEnd = moduleEnd + lootSlots.size();
        int playerEnd = lootEnd + playerSlots.size();

        if (currentTab == TAB_CONFIG) {
            if (slotIndex < moduleEnd) {
                if (!moveItemStackTo(stack, lootEnd, playerEnd, true)) return ItemStack.EMPTY;
            } else if (slotIndex >= lootEnd && slotIndex < playerEnd) {
                if (!moveItemStackTo(stack, 0, moduleEnd, false)) return ItemStack.EMPTY;
            }
        } else if (currentTab == TAB_LOOT) {
            if (slotIndex >= moduleEnd && slotIndex < lootEnd) {
                if (!moveItemStackTo(stack, lootEnd, playerEnd, true)) return ItemStack.EMPTY;
            } else if (slotIndex >= lootEnd && slotIndex < playerEnd) {
                if (!moveItemStackTo(stack, moduleEnd, lootEnd, false)) return ItemStack.EMPTY;
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
            case 0 -> { data.set(DATA_PATROL, data.get(DATA_PATROL) == 0 ? 1 : 0); return true; }
            case 1 -> { int next = (data.get(DATA_THREAT) + 1) % SecurityGolemEntity.ThreatMode.values().length; data.set(DATA_THREAT, next); return true; }
            case 2 -> { golem.clearWaypoints(); golem.setPatrolling(false); return true; }
            case 3 -> { golem.setRecalling(false); golem.setPatrolling(true); return true; }
        }
        // Scroll offset: button IDs 100+ encode absolute offset
        if (buttonId >= 100 && buttonId < 200) {
            setScrollOffset(buttonId - 100);
            return true;
        }
        // Tab sync: button IDs 200+ encode tab index
        if (buttonId >= 200 && buttonId < 210) {
            setTab(buttonId - 200);
            return true;
        }
        // Allow/deny are now SecurityCraft ALLOWLIST/DENYLIST modules — edit them
        // via SC's own list-module screen, not through this menu (button IDs 300-699 retired).
        // Remove waypoint by index: button IDs 700+
        if (buttonId >= 700 && buttonId < 800) {
            int idx = buttonId - 700;
            golem.removeWaypoint(idx);
            return true;
        }
        // Dwell time: 800 = decrease by 1 s, 801 = increase by 1 s
        if (buttonId == 800) { golem.setDwellTicks(Math.max(0, golem.getDwellTicks() - 20)); return true; }
        if (buttonId == 801) { golem.setDwellTicks(golem.getDwellTicks() + 20); return true; }
        // Remove loot filter entry: button IDs 900+
        if (buttonId >= 900 && buttonId < 1000) {
            int idx = buttonId - 900;
            List<String> filters = new ArrayList<>(golem.getLootFilter());
            if (idx >= 0 && idx < filters.size()) golem.removeLootFilter(filters.get(idx));
            return true;
        }
        // Add hovered item to loot filter: button ID 1000
        if (buttonId == 1000) {
            // The item is passed via the last slot the player shift-clicked
            // We use the player's cursor item instead
            return true;
        }
        return false;
    }

    public SecurityGolemEntity getGolem() { return golem; }
    public int getLootRows() { return lootContainer.getContainerSize() / 9; }
    public ContainerData getData() { return data; }
    /** Returns dwell ticks from the synced ContainerData (safe to call client-side). */
    public int getSyncedDwellTicks() { return data.get(DATA_DWELL); }
    public int getPlayerInvY() { return playerInvY; }
    public int getGuiHeight() { return playerInvY + 83; }
}
