package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.JDA

object CommandManager {
    private val logger by LoggerKt()
    private val commands = mutableListOf<Command>()

    fun getCommands() = commands.toList()

    fun getCommand(commandName: String): Command? {
        return commands.firstOrNull {
            it.name == commandName
        }
    }
    fun addCommand(command: Command) {
        commands.add(command)
    }

    fun register(jda: JDA) {
        val guild = jda.getGuildById(263784386376237062)
        guild?.let {
            val clua = guild.updateCommands()
            logger.info("登録済みのコマンドをリセットしました")
            commands.forEach {
                clua.addCommands(it.commandData)
                logger.info("コマンド[${it.name}]を登録しました")
            }
            clua.queue()
        }
    }
}