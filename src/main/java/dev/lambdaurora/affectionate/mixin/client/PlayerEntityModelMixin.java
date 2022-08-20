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

package dev.lambdaurora.affectionate.mixin.client;

import dev.lambdaurora.affectionate.client.AffectionateClient;
import dev.lambdaurora.affectionate.entity.AffectionatePlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin<T extends LivingEntity> {
	@SuppressWarnings("unchecked")
	@Inject(
			method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",
					shift = At.Shift.AFTER
			)
	)
	private void onSetAngles(T livingEntity, float f, float limbDistance, float animationProgress, float i, float pitch, CallbackInfo ci) {
		if (livingEntity instanceof AffectionatePlayerEntity player) {
			AffectionateClient.updatePlayerModel((PlayerEntityModel<T>) (Object) this, player, animationProgress - livingEntity.age);
		}
	}
}
