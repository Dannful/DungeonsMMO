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
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import me.dannly.dungeonsmmo.utils.Logging
import me.dannly.dungeonsmmo.utils.PlayerLoc
import me.dannly.dungeonsmmo.utils.RunnableAPI
import me.dannly.party.party.Party
import java.util.function.Consumer

class DungeonInstance(val party: Party, val dungeon: Dungeon) {

    val spawnedEntities = mutableMapOf<DungeonEntity, LivingEntity>()
    val clearing: List<Player>
        get() = party.onlinePlayers
    lateinit var spawnLocation: Location

    fun start(): QueueResult {
        if (dungeon.dungeonInstances.size - 1 == 0) dungeon.points.addAll(dungeon.spawnPoints)
        val poll = dungeon.points.poll() ?: return QueueResult.NO_SPAWN_LOCATION
        if (clearing.size < dungeon.minimumPartySize) return QueueResult.NOT_ENOUGH_PLAYERS
        if (clearing.size > dungeon.maximumPartySize) return QueueResult.TOO_MANY_PLAYERS
        if (dungeon.boss == null) return QueueResult.NO_BOSS
        spawnLocation = poll
        clearing.forEach {
            PlayerLoc(it.uniqueId, it.location)
            it.teleport(poll)
        }
        Main.instance.config.getStringList("dungeon-start").forEach {
            if (it.contains("%s")) {
                for (onlinePlayer in clearing) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.replace("%s".toRegex(), onlinePlayer.name))
                }
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it)
            }
        }
        for (dungeonEntity in dungeon.dungeonEntities.toList()) {
            if (dungeon.boss == dungeonEntity.id && (dungeonEntity.command == null || dungeonEntity.spawnLocation == null)) return QueueResult.NO_BOSS
            if (dungeonEntity.spawnLocation == null) continue
            val spawnLocation = poll.clone().add(dungeonEntity.spawnLocation!!)
            if (dungeonEntity.command == null) continue
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(), dungeonEntity.command!!
                    .replace(
                        "%l".toRegex(),
                        spawnLocation.world!!.name + "," + spawnLocation.x + "," + spawnLocation.y + "," + spawnLocation.z + "," + spawnLocation.yaw + "," + spawnLocation.pitch
                    )
            )
            val livingEntityList = spawnLocation.world!!.getEntitiesByClass(
                LivingEntity::class.java
            )
                .filter { it.location.blockX == spawnLocation.blockX && it.location.blockY == spawnLocation.blockY && it.location.blockZ == spawnLocation.blockZ }
            if (livingEntityList.isEmpty()) continue
            val livingEntity = livingEntityList[0]
            livingEntity.setAI(false)
            livingEntity.isInvulnerable = true
            livingEntity.canPickupItems = false
            if (dungeonEntity.customName != null) {
                livingEntity.customName = dungeonEntity.customName
                livingEntity.isCustomNameVisible = true
            }
            spawnedEntities[dungeonEntity] = livingEntity
        }
        RunnableAPI.startRunnable("me/dannly/dungeon" + dungeon.dungeonInstances.indexOf(this), object : BukkitRunnable() {
            var i = (dungeon.duration * 60 * 20).toInt()
            override fun run() {
                val i = i / 20 / 60
                if (i % 5 == 0)
                    clearing
                        .forEach {
                            it.sendMessage(
                                Logging.getMessage("@dungeon-time-message@").replace("%t".toRegex(), i.toString())
                            )
                        }
                this.i--
                if (this.i <= 0) {
                    end(countdown = false, success = false)
                    RunnableAPI.cancelRunnable(this)
                }
            }
        }, Main.instance, 0, 20)
        return QueueResult.SUCCESS
    }

    private fun getIdentifier(end: Boolean): String {
        return (if (end) "end" else "me/dannly/dungeon") + dungeon.dungeonInstances.indexOf(this)
    }

    fun end(countdown: Boolean = true, success: Boolean = false) {
        if (!::spawnLocation.isInitialized)
            return
        if (RunnableAPI.isRunning(getIdentifier(false))) RunnableAPI.cancelRunnable(getIdentifier(false))
        spawnedEntities.values.forEach { it.remove() }
        spawnedEntities.clear()
        val instance: Main = Main.instance
        val dungeonInstance = this
        val anInt = instance.config.getInt("dungeon-end-countdown-seconds", 20)
        if (countdown) clearing.forEach {
            it.sendMessage(
                Logging.getMessage(
                    "@dungeon-end-countdown-message@"
                ).replace("%t".toRegex(), anInt.toString())
            )
        }
        RunnableAPI.startRunnable(getIdentifier(true), object : BukkitRunnable() {
            override fun run() {
                if (success) instance.config.getStringList("dungeon-end").forEach(Consumer { s: String ->
                    if (s.contains("%s")) {
                        for (onlinePlayer in clearing) {
                            Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                s.replace("%s".toRegex(), onlinePlayer.name)
                            )
                        }
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s)
                    }
                })
                dungeon.points.add(spawnLocation)
                dungeon.remove(dungeonInstance)
                clearing.forEach {
                    val playerLoc: PlayerLoc? = PlayerLoc.getPlayerLoc(it.uniqueId)
                    if (playerLoc != null) {
                        it.gameMode = GameMode.SURVIVAL
                        it.health = it.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
                        it.teleport(playerLoc.loc)
                        playerLoc.remove()
                    }
                }
            }
        }, instance, (if (countdown) anInt * 20 else 0).toLong())
    }

    enum class QueueResult {
        NOT_ENOUGH_PLAYERS, NO_SPAWN_LOCATION, NO_BOSS, SUCCESS, TOO_MANY_PLAYERS
    }

    init {
        dungeon.add(this)
    }
}