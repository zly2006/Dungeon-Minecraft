package com.github.zly2006.dungeon.ws

import net.minecraft.server.network.ServerPlayerEntity
import org.java_websocket.WebSocket

class ConnectionData(
    val clientId: String,
    val targetId: String,
    val webSocket: WebSocket,
) {
    var player: ServerPlayerEntity? = null
    var maxStrengthA: Int = 0
    var maxStrengthB: Int = 0
    var lastSyncTime: Long = 0
    var lastServerTime: Long = 0
    var lastStrengthA: Int = 0
    var lastStrengthB: Int = 0
    companion object {
        // target id to connection data
        val connectionData = mutableMapOf<String, ConnectionData>()
    }
}
