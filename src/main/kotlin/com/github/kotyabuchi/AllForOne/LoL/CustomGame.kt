package com.github.kotyabuchi.AllForOne.LoL

import com.github.kotyabuchi.AllForOne.LoL.Table.CustomStats
import com.github.kotyabuchi.AllForOne.LoL.Table.Summoners
import com.github.kotyabuchi.AllForOne.transactionWithLogger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ItemComponent
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime
import kotlin.math.abs

class CustomGame(val id: Long, val creator: User) {
    val summoners: MutableList<Summoner> = mutableListOf()
    private val createAt: LocalDateTime = LocalDateTime.now()
    var state = State.WAITING_FOR_JOIN
    var blueTeam: List<Summoner> = listOf()
    var redTeam: List<Summoner> = listOf()

    private var teamPattern: List<List<Summoner>> = listOf()
    private var currentTeamPattern = 0

    fun joinGame(user: User): JoinGameResult {
        var result: JoinGameResult = JoinGameResult.Fail.Undefined

        if (summoners.map { it.id }.contains(user.idLong)) return JoinGameResult.Fail.AlreadyJoined

        transactionWithLogger {
            val summonerData = Summoners.select {
                Summoners.id eq user.idLong
            }.singleOrNull()

            result = if (summonerData == null) {
                JoinGameResult.Fail.NotRegistered
            } else {
                val customStats = CustomStats.select {
                    CustomStats.id eq user.idLong
                }.singleOrNull()

                val additionalPoint = if (customStats == null) 0 else {
                    (customStats[CustomStats.win] * 2) - (customStats[CustomStats.lose] * 2)
                }
                val rankTier = RankTier.valueOf(summonerData[Summoners.tier])
                val rankDivision = summonerData[Summoners.division]?.let { if (rankTier.hasDivision) RankDivision.valueOf(it) else null }
                val summoner = Summoner(user.name, user.idLong, user, rankTier, rankDivision, additionalPoint)

                if (summoners.size >= 10) {
                    JoinGameResult.Fail.FilledSummoner
                } else {
                    summoners.add(summoner)
                    JoinGameResult.Success
                }
            }
        }
        return result
    }

    fun removeSummoner(id: Long): Boolean = summoners.removeIf { it.id == id }

    fun createTeams(): Boolean {
        if (summoners.size != 10) return false
        var highest = summoners.first()
        var lowest = summoners.first()

        summoners.forEach {
            if (highest.rankPoint + highest.additionalPoint < it.rankPoint + it.additionalPoint) {
                highest = it
            } else if (lowest.rankPoint + lowest.additionalPoint > it.rankPoint + it.additionalPoint) {
                lowest = it
            }
        }

        val halfRP = summoners.sumOf { it.rankPoint + it.additionalPoint } / 2
        val tmp = mutableListOf<Summoner>()
        tmp.addAll(summoners)
        tmp.removeAll(listOf(highest, lowest))

        val teams = mutableListOf<List<Summoner>>()

        bruteForceTeam(tmp, teams, listOf(highest, lowest), 0, tmp.size - 3)

        teams.sortBy { team ->
            abs(team.sumOf { it.rankPoint + it.additionalPoint } - halfRP)
        }

        teamPattern = teams.take(10)
        setTeam()
        return true
    }

    private fun bruteForceTeam(summoners: List<Summoner>, result: MutableList<List<Summoner>>, subSet: List<Summoner>, begin: Int, end: Int) {
        for (i in begin .. end) {
            val tmp = listOf(summoners[i], *subSet.toTypedArray())
            if (end + 1 < summoners.size) {
                bruteForceTeam(summoners, result, tmp, i + 1, end + 1)
            } else {
                result.add(tmp)
            }
        }
    }

    fun nextTeam() {
        currentTeamPattern = (currentTeamPattern + 1) % teamPattern.size
        setTeam()
    }

    private fun setTeam() {
        val nextBlueTeam = teamPattern.getOrNull(currentTeamPattern) ?: return
        val nextRedTeam = summoners.minus(nextBlueTeam.toSet())

        blueTeam = nextBlueTeam
        redTeam = nextRedTeam
    }

    fun generateSummonersEmbed(): MessageEmbed {
        val sb = StringBuilder()
        summoners.forEach { summoner ->
            sb.appendLine("<@${summoner.id}>")
        }
        val eb = EmbedBuilder().run {
            setThumbnail(creator.avatarUrl)
            setTitle("${creator.name}'s Custom Game - ${state.displayName}")
            setTimestamp(createAt)
            addField(":mage: Summoners - ${summoners.size}/10", sb.toString(), false)
            this
        }
        return eb.build()
    }

    fun generateTeamEmbed(): MessageEmbed {
        val blueTeamName = StringBuilder()
        val redTeamName = StringBuilder()
        val blueTeamRank = StringBuilder()
        val redTeamRank = StringBuilder()
        val blueTeamScore = blueTeam.sumOf { it.rankPoint + it.additionalPoint }
        val redTeamScore = redTeam.sumOf { it.rankPoint + it.additionalPoint }
        var blueTeamTitle = ":blue_square: Blue - Score[$blueTeamScore]"
        var redTeamTitle = ":red_square: Red - Score[$redTeamScore]"

        if (blueTeamScore != redTeamScore) {
            if (blueTeamScore > redTeamScore) blueTeamTitle += " +${blueTeamScore - redTeamScore}"
            if (redTeamScore > blueTeamScore) redTeamTitle += " +${redTeamScore - blueTeamScore}"
        }

        repeat(5) {
            val blueTeamSummoner = blueTeam[it]
            val redTeamSummoner = redTeam[it]
            blueTeamName.appendLine("<@${blueTeamSummoner.id}>")
            redTeamName.appendLine("<@${redTeamSummoner.id}>")
            blueTeamRank.appendLine("${blueTeamSummoner.tier} ${blueTeamSummoner.division ?: ""}")
            redTeamRank.appendLine("${redTeamSummoner.tier} ${redTeamSummoner.division ?: ""}")
        }

        val eb = EmbedBuilder().run {
            setThumbnail(creator.avatarUrl)
            setTitle("${creator.name}'s Custom Game - ${state.displayName} ${if (state == State.CREATING_TEAM) "${currentTeamPattern + 1}/10" else ""}")
            setTimestamp(createAt)
            addField(blueTeamTitle, blueTeamName.toString(), true)
            addField("", blueTeamRank.toString(), true)
            addBlankField(true)
            addField(redTeamTitle, redTeamName.toString(), true)
            addField("", redTeamRank.toString(), true)
            addBlankField(true)
            this
        }
        return eb.build()
    }

    fun generateActionButtons(): ActionRow {
        val numOfSummoners = summoners.size
        val buttons = mutableListOf<ItemComponent>()

        if (numOfSummoners >= 10) {
            buttons.add(CustomEmbedButton.JOIN_GAME.button.asDisabled())
        } else {
            buttons.add(CustomEmbedButton.JOIN_GAME.button)
        }

        if (numOfSummoners > 0) {
            buttons.add(CustomEmbedButton.LEAVE_GAME.button)
        }
        return ActionRow.of(buttons)
    }

    enum class State(val displayName: String) {
        WAITING_FOR_JOIN("参加待ち"),
        CREATING_TEAM("チーム作成中"),
        IN_GAME("ゲーム中"),
        ENDED("終了")
    }
}