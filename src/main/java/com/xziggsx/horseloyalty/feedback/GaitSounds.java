package com.xziggsx.horseloyalty.feedback;

import com.xziggsx.horseloyalty.feedback.api.FeedbackSettings;
import com.xziggsx.horseloyalty.riding.api.Gait;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hoofbeats that match the gait, so you can hear it: a soft step at a walk, a brisker and louder
 * step at a trot, a quieter rolling gallop at a canter and the full vanilla gallop (with its
 * breathing) at a gallop. Water keeps the vanilla sounds. Picks the block's sound the same way
 * vanilla does. Turned off with the {@code gaitSounds} feedback setting.
 */
public final class GaitSounds {
	private GaitSounds() {
	}

	/**
	 * Plays the step sound for this gait instead of vanilla's. Returns false (and plays nothing)
	 * when vanilla's sound should play after all.
	 *
	 * @param gallopSound  the horse's own gallop sound, which vanilla keeps protected
	 * @param woodSoundType whether a block sound counts as wood, which vanilla keeps private
	 */
	public static boolean play(AbstractHorse horse, Gait gait, BlockPos pos, BlockState state,
			Consumer<SoundType> gallopSound, Predicate<SoundType> woodSoundType) {
		if (state.liquid() || !FeedbackSettings.get().gaitSounds()) {
			return false;
		}
		BlockState above = horse.level().getBlockState(pos.above());
		SoundType type = above.is(Blocks.SNOW) ? above.getSoundType() : state.getSoundType();
		if (gait == Gait.GALLOP) {
			gallopSound.accept(type);
		} else if (gait == Gait.CANTER) {
			horse.playSound(SoundEvents.HORSE_GALLOP, type.getVolume() * 0.12F, type.getPitch() * 0.85F);
		} else {
			SoundEvent step = woodSoundType.test(type) ? SoundEvents.HORSE_STEP_WOOD : SoundEvents.HORSE_STEP;
			boolean trot = gait == Gait.TROT;
			horse.playSound(step, type.getVolume() * (trot ? 0.22F : 0.15F), type.getPitch() * (trot ? 1.15F : 1.0F));
		}
		return true;
	}
}
