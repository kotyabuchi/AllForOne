package com.github.kotyabuchi.AllForOne.Command.Commands

import com.github.kotyabuchi.AllForOne.Command.Command
import com.github.kotyabuchi.AllForOne.Command.CommandManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.awt.Color

object HelpCommand: Command() {
    override val name: String = "help"
    override val description: String = "コマンド一覧を表示します。"
    override val options: List<OptionData> = listOf(
        OptionData(OptionType.STRING, "command", "詳細を確認したいコマンド名").apply {
            CommandManager.getCommands().forEach { command ->
                addChoice(command.name, command.name)
            }
        }
    )
    override val action: SlashCommandInteractionEvent.() -> Unit = {
        val commandName = getOption("command")?.asString?.lowercase()
        val command = commandName?.let { CommandManager.getCommand(it) }

        val eb = EmbedBuilder()
        if (command == null) {
            eb.run {
                setTitle("AllForOne - Help")
                setColor(Color(30, 10, 60))
                setDescription("コマンド一覧\n[ ]内はオプション")
                CommandManager.getCommands().forEach {
                    if (it.subCommands.isEmpty()) {
                        val sb = StringBuilder()
                        it.options.forEach {
                            sb.append(" [${it.name}]")
                            if (it.isRequired) sb.append("<必須>") else sb.append("<任意>")
                        }
                        addField(it.name + sb.toString(), it.description, false)
                    } else {
                        addField(it.name, it.description, false)
                    }
                }
                this
            }
        } else {
            eb.run {
                setTitle("${commandName}コマンドの詳細")
                setDescription("${command.description}\n[ ]内はオプション")
                command.subCommands.forEach { subCommand ->
                    val sb = StringBuilder()
                    subCommand.options.forEach {
                        sb.append(" [${it.name}]")
                        if (it.isRequired) sb.append("<必須>") else sb.append("<任意>")
                    }
                    addField("${command.name} ${subCommand.name}$sb", subCommand.description, false)
                }
                this
            }
        }
        replyEmbeds(eb.build()).queue()
    }
}