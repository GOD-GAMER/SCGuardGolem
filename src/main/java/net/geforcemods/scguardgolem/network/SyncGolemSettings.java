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
 * Client -> Server: Sync golem settings from the GUI.
 */
public record SyncGolemSettings(
        int entityId,
        int threatMode,
        double patrolSpeed,
        boolean patrolling,
        String chestPassword,
        boolean cameraEnabled
) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(SCGuardGolem.MODID, "sync_golem_settings");

    public SyncGolemSettings(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readBoolean(), buf.readUtf(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(threatMode);
        buf.writeDouble(patrolSpeed);
        buf.writeBoolean(patrolling);
        buf.writeUtf(chestPassword);
        buf.writeBoolean(cameraEnabled);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static void handle(SyncGolemSettings packet, PlayPayloadContext ctx) {
        ctx.workHandler().execute(() -> {
            ctx.player().ifPresent(player -> {
                Level level = player.level();
                if (level.getEntity(packet.entityId()) instanceof SecurityGolemEntity golem) {
                    if (!golem.isOwner(player)) return;

                    golem.setThreatMode(SecurityGolemEntity.ThreatMode.fromOrdinal(packet.threatMode()));
                    golem.setPatrolSpeed(packet.patrolSpeed());
                    golem.setPatrolling(packet.patrolling());
                    golem.setChestPassword(packet.chestPassword());
                    golem.setCameraEnabled(packet.cameraEnabled());
                }
            });
        });
    }
}
