package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.core.Treats;
import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.horseinfo.api.InfoProvider;
import com.xziggsx.horseloyalty.loyalty.api.FeedModifiers;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;

/**
 * Hunger: tamed horses get hungry, graze, and lose loyalty when neglected. Also what they can be
 * fed ({@link FeedableFoods}) and what they think of it ({@link Tastes}). Needs loyalty.
 */
public final class HungerFeature implements Feature {
	public static final String ID = "hunger";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Set<String> requires() {
		return Set.of("loyalty");
	}

	@Override
	public SettingsSection settings() {
		return new HungerSettings();
	}

	@Override
	public void registerData() {
		Hunger.init();
		Tastes.init();
	}

	@Override
	public void wire(FeatureContext context) {
		context.on(HorseEvents.SERVER_SECOND, Hunger::tick);
		context.contribute(Treats.ALL, FeedableFoods::isFeedable);
		// A hungry horse takes any treat, and every meal resets its hunger. A full one only takes a treat
		// that counts for loyalty (see LoyaltyFeature), and refuses the rest.
		context.on(HorseEvents.WANTS_TREAT, (horse, player, stack) -> Hunger.isHungry(horse));
		context.on(HorseEvents.ATE, (horse, player, stack) -> {
			Hunger.feed(horse);
			Tastes.Opinion opinion = Tastes.ate(horse, stack);
			if (horse.level() instanceof ServerLevel level) {
				FeedReactions.play(horse, level, opinion);
			}
		});
		// A favorite is worth more loyalty, a disliked food costs the feeder some.
		context.contribute(FeedModifiers.ALL, (horse, player, stack, points) -> switch (Tastes.opinion(horse, stack)) {
			case FAVORITE -> (int) Math.round(points * HungerSettings.get().favoriteLoyaltyMultiplier());
			case DISLIKED -> -HungerSettings.get().dislikeLoyaltyLoss();
			case NEUTRAL -> points;
		});
		HungerInfo info = new HungerInfo();
		context.contribute(InfoProvider.ALL, info);
		HungerCommands.register(context);
	}
}
