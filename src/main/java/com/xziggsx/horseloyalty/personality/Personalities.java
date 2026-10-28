package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.loyalty.api.Bond;
import com.xziggsx.horseloyalty.personality.api.Personality;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Which {@link Personality} each horse has: rolled once, inherited by foals (see {@link #inherit})
 * and kept for life. Odds and numbers are in the user config.
 */
public final class Personalities {
	/**
	 * Saved with the horse and synced to every client: riding runs on the rider's client,
	 * so the lazy gait drops and skittish bolts need to know it there.
	 */
	public static final AttachmentType<Personality> DATA = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "personality"),
			builder -> builder.persistent(Personality.CODEC).syncWith(Personality.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	private Personalities() {
	}

	/** Forces the class to load so the attachment is registered during mod init. */
	public static void init() {
	}

	/**
	 * This horse's personality, as far as behaviour goes. STEADY for horses out of scope, and for a horse that hasn't been given one yet. Works on both sides.
	 */
	public static Personality of(AbstractHorse horse) {
		if (!HorseScope.inScope(horse)) {
			return Personality.STEADY;
		}
		return horse.getAttachedOrElse(DATA, Personality.STEADY);
	}

	/** Whether a horse that would panic stands its ground instead: a brave horse, unless it's burning or freezing. */
	public static boolean standsGround(AbstractHorse horse) {
		return of(horse) == Personality.BRAVE && !horse.isOnFire() && !horse.isFreezing();
	}

	/** Whether this player knows the horse well enough to see its personality. */
	public static boolean isRevealedTo(AbstractHorse horse, Player player) {
		return horse.hasAttached(DATA)
				&& Bond.get().level(horse, player) >= PersonalitySettings.get().revealLevel();
	}

	/**
	 * Gives a horse a random personality if it has none yet. Called on the server for every horse,
	 * so horses from before the mod was added (and wild ones) get one the first time they're loaded.
	 */
	public static void ensureAssigned(AbstractHorse horse) {
		if (HorseScope.inScope(horse) && !horse.hasAttached(DATA)) {
			horse.setAttached(DATA, roll(horse.getRandom()));
		}
	}

	/**
	 * Gives a newly bred foal a personality from its parents: by default a 40% chance of each
	 * parent's, and 20% of a fresh roll. When both parents share one, the foal is much more
	 * likely to have it too, so you can breed for a line of brave horses.
	 */
	public static void inherit(AbstractHorse foal, AbstractHorse parentA, AbstractHorse parentB) {
		if (!HorseScope.inScope(foal)) {
			return;
		}
		PersonalitySettings config = PersonalitySettings.get();
		RandomSource random = foal.getRandom();
		Personality a = parentOrRoll(parentA, random);
		Personality b = parentOrRoll(parentB, random);
		double chance = random.nextDouble();
		Personality result;
		if (a == b) {
			result = chance < config.sameParentsChance() ? a : roll(random);
		} else if (chance < config.inheritChance()) {
			result = a;
		} else if (chance < config.inheritChance() * 2) {
			result = b;
		} else {
			result = roll(random);
		}
		foal.setAttached(DATA, result);
	}

	/** A parent's personality, or a fresh roll if it has none yet (only rolled when needed). */
	private static Personality parentOrRoll(AbstractHorse parent, RandomSource random) {
		Personality personality = parent.getAttached(DATA);
		return personality != null ? personality : roll(random);
	}

	/** A random personality, weighted by the config. */
	public static Personality roll(RandomSource random) {
		PersonalitySettings config = PersonalitySettings.get();
		Personality[] all = Personality.values();
		double total = 0;
		for (Personality personality : all) {
			total += config.weight(personality);
		}
		if (total <= 0) {
			return Personality.STEADY;
		}
		double pick = random.nextDouble() * total;
		for (Personality personality : all) {
			pick -= config.weight(personality);
			if (pick < 0) {
				return personality;
			}
		}
		return Personality.STEADY;
	}
}
