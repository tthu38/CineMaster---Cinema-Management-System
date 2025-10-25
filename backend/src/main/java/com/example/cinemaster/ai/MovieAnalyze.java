package com.example.cinemaster.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MovieAnalyze {

    // Your Groq API key
    private static final String GROQ_API_KEY = "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";

    //  Groq endpoint
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public static void main(String[] args) {
        analyzeMovieService();
    }

    public static void analyzeMovieService() {
        try {
            String jsClassCode = """
                // movieService.js
                class MovieService {
                  constructor() {
                    this.movies = [];
                  }

                  createMovie(movie) {
                    if (!movie.title || movie.title.length > 100) throw new Error("Invalid title");
                    if (movie.duration <= 0) throw new Error("Invalid duration");
                    const exists = this.movies.find(m => m.id === movie.id);
                    if (exists) throw new Error("Movie already exists");
                    this.movies.push(movie);
                    return movie;
                  }

                  getMovieById(id) {
                    const movie = this.movies.find(m => m.id === id);
                    if (!movie) throw new Error("Movie not found");
                    return movie;
                  }

                  updateMovie(id, updated) {
                    const index = this.movies.findIndex(m => m.id === id);
                    if (index === -1) throw new Error("Movie not found");
                    this.movies[index] = { ...this.movies[index], ...updated };
                    return this.movies[index];
                  }

                  deleteMovie(id) {
                    const index = this.movies.findIndex(m => m.id === id);
                    if (index === -1) throw new Error("Movie not found");
                    this.movies.splice(index, 1);
                  }

                  getAllMovies() {
                    return this.movies;
                  }
                }
                module.exports = MovieService;
            """;

            // Prompt phân tích
            String prompt = """
                Analyze this movie management class and identify all functions that need unit testing:

                %s

                For each function, identify:
                1. Main functionality
                2. Input parameters and types
                3. Expected return values
                4. Potential edge cases
                5. Dependencies that need mocking
            """.formatted(jsClassCode);

            // Gọi AI
            String analysis = callGroq(prompt);

            if (analysis == null || analysis.isBlank()) {
                System.err.println("No analysis returned from Groq.");
                return;
            }

            // Ghi ra file
            Path output = Paths.get("D:\\CineMaster\\backend\\src\\test\\java\\com\\example\\cinemaster\\generated\\MovieService_Analysis.txt");
            Files.createDirectories(output.getParent());
            Files.writeString(output, analysis, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("Analysis saved at: " + output.toAbsolutePath());
            System.out.println("\n--- AI Output ---\n" + analysis);

        } catch (IOException e) {
            System.err.println("Error analyzing service: " + e.getMessage());
        }
    }

    private static String callGroq(String prompt) throws IOException {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("model", "llama-3.1-8b-instant");
        jsonBody.put("temperature", 0.4);
        jsonBody.put("max_tokens", 1800);
        jsonBody.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a senior QA engineer. You analyze source code to identify all functions that need unit testing, along with edge cases."),
                Map.of("role", "user", "content", prompt)
        ));

        String requestBody = mapper.writeValueAsString(jsonBody);

        Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.get("application/json; charset=utf-8")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                System.err.println("Groq API error: " + response.code());
                System.err.println("Response: " + responseBody);
                return null;
            }

            JsonNode json = mapper.readTree(responseBody);
            JsonNode contentNode = json.path("choices").get(0).path("message").path("content");

            return contentNode.isMissingNode() ? null : contentNode.asText().trim();
        }
    }
}
