package com.staticum.mientreno.ui.navigation

object Routes {
    const val ROUTINES = "routines"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    const val ROUTINE_EDITOR = "routine_editor?routineId={routineId}"
    const val ROUTINE_EXECUTION = "routine_execution/{routineId}"
    const val QUICK_LOG = "quick_log"
    const val SESSION_DETAIL = "session_detail/{sessionId}"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"

    fun routineEditor(routineId: Long? = null): String =
        if (routineId == null) "routine_editor" else "routine_editor?routineId=$routineId"

    fun routineExecution(routineId: Long): String = "routine_execution/$routineId"

    fun sessionDetail(sessionId: Long): String = "session_detail/$sessionId"

    fun exerciseDetail(exerciseId: Long): String = "exercise_detail/$exerciseId"
}
