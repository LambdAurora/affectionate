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

package dev.lambdaurora.affectionate.entity;

import com.mojang.math.Constants;
import dev.lambdaurora.affectionate.Affectionate;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Represents a placeholder entity to make another entity seat on the laps of a player.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings( "resource" )
public class LapSeatEntity extends Entity {
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(LapSeatEntity.class, EntityDataSerializers.INT);
	private LivingEntity trackedOwner;

	public LapSeatEntity(EntityType<?> type, Level world) {
		super(type, world);

		this.noPhysics = true;
	}

	public void setTrackedOwner(LivingEntity trackedOwner) {
		this.entityData.set(OWNER, trackedOwner == null ? 0 : trackedOwner.getId());
		this.updateTrackedPosition(Entity::setPos);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
		if (OWNER.equals(data)) {
			int ownerId = this.entityData.get(OWNER);
			var owner = this.level().getEntity(ownerId);

			if (owner instanceof LivingEntity player) {
				this.trackedOwner = player;
				this.startRiding(this.trackedOwner, true);
			} else {
				this.trackedOwner = null;
			}
		}
	}

	public void updateTrackedPosition(Entity.MoveFunction positionUpdater) {
		if (this.trackedOwner == null) return;

		var relativePos = new Vec3(0.d, .7d, .55d);
		Vec3 transformedPos = relativePos
				.scale(this.trackedOwner.getAgeScale())
				.yRot(Affectionate.getEffectiveBodyYaw(this.trackedOwner) * -Constants.RAD_TO_DEG);

		var newPos = this.trackedOwner.getPos().add(transformedPos);
		positionUpdater.accept(this, newPos.x(), newPos.y(), newPos.z());

		this.setYaw(this.getVisualRotationYInDegrees());
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		super.remove(reason);
		this.setTrackedOwner(null);
	}


	@Override
	protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
		builder.define(OWNER, 0);
	}

	@Override
	public @NotNull Vec3 getPassengerRidingPosition(Entity entity) {
		return this.getPos();
	}

	@Override
	public @NotNull Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		var vec = super.getDismountLocationForPassenger(passenger);

		if (this.level().getBlockState(this.getBlockPos().above()).isAir()) {
			return new Vec3(vec.x, this.getBlockY() + 1, vec.z);
		}

		return vec;
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}

	/* Serialization */

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataFromNbt(NbtCompound nbt) {
	}

	/* Networking */

	@Override
	public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket( ServerEntity serverEntity) {
		return new ClientboundAddEntityPacket(this, serverEntity);
	}

	/* Ticking */

	@Override
	public void tick() {
		super.tick();

		if (!this.level().isClientSide()) {
			if (!this.isVehicle() || this.trackedOwner == null || this.trackedOwner.isRemoved() || !this.trackedOwner.isPassenger()) {
				this.discard();
			}
		}
	}
}
