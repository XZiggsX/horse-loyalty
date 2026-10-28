package com.xziggsx.horseloyalty.mixin;

import com.xziggsx.horseloyalty.genetics.world.HorseGenetics;
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
 * Gives a newly spawned horse genes from its herd's founders (see {@link HorseGenetics#onSpawn}).
 * Runs after vanilla has rolled the coat, and replaces it with what the genes show.
 * The returned group data is the one shared by the whole spawn group.
 */
@Mixin(Horse.class)
public abstract class HorseSpawnMixin {
	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	private void horseloyalty$spawnGenes(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
		// Bred foals get their genes from their parents instead.
		if (reason != EntitySpawnReason.BREEDING) {
			HorseGenetics.onSpawn((Horse) (Object) this, cir.getReturnValue());
		}
	}
}
