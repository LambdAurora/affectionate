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

package dev.lambdaurora.affectionate;

import dev.lambdaurora.affectionate.entity.AffectionatePlayerEntity;
import dev.lambdaurora.affectionate.entity.LapSeatEntity;
import dev.lambdaurora.affectionate.network.SendHeartsPayload;
import dev.yumi.mc.core.api.ModContainer;
import dev.yumi.mc.core.api.entrypoint.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Formatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Text;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Affectionate implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Affectionate.class);
	public static final String NAMESPACE = "affectionate";

	/* Tags */
	public static final TagKey<EntityType<?>> DISALLOWED_SEATS_FOR_LAP = TagKey.of(Registries.ENTITY_TYPE, id("disallowed_seats_for_lap"));
	public static final TagKey<EntityType<?>> ALLOWED_SEATS_FOR_LAP = TagKey.of(Registries.ENTITY_TYPE, id("allowed_seats_for_lap"));

	/* Entities */
	public static final EntityType<LapSeatEntity> LAP_SEAT_ENTITY_TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("lap_seat"),
			EntityType.Builder.of(LapSeatEntity::new, MobCategory.MISC)
					.sized(0.f, 0.f) // this is not `EntityDimensions.fixed` tho...
					.noSave()
					.noSummon()
					.clientTrackingRange(10)
					.build()
	);

	public static final int SENDING_HEARTS_TICKS = 10;

	@Override
	public void onInitialize(ModContainer mod) {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClientSide() && entity instanceof Player otherPlayer
					&& otherPlayer.getPassengers().stream().noneMatch(e -> e instanceof LapSeatEntity)) {
				var vehicle = otherPlayer.getVehicle();
				if (vehicle == null || (vehicle.getType().is(DISALLOWED_SEATS_FOR_LAP) && !vehicle.getType().is(ALLOWED_SEATS_FOR_LAP))) {
					return InteractionResult.PASS;
				}

				var lapSeat = LAP_SEAT_ENTITY_TYPE.create(world);
				if (lapSeat == null)
					return InteractionResult.PASS;

				// Track player and set position before spawning.
				lapSeat.setTrackedOwner(otherPlayer);
				world.addFreshEntity(lapSeat);
				player.startRiding(lapSeat, true);

				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});

		PayloadTypeRegistry.playS2C().register(SendHeartsPayload.TYPE, SendHeartsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SendHeartsPayload.TYPE, SendHeartsPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SendHeartsPayload.TYPE, (sPayload, ctx) -> {
			ctx.server().execute(() -> {
				final var player = ctx.player();
				final var affectionatePlayer = (AffectionatePlayerEntity) player;

				if (!affectionatePlayer.affectionate$isSendingHeart()) {
					affectionatePlayer.affectionate$startSendHeart();

					var payload = new SendHeartsPayload(player.getId());
					for (ServerPlayer tracking : PlayerLookup.tracking(player)) {
						ServerPlayNetworking.send(tracking, payload);
					}
				}
			});
		});

		final var registeredPack = ResourceManagerHelper.registerBuiltinResourcePack(
				id("recursive_sitting"),
				FabricLoader.getInstance().getModContainer(mod.id()).orElseThrow(),
				Text.literal("Affectionate").withStyle(Formatting.LIGHT_PURPLE)
						.append(Text.literal(" - ").withStyle(Formatting.GRAY))
						.append(Text.literal("Recursive Lap Sitting").withStyle(Formatting.RED)),
				ResourcePackActivationType.NORMAL
		);
		if (!registeredPack) {
			LOGGER.warn("Failed to register built-in resource pack.");
		}
	}

	public static Identifier id(String path) {
		return Identifier.of(NAMESPACE, path);
	}

	public static float getEffectiveBodyYaw(LivingEntity entity) {
		float bodyYaw = entity.yBodyRot;
		if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle) {
			bodyYaw = vehicle.yBodyRot;

			float delta = entity.yHeadRot - bodyYaw;
			float deltaDegrees = MathHelper.wrapDegrees(delta);
			if (deltaDegrees < -85.0F) {
				deltaDegrees = -85.0F;
			}

			if (deltaDegrees >= 85.0F) {
				deltaDegrees = 85.0F;
			}

			bodyYaw = entity.yHeadRot - deltaDegrees;
			if (deltaDegrees * deltaDegrees > 2500.0F) {
				bodyYaw += deltaDegrees * 0.2F;
			}
		}

		return bodyYaw;
	}
}
