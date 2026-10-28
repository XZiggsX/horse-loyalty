package com.xziggsx.horseloyalty.client.horseinfo.section;

import com.xziggsx.horseloyalty.client.horseinfo.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile;

/**
 * One part of the horse info screen. Sections only read the {@link HorseProfile}, never the horse,
 * and know nothing of each other; {@code HorseInfoPanel} decides where each goes.
 */
public interface InfoSection {
	/** How tall this section is for this profile, in GUI pixels. */
	int height(HorseProfile profile);

	/** Draws the section into its box (relative to the window). */
	void draw(InfoContext context, HorseProfile profile, Rect area);
}
