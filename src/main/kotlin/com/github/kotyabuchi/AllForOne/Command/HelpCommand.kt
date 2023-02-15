package com.github.kotyabuchi.AllForOne.Command

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.awt.Color

object HelpCommand: Command() {
    override val name: String = "help"
    override val description: String = "コマンド一覧を表示します。"
    override val action: SlashCommandInteractionEvent.() -> Unit = {
        val embedBuilder = EmbedBuilder().run {
            setTitle("AllForOne - Help")
            setColor(Color(30, 10, 60))
            setDescription("コマンド一覧")
            CommandBuilder.getCommands().forEach {
                addField(it.name, it.description, false)
            }
            this
        }
        replyEmbeds(embedBuilder.build()).queue()
    }
}