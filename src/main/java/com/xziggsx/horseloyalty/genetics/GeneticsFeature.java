package com.xziggsx.horseloyalty.genetics;

import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.genetics.dev.GeneticsCommands;
import com.xziggsx.horseloyalty.genetics.world.GeneticsData;
import com.xziggsx.horseloyalty.genetics.world.HorseGenetics;
import com.xziggsx.horseloyalty.horseinfo.api.InfoProvider;

/** Genetics: coat, marking and temperament genes, herd founders, inheritance and lineage. */
public final class GeneticsFeature implements Feature {
	public static final String ID = "genetics";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public SettingsSection settings() {
		return new GeneticsSettings();
	}

	@Override
	public void registerData() {
		GeneticsData.init();
	}

	@Override
	public void wire(FeatureContext context) {
		context.on(HorseEvents.WILD_SPAWN, HorseGenetics::onSpawn);
		context.on(HorseEvents.BRED, HorseEvents.GENES, (foal, parentA, parentB, breeder) -> HorseGenetics.onBred(foal, parentA, parentB));
		context.on(HorseEvents.SERVER_SECOND, (horse, level) -> HorseGenetics.ensureAssigned(horse));
		context.contribute(InfoProvider.ALL, new GeneticsInfo());
		context.contribute(DebugSource.ALL, new GeneticsDebug());
		GeneticsCommands.register(context);
	}
}
