package com.xziggsx.horseloyalty;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.config.RidingRules;
import com.xziggsx.horseloyalty.dev.HorseGenesCommand;
import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.genetics.world.GeneticsData;
import com.xziggsx.horseloyalty.horn.HornBinding;
import com.xziggsx.horseloyalty.horn.HornCall;
import com.xziggsx.horseloyalty.loyalty.Family;
import com.xziggsx.horseloyalty.loyalty.Hunger;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.xziggsx.horseloyalty.loyalty.LoyaltyChangePayload;
import com.xziggsx.horseloyalty.personality.Personality;
import com.xziggsx.horseloyalty.riding.GaitSync;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint: runs on dedicated servers and on the client (whose built-in server
 * runs singleplayer worlds). Everything in the main source set is loaded on both sides;
 * the game logic in it (loyalty, hunger, feeding) only does work on the server side.
 * Client-only code (riding controls, screens, HUD) is in the client source set.
 */
public class HorseLoyaltyMod implements ModInitializer {
	public static final String MOD_ID = "horseloyalty";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Config first: the attachments and all game logic read their numbers from it.
		HorseLoyaltyConfig.load();
		// Registers the data saved on horses (Fabric Data Attachment API).
		Loyalty.init();
		Hunger.init();
		Family.init();
		Personality.init();
		// Genetics: the traits genes can show, and the genome and lineage saved on horses.
		HorseTraits.init();
		GeneticsData.init();
		// Goat horns bound to a horse: the item data, the sneak-click to bind and the call.
		HornBinding.init();
		HornCall.init();
		// Packets: the rider's gait (to the server), loyalty toasts and the server's riding rules (to the client).
		GaitSync.init();
		LoyaltyChangePayload.init();
		RidingRules.init();
		// Operator-only /horsegenes, for testing genetics.
		HorseGenesCommand.init();
		LOGGER.info("Horse Loyalty loaded");
	}
}
