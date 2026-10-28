package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.loyalty.api.Bond;
import com.xziggsx.horseloyalty.personality.api.Personality;
import com.xziggsx.horseloyalty.riding.api.RideModifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;

/**
 * How personalities change riding. Lazy horses drop gaits sooner and more often; horses with a
 * buck-off personality may throw their rider when they refuse to go faster; skittish horses bolt
 * when a creeper comes near. Riding itself doesn't know personalities exist.
 */
final class PersonalityRideModifier implements RideModifier {
	@Override
	public double gaitDropChance(AbstractHorse horse, Player rider, double chance) {
		if (Personalities.of(horse) != Personality.LAZY) {
			return chance;
		}
		return Math.min(1, chance * PersonalitySettings.get().lazyGaitDropChanceMultiplier());
	}

	@Override
	public int gaitDropTicks(AbstractHorse horse, Player rider, int ticks) {
		if (Personalities.of(horse) != Personality.LAZY) {
			return ticks;
		}
		return (int) Math.max(20, Math.round(ticks * PersonalitySettings.get().lazyGaitDropTimeMultiplier()));
	}

	@Override
	public double buckChance(AbstractHorse horse, Player rider, double chance) {
		PersonalitySettings config = PersonalitySettings.get();
		return config.bucksOff(Personalities.of(horse)) ? Math.max(chance, config.buckOffChance()) : chance;
	}

	/**
	 * A skittish horse bolts when a creeper is close. Half as long at loyalty 4; a level 5 horse
	 * trusts its rider and doesn't bolt at all.
	 */
	@Override
	public int spook(AbstractHorse horse, Player rider) {
		if (Personalities.of(horse) != Personality.SKITTISH) {
			return 0;
		}
		PersonalitySettings config = PersonalitySettings.get();
		int level = Bond.get().level(horse, rider);
		int ticks = config.boltTicks();
		ticks = level >= Bond.MAX_LEVEL ? 0 : level == Bond.MAX_LEVEL - 1 ? ticks / 2 : ticks;
		double distance = config.boltCreeperDistance();
		if (ticks <= 0 || horse.level().getEntitiesOfClass(Creeper.class,
				horse.getBoundingBox().inflate(distance), Entity::isAlive).isEmpty()) {
			return 0;
		}
		return ticks;
	}
}
