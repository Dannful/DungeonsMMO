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
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object InventoryLib {
    fun createInventory(
        items: List<ItemStack?>, title: String, baseItems: Array<ItemStack?>,
        previousPageSlot: Int, nextPageSlot: Int, previousPageItem: ItemStack?, nextPageItem: ItemStack?,
        insertPageNumber: Boolean
    ): List<Inventory> {
        if (items.isEmpty()) {
            val inventory = Bukkit.createInventory(
                null, baseItems.size,
                title + if (insertPageNumber) " - P�gina 1" else ""
            )
            return listOf(inventory)
        }
        val inventories: MutableList<Inventory> = ArrayList()
        val size = baseItems.size
        val a = ArrayList(items)
        val iterator: Iterator<ItemStack?> = a.iterator()
        while (iterator.hasNext()) {
            val inventory = Bukkit.createInventory(
                null, size,
                title + if (insertPageNumber) " - P�gina " + (inventories.size + 1) else ""
            )
            inventory.contents = baseItems
            inventories.add(inventory)
            if (inventories.indexOf(inventory) > 0) inventory.setItem(previousPageSlot, previousPageItem)
            Bukkit.getPluginManager().registerEvents(object : Listener {
                @EventHandler
                fun click(event: InventoryClickEvent) {
                    if (inventory == event.inventory) {
                        if (event.currentItem == null) return
                        if (event.currentItem == previousPageItem) {
                            if (inventories.indexOf(inventory) > 0) {
                                event.whoClicked.openInventory(inventories[inventories.indexOf(inventory) - 1])
                                event.isCancelled = true
                            }
                        } else if (event.currentItem == nextPageItem) {
                            if (inventories.indexOf(inventory) < inventories.size - 1) {
                                event.whoClicked.openInventory(inventories[inventories.indexOf(inventory) + 1])
                                event.isCancelled = true
                            }
                        } else {
                            for (i in baseItems) {
                                if (i != null) {
                                    if (i.type != Material.AIR) {
                                        if (i == event.currentItem) {
                                            event.isCancelled = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                @EventHandler
                fun close(event: InventoryCloseEvent) {
                    if (inventory == event.inventory) {
                        val l: Listener = this
                        Bukkit.getScheduler().runTask(
                            Main.instance,
                            Runnable {
                                if (!inventories.contains(event.player.openInventory.topInventory)) HandlerList.unregisterAll(
                                    l
                                )
                            })
                    }
                }
            }, Main.instance)
            for (i in 0 until size - 9) {
                if (!iterator.hasNext()) return inventories
                inventory.setItem(i, iterator.next())
            }
            if (iterator.hasNext()) {
                inventory.setItem(nextPageSlot, nextPageItem)
            }
        }
        return inventories
    }

    fun getItem(
        type: Material?, amount: Int, name: String?,
        vararg lore: String
    ): ItemStack {
        val item = ItemStack(type!!, amount)
        val im = item.itemMeta
        if (name != null) if (name.isNotEmpty()) im!!.setDisplayName(name) else im!!.setDisplayName(ChatColor.RESET.toString())
        if (lore.isNotEmpty() && lore[0].isNotEmpty()) im!!.lore = listOf(*lore)
        item.itemMeta = im
        return item
    }

}