package net.geforcemods.scguardgolem.network;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ModifyWaypoint {
    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;
    public static final int ACTION_CLEAR = 2;

    private final int entityId;
    private final int action;
    private final BlockPos pos;

    public ModifyWaypoint(int entityId, int action, BlockPos pos) {
        this.entityId = entityId;
        this.action = action;
        this.pos = pos;
    }

    public ModifyWaypoint(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.action = buf.readVarInt();
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(action);
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player == null) return;
            Level level = player.level();
            if (level.getEntity(entityId) instanceof SecurityGolemEntity golem) {
                if (!golem.isOwner(player)) return;
                switch (action) {
                    case ACTION_ADD -> golem.addWaypoint(pos);
                    case ACTION_REMOVE -> golem.removeWaypoint(pos.getX());
                    case ACTION_CLEAR -> golem.clearWaypoints();
                }
            }
        });
        context.setPacketHandled(true);
    }
}
