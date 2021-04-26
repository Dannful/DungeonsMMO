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

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

object RunnableAPI {
    private val tasks = mutableMapOf<String, BukkitTask>()

    fun cancelRunnable(runnable: BukkitRunnable) {
        for (task in Bukkit.getScheduler().pendingTasks) {
            if (task.taskId == runnable.taskId) {
                task.cancel()
                tasks.remove(getIdentifier(task).orEmpty())
            }
        }
    }

    fun cancelRunnable(identifier: String) {
        val bukkitTask = tasks[identifier]
        if (bukkitTask != null) {
            bukkitTask.cancel()
            tasks.remove(identifier)
        }
    }

    private fun getIdentifier(task: BukkitTask): String? {
        return tasks.keys.find { tasks[it] == task }
    }

    fun isRunning(identifier: String): Boolean {
        val bukkitTask = tasks[identifier]
        return bukkitTask != null && Bukkit.getScheduler().isCurrentlyRunning(bukkitTask.taskId)
    }

    fun startRunnable(identifier: String, runnable: BukkitRunnable, plugin: Plugin, vararg value: Long) {
        when {
            value.size == 1 -> runnable.runTaskLater(plugin, value[0])
            value.size >= 2 -> tasks[identifier] = runnable.runTaskTimer(plugin, value[0], value[1])
            else -> tasks[identifier] = runnable.runTask(plugin)
        }
    }
}