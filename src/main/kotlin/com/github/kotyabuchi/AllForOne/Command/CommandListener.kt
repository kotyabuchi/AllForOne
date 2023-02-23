package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger

object CommandListener: ListenerAdapter() {

    private val logger: Logger by LoggerKt()
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        val user = event.user
        val command = CommandManager.getCommand(commandName)

        if (command == null) {
            event.reply("Command[${commandName}]は存在しません。").queue()
            logger.info("[${user.name}]が[${commandName}]を実行しようとしました。")
        } else {
            val subcommandName = event.subcommandName
            val subCommands = command.subCommands
            val guild = event.guild
            if (subcommandName == null || subCommands.isEmpty()) {
                command.action.invoke(event)
                printCommandLog(commandName, user, guild)
            } else {
                val subCommand = command.getSubCommand(subcommandName)
                if (subCommand == null) {
                    event.reply("Subcommand[${commandName} ${subcommandName}]は存在しません。").queue()
                } else {
                    subCommand.action.invoke(event)
                    printCommandLog("$commandName $subcommandName", user, guild)
                }
            }
        }
    }

    private fun printCommandLog(commandName: String, user: User, guild: Guild?) {
        val sb = StringBuilder()
            .appendLine("[${user.name}]が[${commandName}]コマンドを使用しました")
            .appendLine("UserID: ${user.id}")
        guild?.let {
            sb.appendLine("GuildName: ${it.name}")
                .appendLine("GuildID: ${it.id}")
        }
        logger.info(sb.toString())
    }
}