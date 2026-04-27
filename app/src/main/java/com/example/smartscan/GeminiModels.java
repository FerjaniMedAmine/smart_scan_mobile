package com.example.smartscan;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class GeminiModels {

    // Request Models
    public static class Request {
        @SerializedName("contents")
        public List<Content> contents;
        
        public Request(String text) {
            this.contents = new ArrayList<>();
            this.contents.add(new Content(text));
        }
    }

    public static class Content {
        @SerializedName("parts")
        public List<Part> parts;
        
        public Content(String text) {
            this.parts = new ArrayList<>();
            this.parts.add(new Part(text));
        }
    }

    public static class Part {
        @SerializedName("text")
        public String text;
        
        public Part(String text) {
            this.text = text;
        }
    }

    // Response Models
    public static class Response {
        @SerializedName("candidates")
        public List<Candidate> candidates;

        public String getText() {
            if (candidates != null && !candidates.isEmpty()) {
                Candidate first = candidates.get(0);
                if (first.content != null && first.content.parts != null && !first.content.parts.isEmpty()) {
                    return first.content.parts.get(0).text;
                }
            }
            return null;
        }
    }

    public static class Candidate {
        @SerializedName("content")
        public Content content;
    }
}
