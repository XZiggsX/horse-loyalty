package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How a {@link Genome} is saved and synced: a map of locus id to "first/second" version names,
 * such as {@code {"cream": "cream/none"}}. Every locus is optional and unknown entries are
 * skipped, so saves survive loci being added or removed.
 */
public final class GenomeCodecs {
	public static final Codec<Genome> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
			.xmap(Genome::fromStrings, Genome::toStrings);

	public static final StreamCodec<ByteBuf, Genome> STREAM_CODEC =
			ByteBufCodecs.<ByteBuf, String, String, Map<String, String>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8)
					.map(Genome::fromStrings, Genome::toStrings);

	private GenomeCodecs() {
	}
}
