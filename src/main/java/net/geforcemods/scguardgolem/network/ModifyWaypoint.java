package net.geforcemods.scguardgolem.network;

import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server: Add or remove a patrol waypoint.
 */
public record ModifyWaypoint(int entityId, int action, BlockPos pos) implements CustomPacketPayload {

    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;
    public static final int ACTION_CLEAR = 2;

    public static final Type<ModifyWaypoint> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SCGuardGolem.MODID, "modify_waypoint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModifyWaypoint> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ModifyWaypoint::entityId,
                    ByteBufCodecs.VAR_INT, ModifyWaypoint::action,
                    BlockPos.STREAM_CODEC, ModifyWaypoint::pos,
                    ModifyWaypoint::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ModifyWaypoint packet, IPayloadContext ctx) {
        Player player = ctx.player();
        Level level = player.level();

        if (level.getEntity(packet.entityId()) instanceof SecurityGolemEntity golem) {
            if (!golem.isOwner(player)) return;

            switch (packet.action()) {
                case ACTION_ADD -> golem.addWaypoint(packet.pos());
                case ACTION_REMOVE -> {
                    // pos.getX() is used as the index to remove
                    golem.removeWaypoint(packet.pos().getX());
                }
                case ACTION_CLEAR -> golem.clearWaypoints();
            }
        }
    }
}
