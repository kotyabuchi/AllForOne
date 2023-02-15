package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData

abstract class Command: ListenerAdapter() {
    protected val logger by LoggerKt()
    abstract val name: String
    abstract val description: String
    open val options: List<OptionData> = listOf()
    open val subCommands: List<SubCommand> = listOf()
    open val useEvent: Boolean = false
    abstract val action: SlashCommandInteractionEvent.() -> Unit
    val commandData: CommandData by lazy {
        if (subCommands.isEmpty()) {
            Commands.slash(name, description).addOptions(options)
        } else {
            Commands.slash(name, description).addSubcommands(subCommands.map { it.subCommandData })
        }
    }

    fun getSubCommand(subCommandName: String): SubCommand? {
        return subCommands.firstOrNull {
            it.name == subCommandName
        }
    }
}