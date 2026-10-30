package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;

/**
 * Every horse's favorite foods and the food it dislikes, rolled once from the
 * {@link FeedableFoods} and saved on the horse. Players find them out by feeding: a food is
 * revealed on the horse info screen once someone has fed it to the horse. What a feed does with
 * the opinion is in {@link FeedReactions} and the hunger feature's feed modifier.
 * <p>
 * Changing the feedable foods never re-rolls a horse. A preference whose food left the list is
 * ignored but kept, so it comes back if the food is added again.
 */
public final class Tastes {
	/** What a horse thinks of a food. */
	public enum Opinion {
		FAVORITE, NEUTRAL, DISLIKED
	}

	/**
	 * A horse's preferences, as config entries (an item id or a {@code #tag}).
	 *
	 * @param revealed the favorites and dislikes someone has fed it
	 */
	public record State(List<String> favorites, List<String> dislikes, List<String> revealed) {
		public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.listOf().optionalFieldOf("favorites", List.of()).forGetter(State::favorites),
				Codec.STRING.listOf().optionalFieldOf("dislikes", List.of()).forGetter(State::dislikes),
				Codec.STRING.listOf().optionalFieldOf("revealed", List.of()).forGetter(State::revealed)
		).apply(instance, State::new));

		/** A copy with this entry revealed too. */
		State reveal(String key) {
			if (revealed.contains(key)) {
				return this;
			}
			List<String> more = new ArrayList<>(revealed);
			more.add(key);
			return new State(favorites, dislikes, List.copyOf(more));
		}
	}

	/**
	 * What players may know of a horse's preferences, synced for the horse info screen: the revealed
	 * foods as item ids to show, and how many are still unknown. Kept apart from {@link State} so
	 * clients never learn what hasn't been revealed.
	 */
	public record Known(List<String> loves, int hiddenLoves, List<String> dislikes, int hiddenDislikes) {
		public static final Codec<Known> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.listOf().optionalFieldOf("loves", List.of()).forGetter(Known::loves),
				Codec.INT.optionalFieldOf("hidden_loves", 0).forGetter(Known::hiddenLoves),
				Codec.STRING.listOf().optionalFieldOf("dislikes", List.of()).forGetter(Known::dislikes),
				Codec.INT.optionalFieldOf("hidden_dislikes", 0).forGetter(Known::hiddenDislikes)
		).apply(instance, Known::new));

		public static final StreamCodec<ByteBuf, Known> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Known::loves,
				ByteBufCodecs.VAR_INT, Known::hiddenLoves,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Known::dislikes,
				ByteBufCodecs.VAR_INT, Known::hiddenDislikes,
				Known::new);
	}

	/** Server only, saved with the horse. Absent until the horse's preferences are first needed. */
	public static final AttachmentType<State> STATE = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "tastes"),
			builder -> builder.persistent(State.CODEC)
	);

	/** Saved and synced to every client, see {@link Known}. */
	public static final AttachmentType<Known> KNOWN = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "known_tastes"),
			builder -> builder.persistent(Known.CODEC).syncWith(Known.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	private Tastes() {
	}

	/** Forces the class to load so the attachments are registered during mod init. */
	public static void init() {
	}

	/** The horse's preferences, rolled now if it has none yet. Server side. */
	public static State get(AbstractHorse horse) {
		State state = horse.getAttached(STATE);
		return state != null ? state : reroll(horse);
	}

	/** Rolls new preferences from the feedable foods, nothing revealed. Server side. */
	public static State reroll(AbstractHorse horse) {
		HungerSettings config = HungerSettings.get();
		TasteRoll.Result result = TasteRoll.roll(FeedableFoods.forRoll(), config.minFavorites(), config.maxFavorites(),
				config.dislikes(), horse.getRandom()::nextInt);
		State state = new State(result.favorites(), result.dislikes(), List.of());
		set(horse, state);
		return state;
	}

	/** Saves new preferences and updates what players know of them. Server side. */
	public static void set(AbstractHorse horse, State state) {
		horse.setAttached(STATE, state);
		horse.setAttached(KNOWN, known(state));
	}

	/**
	 * Rolls preferences for a horse that has none yet, and brings what players know up to date
	 * with the feedable foods. Called once a second for tamed horses while preferences are on.
	 */
	static void refresh(AbstractHorse horse) {
		State state = horse.getAttached(STATE);
		if (state == null) {
			reroll(horse);
			return;
		}
		Known known = known(state);
		if (!known.equals(horse.getAttached(KNOWN))) {
			horse.setAttached(KNOWN, known);
		}
	}

	/** What the horse thinks of this food. Neutral while preferences are off. Server side. */
	public static Opinion opinion(AbstractHorse horse, ItemStack stack) {
		if (!HungerSettings.get().preferences()) {
			return Opinion.NEUTRAL;
		}
		State state = get(horse);
		if (matching(state.favorites(), stack) != null) {
			return Opinion.FAVORITE;
		}
		return matching(state.dislikes(), stack) != null ? Opinion.DISLIKED : Opinion.NEUTRAL;
	}

	/** The horse ate this: reveals it if it's a favorite or dislike, and returns the opinion. Server side. */
	public static Opinion ate(AbstractHorse horse, ItemStack stack) {
		Opinion opinion = opinion(horse, stack);
		if (opinion != Opinion.NEUTRAL) {
			State state = get(horse);
			String key = matching(opinion == Opinion.FAVORITE ? state.favorites() : state.dislikes(), stack);
			State revealed = state.reveal(key);
			if (revealed != state) {
				set(horse, revealed);
			}
		}
		return opinion;
	}

	/** Reveals every preference, for the dev command. Server side. */
	public static void revealAll(AbstractHorse horse) {
		State state = get(horse);
		List<String> all = new ArrayList<>(state.favorites());
		all.addAll(state.dislikes());
		set(horse, new State(state.favorites(), state.dislikes(), List.copyOf(all)));
	}

	/** Makes this config entry a favorite (or a dislike), revealed, for the dev command. Server side. */
	public static void prefer(AbstractHorse horse, String key, boolean favorite) {
		State state = get(horse);
		List<String> favorites = new ArrayList<>(state.favorites());
		List<String> dislikes = new ArrayList<>(state.dislikes());
		favorites.remove(key);
		dislikes.remove(key);
		(favorite ? favorites : dislikes).add(key);
		set(horse, new State(List.copyOf(favorites), List.copyOf(dislikes), state.revealed()).reveal(key));
	}

	/** The first of these entries that is in the list now and matches the stack, or null. */
	private static String matching(List<String> keys, ItemStack stack) {
		for (String key : keys) {
			if (FeedableFoods.matches(key, stack)) {
				return key;
			}
		}
		return null;
	}

	/** What players may know of these preferences, leaving out entries no longer in the list. */
	static Known known(State state) {
		List<String> loves = new ArrayList<>();
		int hiddenLoves = 0;
		for (String key : state.favorites()) {
			if (!FeedableFoods.has(key)) {
				continue;
			}
			if (state.revealed().contains(key)) {
				loves.add(FeedableFoods.displayItem(key));
			} else {
				hiddenLoves++;
			}
		}
		List<String> dislikes = new ArrayList<>();
		int hiddenDislikes = 0;
		for (String key : state.dislikes()) {
			if (!FeedableFoods.has(key)) {
				continue;
			}
			if (state.revealed().contains(key)) {
				dislikes.add(FeedableFoods.displayItem(key));
			} else {
				hiddenDislikes++;
			}
		}
		return new Known(List.copyOf(loves), hiddenLoves, List.copyOf(dislikes), hiddenDislikes);
	}
}
