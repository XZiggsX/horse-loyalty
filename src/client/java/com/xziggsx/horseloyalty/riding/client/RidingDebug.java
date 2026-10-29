package com.xziggsx.horseloyalty.riding.client;

import com.xziggsx.horseloyalty.core.HorseUnits;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.DebugTone;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.client.sense.ConeScan;
import com.xziggsx.horseloyalty.riding.client.sense.PathFollow;
import com.xziggsx.horseloyalty.riding.client.sense.Senses;
import com.xziggsx.horseloyalty.riding.client.sense.Terrain;
import java.util.Locale;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** Riding's section in the dev HUD: gait, speed against target, loyalty limits, gait drops, balking and path following. Only while you ride. */
final class RidingDebug implements DebugSource {
	@Override
	public DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		if (player == null || player.getVehicle() != horse || !(horse instanceof RideHolder holder)) {
			return null;
		}
		RideControl ride = holder.horseloyalty$ride();
		DebugSection section = new DebugSection("Riding");
		Gait gait = ride.gait();
		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double top = RideNumbers.baseSpeed(Gait.GALLOP, speed) * HorseUnits.SPEED_TO_BLOCKS_PER_SECOND;
		double push = ride.push().extra();
		double target = RideNumbers.gaitSpeed(gait, speed, horse, player) * HorseUnits.SPEED_TO_BLOCKS_PER_SECOND * (1 + push);
		// Distance actually covered last tick. Not getDeltaMovement(): that is stored after
		// ground friction is applied, so it reads only about 55% of the real speed.
		double actual = Math.hypot(horse.getX() - horse.xo, horse.getZ() - horse.zo) * 20;

		section.add("Gait", gait.ordinal() + " " + gait.displayName().getString()
				+ (push > 0 ? String.format(Locale.ROOT, "  push +%.0f%%", push * 100) : ""));
		section.add("Speed", String.format(Locale.ROOT, "%.1f b/s  (target %.1f, %.0f%% of max, gallop ~%.1f)",
				actual, target, target / top * 100, top));
		section.add("Limits", String.format(Locale.ROOT, "top %.0f%%, turn %.1f deg/t",
				RideNumbers.topSpeedShare(horse, player) * 100, RideNumbers.turnRate(horse, player)));

		Senses senses = ride.senses();
		int dropTicks = RideNumbers.gaitDropTicks(horse, player, senses.path().active());
		int dropIn = Math.max(0, dropTicks - ride.gaitDropTimer());
		section.add("Gait drop", String.format(Locale.ROOT, "%.0f%% every %s, next roll in %s",
				RideNumbers.gaitDropChance(horse, player) * 100, DebugSection.seconds(dropTicks), DebugSection.seconds(dropIn)));

		int raiseCooldown = ride.raiseCooldown(horse);
		double balk = GaitRaise.failChance(horse, player, gait) * 100;
		if (raiseCooldown > 0) {
			section.add("Balk", "unsettled for " + DebugSection.seconds(raiseCooldown + 19), DebugTone.WARN);
		} else {
			section.add("Balk", String.format(Locale.ROOT, "%.1f%% on shifting up", balk), balk > 0 ? DebugTone.NORMAL : DebugTone.DIM);
		}
		addPath(section, senses);
		return section;
	}

	/** What path following makes of the way ahead, and what the scan cost. */
	private static void addPath(DebugSection section, Senses senses) {
		PathFollow path = senses.path();
		PathFollow.Decision decision = path.lastDecision();
		ConeScan scan = senses.lastScan();
		if (!path.active() || scan == null) {
			section.add("Path", decision.outcome().name().toLowerCase(Locale.ROOT), DebugTone.DIM);
			return;
		}
		String turn = Float.isNaN(path.lastTarget()) ? "none" : String.format(Locale.ROOT, "%.1f deg/t", path.lastTarget());
		section.add("Path", String.format(Locale.ROOT, "%s, error %.0f deg, turn %s",
				decision.outcome().name().toLowerCase(Locale.ROOT), decision.error(), turn));
		section.add("Cone", String.format(Locale.ROOT, "%d deep, %d columns, %d path, %d us",
				scan.depth(), scan.samples().size(), scan.count(Terrain.PATH),
				senses.scanNanos() / 1000), DebugTone.DIM);
	}
}
