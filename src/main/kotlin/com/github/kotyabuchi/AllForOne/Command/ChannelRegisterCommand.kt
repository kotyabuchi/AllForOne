package com.github.kotyabuchi.AllForOne.Command

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object ChannelRegisterCommand: Command() {
    override val name: String = "channel"
    override val description: String = "Botが利用するチャンネルを管理するコマンドです。"
    override val subCommands: List<SubCommand> = listOf(ShowCommand, SetCommand)
    override val action: SlashCommandInteractionEvent.() -> Unit = {}

    init {
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(ChannelTypes, Channels)

            ChannelType.values().forEach { type ->
                ChannelTypes.insertIgnore {
                    it[name] = type.name
                }
            }
        }
    }


    object ShowCommand: SubCommand() {
        override val name: String = "show"
        override val description: String = "設定されているチャンネルを表示します。"
        override val action: SlashCommandInteractionEvent.() -> Unit = {
            val guildId = guild?.id

            if (guildId == null) {
                reply("このコマンドはサーバー専用です").queue()
            } else {
                transaction {
                    val eb = EmbedBuilder()
                    val result = Channels.join(ChannelTypes, JoinType.INNER, additionalConstraint = {Channels.channelTypeID eq ChannelTypes.id})
                        .slice(Channels.channelID, ChannelTypes.name)
                        .select { Channels.guildID eq guildId }
                    if (result.empty()) {
                        reply("まだチャンネルが設定されていません。").queue()
                    } else {
                        result.forEach {
                            val channelType = ChannelType.valueOf(it[ChannelTypes.name])
                            eb.addField("${channelType.displayName} - <#${it[Channels.channelID]}>", channelType.description, false)
                        }
                        replyEmbeds(eb.build()).queue()
                    }
                }
            }
        }
    }

    object SetCommand: SubCommand() {
        override val name: String = "set"
        override val description: String = "チャンネルを登録します。"
        override val options: List<OptionData> = listOf(
            OptionData(OptionType.CHANNEL, "channel", "対象のチャンネル", true).run {
                setChannelTypes(net.dv8tion.jda.api.entities.channel.ChannelType.TEXT)
                this
            },
            OptionData(OptionType.STRING, "type", "設定するチャンネルの種類", true).run {
                ChannelType.values().forEach {
                    addChoice(it.name, it.name)
                }
                this
            }
        )
        override val action: SlashCommandInteractionEvent.() -> Unit = {
            val guildId = guild?.id
            val channelId = options[0].asChannel.id
            val channelType = options[1].asString

            if (guildId == null) {
                reply("このコマンドはサーバー専用です").queue()
            } else {
                transaction {
                    val channelTypeId = ChannelTypes.slice(ChannelTypes.id).select { ChannelTypes.name eq channelType }.single()[ChannelTypes.id]
                    Channels.insertIgnore {
                        it[guildID] = guildId
                        it[channelTypeID] = channelTypeId
                        it[channelID] = channelId
                    }
                }
            }

            reply("<#$channelId>を${ChannelType.valueOf(channelType).displayName}に設定しました。").queue()
        }
    }
}

enum class ChannelType(val displayName: String, val description: String) {
    PATCH_NOTE("Patch Note", "パッチノートを定期配信するためのチャンネル"),
}

object ChannelTypes: Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 20).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object Channels: Table() {
    val guildID = varchar("guild_id", 24)
    val channelTypeID = integer("channel_type_id").index().references(
        ChannelTypes.id,
        fkName = "fk_channel_type_id",
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.RESTRICT)
    val channelID = varchar("channel_id", 24)

    override val primaryKey = PrimaryKey(guildID)
}