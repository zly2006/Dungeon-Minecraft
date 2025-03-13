package com.github.zly2006.dungeon

import com.github.zly2006.dungeon.net.DungeonPackets
import com.github.zly2006.dungeon.ws.ConnectionData
import com.github.zly2006.dungeon.ws.DungeonWSServer
import com.github.zly2006.untitled6.ktdsl.register
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import java.net.DatagramSocket
import java.net.InetAddress

object Dungeon : ModInitializer {
    lateinit var socketAddress: String
    lateinit var webSocketServer: DungeonWSServer

    override fun onInitialize() {
        val ip = DatagramSocket().use { socket ->
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
            socket.localAddress.hostAddress
        }
        socketAddress = "ws://$ip:2060"
        DungeonPackets.registerServer()
        ServerLifecycleEvents.SERVER_STARTED.register {
            webSocketServer = DungeonWSServer(it)
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
