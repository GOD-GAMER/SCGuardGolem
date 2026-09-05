package net.geforcemods.scguardgolem.entity;

import java.util.List;
import java.util.UUID;

import net.geforcemods.securitycraft.items.ModuleItem;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.SaltData;
import net.geforcemods.securitycraft.network.client.OpenScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
//? if >=1.21.8 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

/**
 * The shared, parent-independent guard implementation: SecurityCraft module
 * inventory (binary effects), the {@code IPasscodeProtected} loot lock, and the
 * loot storage. Both the Iron Golem guard and tamed-mob guards can back their
 * {@code IModuleInventory}/{@code IPasscodeProtected} surface with one of these,
 * calling out through {@link Host} for the few things that need the live entity.
 *
 * <p>See {@code docs/TAMED-GUARDS-DESIGN.md}.
 */
public class GuardCore {

    public static final ModuleType[] ACCEPTED_MODULES = {
        ModuleType.HARMING, ModuleType.SPEED, ModuleType.SMART, ModuleType.STORAGE,
        ModuleType.ALLOWLIST, ModuleType.DENYLIST
    };
    public static final int MODULE_SLOTS = ACCEPTED_MODULES.length;
    public static final int MAX_LOOT_ROWS = 6;
    public static final long PASSCODE_COOLDOWN_MS = 5000L;

    /** The live-entity callbacks GuardCore needs. */
    public interface Host {
        Level level();
        int id();
        void openLootMenu(ServerPlayer player);
        /** Reapply module-driven attribute effects (called when modules change). */
        void onModulesChanged();
        /** How the entity sends a SecurityCraft screen-open packet to a player. */
        void sendPasscodeScreen(ServerPlayer player, OpenScreen.DataType type);
    }

    private final Host host;

    private final SimpleContainer moduleInventory;
    private SimpleContainer lootInventory = new SimpleContainer(9);

    // IPasscodeProtected state
    private byte[] passcode;
    private UUID saltKey;
    private boolean saveSalt = true;
    private long cooldownEnd = 0;

    public GuardCore(Host host) {
        this.host = host;
        this.moduleInventory = new SimpleContainer(MODULE_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                host.onModulesChanged();
            }
            //? if >=1.21.1 {
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
            //?} else {
            /*@Override
            public int getMaxStackSize() {
                return 1;
            }
            *///?}
        };
    }

    // -- Module inventory (backs IModuleInventory) --
    public SimpleContainer moduleContainer() { return moduleInventory; }

    public NonNullList<ItemStack> moduleView() {
        NonNullList<ItemStack> view = NonNullList.withSize(moduleInventory.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < moduleInventory.getContainerSize(); i++)
            view.set(i, moduleInventory.getItem(i));
        return view;
    }

    public ModuleType[] acceptedModules() { return ACCEPTED_MODULES; }

    public boolean hasModule(ModuleType type) {
        for (int i = 0; i < moduleInventory.getContainerSize(); i++) {
            ItemStack s = moduleInventory.getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof ModuleItem m && m.getModuleType() == type) return true;
        }
        return false;
    }

    public boolean hasHarmingModule() { return hasModule(ModuleType.HARMING); }
    public boolean hasSpeedModule()   { return hasModule(ModuleType.SPEED); }
    public boolean hasSmartModule()   { return hasModule(ModuleType.SMART); }
    public boolean hasStorageModule() { return hasModule(ModuleType.STORAGE); }
    public boolean hasAllowlistModule() { return hasModule(ModuleType.ALLOWLIST); }
    public boolean hasDenylistModule()  { return hasModule(ModuleType.DENYLIST); }

    public static boolean isValidModuleForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!(stack.getItem() instanceof ModuleItem moduleItem)) return false;
        return slot >= 0 && slot < ACCEPTED_MODULES.length && moduleItem.getModuleType() == ACCEPTED_MODULES[slot];
    }

    // -- Loot inventory --
    public SimpleContainer lootContainer() { return lootInventory; }
    public void setLootContainer(SimpleContainer inv) { this.lootInventory = inv; }

    public int lootRows() { return hasStorageModule() ? MAX_LOOT_ROWS : 1; }

    public void resizeLoot() {
        int needed = lootRows() * 9;
        if (lootInventory.getContainerSize() == needed) return;
        SimpleContainer newInv = new SimpleContainer(needed);
        for (int i = 0; i < Math.min(lootInventory.getContainerSize(), needed); i++)
            newInv.setItem(i, lootInventory.getItem(i));
        if (host.level() instanceof ServerLevel serverLevel) {
            for (int i = needed; i < lootInventory.getContainerSize(); i++) {
                ItemStack overflow = lootInventory.getItem(i);
                if (!overflow.isEmpty()) {
                    ItemEntity e = new ItemEntity(serverLevel, 0, 0, 0, overflow);
                    serverLevel.addFreshEntity(e);
                }
            }
        }
        lootInventory = newInv;
    }

    /** Collect nearby dropped items when a Storage module is installed. */
    public void pickupNearbyItems(AABB box) {
        if (!hasStorageModule()) return;
        List<ItemEntity> items = host.level().getEntitiesOfClass(ItemEntity.class, box);
        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;
            ItemStack remainder = lootInventory.addItem(itemEntity.getItem().copy());
            if (remainder.isEmpty()) itemEntity.discard();
            else itemEntity.setItem(remainder);
        }
    }

    // -- IPasscodeProtected backing --
    public byte[] getPasscode() { return passcode == null || passcode.length == 0 ? null : passcode; }
    public void setPasscode(byte[] passcode) { this.passcode = passcode; }
    public UUID getSaltKey() { return saltKey; }
    public void setSaltKey(UUID saltKey) { this.saltKey = saltKey; }
    public void setSaveSalt(boolean saveSalt) { this.saveSalt = saveSalt; }
    public boolean shouldSaveSalt() { return saveSalt; }
    public long getCooldownEnd() { return cooldownEnd; }
    public boolean isOnCooldown() { return System.currentTimeMillis() < cooldownEnd; }
    public void startCooldown() { if (!isOnCooldown()) cooldownEnd = System.currentTimeMillis() + PASSCODE_COOLDOWN_MS; }

    public void activate(ServerPlayer player) { host.openLootMenu(player); }

    public void openCheckScreen(ServerPlayer player) {
        if (getPasscode() != null) host.sendPasscodeScreen(player, OpenScreen.DataType.CHECK_PASSCODE_FOR_ENTITY);
    }

    public void openSetScreen(ServerPlayer player) {
        host.sendPasscodeScreen(player, OpenScreen.DataType.SET_PASSCODE_FOR_ENTITY);
    }

    public void releaseSalt() {
        if (getSaltKey() != null) SaltData.removeSalt(getSaltKey());
    }

    // -- Persistence for the module + loot inventories (era-guarded, three shapes).
    // The entity handles owner/passcode/shutdown NBT via its own inherited defaults.
    //? if >=1.21.8 {
    public void save(ValueOutput tag) {
        moduleInventory.storeAsItemList(tag.list("Modules", ItemStack.CODEC));
        lootInventory.storeAsItemList(tag.list("LootItems", ItemStack.CODEC));
    }

    public void load(ValueInput tag) {
        moduleInventory.clearContent();
        moduleInventory.fromItemList(tag.listOrEmpty("Modules", ItemStack.CODEC));
        resizeLoot();
        lootInventory.clearContent();
        lootInventory.fromItemList(tag.listOrEmpty("LootItems", ItemStack.CODEC));
    }
    //?} elif >=1.21.1 {
    /*public void save(CompoundTag tag) {
        tag.put("Modules", saveContainer(moduleInventory));
        tag.put("LootItems", saveContainer(lootInventory));
    }

    private net.minecraft.nbt.ListTag saveContainer(SimpleContainer inv) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag it = (CompoundTag) s.save(host.level().registryAccess());
                it.putInt("Slot", i);
                list.add(it);
            }
        }
        return list;
    }

    public void load(CompoundTag tag) {
        loadContainer(moduleInventory, tag, "Modules");
        resizeLoot();
        loadContainer(lootInventory, tag, "LootItems");
    }

    private void loadContainer(SimpleContainer inv, CompoundTag tag, String key) {
        inv.clearContent();
        if (!tag.contains(key)) return;
        net.minecraft.nbt.ListTag list = tag.getList(key, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag it = list.getCompound(i);
            int slot = it.getInt("Slot");
            ItemStack.parse(host.level().registryAccess(), it).ifPresent(s -> { if (slot >= 0 && slot < inv.getContainerSize()) inv.setItem(slot, s); });
        }
    }
    *///?} else {
    /*public void save(CompoundTag tag) {
        tag.put("Modules", saveContainer(moduleInventory));
        tag.put("LootItems", saveContainer(lootInventory));
    }

    private net.minecraft.nbt.ListTag saveContainer(SimpleContainer inv) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                CompoundTag it = s.save(new CompoundTag());
                it.putInt("Slot", i);
                list.add(it);
            }
        }
        return list;
    }

    public void load(CompoundTag tag) {
        loadContainer(moduleInventory, tag, "Modules");
        resizeLoot();
        loadContainer(lootInventory, tag, "LootItems");
    }

    private void loadContainer(SimpleContainer inv, CompoundTag tag, String key) {
        inv.clearContent();
        if (!tag.contains(key)) return;
        net.minecraft.nbt.ListTag list = tag.getList(key, 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag it = list.getCompound(i);
            int slot = it.getInt("Slot");
            ItemStack s = ItemStack.of(it);
            if (!s.isEmpty() && slot >= 0 && slot < inv.getContainerSize()) inv.setItem(slot, s);
        }
    }
    *///?}
}
