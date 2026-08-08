package com.nandan.calorimeterapp.ui.camera

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nandan.calorimeterapp.data.model.*
import com.nandan.calorimeterapp.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CameraUiState {
    data object Idle : CameraUiState()
    data object Analyzing : CameraUiState()
    data class Result(val data: AnalyzeResult) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
    data object BarcodeMode : CameraUiState()
}

class CameraViewModel : ViewModel() {

    private val repository = FoodRepository()

    private val _state = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val state = _state.asStateFlow()

    fun analyzeImage(imageBytes: ByteArray) {
        if (_state.value is CameraUiState.Analyzing) return // prevent double-tap
        viewModelScope.launch {
            _state.value = CameraUiState.Analyzing
            Log.d("CameraViewModel", "Sending ${imageBytes.size} bytes to analyze-image")
            when (val result = repository.analyzeImage(imageBytes)) {
                is NetworkResult.Success -> {
                    _state.value = CameraUiState.Result(result.data)
                }
                is NetworkResult.Error -> {
                    _state.value = CameraUiState.Error(result.message)
                }
                else -> Unit
            }
        }
    }

    fun reset() {
        _state.value = CameraUiState.Idle
    }
}
