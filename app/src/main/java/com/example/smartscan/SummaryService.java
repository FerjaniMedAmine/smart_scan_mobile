package com.example.smartscan;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SummaryService {

    private static final String TAG = "SummaryService";
    // Place your Gemini API Key here
    private static final String GEMINI_API_KEY = "AIzaSyAMwK1HC-y_R7yawmiyDD5fwx5IuVSf30Q";

    public interface SummaryCallback {
        void onSuccess(String summary, String keywords);
        void onError(String message);
    }

    private final GeminiApi api;

    public SummaryService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(GeminiApi.class);
    }

    public void summarize(String rawText, SummaryCallback callback) {
        String normalized = rawText == null ? "" : rawText.trim();
        if (normalized.length() < 10) {
            callback.onError("Text is too short to summarize.");
            return;
        }

        // Constructing a detailed prompt for Gemini to get both summary and keywords
        String prompt = "Please analyze the following text and provide two things: " +
                "1. A concise summary (max 3 sentences). " +
                "2. A list of 5-6 key words or phrases. " +
                "Format your response EXACTLY like this:\n" +
                "Summary: [The summary text]\n" +
                "Keywords: [word1, word2, ...]\n\n" +
                "Text:\n" + normalized;

        GeminiModels.Request request = new GeminiModels.Request(prompt);

        api.generateContent(GEMINI_API_KEY, request).enqueue(new Callback<GeminiModels.Response>() {
            @Override
            public void onResponse(Call<GeminiModels.Response> call, Response<GeminiModels.Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String fullText = response.body().getText();
                    if (fullText != null && !fullText.trim().isEmpty()) {
                        parseGeminiResponse(fullText, callback, normalized);
                    } else {
                        callback.onError("Empty response from Gemini.");
                    }
                } else {
                    String errorDetail = "";
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    
                    Log.e(TAG, "API Error " + response.code() + ": " + errorDetail);
                    callback.onError("API Error " + response.code() + ": " + (errorDetail.isEmpty() ? "Check logs" : errorDetail));
                }
            }

            @Override
            public void onFailure(Call<GeminiModels.Response> call, Throwable t) {
                Log.e(TAG, "Network failure calling Gemini", t);
                callback.onError("Network failure: " + t.getMessage());
            }
        });
    }

    private void parseGeminiResponse(String responseText, SummaryCallback callback, String originalText) {
        String summary = "";
        String keywords = "";

        // Simple parsing logic based on the requested format
        String[] lines = responseText.split("\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("summary:")) {
                summary = line.substring("summary:".length()).trim();
            } else if (line.toLowerCase().startsWith("keywords:")) {
                keywords = line.substring("keywords:".length()).trim();
            }
        }

        // Fallbacks if parsing fails
        if (summary.isEmpty()) {
            summary = responseText.length() > 200 ? responseText.substring(0, 197) + "..." : responseText;
        }
        if (keywords.isEmpty()) {
            keywords = buildFallbackKeywords(originalText);
        }

        callback.onSuccess(summary, keywords);
    }

    private String buildFallbackKeywords(String text) {
        String[] stopWordsArray = {"the", "a", "an", "and", "or", "to", "of", "in", "on", "for", "with", "is", "are", "le", "la", "les", "de", "des", "et", "ou", "dans", "pour", "avec", "est", "une", "un"};
        Set<String> stopWords = new HashSet<>(Arrays.asList(stopWordsArray));
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        StringBuilder keywords = new StringBuilder();
        int count = 0;
        Set<String> used = new HashSet<>();
        for (String word : words) {
            if (word.length() < 4 || stopWords.contains(word) || used.contains(word)) continue;
            if (keywords.length() > 0) keywords.append(", ");
            keywords.append(word);
            used.add(word);
            if (++count >= 6) break;
        }
        return keywords.toString();
    }

    private String buildFallbackSummary(String text) {
        String clean = text.replace("\n", " ").replaceAll("\\s+", " ").trim();
        String[] sentences = clean.split("(?<=[.!?])\\s+");
        if (sentences.length > 2) {
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(sentences.length, 3);
            for (int i = 0; i < limit; i++) {
                sb.append(sentences[i]).append(" ");
            }
            return sb.toString().trim();
        }
        return clean.length() > 200 ? clean.substring(0, 197) + "..." : clean;
    }

    private String buildKeywords(String text) {
        String[] stopWordsArray = {"the", "a", "an", "and", "or", "to", "of", "in", "on", "for", "with", "is", "are", "le", "la", "les", "de", "des", "et", "ou", "dans", "pour", "avec", "est", "une", "un"};
        Set<String> stopWords = new HashSet<>(Arrays.asList(stopWordsArray));
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        StringBuilder keywords = new StringBuilder();
        int count = 0;
        Set<String> used = new HashSet<>();
        for (String word : words) {
            if (word.length() < 4 || stopWords.contains(word) || used.contains(word)) continue;
            if (keywords.length() > 0) keywords.append(", ");
            keywords.append(word);
            used.add(word);
            if (++count >= 6) break;
        }
        return keywords.toString();
    }
}
