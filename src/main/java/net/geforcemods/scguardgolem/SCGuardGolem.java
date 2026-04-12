package net.geforcemods.scguardgolem;

import net.geforcemods.scguardgolem.command.SCGCommands;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.geforcemods.securitycraft.items.KeycardItem;
import net.geforcemods.securitycraft.items.WireCuttersItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SCGuardGolem.MODID)
@EventBusSubscriber(modid = SCGuardGolem.MODID)
public class SCGuardGolem {
    public static final String MODID = "scguardgolem";
    public static final String VERSION = "1.2.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean scLoaded;

    public SCGuardGolem(IEventBus modBus) {
        scLoaded = ModList.get().isLoaded("securitycraft");
        SCGContent.register(modBus);
        LOGGER.info("SecurityCraft Guard Golem addon initialized (MC 26.1)");
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SCGCommands.register(event.getDispatcher());
    }

    /**
     * Right-click a vanilla Iron Golem with any SecurityCraft keycard to
     * convert it into a Security Guard Golem.
     * Right-click a Security Guard Golem with Wire Cutters to open the GUI.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        // Wire Cutters on a Security Golem → open configuration GUI
        if (event.getTarget() instanceof SecurityGolemEntity golem && isWireCutters(held)) {
            if (golem.isOwner(player) || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(golem);
                }
            } else {
                player.sendSystemMessage(Component.literal("\u00a7c[Security Golem] You are not the owner."));
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        // Keycard on a vanilla Iron Golem → convert to Security Golem
        if (!(event.getTarget() instanceof IronGolem ironGolem)) return;
        if (event.getTarget() instanceof SecurityGolemEntity) return;
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
        player.sendSystemMessage(
                Component.translatable("scguardgolem.conversion.success"));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static boolean isKeycardItem(ItemStack stack) {
        if (!scLoaded || stack.isEmpty()) return false;
        try {
            return stack.getItem() instanceof KeycardItem;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static boolean isWireCutters(ItemStack stack) {
        if (!scLoaded || stack.isEmpty()) return false;
        try {
            return stack.getItem() instanceof WireCuttersItem;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static boolean isPlayerTrustedByOwner(String playerUUID, String playerName,
                                                  String ownerUUID, String ownerName) {
        if (!scLoaded || ownerUUID == null || ownerUUID.isEmpty()) return false;
        return ownerUUID.equals(playerUUID);
    }
}
