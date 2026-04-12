package net.geforcemods.scguardgolem.network;

import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

/**
 * Client -> Server: Request access to the golem's loot chest with a password.
 */
public record CheckChestPassword(int entityId, String password) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(SCGuardGolem.MODID, "check_chest_password");

    public CheckChestPassword(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readUtf());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(password);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static void handle(CheckChestPassword packet, PlayPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            ctx.player().ifPresent(player -> {
                Level level = player.level();
                if (level.getEntity(packet.entityId()) instanceof SecurityGolemEntity golem) {
                    if (golem.isOwner(player) || golem.checkChestPassword(packet.password())) {
                        golem.openLootScreen(player);
                    }
                }
            });
        });
    }
}
