package com.example.smartscan;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface HuggingFaceApi {

    @POST("hf-inference/models/facebook/bart-large-cnn")
    Call<ResponseBody> summarize(
            @Header("Authorization") String authorization,
            @Body Map<String, Object> request);
}
