package com.example.horseloyalty.mixin;

import com.example.horseloyalty.loyalty.Hunger;
import com.example.horseloyalty.loyalty.Loyalty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side horse hooks: loyalty from feeding and riding, hunger, and the loyalty
 * status shown on mounting. Every hook here returns straight away on the client.
 * <p>
 * The riding controls (gaits, steering) run on the rider's client instead, see
 * {@code mixin/client/AbstractHorseRidingMixin} in the client source set.
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {
	@Shadow
	public abstract boolean isTamed();

	@Shadow
	public abstract boolean isFood(ItemStack stack);

	@Shadow
	private void eating() {
	}

	/** Horse position last tick, to tell whether a ridden horse is actually moving. */
	@Unique
	private double horseloyalty$lastX = Double.NaN;

	@Unique
	private double horseloyalty$lastZ = Double.NaN;

	@Unique
	private AbstractHorse horseloyalty$self() {
		return (AbstractHorse) (Object) this;
	}

	/**
	 * Counts a feed for whoever fed the horse, and marks it as fed for hunger.
	 * A tamed horse at full health normally refuses food, so it also accepts a treat
	 * whenever that player's feed cooldown has passed, or any time it's hungry.
	 */
	@Inject(method = "handleEating", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$countFeed(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		AbstractHorse horse = horseloyalty$self();
		if (horse.level().isClientSide() || !Loyalty.appliesTo(horse) || !isTamed()) {
			return;
		}
		boolean countsForLoyalty = Loyalty.canCountFeed(horse, player);
		boolean ate = cir.getReturnValueZ();
		if (!ate && isFood(stack) && (countsForLoyalty || Hunger.isHungry(horse))) {
			eating();
			cir.setReturnValue(true);
			ate = true;
		}
		if (ate) {
			Hunger.feed(horse);
			if (countsForLoyalty) {
				Loyalty.recordFeed(horse, player);
			}
		}
	}

	/** Runs hunger, and counts riding time for whoever is in the saddle while the horse is moving. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void horseloyalty$countRide(CallbackInfo ci) {
		AbstractHorse horse = horseloyalty$self();
		if (!(horse.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		Hunger.tick(horse, serverLevel);

		double dx = horse.getX() - horseloyalty$lastX;
		double dz = horse.getZ() - horseloyalty$lastZ;
		horseloyalty$lastX = horse.getX();
		horseloyalty$lastZ = horse.getZ();

		if (!isTamed() || !Loyalty.appliesTo(horse)) {
			return;
		}
		LivingEntity rider = horse.getControllingPassenger();
		if (rider instanceof Player player && dx * dx + dz * dz >= Loyalty.MIN_RIDE_MOVE_SQR) {
			Loyalty.recordRideTick(horse, player);
		}
	}

	/** Shows the rider's own loyalty status above the hotbar when they mount. */
	@Inject(method = "addPassenger", at = @At("TAIL"))
	private void horseloyalty$showStatus(Entity passenger, CallbackInfo ci) {
		AbstractHorse horse = horseloyalty$self();
		if (passenger instanceof ServerPlayer player && isTamed() && Loyalty.appliesTo(horse)) {
			player.sendOverlayMessage(Loyalty.describe(horse, player));
		}
	}
}
