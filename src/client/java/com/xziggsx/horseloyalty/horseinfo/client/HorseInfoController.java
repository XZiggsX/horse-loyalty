package com.xziggsx.horseloyalty.horseinfo.client;

import com.xziggsx.horseloyalty.horseinfo.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.HorseProfiles;
import com.xziggsx.horseloyalty.horseinfo.RevealPolicies;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The horse info feature on one open horse screen: adds the tabs, switches between the
 * inventory and the info view (changing the window size), keeps the profile fresh, scrolls the
 * gene table and draws the view. The screen's mixin only forwards to this. A new one is made
 * for every screen, so the screen always opens on the inventory.
 * <p>
 * The profile is built when the info view opens and checked every {@link #REFRESH_TICKS} ticks;
 * it is only rebuilt when something it shows has changed. Drawing a frame only reads it.
 * Nothing shows when genetics is off (the server's setting) or the horse has no genes yet.
 */
public final class HorseInfoController {
	/** How far one mouse wheel notch scrolls the gene table. */
	private static final int SCROLL_STEP = 11;
	private static final int REFRESH_TICKS = 10;

	private final HorseScreenAccess screen;
	private int vanillaWidth = -1;
	private int vanillaHeight = -1;
	private ViewToggle toggle;

	private boolean open;
	private HorseProfile profile;
	private Object profileKey;
	private int ticksUntilRefresh;
	/** How many pixels the gene table is scrolled down. */
	private int scroll;

	public HorseInfoController(HorseScreenAccess screen) {
		this.screen = screen;
	}

	public boolean isOpen() {
		return open;
	}

	/** Whether this mount is a horse with an info view. Llamas, camels and other mounts never are. */
	private boolean available() {
		return screen.horseloyalty$horse() != null && HorseProfiles.available(screen.horseloyalty$horse());
	}

	/** Called at the start of the screen's init, before the window is centred: picks the window size. */
	public void beforeInit() {
		if (vanillaWidth < 0) {
			vanillaWidth = screen.horseloyalty$windowWidth();
			vanillaHeight = screen.horseloyalty$windowHeight();
		}
		if (open && !available()) {
			open = false;
		}
		if (open && profile != null) {
			// As wide as fits; as tall as the content needs, up to what fits (then the genes scroll).
			int width = InfoLayout.infoWidth(screen.horseloyalty$screenWidth());
			int height = Math.min(InfoLayout.infoHeight(screen.horseloyalty$screenHeight()),
					Math.max(InfoLayout.VANILLA_HEIGHT, HorseInfoPanel.neededHeight(profile, width)));
			screen.horseloyalty$setWindowSize(width, height);
		} else {
			screen.horseloyalty$setWindowSize(vanillaWidth, vanillaHeight);
		}
	}

	/** Called at the end of the screen's init: adds the tabs when this horse has an info view. */
	public void afterInit() {
		toggle = null;
		if (!available()) {
			return;
		}
		toggle = new ViewToggle(screen.horseloyalty$left(), screen.horseloyalty$top(), screen.horseloyalty$windowWidth(),
				open, this::setOpen);
		toggle.setCarrying(screen.horseloyalty$carrying());
		screen.horseloyalty$addButton(toggle.inventoryTab());
		screen.horseloyalty$addButton(toggle.infoTab());
	}

	/** Switches view; the window is resized and laid out again. Opening builds a fresh profile, scrolled to the top. */
	private void setOpen(boolean wantOpen) {
		Player player = Minecraft.getInstance().player;
		if (wantOpen == open || player == null || (wantOpen && screen.horseloyalty$carrying())) {
			return;
		}
		open = wantOpen;
		if (open) {
			scroll = 0;
			rebuildProfile(player);
		}
		screen.horseloyalty$rebuild();
	}

	/** Called every screen tick: follows the tabs' carrying rule and refreshes the profile when the horse changed. */
	public void tick() {
		Player player = Minecraft.getInstance().player;
		if (toggle == null || player == null) {
			return;
		}
		if (!available()) {
			// Genetics was turned off, or the horse lost its genes: back to the plain inventory.
			open = false;
			screen.horseloyalty$rebuild();
			return;
		}
		toggle.setCarrying(screen.horseloyalty$carrying());
		if (open && --ticksUntilRefresh <= 0) {
			ticksUntilRefresh = REFRESH_TICKS;
			if (!Objects.equals(profileKey, HorseProfiles.changeKey(screen.horseloyalty$horse(), player))) {
				rebuildProfile(player);
			}
		}
	}

	private void rebuildProfile(Player player) {
		profileKey = HorseProfiles.changeKey(screen.horseloyalty$horse(), player);
		profile = HorseProfiles.of(screen.horseloyalty$horse(), player, RevealPolicies.everything());
		ticksUntilRefresh = REFRESH_TICKS;
	}

	/** Scrolls the gene table when the wheel turns over it. Returns whether it did. */
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!open || profile == null) {
			return false;
		}
		int width = screen.horseloyalty$windowWidth();
		int height = screen.horseloyalty$windowHeight();
		Rect area = HorseInfoPanel.scrollArea(profile, width, height);
		if (!area.contains(mouseX - screen.horseloyalty$left(), mouseY - screen.horseloyalty$top())) {
			return false;
		}
		scrollBy((int) Math.round(-amount * SCROLL_STEP), HorseInfoPanel.maxScroll(profile, width, height));
		return true;
	}

	private void scrollBy(int pixels, int maxScroll) {
		scroll = Math.clamp(scroll + pixels, 0, Math.max(0, maxScroll));
	}

	/** Draws the info view in place of the inventory. Returns false when it isn't showing, so vanilla draws. */
	public boolean draw(GuiGraphicsExtractor graphics, Font font, Component title, int mouseX, int mouseY) {
		if (!open || profile == null) {
			return false;
		}
		int width = screen.horseloyalty$windowWidth();
		int height = screen.horseloyalty$windowHeight();
		// The window may have shrunk since the last scroll (a resized game window).
		scrollBy(0, HorseInfoPanel.maxScroll(profile, width, height));
		HorseInfoPanel.draw(graphics, font, screen.horseloyalty$horse(), profile, title, screen.horseloyalty$left(),
				screen.horseloyalty$top(), width, height, mouseX, mouseY, scroll);
		return true;
	}
}
