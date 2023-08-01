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

package dev.lambdaurora.affectionate.client;

import com.mojang.blaze3d.platform.InputUtil;
import dev.lambdaurora.affectionate.Affectionate;
import dev.lambdaurora.affectionate.client.renderer.LapSeatEntityRenderer;
import dev.lambdaurora.affectionate.entity.AffectionatePlayerEntity;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBind;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.minecraft.ClientOnly;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientWorldTickEvents;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

@ClientOnly
public final class AffectionateClient implements ClientModInitializer, ClientWorldTickEvents.Start {
	private static final KeyBind SEND_HEART_KEY_BIND = KeyBindingHelper.registerKeyBinding(new KeyBind(
			"key.affectionate.interact", InputUtil.KEY_G_CODE, KeyBind.MULTIPLAYER_CATEGORY
	));

	public static final AffectionateClient INSTANCE = new AffectionateClient();

	@Override
	public void onInitializeClient(ModContainer mod) {
		EntityRendererRegistry.register(Affectionate.LAP_SEAT_ENTITY_TYPE, LapSeatEntityRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(Affectionate.SEND_HEARTS_PACKET, (client, handler, buf, responseSender) -> {
			int playerId = buf.readVarInt();

			client.execute(() -> {
				if (client.world != null && client.world.getEntityById(playerId) instanceof AffectionatePlayerEntity player) {
					player.affectionate$startSendHeart();
				}
			});
		});
	}

	@Override
	public void startWorldTick(MinecraftClient client, ClientWorld world) {
		if (SEND_HEART_KEY_BIND.wasPressed() && client.player != null) {
			if (!((AffectionatePlayerEntity) client.player).affectionate$isSendingHeart()) {
				((AffectionatePlayerEntity) client.player).affectionate$startSendHeart();

				ClientPlayNetworking.send(Affectionate.SEND_HEARTS_PACKET, PacketByteBufs.empty());
			}
		}
	}

	/**
	 * Updates the player model freely.
	 *
	 * @param model the player model
	 * @param player the player
	 * @param tickDelta the tick delta
	 * @param <E> the type of entity the model accepts
	 */
	public static <E extends LivingEntity> void updatePlayerModel(PlayerEntityModel<E> model, AffectionatePlayerEntity player, float tickDelta) {
		if (player.affectionate$isSendingHeart()) {
			float delta = player.affectionate$getHeartSendingDelta(tickDelta);

			final float targetPitch = (float) Math.toRadians(-110.f);
			model.rightArm.pitch = MathHelper.lerp(delta, model.rightArm.pitch, targetPitch);
			model.leftArm.pitch = MathHelper.lerp(delta, model.leftArm.pitch, targetPitch);

			final float targetYaw = (float) Math.toRadians(25.f);
			model.rightArm.yaw = MathHelper.lerp(delta, model.rightArm.yaw, -targetYaw);
			model.leftArm.yaw = MathHelper.lerp(delta, model.leftArm.yaw, targetYaw);
		}
	}
}
