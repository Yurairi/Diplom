package com.example.smartflowusiassistant.network

import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.MultipartBody

interface ApiService {
    @Multipart
    @POST("predict")
    suspend fun processImage(
        @Part file: MultipartBody.Part
    ): Response<ApiResponse>
}
