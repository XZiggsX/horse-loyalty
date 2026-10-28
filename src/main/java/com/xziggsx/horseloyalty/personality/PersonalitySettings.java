package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.Range;
import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.config.Synced;
import com.xziggsx.horseloyalty.personality.api.Personality;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The personality section of the config. Synced, because riding runs on the rider's client and
 * the lazy drops, skittish bolts and bucking happen there. Field names are the JSON keys.
 */
@Synced
@SuppressWarnings("FieldMayBeFinal")
public final class PersonalitySettings implements SettingsSection {
	@Range(min = 1, max = 5)
	@Doc("Loyalty level at which a player sees a horse's personality.")
	private int revealAtLevel = 2;

	@Doc("Relative odds of each personality. Steady has no quirk; brave only panics when burning or freezing; skittish flees and bolts; curious wanders to villages; lazy drops gaits more.")
	private Map<String, Double> weights = defaultWeights();

	@Range(min = 0, max = 50)
	@Doc("A foal gets each parent's personality with this chance (the rest is a fresh roll). Not used while genetics is on.")
	private double inheritPercent = 40;

	@Range(min = 0, max = 100)
	@Doc("Chance a foal gets its parents' personality when both share one. Not used while genetics is on.")
	private double sameParentsPercent = 70;

	@Range(min = 1, max = 32)
	@Doc("Blocks within which a loose skittish horse runs from a creeper (anything in the spooks_horses tag).")
	private double skittishFleeDistance = 8;

	@Range(min = 1, max = 32)
	@Doc("Blocks within which a creeper makes a ridden skittish horse bolt.")
	private double boltCreeperDistance = 6;

	@Range(min = 0, max = 30)
	@Doc("Seconds a ridden skittish horse bolts at a gallop, half as long at loyalty 4 and never at 5.")
	private double boltSeconds = 2.5;

	@Range(min = 8, max = 128)
	@Doc("Blocks a curious horse looks for a village bell in.")
	private int curiousSearchRadius = 48;

	@Range(min = 10, max = 86400)
	@Doc("A loose curious horse wanders toward a village about this often.")
	private double curiousWanderEverySeconds = 300;

	@Range(min = 0, max = 10)
	@Doc("A lazy horse's gait drop chance is multiplied by this.")
	private double lazyGaitDropChanceMultiplier = 1.5;

	@Range(min = 0.1, max = 10)
	@Doc("A lazy horse's time between gait drop rolls is multiplied by this.")
	private double lazyGaitDropTimeMultiplier = 0.7;

	@Doc("Personalities that may throw their rider off when they refuse to go faster, instead of only prancing.")
	private List<String> bucksOff = new ArrayList<>(List.of("skittish"));

	@Range(min = 0, max = 100)
	@Doc("Chance that a balking horse with a bucksOff personality throws its rider.")
	private double buckOffPercent = 35;

	private transient List<Personality> resolvedBucksOff;

	/** The section in use. */
	public static PersonalitySettings get() {
		return ConfigFile.section(PersonalitySettings.class);
	}

	@Override
	public String key() {
		return "personality";
	}

	private static Map<String, Double> defaultWeights() {
		Map<String, Double> weights = new LinkedHashMap<>();
		weights.put("steady", 40.0);
		weights.put("brave", 15.0);
		weights.put("skittish", 15.0);
		weights.put("curious", 15.0);
		weights.put("lazy", 15.0);
		return weights;
	}

	@Override
	public void resolve() {
		// Missing personalities keep their default weight; negative weights count as 0.
		Map<String, Double> resolved = defaultWeights();
		weights.forEach((id, weight) -> {
			if (resolved.containsKey(id) && weight != null) {
				resolved.put(id, Math.max(0, weight));
			}
		});
		weights = resolved;
		resolvedBucksOff = new ArrayList<>();
		for (String id : bucksOff) {
			// byId falls back to steady for typos; only take ids that really match.
			Personality match = Personality.byId(id);
			if (match.id().equals(id)) {
				resolvedBucksOff.add(match);
			}
		}
	}

	/** Loyalty level at which a player sees a horse's personality. */
	public int revealLevel() {
		return revealAtLevel;
	}

	/** Relative odds of rolling this personality. */
	public double weight(Personality personality) {
		return weights.getOrDefault(personality.id(), 0.0);
	}

	/** Chance (0.4 = 40%) a foal gets one given parent's personality. */
	public double inheritChance() {
		return inheritPercent / 100.0;
	}

	/** Chance a foal gets its parents' personality when both share it. */
	public double sameParentsChance() {
		return sameParentsPercent / 100.0;
	}

	/** Blocks within which an unridden skittish horse runs from a creeper. */
	public double skittishFleeDistance() {
		return skittishFleeDistance;
	}

	/** Blocks within which a creeper makes a ridden skittish horse bolt. */
	public double boltCreeperDistance() {
		return boltCreeperDistance;
	}

	/** Ticks a skittish horse bolts for, at loyalty below 4. */
	public int boltTicks() {
		return (int) Math.round(boltSeconds * 20);
	}

	/** Blocks a curious horse looks for a village bell in. */
	public int curiousSearchRadius() {
		return curiousSearchRadius;
	}

	/** Average ticks between a loose curious horse's trips toward a village. */
	public int curiousWanderEveryTicks() {
		return (int) Math.round(curiousWanderEverySeconds * 20);
	}

	/** A lazy horse's gait drop chance is multiplied by this. */
	public double lazyGaitDropChanceMultiplier() {
		return lazyGaitDropChanceMultiplier;
	}

	/** A lazy horse's time between gait drop rolls is multiplied by this. */
	public double lazyGaitDropTimeMultiplier() {
		return lazyGaitDropTimeMultiplier;
	}

	/** Whether a horse with this personality throws its rider off when it balks, instead of only prancing. */
	public boolean bucksOff(Personality personality) {
		return resolvedBucksOff.contains(personality);
	}

	/** Chance (0.35 = 35%) that a balking horse with a buck-off personality throws its rider. */
	public double buckOffChance() {
		return buckOffPercent / 100.0;
	}
}
