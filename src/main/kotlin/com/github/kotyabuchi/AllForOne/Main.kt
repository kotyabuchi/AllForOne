package com.github.kotyabuchi.AllForOne

import com.github.kotyabuchi.AllForOne.Command.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val logger = LoggerFactory.getLogger(Main::class.java)

            val token = args.getOrNull(0) ?: run {
                logger.error("第一引数にBotトークンが必要です。")
                exitProcess(1)
            }

            val dbFilePath = args.getOrNull(1) ?: run {
                logger.error("第二引数にDBFileのパスが必要です。")
                exitProcess(1)
            }
            DBConnector.registerDBFile(dbFilePath).connect()

            Bot.bot(token, HelpCommand, RoomCommand, VoteCommand, DiceCommand, ChannelRegisterCommand)

            LoLPatchNoteNotificator.start()
        }
    }
}
