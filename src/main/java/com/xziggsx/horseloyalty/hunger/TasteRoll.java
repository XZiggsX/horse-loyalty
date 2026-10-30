package com.xziggsx.horseloyalty.hunger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Picks a horse's favorite and disliked foods from the feedable foods. Plain Java, so it can be
 * tested without the game.
 */
final class TasteRoll {
	/**
	 * One feedable food as the roll sees it.
	 *
	 * @param key the config entry: an item id, or a tag starting with {@code #}
	 */
	record Food(String key, boolean canBeFavorite, boolean canBeDisliked) {
	}

	/** The foods picked, as config entries. All different. */
	record Result(List<String> favorites, List<String> dislikes) {
	}

	private TasteRoll() {
	}

	/**
	 * Rolls {@code minFavorites} to {@code maxFavorites} favorites and {@code dislikes} dislikes.
	 * When there aren't enough foods, a horse gets fewer favorites first: a dislike is only
	 * skipped when taking it would leave no favorite at all.
	 *
	 * @param nextInt a random number from 0 up to (not including) its argument
	 */
	static Result roll(List<Food> foods, int minFavorites, int maxFavorites, int dislikes, IntUnaryOperator nextInt) {
		int wantedFavorites = minFavorites + nextInt.applyAsInt(Math.max(1, maxFavorites - minFavorites + 1));
		List<Food> shuffled = shuffle(dedupe(foods), nextInt);

		List<String> disliked = new ArrayList<>();
		for (Food food : shuffled) {
			if (disliked.size() >= dislikes) {
				break;
			}
			if (!food.canBeDisliked()) {
				continue;
			}
			long favoritesLeft = shuffled.stream()
					.filter(other -> other.canBeFavorite() && !other.key().equals(food.key()) && !disliked.contains(other.key()))
					.count();
			if (wantedFavorites > 0 && favoritesLeft == 0) {
				continue;
			}
			disliked.add(food.key());
		}

		List<String> favorites = new ArrayList<>();
		for (Food food : shuffled) {
			if (favorites.size() >= wantedFavorites) {
				break;
			}
			if (food.canBeFavorite() && !disliked.contains(food.key())) {
				favorites.add(food.key());
			}
		}
		return new Result(List.copyOf(favorites), List.copyOf(disliked));
	}

	/** The foods without repeated keys, first one wins. */
	private static List<Food> dedupe(List<Food> foods) {
		List<Food> unique = new ArrayList<>();
		for (Food food : foods) {
			if (unique.stream().noneMatch(other -> other.key().equals(food.key()))) {
				unique.add(food);
			}
		}
		return unique;
	}

	/** Fisher-Yates, with the given random. */
	private static List<Food> shuffle(List<Food> foods, IntUnaryOperator nextInt) {
		List<Food> shuffled = new ArrayList<>(foods);
		for (int i = shuffled.size() - 1; i > 0; i--) {
			int j = nextInt.applyAsInt(i + 1);
			Food swap = shuffled.get(i);
			shuffled.set(i, shuffled.get(j));
			shuffled.set(j, swap);
		}
		return shuffled;
	}
}
