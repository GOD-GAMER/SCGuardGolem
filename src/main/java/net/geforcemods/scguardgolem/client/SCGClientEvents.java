package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.SCGuardGolem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = SCGuardGolem.MODID, value = Dist.CLIENT)
public class SCGClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SCGContent.SECURITY_GOLEM.get(), SecurityGolemRenderer::new);
        event.registerEntityRenderer(SCGContent.GOLEM_CAMERA.get(), GolemCameraRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SCGContent.GOLEM_MENU.get(), GolemScreen::new);
    }
}
