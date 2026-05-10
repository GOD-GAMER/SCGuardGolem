package net.geforcemods.scguardgolem.client;

import net.geforcemods.scguardgolem.SCGContent;
import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.network.AddWaypointPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

@EventBusSubscriber(modid = SCGuardGolem.MODID, value = Dist.CLIENT)
public class SCGClientEvents {

    // Double-crouch detection state
    private static boolean wasSneaking = false;
    private static long lastSneakTime = 0L;
    private static final long DOUBLE_SNEAK_WINDOW_MS = 500L;

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SCGContent.SECURITY_GOLEM.get(), SecurityGolemRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SCGContent.GOLEM_MENU.get(), GolemScreen::new);
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(event.getEntity() instanceof LocalPlayer player)) return;
        if (player != mc.player) return;

        // Must be holding wire cutters in either hand
        boolean hasWireCutters = SCGuardGolem.isWireCutters(player.getMainHandItem())
                || SCGuardGolem.isWireCutters(player.getOffhandItem());
        if (!hasWireCutters) {
            wasSneaking = player.isCrouching();
            return;
        }

        boolean isSneaking = player.isCrouching();

        // Detect leading edge of crouch (was not crouching, now is)
        if (isSneaking && !wasSneaking) {
            long now = System.currentTimeMillis();
            if (now - lastSneakTime <= DOUBLE_SNEAK_WINDOW_MS) {
                // Double-crouch detected — send waypoint packet
                ClientPacketDistributor.sendToServer(new AddWaypointPayload());
                lastSneakTime = 0L; // reset so triple-crouch doesn't re-trigger
            } else {
                lastSneakTime = now;
            }
        }

        wasSneaking = isSneaking;
    }
}

