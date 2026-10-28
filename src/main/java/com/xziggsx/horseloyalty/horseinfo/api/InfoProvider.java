package com.xziggsx.horseloyalty.horseinfo.api;

import com.xziggsx.horseloyalty.core.link.Contributions;
import java.util.List;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * One feature's parts of the horse info screen: loyalty adds the loyalty line, personality the
 * personality, genetics the parents, looks and genes. Add one with
 * {@code context.contribute(InfoProvider.ALL, provider)}; while its feature is off its parts show as off.
 * Runs on the client, so it may only read what is synced.
 */
public interface InfoProvider {
	/** The providers of the features that are on. */
	Contributions<InfoProvider> ALL = new Contributions<>("horseinfo:providers");

	/** Adds this feature's parts to the profile for this horse and viewer. */
	void contribute(AbstractHorse horse, Player player, HorseProfile.Builder profile);

	/**
	 * What this feature's parts are built from, as comparable values, so the screen only rebuilds
	 * the profile when one changes. Cheap; empty by default.
	 */
	default List<Object> changeKey(AbstractHorse horse, Player player) {
		return List.of();
	}
}
