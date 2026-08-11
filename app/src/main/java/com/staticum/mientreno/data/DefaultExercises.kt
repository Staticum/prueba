package com.staticum.mientreno.data

object DefaultExercises {

    val all: List<Exercise> = listOf(
        // Correr / cardio al aire libre
        Exercise(
            name = "Trote continuo",
            category = ExerciseCategory.CORRER,
            measureType = MeasureType.TIME,
            equipment = "Zapatillas",
            instructions = "Mantén un ritmo constante y conversable durante todo el recorrido.",
            defaultRestSeconds = 0,
            suggestedDurationSeconds = 1800
        ),
        Exercise(
            name = "Intervalos (sprint / trote suave)",
            category = ExerciseCategory.CORRER,
            measureType = MeasureType.TIME,
            equipment = "Zapatillas",
            instructions = "Alterna tramos rápidos con tramos de recuperación al trote suave.",
            defaultRestSeconds = 60,
            suggestedDurationSeconds = 60
        ),
        Exercise(
            name = "Caminata de recuperación",
            category = ExerciseCategory.CORRER,
            measureType = MeasureType.TIME,
            equipment = "Zapatillas",
            instructions = "Ritmo suave para bajar pulsaciones.",
            defaultRestSeconds = 0,
            suggestedDurationSeconds = 600
        ),

        // Kettlebell
        Exercise(
            name = "Kettlebell swing",
            category = ExerciseCategory.KETTLEBELL,
            measureType = MeasureType.REPS,
            equipment = "Kettlebell",
            instructions = "Empuja la cadera hacia atrás y extiende con fuerza de cadera, no de brazos.",
            defaultRestSeconds = 60,
            suggestedReps = 15
        ),
        Exercise(
            name = "Goblet squat con kettlebell",
            category = ExerciseCategory.KETTLEBELL,
            measureType = MeasureType.REPS,
            equipment = "Kettlebell",
            instructions = "Sostén la kettlebell contra el pecho y baja controlando la rodilla.",
            defaultRestSeconds = 60,
            suggestedReps = 12
        ),
        Exercise(
            name = "Kettlebell clean and press",
            category = ExerciseCategory.KETTLEBELL,
            measureType = MeasureType.REPS,
            equipment = "Kettlebell",
            instructions = "Sube la kettlebell al hombro y empuja hacia arriba con control.",
            defaultRestSeconds = 90,
            suggestedReps = 8
        ),
        Exercise(
            name = "Kettlebell Turkish get-up",
            category = ExerciseCategory.KETTLEBELL,
            measureType = MeasureType.REPS,
            equipment = "Kettlebell",
            instructions = "Movimiento lento y controlado desde el suelo hasta de pie.",
            defaultRestSeconds = 90,
            suggestedReps = 5
        ),

        // Gimnasio
        Exercise(
            name = "Sentadilla con barra",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Barra y discos",
            instructions = "Espalda recta, baja hasta paralelo controlando la técnica.",
            defaultRestSeconds = 90,
            suggestedReps = 10
        ),
        Exercise(
            name = "Press de banca",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Barra y banco",
            instructions = "Baja la barra controlada hasta el pecho y empuja sin rebotar.",
            defaultRestSeconds = 90,
            suggestedReps = 10
        ),
        Exercise(
            name = "Remo con máquina",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Máquina de remo",
            instructions = "Aprieta los omóplatos al final del movimiento.",
            defaultRestSeconds = 60,
            suggestedReps = 12
        ),
        Exercise(
            name = "Peso muerto",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Barra y discos",
            instructions = "Mantén la barra cerca del cuerpo, espalda neutra.",
            defaultRestSeconds = 120,
            suggestedReps = 8
        ),
        Exercise(
            name = "Prensa de piernas",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Máquina de prensa",
            instructions = "No bloquees las rodillas al extender.",
            defaultRestSeconds = 90,
            suggestedReps = 12
        ),
        Exercise(
            name = "Bicicleta estática",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.TIME,
            equipment = "Bicicleta estática",
            instructions = "Mantén una cadencia constante.",
            defaultRestSeconds = 0,
            suggestedDurationSeconds = 1200
        ),

        // Casa / peso corporal
        Exercise(
            name = "Flexiones de brazos",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.REPS,
            equipment = "Ninguno",
            instructions = "Cuerpo recto, baja el pecho cerca del suelo.",
            defaultRestSeconds = 45,
            suggestedReps = 15
        ),
        Exercise(
            name = "Sentadilla con peso corporal",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.REPS,
            equipment = "Ninguno",
            instructions = "Baja controlando la rodilla, talones apoyados.",
            defaultRestSeconds = 45,
            suggestedReps = 20
        ),
        Exercise(
            name = "Plancha abdominal",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.TIME,
            equipment = "Ninguno",
            instructions = "Cuerpo recto de la cabeza a los talones, aprieta el abdomen.",
            defaultRestSeconds = 30,
            suggestedDurationSeconds = 45
        ),
        Exercise(
            name = "Zancadas alternadas",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.REPS,
            equipment = "Ninguno",
            instructions = "Rodilla trasera cerca del suelo, tronco erguido.",
            defaultRestSeconds = 45,
            suggestedReps = 16
        ),
        Exercise(
            name = "Burpees",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.REPS,
            equipment = "Ninguno",
            instructions = "Movimiento explosivo, mantén el ritmo constante.",
            defaultRestSeconds = 45,
            suggestedReps = 10
        ),
        Exercise(
            name = "Mountain climbers",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.TIME,
            equipment = "Ninguno",
            instructions = "Cadera baja y estable, lleva las rodillas al pecho rápido.",
            defaultRestSeconds = 30,
            suggestedDurationSeconds = 30
        ),
        Exercise(
            name = "Saltos de tijera (jumping jacks)",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.TIME,
            equipment = "Ninguno",
            instructions = "Ritmo constante para elevar pulsaciones.",
            defaultRestSeconds = 30,
            suggestedDurationSeconds = 30
        )
    )
}
