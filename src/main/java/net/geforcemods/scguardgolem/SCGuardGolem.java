package net.geforcemods.scguardgolem;

import net.geforcemods.scguardgolem.command.SCGCommands;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.scguardgolem.network.CheckChestPassword;
import net.geforcemods.scguardgolem.network.ModifyWaypoint;
import net.geforcemods.scguardgolem.network.SyncGolemSettings;
import net.geforcemods.securitycraft.items.KeycardItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SCGuardGolem.MODID)
@EventBusSubscriber(modid = SCGuardGolem.MODID)
public class SCGuardGolem {
    public static final String MODID = "scguardgolem";
    public static final String VERSION = "1.2.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Cached flag — true when SecurityCraft is loaded at runtime. */
    public static boolean scLoaded;

    public SCGuardGolem(IEventBus modBus) {
        scLoaded = ModList.get().isLoaded("securitycraft");
        SCGContent.register(modBus);
        LOGGER.info("SecurityCraft Guard Golem addon initialized (MC 1.21.8)");
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SCGCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.2.0");
        registrar.playToServer(SyncGolemSettings.TYPE, SyncGolemSettings.STREAM_CODEC, SyncGolemSettings::handle);
        registrar.playToServer(ModifyWaypoint.TYPE, ModifyWaypoint.STREAM_CODEC, ModifyWaypoint::handle);
        registrar.playToServer(CheckChestPassword.TYPE, CheckChestPassword.STREAM_CODEC, CheckChestPassword::handle);
    }

    /**
     * Right-click a vanilla Iron Golem with any SecurityCraft keycard to
     * convert it into a Security Guard Golem.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof IronGolem ironGolem)) return;
        if (event.getTarget() instanceof SecurityGolemEntity) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        if (!isKeycardItem(held)) return;

        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        SecurityGolemEntity golem = SCGContent.SECURITY_GOLEM.get()
                .create(serverLevel, EntitySpawnReason.CONVERSION);
        if (golem == null) return;

        golem.snapTo(ironGolem.getX(), ironGolem.getY(), ironGolem.getZ(),
                ironGolem.getYRot(), ironGolem.getXRot());
        golem.setHealth(ironGolem.getHealth());
        golem.setPlayerCreated(ironGolem.isPlayerCreated());
        golem.setGolemOwner(player);

        ironGolem.discard();
        serverLevel.addFreshEntity(golem);

        held.shrink(1);
        player.displayClientMessage(
                Component.translatable("scguardgolem.conversion.success"), false);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Check whether an item stack is a SecurityCraft keycard. */
    public static boolean isKeycardItem(ItemStack stack) {
        if (!scLoaded || stack.isEmpty()) return false;
        try {
            return stack.getItem() instanceof KeycardItem;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * Check whether a player is the same as the golem owner via UUID comparison.
     */
    public static boolean isPlayerTrustedByOwner(String playerUUID, String playerName,
                                                  String ownerUUID, String ownerName) {
        if (!scLoaded || ownerUUID == null || ownerUUID.isEmpty()) return false;
        return ownerUUID.equals(playerUUID);
    }
}
