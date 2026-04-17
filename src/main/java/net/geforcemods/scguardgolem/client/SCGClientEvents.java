package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.inventory.GolemMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;

@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SCGClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SCGContent.SECURITY_GOLEM.get(), SecurityGolemRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(SCGContent.GOLEM_MENU.get(), GolemScreen::new));
    }
}
