package com.github.kotyabuchi.AllForOne

import com.github.kotyabuchi.AllForOne.Command.Command
import com.github.kotyabuchi.AllForOne.Command.CommandListener
import com.github.kotyabuchi.AllForOne.Command.CommandManager
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger

class Bot: ListenerAdapter() {
    private val logger: Logger by LoggerKt()

    fun bot(token: String, vararg registerCommands: Command) {
        val jDABuilder = JDABuilder.createLight(token,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_VOICE_STATES)
            .addEventListeners(this)
            .addEventListeners(CommandListener)
            .enableCache(CacheFlag.VOICE_STATE)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
        val jda = jDABuilder.build()
        jda.awaitReady()

        registerCommands.forEach {
            CommandManager.addCommand(it)
            if (it.useEvent) jda.addEventListener(it)
        }
        CommandManager.register(jda)
    }

    override fun onReady(event: ReadyEvent) {
        logger.info("起動しました")
    }
}