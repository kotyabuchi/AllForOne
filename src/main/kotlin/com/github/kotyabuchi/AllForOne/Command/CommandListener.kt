package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger

object CommandListener: ListenerAdapter() {

    private val logger: Logger by LoggerKt()
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        CommandBuilder.getCommandAction(commandName)?.let {
            it.invoke(event)
            logger.info("""
                コマンドが実行されました
                CommandName: $commandName
                GuildName: ${event.guild?.name}
                GuildID: ${event.guild?.id}
                User: ${event.user.name}
                UserID: ${event.user.id}
            """.trimIndent())
        }
    }
}