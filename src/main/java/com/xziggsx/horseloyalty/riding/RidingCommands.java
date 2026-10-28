package com.xziggsx.horseloyalty.riding;

import com.xziggsx.horseloyalty.core.HorseUnits;
import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.DebugTone;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.loyalty.api.Bond;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.api.GaitEvent;
import com.xziggsx.horseloyalty.riding.api.RideModifiers;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * {@code /hl riding}: the resolved riding numbers for a level, and the gait a rider last reported.
 * Riding runs on the rider's client, so the server can't set a gait or force a bolt; the dev HUD
 * shows the live ride. Also the server's riding section in {@code /hl info}.
 */
final class RidingCommands implements DebugSource {
	private static final String LEVEL = "level";

	private RidingCommands() {
	}

	/** Adds the commands and the server's debug section. */
	static void register(FeatureContext context) {
		context.contribute(DebugSource.ALL, new RidingCommands());
		context.adminCommand("riding", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("numbers"), RidingCommands::numbers)
						.then(HorseCommands.withHorse(Commands.argument(LEVEL, IntegerArgumentType.integer(0, Bond.MAX_LEVEL)),
								RidingCommands::numbers)))
				.then(HorseCommands.withHorse(Commands.literal("gait"), RidingCommands::gait)));
	}

	/**
	 * The gait the rider reported and the last event, as the server knows them. Only on the
	 * server itself: on a client the riding client's own section shows the live ride instead.
	 */
	@Override
	public DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		if (serverHorse != horse) {
			return null;
		}
		Gait gait = GaitSync.gait(horse);
		GaitEvent event = horse.getAttached(GaitSync.LAST_EVENT);
		return new DebugSection("Riding (server)")
				.add("Gait", gait == null ? "none (nobody with the mod rides it)" : gait.id(), gait == null ? DebugTone.DIM : DebugTone.NORMAL)
				.add("Last event", event == null ? "none" : event.name().toLowerCase(Locale.ROOT), DebugTone.DIM);
	}

	/** Top speed, each gait in blocks per second, turn rate, gait drops, raise odds and push limits. */
	private static int numbers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		ServerPlayer player = context.getSource().getPlayerOrException();
		int level = HorseCommands.has(context, LEVEL) ? IntegerArgumentType.getInteger(context, LEVEL) : Bond.get().level(horse, player);
		RidingSettings settings = RidingSettings.get();
		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		List<String> lines = new ArrayList<>();
		lines.add("Riding " + horse.getName().getString() + " at loyalty " + level + " (ride modifiers for you applied):");
		lines.add(String.format(Locale.ROOT, "  Top speed: %.0f%%", settings.topSpeedShare(level) * 100));
		StringBuilder gaits = new StringBuilder("  Gaits:");
		for (Gait gait : Gait.values()) {
			if (gait != Gait.HALT) {
				double blocks = HorseUnits.blocksPerSecond(speed * 0.98 * settings.gaitSpeedShare(gait) * settings.topSpeedShare(level));
				gaits.append(String.format(Locale.ROOT, " %s %.1f b/s", gait.id(), blocks));
			}
		}
		lines.add(gaits.toString());
		lines.add(String.format(Locale.ROOT, "  Turn: %.1f deg/tick", settings.turnDegreesPerTick(level)));
		double dropChance = RideModifiers.gaitDropChance(horse, player, settings.gaitDropChance(level));
		int dropTicks = RideModifiers.gaitDropTicks(horse, player, settings.gaitDropTicks(level));
		lines.add(String.format(Locale.ROOT, "  Gait drop: %.0f%% every %s", dropChance * 100, DebugSection.seconds(dropTicks)));
		StringBuilder raise = new StringBuilder("  Raise fails:");
		for (Gait gait : Gait.values()) {
			raise.append(String.format(Locale.ROOT, " from %s %.1f%%", gait.id(),
					settings.gaitRaiseEnabled() ? settings.gaitRaiseFailChance(gait, level) * 100 : 0));
		}
		lines.add(raise.toString());
		lines.add(String.format(Locale.ROOT, "  Buck chance on refusing: %.0f%%", RideModifiers.buckChance(horse, player, 0) * 100));
		lines.add(String.format(Locale.ROOT, "  Gallop push: %s, +%.0f%% a tap up to +%.0f%%, holds %s",
				settings.gallopPushEnabled() ? "on" : "off", settings.gallopPushStep() * 100, settings.gallopPushMax() * 100,
				DebugSection.seconds(RideModifiers.gallopPushHoldTicks(horse, player, settings.gallopPushHoldTicks()))));
		return HorseCommands.replyLines(context, lines);
	}

	/** The gait the rider last reported for this horse, and the last event. */
	private static int gait(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		return HorseCommands.replyLines(context, new RidingCommands().section(horse, horse, context.getSource().getPlayer()).lines());
	}
}
