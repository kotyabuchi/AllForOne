package com.github.kotyabuchi.AllForOne.LoL.Table

import org.jetbrains.exposed.sql.Table

object Summoners: Table() {
    val id = long("id")
    val tier = varchar("tier", 15)
    val division = varchar("division", 3).nullable()

    override val primaryKey = PrimaryKey(id)
}