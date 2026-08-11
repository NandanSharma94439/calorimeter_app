package com.nandan.calorimeterapp.ui.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nandan.calorimeterapp.data.model.*
import com.nandan.calorimeterapp.data.repository.FoodRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AddFoodUiState(
    val foodName: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val imageUrl: String = "",
    val quantity: Double = 1.0,
    val unit: String = "serving",
    val isSearching: Boolean = false,
    val isSaving: Boolean = false,
    val searchError: String? = null,
    val saveSuccess: Boolean = false,
)

@OptIn(FlowPreview::class)
class AddFoodViewModel : ViewModel() {

    private val repository = FoodRepository()

    private val _uiState = MutableStateFlow(AddFoodUiState())
    val uiState = _uiState.asStateFlow()

    // Track search inputs for debouncing
    private val _searchTrigger = MutableSharedFlow<Triple<String, Double, String>>(replay = 1)

    // Active search job — cancels old search before launching new one
    private var searchJob: Job? = null

    init {
        // Debounced search: only fires 700ms after last input change
        viewModelScope.launch {
            _searchTrigger
                .debounce(700L)
                .distinctUntilChanged()
                .filter { (name, qty, _) -> name.length >= 2 && qty > 0 }
                .collectLatest { (name, qty, unit) ->
                    performSearch(name, qty, unit)
                }
        }
    }

    fun onFoodNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(foodName = name)
        if (name.length >= 2) {
            val state = _uiState.value
            viewModelScope.launch {
                _searchTrigger.emit(Triple(name, state.quantity, state.unit))
            }
        }
    }

    fun onQuantityChanged(qty: Double) {
        _uiState.value = _uiState.value.copy(quantity = qty)
        val state = _uiState.value
        if (state.foodName.length >= 2) {
            viewModelScope.launch {
                _searchTrigger.emit(Triple(state.foodName, qty, state.unit))
            }
        }
    }

    fun onUnitChanged(unit: String) {
        _uiState.value = _uiState.value.copy(unit = unit)
        val state = _uiState.value
        if (state.foodName.length >= 2) {
            viewModelScope.launch {
                _searchTrigger.emit(Triple(state.foodName, state.quantity, unit))
            }
        }
    }

    fun onCaloriesChanged(cal: Int) {
        _uiState.value = _uiState.value.copy(calories = cal)
    }

    fun searchNow() {
        val state = _uiState.value
        if (state.foodName.isNotBlank()) {
            viewModelScope.launch { performSearch(state.foodName, state.quantity, state.unit) }
        }
    }

    private suspend fun performSearch(name: String, qty: Double, unit: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, searchError = null)
        when (val result = repository.searchFood(name, qty, unit)) {
            is NetworkResult.Success -> {
                val d = result.data
                _uiState.value = _uiState.value.copy(
                    foodName = d.name.ifBlank { _uiState.value.foodName },
                    calories = d.calories.toInt(),
                    protein = d.protein,
                    carbs = d.carbs,
                    fat = d.fat,
                    imageUrl = d.imageUrl,
                    isSearching = false,
                )
            }
            is NetworkResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = result.message,
                )
            }
            else -> Unit
        }
    }

    fun fetchBarcode(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchError = null)
            when (val result = repository.getBarcode(code)) {
                is NetworkResult.Success -> {
                    applyBarcodeResult(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchError = result.message,
                    )
                }
                else -> Unit
            }
        }
    }

    fun applyBarcodeResult(result: BarcodeResult) {
        _uiState.value = _uiState.value.copy(
            foodName = result.name,
            calories = result.calories.toInt(),
            protein = result.protein,
            carbs = result.carbs,
            fat = result.fat,
            quantity = if (result.quantity > 0) result.quantity else 1.0,
            unit = result.unit.ifBlank { "serving" },
            imageUrl = result.imageUrl,
            isSearching = false,
        )
    }

    fun applyAnalyzeResult(result: AnalyzeResult) {
        _uiState.value = _uiState.value.copy(
            foodName = result.name,
            calories = result.calories.toInt(),
            protein = result.protein,
            carbs = result.carbs,
            fat = result.fat,
            imageUrl = result.imageUrl,
            isSearching = false,
        )
    }

    fun addFood(uid: String) {
        val s = _uiState.value
        if (s.foodName.isBlank()) return
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true)
            val request = FoodRequest(
                uid = uid,
                foodName = s.foodName,
                calories = s.calories,
                protein = s.protein,
                carbs = s.carbs,
                fat = s.fat,
                imageUrl = s.imageUrl,
                quantity = s.quantity,
                unit = s.unit,
            )
            when (val result = repository.addFood(request)) {
                is NetworkResult.Success -> {
                    val streakRepo = com.nandan.calorimeterapp.data.repository.StreakRepository()
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    streakRepo.updateStreak(uid, date)
                    _uiState.value = AddFoodUiState(saveSuccess = true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = s.copy(isSaving = false, searchError = result.message)
                    Log.e("AddFoodViewModel", "addFood error: ${result.message}")
                }
                else -> Unit
            }
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(searchError = null)
    }
}
