package com.xziggsx.horseloyalty.hunger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xziggsx.horseloyalty.hunger.TasteRoll.Food;
import com.xziggsx.horseloyalty.hunger.TasteRoll.Result;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TasteRollTest {
	private static final List<Food> FOODS = List.of(
			new Food("minecraft:wheat", true, true),
			new Food("minecraft:apple", true, true),
			new Food("minecraft:sugar", true, true),
			new Food("minecraft:carrot", true, true),
			new Food("#c:foods/berries", true, true),
			new Food("minecraft:golden_carrot", true, false),
			new Food("minecraft:enchanted_golden_apple", false, false));

	private static Result roll(List<Food> foods, int min, int max, int dislikes, long seed) {
		Random random = new Random(seed);
		return TasteRoll.roll(foods, min, max, dislikes, random::nextInt);
	}

	@Test
	void countsStayInRangeAndFoodsDiffer() {
		Set<Integer> favoriteCounts = new HashSet<>();
		for (long seed = 0; seed < 500; seed++) {
			Result result = roll(FOODS, 1, 3, 1, seed);
			int favorites = result.favorites().size();
			favoriteCounts.add(favorites);
			assertTrue(favorites >= 1 && favorites <= 3, "favorites " + favorites);
			assertEquals(1, result.dislikes().size());
			Set<String> all = new HashSet<>(result.favorites());
			all.addAll(result.dislikes());
			assertEquals(favorites + 1, all.size(), "no food is picked twice: " + result);
		}
		assertEquals(Set.of(1, 2, 3), favoriteCounts, "every count from 1 to 3 comes up");
	}

	@Test
	void respectsWhatEachFoodCanBe() {
		for (long seed = 0; seed < 500; seed++) {
			Result result = roll(FOODS, 1, 3, 1, seed);
			assertFalse(result.dislikes().contains("minecraft:golden_carrot"), "a breeding food is never disliked");
			assertFalse(result.dislikes().contains("minecraft:enchanted_golden_apple"));
			assertFalse(result.favorites().contains("minecraft:enchanted_golden_apple"), "a neutral-only food is never a favorite");
		}
	}

	@Test
	void smallListsLoseFavoritesFirst() {
		List<Food> two = List.of(new Food("minecraft:apple", true, true), new Food("minecraft:sugar", true, true));
		for (long seed = 0; seed < 50; seed++) {
			Result result = roll(two, 3, 3, 1, seed);
			assertEquals(1, result.favorites().size(), "one favorite left");
			assertEquals(1, result.dislikes().size(), "the dislike is kept");
		}
		List<Food> one = List.of(new Food("minecraft:apple", true, true));
		Result single = roll(one, 1, 3, 1, 7);
		assertEquals(List.of("minecraft:apple"), single.favorites(), "one food: a favorite, no dislike");
		assertTrue(single.dislikes().isEmpty());
	}

	@Test
	void zeroFavoritesStillDislikes() {
		Result result = roll(List.of(new Food("minecraft:apple", true, true)), 0, 0, 1, 3);
		assertTrue(result.favorites().isEmpty());
		assertEquals(List.of("minecraft:apple"), result.dislikes());
	}

	@Test
	void repeatedEntriesCountOnce() {
		List<Food> repeated = List.of(new Food("minecraft:apple", true, true), new Food("minecraft:apple", true, true));
		Result result = roll(repeated, 2, 2, 0, 1);
		assertEquals(List.of("minecraft:apple"), result.favorites());
	}

	@Test
	void emptyListRollsNothing() {
		Result result = roll(List.of(), 1, 3, 1, 1);
		assertTrue(result.favorites().isEmpty());
		assertTrue(result.dislikes().isEmpty());
	}
}
