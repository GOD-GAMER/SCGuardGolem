package net.geforcemods.scguardgolem.network;

import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

/**
 * Client -> Server: Add or remove a patrol waypoint.
 */
public record ModifyWaypoint(int entityId, int action, BlockPos pos) implements CustomPacketPayload {

    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;
    public static final int ACTION_CLEAR = 2;

    public static final ResourceLocation ID = new ResourceLocation(SCGuardGolem.MODID, "modify_waypoint");

    public ModifyWaypoint(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(action);
        buf.writeBlockPos(pos);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static void handle(ModifyWaypoint packet, PlayPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            ctx.player().ifPresent(player -> {
                Level level = player.level();
                if (level.getEntity(packet.entityId()) instanceof SecurityGolemEntity golem) {
                    if (!golem.isOwner(player)) return;

                    switch (packet.action()) {
                        case ACTION_ADD -> golem.addWaypoint(packet.pos());
                        case ACTION_REMOVE -> golem.removeWaypoint(packet.pos().getX());
                        case ACTION_CLEAR -> golem.clearWaypoints();
                    }
                }
            });
        });
    }
}
