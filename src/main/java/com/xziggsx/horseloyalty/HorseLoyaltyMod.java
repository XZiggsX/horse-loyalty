package com.xziggsx.horseloyalty;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.config.RidingRules;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.dev.DevToolsFeature;
import com.xziggsx.horseloyalty.foals.FoalsFeature;
import com.xziggsx.horseloyalty.genetics.GeneticsFeature;
import com.xziggsx.horseloyalty.horseinfo.HorseInfoFeature;
import com.xziggsx.horseloyalty.hunger.HungerFeature;
import com.xziggsx.horseloyalty.loyalty.LoyaltyFeature;
import com.xziggsx.horseloyalty.personality.PersonalityFeature;
import com.xziggsx.horseloyalty.riding.RidingFeature;
import com.xziggsx.horseloyalty.summoning.SummoningFeature;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint: runs on dedicated servers and on the client (whose built-in server
 * runs singleplayer worlds). Registers every feature, then lets the switches decide what runs.
 * Everything in the main source set is loaded on both sides; client-only code (riding controls,
 * screens, HUD) is in the client source set.
 */
public class HorseLoyaltyMod implements ModInitializer {
	public static final String MOD_ID = "horseloyalty";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Config first: the attachments and all game logic read their numbers from it.
		HorseLoyaltyConfig.load();
		// Personality before genetics: genetics builds genes around a horse's personality.
		Features.register(new RidingFeature());
		Features.register(new LoyaltyFeature());
		Features.register(new HungerFeature());
		Features.register(new PersonalityFeature());
		Features.register(new GeneticsFeature());
		Features.register(new FoalsFeature());
		Features.register(new SummoningFeature());
		Features.register(new HorseInfoFeature());
		Features.register(new DevToolsFeature());
		// registerData() and wire() for all of them; the switches decide what actually runs.
		Features.wireAll();
		// The server's riding rules, sent to every player who joins.
		RidingRules.init();
		LOGGER.info("Horse Loyalty loaded");
	}
}
