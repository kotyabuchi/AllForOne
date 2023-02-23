package com.github.kotyabuchi.AllForOne.Command

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import kotlin.random.Random

object DiceCommand: Command() {
    override val name: String = "dice"
    override val description: String = "ダイスを振ります。"
    override val options: List<OptionData> = listOf(
        OptionData(OptionType.INTEGER, "sides", "ダイスの面の数", true),
        OptionData(OptionType.INTEGER, "times", "ダイスを振る回数", true)
    )
    override val action: SlashCommandInteractionEvent.() -> Unit = {
        val sides = options[0].asInt
        val times = options[1].asInt

        var rollsStr = ""
        var total = 0
        var max = 0
        var min = sides + 1
        repeat(times) {
            val roll = Random.nextInt(sides) + 1
            rollsStr += roll
            if (it < times - 1) rollsStr += ", "
            total += roll
            if (max < roll) max = roll
            if (min > roll) min = roll
        }
        val embedBuilder = EmbedBuilder().run {
            setTitle("${sides}d${times}")
            addField("合計", total.toString(), true)
            addField("最大", max.toString(), true)
            addField("最小", min.toString(), true)
            addField("各スコア", rollsStr, false)
            this
        }
        replyEmbeds(embedBuilder.build()).queue()
    }
}