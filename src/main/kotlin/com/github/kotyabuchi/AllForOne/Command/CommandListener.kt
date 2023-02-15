package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger

object CommandListener: ListenerAdapter() {

    private val logger: Logger by LoggerKt()
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        val command = CommandManager.getCommand(commandName)

        if (command == null) {
            event.reply("Command[${commandName}]は存在しません。").queue()
        } else {
            val subcommandName = event.subcommandName
            val subCommands = command.subCommands
            if (subcommandName == null || subCommands.isEmpty()) {
                command.action.invoke(event)
                logger.info("""
                    コマンドが実行されました
                    CommandName: $commandName
                    GuildName: ${event.guild?.name}
                    GuildID: ${event.guild?.id}
                    User: ${event.user.name}
                    UserID: ${event.user.id}
                """.trimIndent())
            } else {
                val subCommand = command.getSubCommand(subcommandName)
                if (subCommand == null) {
                    event.reply("Subcommand[${subcommandName}]は存在しません。").queue()
                } else {
                    subCommand.action.invoke(event)
                    logger.info("""
                        コマンドが実行されました
                        CommandName: $commandName
                        SubcommandName: $subcommandName
                        GuildName: ${event.guild?.name}
                        GuildID: ${event.guild?.id}
                        User: ${event.user.name}
                        UserID: ${event.user.id}
                    """.trimIndent())
                }
            }
        }
    }
}