package com.xziggsx.horseloyalty.client.horseinfo;

import com.xziggsx.horseloyalty.client.horseinfo.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.HorseProfiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The horse info feature on one open horse screen: adds the tabs, switches between the
 * inventory and the info view (changing the window size), keeps the profile fresh, scrolls the
 * gene table and draws the view. The screen's mixin only forwards to this.
 * <p>
 * Nothing shows when genetics is off (the server's setting) or the horse has no genes yet.
 */
public final class HorseInfoController {
	/** How far one mouse wheel notch scrolls the gene table. */
	private static final int SCROLL_STEP = 11;

	private final HorseScreenAccess screen;
	private final HorseInfoView view = new HorseInfoView();
	private int vanillaWidth = -1;
	private int vanillaHeight = -1;
	private ViewToggle toggle;

	public HorseInfoController(HorseScreenAccess screen) {
		this.screen = screen;
	}

	public boolean isOpen() {
		return view.isOpen();
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
		if (view.isOpen() && !available()) {
			view.close();
		}
		if (view.isOpen() && view.profile() != null) {
			// As wide as fits; as tall as the content needs, up to what fits (then the genes scroll).
			int width = InfoLayout.infoWidth(screen.horseloyalty$screenWidth());
			int height = Math.min(InfoLayout.infoHeight(screen.horseloyalty$screenHeight()),
					Math.max(InfoLayout.VANILLA_HEIGHT, HorseInfoPanel.neededHeight(view.profile(), width)));
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
				view.isOpen(), this::setOpen);
		toggle.setCarrying(screen.horseloyalty$carrying());
		screen.horseloyalty$addButton(toggle.inventoryTab());
		screen.horseloyalty$addButton(toggle.infoTab());
	}

	/** Switches view; the window is resized and laid out again. */
	private void setOpen(boolean open) {
		Player player = Minecraft.getInstance().player;
		if (open == view.isOpen() || player == null || (open && screen.horseloyalty$carrying())) {
			return;
		}
		if (open) {
			view.open(screen.horseloyalty$horse(), player);
		} else {
			view.close();
		}
		screen.horseloyalty$rebuild();
	}

	/** Called every screen tick. */
	public void tick() {
		Player player = Minecraft.getInstance().player;
		if (toggle == null || player == null) {
			return;
		}
		if (!available()) {
			// Genetics was turned off, or the horse lost its genes: back to the plain inventory.
			view.close();
			screen.horseloyalty$rebuild();
			return;
		}
		toggle.setCarrying(screen.horseloyalty$carrying());
		view.tick(screen.horseloyalty$horse(), player);
	}

	/** Scrolls the gene table when the wheel turns over it. Returns whether it did. */
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		HorseProfile profile = view.profile();
		if (!view.isOpen() || profile == null) {
			return false;
		}
		int width = screen.horseloyalty$windowWidth();
		int height = screen.horseloyalty$windowHeight();
		Rect area = HorseInfoPanel.scrollArea(profile, width, height);
		if (!area.contains(mouseX - screen.horseloyalty$left(), mouseY - screen.horseloyalty$top())) {
			return false;
		}
		view.scrollBy((int) Math.round(-amount * SCROLL_STEP), HorseInfoPanel.maxScroll(profile, width, height));
		return true;
	}

	/** Draws the info view in place of the inventory. Returns false when it isn't showing, so vanilla draws. */
	public boolean draw(GuiGraphicsExtractor graphics, Font font, Component title, int mouseX, int mouseY) {
		HorseProfile profile = view.profile();
		if (!view.isOpen() || profile == null) {
			return false;
		}
		int width = screen.horseloyalty$windowWidth();
		int height = screen.horseloyalty$windowHeight();
		// The window may have shrunk since the last scroll (a resized game window).
		view.scrollBy(0, HorseInfoPanel.maxScroll(profile, width, height));
		HorseInfoPanel.draw(graphics, font, screen.horseloyalty$horse(), profile, title, screen.horseloyalty$left(),
				screen.horseloyalty$top(), width, height, mouseX, mouseY, view.scroll());
		return true;
	}
}
