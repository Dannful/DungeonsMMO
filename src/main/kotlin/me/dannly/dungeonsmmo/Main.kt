/*
 * Copyright 2021 Dannly
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to use
 * and distribute it as a library or an Application Programming Interface without
 * billing purposes.
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package me.dannly.dungeonsmmo

import me.dannly.dungeonsmmo.dungeon.Dungeon
import me.dannly.dungeonsmmo.dungeon.DungeonCommand
import me.dannly.dungeonsmmo.dungeon.DungeonEntity
import me.dannly.dungeonsmmo.dungeon.DungeonEvents
import me.dannly.dungeonsmmo.utils.PlayerLoc
import me.dannly.inventories.InventoryListener
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Main : JavaPlugin() {
    companion object {
        val instance: Main
            get() = getPlugin(Main::class.java)

        fun getConfig(key: String): String {
            return ChatColor.translateAlternateColorCodes(
                '&', instance.config.getString(
                    key, ""
                )!!
            )
        }

        fun saveDungeons() {
            val dungeonsFile = File(instance.dataFolder, "dungeons.yml")
            val dungeonsYaml = YamlConfiguration.loadConfiguration(dungeonsFile)
            dungeonsYaml["dungeons"] = Dungeon.dungeons
            dungeonsYaml.save(dungeonsFile)
        }

        fun saveLocations() {
            val locations: MutableList<PlayerLoc> = PlayerLoc.locations
            val locationsFile = File(instance.dataFolder, "locations.yml")
            val locationsYaml = YamlConfiguration.loadConfiguration(locationsFile)
            locationsYaml["locations"] = locations
            locationsYaml.save(locationsFile)
        }

        init {
            ConfigurationSerialization.registerClass(DungeonEntity::class.java, "DungeonEntity")
            ConfigurationSerialization.registerClass(Dungeon::class.java, "Dungeon")
            ConfigurationSerialization.registerClass(PlayerLoc::class.java, "PlayerLocation")
        }
    }

    override fun onEnable() {
        if(server.pluginManager.getPlugin("PartyMMO") == null) {
            println("${ChatColor.YELLOW}[${description.name}] Disabling plugin: PartyMMO not found!")
            server.pluginManager.disablePlugin(this)
            return
        }
        saveDefaultConfig()
        server.pluginManager.registerEvents(InventoryListener(), this)
        server.pluginManager.registerEvents(DungeonEvents(), this)
        getCommand("dungeon")!!.setExecutor(DungeonCommand())
        getCommand("dungeon")!!.tabCompleter = DungeonCommand()
        setupFiles()
    }

    override fun onDisable() {}

    @Suppress("UNCHECKED_CAST")
    private fun setupFiles() {
        run {
            val file = File(dataFolder, "dungeons.yml")
            try {
                if (!file.exists()) file.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        run {
            val file = File(dataFolder, "locations.yml")
            try {
                if (!file.exists()) file.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val dungeons = YamlConfiguration.loadConfiguration(File(dataFolder, "dungeons.yml"))
        val locations = YamlConfiguration.loadConfiguration(File(dataFolder, "locations.yml"))
        Dungeon.dungeons.addAll(dungeons["dungeons", mutableListOf<Dungeon>()] as MutableList<Dungeon>)
        PlayerLoc.setPlayerLocs(locations["locations", mutableListOf<PlayerLoc>()] as MutableList<PlayerLoc>)
    }
}