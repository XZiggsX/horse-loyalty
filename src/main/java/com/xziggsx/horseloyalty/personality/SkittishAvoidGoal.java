package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.core.HorseTags;
import com.xziggsx.horseloyalty.personality.api.Personality;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * A skittish horse runs from creepers (anything in the {@code horseloyalty:spooks_horses} tag) while
 * nobody rides it. Ridden, the rider's client
 * handles it instead (the bolt in HorseRidingMixin), since riding runs there.
 */
public class SkittishAvoidGoal extends AvoidEntityGoal<LivingEntity> {
	private final AbstractHorse horse;

	public SkittishAvoidGoal(AbstractHorse horse) {
		super(horse, LivingEntity.class, (float) PersonalitySettings.get().skittishFleeDistance(), 1.2, 1.6,
				entity -> entity.getType().builtInRegistryHolder().is(HorseTags.SPOOKS_HORSES));
		this.horse = horse;
	}

	@Override
	public boolean canUse() {
		return Personalities.of(horse) == Personality.SKITTISH && !horse.isVehicle() && !horse.isLeashed()
				&& super.canUse();
	}
}
