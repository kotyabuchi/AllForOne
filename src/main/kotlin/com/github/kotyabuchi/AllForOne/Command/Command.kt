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
    open val useEvent: Boolean = false
    abstract val action: SlashCommandInteractionEvent.() -> Unit
    val commandData: CommandData by lazy { Commands.slash(name, description).addOptions(options) }
}