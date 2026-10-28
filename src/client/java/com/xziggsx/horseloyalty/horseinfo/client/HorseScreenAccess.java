package com.xziggsx.horseloyalty.horseinfo.client;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * What {@link HorseInfoController} needs from the horse screen, which keeps these behind
 * protected fields. Implemented by the screen's mixin, one line each.
 */
public interface HorseScreenAccess {
	/** The horse, or null for a mount that isn't one. */
	AbstractHorse horseloyalty$horse();

	/** The window's left and top edge on screen, and its size. */
	int horseloyalty$left();

	int horseloyalty$top();

	int horseloyalty$windowWidth();

	int horseloyalty$windowHeight();

	/** The whole screen's size, in GUI pixels. */
	int horseloyalty$screenWidth();

	int horseloyalty$screenHeight();

	/** Sets the window size; takes effect at the next {@link #horseloyalty$rebuild()}. */
	void horseloyalty$setWindowSize(int width, int height);

	/** Lays the screen out again: re-centres the window and re-adds its widgets. */
	void horseloyalty$rebuild();

	void horseloyalty$addButton(AbstractButton button);

	/** Whether an item is on the cursor. */
	boolean horseloyalty$carrying();

	/** Whether the info view is showing; container-screen hooks use this to block the slots. */
	boolean horseloyalty$infoOpen();
}
