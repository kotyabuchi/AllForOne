package com.github.kotyabuchi.AllForOne.LoL.Table

import org.jetbrains.exposed.sql.Table

object CustomStats: Table() {
    val id = long("id")
    val totalGame = integer("total_game")
    val win = integer("win")
    val lose = integer("lose")

    override val primaryKey = PrimaryKey(id)
}