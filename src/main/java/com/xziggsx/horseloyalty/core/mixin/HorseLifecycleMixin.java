package com.xziggsx.horseloyalty.core.mixin;

import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.Treats;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
 * Fires the {@link HorseEvents} of a horse's life: ticks, treats, eating, mounting and its goals.
 * Holds no game logic; every feature listens to the events instead.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseLifecycleMixin {
	@Shadow
	public abstract boolean isTamed();

	@Shadow
	public abstract boolean isFood(ItemStack stack);

	@Shadow
	private void eating() {
	}

	@Unique
	private AbstractHorse horseloyalty$self() {
		return (AbstractHorse) (Object) this;
	}

	/**
	 * A tamed horse at full health normally refuses food, so it also eats (its own food, or anything in
	 * {@link Treats}) when a listener wants it to (see {@link HorseEvents#WANTS_TREAT}).
	 * Whenever it ate, {@link HorseEvents#ATE} fires; when it still refused, {@link HorseEvents#REFUSED}.
	 */
	@Inject(method = "handleEating", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$treat(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		AbstractHorse horse = horseloyalty$self();
		if (horse.level().isClientSide() || !HorseScope.inScope(horse) || !isTamed()) {
			return;
		}
		boolean ate = cir.getReturnValueZ();
		if (!ate && (isFood(stack) || Treats.isTreat(stack))) {
			if (HorseEvents.WANTS_TREAT.invoker().wantsTreat(horse, player, stack)) {
				eating();
				cir.setReturnValue(true);
				ate = true;
			} else {
				HorseEvents.REFUSED.invoker().refused(horse, player, stack);
			}
		}
		if (ate) {
			HorseEvents.ATE.invoker().ate(horse, player, stack);
		}
	}

	/**
	 * Vanilla only offers a horse its own food. A {@link Treats treat} that isn't its food goes the
	 * same way, through vanilla's feeding (which uses up the item if it ate).
	 */
	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$offerTreat(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		AbstractHorse horse = horseloyalty$self();
		ItemStack stack = player.getItemInHand(hand);
		if (isFood(stack) || !Treats.isTreat(stack) || !isTamed() || player.isSecondaryUseActive() || !HorseScope.inScope(horse)) {
			return;
		}
		cir.setReturnValue(horse.level().isClientSide() ? InteractionResult.SUCCESS : horse.fedFood(player, stack));
	}

	/** Every server tick, and once a second spread out by entity id, for horses in scope. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void horseloyalty$tick(CallbackInfo ci) {
		AbstractHorse horse = horseloyalty$self();
		if (!(horse.level() instanceof ServerLevel level) || !HorseScope.inScope(horse)) {
			return;
		}
		HorseEvents.SERVER_TICK.invoker().tick(horse, level);
		if ((horse.tickCount + horse.getId()) % 20 == 0) {
			HorseEvents.SERVER_SECOND.invoker().tick(horse, level);
		}
	}

	/** A player mounted a tamed horse in scope. */
	@Inject(method = "addPassenger", at = @At("TAIL"))
	private void horseloyalty$mounted(Entity passenger, CallbackInfo ci) {
		AbstractHorse horse = horseloyalty$self();
		if (passenger instanceof ServerPlayer player && isTamed() && HorseScope.inScope(horse)) {
			HorseEvents.MOUNTED.invoker().mounted(horse, player);
		}
	}

	/** Lets features add their goals, after vanilla's. */
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void horseloyalty$addGoals(CallbackInfo ci) {
		AbstractHorse horse = horseloyalty$self();
		HorseEvents.REGISTER_GOALS.invoker().addGoals(horse, ((MobAccessor) horse).horseloyalty$getGoalSelector());
	}
}
