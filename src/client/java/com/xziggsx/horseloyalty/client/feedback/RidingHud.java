package com.xziggsx.horseloyalty.client.feedback;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.client.riding.ClientGait;
import com.xziggsx.horseloyalty.client.riding.Gait;
import com.xziggsx.horseloyalty.client.riding.GaitHolder;
import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * While riding: five horseshoes for your loyalty with the horse and a four-step gait bar, in a
 * row right above the horse's health hearts. While you push a gallop, the bar warms up and glows,
 * stronger the harder you push, and fades with the push. The horseshoes are GUI sprites under
 * {@code textures/gui/sprites/hud/}, so resource packs can restyle them. Turn it off with
 * {@code feedback.ridingHud} in the config.
 */
public final class RidingHud implements HudElement {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "riding_hud");
	private static final Identifier SHOE_FULL = Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "hud/loyalty_full");
	private static final Identifier SHOE_EMPTY = Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "hud/loyalty_empty");

	private static final int ICON_SIZE = 9;
	/** Same spacing as vanilla hearts. */
	private static final int ICON_STEP = 8;
	private static final int BAR_WIDTH = 2;
	private static final int BAR_GAP = 1;
	private static final int BAR_COUNT = Gait.GALLOP.ordinal();
	private static final int GAP_BEFORE_BARS = 4;

	private static final int BAR_ON_COLOR = 0xFFF3E3B4;
	private static final int BAR_OFF_COLOR = 0x50FFFFFF;
	private static final int OUTLINE_COLOR = 0xA0000000;
	/** Lit bars turn this color at the full gallop push. */
	private static final int BAR_PUSH_COLOR = 0xFFFF9A3C;
	/** Glow around the bars at the full gallop push. */
	private static final int PUSH_GLOW_COLOR = 0x90FF7A1A;

	private RidingHud() {
	}

	/** Adds the row to the HUD, drawn right after the horse's health. */
	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MOUNT_HEALTH, ID, new RidingHud());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || !HorseLoyaltyConfig.get().ridingHud()
				|| !(player.getVehicle() instanceof AbstractHorse horse) || !Loyalty.appliesTo(horse)) {
			return;
		}
		// Vanilla draws the horse's hearts in rows of 10 from 39 pixels above the bottom, going up,
		// right-aligned to the hotbar. Sit on top of the last row.
		int hearts = horse.showVehicleHealth() ? Math.min(30, (int) (horse.getMaxHealth() + 0.5F) / 2) : 0;
		int rows = (int) Math.ceil(hearts / 10.0);
		int top = graphics.guiHeight() - 39 - rows * 10;
		int right = graphics.guiWidth() / 2 + 91;

		int barsWidth = BAR_COUNT * (BAR_WIDTH + BAR_GAP) - BAR_GAP;
		int barsLeft = right - barsWidth;
		// Only the rider knows the push; it isn't synced.
		float push = horse.getControllingPassenger() == player
				? (float) ((GaitHolder) horse).horseloyalty$getGallopPush().effort() : 0;
		extractGaitBars(graphics, ClientGait.of(horse), push, barsLeft, top + ICON_SIZE);

		// Untamed horses have no loyalty to show yet.
		if (horse.isTamed()) {
			int level = Loyalty.level(horse, player);
			int shoesLeft = barsLeft - GAP_BEFORE_BARS - (Loyalty.MAX_LEVEL - 1) * ICON_STEP - ICON_SIZE;
			for (int i = 0; i < Loyalty.MAX_LEVEL; i++) {
				Identifier sprite = i < level ? SHOE_FULL : SHOE_EMPTY;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, shoesLeft + i * ICON_STEP, top, ICON_SIZE, ICON_SIZE);
			}
		}
	}

	/**
	 * Four bars, each taller than the last, lit up to the current gait. {@code push} (0 to 1) is how
	 * hard a gallop is pushed: it tints the lit bars and puts a halo around them. {@code bottom} is exclusive.
	 */
	private static void extractGaitBars(GuiGraphicsExtractor graphics, Gait gait, float push, int left, int bottom) {
		if (push > 0) {
			// A halo one pixel outside each lit bar's outline, all drawn before any bar.
			int glow = ARGB.color(Math.round(ARGB.alpha(PUSH_GLOW_COLOR) * push), PUSH_GLOW_COLOR);
			for (int i = 0; i < Math.min(BAR_COUNT, gait.ordinal()); i++) {
				int x = left + i * (BAR_WIDTH + BAR_GAP);
				graphics.fill(x - 2, bottom - (3 + i * 2) - 2, x + BAR_WIDTH + 2, bottom + 2, glow);
			}
		}
		int lit = ARGB.srgbLerp(push, BAR_ON_COLOR, BAR_PUSH_COLOR);
		for (int i = 0; i < BAR_COUNT; i++) {
			int x = left + i * (BAR_WIDTH + BAR_GAP);
			int height = 3 + i * 2;
			int y = bottom - height;
			graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, bottom + 1, OUTLINE_COLOR);
			graphics.fill(x, y, x + BAR_WIDTH, bottom, i < gait.ordinal() ? lit : BAR_OFF_COLOR);
		}
	}
}
