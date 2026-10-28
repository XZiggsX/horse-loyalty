package com.xziggsx.horseloyalty.genetics;

import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.dev.HorseGenesCommand;
import com.xziggsx.horseloyalty.genetics.world.GeneticsData;

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
		HorseGenesCommand.init(context);
	}
}
