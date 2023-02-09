package com.github.kotyabuchi.AllForOne

import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class Main {
}

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(Main::class.java)

    val token = args.firstOrNull() ?: run {
        logger.error("第一引数にBotトークンが必要です。")
        exitProcess(1)
    }

    Bot().bot(token) {
        command("test", "てすとこまんどぉ") {
            execute {
                reply("This is test command!").setEphemeral(true).queue()
            }
        }
    }
}
