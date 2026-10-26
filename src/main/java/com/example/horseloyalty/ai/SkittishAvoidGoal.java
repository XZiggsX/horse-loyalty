package com.example.horseloyalty.ai;

import com.example.horseloyalty.config.HorseLoyaltyConfig;
import com.example.horseloyalty.personality.Personality;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;

/**
 * A skittish horse runs from creepers while nobody rides it. Ridden, the rider's client
 * handles it instead (the bolt in AbstractHorseRidingMixin), since riding runs there.
 */
public class SkittishAvoidGoal extends AvoidEntityGoal<Creeper> {
	private final AbstractHorse horse;

	public SkittishAvoidGoal(AbstractHorse horse) {
		super(horse, Creeper.class, (float) HorseLoyaltyConfig.get().skittishFleeDistance(), 1.2, 1.6);
		this.horse = horse;
	}

	@Override
	public boolean canUse() {
		return Personality.of(horse) == Personality.SKITTISH && !horse.isVehicle() && !horse.isLeashed()
				&& super.canUse();
	}
}
