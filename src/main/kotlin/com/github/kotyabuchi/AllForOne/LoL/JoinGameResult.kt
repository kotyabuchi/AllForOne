package com.github.kotyabuchi.AllForOne.LoL

import com.github.kotyabuchi.AllForOne.LoL.Command.SummonerCommand
import net.dv8tion.jda.api.interactions.modals.Modal

sealed interface JoinGameResult {
    object Success: JoinGameResult

    sealed interface Fail: JoinGameResult {
        val reason: String
        object NotRegistered: Fail {
            override val reason: String = "ランク情報が登録されていません。"
            fun getModal(): Modal = SummonerCommand.ModalRegisterCommand.createRegisterModal()
        }

        object AlreadyJoined: Fail {
            override val reason: String = "既に参加しています。"
        }

        object FilledSummoner: Fail {
            override val reason: String = "参加者が10人に達しています。"
        }

        object Undefined: Fail {
            override val reason: String = "undefined"
        }
    }
}