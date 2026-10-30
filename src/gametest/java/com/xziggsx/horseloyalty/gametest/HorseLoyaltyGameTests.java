package com.xziggsx.horseloyalty.gametest;

import com.xziggsx.horseloyalty.core.feature.FeatureSwitch;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.foals.Family;
import com.xziggsx.horseloyalty.genetics.world.GeneticsData;
import com.xziggsx.horseloyalty.hunger.FeedableFoods;
import com.xziggsx.horseloyalty.hunger.Hunger;
import com.xziggsx.horseloyalty.hunger.HungerSettings;
import com.xziggsx.horseloyalty.hunger.Tastes;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.xziggsx.horseloyalty.loyalty.LoyaltySettings;
import com.xziggsx.horseloyalty.loyalty.api.Bond;
import com.xziggsx.horseloyalty.personality.Personalities;
import com.xziggsx.horseloyalty.personality.api.Personality;
import com.xziggsx.horseloyalty.personality.api.PersonalityView;
import com.xziggsx.horseloyalty.riding.api.RideModifiers;
import com.xziggsx.horseloyalty.summoning.HornCall;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Server game tests for the features and their switches. Riding, the HUD and the screens run on
 * the client and are checked by hand (see TESTING-feature-modules.md). A test that changes a
 * switch puts it back before it returns, in the same tick, so tests running alongside never see it.
 */
public class HorseLoyaltyGameTests {
	private static final BlockPos SPOT = new BlockPos(2, 1, 2);

	/** A horse tamed to the player, at full health. */
	private static Horse tamedHorse(GameTestHelper helper, ServerPlayer player) {
		Horse horse = helper.spawnWithNoFreeWill(EntityTypes.HORSE, SPOT);
		// The mock player logs in at the world origin; put them next to the horse.
		player.snapTo(horse.getX() + 1, horse.getY(), horse.getZ());
		horse.tameWithName(player);
		horse.setHealth(horse.getMaxHealth());
		return horse;
	}

	/** Runs {@code check} with these switches set, then puts every switch back. */
	private static void withSwitches(Map<String, FeatureSwitch> switches, Runnable check) {
		Map<String, FeatureSwitch> before = Features.localSwitches();
		Map<String, FeatureSwitch> changed = new HashMap<>(before);
		changed.putAll(switches);
		Features.useLocal(changed);
		try {
			check.run();
		} finally {
			Features.useLocal(before);
		}
	}

	/** A mock player whose items get used up: the mock player is always in creative, which never uses them up. */
	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getAbilities().instabuild = false;
		return player;
	}

	/** Gives the horse these preferences, nothing revealed, so a test doesn't depend on the roll. */
	private static void tastes(AbstractHorse horse, List<String> favorites, List<String> dislikes) {
		Tastes.set(horse, new Tastes.State(favorites, dislikes, List.of()));
	}

	/** A tamed horse at full health takes a treat when the feeder's cooldown has passed, and the feed counts. */
	@GameTest
	public void loyaltyFromFeeding(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		Horse horse = tamedHorse(helper, player);
		tastes(horse, List.of(), List.of());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
		horse.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(Loyalty.get(horse, player).feeds(), 1, "feeds after one treat");
		helper.assertValueEqual(Loyalty.get(horse, player).points(), LoyaltySettings.get().pointsPerFeed(), "points after one treat");
		// Full, and within the feeder's cooldown: the horse refuses the second treat and the wheat stays.
		horse.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "the second wheat was refused");
		helper.assertValueEqual(Loyalty.get(horse, player).feeds(), 1, "feeds after a second treat within the cooldown");
		helper.succeed();
	}

	/** A hungry horse eats within the feeder's cooldown, and the meal resets its hunger but doesn't count. */
	@GameTest
	public void hungryHorseEatsWithinTheCooldown(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		Horse horse = tamedHorse(helper, player);
		tastes(horse, List.of(), List.of());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
		horse.mobInteract(player, InteractionHand.MAIN_HAND);
		Hunger.starve(horse);
		helper.assertTrue(Hunger.isHungry(horse), "hungry before the second treat");
		horse.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "the hungry horse ate the second wheat");
		helper.assertFalse(Hunger.isHungry(horse), "fed again");
		helper.assertValueEqual(Loyalty.get(horse, player).feeds(), 1, "the second treat doesn't count within the cooldown");
		helper.succeed();
	}

	/** Points reach levels at the configured thresholds, and the synced level follows. */
	@GameTest
	public void loyaltyLevels(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, player);
		helper.assertValueEqual(Loyalty.level(horse, player), 1, "level when just tamed");
		Loyalty.setPoints(horse, player, LoyaltySettings.get().pointsNeeded(3));
		helper.assertValueEqual(Loyalty.level(horse, player), 3, "level at the level 3 threshold");
		Loyalty.setPoints(horse, player, LoyaltySettings.get().pointsNeeded(3) - 1);
		helper.assertValueEqual(Loyalty.level(horse, player), 2, "level one point below it");
		helper.assertValueEqual(Bond.get().level(horse, player), 2, "the bond link reads the same level");
		helper.succeed();
	}

	/** A hungry horse loses a point with every rider each interval, and never drops below level 1. */
	@GameTest
	public void hungerLosesLoyalty(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, player);
		Loyalty.setPoints(horse, player, 10);
		ServerLevel level = helper.getLevel();
		HungerSettings settings = HungerSettings.get();
		long now = level.getGameTime();
		// Ridden, so it can't graze on the test floor's grass.
		player.startRiding(horse, true, false);
		// Hungry for a while, last loss one interval ago. (A new test world's game time is small, so
		// "fed long ago" would be negative; an explicit last loss keeps the maths the same as in a real world.)
		horse.setAttached(Hunger.STATE, new Hunger.State(now - settings.ticksUntilHungry() - settings.loyaltyLossIntervalTicks(),
				now - settings.loyaltyLossIntervalTicks()));
		helper.assertTrue(Hunger.isHungry(horse), "starved horse is hungry");
		Hunger.tick(horse, level);
		helper.assertValueEqual(Loyalty.get(horse, player).points(), 10 - settings.loyaltyPointsLost(), "points after one hunger loss");
		// Eating resets it.
		Hunger.feed(horse);
		helper.assertFalse(Hunger.isHungry(horse), "fed horse is hungry");
		helper.succeed();
	}

	/** Every horse in scope gets a personality within a second or so of being loaded. */
	@GameTest(maxTicks = 100)
	public void personalityIsAssigned(GameTestHelper helper) {
		Horse horse = helper.spawnWithNoFreeWill(EntityTypes.HORSE, SPOT);
		helper.succeedWhen(() -> helper.assertTrue(horse.hasAttached(Personalities.DATA), "horse has a personality"));
	}

	/** A wild-spawned horse gets genes from its herd's founders, and a lineage. */
	@GameTest
	public void geneticsOnSpawn(GameTestHelper helper) {
		Horse horse = EntityTypes.HORSE.spawn(helper.getLevel(), helper.absolutePos(SPOT), EntitySpawnReason.SPAWN_ITEM_USE);
		helper.assertTrue(horse != null, "horse spawned");
		helper.assertTrue(horse.hasAttached(GeneticsData.GENOME), "spawned horse has genes");
		helper.assertTrue(horse.hasAttached(GeneticsData.LINEAGE), "spawned horse has a lineage");
		helper.succeed();
	}

	/**
	 * Breeding runs every phase: the foal gets crossed genes and a lineage (genes), a personality
	 * (traits), and a family with the breeder, who it's born tamed to (family).
	 */
	@GameTest
	public void breedingSetsUpTheFoal(GameTestHelper helper) {
		ServerPlayer breeder = helper.makeMockServerPlayerInLevel();
		AbstractHorse foal = breed(helper, breeder);
		helper.assertTrue(foal.hasAttached(GeneticsData.GENOME), "foal has genes");
		helper.assertTrue(foal.hasAttached(GeneticsData.LINEAGE), "foal has a lineage");
		helper.assertTrue(foal.hasAttached(Personalities.DATA), "foal has a personality");
		Family family = foal.getAttached(Family.DATA);
		helper.assertTrue(family != null, "foal has a family");
		helper.assertTrue(family.breeder().isPresent() && family.breeder().get().equals(breeder.getUUID()), "family knows the breeder");
		helper.assertTrue(foal.isTamed(), "foal is born tamed to its breeder");
		helper.succeed();
	}

	/** With genetics off, a foal still gets a personality, from personality's own inheritance. */
	@GameTest
	public void breedingWithGeneticsOff(GameTestHelper helper) {
		ServerPlayer breeder = helper.makeMockServerPlayerInLevel();
		withSwitches(Map.of("genetics", FeatureSwitch.OFF), () -> {
			AbstractHorse foal = breed(helper, breeder);
			helper.assertFalse(foal.hasAttached(GeneticsData.GENOME), "foal has no genes with genetics off");
			helper.assertTrue(foal.hasAttached(Personalities.DATA), "foal still has a personality");
		});
		helper.succeed();
	}

	/** With foals off, a bred foal is vanilla: no family, not tamed. */
	@GameTest
	public void breedingWithFoalsOff(GameTestHelper helper) {
		ServerPlayer breeder = helper.makeMockServerPlayerInLevel();
		withSwitches(Map.of("foals", FeatureSwitch.OFF), () -> {
			AbstractHorse foal = breed(helper, breeder);
			helper.assertFalse(foal.hasAttached(Family.DATA), "foal has no family with foals off");
			helper.assertFalse(foal.isTamed(), "foal is untamed with foals off");
		});
		helper.succeed();
	}

	/** Two horses bred by the player, as vanilla does it; returns the foal. */
	private static AbstractHorse breed(GameTestHelper helper, ServerPlayer breeder) {
		ServerLevel level = helper.getLevel();
		Horse parentA = helper.spawnWithNoFreeWill(EntityTypes.HORSE, SPOT);
		Horse parentB = helper.spawnWithNoFreeWill(EntityTypes.HORSE, SPOT.east());
		parentA.tameWithName(breeder);
		parentB.tameWithName(breeder);
		parentA.setInLove(breeder);
		parentB.setInLove(breeder);
		parentA.spawnChildFromBreeding(level, parentB);
		AABB area = new AABB(helper.absolutePos(SPOT)).inflate(4);
		List<AbstractHorse> foals = level.getEntitiesOfClass(AbstractHorse.class, area, AbstractHorse::isBaby);
		helper.assertTrue(!foals.isEmpty(), "a foal was born");
		return foals.getFirst();
	}

	/** A horn call from a player the horse trusts makes a nearby horse come. */
	@GameTest
	public void hornCallsTheHorse(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, player);
		Loyalty.setPoints(horse, player, LoyaltySettings.get().pointsNeeded(2));
		HornCall.call(player, horse.getUUID());
		helper.assertTrue(horse.hasAttached(HornCall.CALL), "trusted horse answers the call");
		helper.succeed();
	}

	/** At level 1 a horse doesn't answer, unless loyalty is off: then every tamed horse is at level 5. */
	@GameTest
	public void hornAndLoyaltyOff(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, player);
		HornCall.call(player, horse.getUUID());
		helper.assertFalse(horse.hasAttached(HornCall.CALL), "a level 1 horse doesn't answer");
		withSwitches(Map.of("loyalty", FeatureSwitch.OFF), () -> {
			helper.assertValueEqual(Bond.get().level(horse, player), Bond.MAX_LEVEL, "tamed horse level with loyalty off");
			HornCall.call(player, horse.getUUID());
			helper.assertTrue(horse.hasAttached(HornCall.CALL), "with loyalty off the horse answers");
		});
		helper.succeed();
	}

	/** Hunger needs loyalty: switching loyalty off switches hunger off too, and back on with it. */
	@GameTest
	public void hungerNeedsLoyalty(GameTestHelper helper) {
		withSwitches(Map.of("loyalty", FeatureSwitch.OFF), () -> {
			helper.assertFalse(Features.isOn("loyalty"), "loyalty is off");
			helper.assertFalse(Features.isOn("hunger"), "hunger is off with loyalty off");
			helper.assertValueEqual(Features.offBecause("hunger").orElse(""), "loyalty", "hunger says why it's off");
		});
		helper.assertTrue(Features.isOn("hunger"), "hunger is back on with loyalty");
		helper.succeed();
	}

	/** With personality off every horse acts steady and no ride modifier applies; saved personalities are kept. */
	@GameTest
	public void personalityOff(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, player);
		horse.setAttached(Personalities.DATA, Personality.LAZY);
		withSwitches(Map.of("personality", FeatureSwitch.OFF), () -> {
			helper.assertValueEqual(PersonalityView.get().of(horse), Personality.STEADY, "personality with personality off");
			helper.assertTrue(RideModifiers.ALL.active().isEmpty(), "no ride modifiers with personality off");
		});
		helper.assertValueEqual(PersonalityView.get().of(horse), Personality.LAZY, "personality back on");
		helper.assertTrue(!RideModifiers.ALL.active().isEmpty(), "ride modifiers back");
		helper.succeed();
	}

	/** With genetics off, a wild-spawned horse keeps its vanilla coat and gets no genes. */
	@GameTest
	public void geneticsOffOnSpawn(GameTestHelper helper) {
		withSwitches(Map.of("genetics", FeatureSwitch.OFF), () -> {
			Horse horse = EntityTypes.HORSE.spawn(helper.getLevel(), helper.absolutePos(SPOT), EntitySpawnReason.SPAWN_ITEM_USE);
			helper.assertTrue(horse != null, "horse spawned");
			helper.assertFalse(horse.hasAttached(GeneticsData.GENOME), "no genes with genetics off");
		});
		helper.succeed();
	}

	/** With loyalty off, treats aren't counted and a full-health horse refuses them, as in vanilla. */
	@GameTest
	public void feedingWithLoyaltyOff(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, player);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
		withSwitches(Map.of("loyalty", FeatureSwitch.OFF), () -> {
			horse.mobInteract(player, InteractionHand.MAIN_HAND);
			helper.assertValueEqual(Loyalty.get(horse, player).feeds(), 0, "feeds with loyalty off");
			helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "the wheat wasn't eaten");
		});
		helper.succeed();
	}

	/** A favorite food is worth the multiplier, and feeding it reveals it on the info screen. */
	@GameTest
	public void favoriteFoodDoublesLoyalty(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, player);
		tastes(horse, List.of("minecraft:wheat"), List.of("minecraft:apple"));
		helper.assertValueEqual(horse.getAttached(Tastes.KNOWN).hiddenLoves(), 1, "the favorite starts hidden");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
		horse.mobInteract(player, InteractionHand.MAIN_HAND);
		int expected = (int) Math.round(LoyaltySettings.get().pointsPerFeed() * HungerSettings.get().favoriteLoyaltyMultiplier());
		helper.assertValueEqual(Loyalty.get(horse, player).points(), expected, "points after a favorite");
		Tastes.Known known = horse.getAttached(Tastes.KNOWN);
		helper.assertValueEqual(known.loves(), List.of("minecraft:wheat"), "the favorite is revealed");
		helper.assertValueEqual(known.hiddenDislikes(), 1, "the dislike stays hidden");
		helper.succeed();
	}

	/** A disliked food costs only the feeder, and never takes anyone below level 1. */
	@GameTest
	public void dislikedFoodCostsTheFeeder(GameTestHelper helper) {
		ServerPlayer feeder = survivalPlayer(helper);
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, feeder);
		tastes(horse, List.of(), List.of("minecraft:wheat"));
		Loyalty.setPoints(horse, feeder, 10);
		Loyalty.setPoints(horse, other, 10);
		feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
		horse.mobInteract(feeder, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(Loyalty.get(horse, feeder).points(), 10 - HungerSettings.get().dislikeLoyaltyLoss(), "feeder's points");
		helper.assertValueEqual(Loyalty.get(horse, other).points(), 10, "the other rider keeps theirs");
		helper.assertValueEqual(feeder.getMainHandItem().getCount(), 3, "the horse still ate it");

		ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
		stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
		horse.mobInteract(stranger, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(Loyalty.get(horse, stranger).points(), 0, "no debt below 0 points");
		helper.assertValueEqual(Loyalty.level(horse, stranger), 1, "still level 1");
		helper.succeed();
	}

	/** A food from the config list that isn't horse food in vanilla is eaten; items used on horses never are. */
	@GameTest
	public void feedableFoodsFromTheConfig(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		Horse horse = tamedHorse(helper, player);
		tastes(horse, List.of(), List.of());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT, 4));
		horse.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "the carrot was eaten");
		helper.assertValueEqual(Loyalty.get(horse, player).feeds(), 1, "the carrot counts as a feed");
		helper.assertTrue(FeedableFoods.isFeedable(new ItemStack(Items.CARROT)), "carrots are feedable by default");
		helper.assertFalse(FeedableFoods.isFeedable(new ItemStack(Items.SADDLE)), "a saddle is never feedable");
		helper.assertFalse(FeedableFoods.isFeedable(new ItemStack(Items.STONE_BRICKS)), "stone bricks aren't in the default list");
		helper.succeed();
	}

	/** A tamed horse rolls its preferences on its own, from the feedable foods, and keeps them. */
	@GameTest(maxTicks = 100)
	public void preferencesAreRolledOnce(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Horse horse = tamedHorse(helper, player);
		helper.succeedWhen(() -> {
			Tastes.State state = horse.getAttached(Tastes.STATE);
			helper.assertTrue(state != null, "preferences are rolled");
			HungerSettings settings = HungerSettings.get();
			int favorites = state.favorites().size();
			helper.assertTrue(favorites >= settings.minFavorites() && favorites <= settings.maxFavorites(), "favorites in range: " + favorites);
			helper.assertValueEqual(state.dislikes().size(), settings.dislikes(), "dislikes");
			helper.assertTrue(state.favorites().stream().allMatch(FeedableFoods::has), "favorites come from the list");
			helper.assertValueEqual(Tastes.get(horse), state, "asking again doesn't re-roll");
		});
	}
}
