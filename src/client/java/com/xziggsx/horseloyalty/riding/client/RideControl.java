package com.xziggsx.horseloyalty.riding.client;

import com.xziggsx.horseloyalty.riding.GaitSync;
import com.xziggsx.horseloyalty.riding.RidingSettings;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.api.GaitEvent;
import com.xziggsx.horseloyalty.riding.client.sense.Senses;
import com.xziggsx.horseloyalty.riding.mixin.HorseSoundInvoker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * One horse's ride, on the rider's client: gaits (W/S taps), backing up, gait speeds, boat-style
 * A/D steering, sidestepping (sprint key + A/D), pushing at a gallop (W taps), the horse refusing
 * to go faster, random gait drops, spooked horses bolting, and the horse following a path by itself
 * (see {@link Senses}). The gait is sent to the server
 * whenever it changes (see {@link GaitSync}), so other players can see and hear it.
 * <p>
 * One of these lives on every horse (see {@code HorseRidingMixin}, which only forwards here).
 * Minecraft moves a player-ridden horse on that player's own client, so vanilla only asks for the
 * ridden input, speed and rotation there.
 */
public final class RideControl {
	/** Current gait. */
	private Gait gait = Gait.HALT;
	/** Whether the rider is holding the sprint key with A or D: move diagonally instead of turning. */
	private boolean sidestepping;
	/** Whether the rider is holding S at a halt to back the horse up. */
	private boolean reversing;
	/** Rider's forward input last tick, to detect W and S taps. */
	private float lastForward;
	/** Current turn rate in degrees per tick, eased towards the A/D target. */
	private float turnSpeed;
	/** Turn applied to the horse this tick, still to be applied to the rider's view. */
	private float pendingRiderTurn;
	/** Ticks spent above a halt since the rider last shifted gait or the last drop roll. */
	private int gaitDropTimer;
	/** Ticks left of a bolt. While above 0 the horse gallops and steers itself. */
	private int boltTicks;
	/** Ticks until the next spook check; also the calm-down time after a bolt. */
	private int boltCheckTimer;
	/** Which way a bolting horse veers: -1 left, 0 straight, 1 right. */
	private int boltTurn;
	/** Gait last sent to the server, null if none yet this ride. */
	private Gait sentGait;
	/** A drop, bolt or refusal still to be sent to the server with the gait, or null. */
	private GaitEvent pendingEvent;
	/** Pushing at a gallop with W taps. */
	private final GallopPush push = new GallopPush();
	/** Whether the horse goes along with more speed, and its cooldown after refusing. Kept between rides. */
	private final GaitRaise raise = new GaitRaise();
	/** What the horse senses ahead and does about it on its own, such as following a path. */
	private final Senses senses = new Senses();

	/** The current gait. */
	public Gait gait() {
		return gait;
	}

	/** Ticks the horse has been moving since the rider last shifted gait (or the last drop roll). */
	public int gaitDropTimer() {
		return gaitDropTimer;
	}

	/** The gallop push, to read how hard the horse is pushed. */
	public GallopPush push() {
		return push;
	}

	/** The horse's senses, to read what it makes of the way ahead. */
	public Senses senses() {
		return senses;
	}

	/** Ticks until the horse will go faster again after refusing, 0 if it will now. */
	public int raiseCooldown(AbstractHorse horse) {
		return raise.cooldownLeft(horse);
	}

	/** Starts a ride at a halt, with no turn in progress. */
	public void reset() {
		gait = Gait.HALT;
		lastForward = 0;
		turnSpeed = 0;
		pendingRiderTurn = 0;
		gaitDropTimer = 0;
		reversing = false;
		sidestepping = false;
		boltTicks = 0;
		boltCheckTimer = 0;
		boltTurn = 0;
		sentGait = null;
		pendingEvent = null;
		push.reset();
		senses.reset();
	}

	/**
	 * Gait control, once per riding tick: W taps shift up, S taps shift down, and the horse keeps going
	 * at its gait without holding a key. Pressing S again at a halt and holding it backs up. A and D
	 * steer (see {@link #rotation}), so they no longer strafe. W taps at a gallop push the horse faster.
	 * A shift up or push can fail (see {@link #askForMore}). Also rolls the random gait drop. Returns
	 * the input to move the horse with, or vanilla's own input while the horse is rearing.
	 */
	public Vec3 input(AbstractHorse horse, Player player, Vec3 vanilla) {
		float forward = player.zza;
		Gait before = gait;
		boolean bolting = updateBolt(horse, player);
		if (bolting) {
			// The rider has no say until the bolt is over.
			gait = Gait.GALLOP;
			reversing = false;
		} else if (forward > 0 && lastForward <= 0) {
			if (gait == Gait.GALLOP) {
				if (push.tap(horse, player, () -> askForMore(horse, player, Gait.GALLOP))) {
					// Pushing shows the rider is paying attention, like a shift.
					gaitDropTimer = 0;
				}
			} else if (askForMore(horse, player, gait)) {
				gait = gait.up();
			}
		} else if (forward < 0 && lastForward >= 0) {
			if (gait == Gait.HALT) {
				// A fresh S press while already halted: back up for as long as it's held.
				reversing = true;
				player.sendOverlayMessage(Component.translatable("horseloyalty.gait.reverse"));
			} else {
				gait = gait.down();
			}
		}
		if (forward >= 0) {
			reversing = false;
		}
		// Sprint key (Ctrl by default) + A/D: the horse keeps facing the same way and moves at an
		// angle instead. Halted, it steps straight sideways. A bolting horse ignores the rider.
		sidestepping = !bolting && player.xxa != 0 && Minecraft.getInstance().options.keySprint.isDown();
		lastForward = forward;
		// While bolting there are no gait messages or drops; updateBolt sends its own messages.
		if (bolting) {
			gaitDropTimer = 0;
		} else if (gait != before) {
			// Only the rider's own client runs this, so this shows on their screen only.
			// A buck has its own toast; "Halt" on top of it would only confuse.
			if (pendingEvent != GaitEvent.BUCKED) {
				player.sendOverlayMessage(gait.displayName());
			}
			// Shifting gait shows the rider is paying attention, so the drop timer restarts.
			gaitDropTimer = 0;
		} else if (gait != Gait.HALT && ++gaitDropTimer >= RideNumbers.gaitDropTicks(horse, player, senses.path().active())) {
			// Less loyal horses lose interest sooner and drop a gait on their own, which
			// also stops riders from holding nothing and crossing the map AFK. On a path the
			// horse follows by itself the timer is longer, to reward building roads. With a
			// 100% chance (the level 5 default) this is a fixed timer.
			gaitDropTimer = 0;
			if (horse.getRandom().nextDouble() < RideNumbers.gaitDropChance(horse, player)) {
				gait = gait.down();
				player.sendOverlayMessage(Component.translatable("horseloyalty.gait.dropped", gait.displayName()));
				playAngrySound(horse);
				pendingEvent = GaitEvent.DROPPED;
			}
		}
		push.tick(horse, player, !bolting && gait == Gait.GALLOP);
		syncGait();

		// Vanilla returns the Vec3.ZERO constant itself while the horse is rearing. Keep that.
		if (vanilla == Vec3.ZERO) {
			return vanilla;
		}
		double forwardInput = RideMath.forwardInput(reversing, gait == Gait.HALT);
		double sideInput = sidestepping
				? RideMath.sideInput(forwardInput, player.xxa, RidingSettings.get().sidestepAngleDegrees()) : 0;
		// Vanilla scales an input longer than 1 back down to 1, so moving diagonally keeps the gait's speed.
		return new Vec3(sideInput, 0, forwardInput);
	}

	/**
	 * The horse's speed for this rider: the current gait's, scaled by their loyalty and any gallop
	 * push. Null at a halt, so vanilla's speed stays.
	 */
	public Float speed(AbstractHorse horse, Player player, float vanilla) {
		if (reversing) {
			return (float) Gait.REVERSE_SPEED;
		}
		if (sidestepping && gait == Gait.HALT) {
			// Stepping sideways from a halt goes at walking pace.
			return (float) RideNumbers.gaitSpeed(Gait.WALK, vanilla, horse, player);
		}
		if (gait != Gait.HALT) {
			return (float) (RideNumbers.gaitSpeed(gait, vanilla, horse, player) * (1 + push.extra()));
		}
		return null;
	}

	/**
	 * Boat-style steering. Vanilla points the horse wherever the rider looks; instead A and D turn it,
	 * easing up to a fixed rate set by the rider's loyalty, and the rider's view turns along with it
	 * (see {@link #takeRiderTurn}). The rider can still look around freely with the mouse. With no A
	 * or D held, the horse's senses may steer it, such as along a path.
	 */
	public Vec2 rotation(AbstractHorse horse, LivingEntity rider) {
		// Turn rate depends on loyalty. Only players steer horses, see getControllingPassenger.
		float maxRate = rider instanceof Player player ? RideNumbers.turnRate(horse, player) : RideNumbers.defaultTurnRate();
		boolean bolting = boltTicks > 0;
		boolean steering = rider.xxa != 0;
		senses.tick(horse, gait, !bolting && !sidestepping && !reversing && gait != Gait.HALT, steering);
		float senseTarget = senses.turnTarget();
		float target = RideMath.turnTarget(bolting, boltTurn, sidestepping, rider.xxa, maxRate, senseTarget);
		// A turn the horse makes by itself eases at its own rate, so loyalty doesn't creep in through the easing.
		boolean sensed = !bolting && !sidestepping && !steering && !Float.isNaN(senseTarget);
		float easeRate = sensed ? RideNumbers.pathTurnRate(gait) : maxRate;
		turnSpeed = RideMath.easeTurn(turnSpeed, target, RideNumbers.turnEase(easeRate));
		pendingRiderTurn += turnSpeed;
		return new Vec2(rider.getXRot() * 0.5F, horse.getYRot() + turnSpeed);
	}

	/** The turn still to be applied to the rider's view, cleared once taken. */
	public float takeRiderTurn() {
		float turn = pendingRiderTurn;
		pendingRiderTurn = 0;
		return turn;
	}

	/**
	 * Asks the horse for more speed from {@code from}: a shift up, or a push at a gallop. Returns
	 * whether it goes along. Every "go faster" goes through here, so a new one gets the same
	 * chance to refuse (see {@link GaitRaise}). A refusing horse snorts and rears up, or throws its
	 * rider off; the server makes that happen for everyone (see {@link GaitSync}).
	 */
	private boolean askForMore(AbstractHorse horse, Player player, Gait from) {
		RaiseOutcome outcome = raise.ask(horse, player, from);
		if (outcome == RaiseOutcome.OK || outcome == RaiseOutcome.UNSETTLED) {
			return outcome == RaiseOutcome.OK;
		}
		playAngrySound(horse);
		// Asking counts as paying attention, like a shift, so the drop timer restarts.
		gaitDropTimer = 0;
		if (outcome == RaiseOutcome.BUCK) {
			// The server throws the rider off; stop here in the meantime.
			gait = Gait.HALT;
			pendingEvent = GaitEvent.BUCKED;
		} else {
			pendingEvent = GaitEvent.PRANCED;
		}
		return false;
	}

	/**
	 * Runs a bolt: counts down a bolt in progress, or every half second asks the ride modifiers
	 * whether something spooks the horse (a skittish horse and a creeper, say). Returns whether the
	 * horse is bolting this tick.
	 */
	private boolean updateBolt(AbstractHorse horse, Player player) {
		if (boltTicks > 0) {
			boltTicks--;
			if (boltTicks % 10 == 0) {
				boltTurn = horse.getRandom().nextInt(3) - 1;
			}
			if (boltTicks == 0) {
				boltTurn = 0;
				player.sendOverlayMessage(Component.translatable("horseloyalty.personality.bolt_end", horse.getName()));
			}
			return true;
		}
		if (--boltCheckTimer > 0) {
			return false;
		}
		boltCheckTimer = 10;
		int ticks = RideNumbers.spookTicks(horse, player);
		if (ticks <= 0) {
			return false;
		}
		boltTicks = ticks;
		// Five seconds to calm down after the bolt before it can bolt again.
		boltCheckTimer = 100;
		boltTurn = horse.getRandom().nextInt(3) - 1;
		player.sendOverlayMessage(Component.translatable("horseloyalty.personality.bolt", horse.getName()));
		playAngrySound(horse);
		pendingEvent = GaitEvent.BOLTED;
		return true;
	}

	/**
	 * Sends the gait to the server when it changed, or a drop, bolt or refusal happened, since the last send.
	 * A few packets a minute at most: nothing is sent while the rider holds a gait.
	 */
	private void syncGait() {
		if (gait == sentGait && pendingEvent == null) {
			return;
		}
		if (ClientPlayNetworking.canSend(GaitSync.GaitPayload.TYPE)) {
			GaitEvent event = pendingEvent != null ? pendingEvent : GaitEvent.SHIFT;
			ClientPlayNetworking.send(new GaitSync.GaitPayload(gait, event));
		}
		sentGait = gait;
		pendingEvent = null;
	}

	/**
	 * The vanilla bucking/angry sound, played locally so the rider hears it without delay.
	 * Everyone else hears it from the server (see {@link GaitSync}).
	 */
	private static void playAngrySound(AbstractHorse horse) {
		SoundEvent angry = ((HorseSoundInvoker) horse).horseloyalty$getAngrySound();
		if (angry != null) {
			horse.level().playLocalSound(horse, angry, horse.getSoundSource(), 1.0F, horse.getVoicePitch());
		}
	}
}
