package com.github.kotyabuchi.AllForOne.Command.Commands

import com.github.kotyabuchi.AllForOne.Command.Command
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

object RoomCommand: Command() {
    private val rooms = mutableMapOf<String, String>()
    override val name: String = "room"
    override val description: String = "一時的なボイスチャンネルを作成します。"
    override val options: List<OptionData> = listOf(OptionData(OptionType.STRING, "name", "作成するチャンネル名", false))
    override val useEvent: Boolean = true
    override val action: SlashCommandInteractionEvent.() -> Unit = {
        val guild = guild
        val connectingChannelCategory = member?.voiceState?.channel?.parentCategory
        val channelName = getOption("name")?.asString ?: "一時部屋"
        guild?.let {
            guild.createVoiceChannel(channelName, connectingChannelCategory).queue {
                reply("${channelName}を作成しました。ID: ${it.id}")
                    .addActionRow(Button.danger("delete_room", "チャンネルを削除する。"))
                    .queue()
                rooms[guild.id] = it.id
                logger.info("Guild[${guild.id}]にVoiceChannel[$channelName]を作成しました。")
            }
        }
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val guildId = event.guild.id
        val channel = event.channelLeft?.asVoiceChannel() ?: return
        val createdRooms = rooms[guildId] ?: return

        if (channel.members.isNotEmpty() || !createdRooms.contains(channel.id)) return
        deleteRoom(guildId, channel)
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId == "delete_room") {
            val channelId = event.message.contentRaw.split("ID: ")[1]
            val guild = event.guild
            val channel = guild?.getChannelById(VoiceChannel::class.java, channelId)
            var button = event.component.asDisabled()
            button = if (channel == null) {
                logger.info("Guild[${guild?.id}]のVoiceChannel[${channelId}]を見つけることが出来ませんでした。")
                button.withLabel("チャンネルが存在しません。")
            } else {
                deleteRoom(guild.id, channel)
                button.withLabel("削除済です。")
            }
            event.editComponents(ActionRow.of(button)).queue()
        }
    }

    private fun deleteRoom(guildId: String, channel: VoiceChannel) {
        val createdRooms = rooms[guildId] ?: return
        if (!createdRooms.contains(channel.id)) return
        channel.delete().queue()
        rooms.remove(guildId, channel.id)
        logger.info("Guild[$guildId]のVoiceChannel[${channel.name}]を削除しました。")
    }
}