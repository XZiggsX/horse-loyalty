package com.xziggsx.horseloyalty.summoning;

import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.loyalty.api.Bond;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /hl horn}: bind and show for operators, call while dev tools are on. They act on the horn
 * in your main hand; bind also on the horse you look at.
 */
final class SummoningCommands {
	private SummoningCommands() {
	}

	/** Adds the commands. */
	static void register(FeatureContext context) {
		context.adminCommand("horn", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("bind"), SummoningCommands::bind))
				.then(Commands.literal("show").executes(SummoningCommands::show)));
		context.devCommand("horn", branch -> branch
				.then(Commands.literal("call").executes(SummoningCommands::call)
						.then(Commands.argument(HorseCommands.PLAYER, EntityArgument.player()).executes(SummoningCommands::call))));
	}

	/** The horn in the player's main hand, or a failure message and null. */
	private static ItemStack horn(CommandContext<CommandSourceStack> context, ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (!HornBinding.isHorn(stack)) {
			context.getSource().sendFailure(Component.translatable("horseloyalty.command.horn.hold_one"));
			return null;
		}
		return stack;
	}

	/** Binds the held horn to the horse, skipping the loyalty check. */
	private static int bind(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack stack = horn(context, player);
		if (stack == null) {
			return 0;
		}
		AbstractHorse horse = HorseCommands.target(context);
		HornBinding.bind(stack, horse);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.horn.bound", horse.getName()));
	}

	/** Which horse the held horn calls, where it is, and whether it would answer the player. */
	private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack stack = horn(context, player);
		if (stack == null) {
			return 0;
		}
		UUID id = stack.get(HornBinding.BOUND_HORSE);
		if (id == null) {
			return HorseCommands.reply(context, Component.translatable("horseloyalty.command.horn.unbound"));
		}
		AbstractHorse horse = HornCall.find(context.getSource().getServer(), id);
		if (horse == null) {
			return HorseCommands.reply(context, Component.translatable("horseloyalty.command.horn.not_loaded", id.toString()));
		}
		SummoningSettings settings = SummoningSettings.get();
		int level = Bond.get().level(horse, player);
		boolean sameDimension = horse.level() == player.level();
		Component answer = level < settings.minLevel()
				? Component.translatable("horseloyalty.horn.not_trusted", horse.getName(), settings.minLevel(), Bond.MAX_LEVEL,
						Bond.levelName(settings.minLevel()))
				: !sameDimension && level < settings.crossDimensionLevel()
						? Component.translatable("horseloyalty.horn.other_dimension", horse.getName())
						: Component.translatable("horseloyalty.command.horn.would_answer");
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.horn.show", horse.getName(),
				horse.level().dimension().identifier().toString(), horse.blockPosition().toShortString(), answer));
	}

	/** Calls the horse to the player as if they blew the horn, without a horn sound or cooldown. */
	private static int call(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = HorseCommands.player(context, HorseCommands.PLAYER);
		ItemStack stack = horn(context, context.getSource().getPlayerOrException());
		if (stack == null) {
			return 0;
		}
		UUID id = stack.get(HornBinding.BOUND_HORSE);
		if (id == null) {
			return HorseCommands.reply(context, Component.translatable("horseloyalty.command.horn.unbound"));
		}
		// The player sees the same message above the hotbar as when blowing the horn.
		HornCall.call(player, id);
		return 1;
	}
}
