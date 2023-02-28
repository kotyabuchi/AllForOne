package com.github.kotyabuchi.AllForOne

import org.jetbrains.exposed.sql.Database

object DBConnector {
    private lateinit var dbFilePath: String

    fun registerDBFile(path: String): DBConnector {
        dbFilePath = "jdbc:sqlite:$path"
        return this
    }

    fun connect() {
        Database.connect(dbFilePath, "org.sqlite.JDBC")
    }
}