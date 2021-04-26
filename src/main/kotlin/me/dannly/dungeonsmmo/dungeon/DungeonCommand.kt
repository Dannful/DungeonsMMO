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
import me.dannly.dungeonsmmo.dungeon.DungeonInstance.QueueResult
import me.dannly.party.party.Party
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import me.dannly.dungeonsmmo.utils.Logging
import me.dannly.dungeonsmmo.utils.Utils

class DungeonCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            Logging.log(sender, "@player-only@")
            return true
        }
        if (label.equals("dungeon", ignoreCase = true)) {
            if (args.isEmpty()) {
                sender.chat("/$label help")
                return true
            }
            if (args[0].equals("help", ignoreCase = true)) {
                sender.sendMessage(
                    Logging.getMessage("@help-command@").replace(
                        "%c".toRegex(), "$label ${
                            Utils.getListForPlayer(
                                sender,
                                "create:" + Main.getConfig("dungeon-manage"),
                                "list",
                                "queue",
                                "help"
                            ).joinToString("\n$label ")
                        }"
                    )
                )
            } else if (args[0].equals("create", ignoreCase = true)) {
                if (sender.hasPermission(Main.getConfig("dungeon-manage"))) {
                    val dungeonName = ChatColor.translateAlternateColorCodes(
                        '&',
                        args.copyOfRange(1, args.size).joinToString(" ")
                    )
                    if (dungeonName.isEmpty()) {
                        Logging.log(sender, "@dungeon-name-empty@")
                        return true
                    }
                    if (Dungeon.dungeons.find { it.dungeonName.equals(dungeonName, true) } != null) {
                        Logging.log(sender, "@dungeon-create-name-in-use@")
                        return true
                    }
                    Dungeon(dungeonName, Material.SPAWNER)
                    sender.sendMessage(
                        Logging.getMessage("@dungeon-named@").replace("%n".toRegex(), dungeonName)
                    )
                }
            } else if (args[0].equals("list", ignoreCase = true)) {
                if (!sender.hasPermission(Main.getConfig("dungeon-manage"))) {
                    Logging.log(
                        sender,
                        "Dungeons: " + Dungeon.dungeons.joinToString(", ")
                    )
                } else {
                    DungeonInventory.setupInventory(sender)
                }
            } else if (args[0].equals("queue", ignoreCase = true)) {
                if (args.size < 2) {
                    sender.chat("/$label help")
                    return false
                }
                val dungeon = Dungeon.dungeons.find { it.dungeonName.equals(args[1], true) }
                if (dungeon == null) {
                    Logging.log(sender, "@dungeon-not-found@")
                    return true
                }
                val party: Party = Party.getParty(sender.uniqueId) ?: Party(sender)
                when (DungeonInstance(party, dungeon).start()) {
                    QueueResult.NO_SPAWN_LOCATION -> Logging.log(sender, "@no-spawn-set-message@")
                    QueueResult.NOT_ENOUGH_PLAYERS -> Logging.log(sender, "@not-enough-players-message@")
                    QueueResult.NO_BOSS -> Logging.log(sender, "@no-boss-message@")
                    QueueResult.TOO_MANY_PLAYERS -> Logging.log(sender, "@too-many-players-message@")
                    QueueResult.SUCCESS -> {
                    }
                }
            } else {
                sender.chat("/$label help")
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        if (label.equals("dungeon", ignoreCase = true)) {
            if (args.size == 1) {
                return Utils.getListForPlayer(
                    sender,
                    "create:" + Main.getConfig("dungeon-manage"),
                    "list",
                    "queue"
                ).filter {
                    it.toLowerCase().startsWith(
                        args[0].toLowerCase()
                    )
                }
            } else if (args.size == 2) {
                if (args[0].equals("queue", ignoreCase = true)) {
                    return Dungeon.dungeons.map { it.dungeonName }
                        .filter { it.toLowerCase().startsWith(args[1].toLowerCase()) }
                }
            }
        }
        return listOf()
    }
}