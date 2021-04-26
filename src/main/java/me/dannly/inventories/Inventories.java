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

import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Inventories {
	private static final List<Inventories> invs = new ArrayList<Inventories>();
	private CustomInventory[] current;
	private final List<CustomInventory> inventories = new ArrayList<CustomInventory>();
	public Inventories(Inventory inventory, String alias) {
		append(inventory, alias);
		invs.add(this);
	}

	public Inventories() {
		invs.add(this);
	}

	public static List<Inventories> getInvs() {
		return invs;
	}

	public static void removeInventory(Inventories inventories) {
		invs.remove(inventories);
	}

	public List<CustomInventory> getCustomInventories() {
		return inventories;
	}

	public List<Inventory> getInventories() {
		return inventories.stream().map(CustomInventory::getInventory).collect(Collectors.toList());
	}

	public Inventories append(Inventory inventory, String alias) {
		CustomInventory customInventory = new CustomInventory();
		customInventory.setAlias(alias);
		customInventory.setInventory(inventory);
		current = new CustomInventory[]{customInventory};
		inventories.add(customInventory);
		return this;
	}

	public Inventories append(List<Inventory> inventory, String alias) {
		current = new CustomInventory[inventory.size()];
		for (int i = 0; i < inventory.size(); i++) {
			Inventory inv = inventory.get(i);
			CustomInventory customInventory = new CustomInventory();
			if (i == 0)
				customInventory.setAlias(alias);
			customInventory.setInventory(inv);
			current[i] = customInventory;
			inventories.add(customInventory);
		}
		return this;
	}

	public Inventories append(Inventory inventory, List<String> aliases) {
		CustomInventory customInventory = new CustomInventory();
		aliases.forEach(customInventory::setAlias);
		customInventory.setInventory(inventory);
		current = new CustomInventory[]{customInventory};
		inventories.add(customInventory);
		return this;
	}

	public Inventories append(List<Inventory> inventory, List<String> aliases) {
		current = new CustomInventory[inventory.size()];
		for (int i = 0; i < inventory.size(); i++) {
			Inventory inv = inventory.get(i);
			CustomInventory customInventory = new CustomInventory();
			if (i == 0)
				aliases.forEach(customInventory::setAlias);
			customInventory.setInventory(inv);
			current[i] = customInventory;
			inventories.add(customInventory);
		}
		return this;
	}

	public Inventory getInventory(String alias) {
		List<Inventory> collect = inventories.stream().filter(
				customInventory -> customInventory.getAliases() != null && customInventory.getAliases().contains(alias))
				.map(CustomInventory::getInventory).collect(Collectors.toList());
		return collect.size() > 0 ? collect.get(0) : null;
	}

	public CustomInventory getCustomInventory(Inventory inventory) {
		List<CustomInventory> collect = inventories.stream()
				.filter(inventory1 -> Objects.equals(inventory, inventory1.getInventory()))
				.collect(Collectors.toList());
		return collect.size() > 0 ? collect.get(0) : null;
	}

	public CustomInventory getCustomInventory(String alias) {
		List<CustomInventory> collect = inventories.stream()
				.filter(customInventory -> customInventory.getAliases().contains(alias)).collect(Collectors.toList());
		return collect.size() > 0 ? collect.get(0) : null;
	}

	public Inventory[] getCurrent() {
		return Arrays.stream(current).map(CustomInventory::getInventory).toArray(Inventory[]::new);
	}

	@Override
	public Inventories clone() {
		Inventories cloned = new Inventories();
		for (CustomInventory customInventory : inventories) {
			cloned.append(customInventory.getInventory(), customInventory.getAliases());
			customInventory.getInventoriesEvents().forEach(cloned::event);
			if (customInventory.getInventoriesOpenEvent() != null)
				customInventory.setInventoriesOpenEvent(customInventory.getInventoriesOpenEvent());
			customInventory.getPages().forEach(cloned::addPage);
		}
		return cloned;
	}

	public Inventories event(InventoriesEvent event) {
		Inventory[] inventories = getCurrent();
		for (CustomInventory customInventory : current)
			customInventory.addInventoryEvent(event);
		return this;
	}

	public Inventories addPage(int slot, ItemStack item, String inventoryAlias) {
		getCurrent()[0].setItem(slot, item);
		current[0].addPage(slot, inventoryAlias);
		return this;
	}

	public Inventories addPage(int slot, String inventoryAlias) {
		current[0].addPage(slot, inventoryAlias);
		return this;
	}

	public Inventories addPage(int slot, ItemStack item, int page, String inventoryAlias) {
		getCurrent()[page].setItem(slot, item);
		current[page].addPage(slot, inventoryAlias);
		return this;
	}

	public Inventories addPage(ItemStack item, int page, String inventoryAlias) {
		int slot = getCurrent()[page].firstEmpty();
		current[page].getInventory().setItem(slot, item);
		current[page].addPage(slot, inventoryAlias);
		return this;
	}

	public Inventories addPage(ItemStack item, String inventoryAlias) {
		int page = getCurrent().length - 1;
		int slot = getCurrent()[page].firstEmpty();
		current[page].getInventory().setItem(slot, item);
		current[page].addPage(slot, inventoryAlias);
		return this;
	}

	public Inventories setInventory(String alias, Inventory inventory) {
		inventories.removeIf(customInventory -> customInventory.getAliases().contains(alias));
		append(inventory, alias);
		return this;
	}

	public Inventories setInventory(String alias, List<Inventory> inventories) {
		this.inventories.removeIf(customInventory -> customInventory.getAliases().contains(alias));
		append(inventories, alias);
		return this;
	}

	public Inventories addInventoriesOpenEvent(InventoriesOpenEvent event) {
		for (CustomInventory customInventory : current)
			customInventory.setInventoriesOpenEvent(event);
		return this;
	}

	public void build(Player player) {
		player.openInventory(getInventories().get(0));
	}

	public static class CustomInventory {
		private List<String> alias = new ArrayList<String>();
		private Inventory inventory;
		private Map<Integer, String> pages = new HashMap<Integer, String>();
		private InventoriesOpenEvent inventoriesOpenEvent;
		private List<InventoriesEvent> inventoriesEvents = new ArrayList<InventoriesEvent>();

		public CustomInventory() {
		}

		public void addInventoryEvent(InventoriesEvent inventoryEvent) {
			inventoriesEvents.add(inventoryEvent);
		}

		public List<InventoriesEvent> getInventoriesEvents() {
			return inventoriesEvents;
		}

		public void removeInventoryEvent(InventoriesEvent inventoriesEvent) {
			inventoriesEvents.remove(inventoriesEvent);
		}

		public InventoriesOpenEvent getInventoriesOpenEvent() {
			return inventoriesOpenEvent;
		}

		public void setInventoriesOpenEvent(InventoriesOpenEvent inventoriesOpenEvent) {
			this.inventoriesOpenEvent = inventoriesOpenEvent;
		}

		public Map<Integer, String> getPages() {
			return pages;
		}

		public void addPage(int slot, String alias) {
			pages.put(slot, alias);
		}

		public void removePage(int slot) {
			pages.remove(slot);
		}

		public List<String> getAliases() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias.add(alias);
		}

		public Inventory getInventory() {
			return inventory;
		}

		public void setInventory(Inventory inventory) {
			this.inventory = inventory;
		}
	}
}
