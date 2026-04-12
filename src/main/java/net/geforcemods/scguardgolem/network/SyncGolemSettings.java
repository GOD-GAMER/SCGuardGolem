package net.geforcemods.scguardgolem.network;

import net.geforcemods.scguardgolem.SCGuardGolem;
import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

    public static final Type<SyncGolemSettings> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SCGuardGolem.MODID, "sync_golem_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGolemSettings> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncGolemSettings::entityId,
                    ByteBufCodecs.VAR_INT, SyncGolemSettings::threatMode,
                    ByteBufCodecs.DOUBLE, SyncGolemSettings::patrolSpeed,
                    ByteBufCodecs.BOOL, SyncGolemSettings::patrolling,
                    ByteBufCodecs.STRING_UTF8, SyncGolemSettings::chestPassword,
                    ByteBufCodecs.BOOL, SyncGolemSettings::cameraEnabled,
                    SyncGolemSettings::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncGolemSettings packet, IPayloadContext ctx) {
        Player player = ctx.player();
        Level level = player.level();

        if (level.getEntity(packet.entityId()) instanceof SecurityGolemEntity golem) {
            if (!golem.isOwner(player)) return;

            golem.setThreatMode(SecurityGolemEntity.ThreatMode.fromOrdinal(packet.threatMode()));
            golem.setPatrolSpeed(packet.patrolSpeed());
            golem.setPatrolling(packet.patrolling());
            golem.setChestPassword(packet.chestPassword());
            golem.setCameraEnabled(packet.cameraEnabled());
        }
    }
}
