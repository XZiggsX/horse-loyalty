package com.xziggsx.horseloyalty.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the horse screen change its window size when switching to the info view. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Mutable
	@Accessor("imageWidth")
	void horseloyalty$setImageWidth(int width);

	@Mutable
	@Accessor("imageHeight")
	void horseloyalty$setImageHeight(int height);
}
