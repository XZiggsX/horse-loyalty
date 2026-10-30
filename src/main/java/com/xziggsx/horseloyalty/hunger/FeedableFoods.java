package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.hunger.HungerSettings.FoodEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The {@code feedableFoods} of the hunger config, read into items and tags. A tamed horse can be
 * fed any of them, and its preferences are rolled from them (see {@link Tastes}). Items that are
 * already used on a horse, such as saddles or leads, are never feedable, or the horse would eat
 * them instead.
 * <p>
 * Read again whenever the config changes. Items are looked up when first needed rather than at
 * load, so foods from mods that register after this one are found too.
 */
public final class FeedableFoods {
	/** Items with a use of their own on a horse, besides anything it can wear (see {@link #usedOnHorses}). */
	private static final Set<Item> USED_ON_HORSES = Set.of(Items.SADDLE, Items.LEAD, Items.NAME_TAG, Items.CHEST);

	/** One entry: its config key and what it matches, an item or a tag. */
	private record Food(TasteRoll.Food roll, Item item, TagKey<Item> tag) {
		boolean matches(ItemStack stack) {
			return item != null ? stack.is(item) : stack.is(tag);
		}
	}

	private static List<FoodEntry> readFrom;
	private static List<Food> foods = List.of();

	private FeedableFoods() {
	}

	/** Whether a tamed horse can be fed this. */
	public static boolean isFeedable(ItemStack stack) {
		if (stack.isEmpty() || usedOnHorses(stack)) {
			return false;
		}
		for (Food food : current()) {
			if (food.matches(stack)) {
				return true;
			}
		}
		return false;
	}

	/** Whether this config entry is in the list now. */
	public static boolean has(String key) {
		return current().stream().anyMatch(food -> food.roll().key().equals(key));
	}

	/** Whether this stack is the food of this config entry. False if the entry isn't in the list now. */
	public static boolean matches(String key, ItemStack stack) {
		for (Food food : current()) {
			if (food.roll().key().equals(key)) {
				return !usedOnHorses(stack) && food.matches(stack);
			}
		}
		return false;
	}

	/** Every entry in the list now, as written in the config. */
	public static List<String> keys() {
		return current().stream().map(food -> food.roll().key()).toList();
	}

	/** The foods as the preference roll sees them. */
	static List<TasteRoll.Food> forRoll() {
		return current().stream().map(Food::roll).toList();
	}

	/**
	 * The item id to show for a config entry: the item itself, or the first item of a tag. Falls back
	 * to the entry as written when the tag is empty or unknown.
	 */
	public static String displayItem(String key) {
		if (!key.startsWith("#")) {
			return key;
		}
		Identifier id = Identifier.tryParse(key.substring(1));
		if (id == null) {
			return key;
		}
		return BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id))
				.flatMap(set -> set.stream().findFirst())
				.map(Holder::value)
				.map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
				.orElse(key);
	}

	/** Anything a horse can wear, plus saddles, leads, name tags, chests and buckets. */
	static boolean usedOnHorses(ItemStack stack) {
		return stack.has(DataComponents.EQUIPPABLE) || USED_ON_HORSES.contains(stack.getItem())
				|| BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("bucket");
	}

	/** The foods of the config in use, read again if it changed. */
	private static synchronized List<Food> current() {
		List<FoodEntry> entries = HungerSettings.get().feedableFoods();
		if (entries != readFrom) {
			foods = read(entries);
			readFrom = entries;
		}
		return foods;
	}

	private static List<Food> read(List<FoodEntry> entries) {
		List<Food> read = new ArrayList<>();
		for (FoodEntry entry : entries) {
			String key = entry.item() == null ? "" : entry.item().trim();
			if (key.isEmpty()) {
				HorseLoyaltyMod.LOGGER.warn("A hunger.feedableFoods entry has no item; skipped");
				continue;
			}
			if (read.stream().anyMatch(food -> food.roll().key().equals(key))) {
				continue;
			}
			TasteRoll.Food roll = new TasteRoll.Food(key, entry.canBeFavorite(), entry.canBeDisliked());
			boolean isTag = key.startsWith("#");
			Identifier id = Identifier.tryParse(isTag ? key.substring(1) : key);
			if (id == null) {
				HorseLoyaltyMod.LOGGER.warn("hunger.feedableFoods: {} isn't a valid id; skipped", key);
				continue;
			}
			if (isTag) {
				read.add(new Food(roll, null, TagKey.create(Registries.ITEM, id)));
				continue;
			}
			Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
			if (item.isEmpty()) {
				HorseLoyaltyMod.LOGGER.warn("hunger.feedableFoods: no item {}; skipped", key);
			} else if (usedOnHorses(new ItemStack(item.get()))) {
				HorseLoyaltyMod.LOGGER.warn("hunger.feedableFoods: {} is used on horses, so it can't be fed; skipped", key);
			} else {
				read.add(new Food(roll, item.get(), null));
			}
		}
		return List.copyOf(read);
	}
}
