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

package dev.lambdaurora.affectionate.client.renderer;

import dev.lambdaurora.affectionate.entity.LapSeatEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;

@Environment(EnvType.CLIENT)
public class LapSeatEntityRenderer extends NoopRenderer<LapSeatEntity> {
	public LapSeatEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(LapSeatEntity entity, Frustum frustum, double x, double y, double z) {
		return false;
	}
}
