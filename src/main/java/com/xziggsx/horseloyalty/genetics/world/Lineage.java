package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A horse's parents as they were when it was born: their genomes and, if they had one, their
 * names. Only the parents, never grandparents, so it stays small however long the line gets.
 * A wild horse's parents are its herd's founders, which have no names.
 *
 * @param parentA genome of the parent that ran the breed, or of the first founder
 * @param parentB genome of the other parent or founder
 * @param nameA   parent A's custom name, if it had one
 * @param nameB   parent B's custom name, if it had one
 */
public record Lineage(Optional<Genome> parentA, Optional<Genome> parentB, Optional<String> nameA, Optional<String> nameB) {
	public static final Codec<Lineage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			GenomeCodecs.CODEC.optionalFieldOf("parent_a").forGetter(Lineage::parentA),
			GenomeCodecs.CODEC.optionalFieldOf("parent_b").forGetter(Lineage::parentB),
			Codec.STRING.optionalFieldOf("name_a").forGetter(Lineage::nameA),
			Codec.STRING.optionalFieldOf("name_b").forGetter(Lineage::nameB)
	).apply(instance, Lineage::new));

	public static final StreamCodec<ByteBuf, Lineage> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(GenomeCodecs.STREAM_CODEC), Lineage::parentA,
			ByteBufCodecs.optional(GenomeCodecs.STREAM_CODEC), Lineage::parentB,
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Lineage::nameA,
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Lineage::nameB,
			Lineage::new
	);

	/** Whether at least one parent's genome is known. */
	public boolean hasParents() {
		return parentA.isPresent() || parentB.isPresent();
	}
}
