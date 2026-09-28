package com.staticum.diariocalorico.ui.savedfoods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.SavedFood
import com.staticum.diariocalorico.data.SavedFoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SavedFoodsViewModel(private val repository: SavedFoodRepository) : ViewModel() {
    private val _foods = MutableStateFlow<List<SavedFood>>(emptyList())
    val foods: StateFlow<List<SavedFood>> = _foods

    init {
        repository.observeAll().onEach { _foods.value = it }.launchIn(viewModelScope)
    }

    fun add(name: String, calories: Int, protein: Double, carbs: Double, fat: Double) {
        viewModelScope.launch {
            repository.save(
                SavedFood(
                    name = name,
                    calories = calories,
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    detectedFoods = name
                )
            )
        }
    }

    fun update(food: SavedFood) {
        viewModelScope.launch { repository.update(food) }
    }

    fun delete(food: SavedFood) {
        viewModelScope.launch { repository.delete(food) }
    }
}
