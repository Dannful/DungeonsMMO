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

import java.util.*
import java.util.function.Consumer

object Data {

    var data = HashMap<String, Any>()

    operator fun get(key: String): Any? {
        return data[key]
    }

    operator fun get(key: String, def: Any): Any {
        return data.getOrDefault(key, def)
    }

    inline operator fun <reified T> set(map: Map<String, Any>, key: String, c: Consumer<T>) {
        val o = map[key] as? T? ?: return
        c.accept(o)
    }

    fun insertIntoMap(map: MutableMap<String, Any>, key: String, o: Any?) {
        if(o != null)
            map[key] = o
    }
}