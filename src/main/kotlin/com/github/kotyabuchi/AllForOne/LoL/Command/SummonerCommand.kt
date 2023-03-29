package com.github.kotyabuchi.AllForOne.LoL.Command

import com.github.kotyabuchi.AllForOne.Command.Command
import com.github.kotyabuchi.AllForOne.LoL.Table.CustomStats
import com.github.kotyabuchi.AllForOne.LoL.Table.Summoners
import com.github.kotyabuchi.AllForOne.Command.SubCommand
import com.github.kotyabuchi.AllForOne.LoL.RankDivision
import com.github.kotyabuchi.AllForOne.LoL.RankTier
import com.github.kotyabuchi.AllForOne.transactionWithLogger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object SummonerCommand: Command() {
    override val name: String = "summoner"
    override val description: String = "サモナーに関するコマンドです。"
    override val subCommands: List<SubCommand> = listOf(ShowCommand, ModalRegisterCommand)
    override val useEvent: Boolean = true
    override val action: SlashCommandInteractionEvent.() -> Unit = {}

    val openRegisterModalButton = Button.primary("open_summoner_register", "ランクを登録する")

    init {
        transactionWithLogger {
            SchemaUtils.create(Summoners)
        }
    }

    object ShowCommand : SubCommand() {
        override val name: String = "show"
        override val description: String = "登録されているランクを表示します。"
        override val options: List<OptionData> = listOf(OptionData(OptionType.USER, "user", "表示したいユーザー"))
        override val action: SlashCommandInteractionEvent.() -> Unit = {
            val targetUser = options.getOrNull(0)?.asUser ?: user
            transactionWithLogger {
                val summoner = Summoners.select(Summoners.id eq targetUser.idLong).singleOrNull()

                val stats = CustomStats.select(CustomStats.id eq targetUser.idLong).singleOrNull()

                if (summoner == null) {
                    reply("**${user.name}**はまだランクを登録していません。").queue()
                } else {
                    var rank = summoner[Summoners.tier]
                    summoner[Summoners.division]?.let {
                        rank += " $it"
                    }
                    val eb = EmbedBuilder().run {
                        setThumbnail(targetUser.avatarUrl)
                        setTitle(targetUser.name)
                        addField("Rank", rank, false)
                        stats?.let {
                            val total = "Total: ${stats[CustomStats.totalGame]}"
                            val win = "Win: ${stats[CustomStats.win]}"
                            val lose = "Lose: ${stats[CustomStats.lose]}"
                            addField("Stats", "$total\n$win\n$lose\n", false)
                        }
                        this
                    }
                    replyEmbeds(eb.build()).queue()
                }
            }
        }
    }

    object ModalRegisterCommand : SubCommand() {
        override val name: String = "register"
        override val description: String = "ランクを登録します。"
        const val modalName = "rank_register_modal"
        override val action: SlashCommandInteractionEvent.() -> Unit = {
            replyModal(createRegisterModal()).queue()
        }

        const val tierId = "tier"
        const val divisionId = "division"

        fun getTierInput(value: String?): TextInput {
            return TextInput.create(tierId, "Tier", TextInputStyle.SHORT)
                .setPlaceholder("現在のランクティアを英語で入力してください。")
                .setRequiredRange(4, 12)
                .setValue(value)
                .build()
        }

        fun getDivisionTextInput(value: String?): TextInput {
            return TextInput.create(divisionId, "Division", TextInputStyle.SHORT)
                .setPlaceholder("現在のディビジョンを半角数字で入力してください。")
                .setRequiredRange(1, 1)
                .setRequired(false)
                .setValue(value)
                .build()
        }

        fun createRegisterModal(tier: String? = null, division: String? = null): Modal {
            return Modal.create(modalName, "ランク登録")
                .addActionRows(ActionRow.of(getTierInput(tier)), ActionRow.of(getDivisionTextInput(division)))
                .build()
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != ModalRegisterCommand.modalName) return
        println()
        val rankTier = RankTier.valueOfOrNull(
            event.getValue(ModalRegisterCommand.tierId)?.asString?.replace(" ", "_")?.uppercase()
        )
        val rankDivision =
            RankDivision.fromArabic(event.getValue(ModalRegisterCommand.divisionId)?.asString?.toIntOrNull())
        val user = event.user

        if (rankTier == null) {
            event.reply("ティアが入力されていない、もしくは正しくありません。\n使用できるティアは`${
                RankTier.values().joinToString { it.camelString() }
            }`です。")
                .addActionRow(openRegisterModalButton)
                .setEphemeral(true).queue()
            return
        }

        println("NeedDivision: ${rankTier.hasDivision}, $rankDivision")

        if (rankTier.hasDivision && rankDivision == null) {
            event.reply("ディビジョンの入力がされていない、もしくは正しくありません。\n1~4で入力してください。")
                .addActionRow(openRegisterModalButton)
                .setEphemeral(true).queue()
            return
        }

        transactionWithLogger {
            if (Summoners.select { Summoners.id eq user.idLong }.empty()) {
                Summoners.insert {
                    it[id] = user.idLong
                    it[tier] = rankTier.name
                    it[division] = rankDivision?.name
                }
                event.reply("ランクを登録しました").setEphemeral(true).queue()
            } else {
                Summoners.update({ Summoners.id eq user.idLong }) {
                    it[id] = user.idLong
                    it[tier] = rankTier.name
                    it[division] = rankDivision?.name
                }
                event.reply("ランクを更新しました").setEphemeral(true).queue()
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId != openRegisterModalButton.id) return
        event.replyModal(ModalRegisterCommand.createRegisterModal()).queue()
    }
}
