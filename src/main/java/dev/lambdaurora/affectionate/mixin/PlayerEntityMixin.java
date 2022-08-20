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

import dev.lambdaurora.affectionate.Affectionate;
import dev.lambdaurora.affectionate.entity.AffectionatePlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements AffectionatePlayerEntity {
	@Unique
	private int affectionate$heartSendingTicks;

	protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public void affectionate$startSendHeart() {
		this.affectionate$heartSendingTicks = Affectionate.SENDING_HEARTS_TICKS + 5;
	}

	@Override
	public boolean affectionate$isSendingHeart() {
		return this.affectionate$heartSendingTicks > 0;
	}

	@Override
	public float affectionate$getHeartSendingDelta(float tickDelta) {
		float progress = (Affectionate.SENDING_HEARTS_TICKS - this.affectionate$heartSendingTicks + 5) + tickDelta;
		return Math.min(progress / Affectionate.SENDING_HEARTS_TICKS, 1.f);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		if (this.affectionate$isSendingHeart()) {
			this.affectionate$heartSendingTicks--;

			if (this.world.isClient() && this.affectionate$heartSendingTicks <= 5) {
				var relativePos = new Vec3f(0.f, 1.5f, .60f);
				relativePos.rotate(new Quaternion(0.f, -this.bodyYaw, 0.f, true));
				Vec3d transformedPos = new Vec3d(relativePos);

				var pos = this.getPos().add(transformedPos);
				this.world.addImportantParticle(ParticleTypes.HEART,
						pos.getX(), pos.getY(), pos.getZ(),
						transformedPos.getX() * 10, -0.2f, transformedPos.getZ() * 10
				);
			}
		}
	}
}
