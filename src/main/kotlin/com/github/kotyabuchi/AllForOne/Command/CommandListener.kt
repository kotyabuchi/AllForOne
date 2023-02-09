package com.github.kotyabuchi.AllForOne.Command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandListener(private val commandBuilder: CommandBuilder): ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        commandBuilder.getCommandAction(commandName)?.invoke(event)
    }
}