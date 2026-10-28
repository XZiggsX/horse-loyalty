package com.xziggsx.horseloyalty;

import com.xziggsx.horseloyalty.core.command.CoreCommands;
import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.config.ClientPrefs;
import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.SettingsSync;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.HorseBasics;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.dev.DevToolsFeature;
import com.xziggsx.horseloyalty.feedback.api.FeedbackSettings;
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
		// The config next: each feature's section and the switches. The client prefs come after, since
		// an old config hands its feedback settings over to them.
		ConfigFile.registerSections();
		ClientPrefs.register(new FeedbackSettings());
		ConfigFile.load();
		ClientPrefs.load();
		// registerData() and wire() for all of them; the switches decide what actually runs.
		Features.wireAll();
		// The server's switches and synced settings, sent to every player who joins.
		SettingsSync.init();
		// The /hl command tree, with every feature's branches, and core's own debug section.
		CoreCommands.init();
		HorseCommands.init();
		DebugSource.ALL.add(null, new HorseBasics());
		LOGGER.info("Horse Loyalty loaded");
	}
}
