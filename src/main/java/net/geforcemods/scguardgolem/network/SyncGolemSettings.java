package net.geforcemods.scguardgolem.network;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncGolemSettings {
    private final int entityId;
    private final int threatMode;
    private final double patrolSpeed;
    private final boolean patrolling;
    private final String chestPassword;
    private final boolean cameraEnabled;

    public SyncGolemSettings(int entityId, int threatMode, double patrolSpeed,
                             boolean patrolling, String chestPassword, boolean cameraEnabled) {
        this.entityId = entityId;
        this.threatMode = threatMode;
        this.patrolSpeed = patrolSpeed;
        this.patrolling = patrolling;
        this.chestPassword = chestPassword;
        this.cameraEnabled = cameraEnabled;
    }

    public SyncGolemSettings(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.threatMode = buf.readVarInt();
        this.patrolSpeed = buf.readDouble();
        this.patrolling = buf.readBoolean();
        this.chestPassword = buf.readUtf();
        this.cameraEnabled = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(threatMode);
        buf.writeDouble(patrolSpeed);
        buf.writeBoolean(patrolling);
        buf.writeUtf(chestPassword);
        buf.writeBoolean(cameraEnabled);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player == null) return;
            Level level = player.level();
            if (level.getEntity(entityId) instanceof SecurityGolemEntity golem) {
                if (!golem.isOwner(player)) return;
                golem.setThreatMode(SecurityGolemEntity.ThreatMode.fromOrdinal(threatMode));
                golem.setPatrolSpeed(patrolSpeed);
                golem.setPatrolling(patrolling);
                golem.setChestPassword(chestPassword);
                golem.setCameraEnabled(cameraEnabled);
            }
        });
        context.setPacketHandled(true);
    }
}
