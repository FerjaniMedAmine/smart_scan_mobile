package com.example.smartscan;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class SummaryService {

    private static final String TAG = "SummaryService";
    private static final String HF_TOKEN = BuildConfig.HUGGING_FACE_TOKEN;

    public interface SummaryCallback {
        void onSuccess(String summary, String keywords);
        void onError(String message);
    }

    private final HuggingFaceApi api;

    public SummaryService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.redactHeader("Authorization");
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://router.huggingface.co/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(HuggingFaceApi.class);
    }

    public void summarize(String rawText, SummaryCallback callback) {
        String normalized = rawText == null ? "" : rawText.trim();
        if (normalized.length() < 10) {
            callback.onError("Text is too short to summarize.");
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("inputs", normalized);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_length", 120);
        parameters.put("min_length", 30);
        parameters.put("do_sample", false);
        request.put("parameters", parameters);

        api.summarize("Bearer " + HF_TOKEN, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseText = response.body().string();
                        String summaryText = extractSummaryText(responseText);
                        deliverSummary(summaryText, normalized, callback);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse Hugging Face response", e);
                        callback.onError("Failed to parse summary response.");
                    }
                } else {
                    String errorDetail = "";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = response.errorBody().string();
                        }
                    } catch (Exception ignored) {
                    }

                    Log.e(TAG, "API Error " + response.code() + ": " + errorDetail);
                    callback.onError("API Error " + response.code() + ": " + (errorDetail.isEmpty() ? "Check logs" : errorDetail));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network failure calling Hugging Face", t);
                callback.onError("Network failure: " + t.getMessage());
            }
        });
    }

    private String extractSummaryText(String responseText) {
        String trimmed = responseText == null ? "" : responseText.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        try {
            if (trimmed.startsWith("[")) {
                JSONArray array = new JSONArray(trimmed);
                if (array.length() > 0) {
                    JSONObject first = array.optJSONObject(0);
                    if (first != null) {
                        return first.optString("summary_text", "");
                    }
                }
            } else if (trimmed.startsWith("{")) {
                JSONObject object = new JSONObject(trimmed);
                return object.optString("summary_text", object.optString("error", ""));
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected Hugging Face JSON shape", e);
        }

        return trimmed;
    }

    private void deliverSummary(String summaryText, String originalText, SummaryCallback callback) {
        String summary = summaryText == null ? "" : summaryText.trim();
        String keywords = buildKeywords(originalText);
        callback.onSuccess(summary, keywords);
    }

    private String buildKeywords(String text) {
        String[] stopWordsArray = {"the", "a", "an", "and", "or", "to", "of", "in", "on", "for", "with", "is", "are", "le", "la", "les", "de", "des", "et", "ou", "dans", "pour", "avec", "est", "une", "un"};
        Set<String> stopWords = new HashSet<>(Arrays.asList(stopWordsArray));
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        StringBuilder keywords = new StringBuilder();
        int count = 0;
        Set<String> used = new HashSet<>();
        for (String word : words) {
            if (word.length() < 4 || stopWords.contains(word) || used.contains(word)) {
                continue;
            }
            if (keywords.length() > 0) {
                keywords.append(", ");
            }
            keywords.append(word);
            used.add(word);
            if (++count >= 6) {
                break;
            }
        }
        return keywords.toString();
    }
}
