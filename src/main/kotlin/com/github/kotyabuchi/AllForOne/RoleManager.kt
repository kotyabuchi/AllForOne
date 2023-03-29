package com.github.kotyabuchi.AllForOne

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object RoleManager {

    init {
        transactionWithLogger {
            SchemaUtils.create(Roles)
        }
    }

    fun registerRole(guild: Guild, role: Role) {
        transactionWithLogger {
            val roleCondition = (Roles.guildId eq guild.idLong) and (Roles.name eq role.name)
            if (Roles.select { roleCondition }.empty()) {
                Roles.insert {
                    it[guildId] = guild.idLong
                    it[name] = role.name
                    it[roleId] = role.idLong
                }
            } else {
                Roles.update({ roleCondition }) {
                    it[roleId] = role.idLong
                }
            }
        }
    }

    fun getRoleId(guild: Guild, roleName: String): Long? {
        return Roles.select {(Roles.guildId eq guild.idLong) and (Roles.name eq roleName)}.singleOrNull()?.get(Roles.roleId)
    }

    object Roles: Table() {
        val guildId = long("guild_id")
        val name = varchar("role_name", 100)
        val roleId = long("role_id")
    }
}