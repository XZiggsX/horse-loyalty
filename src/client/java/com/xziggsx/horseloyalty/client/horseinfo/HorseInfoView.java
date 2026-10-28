package com.xziggsx.horseloyalty.client.horseinfo;

import com.xziggsx.horseloyalty.horseinfo.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.HorseProfiles;
import com.xziggsx.horseloyalty.horseinfo.RevealPolicies;
import java.util.Objects;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * The info view's state on one open horse screen: whether it's showing, the profile it draws,
 * and how far the gene table is scrolled. A new one is made for every screen, so the screen
 * always opens on the inventory.
 * <p>
 * The profile is built when the view opens and checked every {@link #REFRESH_TICKS} ticks; it is
 * only rebuilt when something it shows has changed. Drawing a frame only reads it.
 */
public final class HorseInfoView {
	static final int REFRESH_TICKS = 10;

	private boolean open;
	private HorseProfile profile;
	private Object profileKey;
	private int ticksUntilRefresh;
	private int scroll;

	public boolean isOpen() {
		return open;
	}

	/** Shows the info view, with a fresh profile and the gene table scrolled to the top. */
	public void open(AbstractHorse horse, Player player) {
		open = true;
		scroll = 0;
		rebuild(horse, player);
	}

	/** Back to the inventory. */
	public void close() {
		open = false;
	}

	/** Called every screen tick; refreshes the profile when the horse has changed. */
	public void tick(AbstractHorse horse, Player player) {
		if (!open || --ticksUntilRefresh > 0) {
			return;
		}
		ticksUntilRefresh = REFRESH_TICKS;
		if (!Objects.equals(profileKey, HorseProfiles.changeKey(horse, player))) {
			rebuild(horse, player);
		}
	}

	private void rebuild(AbstractHorse horse, Player player) {
		profileKey = HorseProfiles.changeKey(horse, player);
		profile = HorseProfiles.of(horse, player, RevealPolicies.everything());
		ticksUntilRefresh = REFRESH_TICKS;
	}

	/** What the view draws. Null until it has been opened. */
	public HorseProfile profile() {
		return profile;
	}

	/** How many pixels the gene table is scrolled down. */
	public int scroll() {
		return scroll;
	}

	/** Scrolls the gene table, kept between the top and {@code maxScroll}. */
	public void scrollBy(int pixels, int maxScroll) {
		scroll = Math.clamp(scroll + pixels, 0, Math.max(0, maxScroll));
	}
}
