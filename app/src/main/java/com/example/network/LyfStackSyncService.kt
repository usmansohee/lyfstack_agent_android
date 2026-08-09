package com.example.network

import com.example.data.model.SyncPayload
import com.example.data.model.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface LyfStackSyncService {

    @POST
    suspend fun syncActivity(
        @Url url: String,
        @Query("range") range: String = "since_last",
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Body payload: SyncPayload
    ): Response<SyncResponse>
}
