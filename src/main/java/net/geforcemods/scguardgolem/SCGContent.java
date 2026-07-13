package net.geforcemods.scguardgolem;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.entity.TamedGuardEntity;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.geforcemods.scguardgolem.item.SCGManualItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
//? if >=1.21.8 {
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
//? if >=1.21.11 {
import net.minecraft.world.entity.animal.golem.IronGolem;
//?} else
/*import net.minecraft.world.entity.animal.IronGolem;*/
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if forge {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
//? if <1.21.1 {
/*import net.neoforged.fml.common.Mod;
*///?} else
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

//? if forge {
/*@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
*///?} elif <1.21.1 {
/*@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
*///?} else {
@EventBusSubscriber(modid = SCGuardGolem.MODID)
//?}
public class SCGContent {

    // Registration. NeoForge uses the generic DeferredRegister across every
    // version; Forge 1.20.1 uses ForgeRegistries + RegistryObject.
    //? if forge {
    /*public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SCGuardGolem.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SCGuardGolem.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SCGuardGolem.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SCGuardGolem.MODID);
    *///?} else {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SCGuardGolem.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, SCGuardGolem.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SCGuardGolem.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, SCGuardGolem.MODID);
    //?}

    //? if forge {
    /*public static final RegistryObject<EntityType<SecurityGolemEntity>> SECURITY_GOLEM =
            ENTITY_TYPES.register("security_golem", () ->
                    EntityType.Builder.<SecurityGolemEntity>of(SecurityGolemEntity::new, MobCategory.MISC)
                            .sized(1.4F, 2.7F)
                            .setTrackingRange(128)
                            .setUpdateInterval(3)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("security_golem"));
    *///?} else {
    public static final DeferredHolder<EntityType<?>, EntityType<SecurityGolemEntity>> SECURITY_GOLEM =
            ENTITY_TYPES.register("security_golem", () ->
                    EntityType.Builder.<SecurityGolemEntity>of(SecurityGolemEntity::new, MobCategory.MISC)
                            .sized(1.4F, 2.7F)
                            .setTrackingRange(128)
                            .setUpdateInterval(3)
                            .setShouldReceiveVelocityUpdates(true)
                            //? if >=1.21.8 {
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(SCGuardGolem.MODID, "security_golem"))));
                            //?} else
                            /*.build(SCGuardGolem.MODID + ":security_golem"));*/
    //?}

    //? if forge {
    /*public static final RegistryObject<EntityType<TamedGuardEntity>> TAMED_GUARD =
            ENTITY_TYPES.register("tamed_guard", () ->
                    EntityType.Builder.<TamedGuardEntity>of(TamedGuardEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .setTrackingRange(80)
                            .setUpdateInterval(3)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("tamed_guard"));
    *///?} else {
    public static final DeferredHolder<EntityType<?>, EntityType<TamedGuardEntity>> TAMED_GUARD =
            ENTITY_TYPES.register("tamed_guard", () ->
                    EntityType.Builder.<TamedGuardEntity>of(TamedGuardEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .setTrackingRange(80)
                            .setUpdateInterval(3)
                            .setShouldReceiveVelocityUpdates(true)
                            //? if >=1.21.8 {
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(SCGuardGolem.MODID, "tamed_guard"))));
                            //?} else
                            /*.build(SCGuardGolem.MODID + ":tamed_guard"));*/
    //?}

    //? if forge {
    /*public static final RegistryObject<MenuType<GolemMenu>> GOLEM_MENU =
            MENU_TYPES.register("golem_menu", () -> IForgeMenuType.create(GolemMenu::new));
    *///?} else {
    public static final DeferredHolder<MenuType<?>, MenuType<GolemMenu>> GOLEM_MENU =
            MENU_TYPES.register("golem_menu", () -> IMenuTypeExtension.create(GolemMenu::new));
    //?}

    // "Open the guide" item — opens SecurityCraft's Manual (the golem's page is a
    // native SCManualPage registered by SCGManualPages). MC 1.21.2+ requires a
    // registry id in Item.Properties (setId), else the generic DeferredRegister
    // path throws "Item id not set" (boundary written as 1.21.8 — no 1.21.2-1.21.7 target).
    //? if forge {
    /*public static final RegistryObject<SCGManualItem> SCG_MANUAL =
            ITEMS.register("scg_manual", () ->
                    new SCGManualItem(new Item.Properties().stacksTo(1)));
    *///?} elif <1.21.8 {
    /*public static final DeferredHolder<Item, SCGManualItem> SCG_MANUAL =
            ITEMS.register("scg_manual", () ->
                    new SCGManualItem(new Item.Properties().stacksTo(1)));
    *///?} else {
    public static final DeferredHolder<Item, SCGManualItem> SCG_MANUAL =
            ITEMS.register("scg_manual", () ->
                    new SCGManualItem(new Item.Properties().stacksTo(1)
                            .setId(ResourceKey.create(Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(SCGuardGolem.MODID, "scg_manual")))));
    //?}

    //? if forge {
    /*public static final RegistryObject<CreativeModeTab> SCG_TAB =
            CREATIVE_MODE_TABS.register("scguardgolem", () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(SCG_MANUAL.get()))
                    .title(Component.translatable("itemGroup.scguardgolem"))
                    .displayItems((params, output) -> output.accept(new ItemStack(SCG_MANUAL.get())))
                    .build());
    *///?} else {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCG_TAB =
            CREATIVE_MODE_TABS.register("scguardgolem", () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(SCG_MANUAL.get()))
                    .title(Component.translatable("itemGroup.scguardgolem"))
                    .displayItems((params, output) -> output.accept(new ItemStack(SCG_MANUAL.get())))
                    .build());
    //?}

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        MENU_TYPES.register(modBus);
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SECURITY_GOLEM.get(),
                IronGolem.createAttributes()
                        .add(Attributes.MAX_HEALTH, 100.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.25D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                        .add(Attributes.ATTACK_DAMAGE, 15.0D)
                        //? if >=1.20.5
                        .add(Attributes.STEP_HEIGHT, 1.0D)
                        .build());
        event.put(TAMED_GUARD.get(),
                //? if >=1.21.11 {
                net.minecraft.world.entity.monster.zombie.Zombie.createAttributes()
                //?} else
                /*net.minecraft.world.entity.monster.Zombie.createAttributes()*/
                        .add(Attributes.MAX_HEALTH, 30.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.23D)
                        .add(Attributes.ATTACK_DAMAGE, 3.0D)
                        .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D)
                        .build());
    }
}
