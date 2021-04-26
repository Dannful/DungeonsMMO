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
import me.dannly.party.party.Party
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import me.dannly.dungeonsmmo.utils.Logging
import me.dannly.dungeonsmmo.utils.PlayerLoc

class DungeonEvents : Listener {
    @EventHandler
    fun teleport(event: EntityTeleportEvent) {
        val entity = event.entity as? LivingEntity ?: return
        if (Dungeon.isDungeonEntity(entity)) event.isCancelled = true
    }

    @EventHandler
    fun join(event: PlayerJoinEvent) {
        val player = event.player
        val playerLoc: PlayerLoc? = PlayerLoc.getPlayerLoc(player.uniqueId)
        if (playerLoc != null) {
            val loc = playerLoc.loc
            player.teleport(loc)
            playerLoc.remove()
        }
    }

    @EventHandler
    fun die(event: EntityDamageByEntityEvent) {
        val entity = event.entity as? Player ?: return
        val instance: DungeonInstance? = Dungeon.getInstance(entity)
        if (instance != null) {
            if (entity.health - event.finalDamage <= 0.0) {
                event.damage = 0.0
                entity.gameMode = GameMode.SPECTATOR
                if (instance.party.onlinePlayers.stream()
                        .noneMatch { it.gameMode != GameMode.SPECTATOR }
                ) instance.end(true, false)
            }
        }
    }

    @EventHandler
    fun quit(event: PlayerQuitEvent) {
        val player = event.player
        if (Dungeon.isInDungeon(player)) Dungeon.getInstance(player)!!.end(countdown = false, false)
    }

    @EventHandler
    fun command(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        if(Dungeon.isInDungeon(player) || Party.getParty(player.uniqueId)?.onlinePlayers?.any { Dungeon.isInDungeon(it) } == true) {
            Logging.log(player, "@in-dungeon-cancel-command@")
            event.isCancelled = true
        }
    }

    @EventHandler
    fun move(event: PlayerMoveEvent) {
        val p = event.player
        val instance: DungeonInstance? = Dungeon.getInstance(p)
        if (p.gameMode == GameMode.SPECTATOR) return
        if (instance != null) {
            val distance: Double =
                Main.instance.config.getDouble("dungeon-entity-activate-ai-distance", 5.0)
            for (nearbyEntity in p.getNearbyEntities(distance, distance, distance)) {
                if (nearbyEntity is LivingEntity) {
                    if (instance.spawnedEntities.containsValue(nearbyEntity)) {
                        if (!nearbyEntity.hasAI()) {
                            nearbyEntity.setAI(true)
                            nearbyEntity.isInvulnerable = false
                        }
                    }
                }
            }
            val aDouble: Double =
                Main.instance.config.getDouble("dungeon-entity-block-respawn-distance", 0.0)
            if (aDouble > 0 && p.getNearbyEntities(aDouble, aDouble, aDouble).isEmpty()) {
                for (player in instance.clearing) {
                    if (player.gameMode != GameMode.SPECTATOR) for (value in instance.spawnedEntities.values) if (value is Mob && value.target is Player) return
                    if (player.gameMode == GameMode.SPECTATOR) {
                        player.teleport(p)
                        player.gameMode = GameMode.SURVIVAL
                        player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
                    }
                }
            }
        }
    }

    @EventHandler
    fun death(event: EntityDeathEvent) {
        val livingEntity = event.entity
        val dungeonEntity: DungeonEntity? = Dungeon.getDungeonEntity(livingEntity)
        if (dungeonEntity != null) {
            val dungeon: Dungeon = Dungeon.getDungeon(livingEntity)!!
            if (dungeon.boss != null && dungeon.boss == dungeonEntity.id) {
                Dungeon.getInstance(livingEntity)!!.end(countdown = true, success = true)
            }
        }
    }
}