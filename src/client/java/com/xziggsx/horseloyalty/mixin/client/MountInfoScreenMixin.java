package com.xziggsx.horseloyalty.mixin.client;

import com.xziggsx.horseloyalty.client.horseinfo.HorseInfoController;
import com.xziggsx.horseloyalty.client.horseinfo.HorseScreenAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the horse info view to the horse inventory screen. Only forwards to
 * {@link HorseInfoController}, which does the work: the tabs, switching views, the profile,
 * scrolling and drawing. The loyalty text of {@link AbstractMountInventoryScreenMixin} is
 * skipped in the info view, since its hook comes after the one that draws the view.
 */
@Mixin(AbstractMountInventoryScreen.class)
public abstract class MountInfoScreenMixin extends AbstractContainerScreen<AbstractMountInventoryMenu>
		implements HorseScreenAccess {
	@Shadow
	@Final
	protected LivingEntity mount;

	@Unique
	private final HorseInfoController horseloyalty$info = new HorseInfoController(this);

	private MountInfoScreenMixin(AbstractMountInventoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		horseloyalty$info.beforeInit();
		super.init();
		horseloyalty$info.afterInit();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		horseloyalty$info.tick();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return horseloyalty$info.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** In the info view, draws it instead of the slots, chest and saddle backgrounds and the usual horse model. */
	@Inject(method = "extractBackground", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
	private void horseloyalty$drawInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (horseloyalty$info.draw(graphics, font, title, mouseX, mouseY)) {
			ci.cancel();
		}
	}

	@Override
	public boolean horseloyalty$infoOpen() {
		return horseloyalty$info.isOpen();
	}

	@Override
	public AbstractHorse horseloyalty$horse() {
		return mount instanceof AbstractHorse horse ? horse : null;
	}

	@Override
	public int horseloyalty$left() {
		return leftPos;
	}

	@Override
	public int horseloyalty$top() {
		return topPos;
	}

	@Override
	public int horseloyalty$windowWidth() {
		return imageWidth;
	}

	@Override
	public int horseloyalty$windowHeight() {
		return imageHeight;
	}

	@Override
	public int horseloyalty$screenWidth() {
		return width;
	}

	@Override
	public int horseloyalty$screenHeight() {
		return height;
	}

	@Override
	public void horseloyalty$setWindowSize(int windowWidth, int windowHeight) {
		ContainerScreenAccessor accessor = (ContainerScreenAccessor) this;
		accessor.horseloyalty$setImageWidth(windowWidth);
		accessor.horseloyalty$setImageHeight(windowHeight);
	}

	@Override
	public void horseloyalty$rebuild() {
		rebuildWidgets();
	}

	@Override
	public void horseloyalty$addButton(AbstractButton button) {
		addRenderableWidget(button);
	}

	@Override
	public boolean horseloyalty$carrying() {
		return !menu.getCarried().isEmpty();
	}
}
