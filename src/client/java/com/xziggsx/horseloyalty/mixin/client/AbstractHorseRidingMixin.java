package com.xziggsx.horseloyalty.mixin.client;

import com.xziggsx.horseloyalty.client.riding.Gait;
import com.xziggsx.horseloyalty.client.riding.GaitHolder;
import com.xziggsx.horseloyalty.client.riding.GaitRaise;
import com.xziggsx.horseloyalty.client.riding.GallopPush;
import com.xziggsx.horseloyalty.client.riding.Riding;
import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.personality.Personality;
import com.xziggsx.horseloyalty.riding.GaitSync;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
 * sidestepping (sprint key + A/D), pushing at a gallop (W taps), the horse refusing to go faster,
 * random gait drops, and skittish horses bolting from creepers. The gait is sent to the server
 * whenever it changes (see {@link GaitSync}), so other players can see and hear it.
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

	/** Gait index last sent to the server, -1 if none yet this ride. */
	@Unique
	private int horseloyalty$sentGait = -1;

	/** A drop, bolt or refusal still to be sent to the server with the gait, or null. */
	@Unique
	private GaitSync.Event horseloyalty$pendingEvent;

	/** Pushing at a gallop with W taps. */
	@Unique
	private final GallopPush horseloyalty$push = new GallopPush();

	/** Whether the horse goes along with more speed, and its cooldown after refusing. Kept between rides. */
	@Unique
	private final GaitRaise horseloyalty$raise = new GaitRaise();

	@Override
	public Gait horseloyalty$getGait() {
		return horseloyalty$gait;
	}

	@Override
	public int horseloyalty$getGaitDropTimer() {
		return horseloyalty$gaitDropTimer;
	}

	@Override
	public GallopPush horseloyalty$getGallopPush() {
		return horseloyalty$push;
	}

	@Override
	public int horseloyalty$getRaiseCooldown() {
		return horseloyalty$raise.cooldownLeft(horseloyalty$self());
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
		horseloyalty$sentGait = -1;
		horseloyalty$pendingEvent = null;
		horseloyalty$push.reset();
	}

	/**
	 * Gait control: W taps shift up, S taps shift down, and the horse keeps going at
	 * its gait without holding a key. Pressing S again at a halt and holding it backs up. A and D steer (see getRiddenRotation), so they
	 * no longer strafe. W taps at a gallop push the horse faster. A shift up or push can fail (see
	 * askForMore). Also rolls the random gait drop.
	 */
	@Inject(method = "getRiddenInput", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$gaitInput(Player player, Vec3 travelVector, CallbackInfoReturnable<Vec3> cir) {
		AbstractHorse horse = horseloyalty$self();
		if (!HorseScope.inScope(horse)) {
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
			if (horseloyalty$gait == Gait.GALLOP) {
				if (horseloyalty$push.tap(horse, player, () -> horseloyalty$askForMore(horse, player, Gait.GALLOP))) {
					// Pushing shows the rider is paying attention, like a shift.
					horseloyalty$gaitDropTimer = 0;
				}
			} else if (horseloyalty$askForMore(horse, player, horseloyalty$gait)) {
				horseloyalty$gait = horseloyalty$gait.up();
			}
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
			// A buck has its own toast; "Halt" on top of it would only confuse.
			if (horseloyalty$pendingEvent != GaitSync.Event.BUCKED) {
				player.sendOverlayMessage(horseloyalty$gait.displayName());
			}
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
				horseloyalty$pendingEvent = GaitSync.Event.DROPPED;
			}
		}
		horseloyalty$push.tick(horse, player, !bolting && horseloyalty$gait == Gait.GALLOP);
		horseloyalty$syncGait();

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
	 * Asks the horse for more speed from {@code from}: a shift up, or a push at a gallop. Returns
	 * whether it goes along. Every "go faster" goes through here, so a new one gets the same
	 * chance to refuse (see {@link GaitRaise}). A refusing horse snorts and rears up, or throws its
	 * rider off; the server makes that happen for everyone (see {@link GaitSync}).
	 */
	@Unique
	private boolean horseloyalty$askForMore(AbstractHorse horse, Player player, Gait from) {
		GaitRaise.Outcome outcome = horseloyalty$raise.ask(horse, player, from);
		if (outcome == GaitRaise.Outcome.OK || outcome == GaitRaise.Outcome.UNSETTLED) {
			return outcome == GaitRaise.Outcome.OK;
		}
		horseloyalty$playAngrySound(horse);
		// Asking counts as paying attention, like a shift, so the drop timer restarts.
		horseloyalty$gaitDropTimer = 0;
		if (outcome == GaitRaise.Outcome.BUCK) {
			// The server throws the rider off; stop here in the meantime.
			horseloyalty$gait = Gait.HALT;
			horseloyalty$pendingEvent = GaitSync.Event.BUCKED;
		} else {
			horseloyalty$pendingEvent = GaitSync.Event.PRANCED;
		}
		return false;
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
		horseloyalty$pendingEvent = GaitSync.Event.BOLTED;
		return true;
	}

	/**
	 * Sends the gait to the server when it changed, or a drop, bolt or refusal happened, since the last send.
	 * A few packets a minute at most: nothing is sent while the rider holds a gait.
	 */
	@Unique
	private void horseloyalty$syncGait() {
		int gait = horseloyalty$gait.ordinal();
		if (gait == horseloyalty$sentGait && horseloyalty$pendingEvent == null) {
			return;
		}
		if (ClientPlayNetworking.canSend(GaitSync.GaitPayload.TYPE)) {
			GaitSync.Event event = horseloyalty$pendingEvent != null ? horseloyalty$pendingEvent : GaitSync.Event.SHIFT;
			ClientPlayNetworking.send(new GaitSync.GaitPayload(gait, event));
		}
		horseloyalty$sentGait = gait;
		horseloyalty$pendingEvent = null;
	}

	/**
	 * The vanilla bucking/angry sound, played locally so the rider hears it without delay.
	 * Everyone else hears it from the server (see {@link GaitSync}).
	 */
	@Unique
	private void horseloyalty$playAngrySound(AbstractHorse horse) {
		SoundEvent angry = getAngrySound();
		if (angry != null) {
			horse.level().playLocalSound(horse, angry, horse.getSoundSource(), 1.0F, horse.getVoicePitch());
		}
	}

	/** Replaces the horse's speed with the current gait's speed, scaled by the rider's loyalty and any gallop push. */
	@Inject(method = "getRiddenSpeed", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$gaitSpeed(Player player, CallbackInfoReturnable<Float> cir) {
		if (!HorseScope.inScope(horseloyalty$self())) {
			return;
		}
		if (horseloyalty$reversing) {
			cir.setReturnValue((float) Gait.REVERSE_SPEED);
		} else if (horseloyalty$sidestepping && horseloyalty$gait == Gait.HALT) {
			// Stepping sideways from a halt goes at walking pace.
			cir.setReturnValue((float) Riding.gaitSpeed(Gait.WALK, cir.getReturnValueF(), horseloyalty$self(), player));
		} else if (horseloyalty$gait != Gait.HALT) {
			double speed = Riding.gaitSpeed(horseloyalty$gait, cir.getReturnValueF(), horseloyalty$self(), player);
			cir.setReturnValue((float) (speed * (1 + horseloyalty$push.extra())));
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
		if (!HorseScope.inScope(horse)) {
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
