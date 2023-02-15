package com.github.kotyabuchi.AllForOne.Command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

abstract class SubCommand {
    abstract val name: String
    abstract val description: String
    open val options: List<OptionData> = listOf()
    abstract val action: SlashCommandInteractionEvent.() -> Unit
    val subCommandData: SubcommandData by lazy { SubcommandData(name, description).addOptions(options) }
}