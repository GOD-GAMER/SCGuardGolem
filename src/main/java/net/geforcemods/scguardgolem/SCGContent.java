package net.geforcemods.scguardgolem;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.item.SCGManualItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SCGContent {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SCGuardGolem.MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(SCGuardGolem.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SCGuardGolem.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SecurityGolemEntity>> SECURITY_GOLEM =
            ENTITY_TYPES.register("security_golem", () ->
                    EntityType.Builder.<SecurityGolemEntity>of(SecurityGolemEntity::new, MobCategory.MISC)
                            .sized(1.4F, 2.7F)
                            .clientTrackingRange(128)
                            .updateInterval(3)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(SCGuardGolem.MODID + ":security_golem"));

    public static final DeferredItem<SCGManualItem> SCG_MANUAL =
            ITEMS.register("scg_manual", () -> new SCGManualItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCG_TAB =
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
