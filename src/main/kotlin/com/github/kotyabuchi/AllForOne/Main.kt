package com.github.kotyabuchi.AllForOne

import com.github.kotyabuchi.AllForOne.Command.HelpCommand
import com.github.kotyabuchi.AllForOne.Command.VoteCommand
import com.github.kotyabuchi.AllForOne.Command.RoomCommand
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

    Bot().bot(token, HelpCommand, RoomCommand, VoteCommand)
}
