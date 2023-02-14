package com.github.kotyabuchi.AllForOne

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

class LoggerKt {
    private var logger: Logger? = null

    operator fun getValue(thisRef: Any, property: KProperty<*>): Logger {
        return logger?:LoggerFactory.getLogger(thisRef.javaClass.name.let {
            val matchIndex = it.length - 10
            when (it.lastIndexOf("\$Companion")) {
                matchIndex -> it.substring(0, matchIndex)
                else -> it
            }
        }).apply { this@LoggerKt.logger = this }
    }
}