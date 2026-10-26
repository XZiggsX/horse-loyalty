package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a horse remembers about one rider. A horse keeps one of these per player
 * (see {@link Loyalty#DATA}), so a sold or gifted horse starts fresh with its new rider.
 *
 * @param feeds       times this player has fed it (with a cooldown, see {@link HorseLoyaltyConfig})
 * @param rideTicks   ticks this player has spent riding it while it was moving
 * @param lastFedTick game time of the last counted feed, 0 if never fed
 * @param lostPoints  points lost to hunger (see {@link Hunger}). Never more than the points earned,
 *                    so a neglected horse bottoms out at level 1 instead of building up a debt.
 * @param bonusPoints points given outright, such as a bred foal's head start (see {@link Family})
 */
public record LoyaltyData(int feeds, int rideTicks, long lastFedTick, int lostPoints, int bonusPoints) {
	public static final LoyaltyData EMPTY = new LoyaltyData(0, 0, 0L, 0, 0);

	public static final Codec<LoyaltyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("feeds", 0).forGetter(LoyaltyData::feeds),
			Codec.INT.optionalFieldOf("ride_ticks", 0).forGetter(LoyaltyData::rideTicks),
			Codec.LONG.optionalFieldOf("last_fed_tick", 0L).forGetter(LoyaltyData::lastFedTick),
			Codec.INT.optionalFieldOf("lost_points", 0).forGetter(LoyaltyData::lostPoints),
			Codec.INT.optionalFieldOf("bonus_points", 0).forGetter(LoyaltyData::bonusPoints)
	).apply(instance, LoyaltyData::new));

	/** A copy with one more feed, fed at {@code gameTime}. */
	public LoyaltyData withFeed(long gameTime) {
		return new LoyaltyData(feeds + 1, rideTicks, gameTime, lostPoints, bonusPoints);
	}

	/** A copy with one more tick of riding. */
	public LoyaltyData withRideTick() {
		return new LoyaltyData(feeds, rideTicks + 1, lastFedTick, lostPoints, bonusPoints);
	}

	/** A copy that has lost {@code points} more to hunger, capped so points never go below 0. */
	public LoyaltyData withLoss(int points) {
		return new LoyaltyData(feeds, rideTicks, lastFedTick, Math.min(lostPoints + points, earnedPoints()), bonusPoints);
	}

	/** A copy with {@code points} more bonus points. */
	public LoyaltyData withBonus(int points) {
		return new LoyaltyData(feeds, rideTicks, lastFedTick, lostPoints, bonusPoints + Math.max(0, points));
	}

	/** Points earned from feeding, riding and bonuses, before hunger losses. */
	public int earnedPoints() {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		return feeds * config.pointsPerFeed() + rideTicks / config.ridingTicksPerPoint() + bonusPoints;
	}

	/** Current points: earned minus lost to hunger. */
	public int points() {
		return Math.max(0, earnedPoints() - lostPoints);
	}

	/** Whole minutes ridden, for display. */
	public int rideMinutes() {
		return rideTicks / 1200;
	}
}
