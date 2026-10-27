package com.xziggsx.horseloyalty.client.feedback;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Toasts about the ride itself, such as a horse refusing to go faster. The same kind of toast as
 * {@link LoyaltyToasts}, with its own id so it doesn't replace a loyalty toast that's still up.
 * With {@code feedback.ridingToasts} off, the message goes above the hotbar instead.
 */
public final class RidingToasts {
	/** One id for all riding toasts, so a new one replaces the last instead of stacking. Shown for 5 s. */
	private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(5000L);

	private RidingToasts() {
	}

	/** Shows a toast to the local rider. Only call this on the rider's own client. */
	public static void show(Player player, Component title, Component message) {
		if (!HorseLoyaltyConfig.get().ridingToasts()) {
			player.sendOverlayMessage(Component.empty().append(title).append("  ").append(message));
			return;
		}
		SystemToast.addOrUpdate(Minecraft.getInstance().gui.toastManager(), TOAST_ID, title, message);
	}
}
