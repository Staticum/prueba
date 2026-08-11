package com.staticum.mientreno.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "routine_templates")
data class RoutineTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_blocks")
data class RoutineBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val orderIndex: Int,
    val name: String? = null,
    val rounds: Int = 1,
    val restBetweenRoundsSeconds: Int = 60
)

@Entity(tableName = "routine_exercises")
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blockId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val measureType: MeasureType,
    val orderIndex: Int,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Int? = null,
    val restAfterSeconds: Int = 30,
    val notes: String? = null
)

data class BlockWithExercises(
    @Embedded val block: RoutineBlock,
    @Relation(
        entity = RoutineExercise::class,
        parentColumn = "id",
        entityColumn = "blockId"
    )
    val exercises: List<RoutineExercise>
)

data class RoutineWithBlocks(
    @Embedded val routine: RoutineTemplate,
    @Relation(
        entity = RoutineBlock::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val blocks: List<BlockWithExercises>
)
