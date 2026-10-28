package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * {@code /hl loyalty}: show, list, set, forget and tame for operators, and {@code points} for
 * testing thresholds while dev tools are on. Each acts on the horse you look at (or a selector at
 * the end) and, where it's about one rider, on you or the player you name.
 */
final class LoyaltyCommands {
	private static final String LEVEL = "level";
	private static final String AMOUNT = "amount";

	private LoyaltyCommands() {
	}

	/** Adds the commands. */
	static void register(FeatureContext context) {
		context.adminCommand("loyalty", branch -> branch
				.then(HorseCommands.withPlayerAndHorse(Commands.literal("show"), LoyaltyCommands::show))
				.then(HorseCommands.withHorse(Commands.literal("list"), LoyaltyCommands::list))
				.then(Commands.literal("set").then(HorseCommands.withPlayerAndHorse(
						Commands.argument(LEVEL, IntegerArgumentType.integer(1, Loyalty.MAX_LEVEL)), LoyaltyCommands::setLevel)))
				.then(HorseCommands.withPlayerAndHorse(Commands.literal("forget"), LoyaltyCommands::forget))
				.then(HorseCommands.withPlayerAndHorse(Commands.literal("tame"), LoyaltyCommands::tame)));
		context.devCommand("loyalty", branch -> branch
				.then(Commands.literal("points")
						.then(Commands.literal("add").then(HorseCommands.withPlayerAndHorse(
								Commands.argument(AMOUNT, IntegerArgumentType.integer()), LoyaltyCommands::addPoints)))
						.then(Commands.literal("set").then(HorseCommands.withPlayerAndHorse(
								Commands.argument(AMOUNT, IntegerArgumentType.integer(0)), LoyaltyCommands::setPoints)))));
	}

	/** Level, points, feeds, ride time, points lost to hunger, and points to the next level. */
	private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		ServerPlayer player = HorseCommands.player(context, HorseCommands.PLAYER);
		DebugSection section = new LoyaltyInfo().section(horse, horse, player);
		if (section == null) {
			return HorseCommands.reply(context, Component.translatable("horseloyalty.command.loyalty.untamed", horse.getName()));
		}
		List<String> lines = new ArrayList<>();
		lines.add(player.getName().getString() + " with " + horse.getName().getString() + ":");
		lines.addAll(section.lines());
		return HorseCommands.replyLines(context, lines);
	}

	/** Every rider the horse remembers, with their level and points. */
	private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		Map<UUID, LoyaltyData> riders = horse.getAttachedOrElse(Loyalty.DATA, Map.of());
		List<String> lines = new ArrayList<>();
		lines.add(horse.getName().getString() + " remembers " + riders.size() + "/" + Loyalty.MAX_REMEMBERED_RIDERS + " riders:");
		riders.entrySet().stream()
				.sorted(Comparator.comparingInt((Map.Entry<UUID, LoyaltyData> entry) -> entry.getValue().points()).reversed())
				.forEach(entry -> lines.add("  " + name(context, entry.getKey()) + ": level " + Loyalty.earnedLevel(entry.getValue())
						+ ", " + entry.getValue().points() + " points"));
		return HorseCommands.replyLines(context, lines);
	}

	/** Gives or takes bonus points so the player is at the start of that level. */
	private static int setLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		ServerPlayer player = HorseCommands.player(context, HorseCommands.PLAYER);
		int level = IntegerArgumentType.getInteger(context, LEVEL);
		Loyalty.setPoints(horse, player, LoyaltySettings.get().pointsNeeded(level));
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.loyalty.set",
				player.getName(), horse.getName(), level, Loyalty.MAX_LEVEL, Loyalty.levelName(level)));
	}

	private static int addPoints(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return points(context, true);
	}

	private static int setPoints(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return points(context, false);
	}

	/** Adds to or sets the player's points directly. */
	private static int points(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		ServerPlayer player = HorseCommands.player(context, HorseCommands.PLAYER);
		int amount = IntegerArgumentType.getInteger(context, AMOUNT);
		int points = add ? Loyalty.get(horse, player).points() + amount : amount;
		Loyalty.setPoints(horse, player, points);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.loyalty.points",
				player.getName(), horse.getName(), Loyalty.get(horse, player).points(), Loyalty.level(horse, player)));
	}

	private static int forget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		ServerPlayer player = HorseCommands.player(context, HorseCommands.PLAYER);
		Loyalty.forget(horse, player.getUUID());
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.loyalty.forget", horse.getName(), player.getName()));
	}

	/** Tames the horse to the player at level 1, without the taming minigame. */
	private static int tame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		ServerPlayer player = HorseCommands.player(context, HorseCommands.PLAYER);
		horse.setOwner(player);
		horse.setTamed(true);
		Loyalty.setPoints(horse, player, 0);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.loyalty.tame", horse.getName(), player.getName()));
	}

	/** An online player's name, else the start of their id. */
	private static String name(CommandContext<CommandSourceStack> context, UUID id) {
		ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(id);
		return player != null ? player.getName().getString() : id.toString().substring(0, 8);
	}
}
