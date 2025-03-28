package com.github.zly2006.dungeon.config

import com.google.gson.Gson
import net.fabricmc.loader.api.FabricLoader
import java.io.File

data class DungeonConfig(
    val websocketPort: Int = 2060,
    val damageAmplifier: Float = 5f,
) {
    companion object {
        private val configFile: File = FabricLoader.getInstance().configDir.resolve("dungeon.json").toFile()
        private val gson = Gson()

        fun load(): DungeonConfig {
            if (!configFile.exists()) {
                save(DungeonConfig())
            }
            return gson.fromJson(configFile.reader(), DungeonConfig::class.java)
        }

        private fun save(config: DungeonConfig) {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(config))
        }
    }
}
