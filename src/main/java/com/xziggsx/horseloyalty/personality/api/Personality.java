package com.xziggsx.horseloyalty.personality.api;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A horse's character, rolled once and kept for life. Each one changes a little of how the
 * horse behaves; most horses are steady and have no quirk at all.
 * <ul>
 *   <li>Steady: plain vanilla behaviour.</li>
 *   <li>Brave: only panics when burning or freezing, not when hit.</li>
 *   <li>Skittish: runs from creepers, and bolts at a gallop when one comes near while ridden.</li>
 *   <li>Curious: now and then wanders off toward a nearby village.</li>
 *   <li>Lazy: drops gaits sooner and more often while ridden.</li>
 * </ul>
 * Only the type lives here, so genetics' temperament trait can use it; what a horse has is read
 * through {@link PersonalityView}. Names live in the lang file under
 * {@code horseloyalty.personality.<id>}.
 */
public enum Personality {
	STEADY,
	BRAVE,
	SKITTISH,
	CURIOUS,
	LAZY;

	private static final Personality[] VALUES = values();

	public static final Codec<Personality> CODEC = Codec.STRING.xmap(Personality::byId, Personality::id);

	public static final StreamCodec<ByteBuf, Personality> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(i -> VALUES[Math.clamp(i, 0, VALUES.length - 1)], Personality::ordinal);

	/** The id used in saves, the config and lang keys, such as "skittish". */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Translated name, such as "Skittish". */
	public Component displayName() {
		return Component.translatable("horseloyalty.personality." + id());
	}

	/** The personality with this id, or STEADY for anything unknown. */
	public static Personality byId(String id) {
		for (Personality personality : VALUES) {
			if (personality.id().equals(id)) {
				return personality;
			}
		}
		return STEADY;
	}
}
