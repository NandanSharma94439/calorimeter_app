package com.nandan.calorimeterapp.data.repository

import android.util.Log
import com.nandan.calorimeterapp.data.model.*
import com.nandan.calorimeterapp.data.network.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class FoodRepository {

    private val api = ApiClient.apiService

    suspend fun getFoods(uid: String): NetworkResult<List<FoodItem>> = safeCall {
        api.getFoods(uid)
    }

    suspend fun addFood(request: FoodRequest): NetworkResult<SimpleResponse> = safeCall {
        api.addFood(request)
    }

    suspend fun deleteFood(id: String): NetworkResult<SimpleResponse> = safeCall {
        api.deleteFood(id)
    }

    suspend fun searchFood(name: String, quantity: Double, unit: String): NetworkResult<SearchResult> = safeCall {
        api.searchFood(name, quantity, unit)
    }

    suspend fun getBarcode(code: String): NetworkResult<BarcodeResult> = safeCall {
        api.getBarcode(code)
    }

    suspend fun analyzeImage(imageBytes: ByteArray): NetworkResult<AnalyzeResult> = safeCall {
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = imageBytes.toRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("image", "photo.jpg", requestBody)
        api.analyzeImage(part)
    }

    // ── Generic error handler ────────────────────────────────────────────────

    private suspend fun <T> safeCall(call: suspend () -> T): NetworkResult<T> {
        return try {
            NetworkResult.Success(call())
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val rawMsg = try { e.response()?.errorBody()?.string() } catch (ignore: Exception) { null }
            Log.e("FoodRepository", "HTTP $code: $rawMsg")
            val parsedMsg = try {
                if (!rawMsg.isNullOrBlank()) {
                    val json = org.json.JSONObject(rawMsg)
                    json.optString("error", rawMsg)
                } else null
            } catch (ignore: Exception) {
                rawMsg
            }
            val displayMessage = if (!parsedMsg.isNullOrBlank()) parsedMsg else "Server error ($code)"
            NetworkResult.Error(displayMessage, code)
        } catch (e: java.io.IOException) {
            Log.e("FoodRepository", "Network error: ${e.message}")
            NetworkResult.Error("Network error. Check your connection.")
        } catch (e: Exception) {
            Log.e("FoodRepository", "Unknown error: ${e.message}")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
}
