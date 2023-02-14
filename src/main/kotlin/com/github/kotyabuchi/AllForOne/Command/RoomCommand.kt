package com.github.kotyabuchi.AllForOne.Command

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object RoomCommand: Command() {
    private val rooms = mutableMapOf<String, String>()
    override val name: String = "room"
    override val description: String = "一時的なボイスチャンネルを作成します。"
    override val useEvent: Boolean = true
    override val action: SlashCommandInteractionEvent.() -> Unit = {
        val guild = guild
        val connectingChannelCategory = member?.voiceState?.channel?.parentCategory
        guild?.let {
            guild.createVoiceChannel("一時部屋", connectingChannelCategory).queue {
                rooms[guild.id] = it.id
                reply("部屋を作成しました。ID: ${it.id}").queue()
                logger.info("Guild[${it.id}]にVoiceChannel[一時部屋]を作成しました。")
            }
        }
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val guildId = event.guild.id
        val channel = event.channelLeft?.asVoiceChannel() ?: return
        val createdRooms = rooms[guildId] ?: return

        if (channel.members.isEmpty() && createdRooms.contains(channel.id)) {
            channel.delete().queue()
            rooms.remove(guildId, channel.id)
            logger.info("Guild[$guildId]のVoiceChannel[${channel.name}]を削除しました。")
        }
    }
}