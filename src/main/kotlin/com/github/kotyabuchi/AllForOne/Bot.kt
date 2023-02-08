package com.github.kotyabuchi.AllForOne

import com.github.kotyabuchi.AllForOne.Command.CommandListener
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory

class Bot: ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(Bot::class.java)

    fun bot(token: String) {
        val jda = JDABuilder.createLight(token,
            GatewayIntent.GUILD_MESSAGES)
            .addEventListeners(this)
            .addEventListeners(CommandListener())
            .build()

        jda.awaitReady()
    }

    override fun onReady(event: ReadyEvent) {
        logger.info("起動しました")
    }
}