package com.github.kotyabuchi.AllForOne.LoL

import com.github.kotyabuchi.AllForOne.LoL.Command.CustomCommand
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow

class CustomButtonInteractionEvent(val customId: Long, val customGame: CustomGame, event: ButtonInteractionEvent): ButtonInteractionEvent(event.jda, event.responseNumber, event.interaction) {
    fun closeGame() {
        customGame.state = CustomGame.State.ENDED
        CustomCommand.recentGames[customGame.creator.idLong] = customGame
        editComponents(ActionRow.of(CustomEmbedButton.CREATE_SAME_GAME.button, CustomEmbedButton.CREATE_REVENGE_GAME.button)).queue()
        channel.editMessageEmbedsById(customId, customGame.generateTeamEmbed()).queue()
    }
}