package com.xziggsx.horseloyalty.riding.api;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Why the rider's client sent its gait. */
public enum GaitEvent {
	/** The gait changed: the rider shifted, or the ride just started. */
	SHIFT,
	/** The horse dropped a gait on its own. */
	DROPPED,
	/** A skittish horse spooked and bolted. */
	BOLTED,
	/** The rider asked for more speed and the horse refused, rearing up instead. */
	PRANCED,
	/** As PRANCED, but the horse threw its rider off. */
	BUCKED;

	private static final GaitEvent[] VALUES = values();

	public static final StreamCodec<ByteBuf, GaitEvent> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(i -> VALUES[Math.clamp(i, 0, VALUES.length - 1)], GaitEvent::ordinal);
}
