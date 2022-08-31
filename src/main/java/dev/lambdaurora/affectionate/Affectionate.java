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
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.PlayerLookup;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.quiltmc.qsl.resource.loader.api.ResourceLoader;
import org.quiltmc.qsl.resource.loader.api.ResourcePackActivationType;

public final class Affectionate implements ModInitializer {
	public static final String NAMESPACE = "affectionate";

	/* Tags */
	public static final TagKey<EntityType<?>> DISALLOWED_SEATS_FOR_LAP = TagKey.of(Registry.ENTITY_TYPE_KEY, id("disallowed_seats_for_lap"));
	public static final TagKey<EntityType<?>> ALLOWED_SEATS_FOR_LAP = TagKey.of(Registry.ENTITY_TYPE_KEY, id("allowed_seats_for_lap"));

	/* Packets */
	public static final Identifier SEND_HEARTS_PACKET = id("send_hearts");


	/* Entities */
	public static final EntityType<LapSeatEntity> LAP_SEAT_ENTITY_TYPE = Registry.register(Registry.ENTITY_TYPE, id("lap_seat"),
			FabricEntityTypeBuilder.create(SpawnGroup.MISC, LapSeatEntity::new)
					.dimensions(EntityDimensions.fixed(0.f, 0.f))
					.disableSaving()
					.disableSummon()
					.trackRangeChunks(10)
					.build()
	);

	public static final int SENDING_HEARTS_TICKS = 10;

	@Override
	public void onInitialize(ModContainer mod) {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClient() && entity instanceof PlayerEntity otherPlayer
					&& otherPlayer.getPassengerList().stream().noneMatch(e -> e instanceof LapSeatEntity)) {
				var vehicle = otherPlayer.getVehicle();
				if (vehicle == null || (vehicle.getType().isIn(DISALLOWED_SEATS_FOR_LAP) && !vehicle.getType().isIn(ALLOWED_SEATS_FOR_LAP))) {
					return ActionResult.PASS;
				}

				var lapSeat = LAP_SEAT_ENTITY_TYPE.create(world);
				if (lapSeat == null)
					return ActionResult.PASS;

				world.spawnEntity(lapSeat);
				lapSeat.setTrackedOwner(otherPlayer);
				player.startRiding(lapSeat, true);

				return ActionResult.SUCCESS;
			}

			return ActionResult.PASS;
		});

		ServerPlayNetworking.registerGlobalReceiver(SEND_HEARTS_PACKET, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				var affectionatePlayer = (AffectionatePlayerEntity) player;

				if (!affectionatePlayer.affectionate$isSendingHeart()) {
					affectionatePlayer.affectionate$startSendHeart();

					var newBuf = PacketByteBufs.create();
					newBuf.writeVarInt(player.getId());

					ServerPlayNetworking.send(PlayerLookup.tracking(player), SEND_HEARTS_PACKET, newBuf);
				}
			});
		});

		ResourceLoader.registerBuiltinResourcePack(id("recursive_sitting"), mod, ResourcePackActivationType.NORMAL,
				Text.literal("Affectionate").formatted(Formatting.LIGHT_PURPLE)
						.append(Text.literal(" - ").formatted(Formatting.GRAY))
						.append(Text.literal("Recursive Lap Sitting").formatted(Formatting.RED))
		);
	}

	public static Identifier id(String path) {
		return new Identifier(NAMESPACE, path);
	}
}
