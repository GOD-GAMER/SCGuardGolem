package net.geforcemods.scguardgolem.network;

import net.geforcemods.scguardgolem.entity.SecurityGolemEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CheckChestPassword {
    private final int entityId;
    private final String password;

    public CheckChestPassword(int entityId, String password) {
        this.entityId = entityId;
        this.password = password;
    }

    public CheckChestPassword(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.password = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(password);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player == null) return;
            Level level = player.level();
            if (level.getEntity(entityId) instanceof SecurityGolemEntity golem) {
                if (golem.isOwner(player) || golem.checkChestPassword(password)) {
                    golem.openLootScreen(player);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
