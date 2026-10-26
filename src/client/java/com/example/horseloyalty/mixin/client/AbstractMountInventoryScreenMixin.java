package com.example.horseloyalty.mixin.client;

import com.example.horseloyalty.loyalty.Loyalty;
import com.example.horseloyalty.personality.Personality;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds your own loyalty level with the horse, and its name, to the horse inventory screen, plus the
 * horse's personality once you know it well enough (see {@link Personality#isRevealedTo}). With no chest
 * it goes in the empty panel right of the horse preview; with a chest it goes on
 * the title row instead, so it never covers the chest slots.
 */
@Mixin(AbstractMountInventoryScreen.class)
public abstract class AbstractMountInventoryScreenMixin extends AbstractContainerScreen<AbstractMountInventoryMenu> {
	private static final int LABEL_COLOR = 0xFF404040;

	@Shadow
	@Final
	protected LivingEntity mount;

	@Shadow
	@Final
	protected int inventoryColumns;

	private AbstractMountInventoryScreenMixin(AbstractMountInventoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void horseloyalty$drawLoyalty(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Player player = Minecraft.getInstance().player;
		if (!(mount instanceof AbstractHorse horse) || !Loyalty.appliesTo(horse) || player == null) {
			return;
		}
		int level = Loyalty.level(horse, player);
		Component score = Component.translatable("horseloyalty.loyalty.screen", level, Loyalty.MAX_LEVEL);
		Component name = Loyalty.levelName(level);
		Component personality = Personality.isRevealedTo(horse, player)
				? Personality.of(horse).displayName() : null;

		if (inventoryColumns == 0) {
			int x = leftPos + 82;
			int y = topPos + 22;
			graphics.text(font, score, x, y, LABEL_COLOR, false);
			graphics.text(font, name, x, y + 11, LABEL_COLOR, false);
			if (personality != null) {
				graphics.text(font, personality, x, y + 22, LABEL_COLOR, false);
			}
		} else {
			Component line = Component.empty().append(score).append(" ").append(name);
			if (personality != null) {
				line = Component.empty().append(line).append(", ").append(personality);
			}
			int x = leftPos + imageWidth - 8 - font.width(line);
			graphics.text(font, line, x, topPos + 6, LABEL_COLOR, false);
		}
	}
}
