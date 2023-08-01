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

import com.unascribed.ears.api.features.EarsFeatures;
import com.unascribed.ears.common.render.EarsRenderDelegate;
import dev.lambdaurora.affectionate.entity.AffectionatePlayerEntity;
import org.quiltmc.loader.api.minecraft.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@ClientOnly
@Mixin(targets = "com.unascribed.ears.common.EarsRenderer")
public class EarsRendererMixin {
	@Inject(
			method = "renderInner(Lcom/unascribed/ears/api/features/EarsFeatures;Lcom/unascribed/ears/common/render/EarsRenderDelegate;IZ)V",
			at = @At(
					value = "FIELD",
					target = "Lcom/unascribed/ears/api/features/EarsFeatures;tailSegments:I"
			),
			remap = false,
			require = 0
	)
	private static void onRenderTail(EarsFeatures features, EarsRenderDelegate delegate, int p, boolean drawEmissivity, CallbackInfo ci) {
		if (features.tailMode == EarsFeatures.TailMode.VERTICAL && delegate.getPeer() instanceof AffectionatePlayerEntity player) {
			if (player.affectionate$shouldWagTail()) {
				var entity = player.affectionate$asPlayer();
				final double tailAnimation = Math.sin((entity.getId() + delegate.getTime()) * 0.5) * 30.0;
				delegate.rotate((float) tailAnimation, 1, 0, 0);
			}
		}
	}
}
