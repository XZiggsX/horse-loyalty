package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.InfoProvider;
import com.xziggsx.horseloyalty.personality.api.Personality;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * {@code /hl personality}: show and set for operators, reroll while dev tools are on. Also
 * personality's part of the horse info screen and its section in the dev HUD and {@code /hl info}.
 * Setting a personality while genetics is on refits the temperament genes within a second, the
 * same way genetics keeps every horse's genes in step with what it shows.
 */
final class PersonalityCommands implements InfoProvider, DebugSource {
	private static final String PERSONALITY = "personality";
	private static final SuggestionProvider<CommandSourceStack> IDS = (context, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(Personality.values()).map(Personality::id), builder);

	private PersonalityCommands() {
	}

	/** Adds the commands, the info screen part and the debug section. */
	static void register(FeatureContext context) {
		PersonalityCommands parts = new PersonalityCommands();
		context.contribute(InfoProvider.ALL, parts);
		context.contribute(DebugSource.ALL, parts);
		context.adminCommand("personality", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("show"), PersonalityCommands::show))
				.then(Commands.literal("set").then(HorseCommands.withHorse(
						Commands.argument(PERSONALITY, StringArgumentType.word()).suggests(IDS), PersonalityCommands::set))));
		context.devCommand("personality", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("reroll"), PersonalityCommands::reroll)));
	}

	@Override
	public void contribute(AbstractHorse horse, Player player, HorseProfile.Builder profile) {
		profile.personality(() -> Personalities.of(horse));
	}

	@Override
	public List<Object> changeKey(AbstractHorse horse, Player player) {
		return List.of(Personalities.of(horse));
	}

	/** The personality and, for the viewer, whether it's revealed yet. */
	@Override
	public DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		DebugSection section = new DebugSection("Personality");
		boolean revealed = player != null && Personalities.isRevealedTo(horse, player);
		section.add("Personality", Personalities.of(horse).displayName().getString()
				+ (horse.hasAttached(Personalities.DATA) ? "" : "  (none yet)")
				+ (revealed ? "" : "  (hidden from you)"));
		return section;
	}

	private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		return HorseCommands.replyLines(context, new PersonalityCommands().section(horse, horse, context.getSource().getPlayer()).lines());
	}

	private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		String id = StringArgumentType.getString(context, PERSONALITY);
		Personality personality = Personality.byId(id);
		if (!personality.id().equals(id)) {
			context.getSource().sendFailure(Component.translatable("horseloyalty.command.personality.unknown", id));
			return 0;
		}
		horse.setAttached(Personalities.DATA, personality);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.personality.set",
				horse.getName(), personality.displayName()));
	}

	/** A fresh roll with the configured weights. */
	private static int reroll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		Personality personality = Personalities.roll(horse.getRandom());
		horse.setAttached(Personalities.DATA, personality);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.personality.set",
				horse.getName(), personality.displayName()));
	}
}
