package com.nandan.calorimeterapp.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nandan.calorimeterapp.data.model.*
import com.nandan.calorimeterapp.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val foods: List<FoodItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel : ViewModel() {

    private val repository = FoodRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun loadFoods(uid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getFoods(uid)) {
                is NetworkResult.Success -> {
                    _uiState.value = HomeUiState(foods = result.data, isLoading = false)
                }
                is NetworkResult.Error -> {
                    _uiState.value = HomeUiState(isLoading = false, error = result.message)
                    Log.e("HomeViewModel", "loadFoods: ${result.message}")
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun deleteFood(id: String, uid: String) {
        viewModelScope.launch {
            // Optimistic update — remove from list immediately
            _uiState.value = _uiState.value.copy(
                foods = _uiState.value.foods.filter { it.id != id }
            )
            when (val result = repository.deleteFood(id)) {
                is NetworkResult.Error -> {
                    // On failure, reload to restore consistent state
                    Log.e("HomeViewModel", "deleteFood failed: ${result.message}")
                    loadFoods(uid)
                }
                else -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
