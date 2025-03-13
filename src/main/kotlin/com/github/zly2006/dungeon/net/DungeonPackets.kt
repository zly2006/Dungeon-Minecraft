package com.github.zly2006.dungeon.net

import com.github.zly2006.dungeon.Dungeon.socketAddress
import com.github.zly2006.dungeon.client.DungeonClient
import com.github.zly2006.dungeon.ws.ConnectionData
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.world.GameMode

object DungeonPackets {
    data class TradePayload(val index: Int, val tradeAll: Boolean) : CustomPayload {
        override fun getId() = ID

        companion object {
            val ID = CustomPayload.Id<TradePayload>(Identifier.of("dungeon", "trade"))
            val CODEC = CustomPayload.codecOf<PacketByteBuf, TradePayload>({ data, buf ->
                buf.writeVarInt(data.index)
                buf.writeBoolean(data.tradeAll)
            }, { buf ->
                TradePayload(buf.readVarInt(), buf.readBoolean())
            })!!
        }
    }

    data class WebsocketJoinPacket(val serverAddr: String) : CustomPayload {
        override fun getId() = ID

        companion object {
            val ID = CustomPayload.Id<WebsocketJoinPacket>(Identifier.of("dungeon", "websocket"))
            val CODEC = CustomPayload.codecOf<PacketByteBuf, WebsocketJoinPacket>({ data, buf ->
                buf.writeString(data.serverAddr)
            }, { buf ->
                WebsocketJoinPacket(buf.readString())
            })!!
        }
    }

    data class StrengthSyncPacket(
        val strengthA: Int,
        val strengthB: Int,
        val lastSyncTime: Long,
        val maxStrengthA: Int,
        val maxStrengthB: Int,
    ) : CustomPayload {
        override fun getId() = ID

        companion object {
            val ID = CustomPayload.Id<StrengthSyncPacket>(Identifier.of("dungeon", "strength_sync"))
            val CODEC = CustomPayload.codecOf<PacketByteBuf, StrengthSyncPacket>({ data, buf ->
                buf.writeVarInt(data.strengthA)
                buf.writeVarInt(data.strengthB)
                buf.writeLong(data.lastSyncTime)
                buf.writeVarInt(data.maxStrengthA)
                buf.writeVarInt(data.maxStrengthB)
            }, { buf ->
                StrengthSyncPacket(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readLong(),
                    buf.readVarInt(),
                    buf.readVarInt()
                )
            })!!
        }
    }

    fun registerServer() {
        PayloadTypeRegistry.playC2S().register(TradePayload.ID, TradePayload.CODEC)
        PayloadTypeRegistry.playS2C().register(WebsocketJoinPacket.ID, WebsocketJoinPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(StrengthSyncPacket.ID, StrengthSyncPacket.CODEC)
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            val uuid = handler.player.uuidAsString
            if (uuid !in ConnectionData.connectionData) {
                val url = "$socketAddress/$uuid"
                sender.sendPacket(
                    WebsocketJoinPacket(
                        url
                    )
                )
                handler.player.changeGameMode(GameMode.SPECTATOR)
                handler.player.sendMessage(net.minecraft.text.Text.literal("请扫描二维码加入游戏"))
            }
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(WebsocketJoinPacket.ID) { packet, ctx ->
            DungeonClient.onQR(packet)
        }
        ClientPlayNetworking.registerGlobalReceiver(StrengthSyncPacket.ID) { packet, ctx ->
            DungeonClient.syncPacket = packet
        }
    }
}
