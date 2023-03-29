package com.github.kotyabuchi.AllForOne.LoL

import com.github.kotyabuchi.AllForOne.LoL.Command.CustomCommand
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

enum class CustomEmbedButton(val button: Button, val action: CustomButtonInteractionEvent.() -> Unit) {
    IMPORT_RECENT_GAME(
        Button.primary("import_recent_game", "前回のメンバーを引き継ぐ").withEmoji(Emoji.fromFormatted("\uD83D\uDD04")),
        {
            val recentGame = CustomCommand.recentGames[user.idLong]
            if (recentGame == null) {
                reply("前回のゲームが見つかりませんでした。").setEphemeral(true).queue()
            } else {
                customGame.summoners.clear()
                recentGame.summoners.forEach { recentSummoner ->
                    customGame.joinGame(recentSummoner.user)
                }
                channel.editMessageEmbedsById(customId, customGame.generateSummonersEmbed()).queue()
                reply("前回のゲームを呼び出しました。").setEphemeral(true).queue()
            }
        }
    ),
    JOIN_GAME(
        Button.primary("join_game", "参加する").withEmoji(Emoji.fromFormatted("⚔️")),
        {
            when (val joinGameResult = customGame.joinGame(user)) {
                is JoinGameResult.Success -> {
                    editMessageEmbeds(customGame.generateSummonersEmbed()).queue()
                    hook.editOriginalComponents(customGame.generateActionButtons()).queue()
                }
                is JoinGameResult.Fail -> {
                    when (joinGameResult) {
                        is JoinGameResult.Fail.NotRegistered -> {
                            replyModal(JoinGameResult.Fail.NotRegistered.getModal()).queue()
                        }
                        else -> reply(joinGameResult.reason).setEphemeral(true).queue()
                    }
                }
            }
        }
    ),
    LEAVE_GAME(
        Button.danger("leave_game", "辞退する").withEmoji(Emoji.fromFormatted("⚰️")),
        {
            if (customGame.removeSummoner(user.idLong)) {
                editMessageEmbeds(customGame.generateSummonersEmbed()).queue()
                hook.editOriginalComponents(customGame.generateActionButtons()).queue()
            } else {
                reply("ゲームに参加していません。").setEphemeral(true).queue()
            }
        }
    ),
    CREATE_TEAM(
        Button.success("create_team", "チームを振り分ける").withEmoji(Emoji.fromFormatted("⚖️")),
        {
            if (customGame.createTeams()) {
                customGame.state = CustomGame.State.CREATING_TEAM
                channel.editMessageEmbedsById(customId, customGame.generateTeamEmbed()).queue()
                channel.editMessageComponentsById(customId,).queue()
                editComponents(
                    ActionRow.of(
                        NEXT_TEAM.button,
                        LOCK_TEAM.button
                    )).queue()
            } else {
                reply("振り分けるには10人必要です。").setEphemeral(true).queue()
            }
        }
    ),
    NEXT_TEAM(
        Button.primary("next_team", "次の候補を表示する"),
        {
            customGame.nextTeam()
            channel.editMessageEmbedsById(customId, customGame.generateTeamEmbed()).queue()
            editComponents(
                ActionRow.of(
                    NEXT_TEAM.button,
                    LOCK_TEAM.button
                )).queue()
        }
    ),
    LOCK_TEAM(
        Button.success("lock_team", "チームを確定する").withEmoji(Emoji.fromFormatted("\uD83D\uDD12")),
        {
            customGame.state = CustomGame.State.IN_GAME
            channel.editMessageEmbedsById(customId, customGame.generateTeamEmbed()).queue()
            editComponents(
                ActionRow.of(
                    BLUE_TEAM_WON.button,
                    RED_TEAM_WON.button
                )).queue()
        }
    ),
    BLUE_TEAM_WON(
        Button.primary("win_blue_team", "ブルーチームの勝利"),
        {
            CustomCommand.increaseWinCount(customGame.blueTeam)
            CustomCommand.increaseLoseCount(customGame.redTeam)
            closeGame()
        }
    ),
    RED_TEAM_WON(
        Button.danger("win_red_team", "レッドチームの勝利"),
        {
            CustomCommand.increaseWinCount(customGame.redTeam)
            CustomCommand.increaseLoseCount(customGame.blueTeam)
            closeGame()
        }
    ),
    CREATE_SAME_GAME(
        Button.primary("create_same_game", "メンバーを引き継いでゲームを作成する").withEmoji(Emoji.fromFormatted("\uD83D\uDD04")),
        {
            CustomCommand.createCustomGame(channel, user) {
                customGame.summoners.forEach { summoner ->
                    this.joinGame(summoner.user)
                }
                channel.editMessageEmbedsById(this.id, this.generateSummonersEmbed()).queue()
                reply("カスタムを作成しました。 ID: ${this.id}")
                    .addActionRow(CREATE_TEAM.button)
                    .setEphemeral(true).queue()
            }
        }
    ),
    CREATE_REVENGE_GAME(
        Button.danger("create_revenge_game", "リベンジマッチを作成する").withEmoji(Emoji.fromFormatted("❤️\u200D\uD83D\uDD25")),
        {
            CustomCommand.createCustomGame(channel, user) {
                this.state = CustomGame.State.IN_GAME
                this.summoners.addAll(customGame.summoners)
                this.blueTeam = customGame.blueTeam
                this.redTeam = customGame.redTeam
                channel.editMessageEmbedsById(this.id, this.generateTeamEmbed()).queue()
                reply("カスタムを作成しました。 ID: ${this.id}")
                    .addActionRow(BLUE_TEAM_WON.button, RED_TEAM_WON.button)
                    .setEphemeral(true).queue()
            }
        }
    )
}