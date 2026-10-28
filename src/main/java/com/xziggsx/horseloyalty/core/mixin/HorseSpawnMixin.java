package com.xziggsx.horseloyalty.core.mixin;

import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.event.HorseEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link HorseEvents#WILD_SPAWN} for a newly spawned horse that wasn't bred. Runs after
 * vanilla has rolled the coat. The returned group data is the one shared by the whole spawn group.
 */
@Mixin(Horse.class)
public abstract class HorseSpawnMixin {
	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	private void horseloyalty$wildSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
		Horse horse = (Horse) (Object) this;
		// Bred foals are set up by the breeding event instead.
		if (reason != EntitySpawnReason.BREEDING && !horse.level().isClientSide() && HorseScope.inScope(horse)) {
			HorseEvents.WILD_SPAWN.invoker().spawned(horse, cir.getReturnValue());
		}
	}
}
