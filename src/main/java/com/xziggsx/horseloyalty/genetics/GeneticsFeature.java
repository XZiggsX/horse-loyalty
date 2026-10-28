package com.xziggsx.horseloyalty.genetics;

import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.genetics.dev.HorseGenesCommand;
import com.xziggsx.horseloyalty.genetics.world.GeneticsData;
import com.xziggsx.horseloyalty.genetics.world.HorseGenetics;

/** Genetics: coat, marking and temperament genes, herd founders, inheritance and lineage. */
public final class GeneticsFeature implements Feature {
	public static final String ID = "genetics";

	@Override
	public String id() {
		return ID;
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
		HorseGenesCommand.init(context);
	}
}
