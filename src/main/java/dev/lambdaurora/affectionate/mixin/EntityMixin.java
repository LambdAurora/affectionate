/*
 * Copyright (c) 2022 LambdAurora <email@lambdaurora.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.affectionate.mixin;

import dev.lambdaurora.affectionate.entity.LapSeatEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
	@Inject(
			method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getPassengerRidingPosition(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/Vec3;"),
			cancellable = true
	)
	private void affectionate$onUpdatePassengerPosition(Entity passenger, Entity.MoveFunction positionUpdater, CallbackInfo ci) {
		if (passenger instanceof LapSeatEntity lapSeat) {
			ci.cancel();
			lapSeat.updateTrackedPosition(positionUpdater);
		}
	}
}
