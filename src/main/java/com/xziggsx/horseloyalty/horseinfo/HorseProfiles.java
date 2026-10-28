package com.xziggsx.horseloyalty.horseinfo;

import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.HorseUnits;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.StatLine;
import com.xziggsx.horseloyalty.horseinfo.api.InfoProvider;
import com.xziggsx.horseloyalty.horseinfo.api.RevealPolicy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Builds the {@link HorseProfile} the horse info screen shows. Horse info itself adds the kind of
 * horse and its stats; every other part comes from the {@link InfoProvider}s of the features that
 * are on. The screen only sees the profile, so either side can change without the other. Works on
 * the client, where everything the providers read is synced.
 */
public final class HorseProfiles {
	private HorseProfiles() {
	}

	/** Whether this horse has an info screen: any horse in scope does. */
	public static boolean available(AbstractHorse horse) {
		return HorseScope.inScope(horse);
	}

	/** The profile of this horse as the player may see it. Call only when {@link #available}. */
	public static HorseProfile of(AbstractHorse horse, Player player, RevealPolicy policy) {
		HorseProfile.Builder builder = new HorseProfile.Builder(horse.getType().getDescription(), () -> stats(horse));
		for (InfoProvider provider : InfoProvider.ALL.active()) {
			provider.contribute(horse, player, builder);
		}
		return builder.build(policy, horse, player);
	}

	/**
	 * Everything the profile is built from, as one comparable value: the screen rebuilds the
	 * profile only when this changes. Cheap, no gene reading.
	 */
	public static Object changeKey(AbstractHorse horse, Player player) {
		List<Object> key = new ArrayList<>(List.of(horse.getHealth(), horse.getMaxHealth(),
				horse.getAttributeValue(Attributes.MOVEMENT_SPEED), horse.getAttributeValue(Attributes.JUMP_STRENGTH)));
		List<InfoProvider> providers = InfoProvider.ALL.active();
		// A feature switching on or off changes the profile too.
		key.add(providers.size());
		for (InfoProvider provider : providers) {
			key.addAll(provider.changeKey(horse, player));
		}
		return key;
	}

	/** Health, speed and jump. Add a line here for a new stat; the screen formats it by id. */
	private static List<StatLine> stats(AbstractHorse horse) {
		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
		return List.of(
				new StatLine("health", horse.getHealth(), horse.getMaxHealth(),
						HorseUnits.fill(horse.getHealth(), 0, horse.getMaxHealth())),
				new StatLine("speed", HorseUnits.blocksPerSecond(speed), HorseUnits.blocksPerSecond(HorseUnits.MAX_SPEED),
						HorseUnits.fill(speed, HorseUnits.MIN_SPEED, HorseUnits.MAX_SPEED)),
				new StatLine("jump", HorseUnits.jumpBlocks(jump), HorseUnits.jumpBlocks(HorseUnits.MAX_JUMP),
						HorseUnits.fill(jump, HorseUnits.MIN_JUMP, HorseUnits.MAX_JUMP)));
	}
}
