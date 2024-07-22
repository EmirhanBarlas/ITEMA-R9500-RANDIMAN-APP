package com.example.jakar.api

import retrofit2.Response
import retrofit2.http.GET
import okhttp3.ResponseBody

interface ApiService {
    @GET("report.html")
    suspend fun fetchReport(): Response<ResponseBody>
}
