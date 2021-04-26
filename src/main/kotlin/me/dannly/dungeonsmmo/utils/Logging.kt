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
import org.bukkit.command.CommandSender
import java.util.regex.Pattern

object Logging {
    fun getMessage(raw: String): String {
        var s = raw
        run {
            val pattern = Pattern.compile("@.*@", Pattern.DOTALL)
            val matcher = pattern.matcher(s)
            while (matcher.find()) {
                val group = matcher.group()
                s = s.replace(group.toRegex(), Main.getConfig(group.replace("@".toRegex(), "")))
            }
        }
        return s
    }

    fun log(sender: CommandSender, raw: String) {
        sender.sendMessage(getMessage(raw))
    }
}