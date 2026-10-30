package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.DebugTone;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * {@code /hl hunger}: show and feed for operators, starve while dev tools are on; {@code /hl hunger
 * tastes} shows and re-rolls the preferences, and dev tools add {@code like}, {@code dislike} and
 * {@code reveal}. Also hunger's section in the dev HUD and {@code /hl info}, which {@code show} reuses.
 */
final class HungerCommands implements DebugSource {
	private static final String FOOD = "food";

	private HungerCommands() {
	}

	/** Adds the commands and the debug section. */
	static void register(FeatureContext context) {
		context.contribute(DebugSource.ALL, new HungerCommands());
		context.adminCommand("hunger", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("show"), HungerCommands::show))
				.then(HorseCommands.withHorse(Commands.literal("feed"), HungerCommands::feed))
				.then(Commands.literal("tastes")
						.then(HorseCommands.withHorse(Commands.literal("show"), HungerCommands::show))
						.then(HorseCommands.withHorse(Commands.literal("reroll"), HungerCommands::reroll))));
		context.devCommand("hunger", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("starve"), HungerCommands::starve))
				.then(Commands.literal("like").then(food(true)))
				.then(Commands.literal("dislike").then(food(false)))
				.then(HorseCommands.withHorse(Commands.literal("reveal"), HungerCommands::reveal)));
	}

	/** The food argument of like and dislike, suggesting the feedable foods. Acts on the horse you look at. */
	private static RequiredArgumentBuilder<CommandSourceStack, String> food(boolean favorite) {
		return Commands.argument(FOOD, StringArgumentType.greedyString())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(FeedableFoods.keys(), builder))
				.executes(context -> prefer(context, favorite));
	}

	/** Time since fed and until hungry, or the time to the next loss, then the preferences. Needs the server's copy; tamed horses only. */
	@Override
	public DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		if (serverHorse == null || !serverHorse.isTamed()) {
			return null;
		}
		DebugSection section = new DebugSection("Hunger");
		long sinceAte = Hunger.ticksSinceFed(serverHorse);
		if (Hunger.isHungry(serverHorse)) {
			section.add("Hunger", "HUNGRY, loses points in " + DebugSection.seconds(Hunger.ticksToNextLoss(serverHorse)), DebugTone.WARN);
		} else {
			section.add("Hunger", "fed " + DebugSection.seconds(sinceAte) + " ago, hungry in "
					+ DebugSection.seconds(Math.max(0, HungerSettings.get().ticksUntilHungry() - sinceAte)));
		}
		Tastes.State tastes = serverHorse.getAttached(Tastes.STATE);
		if (!HungerSettings.get().preferences()) {
			section.add("Tastes", "off", DebugTone.DIM);
		} else if (tastes == null) {
			section.add("Tastes", "not rolled yet", DebugTone.DIM);
		} else {
			section.add("Loves", foods(tastes.favorites(), tastes));
			section.add("Dislikes", foods(tastes.dislikes(), tastes));
		}
		return section;
	}

	/** The entries, with a * after each revealed one and the ones no longer feedable in brackets. */
	private static String foods(List<String> keys, Tastes.State tastes) {
		if (keys.isEmpty()) {
			return "none";
		}
		return String.join(", ", keys.stream().map(key -> {
			String shown = key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
			shown += tastes.revealed().contains(key) ? "*" : "";
			return FeedableFoods.has(key) ? shown : "(" + shown + ")";
		}).toList());
	}

	private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		if (horse.isTamed() && HungerSettings.get().preferences()) {
			Tastes.get(horse);
		}
		DebugSection section = new HungerCommands().section(horse, horse, context.getSource().getPlayer());
		if (section == null) {
			return HorseCommands.reply(context, Component.translatable("horseloyalty.command.hunger.untamed", horse.getName()));
		}
		return HorseCommands.replyLines(context, section.lines());
	}

	private static int feed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		Hunger.feed(horse);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.hunger.feed", horse.getName()));
	}

	private static int starve(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		Hunger.starve(horse);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.hunger.starve", horse.getName()));
	}

	private static int reroll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		Tastes.reroll(horse);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.hunger.reroll", horse.getName()));
	}

	private static int reveal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		Tastes.revealAll(horse);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.hunger.reveal", horse.getName()));
	}

	private static int prefer(CommandContext<CommandSourceStack> context, boolean favorite) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		String food = StringArgumentType.getString(context, FOOD).trim();
		if (!FeedableFoods.has(food)) {
			return HorseCommands.reply(context, Component.translatable("horseloyalty.command.hunger.not_feedable", food));
		}
		Tastes.prefer(horse, food, favorite);
		return HorseCommands.reply(context, Component.translatable(
				favorite ? "horseloyalty.command.hunger.like" : "horseloyalty.command.hunger.dislike", horse.getName(), food));
	}
}
