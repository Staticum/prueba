package com.staticum.mientreno.data

object DeskBikeRoutine {
    const val NAME = "Bici escritorio: cambios de ritmo"
    const val EXERCISE_NAME = "Bicicleta de escritorio"

    private val segments = listOf(
        "Nivel 1 · ritmo de trabajo" to 900,
        "Nivel 4 · cambio de ritmo" to 180,
        "Nivel 1 · vuelve al ritmo de trabajo" to 720,
        "Nivel 6 · cambio de ritmo" to 180,
        "Nivel 2 · ritmo un poco más firme" to 720,
        "Nivel 5 · escalera ascendente" to 120,
        "Nivel 7 · escalera ascendente" to 120,
        "Nivel 9 · pico" to 120,
        "Nivel 7 · escalera descendente" to 120,
        "Nivel 5 · escalera descendente" to 120,
        "Nivel 1 · enfriamiento" to 300
    )

    fun build(bikeExercise: Exercise): Pair<RoutineTemplate, List<Pair<RoutineBlock, List<RoutineExercise>>>> {
        val routine = RoutineTemplate(
            name = NAME,
            description = "60 minutos en la bicicleta de escritorio: ritmo de trabajo con cambios de nivel y una escalera de resistencia al final."
        )
        val exercises = segments.mapIndexed { index, (label, seconds) ->
            RoutineExercise(
                blockId = 0,
                exerciseId = bikeExercise.id,
                exerciseName = bikeExercise.name,
                category = bikeExercise.category,
                measureType = bikeExercise.measureType,
                orderIndex = index,
                targetDurationSeconds = seconds,
                restAfterSeconds = 0,
                notes = label
            )
        }
        val block = RoutineBlock(
            routineId = 0,
            orderIndex = 0,
            name = null,
            rounds = 1,
            restBetweenRoundsSeconds = 0
        )
        return routine to listOf(block to exercises)
    }
}
