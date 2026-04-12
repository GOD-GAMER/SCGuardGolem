package net.geforcemods.scguardgolem;

import net.geforcemods.scguardgolem.entity.GolemCameraEntity;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.geforcemods.scguardgolem.item.SCGManualItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SCGContent {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SCGuardGolem.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SCGuardGolem.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SCGuardGolem.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SCGuardGolem.MODID);

    public static final RegistryObject<EntityType<SecurityGolemEntity>> SECURITY_GOLEM =
            ENTITY_TYPES.register("security_golem", () ->
                    EntityType.Builder.<SecurityGolemEntity>of(SecurityGolemEntity::new, MobCategory.MISC)
                            .sized(1.4F, 2.7F)
                            .clientTrackingRange(128)
                            .updateInterval(3)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(new ResourceLocation(SCGuardGolem.MODID, "security_golem").toString()));

    public static final RegistryObject<EntityType<GolemCameraEntity>> GOLEM_CAMERA =
            ENTITY_TYPES.register("golem_camera", () ->
                    EntityType.Builder.<GolemCameraEntity>of(GolemCameraEntity::new, MobCategory.MISC)
                            .sized(0.0F, 0.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .setShouldReceiveVelocityUpdates(false)
                            .noSummon()
                            .build(new ResourceLocation(SCGuardGolem.MODID, "golem_camera").toString()));

    public static final RegistryObject<SCGManualItem> SCG_MANUAL =
            ITEMS.register("scg_manual", () -> new SCGManualItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<MenuType<GolemMenu>> GOLEM_MENU =
            MENU_TYPES.register("golem_menu", () ->
                    IForgeMenuType.create(GolemMenu::new));

    public static final RegistryObject<CreativeModeTab> SCG_TAB =
            CREATIVE_MODE_TABS.register("scguardgolem", () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(SCG_MANUAL.get()))
                    .title(Component.translatable("itemGroup.scguardgolem"))
                    .displayItems((params, output) -> output.accept(new ItemStack(SCG_MANUAL.get())))
                    .build());

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
                        .build());
    }
}
