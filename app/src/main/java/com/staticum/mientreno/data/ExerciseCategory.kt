package com.staticum.mientreno.data

enum class ExerciseCategory(val label: String) {
    CORRER("Correr"),
    KETTLEBELL("Kettlebell"),
    GIMNASIO("Gimnasio"),
    CASA("Casa / peso corporal"),
    OTRO("Otro")
}

enum class ExerciseType(val label: String) {
    FUERZA("Fuerza (series y repeticiones)"),
    CARDIO("Cardio (duración y distancia)")
}
