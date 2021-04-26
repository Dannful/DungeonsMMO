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

import com.sun.istack.internal.NotNull
import me.dannly.dungeonsmmo.Main
import me.dannly.dungeonsmmo.utils.InventoryLib
import me.dannly.dungeonsmmo.utils.Logging
import me.dannly.inventories.Inventories
import me.dannly.inventories.InventoriesEvent
import me.dannly.party.party.Party
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.conversations.*
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object DungeonInventory {
    private val previousPage: ItemStack =
        InventoryLib.getItem(Material.ENDER_PEARL, 1, Main.getConfig("previous-page-item-name"), "")
    private val nextPage: ItemStack =
        InventoryLib.getItem(Material.ENDER_EYE, 1, Main.getConfig("next-page-item-name"), "")

    fun setupInventory(player: Player) {
        val inventories = Inventories()
        run {
            val items = mutableListOf<ItemStack>()
            for (dungeon in Dungeon.dungeons) items.add(
                InventoryLib.getItem(
                    dungeon.icon,
                    1,
                    ChatColor.RESET.toString() + dungeon.dungeonName
                )
            )
            inventories.append(
                InventoryLib.createInventory(
                    items,
                    "Dungeons",
                    arrayOfNulls(27),
                    18,
                    26,
                    previousPage,
                    nextPage,
                    false
                ), "me/dannly/main"
            )
            inventories.event(object : InventoriesEvent() {
                override fun click(
                    inventory: Inventory,
                    currentItem: ItemStack?,
                    who: Player,
                    clickType: ClickType,
                    slot: Int,
                    rawSlot: Int,
                    inventoryAction: InventoryAction,
                    cursor: ItemStack?
                ): Boolean {
                    if (currentItem != null && currentItem.type != Material.AIR && currentItem.hasItemMeta()) {
                        Bukkit.getScheduler().runTask(Main.instance, Runnable {
                            setupDungeon(
                                inventories, Dungeon.getDungeonByName(
                                    currentItem.itemMeta!!.displayName
                                )!!
                            )
                            player.openInventory(inventories.getInventory("actions")!!)
                        })
                    }
                    return true
                }

                override fun close(inventory: Inventory, player: Player) {}
            })
        }
        inventories.build(player)
    }

    private fun setupEntityList(inventories: Inventories, dungeon: Dungeon) {
        val items = ArrayList<ItemStack>()
        val toList = dungeon.dungeonEntities.toList()
        for (dungeonEntity in toList) {
            val id = toList.indexOf(dungeonEntity)
            val name = dungeonEntity.customName ?: "Monster #$id"
            val item: ItemStack = InventoryLib.getItem(Material.MAGMA_CREAM, 1, name, "ID: $id")
            if (dungeon.boss == dungeonEntity.id) {
                val im = item.itemMeta
                val lore = im!!.lore
                lore!!.add("Boss")
                im.lore = lore
                item.itemMeta = im
            }
            items.add(item)
        }
        val add: ItemStack = InventoryLib.getItem(
            Material.TOTEM_OF_UNDYING,
            1,
            ChatColor.RESET.toString() + Main.getConfig("entities-add"),
            ""
        )
        items.add(add)
        inventories.append(
            InventoryLib.createInventory(
                items,
                Main.getConfig("entities-inventory-title"),
                arrayOfNulls(54),
                45,
                53,
                previousPage,
                nextPage,
                false
            ), "entities"
        )
        inventories.addPage(45, previousPage, "actions")
        inventories.event(object : InventoriesEvent() {
            override fun click(
                inventory: Inventory,
                currentItem: ItemStack?,
                who: Player,
                clickType: ClickType,
                slot: Int,
                rawSlot: Int,
                inventoryAction: InventoryAction,
                cursor: ItemStack?
            ): Boolean {
                if (currentItem != null) {
                    if (currentItem == add) {
                        if (dungeon.spawnPoints.isEmpty()) {
                            Logging.log(who, "@entity-add-setup-spawnpoint@")
                            return true
                        }
                        who.closeInventory()
                        Logging.log(who, "@entity-added@")
                        dungeon.addEntity(DungeonEntity())
                        Main.saveDungeons()
                    } else if (currentItem != previousPage && currentItem != nextPage) {
                        val dungeonEntity = dungeon.dungeonEntities[ChatColor.stripColor(
                            currentItem.itemMeta!!.lore!![0]
                        )!!.replace("ID: ".toRegex(), "").toInt()]
                        if (!clickType.name.contains("RIGHT")) {
                            Bukkit.getScheduler().runTask(Main.instance, Runnable {
                                setupCustomizeEntity(dungeon, dungeonEntity, inventories)
                                who.openInventory(inventories.getInventory("entityManagement")!!)
                            })
                        } else {
                            if (dungeon.boss == dungeonEntity.id) {
                                dungeon.boss = null
                                val itemMeta = currentItem.itemMeta
                                val lore = itemMeta!!.lore
                                lore!!.removeAt(lore.size - 1)
                                itemMeta.lore = lore
                                currentItem.itemMeta = itemMeta
                                who.updateInventory()
                            } else {
                                dungeon.boss = dungeonEntity.id
                                val itemMeta = currentItem.itemMeta
                                val lore = itemMeta!!.lore
                                lore!!.add("Boss")
                                itemMeta.lore = lore
                                currentItem.itemMeta = itemMeta
                                who.updateInventory()
                            }
                            Main.saveDungeons()
                        }
                    }
                }
                return true
            }

            override fun close(inventory: Inventory, player: Player) {}
        })
    }

    private fun setupCustomizeEntity(dungeon: Dungeon, dungeonEntity: DungeonEntity, inventories: Inventories) {
        val actions = Bukkit.createInventory(null, 18, Main.getConfig("entity-customization-title"))
        inventories.append(actions, "entityManagement")
        inventories.addPage(9, previousPage, "entities")
        val spawn: ItemStack = InventoryLib.getItem(
            Material.END_PORTAL_FRAME,
            1,
            Main.getConfig("entity-spawnpoint-item"),
            ""
        )
        val rename: ItemStack =
            InventoryLib.getItem(Material.NAME_TAG, 1, Main.getConfig("entity-rename-item"), "")
        val command: ItemStack = InventoryLib.getItem(
            Material.COMMAND_BLOCK,
            1,
            Main.getConfig("entity-command-inventory-item"),
            ""
        )
        val delete: ItemStack =
            InventoryLib.getItem(Material.BARRIER, 1, Main.getConfig("entity-delete-item"), "")
        actions.setItem(0, spawn)
        actions.setItem(2, rename)
        actions.setItem(4, command)
        actions.setItem(8, delete)
        inventories.event(object : InventoriesEvent() {
            override fun click(
                inventory: Inventory,
                currentItem: ItemStack?,
                who: Player,
                clickType: ClickType,
                slot: Int,
                rawSlot: Int,
                inventoryAction: InventoryAction,
                cursor: ItemStack?
            ): Boolean {
                if (currentItem != null) {
                    if (currentItem == spawn) {
                        who.closeInventory()
                        if (dungeon.spawnPoints.isEmpty()) {
                            Logging.log(who, "@entity-spawnpoint-dungeon-spawnpoint-not-set@")
                            return true
                        }
                        val location = dungeon.spawnPoints[0]
                        val clone = who.location.toVector().subtract(location.toVector()).clone()
                        for (entity in dungeon.dungeonEntities) {
                            val spawnLocation = entity.spawnLocation ?: continue
                            if (spawnLocation.blockX == clone.blockX && spawnLocation.blockY == clone.blockY && spawnLocation.blockZ == clone.blockZ) {
                                Logging.log(who, "@entity-spawnpoint-already-used@")
                                return true
                            }
                        }
                        dungeonEntity.spawnLocation = clone
                        Main.saveDungeons()
                        Logging.log(who, "@entity-spawnpoint-set-message@")
                    } else if (currentItem == rename) {
                        who.closeInventory()
                        val conversationFactory = ConversationFactory(Main.instance)
                        conversationFactory.withTimeout(10).withLocalEcho(false)
                            .addConversationAbandonedListener { conversationAbandonedEvent: ConversationAbandonedEvent ->
                                if (!conversationAbandonedEvent.gracefulExit()) {
                                    conversationAbandonedEvent.context.forWhom.sendRawMessage(Main.getConfig("operation-cancelled"))
                                }
                            }.withFirstPrompt(object : StringPrompt() {
                                override fun getPromptText(conversationContext: ConversationContext): String {
                                    return Main.getConfig("entity-rename-prompt")
                                }

                                override fun acceptInput(
                                    conversationContext: ConversationContext,
                                    s: String?
                                ): Prompt? {
                                    dungeonEntity.customName = s
                                    conversationContext.forWhom.sendRawMessage(
                                        Main.getConfig("entity-rename-success").replace("%n".toRegex(), s!!)
                                    )
                                    Main.saveDungeons()
                                    return null
                                }
                            }).buildConversation(who).begin()
                    } else if (currentItem == command) {
                        who.closeInventory()
                        val conversationFactory = ConversationFactory(Main.instance)
                        conversationFactory.withTimeout(15).withLocalEcho(false)
                            .addConversationAbandonedListener { conversationAbandonedEvent: ConversationAbandonedEvent ->
                                if (!conversationAbandonedEvent.gracefulExit()) {
                                    conversationAbandonedEvent.context.forWhom.sendRawMessage(Main.getConfig("operation-cancelled"))
                                }
                            }.withFirstPrompt(object : StringPrompt() {
                                override fun getPromptText(conversationContext: ConversationContext): String {
                                    return Main.getConfig("entity-command-type-prompt")
                                }

                                override fun acceptInput(
                                    conversationContext: ConversationContext,
                                    s: String?
                                ): Prompt {
                                    if (s == null)
                                        return this
                                    return object : NumericPrompt() {
                                        override fun getPromptText(context: ConversationContext): String {
                                            return Main.getConfig("entity-command-level-prompt")
                                        }

                                        override fun acceptValidatedInput(
                                            context: ConversationContext,
                                            input: Number
                                        ): Prompt? {
                                            dungeonEntity.command =
                                                "mm mobs spawn " + s + ":" + input.toInt() + " 1 %l"
                                            conversationContext.forWhom.sendRawMessage(Main.getConfig("entity-command-success"))
                                            Main.saveDungeons()
                                            return null
                                        }
                                    }
                                }
                            }).buildConversation(who).begin()
                    } else if (currentItem == delete) {
                        who.closeInventory()
                        dungeon.removeEntity(dungeonEntity)
                        Logging.log(who, "@entity-deleted@")
                    }
                }
                return true
            }

            override fun close(inventory: Inventory, player: Player) {}
        })
    }

    private fun setupItemList(inventories: Inventories, dungeon: Dungeon) {
        val items = mutableListOf<ItemStack>()
        for (value in Material.values()) if (!value.isAir) items.add(
            InventoryLib.getItem(
                value,
                1,
                ChatColor.RESET.toString() + format(value.name),
                ""
            )
        )
        inventories.append(
            InventoryLib.createInventory(
                items,
                Main.getConfig("items-inventory-title"),
                arrayOfNulls(54),
                45,
                53,
                previousPage,
                nextPage,
                false
            ), "items"
        )
        inventories.addPage(45, previousPage, "actions")
        inventories.event(object : InventoriesEvent() {
            override fun click(
                inventory: Inventory,
                currentItem: ItemStack?,
                who: Player,
                clickType: ClickType,
                slot: Int,
                rawSlot: Int,
                inventoryAction: InventoryAction,
                cursor: ItemStack?
            ): Boolean {
                if (currentItem != null) {
                    if (currentItem != previousPage && currentItem != nextPage) {
                        dungeon.icon = currentItem.type
                        Logging.log(who, "@dungeon-icon-set@")
                        who.closeInventory()
                        Main.saveDungeons()
                    }
                }
                return true
            }

            override fun close(inventory: Inventory, player: Player) {}
        })
    }

    private fun setupActions(inventories: Inventories, dungeon: Dungeon?) {
        val actions = Bukkit.createInventory(null, 27, Main.getConfig("actions-inventory"))
        val compass: ItemStack = InventoryLib.getItem(
            Material.COMPASS,
            1,
            Main.getConfig("actions-spawnlocation"),
            "Left: Add",
            "Right: Reset"
        )
        actions.setItem(2, compass)
        val minimumPartySize: ItemStack = InventoryLib.getItem(
            Material.PLAYER_HEAD,
            dungeon!!.minimumPartySize,
            Main.getConfig("actions-minimum-party-size-name"),
            ""
        )
        val maximumPartySize: ItemStack = InventoryLib.getItem(
            Material.PLAYER_HEAD,
            dungeon.maximumPartySize,
            Main.getConfig("actions-maximum-party-size-name"),
            ""
        )
        val duration: ItemStack =
            InventoryLib.getItem(Material.CLOCK, 1, Main.getConfig("actions-duration-item-name"), "")
        val delete: ItemStack = InventoryLib.getItem(
            Material.BARRIER,
            dungeon.minimumPartySize,
            Main.getConfig("actions-delete-item-name"),
            ""
        )
        inventories.append(actions, "actions").addPage(18, previousPage, "me/dannly/main")
        inventories.event(object : InventoriesEvent() {
            override fun click(
                inventory: Inventory,
                currentItem: ItemStack?,
                who: Player,
                clickType: ClickType,
                slot: Int,
                rawSlot: Int,
                inventoryAction: InventoryAction,
                cursor: ItemStack?
            ): Boolean {
                if (currentItem != null) {
                    if (currentItem == compass) {
                        who.closeInventory()
                        if (clickType == ClickType.LEFT) {
                            val location = who.location
                            dungeon.addSpawnPoint(location)
                            Logging.log(who, "@dungeon-spawn-set@")
                        } else {
                            Logging.log(who, "@dungeon-spawn-reset@")
                            dungeon.spawnPoints.clear()
                            dungeon.points.clear()
                        }
                        Main.saveDungeons()
                    } else if (currentItem.isSimilar(minimumPartySize) || currentItem.isSimilar(maximumPartySize)) {
                        val a = currentItem.isSimilar(minimumPartySize)
                        val size =
                            (if (a) dungeon.minimumPartySize else dungeon.maximumPartySize) + if (clickType.name.contains(
                                    "LEFT"
                                )
                            ) +1 else -1
                        if (size <= 0 || size > Party.maximumPartySize) return true
                        currentItem.amount = size
                        if (a) dungeon.minimumPartySize = size else dungeon.maximumPartySize = size
                        who.updateInventory()
                        who.sendMessage(Main.getConfig("dungeon-party-size-updated"))
                        Main.saveDungeons()
                    } else if (currentItem == delete) {
                        who.closeInventory()
                        val conversationFactory =
                            ConversationFactory(Main.instance).withLocalEcho(false).withTimeout(10)
                                .addConversationAbandonedListener { conversationAbandonedEvent: ConversationAbandonedEvent ->
                                    if (!conversationAbandonedEvent.gracefulExit()) {
                                        conversationAbandonedEvent.context.forWhom.sendRawMessage(Main.getConfig("operation-cancelled"))
                                    }
                                }.withFirstPrompt(object : StringPrompt() {
                                    @NotNull
                                    override fun getPromptText(@NotNull conversationContext: ConversationContext): String {
                                        return Main.getConfig("actions-delete-confirmation-message")
                                    }

                                    override fun acceptInput(
                                        @NotNull conversationContext: ConversationContext,
                                        s: String?
                                    ): Prompt? {
                                        if (s.equals("yes", ignoreCase = true)) {
                                            dungeon.delete()
                                            Main.saveDungeons()
                                            conversationContext.forWhom.sendRawMessage(Main.getConfig("actions-dungeon-deleted"))
                                            return null
                                        }
                                        conversationContext.forWhom.sendRawMessage(Main.getConfig("operation-cancelled"))
                                        return END_OF_CONVERSATION
                                    }
                                })
                        conversationFactory.buildConversation(who).begin()
                    } else if (currentItem == duration) {
                        val conversationFactory =
                            ConversationFactory(Main.instance).withLocalEcho(false).withTimeout(10)
                                .addConversationAbandonedListener { conversationAbandonedEvent: ConversationAbandonedEvent ->
                                    if (!conversationAbandonedEvent.gracefulExit()) {
                                        conversationAbandonedEvent.context.forWhom.sendRawMessage(Main.getConfig("operation-cancelled"))
                                    }
                                }.withFirstPrompt(object : NumericPrompt() {
                                    override fun acceptValidatedInput(
                                        conversationContext: ConversationContext,
                                        number: Number
                                    ): Prompt? {
                                        conversationContext.forWhom.sendRawMessage(Main.getConfig("actions-duration-success"))
                                        dungeon.duration = number.toDouble()
                                        Main.saveDungeons()
                                        return null
                                    }

                                    override fun getPromptText(conversationContext: ConversationContext): String {
                                        return Main.getConfig("actions-duration-message")
                                    }
                                })
                        conversationFactory.buildConversation(who).begin()
                    }
                }
                return true
            }

            override fun close(inventory: Inventory?, who: Player) {}
        })
        inventories.addPage(
            0,
            InventoryLib.getItem(dungeon.icon, 1, Main.getConfig("actions-icon"), ""),
            "items"
        )
        inventories.addPage(
            4,
            InventoryLib.getItem(Material.ZOMBIE_HEAD, 1, Main.getConfig("entities-icon"), ""),
            "entities"
        )
        actions.setItem(6, minimumPartySize)
        actions.setItem(9, maximumPartySize)
        actions.setItem(11, duration)
        actions.setItem(8, delete)
    }

    private fun setupDungeon(inventories: Inventories, dungeon: Dungeon) {
        setupActions(inventories, dungeon)
        setupItemList(inventories, dungeon)
        setupEntityList(inventories, dungeon)
    }

    private fun format(name: String): String {
        return if (!name.contains("_")) {
            name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase()
        } else {
            val parts = name.split("_").toTypedArray()
            val builder = StringBuilder()
            for (part in parts) builder.append(part.substring(0, 1).toUpperCase())
                .append(part.substring(1).toLowerCase()).append(" ")
            val s = builder.toString()
            s.substring(0, s.length - 1)
        }
    }
}