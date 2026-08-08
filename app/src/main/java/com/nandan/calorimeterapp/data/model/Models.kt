package com.nandan.calorimeterapp.data.model

import com.google.gson.annotations.SerializedName

// ── Request models ──────────────────────────────────────────────────────────

data class FoodRequest(
    val uid: String,
    val foodName: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val imageUrl: String,
    val quantity: Double,
    val unit: String,
)

// ── Response models ─────────────────────────────────────────────────────────

data class FoodItem(
    val id: String = "",
    val uid: String = "",
    val foodName: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val imageUrl: String = "",
    val quantity: Double = 1.0,
    val unit: String = "serving",
)

data class SearchResult(
    val name: String = "",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val imageUrl: String = "",
)

data class BarcodeResult(
    val name: String = "",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val imageUrl: String = "",
    val quantity: Double = 1.0,
    val unit: String = "serving",
)

data class AnalyzeResult(
    val name: String = "",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    @SerializedName("imageUrl") val imageUrl: String = "",
    val confidence: String = "",
)

data class SimpleResponse(
    val message: String? = null,
    val error: String? = null,
)

// ── Network result wrapper ──────────────────────────────────────────────────

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int = -1) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}
