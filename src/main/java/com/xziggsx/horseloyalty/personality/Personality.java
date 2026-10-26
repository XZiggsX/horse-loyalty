package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

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
 * Foals take after their parents (see {@link #inherit}). Names live in the lang file under
 * {@code horseloyalty.personality.<id>}. Odds and numbers are in the user config.
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

	/**
	 * Saved with the horse and synced to every client: riding runs on the rider's client,
	 * so the lazy gait drops and skittish bolts need to know it there.
	 */
	public static final AttachmentType<Personality> DATA = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "personality"),
			builder -> builder.persistent(CODEC).syncWith(STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/** Forces the class to load so the attachment is registered during mod init. */
	public static void init() {
	}

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

	/**
	 * This horse's personality, as far as behaviour goes. STEADY when personalities are turned
	 * off, for llamas and camels, and for a horse that hasn't been given one yet. Works on both sides.
	 */
	public static Personality of(AbstractHorse horse) {
		if (!HorseLoyaltyConfig.get().personalityEnabled() || !Loyalty.appliesTo(horse)) {
			return STEADY;
		}
		return horse.getAttachedOrElse(DATA, STEADY);
	}

	/** Whether this player knows the horse well enough to see its personality. */
	public static boolean isRevealedTo(AbstractHorse horse, Player player) {
		return HorseLoyaltyConfig.get().personalityEnabled() && horse.hasAttached(DATA)
				&& Loyalty.level(horse, player) >= HorseLoyaltyConfig.get().personalityRevealLevel();
	}

	/**
	 * Gives a horse a random personality if it has none yet. Called every server tick, so horses
	 * from before the mod was added (and wild ones) get one the first time they're loaded.
	 */
	public static void ensureAssigned(AbstractHorse horse) {
		if (Loyalty.appliesTo(horse) && !horse.hasAttached(DATA)) {
			horse.setAttached(DATA, roll(horse.getRandom()));
		}
	}

	/**
	 * Gives a newly bred foal a personality from its parents: by default a 40% chance of each
	 * parent's, and 20% of a fresh roll. When both parents share one, the foal is much more
	 * likely to have it too, so you can breed for a line of brave horses.
	 */
	public static void inherit(AbstractHorse foal, AbstractHorse parentA, AbstractHorse parentB) {
		if (!Loyalty.appliesTo(foal)) {
			return;
		}
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		RandomSource random = foal.getRandom();
		Personality a = parentA.getAttachedOrElse(DATA, roll(random));
		Personality b = parentB.getAttachedOrElse(DATA, roll(random));
		double chance = random.nextDouble();
		Personality result;
		if (a == b) {
			result = chance < config.personalitySameParentsChance() ? a : roll(random);
		} else if (chance < config.personalityInheritChance()) {
			result = a;
		} else if (chance < config.personalityInheritChance() * 2) {
			result = b;
		} else {
			result = roll(random);
		}
		foal.setAttached(DATA, result);
	}

	/** A random personality, weighted by the config. */
	public static Personality roll(RandomSource random) {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		double total = 0;
		for (Personality personality : VALUES) {
			total += config.personalityWeight(personality);
		}
		if (total <= 0) {
			return STEADY;
		}
		double pick = random.nextDouble() * total;
		for (Personality personality : VALUES) {
			pick -= config.personalityWeight(personality);
			if (pick < 0) {
				return personality;
			}
		}
		return STEADY;
	}
}
