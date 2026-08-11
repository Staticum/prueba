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
            muscleGroups = "Piernas (cuádriceps, isquiotibiales, gemelos), core, sistema cardiovascular.",
            technique = "1. Aterriza con el medio pie, no con el talón.\n" +
                "2. Mantén el tronco ligeramente inclinado hacia adelante, hombros relajados.\n" +
                "3. Brazos a 90°, balanceo relajado desde el hombro, sin cruzar el cuerpo.\n" +
                "4. Ritmo conversable: deberías poder hablar en frases cortas sin ahogarte.",
            defaultRestSeconds = 0,
            suggestedDurationSeconds = 1800
        ),
        Exercise(
            name = "Intervalos (sprint / trote suave)",
            category = ExerciseCategory.CORRER,
            measureType = MeasureType.TIME,
            equipment = "Zapatillas",
            instructions = "Alterna tramos rápidos con tramos de recuperación al trote suave.",
            muscleGroups = "Piernas, glúteos, core, capacidad cardiovascular y potencia.",
            technique = "1. Calienta con al menos 5-10 minutos de trote suave antes de empezar.\n" +
                "2. En el tramo rápido, acorta y acelera la zancada sin perder la técnica.\n" +
                "3. En la recuperación, baja completamente el ritmo, no te detengas.\n" +
                "4. Termina siempre con un tramo de recuperación, nunca en el pico de esfuerzo.",
            defaultRestSeconds = 60,
            suggestedDurationSeconds = 60
        ),
        Exercise(
            name = "Caminata de recuperación",
            category = ExerciseCategory.CORRER,
            measureType = MeasureType.TIME,
            equipment = "Zapatillas",
            instructions = "Ritmo suave para bajar pulsaciones.",
            muscleGroups = "Piernas, circulación, recuperación activa.",
            technique = "1. Ritmo suelto, respiración nasal si es posible.\n" +
                "2. Postura erguida, mirada al frente.\n" +
                "3. Úsala después de esfuerzos intensos para bajar pulsaciones gradualmente.",
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
            muscleGroups = "Glúteos, isquiotibiales, espalda baja, core.",
            technique = "1. Pies un poco más anchos que los hombros, kettlebell en el suelo entre los pies.\n" +
                "2. Bisagra de cadera: empuja la cadera hacia atrás manteniendo la espalda neutra.\n" +
                "3. Extiende la cadera con fuerza para que la kettlebell suba hasta la altura del pecho.\n" +
                "4. Los brazos solo guían el peso, no lo levantan; el impulso viene de la cadera.\n" +
                "5. Deja que la kettlebell baje entre las piernas por gravedad y repite el impulso.",
            defaultRestSeconds = 60,
            suggestedReps = 15
        ),
        Exercise(
            name = "Goblet squat con kettlebell",
            category = ExerciseCategory.KETTLEBELL,
            measureType = MeasureType.REPS,
            equipment = "Kettlebell",
            instructions = "Sostén la kettlebell contra el pecho y baja controlando la rodilla.",
            muscleGroups = "Cuádriceps, glúteos, core, movilidad de cadera.",
            technique = "1. Sostén la kettlebell con ambas manos contra el pecho, codos apuntando al suelo.\n" +
                "2. Pies al ancho de hombros, puntas levemente hacia afuera.\n" +
                "3. Baja llevando la cadera atrás y abajo, rodillas siguiendo la dirección de los pies.\n" +
                "4. Baja hasta que los codos rocen la parte interna de las rodillas.\n" +
                "5. Empuja el suelo con los talones para volver arriba.",
            defaultRestSeconds = 60,
            suggestedReps = 12
        ),
        Exercise(
            name = "Kettlebell clean and press",
            category = ExerciseCategory.KETTLEBELL,
            measureType = MeasureType.REPS,
            equipment = "Kettlebell",
            instructions = "Sube la kettlebell al hombro y empuja hacia arriba con control.",
            muscleGroups = "Hombros, piernas, core, espalda; movimiento de cuerpo completo.",
            technique = "1. Desde el suelo, usa el impulso de cadera (similar al swing) para llevar la kettlebell al hombro (clean).\n" +
                "2. Deja que la kettlebell 'gire' alrededor de la muñeca en vez de golpear el antebrazo.\n" +
                "3. Desde la posición de hombro, empuja hacia arriba hasta extender el brazo (press).\n" +
                "4. Baja controlado de vuelta al hombro y luego al suelo o repite el clean.",
            defaultRestSeconds = 90,
            suggestedReps = 8
        ),
        Exercise(
            name = "Kettlebell Turkish get-up",
            category = ExerciseCategory.KETTLEBELL,
            measureType = MeasureType.REPS,
            equipment = "Kettlebell",
            instructions = "Movimiento lento y controlado desde el suelo hasta de pie.",
            muscleGroups = "Core, hombros, estabilidad de cuerpo completo.",
            technique = "1. Acuéstate con la kettlebell en una mano, brazo extendido hacia el techo.\n" +
                "2. Apóyate en el codo y luego en la mano del lado libre para incorporarte.\n" +
                "3. Lleva la pierna del lado de la kettlebell por detrás para apoyar la rodilla (posición de caballero).\n" +
                "4. Ponte de pie manteniendo el brazo siempre extendido y la mirada en la kettlebell.\n" +
                "5. Repite el camino en reversa para volver al suelo con control.",
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
            muscleGroups = "Cuádriceps, glúteos, isquiotibiales, core.",
            technique = "1. Barra apoyada en la parte alta de la espalda (trapecio), no en el cuello.\n" +
                "2. Pies al ancho de hombros, pecho arriba, mirada al frente.\n" +
                "3. Baja llevando cadera atrás y abajo, rodillas en línea con los pies.\n" +
                "4. Baja hasta que el muslo quede paralelo al suelo o un poco más si tu movilidad lo permite.\n" +
                "5. Sube empujando el suelo con todo el pie, sin perder la posición de la espalda.",
            defaultRestSeconds = 90,
            suggestedReps = 10
        ),
        Exercise(
            name = "Press de banca",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Barra y banco",
            instructions = "Baja la barra controlada hasta el pecho y empuja sin rebotar.",
            muscleGroups = "Pectoral, tríceps, deltoides anterior.",
            technique = "1. Acuéstate con los ojos bajo la barra, pies firmes en el suelo.\n" +
                "2. Escápulas retraídas y apoyadas en el banco, ligero arco natural en la espalda.\n" +
                "3. Baja la barra controlada hasta rozar el pecho, codos a unos 45-75° del cuerpo.\n" +
                "4. Empuja la barra en línea recta hacia arriba, sin rebotar en el pecho.",
            defaultRestSeconds = 90,
            suggestedReps = 10
        ),
        Exercise(
            name = "Remo con máquina",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Máquina de remo",
            instructions = "Aprieta los omóplatos al final del movimiento.",
            muscleGroups = "Espalda media, dorsales, bíceps.",
            technique = "1. Siéntate con el pecho apoyado o el torso estable, según la máquina.\n" +
                "2. Tira del agarre llevando los codos hacia atrás, cerca del cuerpo.\n" +
                "3. Al final del recorrido, aprieta los omóplatos entre sí un instante.\n" +
                "4. Vuelve controlado a la posición inicial, sin dejar caer el peso de golpe.",
            defaultRestSeconds = 60,
            suggestedReps = 12
        ),
        Exercise(
            name = "Peso muerto",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Barra y discos",
            instructions = "Mantén la barra cerca del cuerpo, espalda neutra.",
            muscleGroups = "Isquiotibiales, glúteos, espalda baja, core, trapecio.",
            technique = "1. Barra sobre la mitad del pie, pies al ancho de cadera.\n" +
                "2. Agarra la barra fuera de las piernas, espalda neutra (no redondeada), pecho arriba.\n" +
                "3. Empuja el suelo con los pies y extiende cadera y rodillas al mismo tiempo.\n" +
                "4. Mantén la barra pegada a las piernas durante todo el recorrido.\n" +
                "5. Termina de pie con cadera totalmente extendida, sin hiperextender la espalda.",
            defaultRestSeconds = 120,
            suggestedReps = 8
        ),
        Exercise(
            name = "Prensa de piernas",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.REPS,
            equipment = "Máquina de prensa",
            instructions = "No bloquees las rodillas al extender.",
            muscleGroups = "Cuádriceps, glúteos, isquiotibiales.",
            technique = "1. Pies al ancho de hombros sobre la plataforma, espalda apoyada en el respaldo.\n" +
                "2. Baja controlado hasta que las rodillas formen un ángulo de 90° aproximadamente.\n" +
                "3. Empuja sin despegar la zona lumbar del respaldo.\n" +
                "4. Extiende sin bloquear completamente las rodillas al llegar arriba.",
            defaultRestSeconds = 90,
            suggestedReps = 12
        ),
        Exercise(
            name = "Bicicleta estática",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.TIME,
            equipment = "Bicicleta estática",
            instructions = "Mantén una cadencia constante.",
            muscleGroups = "Cuádriceps, isquiotibiales, gemelos, sistema cardiovascular.",
            technique = "1. Ajusta el asiento a la altura de la cadera con la pierna casi extendida en el punto más bajo.\n" +
                "2. Mantén una cadencia constante de pedaleo, sin frenar bruscamente.\n" +
                "3. Espalda relajada, evita apoyar todo el peso en las manos.",
            defaultRestSeconds = 0,
            suggestedDurationSeconds = 1200
        ),
        Exercise(
            name = "Bicicleta de escritorio",
            category = ExerciseCategory.GIMNASIO,
            measureType = MeasureType.TIME,
            equipment = "Bicicleta estática de escritorio (niveles 1 a 9)",
            instructions = "Ajusta el nivel de resistencia indicado y pedalea al ritmo señalado.",
            muscleGroups = "Cuádriceps, isquiotibiales, gemelos, circulación; esfuerzo cardiovascular bajo-moderado.",
            technique = "1. Ajusta la resistencia al nivel indicado en cada tramo (1 a 9).\n" +
                "2. En los tramos de trabajo (nivel bajo), pedalea a un ritmo cómodo que te permita seguir escribiendo o hablando.\n" +
                "3. En los tramos de cambio de ritmo, sube el esfuerzo perceptiblemente sin llegar a quedarte sin aire.\n" +
                "4. Mantén una postura erguida aunque estés trabajando, evita encorvarte hacia el teclado.",
            defaultRestSeconds = 0,
            suggestedDurationSeconds = 900
        ),

        // Casa / peso corporal
        Exercise(
            name = "Flexiones de brazos",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.REPS,
            equipment = "Ninguno",
            instructions = "Cuerpo recto, baja el pecho cerca del suelo.",
            muscleGroups = "Pectoral, tríceps, deltoides anterior, core.",
            technique = "1. Manos un poco más anchas que los hombros, cuerpo en línea recta de cabeza a talones.\n" +
                "2. Aprieta el abdomen y los glúteos para evitar que la cadera caiga.\n" +
                "3. Baja controlado hasta que el pecho casi toque el suelo, codos a unos 45° del cuerpo.\n" +
                "4. Empuja de vuelta arriba sin perder la línea recta del cuerpo.",
            defaultRestSeconds = 45,
            suggestedReps = 15
        ),
        Exercise(
            name = "Sentadilla con peso corporal",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.REPS,
            equipment = "Ninguno",
            instructions = "Baja controlando la rodilla, talones apoyados.",
            muscleGroups = "Cuádriceps, glúteos, isquiotibiales.",
            technique = "1. Pies al ancho de hombros, brazos extendidos al frente para equilibrio.\n" +
                "2. Baja llevando la cadera atrás y abajo, talones siempre apoyados.\n" +
                "3. Rodillas en línea con los pies, sin colapsar hacia adentro.\n" +
                "4. Baja hasta donde tu movilidad lo permita manteniendo la técnica, luego sube.",
            defaultRestSeconds = 45,
            suggestedReps = 20
        ),
        Exercise(
            name = "Plancha abdominal",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.TIME,
            equipment = "Ninguno",
            instructions = "Cuerpo recto de la cabeza a los talones, aprieta el abdomen.",
            muscleGroups = "Core (recto abdominal, transverso), hombros, glúteos.",
            technique = "1. Apóyate en antebrazos y puntas de los pies, codos bajo los hombros.\n" +
                "2. Cuerpo en línea recta de la cabeza a los talones, sin subir ni bajar la cadera.\n" +
                "3. Aprieta abdomen y glúteos durante todo el tiempo, respira de forma constante.\n" +
                "4. Si sientes que la espalda baja se hunde, sube la cadera hasta recuperar la línea.",
            defaultRestSeconds = 30,
            suggestedDurationSeconds = 45
        ),
        Exercise(
            name = "Zancadas alternadas",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.REPS,
            equipment = "Ninguno",
            instructions = "Rodilla trasera cerca del suelo, tronco erguido.",
            muscleGroups = "Cuádriceps, glúteos, isquiotibiales, equilibrio.",
            technique = "1. Da un paso largo hacia adelante, tronco erguido.\n" +
                "2. Baja hasta que ambas rodillas formen un ángulo cercano a 90°, la trasera casi rozando el suelo.\n" +
                "3. Empuja con el talón de adelante para volver a la posición inicial.\n" +
                "4. Alterna la pierna en cada repetición.",
            defaultRestSeconds = 45,
            suggestedReps = 16
        ),
        Exercise(
            name = "Burpees",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.REPS,
            equipment = "Ninguno",
            instructions = "Movimiento explosivo, mantén el ritmo constante.",
            muscleGroups = "Cuerpo completo: piernas, pecho, core, hombros; cardiovascular.",
            technique = "1. Desde de pie, agáchate y apoya las manos en el suelo.\n" +
                "2. Salta los pies hacia atrás hasta quedar en posición de plancha.\n" +
                "3. Opcional: agrega una flexión de brazos.\n" +
                "4. Salta los pies de vuelta hacia las manos.\n" +
                "5. Sube explosivamente, con un salto y los brazos extendidos arriba.",
            defaultRestSeconds = 45,
            suggestedReps = 10
        ),
        Exercise(
            name = "Mountain climbers",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.TIME,
            equipment = "Ninguno",
            instructions = "Cadera baja y estable, lleva las rodillas al pecho rápido.",
            muscleGroups = "Core, hombros, piernas; cardiovascular.",
            technique = "1. Posición de plancha alta, manos bajo los hombros.\n" +
                "2. Lleva una rodilla hacia el pecho manteniendo la cadera baja y estable.\n" +
                "3. Alterna las piernas a ritmo rápido, como si corrieras en el lugar horizontalmente.\n" +
                "4. Evita que la cadera suba o se balancee de lado a lado.",
            defaultRestSeconds = 30,
            suggestedDurationSeconds = 30
        ),
        Exercise(
            name = "Saltos de tijera (jumping jacks)",
            category = ExerciseCategory.CASA,
            measureType = MeasureType.TIME,
            equipment = "Ninguno",
            instructions = "Ritmo constante para elevar pulsaciones.",
            muscleGroups = "Cuerpo completo, cardiovascular.",
            technique = "1. De pie, pies juntos y brazos a los costados.\n" +
                "2. Salta abriendo piernas y llevando los brazos por encima de la cabeza al mismo tiempo.\n" +
                "3. Salta de vuelta a la posición inicial.\n" +
                "4. Mantén un ritmo constante y aterriza suave, flexionando ligeramente las rodillas.",
            defaultRestSeconds = 30,
            suggestedDurationSeconds = 30
        )
    )
}
