package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.SCGuardGolem;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.gui.screens.MenuScreens;
*///?} elif <1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
//?}

//? if forge {
/*@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
*///?} elif <1.21.1 {
/*@Mod.EventBusSubscriber(modid = SCGuardGolem.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
*///?} else {
@EventBusSubscriber(modid = SCGuardGolem.MODID, value = Dist.CLIENT)
//?}
public class SCGClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SCGContent.SECURITY_GOLEM.get(), SecurityGolemRenderer::new);
        // Tamed guards extend Zombie, so the vanilla humanoid renderer draws them on every version.
        event.registerEntityRenderer(SCGContent.TAMED_GUARD.get(), net.minecraft.client.renderer.entity.ZombieRenderer::new);
    }

    //? if forge {
    /*// Forge 1.20.1 has no RegisterMenuScreensEvent — register in client setup.
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(SCGContent.GOLEM_MENU.get(), GolemScreen::new));
    }
    *///?} else {
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SCGContent.GOLEM_MENU.get(), GolemScreen::new);
    }
    //?}
}
