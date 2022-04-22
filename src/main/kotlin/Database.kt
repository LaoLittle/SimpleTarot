package org.laolittle.plugin

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object Database {
    object User : Table() {
        val id = long("id").uniqueIndex()
        val card = integer("cards")
        val date = integer("date")
    }
    
    suspend inline fun <T> suspendTransaction(db: Database? = null, crossinline block: suspend Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO, db) { block() }
}