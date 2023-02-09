package com.github.kotyabuchi.AllForOne

import net.dv8tion.jda.api.EmbedBuilder
import org.slf4j.LoggerFactory
import java.awt.Color
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
        command("help", "コマンド一覧を表示します") {
            execute {
                val embedBuilder = EmbedBuilder().run {
                    setTitle("AllForOne - Help")
                    setColor(Color(30, 10, 60))
                    setDescription("コマンド一覧")

                    this@bot.getCommands().forEach {
                        addField(it.commandName, it.description, false)
                    }
                    this
                }
                replyEmbeds(embedBuilder.build()).queue()
            }
        }
    }
}
