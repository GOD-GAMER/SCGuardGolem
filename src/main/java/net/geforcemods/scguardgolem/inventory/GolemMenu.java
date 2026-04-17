package net.geforcemods.scguardgolem.inventory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
    private static final int DATA_COUNT = 2;

    public static final int TAB_CONFIG = 0;
    public static final int TAB_LOOT = 1;
    public static final int TAB_LISTS = 2;
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
    public static final int MOD_COL = 50;  // 18px slot + 32px label space
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
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case DATA_PATROL -> golem.setPatrolling(value != 0);
                    case DATA_THREAT -> golem.setThreatMode(SecurityGolemEntity.ThreatMode.fromOrdinal(value));
                }
            }
            @Override public int getCount() { return DATA_COUNT; }
        };
        addDataSlots(this.data);

        // ?? Module slots (visible on CONFIG tab) -- 2x2 grid ??
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int index = row * 2 + col;
                ModuleSlot ms = new ModuleSlot(moduleContainer, index,
                        MOD_X + col * MOD_COL,
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

    /** Client-side factory -- reads entity ID + lootRows from the network buffer. */
    public GolemMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInv, prepareGolemFromBuf(playerInv, buf));
    }

    /**
     * Reads the buffer written by {@code SecurityGolemEntity.writeClientSideData},
     * finds the entity on the client, and ensures its loot container is correctly
     * sized (the client-side entity hasn't run {@code resizeLootInventory}).
     */
    private static SecurityGolemEntity prepareGolemFromBuf(Inventory playerInv, RegistryFriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int serverLootRows = buf.readInt();
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
        // Remove from ignore list: button IDs 300+
        if (buttonId >= 300 && buttonId < 400) {
            int idx = buttonId - 300;
            List<String> names = new ArrayList<>(golem.getIgnoreListNames());
            if (idx >= 0 && idx < names.size()) golem.removeFromIgnoreList(names.get(idx));
            return true;
        }
        // Remove from attack list: button IDs 400+
        if (buttonId >= 400 && buttonId < 500) {
            int idx = buttonId - 400;
            List<String> names = new ArrayList<>(golem.getAlwaysAttackListNames());
            if (idx >= 0 && idx < names.size()) golem.removeFromAttackList(names.get(idx));
            return true;
        }
        // Add to ignore list by picker index: button IDs 500+
        if (buttonId >= 500 && buttonId < 600) {
            int idx = buttonId - 500;
            List<String> available = getAvailableEntityNames(player);
            if (idx >= 0 && idx < available.size()) golem.addToIgnoreList(available.get(idx));
            return true;
        }
        // Add to attack list by picker index: button IDs 600+
        if (buttonId >= 600 && buttonId < 700) {
            int idx = buttonId - 600;
            List<String> available = getAvailableEntityNames(player);
            if (idx >= 0 && idx < available.size()) golem.addToAttackList(available.get(idx));
            return true;
        }
        return false;
    }

    private List<String> getAvailableEntityNames(Player player) {
        Set<String> existing = new LinkedHashSet<>();
        existing.addAll(golem.getIgnoreListNames());
        existing.addAll(golem.getAlwaysAttackListNames());

        Set<String> seen = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();

        // Online players
        var server = golem.level().getServer();
        if (server != null) {
            for (var sp : server.getPlayerList().getPlayers()) {
                String name = sp.getName().getString();
                if (!existing.contains(name) && seen.add(name)) names.add(name);
            }
        }

        // All living entities in golem's level
        var level = golem.level();
        for (var e : level.getEntitiesOfClass(LivingEntity.class,
                golem.getBoundingBox().inflate(128.0),
                e -> e.isAlive() && e != golem && !(e instanceof net.minecraft.world.entity.player.Player))) {
            String name = e.getName().getString();
            if (!existing.contains(name) && seen.add(name)) names.add(name);
        }
        return names;
    }

    public SecurityGolemEntity getGolem() { return golem; }
    public int getLootRows() { return lootContainer.getContainerSize() / 9; }
    public ContainerData getData() { return data; }
    public int getPlayerInvY() { return playerInvY; }
    public int getGuiHeight() { return playerInvY + 83; }
}
