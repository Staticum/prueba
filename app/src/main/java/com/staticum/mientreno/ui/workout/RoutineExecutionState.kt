package com.staticum.mientreno.ui.workout

enum class ExecutionPhase { LOADING, EXERCISE, RESTING, DONE }

data class RoutineExecutionState(
    val routineName: String = "",
    val steps: List<ExecutionStep> = emptyList(),
    val currentIndex: Int = 0,
    val phase: ExecutionPhase = ExecutionPhase.LOADING,
    val remainingSeconds: Int = 0
) {
    val currentStep: ExecutionStep?
        get() = steps.getOrNull(currentIndex)

    val progressLabel: String
        get() = if (steps.isEmpty()) "" else "${currentIndex + 1} / ${steps.size}"
}
