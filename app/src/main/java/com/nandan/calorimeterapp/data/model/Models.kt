package com.nandan.calorimeterapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// ── Request models ──────────────────────────────────────────────────────────

@Keep
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

@Keep
data class StreakUpdateRequest(
    val uid: String,
    val date: String,
)

// ── Response models ─────────────────────────────────────────────────────────

@Keep
data class StreakData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String = "",
    val totalTrackingDays: Int = 0,
    val streakFreezeAvailable: Int = 0,
    val streakFreezeUsedDate: String = "",
    val milestones: List<Int> = emptyList(),
    val history: List<String> = emptyList(),
)

@Keep
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

@Keep
data class SearchResult(
    val name: String = "",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val imageUrl: String = "",
)

@Keep
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

@Keep
data class AnalyzeResult(
    val name: String = "",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    @SerializedName("imageUrl") val imageUrl: String = "",
    val confidence: String = "",
)

@Keep
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

