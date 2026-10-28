package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.personality.api.Personality;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;

/**
 * A skittish horse runs from creepers while nobody rides it. Ridden, the rider's client
 * handles it instead (the bolt in HorseRidingMixin), since riding runs there.
 */
public class SkittishAvoidGoal extends AvoidEntityGoal<Creeper> {
	private final AbstractHorse horse;

	public SkittishAvoidGoal(AbstractHorse horse) {
		super(horse, Creeper.class, (float) PersonalitySettings.get().skittishFleeDistance(), 1.2, 1.6);
		this.horse = horse;
	}

	@Override
	public boolean canUse() {
		return Personalities.of(horse) == Personality.SKITTISH && !horse.isVehicle() && !horse.isLeashed()
				&& super.canUse();
	}
}
