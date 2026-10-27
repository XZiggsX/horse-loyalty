package com.xziggsx.horseloyalty.client.feedback;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.xziggsx.horseloyalty.loyalty.LoyaltyChangePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * Shows a toast in the corner when the server says your loyalty with a horse went up or down a
 * level. It stays up longer than a message above the hotbar, and doesn't get replaced by the
 * next gait change. With {@code feedback.loyaltyToasts} off, the message goes above the hotbar instead.
 */
public final class LoyaltyToasts {
	/** One id for all our toasts, so a new one replaces the last instead of stacking. Shown for 5 s. */
	private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(5000L);

	private LoyaltyToasts() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(LoyaltyChangePayload.TYPE,
				(payload, context) -> show(context.client(), payload));
	}

	private static void show(Minecraft minecraft, LoyaltyChangePayload payload) {
		if (minecraft.level == null || minecraft.player == null) {
			return;
		}
		Entity horse = minecraft.level.getEntity(payload.horseId());
		Component name = horse != null ? horse.getName() : Component.translatable("horseloyalty.toast.your_horse");
		int level = payload.level();
		String key = payload.up() ? "level_up" : "level_down";
		if (!HorseLoyaltyConfig.get().loyaltyToasts()) {
			minecraft.player.sendOverlayMessage(Component.translatable("horseloyalty.loyalty." + key,
					name, level, Loyalty.MAX_LEVEL, Loyalty.levelName(level)));
			return;
		}
		SystemToast.addOrUpdate(minecraft.gui.toastManager(), TOAST_ID,
				Component.translatable("horseloyalty.toast." + key + ".title", name),
				Component.translatable("horseloyalty.toast." + key, level, Loyalty.MAX_LEVEL, Loyalty.levelName(level)));
	}
}
