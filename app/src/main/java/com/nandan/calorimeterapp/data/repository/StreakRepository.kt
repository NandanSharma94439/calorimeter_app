package com.nandan.calorimeterapp.data.repository

import android.util.Log
import com.nandan.calorimeterapp.data.model.*
import com.nandan.calorimeterapp.data.network.ApiClient

class StreakRepository {

    private val api = ApiClient.apiService

    suspend fun getStreak(uid: String): NetworkResult<StreakData> = safeCall {
        api.getStreak(uid)
    }

    suspend fun updateStreak(uid: String, date: String): NetworkResult<SimpleResponse> = safeCall {
        api.updateStreak(StreakUpdateRequest(uid, date))
    }

    // ── Generic error handler ────────────────────────────────────────────────

    private suspend fun <T> safeCall(call: suspend () -> T): NetworkResult<T> {
        return try {
            NetworkResult.Success(call())
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val rawMsg = try { e.response()?.errorBody()?.string() } catch (ignore: Exception) { null }
            Log.e("StreakRepository", "HTTP $code: $rawMsg")
            NetworkResult.Error("Server error ($code)", code)
        } catch (e: java.io.IOException) {
            Log.e("StreakRepository", "Network error: ${e.message}")
            NetworkResult.Error("Network error. Check your connection.")
        } catch (e: Exception) {
            Log.e("StreakRepository", "Unknown error: ${e.message}")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
}
