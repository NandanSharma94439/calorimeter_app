package com.nandan.calorimeterapp.ui.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nandan.calorimeterapp.data.model.NetworkResult
import com.nandan.calorimeterapp.data.model.StreakData
import com.nandan.calorimeterapp.data.repository.StreakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StreakUiState(
    val streakData: StreakData = StreakData(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class StreakViewModel : ViewModel() {
    private val repository = StreakRepository()
    
    private val _uiState = MutableStateFlow(StreakUiState())
    val uiState = _uiState.asStateFlow()
    
    fun loadStreak(uid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getStreak(uid)) {
                is NetworkResult.Success -> {
                    _uiState.value = StreakUiState(streakData = result.data, isLoading = false)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                else -> Unit
            }
        }
    }
    
    fun updateStreak(uid: String) {
        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            when (repository.updateStreak(uid, date)) {
                is NetworkResult.Success -> {
                    loadStreak(uid)
                }
                else -> Unit
            }
        }
    }
}
