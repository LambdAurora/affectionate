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

import com.mojang.blaze3d.platform.InputConstants;
import dev.lambdaurora.affectionate.Affectionate;
import dev.lambdaurora.affectionate.client.renderer.LapSeatEntityRenderer;
import dev.lambdaurora.affectionate.entity.AffectionatePlayerEntity;
import dev.lambdaurora.affectionate.network.SendHeartsPayload;
import dev.yumi.mc.core.api.ModContainer;
import dev.yumi.mc.core.api.entrypoint.client.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public final class AffectionateClient implements ClientModInitializer {
	private static final KeyMapping SEND_HEART_KEY_BIND = new KeyMapping(
		"key.affectionate.interact", InputConstants.KEY_G, KeyMapping.CATEGORY_MULTIPLAYER
	);

	public static final AffectionateClient INSTANCE = new AffectionateClient();

	@Override
	public void onInitializeClient(ModContainer mod) {
		EntityRendererRegistry.register(Affectionate.LAP_SEAT_ENTITY_TYPE, LapSeatEntityRenderer::new);
		KeyBindingHelper.registerKeyBinding(SEND_HEART_KEY_BIND);

		ClientPlayNetworking.registerGlobalReceiver(SendHeartsPayload.TYPE, (payload, ctx) -> {
			ctx.client().execute(() -> {
				if (ctx.client().level != null && ctx.client().level.getEntity(payload.playerId()) instanceof AffectionatePlayerEntity player) {
					player.affectionate$startSendHeart();
				}
			});
		});

		ClientTickEvents.START_WORLD_TICK.register(this::onStartWorldTick);
	}

	public void onStartWorldTick(ClientLevel world) {
		var client = Minecraft.getInstance();
		if (SEND_HEART_KEY_BIND.isDown() && client.player != null) {
			if (!((AffectionatePlayerEntity) client.player).affectionate$isSendingHeart()) {
				((AffectionatePlayerEntity) client.player).affectionate$startSendHeart();
				
				ClientPlayNetworking.send(new SendHeartsPayload(-1));
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
	public static <E extends LivingEntity> void updatePlayerModel(PlayerModel<E> model, AffectionatePlayerEntity player, float tickDelta) {
		if (player.affectionate$isSendingHeart()) {
			float delta = player.affectionate$getHeartSendingDelta(tickDelta);

			final float targetPitch = (float) Math.toRadians(-110.f);
			model.rightArm.xRot = MathHelper.lerp(delta, model.rightArm.xRot, targetPitch);
			model.leftArm.xRot = MathHelper.lerp(delta, model.leftArm.xRot, targetPitch);

			final float targetYaw = (float) Math.toRadians(25.f);
			model.rightArm.yRot = MathHelper.lerp(delta, model.rightArm.yRot, -targetYaw);
			model.leftArm.yRot = MathHelper.lerp(delta, model.leftArm.yRot, targetYaw);
		}
	}
}
