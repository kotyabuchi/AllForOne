package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class CommandBuilder {
    companion object {
        private val logger by LoggerKt()
    }

    private val commands = mutableListOf<CommandSet>()

    fun getCommands() = commands.toList()
    fun getCommandAction(commandName: String): (SlashCommandInteractionEvent.() -> Unit)? {
        return commands.firstOrNull {
            it.commandData.name == commandName
        }?.action
    }

    fun command(commandName: String, description: String = "", action: CommandSet.() -> Unit) {
        val commandSet = CommandSet(commandName, description)
        action(commandSet)
        commands.add(commandSet)
    }

    fun register(jda: JDA) {
        val guild = jda.getGuildById(263784386376237062)
        guild?.let {
            val clua = guild.updateCommands()
            logger.info("登録済みのコマンドをリセットしました")
            commands.forEach {
                clua.addCommands(it.commandData)
                logger.info("コマンド[${it.commandData.name}]を登録しました")
            }
            clua.queue()
        }
    }
}

class CommandSet(val commandName: String, val description: String) {
    var action: SlashCommandInteractionEvent.() -> Unit = {}
        private set
    val commandData: SlashCommandData = Commands.slash(commandName, description)
    fun execute(action: SlashCommandInteractionEvent.() -> Unit) {
        this.action = action
    }
}