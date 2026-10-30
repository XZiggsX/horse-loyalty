package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.hunger.Tastes.Opinion;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * How a horse shows what it thinks of a food it just ate, for everyone nearby: a couple of hearts
 * for any food, a shower of hearts and a whinny for a favorite, storm clouds and a snorting rear
 * for a food it dislikes.
 */
final class FeedReactions {
	private static final int NEUTRAL_HEARTS = 2;
	private static final int FAVORITE_HEARTS = 7;
	private static final int DISLIKE_CLOUDS = 6;

	private FeedReactions() {
	}

	/** Plays the reaction to a food with this opinion. */
	static void play(AbstractHorse horse, ServerLevel level, Opinion opinion) {
		switch (opinion) {
			case FAVORITE -> {
				particles(horse, level, ParticleTypes.HEART, FAVORITE_HEARTS);
				// The horse's own voice, so donkeys bray and mules sound like mules.
				horse.playAmbientSound();
			}
			case NEUTRAL -> particles(horse, level, ParticleTypes.HEART, NEUTRAL_HEARTS);
			case DISLIKED -> {
				particles(horse, level, ParticleTypes.ANGRY_VILLAGER, DISLIKE_CLOUDS);
				// Rears and makes its angry sound, as when it throws a rider.
				horse.makeMad();
			}
		}
	}

	/** Around the horse's head and back. */
	private static void particles(AbstractHorse horse, ServerLevel level, ParticleOptions type, int count) {
		level.sendParticles(type, horse.getX(), horse.getY() + horse.getBbHeight() * 0.9, horse.getZ(),
				count, horse.getBbWidth() * 0.4, horse.getBbHeight() * 0.2, horse.getBbWidth() * 0.4, 0.0);
	}
}
