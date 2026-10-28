package com.xziggsx.horseloyalty.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The toasts in the corner: loyalty changes, and the ride itself, such as a horse refusing to go
 * faster. They stay up longer than a message above the hotbar and don't get replaced by the next
 * gait change. Each kind has its own id, so a riding toast doesn't replace a loyalty toast that's
 * still up. Whoever shows a toast decides whether the player wants toasts at all.
 */
public final class Toasts {
	/** Loyalty level changes. A new one replaces the last instead of stacking. Shown for 5 s. */
	public static final SystemToast.SystemToastId LOYALTY = new SystemToast.SystemToastId(5000L);
	/** The ride itself, such as a horse balking at a gait change. Shown for 5 s. */
	public static final SystemToast.SystemToastId RIDING = new SystemToast.SystemToastId(5000L);

	private Toasts() {
	}

	/**
	 * Shows a toast to the local player, or the same text above the hotbar if they turned this kind
	 * of toast off. Only call this on the player's own client.
	 */
	public static void show(SystemToast.SystemToastId id, boolean asToast, Player player, Component title, Component message) {
		if (!asToast) {
			player.sendOverlayMessage(Component.empty().append(title).append("  ").append(message));
			return;
		}
		SystemToast.addOrUpdate(Minecraft.getInstance().gui.toastManager(), id, title, message);
	}
}
