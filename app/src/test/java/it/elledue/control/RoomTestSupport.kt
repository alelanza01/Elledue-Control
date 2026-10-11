package it.elledue.control

import it.elledue.control.data.AppDatabase

internal suspend fun AppDatabase.withClosing(action: suspend (AppDatabase) -> Unit) {
    try { action(this) } finally { close() }
}
