package com.example.horseloyalty.mixin;

import com.example.horseloyalty.loyalty.Family;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks horse breeding to set up the foal's family (see {@link Family}). Horses don't
 * override breeding, so this hooks Animal and checks for horses.
 * <p>
 * Runs at the start of finalizeSpawnChildFromBreeding: the foal exists but isn't in the
 * world yet, and the parents still remember who fed them.
 */
@Mixin(Animal.class)
public abstract class AnimalBreedingMixin {
	@Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
	private void horseloyalty$setUpFoal(ServerLevel level, Animal partner, AgeableMob child, CallbackInfo ci) {
		Animal self = (Animal) (Object) this;
		if (self instanceof AbstractHorse parentA && partner instanceof AbstractHorse parentB
				&& child instanceof AbstractHorse foal) {
			// Same order as vanilla's breeding advancement: this parent's feeder, else the partner's.
			ServerPlayer breeder = self.getLoveCause() != null ? self.getLoveCause() : partner.getLoveCause();
			Family.onBorn(foal, parentA, parentB, breeder);
		}
	}
}
