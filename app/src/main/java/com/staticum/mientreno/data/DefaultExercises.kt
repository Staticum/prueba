package com.staticum.mientreno.data

object DefaultExercises {

    val all: List<Exercise> = listOf(
        // Correr / cardio al aire libre
        Exercise(
            name = "Trote continuo",
            category = ExerciseCategory.CORRER,
            type = ExerciseType.CARDIO,
            equipment = "Zapatillas",
            instructions = "Mantén un ritmo constante y conversable durante todo el recorrido.",
            defaultRestSeconds = 0
        ),
        Exercise(
            name = "Intervalos (sprint / trote suave)",
            category = ExerciseCategory.CORRER,
            type = ExerciseType.CARDIO,
            equipment = "Zapatillas",
            instructions = "Alterna tramos rápidos con tramos de recuperación al trote suave.",
            defaultRestSeconds = 60
        ),
        Exercise(
            name = "Caminata de recuperación",
            category = ExerciseCategory.CORRER,
            type = ExerciseType.CARDIO,
            equipment = "Zapatillas",
            instructions = "Ritmo suave para bajar pulsaciones.",
            defaultRestSeconds = 0
        ),

        // Kettlebell
        Exercise(
            name = "Kettlebell swing",
            category = ExerciseCategory.KETTLEBELL,
            type = ExerciseType.FUERZA,
            equipment = "Kettlebell",
            instructions = "Empuja la cadera hacia atrás y extiende con fuerza de cadera, no de brazos.",
            defaultRestSeconds = 60
        ),
        Exercise(
            name = "Goblet squat con kettlebell",
            category = ExerciseCategory.KETTLEBELL,
            type = ExerciseType.FUERZA,
            equipment = "Kettlebell",
            instructions = "Sostén la kettlebell contra el pecho y baja controlando la rodilla.",
            defaultRestSeconds = 60
        ),
        Exercise(
            name = "Kettlebell clean and press",
            category = ExerciseCategory.KETTLEBELL,
            type = ExerciseType.FUERZA,
            equipment = "Kettlebell",
            instructions = "Sube la kettlebell al hombro y empuja hacia arriba con control.",
            defaultRestSeconds = 90
        ),
        Exercise(
            name = "Kettlebell Turkish get-up",
            category = ExerciseCategory.KETTLEBELL,
            type = ExerciseType.FUERZA,
            equipment = "Kettlebell",
            instructions = "Movimiento lento y controlado desde el suelo hasta de pie.",
            defaultRestSeconds = 90
        ),

        // Gimnasio
        Exercise(
            name = "Sentadilla con barra",
            category = ExerciseCategory.GIMNASIO,
            type = ExerciseType.FUERZA,
            equipment = "Barra y discos",
            instructions = "Espalda recta, baja hasta paralelo controlando la técnica.",
            defaultRestSeconds = 90
        ),
        Exercise(
            name = "Press de banca",
            category = ExerciseCategory.GIMNASIO,
            type = ExerciseType.FUERZA,
            equipment = "Barra y banco",
            instructions = "Baja la barra controlada hasta el pecho y empuja sin rebotar.",
            defaultRestSeconds = 90
        ),
        Exercise(
            name = "Remo con máquina",
            category = ExerciseCategory.GIMNASIO,
            type = ExerciseType.FUERZA,
            equipment = "Máquina de remo",
            instructions = "Aprieta los omóplatos al final del movimiento.",
            defaultRestSeconds = 60
        ),
        Exercise(
            name = "Peso muerto",
            category = ExerciseCategory.GIMNASIO,
            type = ExerciseType.FUERZA,
            equipment = "Barra y discos",
            instructions = "Mantén la barra cerca del cuerpo, espalda neutra.",
            defaultRestSeconds = 120
        ),
        Exercise(
            name = "Prensa de piernas",
            category = ExerciseCategory.GIMNASIO,
            type = ExerciseType.FUERZA,
            equipment = "Máquina de prensa",
            instructions = "No bloquees las rodillas al extender.",
            defaultRestSeconds = 90
        ),
        Exercise(
            name = "Bicicleta estática",
            category = ExerciseCategory.GIMNASIO,
            type = ExerciseType.CARDIO,
            equipment = "Bicicleta estática",
            instructions = "Mantén una cadencia constante.",
            defaultRestSeconds = 0
        ),

        // Casa / peso corporal
        Exercise(
            name = "Flexiones de brazos",
            category = ExerciseCategory.CASA,
            type = ExerciseType.FUERZA,
            equipment = "Ninguno",
            instructions = "Cuerpo recto, baja el pecho cerca del suelo.",
            defaultRestSeconds = 45
        ),
        Exercise(
            name = "Sentadilla con peso corporal",
            category = ExerciseCategory.CASA,
            type = ExerciseType.FUERZA,
            equipment = "Ninguno",
            instructions = "Baja controlando la rodilla, talones apoyados.",
            defaultRestSeconds = 45
        ),
        Exercise(
            name = "Plancha abdominal",
            category = ExerciseCategory.CASA,
            type = ExerciseType.FUERZA,
            equipment = "Ninguno",
            instructions = "Cuerpo recto de la cabeza a los talones, aprieta el abdomen.",
            defaultRestSeconds = 30
        ),
        Exercise(
            name = "Zancadas alternadas",
            category = ExerciseCategory.CASA,
            type = ExerciseType.FUERZA,
            equipment = "Ninguno",
            instructions = "Rodilla trasera cerca del suelo, tronco erguido.",
            defaultRestSeconds = 45
        ),
        Exercise(
            name = "Burpees",
            category = ExerciseCategory.CASA,
            type = ExerciseType.FUERZA,
            equipment = "Ninguno",
            instructions = "Movimiento explosivo, mantén el ritmo constante.",
            defaultRestSeconds = 45
        ),
        Exercise(
            name = "Mountain climbers",
            category = ExerciseCategory.CASA,
            type = ExerciseType.FUERZA,
            equipment = "Ninguno",
            instructions = "Cadera baja y estable, lleva las rodillas al pecho rápido.",
            defaultRestSeconds = 30
        ),
        Exercise(
            name = "Saltos de tijera (jumping jacks)",
            category = ExerciseCategory.CASA,
            type = ExerciseType.CARDIO,
            equipment = "Ninguno",
            instructions = "Ritmo constante para elevar pulsaciones.",
            defaultRestSeconds = 30
        )
    )
}
