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

package dev.lambdaurora.affectionate.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static dev.lambdaurora.affectionate.Affectionate.id;

public record SendHeartsPayload(int playerId) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, SendHeartsPayload> CODEC = CustomPacketPayload.codec(SendHeartsPayload::write, SendHeartsPayload::new);
	public static final CustomPacketPayload.Type<SendHeartsPayload> TYPE = new CustomPacketPayload.Type<>(id("send_hearts"));

	public SendHeartsPayload(FriendlyByteBuf buf) {
		this(buf.readVarInt());
	}

	private void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeVarInt(this.playerId);
	}

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
