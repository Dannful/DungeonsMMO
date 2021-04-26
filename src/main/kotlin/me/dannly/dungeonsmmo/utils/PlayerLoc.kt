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

package me.dannly.dungeonsmmo.utils

import me.dannly.dungeonsmmo.Main
import org.bukkit.Location
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs
import java.util.*

@SerializableAs("PlayerLocation")
class PlayerLoc : ConfigurationSerializable {
    var id: UUID
    var loc: Location

    fun remove() {
        locations.remove(this)
        Main.saveLocations()
    }

    constructor(id: UUID, loc: Location) {
        this.id = id
        this.loc = loc
        locations.add(this)
        Main.saveLocations()
    }

    override fun serialize(): Map<String, Any> {
        val map: MutableMap<String, Any> = HashMap()
        map["id"] = id.toString()
        map["location"] = loc
        return map
    }

    private constructor(map: Map<String, Any>) {
        id = UUID.fromString(map["id"] as String)
        loc = map["location"] as Location
    }

    companion object {
        var locations = mutableListOf<PlayerLoc>()
            private set

        fun setPlayerLocs(playerLocs: List<PlayerLoc>) {
            locations = playerLocs.toMutableList()
        }

        fun getPlayerLoc(id: UUID): PlayerLoc? {
            return locations.find { it.id == id }
        }

        fun deserialize(map: Map<String, Any>): PlayerLoc {
            return PlayerLoc(map)
        }
    }
}