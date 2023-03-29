package com.github.kotyabuchi.AllForOne.LoL.Command

import com.github.kotyabuchi.AllForOne.Command.Command
import com.github.kotyabuchi.AllForOne.LoL.Table.CustomStats
import com.github.kotyabuchi.AllForOne.Command.SubCommand
import com.github.kotyabuchi.AllForOne.LoL.*
import com.github.kotyabuchi.AllForOne.transactionWithLogger
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

object CustomCommand: Command() {
    override val name: String = "custom"
    override val description: String = "カスタムに関するコマンドです。"
    override val subCommands: List<SubCommand> = listOf(CreateCommand, AddSummonerCommand)
    override val useEvent: Boolean = true
    override val action: SlashCommandInteractionEvent.() -> Unit = {}

    private val customGames = mutableMapOf<Long, CustomGame>()
    val recentGames = mutableMapOf<Long, CustomGame>()

    init {
        transactionWithLogger {
            SchemaUtils.create(CustomStats)
        }
    }

    object CreateCommand: SubCommand() {
        override val name: String = "create"
        override val description: String = "カスタムを生成します。"
        override val action: SlashCommandInteractionEvent.() -> Unit = {
            if (customGames.map { it.value.creator }.contains(user)) {
                reply("前回作成したカスタムが終了していません。").setEphemeral(true).queue()
            } else {
                createCustomGame(channel, user) {
                    val buttons = ActionRow.of(CustomEmbedButton.CREATE_TEAM.button).actionComponents
                    if (recentGames.containsKey(user.idLong)) buttons.add(CustomEmbedButton.IMPORT_RECENT_GAME.button)
                    reply("カスタムを作成しました。 ID: ${this.id}")
                        .addActionRow(buttons)
                        .setEphemeral(true).queue()
                }
            }
        }
    }

    object AddSummonerCommand: SubCommand() {
        override val name: String = "add"
        override val description: String = "カスタムにサモナーを追加します。"
        override val options: List<OptionData> = listOf(
            OptionData(OptionType.STRING, "id", "カスタム生成時に表示されるID", true),
            OptionData(OptionType.USER, "user", "追加するユーザー", true)
        )
        override val action: SlashCommandInteractionEvent.() -> Unit = {
            val customId = options[0].asString.toLongOrNull()
            val targetUser = options[1].asUser

            if (customId == null) {
                reply("IDは整数で入力してください。").setEphemeral(true).queue()
            } else {
                val customGame = customGames[customId]

                if (customGame == null) {
                    reply("ゲームが存在しません。").setEphemeral(true).queue()
                } else {
                    if (customGame.state == CustomGame.State.WAITING_FOR_JOIN) {
                        if (user == customGame.creator) {
                            when (val joinGameResult = customGame.joinGame(targetUser)) {
                                is JoinGameResult.Success -> {
                                    reply("<@${targetUser.id}>をカスタムに追加しました。").setEphemeral(true).queue()
                                    channel.editMessageEmbedsById(customId, customGame.generateSummonersEmbed()).queue()
                                    channel.editMessageComponentsById(customId, customGame.generateActionButtons()).queue()
                                }
                                is JoinGameResult.Fail -> {
                                    reply(joinGameResult.reason).setEphemeral(true).queue()
                                }
                            }
                        } else {
                            reply("カスタム作成者のみ使用可能です。").setEphemeral(true).queue()
                        }
                    } else {
                        reply("参加募集中のみ使用可能です。").setEphemeral(true).queue()
                    }
                }
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val buttonAction = CustomEmbedButton.values().associate { it.button.id to it.action }[event.componentId] ?: return

        val customId = event.message.contentRaw.split("ID: ").getOrNull(1)?.toLong() ?: event.message.idLong
        val customGame = customGames[customId]

        if (customGame == null) {
            event.reply("ゲームが存在しません。").setEphemeral(true).queue()
            return
        }

        val customButtonInteractionEvent = CustomButtonInteractionEvent(customId, customGame, event)
        buttonAction.invoke(customButtonInteractionEvent)
    }

    fun createCustomGame(channel: MessageChannelUnion, creator: User, callBack: CustomGame.() -> Unit = {}) {
        channel.sendMessage("カスタムを作成中です...")
            .queue {
                val customGame = CustomGame(it.idLong, creator)
                customGames[it.idLong] = customGame
                it.editMessageEmbeds(customGame.generateSummonersEmbed())
                    .setActionRow(CustomEmbedButton.JOIN_GAME.button)
                    .setReplace(true).queue()
                callBack.invoke(customGame)
            }
        return
    }

    fun increaseWinCount(summoners: List<Summoner>) {
        transactionWithLogger {
            summoners.forEach { summoner ->
                val history = CustomStats.select { CustomStats.id eq summoner.id }.singleOrNull()
                if (history == null) {
                    CustomStats.insert {
                        it[id] = summoner.id
                        it[totalGame] = 1
                        it[win] = 1
                        it[lose] = 0
                    }
                } else {
                    CustomStats.update({ CustomStats.id eq summoner.id }) {
                        it[totalGame] = history[totalGame] + 1
                        it[win] = history[win] + 1
                    }
                }
            }
        }
    }

    fun increaseLoseCount(summoners: List<Summoner>) {
        transactionWithLogger {
            summoners.forEach { summoner ->
                val history = CustomStats.select { CustomStats.id eq summoner.id }.singleOrNull()
                if (history == null) {
                    CustomStats.insert {
                        it[id] = summoner.id
                        it[totalGame] = 1
                        it[win] = 0
                        it[lose] = 1
                    }
                } else {
                    CustomStats.update({ CustomStats.id eq summoner.id }) {
                        it[totalGame] = history[totalGame] + 1
                        it[lose] = history[lose] + 1
                    }
                }
            }
        }
    }
}