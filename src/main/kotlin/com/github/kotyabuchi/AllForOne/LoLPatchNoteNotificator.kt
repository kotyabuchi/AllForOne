package com.github.kotyabuchi.AllForOne

import com.github.kotyabuchi.AllForOne.Command.ChannelType
import com.github.kotyabuchi.AllForOne.Command.ChannelTypes
import com.github.kotyabuchi.AllForOne.Command.Channels
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

object LoLPatchNoteNotificator {
    fun start() {
        val current = System.currentTimeMillis()
        val recentWed = LocalDateTime.now()
            .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            .truncatedTo(ChronoUnit.HOURS)
            .withHour(5).toEpochSecond(ZoneOffset.ofHours(9)) * 1000
        val everyWeek = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)
        Timer().schedule(recentWed - current, everyWeek) {
            notice()
        }
    }

    private fun notice() {
        val currentPatchNoteUrl = "https://www.leagueoflegends.com" + (Jsoup.connect("https://www.leagueoflegends.com/ja-jp/news/tags/patch-notes/").get().body()
            .getElementsByAttributeValue("data-testid", "articlelist")
            .select("li > a").first()?.attr("href") ?: return)
        val patchNote = Jsoup.connect(currentPatchNoteUrl).get()
        val body = patchNote.body()
        val imageUrl = body.select("div.white-stone > div > span > a.skins").attr("href")
        val patchDescription = body.select("div#patch-notes-container > blockquote.context").html().split("<br><br>")
        val eb = EmbedBuilder().run {
            setTitle(patchNote.title(), currentPatchNoteUrl)
            setDescription(patchDescription[1])
            setImage(imageUrl)
        }.build()
        transaction {
            val channelTypeId = ChannelTypes.slice(ChannelTypes.id).select { ChannelTypes.name eq ChannelType.PATCH_NOTE.name }.single()[ChannelTypes.id]
            Channels.select { Channels.channelTypeID eq channelTypeId }.forEach {
                val guildId = it[Channels.guildID]
                val chanelId = it[Channels.channelID]
                Bot.jda.getGuildById(guildId)?.let { guild ->
                    guild.getChannelById(MessageChannelUnion::class.java, chanelId)?.sendMessageEmbeds(eb)?.queue()
                }
            }
        }
    }
}