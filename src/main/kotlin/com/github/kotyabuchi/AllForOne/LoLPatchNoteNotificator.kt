package com.github.kotyabuchi.AllForOne

import com.github.kotyabuchi.AllForOne.Command.ChannelType
import com.github.kotyabuchi.AllForOne.Command.ChannelTypes
import com.github.kotyabuchi.AllForOne.Command.Channels
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

object LoLPatchNoteNotificator {

    init {
        transactionWithLogger {
            SchemaUtils.create(PatchNoteVersion)
        }
    }

    fun start() {
        val currentTime = System.currentTimeMillis()
        var recentTime = LocalDateTime.now()
            .truncatedTo(ChronoUnit.HOURS)
            .withHour(5).toEpochSecond(ZoneOffset.ofHours(9)) * 1000
        val everyDay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
        if (recentTime < currentTime) recentTime += everyDay
        Timer().schedule(recentTime - currentTime, everyDay) {
            if (isNeedNotice()) notice()
        }
    }

    private fun isNeedNotice(): Boolean {
        val version = Jsoup.connect("https://www.leagueoflegends.com/ja-jp/news/tags/patch-notes/").get().body()
            .getElementsByAttributeValue("data-testid", "articlelist")
            .select("li > a > article h2").first()?.text()?.split(" ")?.last() ?: return false

        var found = false
        transactionWithLogger {
            found = PatchNoteVersion.select(PatchNoteVersion.version eq version).empty()
        }
        return found
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
        transactionWithLogger {
            val channelTypeId = ChannelTypes.slice(ChannelTypes.id).select { ChannelTypes.name eq ChannelType.PATCH_NOTE.name }.single()[ChannelTypes.id]
            Channels.select { Channels.channelTypeID eq channelTypeId }.forEach {
                val guildId = it[Channels.guildID]
                val chanelId = it[Channels.channelID]
                Bot.jda.getGuildById(guildId)?.let { guild ->
                    guild.getChannelById(MessageChannelUnion::class.java, chanelId)?.sendMessageEmbeds(eb)?.queue()
                }
            }

            PatchNoteVersion.insertIgnore {
                it[version] = patchNote.title().split(" ").last()
                it[updateDate] = LocalDateTime.now()
            }
        }
    }

    object PatchNoteVersion: Table() {
        val version = varchar("version", 10)
        val updateDate = datetime("update_date")
    }
}