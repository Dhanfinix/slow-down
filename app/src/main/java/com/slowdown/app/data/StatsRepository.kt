package com.slowdown.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.statsStore: DataStore<Preferences> by preferencesDataStore("stats")

/** One local day. [date] is ISO yyyy-MM-dd. */
data class DayStats(val date: String, val scrollMs: Long = 0, val slowedCount: Int = 0, val slowedMs: Long = 0)

/** Where the watcher and coordinator write numbers. Lets tests use a fake. */
interface StatsSink {
    suspend fun addScroll(ms: Long)
    suspend fun addSlowStart()
    suspend fun addSlowedTime(ms: Long)
}

object StatsCodec {
    fun encode(days: List<DayStats>): String =
        days.joinToString(";") { "${it.date},${it.scrollMs},${it.slowedCount},${it.slowedMs}" }

    fun decode(s: String?): List<DayStats> {
        if (s.isNullOrBlank()) return emptyList()
        return s.split(';').mapNotNull { row ->
            val f = row.split(',')
            if (f.size != 4) return@mapNotNull null
            val scroll = f[1].toLongOrNull() ?: return@mapNotNull null
            val count = f[2].toIntOrNull() ?: return@mapNotNull null
            val slowed = f[3].toLongOrNull() ?: return@mapNotNull null
            DayStats(f[0], scroll, count, slowed)
        }
    }

    /** Applies [change] to the row for [today] and keeps the newest [keep] days. */
    fun update(days: List<DayStats>, today: String, keep: Int = 60, change: (DayStats) -> DayStats): List<DayStats> {
        val cur = days.firstOrNull { it.date == today } ?: DayStats(today)
        return (days.filter { it.date != today } + change(cur)).sortedBy { it.date }.takeLast(keep)
    }

    /** The last [n] days ending [today], oldest first, with empty days filled in. */
    fun lastDays(days: List<DayStats>, today: LocalDate, n: Int = 7): List<DayStats> =
        (n - 1 downTo 0).map { back ->
            val d = today.minusDays(back.toLong()).toString()
            days.firstOrNull { it.date == d } ?: DayStats(d)
        }
}

class StatsRepository(
    context: Context,
    private val today: () -> LocalDate = { LocalDate.now() },
) : StatsSink {
    private val store = context.applicationContext.statsStore
    private val key = stringPreferencesKey("days")

    val days: Flow<List<DayStats>> = store.data.map { StatsCodec.decode(it[key]) }

    private suspend fun change(f: (DayStats) -> DayStats) {
        store.edit { p ->
            p[key] = StatsCodec.encode(StatsCodec.update(StatsCodec.decode(p[key]), today().toString(), change = f))
        }
    }

    override suspend fun addScroll(ms: Long) = change { it.copy(scrollMs = it.scrollMs + ms) }
    override suspend fun addSlowStart() = change { it.copy(slowedCount = it.slowedCount + 1) }
    override suspend fun addSlowedTime(ms: Long) = change { it.copy(slowedMs = it.slowedMs + ms) }
    suspend fun reset() { store.edit { it.remove(key) } }
}
