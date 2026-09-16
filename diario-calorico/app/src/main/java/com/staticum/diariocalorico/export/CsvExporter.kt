package com.staticum.diariocalorico.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.io.File

object CsvExporter {
    fun export(context: Context, meals: List<MealEntry>): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "diario_calorico_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { writer ->
            writer.appendLine("Fecha,Tipo,Calorias,Proteina_g,Carbohidratos_g,Grasa_g,Alimentos,Descripcion")
            meals.forEach { meal ->
                writer.appendLine(
                    listOf(
                        DateTimeFormatters.formatDateTime(meal.consumedAt),
                        meal.mealType.label,
                        meal.calories.toString(),
                        meal.proteinGrams.toString(),
                        meal.carbsGrams.toString(),
                        meal.fatGrams.toString(),
                        "\"${meal.detectedFoods.replace("\"", "'")}\"",
                        "\"${meal.description.replace("\"", "'")}\""
                    ).joinToString(",")
                )
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
