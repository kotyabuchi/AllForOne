package com.github.kotyabuchi.AllForOne

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

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

fun transactionWithLogger(callBack: Transaction.() -> Unit) {
    transaction {
        addLogger(StdOutSqlLogger)
        callBack.invoke(this)
    }
}