package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.personality.api.PersonalityView;
import com.xziggsx.horseloyalty.riding.api.RideModifiers;

/** Personalities: steady, brave, skittish, curious and lazy horses, see {@link Personalities}. */
public final class PersonalityFeature implements Feature {
	public static final String ID = "personality";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public SettingsSection settings() {
		return new PersonalitySettings();
	}

	@Override
	public void registerData() {
		Personalities.init();
	}

	@Override
	public void wire(FeatureContext context) {
		context.provide(PersonalityView.KEY, new HorsePersonalityView());
		context.contribute(RideModifiers.ALL, new PersonalityRideModifier());
		context.on(HorseEvents.SERVER_SECOND, (horse, level) -> Personalities.ensureAssigned(horse));
		// Only if the genes didn't already decide it (genetics runs in the earlier GENES phase).
		context.on(HorseEvents.BRED, HorseEvents.TRAITS, (foal, parentA, parentB, breeder) -> {
			if (!foal.hasAttached(Personalities.DATA)) {
				Personalities.inherit(foal, parentA, parentB);
			}
		});
		// Skittish fleeing just below panicking, curious wandering alongside random strolling.
		context.goal(2, SkittishAvoidGoal::new);
		context.goal(6, CuriousWanderGoal::new);
		PersonalityCommands.register(context);
	}
}
