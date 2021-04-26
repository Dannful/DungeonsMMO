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

package me.dannly.dungeonsmmo.dungeon

import me.dannly.dungeonsmmo.Main
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import me.dannly.dungeonsmmo.utils.Data
import java.util.*

@SerializableAs("Dungeon")
class Dungeon : ConfigurationSerializable {
    val dungeonEntities = mutableListOf<DungeonEntity>()
    val dungeonName: String
    var icon: Material
    val points = LinkedList<Location>()
    val dungeonInstances = mutableListOf<DungeonInstance>()
    val spawnPoints = mutableListOf<Location>()
    var minimumPartySize = 1
    var maximumPartySize = 10
    var duration = 30.0
    var boss: UUID? = null

    constructor(dungeonName: String, icon: Material) {
        this.dungeonName = dungeonName
        this.icon = icon
        dungeons.add(this)
        Main.saveDungeons()
    }

    constructor(map: Map<String, Any>) {
        Data.set<List<Location>>(map, "spawnPoints") { spawnPoints.addAll(it) }
        Data.set<List<DungeonEntity>>(map, "entities") { dungeonEntities.addAll(it) }
        minimumPartySize = map["minimumPartySize"] as Int
        maximumPartySize = map["maximumPartySize"] as Int
        icon = Material.valueOf(map["icon"] as String)
        dungeonName = map["name"] as String
        Data.set<String>(map, "boss") { if(it != "null") boss = UUID.fromString(it) }
        duration = map["duration"] as Double
    }

    fun add(dungeonInstance: DungeonInstance): Boolean {
        return dungeonInstances.add(dungeonInstance)
    }

    fun remove(o: DungeonInstance): Boolean {
        return dungeonInstances.remove(o)
    }

    fun addSpawnPoint(spawnPoint: Location) {
        spawnPoints.add(spawnPoint)
    }


    fun addEntity(dungeonEntity: DungeonEntity) {
        dungeonEntities.add(dungeonEntity)
    }

    fun removeEntity(dungeonEntity: DungeonEntity) {
        dungeonEntities.remove(dungeonEntity)
    }

    fun delete() {
        removeDungeon(this)
    }

    override fun serialize(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["name"] = dungeonName
        map["icon"] = icon.name
        map["spawnPoints"] = spawnPoints.toList()
        map["minimumPartySize"] = minimumPartySize
        map["entities"] = dungeonEntities.toList()
        map["boss"] = boss.toString()
        map["maximumPartySize"] = maximumPartySize
        map["duration"] = duration
        return map.toMap()
    }

    override fun toString(): String {
        return dungeonName
    }

    companion object {
        val dungeons = mutableListOf<Dungeon>()
        fun getDungeon(entity: LivingEntity): Dungeon? {
            return dungeons.find { it.dungeonInstances.find { o -> o.spawnedEntities.containsValue(entity) } != null }
        }

        fun getInstance(player: Player): DungeonInstance? {
            dungeons.forEach {
                return it.dungeonInstances.find { i -> i.clearing.contains(player) }
            }
            return null
        }

        fun isInDungeon(player: Player): Boolean {
            return getInstance(player) != null
        }

        fun getInstance(entity: LivingEntity): DungeonInstance? {
            return getDungeon(entity)?.dungeonInstances?.singleOrNull { it.spawnedEntities.containsValue(entity) }
        }

        fun getDungeonEntity(entity: LivingEntity): DungeonEntity? {
            dungeons.forEach {
                it.dungeonInstances.map { i -> i.spawnedEntities }.forEach { h ->
                    return h.keys.find { a -> h[a] == entity }
                }
            }
            return null
        }

        fun isDungeonEntity(entity: LivingEntity): Boolean {
            return getDungeon(entity) != null
        }

        private fun removeDungeon(dungeon: Dungeon) {
            dungeon.dungeonEntities.clear()
            dungeon.points.clear()
            dungeon.spawnPoints.clear()
            dungeon.dungeonInstances.forEach { it.end(countdown = false, success = false) }
            dungeon.dungeonInstances.clear()
            dungeons.remove(dungeon)
            Main.saveDungeons()
        }

        fun getDungeonByName(name: String): Dungeon? {
            return dungeons.singleOrNull { it.dungeonName == name }
        }
    }
}