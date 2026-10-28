package com.xziggsx.horseloyalty.riding.client.mixin;

import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.riding.RidingFeature;
import com.xziggsx.horseloyalty.riding.client.RideControl;
import com.xziggsx.horseloyalty.riding.client.RideHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands a ridden horse's controls to its {@link RideControl}. Vanilla only calls getRiddenInput,
 * getRiddenSpeed and getRiddenRotation on the rider's own client, which is why riding lives in the
 * client source set. Each hook only forwards, and only while riding is on (on a client, the server's
 * switch); while it's off the horse rides as in vanilla.
 */
@Mixin(AbstractHorse.class)
public abstract class HorseRidingMixin implements RideHolder {
	@Unique
	private final RideControl horseloyalty$ride = new RideControl();

	@Override
	public RideControl horseloyalty$ride() {
		return horseloyalty$ride;
	}

	@Unique
	private boolean horseloyalty$controls() {
		return Features.isOn(RidingFeature.ID) && HorseScope.inScope((AbstractHorse) (Object) this);
	}

	/** Starts every ride at a halt, with no turn in progress. */
	@Inject(method = "addPassenger", at = @At("TAIL"))
	private void horseloyalty$resetControls(Entity passenger, CallbackInfo ci) {
		horseloyalty$ride.reset();
	}

	@Inject(method = "getRiddenInput", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$gaitInput(Player player, Vec3 travelVector, CallbackInfoReturnable<Vec3> cir) {
		if (horseloyalty$controls()) {
			cir.setReturnValue(horseloyalty$ride.input((AbstractHorse) (Object) this, player, cir.getReturnValue()));
		}
	}

	@Inject(method = "getRiddenSpeed", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$gaitSpeed(Player player, CallbackInfoReturnable<Float> cir) {
		if (horseloyalty$controls()) {
			Float speed = horseloyalty$ride.speed((AbstractHorse) (Object) this, player, cir.getReturnValueF());
			if (speed != null) {
				cir.setReturnValue(speed);
			}
		}
	}

	@Inject(method = "getRiddenRotation", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$boatSteering(LivingEntity rider, CallbackInfoReturnable<Vec2> cir) {
		if (horseloyalty$controls()) {
			cir.setReturnValue(horseloyalty$ride.rotation((AbstractHorse) (Object) this, rider));
		}
	}

	/**
	 * Turns the rider's view with the horse. This has to happen here and not in getRiddenRotation:
	 * the horse ticks before its rider, and the rider then saves its "previous rotation" for frame
	 * interpolation. Turning the rider before that save makes the camera jump a step every tick (the
	 * stutter). Here, during the rider's own tick, the turn is interpolated smoothly across frames,
	 * the same way boats do it.
	 */
	@Inject(method = "positionRider", at = @At("TAIL"))
	private void horseloyalty$turnRider(Entity passenger, Entity.MoveFunction moveFunction, CallbackInfo ci) {
		if (passenger != ((AbstractHorse) (Object) this).getControllingPassenger()) {
			return;
		}
		float turn = horseloyalty$ride.takeRiderTurn();
		if (turn != 0) {
			passenger.setYRot(passenger.getYRot() + turn);
			passenger.setYHeadRot(passenger.getYHeadRot() + turn);
		}
	}
}
