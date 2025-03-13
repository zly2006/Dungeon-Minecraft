package com.github.zly2006.dungeon.client

import com.github.zly2006.dungeon.net.DungeonPackets
import com.github.zly2006.dungeon.net.generateQRCode
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Util
import java.io.File

object DungeonClient : ClientModInitializer {
    var syncPacket: DungeonPackets.StrengthSyncPacket? = null

    override fun onInitializeClient() {
        DungeonPackets.registerClient()

        HudRenderCallback.EVENT.register { context, _ ->
            val client = MinecraftClient.getInstance()
            if (syncPacket != null) {
                context.matrices.push()
                context.drawText(
                    client.textRenderer,
                    "郊狼 A: ${syncPacket!!.strengthA}/${syncPacket!!.maxStrengthA}",
                    10,
                    10,
                    0xFFFFFF,
                    true
                )
                context.drawText(
                    client.textRenderer,
                    "郊狼 B: ${syncPacket!!.strengthB}/${syncPacket!!.maxStrengthB}",
                    10,
                    20,
                    0xFFFFFF,
                    true
                )
                context.matrices.pop()
            }
        }
    }

    fun onQR(packet: DungeonPackets.WebsocketJoinPacket) {
        generateQRCode(
            "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#" + packet.serverAddr,
            "qr.png"
        )
        Util.getOperatingSystem().open(File("qr.png"))
    }
}
