package net.geforcemods.scguardgolem.network;

import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server: Request access to the golem's loot chest with a password.
 */
public record CheckChestPassword(int entityId, String password) implements CustomPacketPayload {

    public static final Type<CheckChestPassword> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SCGuardGolem.MODID, "check_chest_password"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CheckChestPassword> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CheckChestPassword::entityId,
                    ByteBufCodecs.STRING_UTF8, CheckChestPassword::password,
                    CheckChestPassword::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CheckChestPassword packet, IPayloadContext ctx) {
        Player player = ctx.player();
        Level level = player.level();

        if (level.getEntity(packet.entityId()) instanceof SecurityGolemEntity golem) {
            // Owner always has access; non-owners need password
            if (golem.isOwner(player) || golem.checkChestPassword(packet.password())) {
                golem.openLootScreen(player);
            }
        }
    }
}
