package com.benbenlaw.castingmb.network.packets;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.entity.MBControllerBlockEntity;
import com.benbenlaw.castingmb.block.entity.MBSolidifierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public record SyncFuelTanks (BlockPos receivingBlock, @Nullable BlockPos fuelTank) implements CustomPacketPayload {

    public static final Type<SyncFuelTanks> TYPE = new Type<>(CastingMB.identifier("sync_fuel_tanks"));

    public static final IPayloadHandler<SyncFuelTanks> HANDLER = (packet, context) -> {

        Level level = context.player().level();
        if (level.getBlockEntity(packet.receivingBlock()) instanceof MBControllerBlockEntity controller) {
            controller.setClientSideFuelTankPos(packet.fuelTank);
        }
        if (level.getBlockEntity(packet.receivingBlock()) instanceof MBSolidifierBlockEntity solidifier) {
            solidifier.setClientSideFuelTankPos(packet.fuelTank());
        }
    };

    public static final StreamCodec<FriendlyByteBuf, SyncFuelTanks> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SyncFuelTanks::receivingBlock,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
            payload -> Optional.ofNullable(payload.fuelTank()),
            (controller, tankOpt) -> new SyncFuelTanks(controller, tankOpt.orElse(null))
    );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
