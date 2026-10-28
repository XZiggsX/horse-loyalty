package com.xziggsx.horseloyalty.loyalty.client;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.core.client.Toasts;
import com.xziggsx.horseloyalty.loyalty.LoyaltyChangePayload;
import com.xziggsx.horseloyalty.loyalty.api.Bond;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * Client side of loyalty: shows a toast when the server says your loyalty with a horse went up or
 * down a level. With {@code feedback.loyaltyToasts} off, the message goes above the hotbar instead.
 * The loyalty label in the horse inventory hooks in through {@code InventoryLoyaltyLabelMixin}.
 */
public final class LoyaltyClient {
	private LoyaltyClient() {
	}

	/** Called once from the client entrypoint. */
	public static void init() {
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
					name, level, Bond.MAX_LEVEL, Bond.levelName(level)));
			return;
		}
		Toasts.show(Toasts.LOYALTY, true, minecraft.player,
				Component.translatable("horseloyalty.toast." + key + ".title", name),
				Component.translatable("horseloyalty.toast." + key, level, Bond.MAX_LEVEL, Bond.levelName(level)));
	}
}
