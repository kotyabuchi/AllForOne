package com.github.kotyabuchi.AllForOne.Command

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.concurrent.schedule

object VoteCommand: Command() {
    override val name: String = "vote"
    override val description: String = "投票に関するコマンドです。"
    override val subCommands: List<SubCommand> = listOf(CreateCommand, CloseCommand)
    override val useEvent: Boolean = true
    override val action: SlashCommandInteractionEvent.() -> Unit = {}

    private const val deadlineFormat = "yy/MM/dd-HH:mm"
    private val votes: MutableMap<String, Vote> = mutableMapOf()
    private val emojis = listOf(
        Emoji.fromUnicode("\uD83C\uDDE6"),
        Emoji.fromUnicode("\uD83C\uDDE7"),
        Emoji.fromUnicode("\uD83C\uDDE8"),
        Emoji.fromUnicode("\uD83C\uDDE9"),
    )

    class Vote(val author: User, val voteMessageID: String, val question: String, val deadlineTimer: Timer, vararg val choice: String)

    fun parseDeadline(deadline: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(deadline, DateTimeFormatter.ofPattern(deadlineFormat))
        } catch (e: DateTimeParseException) {
            null
        }
    }

    object CreateCommand: SubCommand() {
        override val name: String = "create"
        override val description: String = "投票を作成します。"
        override val options: List<OptionData> = listOf(
            OptionData(OptionType.STRING, "question", "投票の議題/目的", true),
            OptionData(OptionType.STRING, "deadline", "期限($deadlineFormat)", true),
            OptionData(OptionType.STRING, "choice1" , "選択肢1", true),
            OptionData(OptionType.STRING, "choice2" , "選択肢2", true),
            OptionData(OptionType.STRING, "choice3" , "選択肢3"),
            OptionData(OptionType.STRING, "choice4" , "選択肢4"),
        )

        override val action: SlashCommandInteractionEvent.() -> Unit = {
            val options = options.map { it.asString }
            val question = options[0]
            val deadline = parseDeadline(options[1])
            val choices = options.subList(2, options.lastIndex + 1)

            if (deadline == null) {
                reply("期限のフォーマットが正しくありません。[$deadlineFormat]").setEphemeral(true).queue()
            } else {
                val voteEB = EmbedBuilder().run {
                    setAuthor(user.name, user.avatarUrl, user.avatarUrl)
                    setTitle("アンケート")
                    addField(question, "", false)
                    val sb = StringBuilder()
                    choices.forEachIndexed { index, choice ->
                        sb.appendLine("${emojis[index].formatted} $choice")
                    }
                    addField("選択肢", sb.toString(), false)
                    setFooter("期限: $deadline")
                    this
                }

                val channelId = channel.id
                val voteId = "${channelId}_$id"
                reply("投票を作成しました。")
                    .addActionRow(Button.danger("close_vote", "投票を締め切る。"))
                    .setEphemeral(true)
                    .queue()
                user.openPrivateChannel().queue {
                    val privateEB = EmbedBuilder().run {
                        setTitle("投票を作成しました")
                        addField("ID", voteId, false)
                        addField("サーバー", guild?.name ?: "", true)
                        addField("チャンネル", channel.name, true)
                    }
                    it.sendMessageEmbeds(privateEB.build()).queue()
                }

                val timer = Timer()
                timer.schedule(deadline.toEpochSecond(ZoneOffset.ofHours(9)) * 1000 - System.currentTimeMillis()) {
                    closeVote(channel, voteId)
                }

                channel.sendMessageEmbeds(voteEB.build()).queue { message ->
                    val vote = Vote(user, message.id, options[0], timer, *choices.toTypedArray())
                    votes[voteId] = vote
                    repeat(choices.size) {
                        message.addReaction(emojis[it]).queue()
                    }
                }
            }
        }
    }

    object CloseCommand: SubCommand() {
        override val name: String = "close"
        override val description: String = "投票を締め切ります。"
        override val options: List<OptionData> = listOf(OptionData(OptionType.STRING, "id", "Vote作成時に表示されたID", true))
        override val action: SlashCommandInteractionEvent.() -> Unit = {
            val guild = guild
            val id = options[0].asString

            if (guild == null) {
                reply("ギルド限定コマンドです。").setEphemeral(true).queue()
            } else {
                val channel = guild.getChannelById(MessageChannelUnion::class.java, id.split("_")[0])
                if (channel == null) {
                    reply("チャンネルが存在しません。").setEphemeral(true).queue()
                } else {
                    reply("投票を締め切りました。").setEphemeral(true).queue()
                    closeVote(channel, id)
                }
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId == "close_vote") {
            val guild = event.guild ?: return
            val id = event.message.contentRaw.split("ID: ")[1]
            val channelId = id.split("_")[0]
            val channel = guild.getChannelById(MessageChannelUnion::class.java, channelId)
            if (channel == null) {
                event.reply("チャンネルが存在しません。").setEphemeral(true).queue()
            } else {
                closeVote(channel, id)
                event.editComponents(ActionRow.of(event.component.asDisabled().withLabel("投票を締め切りました。"))).queue()
            }
        }
    }

    private fun closeVote(channel: MessageChannelUnion, id: String) {
        votes[id]?.let { vote ->
            val choices = vote.choice
            val choiceAmount = choices.size
            val votes = mutableMapOf<Int, Int>()
            repeat(choiceAmount) { num ->
                val emoji = emojis[num]
                channel.retrieveReactionUsersById(vote.voteMessageID, emoji).queue({
                    votes[num] = it.size - 1

                    if (num == choiceAmount - 1) {
                        val eb = EmbedBuilder().run {
                            val author = vote.author
                            val totalUser = votes.values.sum().toDouble()
                            setAuthor(author.name, author.avatarUrl, author.avatarUrl)
                            setTitle("投票結果")
                            addField(vote.question, "", false)
                            val sb = StringBuilder()
                            choices.forEachIndexed { index, choice ->
                                val votesNum = votes.getOrDefault(index, 1)
                                val votePerTotal = if (totalUser <= 0) 0.0 else votesNum / totalUser * 100
                                sb.appendLine("%s %s".format(emojis[index].formatted, choice))
                                sb.appendLine("%d票 %.1f%%".format(votesNum, votePerTotal))
                                sb.appendLine()
                            }
                            addField("票数", sb.toString(), false)
                            this
                        }
                        vote.deadlineTimer.cancel()
                        channel.sendMessageEmbeds(eb.build()).queue()
                        channel.deleteMessageById(vote.voteMessageID).queue()
                    }
                }, {
                    vote.deadlineTimer.cancel()
                })
            }
        }
    }
}