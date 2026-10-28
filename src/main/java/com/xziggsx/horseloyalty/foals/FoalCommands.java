package com.xziggsx.horseloyalty.foals;

import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.DebugTone;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * {@code /hl foal}: show for operators; breed, grow and follow while dev tools are on. Also the
 * foal's section in the dev HUD and {@code /hl info}, which {@code show} reuses.
 */
final class FoalCommands implements DebugSource {
	private static final String SECONDS = "seconds";

	private FoalCommands() {
	}

	/** Adds the commands and the debug section. */
	static void register(FeatureContext context) {
		context.contribute(DebugSource.ALL, new FoalCommands());
		context.adminCommand("foal", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("show"), FoalCommands::show)));
		context.devCommand("foal", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("mark"), FoalCommands::mark))
				.then(HorseCommands.withHorse(Commands.literal("breed"), FoalCommands::breed))
				.then(HorseCommands.withHorse(Commands.literal("grow"), FoalCommands::grow)
						.then(HorseCommands.withHorse(Commands.argument(SECONDS, IntegerArgumentType.integer(1)), FoalCommands::grow)))
				.then(Commands.literal("follow")
						.then(HorseCommands.withHorse(Commands.literal("parent"), FoalCommands::followParent))
						.then(HorseCommands.withHorse(Commands.literal("other"), FoalCommands::followOther))));
	}

	/** The family: both parents, which one it follows, the breeder, and whether each is loaded. Server's copy only. */
	@Override
	public DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		Family family = serverHorse == null ? null : serverHorse.getAttached(Family.DATA);
		if (family == null || !(serverHorse.level() instanceof ServerLevel level)) {
			return null;
		}
		DebugSection section = new DebugSection("Foal");
		section.add("Follows", describe(level, family.followed()));
		section.add("Other parent", describe(level, family.otherParent()));
		section.add("Breeder", family.breeder().map(id -> describe(level, id)).orElse("nobody"));
		section.add("Looked after", Family.isFamilyFoal(serverHorse) ? "yes" : "no (grown, ridden or on a lead)",
				Family.isFamilyFoal(serverHorse) ? DebugTone.NORMAL : DebugTone.DIM);
		return section;
	}

	private static String describe(ServerLevel level, UUID id) {
		Entity entity = level.getEntity(id);
		return entity != null ? entity.getName().getString() + " (loaded)" : id.toString().substring(0, 8) + " (not loaded)";
	}

	private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		DebugSection section = new FoalCommands().section(horse, horse, context.getSource().getPlayer());
		if (section == null) {
			return HorseCommands.reply(context, Component.translatable("horseloyalty.command.foal.no_family", horse.getName()));
		}
		return HorseCommands.replyLines(context, section.lines());
	}

	private static int mark(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		HorseCommands.mark(context.getSource().getPlayerOrException(), horse);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.foal.marked", horse.getName()));
	}

	/**
	 * Breeds the marked horse with this one, now, as the player who runs it. Runs the full breeding
	 * event, so genes, personality and family are set up as in real breeding.
	 */
	private static int breed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse parentA = HorseCommands.marked(context.getSource());
		AbstractHorse parentB = HorseCommands.target(context);
		ServerPlayer breeder = context.getSource().getPlayerOrException();
		ServerLevel level = context.getSource().getLevel();
		if (!(parentA.getBreedOffspring(level, parentB) instanceof AbstractHorse foal)) {
			context.getSource().sendFailure(Component.translatable("horseloyalty.command.foal.cant_breed"));
			return 0;
		}
		foal.setBaby(true);
		foal.snapTo(parentA.getX(), parentA.getY(), parentA.getZ());
		HorseEvents.BRED.invoker().bred(foal, parentA, parentB, breeder);
		level.addFreshEntityWithPassengers(foal);
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.foal.bred", parentA.getName(), parentB.getName()));
	}

	/** Ages the foal by that many seconds, or straight to adult. */
	private static int grow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		if (HorseCommands.has(context, SECONDS)) {
			horse.ageUp(IntegerArgumentType.getInteger(context, SECONDS));
		} else {
			// Age 0 is grown up.
			horse.setAge(0);
		}
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.foal.grown", horse.getName(), horse.getAge()));
	}

	private static int followParent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return follow(context, false);
	}

	private static int followOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return follow(context, true);
	}

	/** Makes the foal follow its chosen parent, or swaps to the other one. */
	private static int follow(CommandContext<CommandSourceStack> context, boolean other) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		Family family = horse.getAttached(Family.DATA);
		if (family == null) {
			return HorseCommands.reply(context, Component.translatable("horseloyalty.command.foal.no_family", horse.getName()));
		}
		if (other) {
			horse.setAttached(Family.DATA, new Family(family.parentA(), family.parentB(), family.otherParent(), family.breeder()));
		}
		Optional<AbstractHorse> parent = Family.parentToFollow(horse, context.getSource().getLevel());
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.foal.follows", horse.getName(),
				parent.map(AbstractHorse::getName).orElse(Component.translatable("horseloyalty.command.foal.not_loaded"))));
	}
}
