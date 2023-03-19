package com.github.kotyabuchi.AllForOne.Command

import com.github.kotyabuchi.AllForOne.LoggerKt
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object CommandManager: ListenerAdapter() {
    private val logger by LoggerKt()
    private val commands = mutableListOf<Command>()

    fun getCommands() = commands.toList()

    fun getCommand(commandName: String): Command? {
        return commands.firstOrNull {
            it.name == commandName
        }
    }
    fun addCommand(command: Command) {
        commands.add(command)
    }

    fun register(jda: JDA) {
        jda.guilds.forEach { guild ->
            register(guild)
        }
    }

    fun register(guild: Guild) {
        val clua = guild.updateCommands()
        logger.info("Guild[${guild.name} - ${guild.id}]の登録済みのコマンドをリセットしました")
        commands.forEach {
            clua.addCommands(it.commandData)
            logger.info("Guild[${guild.name} - ${guild.id}]にコマンド[${it.name}]を登録しました")
        }
        clua.queue()
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        val guild = event.guild
        logger.info("Guild[${guild.name} - ${guild.id}]に参加しました。")
        register(guild)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        val guild = event.guild
        logger.info("Guild[${guild.name} - ${guild.id}]に脱退しました。")
    }
}