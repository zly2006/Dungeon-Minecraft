package com.github.zly2006.dungeon.ws

import com.github.zly2006.dungeon.DAMAGE_AMPLIFIER
import com.github.zly2006.dungeon.net.DungeonPackets
import com.github.zly2006.dungeon.ws.ConnectionData.Companion.connectionData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.*
import kotlin.math.ceil

class DungeonWSServer(
    val server: MinecraftServer,
) : WebSocketServer(InetSocketAddress("0.0.0.0", 2060)) {
    @Serializable
    class DungeonData(
        val type: String,
        val clientId: String,
        val targetId: String,
        val message: String,
    ) {
        companion object {
            fun setStrength(data: ConnectionData, channel: String, strength: Int): DungeonData {
                val c = when (channel) {
                    "A" -> 1
                    "B" -> 2
                    else -> error("Unexpected channel type $channel")
                }
                val s = when (strength) {
                    in 0..200 -> strength
                    else -> error("Unexpected strength value $strength")
                }
                return DungeonData(
                    "msg",
                    data.clientId,
                    data.targetId,
                    "strength-$c+2+$s"
                )
            }

            val pulse = listOf(
                "4A4A4A4A64646464",
                "4545454564646464",
                "4040404064646464",
                "3B3B3B3B64646464",
                "3636363664646464",
                "3232323264646464",
                "2D2D2D2D64646464",
                "2828282864646464",
                "2323232364646464",
                "1E1E1E1E64646464",
                "1A1A1A1A64646464"
            )
        }
    }

    fun WebSocket.send(data: DungeonData) {
        send(Json.encodeToString(data))
        if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            connectionData.filterValues { it.webSocket === this }.forEach {
                it.value.player?.sendMessage(Text.literal("郊狼send:" + Json.encodeToString(data)))
            }
        }
    }

    fun setStrength(data: ConnectionData, channel: String, strength: Int) {
        val strength = strength.coerceIn(
            0,
            if (channel == "A") data.maxStrengthA else data.maxStrengthB
        )
        data.webSocket.send(DungeonData.setStrength(data, channel, strength))
        if (channel == "A") {
            data.lastStrengthA = strength
        }
        else {
            data.lastStrengthB = strength
        }
        data.lastServerTime = System.currentTimeMillis()
    }

    fun tick() {
        connectionData.values.forEach {
            if (it.lastServerTime + 500 < System.currentTimeMillis()) {
                if (it.lastStrengthA > 0) {
                    val decrease = it.maxStrengthA / 6
                    val strength = maxOf(it.lastStrengthA - decrease, 0)
                    setStrength(it, "A", strength)
                    it.lastStrengthA = strength
                    it.lastServerTime = System.currentTimeMillis()
                }
                if (it.lastStrengthB > 0) {
                    val decrease = it.maxStrengthB / 6
                    val strength = maxOf(it.lastStrengthB - decrease, 0)
                    setStrength(it, "B", strength)
                    it.lastStrengthB = strength
                    it.lastServerTime = System.currentTimeMillis()
                }
            }
//            if (it.lastStrengthA == 0 && it.lastStrengthB == 0 &&
//                it.lastSyncTime + 5000 < it.lastServerTime
//            ) {
//                it.player?.networkHandler?.disconnect(Text.literal("郊狼已断开连接"))
//                connectionData.remove(it.targetId)
//            }
        }
    }

    override fun onOpen(p0: WebSocket, p1: ClientHandshake) {
        try {
            val uuid = UUID.fromString(p1.resourceDescriptor.trimStart('/'))
            // 绑定 SYN
            p0.send(DungeonData("bind", uuid.toString(), "", "targetId"))
        } catch (_: Exception) {
            // 非法 url
            p0.send(DungeonData("bind", "invalid", "", "targetId"))
            p0.send(DungeonData("bind", "", "invalid", "401"))
        }
    }

    override fun onClose(p0: WebSocket, p1: Int, p2: String, p3: Boolean) {
        println("Connection closed${p0.remoteSocketAddress} with exit code $p1 additional info: $p2")
        connectionData.filterValues { it.webSocket == p0 }.forEach {
            it.value.player?.sendMessage(Text.literal("郊狼已断开连接，重进服务器以重新连接。"))
            connectionData.remove(it.key)
        }
    }

    fun onData(data: DungeonData, p0: WebSocket) {
        // 绑定 ACK
        if (data.type == "bind" && data.message == "DGLAB") {
            println("Bind ACK from ${p0.remoteSocketAddress}")
            // target id 没有绑定，或者绑定的是当前连接
            if (data.targetId !in connectionData || p0 == connectionData[data.targetId]?.webSocket) {
                connectionData[data.targetId] = ConnectionData(data.clientId, data.targetId, p0)
                val uuid = UUID.fromString(data.targetId)
                server.execute {
                    val player = server.playerManager.getPlayer(uuid)
                    connectionData[data.targetId]?.player = player
                    player?.sendMessage(Text.literal("郊狼绑定成功"))
                }
                p0.send(
                    DungeonData(
                        "bind",
                        data.clientId,
                        data.targetId,
                        "200"
                    )
                )
            }
            else {
                // target id already bound
                p0.send(
                    DungeonData(
                        "bind",
                        data.clientId,
                        data.targetId,
                        "400"
                    )
                )
            }
        }
        if (data.type == "msg" && data.message.startsWith("strength-")) {
            val strength = data.message.substringAfter("strength-").split("+")
            val cd = connectionData[data.targetId]
            cd?.lastStrengthA = strength[0].toInt()
            cd?.lastStrengthB = strength[1].toInt()
            cd?.lastSyncTime = System.currentTimeMillis()
            cd?.maxStrengthA = strength[2].toInt()
            cd?.maxStrengthB = strength[3].toInt()
            cd?.player?.let {
                ServerPlayNetworking.send(
                    it, DungeonPackets.StrengthSyncPacket(
                        cd.lastStrengthA, cd.lastStrengthB, cd.lastSyncTime, cd.maxStrengthA, cd.maxStrengthB
                    )
                )
            }
            // 强度上报
        }

    }

    override fun onMessage(p0: WebSocket, p1: String) {
        println("From ${p0.remoteSocketAddress}: $p1")
        onData(Json.Default.decodeFromString<DungeonData>(p1), p0)
    }

    override fun onError(p0: WebSocket?, p1: Exception?) {

    }

    override fun onStart() {
        GlobalScope.launch {
            while (true) {
                delay(1000)
                connectionData.forEach {
                    val p0 = it.value.webSocket
                    p0.send(
                        DungeonData(
                            "msg",
                            it.value.clientId,
                            it.value.targetId,
                            "pulse-A:" + Json.encodeToJsonElement(DungeonData.pulse)
                        )
                    )
                    p0.send(
                        DungeonData(
                            "msg",
                            it.value.clientId,
                            it.value.targetId,
                            "pulse-B:" + Json.encodeToJsonElement(DungeonData.pulse)
                        )
                    )
                }
            }
        }
    }

    override fun stop() {
        connectionData.forEach {
            runCatching {
                it.value.webSocket.send(DungeonData("bind", it.value.clientId, it.value.targetId, "500"))
            }
        }
        connectionData.clear()
        super.stop()
    }

    fun onDamage(player: ServerPlayerEntity, source: DamageSource, amount: Float) {
        if (player.uuidAsString in connectionData) {
            val data = connectionData[player.uuidAsString]!!
            setStrength(
                data,
                "A",
                ceil(amount / 20 * data.maxStrengthA * DAMAGE_AMPLIFIER).toInt()
            )
        }
    }

    fun onDeath(player: ServerPlayerEntity, source: DamageSource) {
        if (player.uuidAsString in connectionData) {
            val data = connectionData[player.uuidAsString]!!
            setStrength(
                data,
                "A",
                data.maxStrengthA
            )
        }
    }
}
