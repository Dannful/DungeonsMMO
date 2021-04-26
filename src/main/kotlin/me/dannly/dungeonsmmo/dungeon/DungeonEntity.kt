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

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs
import org.bukkit.util.Vector
import me.dannly.dungeonsmmo.utils.Data
import java.util.*

@SerializableAs("DungeonEntity")
class DungeonEntity : ConfigurationSerializable {
    var customName: String? = null
    var spawnLocation: Vector? = null
    var command: String? = null
    var id: UUID = UUID.randomUUID()

    constructor(map: Map<String, Any>) {
        Data.set<Vector>(map, "spawnLocation") { spawnLocation = it }
        Data.set<String>(map, "customName") { customName = it }
        Data.set<String>(map, "command") { command = it }
        Data.set<String>(map, "id") { id = UUID.fromString(it) }
    }

    constructor()

    override fun serialize(): MutableMap<String, Any> {
        val map: MutableMap<String, Any> = mutableMapOf()
        Data.insertIntoMap(map, "spawnLocation", spawnLocation)
        Data.insertIntoMap(map, "customName", customName)
        Data.insertIntoMap(map, "command", command)
        map["id"] = id.toString()
        return map
    }
}