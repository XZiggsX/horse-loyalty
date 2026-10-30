package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.Range;
import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.config.Synced;
import java.util.ArrayList;
import java.util.List;

/**
 * The hunger section of the config: hunger itself, the feedable foods and the food preferences.
 * Field names are the JSON keys. Synced, because the client also decides whether a horse takes
 * an item and shows the preferences on the horse info screen.
 */
@Synced
@SuppressWarnings("FieldMayBeFinal")
public final class HungerSettings implements SettingsSection {
	@Range(min = 1, max = 10_000_000)
	@Doc("A tamed horse that hasn't been fed or left to graze (unridden, on a grazing block) for this long gets hungry and shows dark particles.")
	private double secondsUntilHungry = 1200;

	@Range(min = 1, max = 10_000_000)
	@Doc("While hungry, it loses loyaltyPointsLost points with every rider this often. It never drops below level 1 or becomes untamed.")
	private double loyaltyLossEverySeconds = 120;

	@Range(min = 0, max = 1000)
	@Doc("Points every rider loses at each hunger loss.")
	private int loyaltyPointsLost = 1;

	@Doc("Every horse has favorite foods and a food it dislikes, from feedableFoods. Off: every food is the same to a horse.")
	private boolean preferences = true;

	@Range(min = 0, max = 10)
	@Doc("Fewest favorite foods a horse rolls.")
	private int minFavorites = 1;

	@Range(min = 0, max = 10)
	@Doc("Most favorite foods a horse rolls.")
	private int maxFavorites = 3;

	@Range(min = 0, max = 10)
	@Doc("Disliked foods a horse rolls.")
	private int dislikes = 1;

	@Range(min = 0, max = 100)
	@Doc("A counted feed of a favorite food is worth the loyalty pointsPerFeed times this.")
	private double favoriteLoyaltyMultiplier = 2.0;

	@Range(min = 0, max = 1000)
	@Doc("Points the feeder loses for a counted feed of a disliked food. Never below level 1.")
	private int dislikeLoyaltyLoss = 1;

	@Doc("What a tamed horse can be fed, besides its vanilla food. Each entry is an item id or a #tag. canBeFavorite and canBeDisliked keep a food out of either side of the preferences. Items used on horses (saddles, armor, carpets, chests, leads, name tags, buckets) are skipped.")
	private List<FoodEntry> feedableFoods = defaultFoods();

	/** One entry of {@code feedableFoods}. */
	public static final class FoodEntry {
		private String item;
		private Boolean canBeFavorite = true;
		private Boolean canBeDisliked = true;

		@SuppressWarnings("unused")
		private FoodEntry() {
		}

		FoodEntry(String item, boolean canBeFavorite, boolean canBeDisliked) {
			this.item = item;
			this.canBeFavorite = canBeFavorite;
			this.canBeDisliked = canBeDisliked;
		}

		/** The item id, or a tag starting with {@code #}. Null if left out. */
		public String item() {
			return item;
		}

		public boolean canBeFavorite() {
			return canBeFavorite == null || canBeFavorite;
		}

		public boolean canBeDisliked() {
			return canBeDisliked == null || canBeDisliked;
		}
	}

	private static List<FoodEntry> defaultFoods() {
		List<FoodEntry> foods = new ArrayList<>();
		for (String item : List.of("minecraft:wheat", "minecraft:hay_block", "minecraft:apple", "minecraft:sugar", "minecraft:carrot",
				"minecraft:beetroot", "minecraft:sweet_berries", "minecraft:melon_slice")) {
			foods.add(new FoodEntry(item, true, true));
		}
		// Breeding foods: never disliked, so breeding never costs loyalty.
		foods.add(new FoodEntry("minecraft:golden_carrot", true, false));
		foods.add(new FoodEntry("minecraft:golden_apple", true, false));
		foods.add(new FoodEntry("minecraft:enchanted_golden_apple", false, false));
		return foods;
	}

	/** The section in use. */
	public static HungerSettings get() {
		return ConfigFile.section(HungerSettings.class);
	}

	@Override
	public String key() {
		return "hunger";
	}

	@Override
	public void resolve() {
		maxFavorites = Math.max(minFavorites, maxFavorites);
	}

	/** Ticks without eating or grazing before a horse is hungry. */
	public long ticksUntilHungry() {
		return Math.round(secondsUntilHungry * 20);
	}

	/** Ticks between loyalty losses while a horse is hungry. */
	public long loyaltyLossIntervalTicks() {
		return Math.round(loyaltyLossEverySeconds * 20);
	}

	/** Points every rider loses at each hunger loss. */
	public int loyaltyPointsLost() {
		return loyaltyPointsLost;
	}

	/** Whether horses have favorite and disliked foods. */
	public boolean preferences() {
		return preferences;
	}

	/** Fewest favorites a horse rolls. */
	public int minFavorites() {
		return minFavorites;
	}

	/** Most favorites a horse rolls, never below {@link #minFavorites()}. */
	public int maxFavorites() {
		return maxFavorites;
	}

	/** Disliked foods a horse rolls. */
	public int dislikes() {
		return dislikes;
	}

	/** Multiplier for the loyalty points of a counted favorite feed. */
	public double favoriteLoyaltyMultiplier() {
		return favoriteLoyaltyMultiplier;
	}

	/** Points the feeder loses for a counted disliked feed. */
	public int dislikeLoyaltyLoss() {
		return dislikeLoyaltyLoss;
	}

	/** The feedable foods as written in the config. */
	public List<FoodEntry> feedableFoods() {
		return feedableFoods;
	}
}
