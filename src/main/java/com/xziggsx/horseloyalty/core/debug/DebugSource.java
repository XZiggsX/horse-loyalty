package com.xziggsx.horseloyalty.core.debug;

import com.xziggsx.horseloyalty.core.link.Contributions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * One feature's part of what the dev HUD and {@code /hl info} show about a horse. Each feature adds
 * its own with {@code context.contribute(DebugSource.ALL, source)}, so the HUD knows no feature, and
 * a feature that is off simply has no section.
 */
@FunctionalInterface
public interface DebugSource {
	/** Every source of the features that are on, in the order they were added. */
	Contributions<DebugSource> ALL = new Contributions<>("core:debug_sources");

	/**
	 * This feature's section about the horse for this player, or null to show none.
	 *
	 * @param horse       the horse as this side sees it (the client's copy in the dev HUD)
	 * @param serverHorse the server's copy, with the server-only data; the same horse on the server,
	 *                    the integrated server's copy in singleplayer, or null in multiplayer
	 * @param player      who is looking
	 */
	DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player);

	/** Every section to show about a horse, top to bottom. */
	static List<DebugSection> sections(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		List<DebugSection> sections = new ArrayList<>();
		for (DebugSource source : ALL.active()) {
			DebugSection section = source.section(horse, serverHorse, player);
			if (section != null) {
				sections.add(section);
			}
		}
		return sections;
	}
}
