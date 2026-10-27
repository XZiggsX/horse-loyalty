package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to one player: their loyalty with a horse went up or down a level. The client shows a
 * toast for it (see {@code LoyaltyToasts} in the client source set).
 *
 * @param horseId the horse's entity id, to show its name
 * @param level   the new level
 * @param up      true for a level up, false for a level lost to hunger
 */
public record LoyaltyChangePayload(int horseId, int level, boolean up) implements CustomPacketPayload {
	public static final Type<LoyaltyChangePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "loyalty_change"));

	public static final StreamCodec<ByteBuf, LoyaltyChangePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, LoyaltyChangePayload::horseId,
			ByteBufCodecs.VAR_INT, LoyaltyChangePayload::level,
			ByteBufCodecs.BOOL, LoyaltyChangePayload::up,
			LoyaltyChangePayload::new);

	/** Registers the packet. Called once from the common entrypoint. */
	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
	}

	@Override
	public Type<LoyaltyChangePayload> type() {
		return TYPE;
	}
}
