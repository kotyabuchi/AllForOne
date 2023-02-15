package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object CommandBuilder {
    private val logger by LoggerKt()
    private val commands = mutableListOf<Command>()

    fun getCommands() = commands.toList()
    fun getCommandAction(commandName: String): (SlashCommandInteractionEvent.() -> Unit)? {
        return commands.firstOrNull {
            it.commandData.name == commandName
        }?.action
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