package com.example.smartscan;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GeminiApi {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiModels.Response> generateContent(
            @Header("x-goog-api-key") String apiKey,
            @Body GeminiModels.Request request);
}