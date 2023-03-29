package com.github.kotyabuchi.AllForOne

import com.github.kotyabuchi.AllForOne.Command.Command
import com.github.kotyabuchi.AllForOne.Command.CommandListener
import com.github.kotyabuchi.AllForOne.Command.CommandManager
import com.github.kotyabuchi.AllForOne.Command.Commands.HelpCommand
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger

object Bot: ListenerAdapter() {
    private val logger: Logger by LoggerKt()
    lateinit var jda: JDA

    fun bot(token: String, vararg registerCommands: Command) {
        val jDABuilder = JDABuilder.createLight(token,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_VOICE_STATES)
            .addEventListeners(this)
            .addEventListeners(CommandManager)
            .addEventListeners(CommandListener)
            .enableCache(CacheFlag.VOICE_STATE)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
        jda = jDABuilder.build()
        jda.awaitReady()

        registerCommands.forEach {
            CommandManager.addCommand(it)
            if (it.useEvent) jda.addEventListener(it)
        }
        CommandManager.addCommand(HelpCommand)
        CommandManager.register(jda)
    }

    override fun onReady(event: ReadyEvent) {
        logger.info("起動しました")
    }
}