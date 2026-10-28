package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.DebugTone;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * {@code /hl hunger}: show and feed for operators, starve while dev tools are on. Also hunger's
 * section in the dev HUD and {@code /hl info}, which {@code show} reuses.
 */
final class HungerCommands implements DebugSource {
	private HungerCommands() {
	}

	/** Adds the commands and the debug section. */
	static void register(FeatureContext context) {
		context.contribute(DebugSource.ALL, new HungerCommands());
		context.adminCommand("hunger", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("show"), HungerCommands::show))
				.then(HorseCommands.withHorse(Commands.literal("feed"), HungerCommands::feed)));
		context.devCommand("hunger", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("starve"), HungerCommands::starve)));
	}

	/** Time since fed and until hungry, or the time to the next loss. Needs the server's copy; tamed horses only. */
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
		return section;
	}

	private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
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
}
