package com.example.smartflowusiassistant.network

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("image_base64") val image: String // Base64 строка
)
