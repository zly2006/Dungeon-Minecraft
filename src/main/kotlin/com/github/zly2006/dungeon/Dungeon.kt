package com.github.zly2006.dungeon

import com.github.zly2006.dungeon.config.DungeonConfig
import com.github.zly2006.dungeon.net.DungeonPackets
import com.github.zly2006.dungeon.ws.ConnectionData
import com.github.zly2006.dungeon.ws.DungeonWSServer
import com.github.zly2006.untitled6.ktdsl.register
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.Text
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL

object Dungeon : ModInitializer {
    lateinit var socketAddress: String
    lateinit var webSocketServer: DungeonWSServer
    private lateinit var config: DungeonConfig

    override fun onInitialize() {
        val ip = if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            DatagramSocket().use { socket ->
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
                socket.localAddress.hostAddress
            }
        }
        else {
            val string =
                URL("https://redenmc.com/api/ip").openConnection().inputStream.readBytes().decodeToString()
            val ip = Gson().fromJson(string, JsonObject::class.java).get("ip").asString
            ip
        }
        config = DungeonConfig.load()
        val port = config.websocketPort
        socketAddress = "ws://$ip:$port"
        DungeonPackets.registerServer()
        ServerLifecycleEvents.SERVER_STARTED.register {
            webSocketServer = DungeonWSServer(it, config.websocketPort)
            webSocketServer.start()
            Runtime.getRuntime().addShutdownHook(Thread {
                webSocketServer.stop()
            })
        }
        ServerLifecycleEvents.SERVER_STOPPING.register {
            webSocketServer.stop()
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, access, _ ->
            dispatcher.register {
                literal("dg") {
                    literal("net-info") {
                        executes {
                            val player = it.source.player!!
                            val connectionData = ConnectionData.connectionData[player.uuidAsString]
                            player.sendMessage(Text.literal("IP: $ip"))
                            if (connectionData != null) {
                                player.sendMessage(Text.literal("Client ID: ${connectionData.clientId}"))
                                player.sendMessage(Text.literal("Target ID: ${connectionData.targetId}"))
                            } else {
                                player.sendMessage(Text.literal("Not connected"), false)
                            }
                            1
                        }
                    }
                    literal("set-strength") {
                        argument("channel", StringArgumentType.word()) {
                            argument("strength", IntegerArgumentType.integer(0, 200)) {
                                executes {
                                    val player = it.source.player!!
                                    val channel = it.getArgument("channel", String::class.java)
                                    val strength = it.getArgument("strength", Int::class.java)
                                    webSocketServer.setStrength(
                                        ConnectionData.connectionData[player.uuidAsString]!!,
                                        channel, strength
                                    )
                                    1
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
