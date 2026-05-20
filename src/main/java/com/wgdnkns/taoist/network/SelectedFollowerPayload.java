package com.wgdnkns.taoist.network;

import com.wgdnkns.taoist.Taoistwith15dogs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record SelectedFollowerPayload(UUID selectedFollowerUuid) implements CustomPacketPayload {
    private static final UUID NULL_SENTINEL = new UUID(0, 0);

    public static final CustomPacketPayload.Type<SelectedFollowerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Taoistwith15dogs.MODID, "selected_follower"));

    public static final StreamCodec<ByteBuf, SelectedFollowerPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SelectedFollowerPayload decode(ByteBuf buf) {
            FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
            UUID uuid = friendly.readUUID();
            if (NULL_SENTINEL.equals(uuid)) {
                uuid = null;
            }
            return new SelectedFollowerPayload(uuid);
        }

        @Override
        public void encode(ByteBuf buf, SelectedFollowerPayload payload) {
            FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
            UUID uuid = payload.selectedFollowerUuid();
            friendly.writeUUID(uuid != null ? uuid : NULL_SENTINEL);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
