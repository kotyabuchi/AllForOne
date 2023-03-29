package com.github.kotyabuchi.AllForOne.LoL

import net.dv8tion.jda.api.entities.User

data class Summoner(val name: String, val id: Long, val user: User, val tier: RankTier, val division: RankDivision? = null, val additionalPoint: Int, val rankPoint: Int = tier.point + (if (tier.hasDivision && division != null) division.point else 0))