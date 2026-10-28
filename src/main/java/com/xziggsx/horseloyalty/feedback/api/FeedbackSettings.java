package com.xziggsx.horseloyalty.feedback.api;

import com.xziggsx.horseloyalty.core.config.ClientPrefs;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.SettingsSection;

/**
 * The feedback toggles, a player's own choice in {@code horseloyalty-client.json}. In the api
 * package because riding and loyalty ask them whether to show toasts, HUD messages and sounds.
 * Field names are the JSON keys.
 */
@SuppressWarnings("FieldMayBeFinal")
public final class FeedbackSettings implements SettingsSection {
	@Doc("Loyalty horseshoes and a gait bar above the hotbar while riding.")
	private boolean ridingHud = true;

	@Doc("Hoofbeats that match the gait.")
	private boolean gaitSounds = true;

	@Doc("Dust and splashes at a canter or gallop.")
	private boolean gaitParticles = true;

	@Doc("Breath puffs in snowy places.")
	private boolean coldBreath = true;

	@Doc("A toast when loyalty changes level.")
	private boolean loyaltyToasts = true;

	@Doc("A toast when the horse balks at a gait change (off: a message above the hotbar).")
	private boolean ridingToasts = true;

	/** This player's choices. */
	public static FeedbackSettings get() {
		return ClientPrefs.section(FeedbackSettings.class);
	}

	@Override
	public String key() {
		return "feedback";
	}

	/** Whether loyalty horseshoes and the gait bar show above the hotbar while riding. */
	public boolean ridingHud() {
		return ridingHud;
	}

	/** Whether ridden horses play hoofbeats that match their gait. */
	public boolean gaitSounds() {
		return gaitSounds;
	}

	/** Whether cantering and galloping horses kick up dust and splashes. */
	public boolean gaitParticles() {
		return gaitParticles;
	}

	/** Whether horses moving at a trot or faster show breath puffs in snowy places. */
	public boolean coldBreath() {
		return coldBreath;
	}

	/** Whether a toast shows when your loyalty with a horse changes level. */
	public boolean loyaltyToasts() {
		return loyaltyToasts;
	}

	/** Whether a toast shows when the horse balks at a gait change. Off: a message above the hotbar. */
	public boolean ridingToasts() {
		return ridingToasts;
	}
}
