package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

abstract class Command: ListenerAdapter() {
    protected val logger by LoggerKt()
    abstract val name: String
    abstract val description: String
    open val useEvent: Boolean = false
    abstract val action: SlashCommandInteractionEvent.() -> Unit
}