package com.staticum.mientreno.data

enum class ExerciseCategory(val label: String) {
    CORRER("Correr"),
    KETTLEBELL("Kettlebell"),
    GIMNASIO("Gimnasio"),
    CASA("Casa / peso corporal"),
    OTRO("Otro")
}

enum class MeasureType(val label: String) {
    REPS("Repeticiones"),
    TIME("Tiempo (cronómetro con voz)")
}
