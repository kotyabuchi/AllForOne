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
        jda.guilds.forEach { guild ->
            val clua = guild.updateCommands()
            logger.info("Guild[${guild.name} - ${guild.id}]の登録済みのコマンドをリセットしました")
            commands.forEach {
                clua.addCommands(it.commandData)
                logger.info("Guild[${guild.name} - ${guild.id}]にコマンド[${it.name}]を登録しました")
            }
            clua.queue()
        }
    }
}