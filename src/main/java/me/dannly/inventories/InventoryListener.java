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

package me.dannly.inventories;

import me.dannly.dungeonsmmo.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InventoryListener implements Listener {

    @EventHandler
    public void click(InventoryClickEvent event) {
        List<Inventories> invs = Inventories.getInvs();
        for (Inventories inv : invs) {
            if(inv.getInventories().contains(event.getClickedInventory())) {
                inv.getCustomInventory(event.getClickedInventory()).getInventoriesEvents().forEach(e -> event.setCancelled(e.click(event.getClickedInventory(), event.getCurrentItem(), (Player) event.getWhoClicked(), event.getClick(), event.getSlot(), event.getRawSlot(), event.getAction(), event.getCursor())));
            }
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        List<Inventories> invs = Inventories.getInvs();
        Player player = (Player) event.getPlayer();
        for (Inventories inv : invs) {
            if(inv.getInventories().contains(event.getInventory())) {
                inv.getCustomInventory(event.getInventory()).getInventoriesEvents().forEach(inventoriesEvent -> inventoriesEvent.close(event.getInventory(), player));
                Bukkit.getScheduler().runTask(Main.Companion.getInstance(), () -> {
                    if (!inv.getInventories().contains(player.getOpenInventory().getTopInventory()) && !inv.getInventories().contains(player.getOpenInventory().getBottomInventory()))
                        Inventories.removeInventory(inv);
                });
            }
        }
    }

    @EventHandler
    public void pages(InventoryClickEvent event) {
        if (event.getCurrentItem() == null)
            return;
        List<Inventories> invs = Inventories.getInvs();
        for (Inventories inv : invs) {
            if (inv.getInventories().contains(event.getClickedInventory())) {
                inv.getCustomInventories().forEach(customInventory -> {
                    ItemStack item = event.getCurrentItem();
                    int slot = event.getSlot();
                    if (customInventory.getInventory() != null)
                        if (!customInventory.getInventory().equals(event.getClickedInventory()))
                            return;
                    customInventory.getPages().forEach((i, s) -> {
                        if (slot == i) {
                            event.setCancelled(true);
                            Inventory toGo = inv.getInventory(s);
                            if (toGo != null) {
                                if (customInventory.getInventoriesOpenEvent() != null)
                                    if (customInventory.getInventoriesOpenEvent().open(s,
                                            (Player) event.getWhoClicked()))
                                        return;
                                event.getWhoClicked().closeInventory();
                                event.getWhoClicked().openInventory(toGo);
                            }
                        }
                    });
                });
                break;
            }
        }
    }
}
