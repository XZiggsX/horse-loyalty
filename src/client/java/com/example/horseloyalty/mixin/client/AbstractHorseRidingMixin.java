package com.example.horseloyalty.mixin.client;

import com.example.horseloyalty.client.riding.Gait;
import com.example.horseloyalty.client.riding.GaitHolder;
import com.example.horseloyalty.client.riding.Riding;
import com.example.horseloyalty.loyalty.Loyalty;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side riding controls: gaits (W/S taps), gait speeds, boat-style A/D steering
 * and random gait drops.
 * <p>
 * Minecraft moves a player-ridden horse on that player's own client and sends the
 * result to the server, so vanilla only calls getRiddenInput, getRiddenSpeed and
 * getRiddenRotation on the rider's client. That's why all of this lives in the client
 * source set and is never loaded on a dedicated server.
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseRidingMixin implements GaitHolder {
	@Shadow
	protected abstract SoundEvent getAngrySound();

	/** Current gait. */
	@Unique
	private Gait horseloyalty$gait = Gait.HALT;

	/** Rider's forward input last tick, to detect W and S taps. */
	@Unique
	private float horseloyalty$lastForward;

	/** Current turn rate in degrees per tick, eased towards the A/D target. */
	@Unique
	private float horseloyalty$turnSpeed;

	/** Turn applied to the horse this tick, still to be applied to the rider's view. */
	@Unique
	private float horseloyalty$pendingRiderTurn;

	/** Ticks spent above a halt since the rider last shifted gait or the last drop roll. */
	@Unique
	private int horseloyalty$gaitDropTimer;

	@Override
	public Gait horseloyalty$getGait() {
		return horseloyalty$gait;
	}

	@Override
	public int horseloyalty$getGaitDropTimer() {
		return horseloyalty$gaitDropTimer;
	}

	@Unique
	private AbstractHorse horseloyalty$self() {
		return (AbstractHorse) (Object) this;
	}

	/** Starts every ride at a halt, with no turn in progress. */
	@Inject(method = "addPassenger", at = @At("TAIL"))
	private void horseloyalty$resetControls(Entity passenger, CallbackInfo ci) {
		horseloyalty$gait = Gait.HALT;
		horseloyalty$lastForward = 0;
		horseloyalty$turnSpeed = 0;
		horseloyalty$pendingRiderTurn = 0;
		horseloyalty$gaitDropTimer = 0;
	}

	/**
	 * Gait control: W taps shift up, S taps shift down, and the horse keeps going at
	 * its gait without holding a key. A and D steer (see getRiddenRotation), so they
	 * no longer strafe. Also rolls the random gait drop.
	 */
	@Inject(method = "getRiddenInput", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$gaitInput(Player player, Vec3 travelVector, CallbackInfoReturnable<Vec3> cir) {
		AbstractHorse horse = horseloyalty$self();
		if (!Loyalty.appliesTo(horse)) {
			return;
		}
		float forward = player.zza;
		Gait before = horseloyalty$gait;
		if (forward > 0 && horseloyalty$lastForward <= 0) {
			horseloyalty$gait = horseloyalty$gait.up();
		} else if (forward < 0 && horseloyalty$lastForward >= 0) {
			horseloyalty$gait = horseloyalty$gait.down();
		}
		horseloyalty$lastForward = forward;
		if (horseloyalty$gait != before) {
			// Only the rider's own client runs this, so this shows on their screen only.
			player.sendOverlayMessage(horseloyalty$gait.displayName());
			// Shifting gait shows the rider is paying attention, so the drop timer restarts.
			horseloyalty$gaitDropTimer = 0;
		} else if (horseloyalty$gait != Gait.HALT && ++horseloyalty$gaitDropTimer >= Riding.gaitDropTicks(horse, player)) {
			// Less loyal horses lose interest sooner and drop a gait on their own, which
			// also stops riders from holding nothing and crossing the map AFK. With a 100%
			// chance (the level 5 default) this is a fixed timer.
			horseloyalty$gaitDropTimer = 0;
			if (horse.getRandom().nextDouble() < Riding.gaitDropChance(horse, player)) {
				horseloyalty$gait = horseloyalty$gait.down();
				player.sendOverlayMessage(Component.translatable("horseloyalty.gait.dropped",
						horseloyalty$gait.displayName()));
				// The vanilla bucking/angry sound, played locally: only the rider hears it for now.
				SoundEvent angry = getAngrySound();
				if (angry != null) {
					horse.level().playLocalSound(horse, angry, horse.getSoundSource(), 1.0F, horse.getVoicePitch());
				}
			}
		}

		// Vanilla returns the Vec3.ZERO constant itself while the horse is rearing. Keep that.
		if (cir.getReturnValue() == Vec3.ZERO) {
			return;
		}
		cir.setReturnValue(new Vec3(0, 0, horseloyalty$gait == Gait.HALT ? 0 : 1));
	}

	/** Replaces the horse's speed with the current gait's speed, capped by the rider's loyalty. */
	@Inject(method = "getRiddenSpeed", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$gaitSpeed(Player player, CallbackInfoReturnable<Float> cir) {
		if (Loyalty.appliesTo(horseloyalty$self()) && horseloyalty$gait != Gait.HALT) {
			cir.setReturnValue((float) Riding.gaitSpeed(horseloyalty$gait, cir.getReturnValueF(), horseloyalty$self(), player));
		}
	}

	/**
	 * Boat-style steering. Vanilla points the horse wherever the rider looks; instead
	 * A and D turn it, easing up to a fixed rate set by the rider's loyalty, and the
	 * rider's view turns along with it (see positionRider). The rider can still look
	 * around freely with the mouse.
	 */
	@Inject(method = "getRiddenRotation", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$boatSteering(LivingEntity rider, CallbackInfoReturnable<Vec2> cir) {
		AbstractHorse horse = horseloyalty$self();
		if (!Loyalty.appliesTo(horse)) {
			return;
		}
		// Turn rate depends on loyalty. Only players steer horses, see getControllingPassenger.
		float maxRate = rider instanceof Player player ? Riding.turnRate(horse, player) : Riding.defaultTurnRate();
		float ease = Riding.turnEase(maxRate);
		// xxa is positive while holding A (left). Turning left lowers yaw.
		float target = Math.signum(rider.xxa) * -maxRate;
		float change = Math.clamp(target - horseloyalty$turnSpeed, -ease, ease);
		horseloyalty$turnSpeed += change;
		horseloyalty$pendingRiderTurn += horseloyalty$turnSpeed;
		cir.setReturnValue(new Vec2(rider.getXRot() * 0.5F, horse.getYRot() + horseloyalty$turnSpeed));
	}

	/**
	 * Turns the rider's view with the horse. This has to happen here and not in
	 * getRiddenRotation: the horse ticks before its rider, and the rider then saves its
	 * "previous rotation" for frame interpolation. Turning the rider before that save
	 * makes the camera jump a step every tick (the stutter). Here, during the rider's own
	 * tick, the turn is interpolated smoothly across frames, the same way boats do it.
	 */
	@Inject(method = "positionRider", at = @At("TAIL"))
	private void horseloyalty$turnRider(Entity passenger, Entity.MoveFunction moveFunction, CallbackInfo ci) {
		AbstractHorse horse = horseloyalty$self();
		if (horseloyalty$pendingRiderTurn != 0 && passenger == horse.getControllingPassenger()) {
			passenger.setYRot(passenger.getYRot() + horseloyalty$pendingRiderTurn);
			passenger.setYHeadRot(passenger.getYHeadRot() + horseloyalty$pendingRiderTurn);
			horseloyalty$pendingRiderTurn = 0;
		}
	}
}
