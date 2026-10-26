package com.example.horseloyalty.mixin.client;

import com.example.horseloyalty.client.riding.Gait;
import com.example.horseloyalty.client.riding.GaitHolder;
import com.example.horseloyalty.client.riding.Riding;
import com.example.horseloyalty.config.HorseLoyaltyConfig;
import com.example.horseloyalty.loyalty.Loyalty;
import com.example.horseloyalty.personality.Personality;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
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
 * Client-side riding controls: gaits (W/S taps), backing up, gait speeds, boat-style A/D steering,
 * sidestepping (sprint key + A/D),
 * random gait drops, and skittish horses bolting from creepers.
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

	/** Whether the rider is holding the sprint key with A or D: move diagonally instead of turning. */
	@Unique
	private boolean horseloyalty$sidestepping;

	/** Whether the rider is holding S at a halt to back the horse up. */
	@Unique
	private boolean horseloyalty$reversing;

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

	/** Ticks left of a skittish bolt. While above 0 the horse gallops and steers itself. */
	@Unique
	private int horseloyalty$boltTicks;

	/** Ticks until the next creeper check; also the calm-down time after a bolt. */
	@Unique
	private int horseloyalty$boltCheckTimer;

	/** Which way a bolting horse veers: -1 left, 0 straight, 1 right. */
	@Unique
	private int horseloyalty$boltTurn;

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
		horseloyalty$reversing = false;
		horseloyalty$sidestepping = false;
		horseloyalty$boltTicks = 0;
		horseloyalty$boltCheckTimer = 0;
		horseloyalty$boltTurn = 0;
	}

	/**
	 * Gait control: W taps shift up, S taps shift down, and the horse keeps going at
	 * its gait without holding a key. Pressing S again at a halt and holding it backs up. A and D steer (see getRiddenRotation), so they
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
		boolean bolting = horseloyalty$updateBolt(horse, player);
		if (bolting) {
			// The rider has no say until the bolt is over.
			horseloyalty$gait = Gait.GALLOP;
			horseloyalty$reversing = false;
		} else if (forward > 0 && horseloyalty$lastForward <= 0) {
			horseloyalty$gait = horseloyalty$gait.up();
		} else if (forward < 0 && horseloyalty$lastForward >= 0) {
			if (horseloyalty$gait == Gait.HALT) {
				// A fresh S press while already halted: back up for as long as it's held.
				horseloyalty$reversing = true;
				player.sendOverlayMessage(Component.translatable("horseloyalty.gait.reverse"));
			} else {
				horseloyalty$gait = horseloyalty$gait.down();
			}
		}
		if (forward >= 0) {
			horseloyalty$reversing = false;
		}
		// Sprint key (Ctrl by default) + A/D: the horse keeps facing the same way and moves at an
		// angle instead. Halted, it steps straight sideways. A bolting horse ignores the rider.
		horseloyalty$sidestepping = !bolting && player.xxa != 0
				&& Minecraft.getInstance().options.keySprint.isDown();
		horseloyalty$lastForward = forward;
		// While bolting there are no gait messages or drops; updateBolt sends its own messages.
		if (bolting) {
			horseloyalty$gaitDropTimer = 0;
		} else if (horseloyalty$gait != before) {
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
				horseloyalty$playAngrySound(horse);
			}
		}

		// Vanilla returns the Vec3.ZERO constant itself while the horse is rearing. Keep that.
		if (cir.getReturnValue() == Vec3.ZERO) {
			return;
		}
		double forwardInput = horseloyalty$reversing ? -1 : horseloyalty$gait == Gait.HALT ? 0 : 1;
		double sideInput = 0;
		if (horseloyalty$sidestepping) {
			// xxa is positive for A, and a positive x input moves left, so the signs match.
			// Moving, the angle sets the sideways share; halted, it's a pure sidestep.
			double side = forwardInput == 0 ? 1 : Math.tan(Math.toRadians(HorseLoyaltyConfig.get().sidestepAngleDegrees()));
			sideInput = Math.signum(player.xxa) * side;
		}
		// Vanilla scales an input longer than 1 back down to 1, so moving diagonally keeps the gait's speed.
		cir.setReturnValue(new Vec3(sideInput, 0, forwardInput));
	}

	/**
	 * Runs a skittish horse's bolt: counts down a bolt in progress, or every half second looks
	 * for a creeper close enough to start one. Returns whether the horse is bolting this tick.
	 */
	@Unique
	private boolean horseloyalty$updateBolt(AbstractHorse horse, Player player) {
		if (horseloyalty$boltTicks > 0) {
			horseloyalty$boltTicks--;
			if (horseloyalty$boltTicks % 10 == 0) {
				horseloyalty$boltTurn = horse.getRandom().nextInt(3) - 1;
			}
			if (horseloyalty$boltTicks == 0) {
				horseloyalty$boltTurn = 0;
				player.sendOverlayMessage(Component.translatable("horseloyalty.personality.bolt_end", horse.getName()));
			}
			return true;
		}
		if (Personality.of(horse) != Personality.SKITTISH || --horseloyalty$boltCheckTimer > 0) {
			return false;
		}
		horseloyalty$boltCheckTimer = 10;
		int ticks = Riding.boltTicks(horse, player);
		double distance = HorseLoyaltyConfig.get().boltCreeperDistance();
		if (ticks <= 0 || horse.level().getEntitiesOfClass(Creeper.class,
				horse.getBoundingBox().inflate(distance), Entity::isAlive).isEmpty()) {
			return false;
		}
		horseloyalty$boltTicks = ticks;
		// Five seconds to calm down after the bolt before it can bolt again.
		horseloyalty$boltCheckTimer = 100;
		horseloyalty$boltTurn = horse.getRandom().nextInt(3) - 1;
		player.sendOverlayMessage(Component.translatable("horseloyalty.personality.bolt", horse.getName()));
		horseloyalty$playAngrySound(horse);
		return true;
	}

	/** The vanilla bucking/angry sound, played locally: only the rider hears it for now. */
	@Unique
	private void horseloyalty$playAngrySound(AbstractHorse horse) {
		SoundEvent angry = getAngrySound();
		if (angry != null) {
			horse.level().playLocalSound(horse, angry, horse.getSoundSource(), 1.0F, horse.getVoicePitch());
		}
	}

	/** Replaces the horse's speed with the current gait's speed, scaled by the rider's loyalty. */
	@Inject(method = "getRiddenSpeed", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$gaitSpeed(Player player, CallbackInfoReturnable<Float> cir) {
		if (!Loyalty.appliesTo(horseloyalty$self())) {
			return;
		}
		if (horseloyalty$reversing) {
			cir.setReturnValue((float) Gait.REVERSE_SPEED);
		} else if (horseloyalty$sidestepping && horseloyalty$gait == Gait.HALT) {
			// Stepping sideways from a halt goes at walking pace.
			cir.setReturnValue((float) Riding.gaitSpeed(Gait.WALK, cir.getReturnValueF(), horseloyalty$self(), player));
		} else if (horseloyalty$gait != Gait.HALT) {
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
		// A bolting horse ignores the rider and veers on its own; a sidestepping one doesn't turn.
		float target = horseloyalty$boltTicks > 0 ? horseloyalty$boltTurn * maxRate
				: horseloyalty$sidestepping ? 0 : Math.signum(rider.xxa) * -maxRate;
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
