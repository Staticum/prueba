package com.staticum.diariocalorico.ui.dashboard

import com.staticum.diariocalorico.data.DailyGoals
import java.time.LocalDate

/** Resumen de un día cerrado, usado solo para calcular patrones — no requiere llamar a Gemini. */
data class DayInsightData(
    val date: LocalDate,
    val consumedCalories: Int,
    val consumedProtein: Double,
    val expenditure: Int?
)

enum class InsightSeverity { INFO, WARNING }

data class DashboardInsight(val message: String, val severity: InsightSeverity)

/**
 * Detecta patrones simples en los últimos días cerrados (déficit/superávit sostenido, proteína
 * baja sostenida) comparando números que ya están guardados, sin usar la IA. El objetivo es una
 * señal temprana que no dependa de que la persona entre a pedir un análisis.
 */
object DashboardInsights {
    private const val MIN_DAYS_FOR_STREAK = 3
    private const val DEFICIT_THRESHOLD_KCAL = 300
    private const val LOW_PROTEIN_RATIO = 0.8

    fun compute(days: List<DayInsightData>, goals: DailyGoals): List<DashboardInsight> {
        val insights = mutableListOf<DashboardInsight>()

        val withExpenditure = days.filter { it.expenditure != null }
        if (withExpenditure.size >= MIN_DAYS_FOR_STREAK) {
            val recent = withExpenditure.sortedByDescending { it.date }.take(MIN_DAYS_FOR_STREAK)
            val diffs = recent.map { it.consumedCalories - (it.expenditure ?: 0) }
            if (diffs.all { it > DEFICIT_THRESHOLD_KCAL }) {
                val avg = diffs.average().toInt()
                insights += DashboardInsight(
                    "Llevas ${recent.size} días seguidos con superávit calórico (~$avg kcal/día por sobre tu gasto medido). " +
                        "Si tu objetivo es bajar o mantener peso, vale la pena revisar las porciones.",
                    InsightSeverity.WARNING
                )
            } else if (diffs.all { it < -DEFICIT_THRESHOLD_KCAL }) {
                val avg = -diffs.average().toInt()
                insights += DashboardInsight(
                    "Llevas ${recent.size} días seguidos con un déficit calórico marcado (~$avg kcal/día por debajo de tu gasto). " +
                        "Si no es intencional, considera que puede afectar tu energía y masa muscular.",
                    InsightSeverity.WARNING
                )
            }
        }

        if (days.size >= MIN_DAYS_FOR_STREAK && goals.proteinGrams > 0) {
            val recent = days.sortedByDescending { it.date }.take(MIN_DAYS_FOR_STREAK)
            val lowProteinThreshold = goals.proteinGrams * LOW_PROTEIN_RATIO
            if (recent.all { it.consumedProtein < lowProteinThreshold }) {
                insights += DashboardInsight(
                    "Llevas ${recent.size} días seguidos bajo el 80% de tu meta de proteína. " +
                        "Podría estar costándote recuperación muscular.",
                    InsightSeverity.INFO
                )
            }
        }

        return insights
    }
}
