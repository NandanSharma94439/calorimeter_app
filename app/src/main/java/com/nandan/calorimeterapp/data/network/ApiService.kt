package com.nandan.calorimeterapp.data.network

import com.nandan.calorimeterapp.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // ── Food CRUD ────────────────────────────────────────────────────────────

    @POST("add-food")
    suspend fun addFood(@Body food: FoodRequest): SimpleResponse

    @GET("get-foods/{uid}")
    suspend fun getFoods(@Path("uid") uid: String): List<FoodItem>

    @DELETE("delete-food/{id}")
    suspend fun deleteFood(@Path("id") id: String): SimpleResponse

    // ── Search ───────────────────────────────────────────────────────────────

    @GET("search-food")
    suspend fun searchFood(
        @Query("name") name: String,
        @Query("quantity") quantity: Double,
        @Query("unit") unit: String,
    ): SearchResult

    // ── Barcode ──────────────────────────────────────────────────────────────

    @GET("barcode/{code}")
    suspend fun getBarcode(@Path("code") code: String): BarcodeResult

    // ── AI Image Analysis ────────────────────────────────────────────────────

    @Multipart
    @POST("analyze-image")
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part,
    ): AnalyzeResult
}
