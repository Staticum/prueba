package com.staticum.diariocalorico.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.MealWithPhotos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: MealRepository) : ViewModel() {
    private val _meals = MutableStateFlow<List<MealWithPhotos>>(emptyList())
    val meals: StateFlow<List<MealWithPhotos>> = _meals

    init {
        repository.observeAllMeals().onEach { _meals.value = it }.launchIn(viewModelScope)
    }

    fun deleteMeal(meal: MealEntry) {
        viewModelScope.launch { repository.deleteMeal(meal) }
    }
}
