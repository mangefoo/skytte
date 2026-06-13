package se.mindphaser.skytte.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.mindphaser.skytte.data.SessionDao
import se.mindphaser.skytte.data.SessionWithRefs
import se.mindphaser.skytte.ui.database
import java.time.LocalDate
import java.time.YearMonth

/** [label] is null when the session had no weapon/ammo set; the UI substitutes a localized label. */
data class LabeledCount(val label: String?, val shots: Int)

/** One month's value in a monthly chart; [value] is shots fired or session count depending on the series. */
data class MonthBucket(val month: YearMonth, val value: Int)

data class DashboardStats(
    val totalShots: Int,
    val shotsThisYear: Int,
    val shotsLast30Days: Int,
    val totalSessions: Int,
    val sessionsThisYear: Int,
    val shotsPerWeapon: List<LabeledCount>,
    val shotsPerAmmo: List<LabeledCount>,
    val monthlyShots: List<MonthBucket>,
    val monthlySessions: List<MonthBucket>
) {
    companion object {
        val EMPTY = DashboardStats(
            totalShots = 0,
            shotsThisYear = 0,
            shotsLast30Days = 0,
            totalSessions = 0,
            sessionsThisYear = 0,
            shotsPerWeapon = emptyList(),
            shotsPerAmmo = emptyList(),
            monthlyShots = emptyList(),
            monthlySessions = emptyList()
        )
    }
}

class DashboardViewModel(dao: SessionDao) : ViewModel() {
    val stats: Flow<DashboardStats> = dao.observeAll().map { sessions ->
        buildStats(sessions, LocalDate.now())
    }

    companion object {
        const val MONTHS_SHOWN = 12

        val Factory = viewModelFactory {
            initializer { DashboardViewModel(database().sessionDao()) }
        }

        fun buildStats(sessions: List<SessionWithRefs>, today: LocalDate): DashboardStats {
            val currentMonth = YearMonth.from(today)
            val last30DaysStart = today.minusDays(30)

            val perWeapon = sessions
                .groupBy { it.weapon?.name?.takeIf(String::isNotBlank) }
                .map { (name, list) -> name to list.sumOf { it.session.ammoCount } }
                .filter { it.second > 0 }

            val perAmmo = sessions
                .groupBy { it.ammunition?.name?.takeIf(String::isNotBlank) }
                .map { (name, list) -> name to list.sumOf { it.session.ammoCount } }
                .filter { it.second > 0 }

            val byMonth = sessions.groupBy { YearMonth.from(it.session.date) }
            val months = (0 until MONTHS_SHOWN).map { offset ->
                currentMonth.minusMonths((MONTHS_SHOWN - 1 - offset).toLong())
            }
            val shotBuckets = months.map { month ->
                MonthBucket(month, byMonth[month]?.sumOf { it.session.ammoCount } ?: 0)
            }
            val sessionBuckets = months.map { month ->
                MonthBucket(month, byMonth[month]?.size ?: 0)
            }

            return DashboardStats(
                totalShots = sessions.sumOf { it.session.ammoCount },
                shotsThisYear = sessions.filter { it.session.date.year == today.year }
                    .sumOf { it.session.ammoCount },
                shotsLast30Days = sessions
                    .filter { !it.session.date.isBefore(last30DaysStart) && !it.session.date.isAfter(today) }
                    .sumOf { it.session.ammoCount },
                totalSessions = sessions.size,
                sessionsThisYear = sessions.count { it.session.date.year == today.year },
                shotsPerWeapon = perWeapon.toLabeledCounts(),
                shotsPerAmmo = perAmmo.toLabeledCounts(),
                monthlyShots = shotBuckets,
                monthlySessions = sessionBuckets
            )
        }

        private fun List<Pair<String?, Int>>.toLabeledCounts(): List<LabeledCount> =
            sortedByDescending { it.second }
                .map { (name, shots) -> LabeledCount(name, shots) }
    }
}
