package com.example.cinemaster.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🎯 MovieServiceTestMatrixGenerator
 * Đọc file MovieService_TestMatrix.md -> Sinh ra MovieService_AutoTest.java
 * (Có sẵn callGroq để bạn có thể mở rộng về sau nếu cần sinh prompt tự động)
 */
public class MovieServiceTestMatrixGenerator {

    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public static void main(String[] args) throws IOException {
        generateTestsFromMatrix(
                "D:\\CineMaster\\backend\\src\\test\\java\\com\\example\\cinemaster\\generated\\MovieService_TestMatrix.md"
        );
    }

    /**
     * Đọc file Markdown chứa bảng test và sinh ra mã JUnit tương ứng.
     */
    public static void generateTestsFromMatrix(String matrixPath) throws IOException {
        String content = Files.readString(Paths.get(matrixPath));

        // ✅ Regex bắt function: ví dụ "create(MovieRequest request) Function"
        Pattern funcPattern = Pattern.compile("(?m)^#+\\s*([a-zA-Z]+\\([^)]*\\))\\s*Function");
        Matcher funcMatcher = funcPattern.matcher(content);

        Map<String, String> functionSections = new LinkedHashMap<>();
        List<Integer> positions = new ArrayList<>();
        List<String> functions = new ArrayList<>();

        while (funcMatcher.find()) {
            functions.add(funcMatcher.group(1).trim());
            positions.add(funcMatcher.start());
        }
        positions.add(content.length());

        for (int i = 0; i < functions.size(); i++) {
            String func = functions.get(i);
            int start = positions.get(i);
            int end = positions.get(i + 1);
            String section = content.substring(start, end).trim();
            functionSections.put(func, section);
        }

        System.out.println("🎬 Found " + functionSections.size() + " function sections.\n");

        int totalTests = 0;
        StringBuilder testCode = new StringBuilder();

        testCode.append("package com.example.cinemaster.generated;\n\n")
                .append("import org.junit.jupiter.api.*;\n")
                .append("import static org.junit.jupiter.api.Assertions.*;\n\n")
                .append("/**\n * ⚙️ Auto-generated from MovieService_TestMatrix.md\n */\n")
                .append("class MovieService_AutoTest {\n\n")
                .append("    private MovieService movieService;\n\n")
                .append("    @BeforeEach\n")
                .append("    void setup() {\n")
                .append("        movieService = new MovieService(null, null);\n")
                .append("    }\n\n");

        // ✅ Regex linh hoạt cho các dòng trong bảng Markdown
        Pattern rowPattern = Pattern.compile(
                "\\|\\s*(Happy Path|Edge Case|Error Scenario|Integration)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|",
                Pattern.CASE_INSENSITIVE
        );

        for (var entry : functionSections.entrySet()) {
            String func = entry.getKey();
            String section = entry.getValue();

            System.out.println("🔍 Processing: " + func);
            Matcher rowMatcher = rowPattern.matcher(section);
            int count = 0;

            String innerClassName = func.replaceAll("[^a-zA-Z]", "") + "Tests";
            testCode.append("    @Nested\n")
                    .append("    class ").append(innerClassName).append(" {\n");

            while (rowMatcher.find()) {
                count++;
                totalTests++;

                String category = rowMatcher.group(1).trim();
                String testCase = rowMatcher.group(2).trim();
                String input = rowMatcher.group(3).trim();
                String expected = rowMatcher.group(4).trim();

                String methodName = "test_" + testCase.replaceAll("[^a-zA-Z0-9]", "_");

                testCode.append("\n        @Test\n")
                        .append("        @DisplayName(\"[").append(category).append("] ").append(testCase).append("\")\n")
                        .append("        void ").append(methodName).append("() {\n")
                        .append("            System.out.println(\"▶ Running: ").append(func).append(" → ").append(testCase).append("\");\n")
                        .append("            // 🧪 Input: ").append(input).append("\n")
                        .append("            // 🎯 Expected: ").append(expected).append("\n")
                        .append("            // TODO: Implement test\n")
                        .append("            assertTrue(true);\n")
                        .append("        }\n");
            }

            System.out.println("   ↳ Parsed " + count + " test rows.\n");
            testCode.append("    }\n\n");
        }

        testCode.append("    @AfterAll\n")
                .append("    static void summary() {\n")
                .append("        System.out.println(\"\\n✅ Total auto-generated test cases: ").append(totalTests).append("\");\n")
                .append("    }\n}\n");

        // ✅ Ghi ra file
        Path output = Paths.get(
                "D:\\CineMaster\\backend\\src\\test\\java\\com\\example\\cinemaster\\generated\\MovieService_AutoTest.java"
        );
        Files.writeString(output, testCode.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("\n✅ DONE! Generated " + totalTests + " test cases.");
        System.out.println("📂 File saved at: " + output.toAbsolutePath());
    }

    /**
     * ⚙️ Hàm tiện ích (hiện chưa dùng nhưng sẵn sàng cho mở rộng Groq API)
     */
    private static String callGroq(String prompt) throws IOException {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("temperature", 0.4);
        body.put("max_tokens", 3000);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are an expert QA test designer. Output must be clean Markdown tables only."),
                Map.of("role", "user", "content", prompt)
        ));

        String reqBody = mapper.writeValueAsString(body);

        Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(reqBody, MediaType.get("application/json; charset=utf-8")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String bodyText = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("Groq API error: " + response.code());
                System.err.println(bodyText);
                return null;
            }
            JsonNode json = mapper.readTree(bodyText);
            return json.path("choices").get(0).path("message").path("content").asText().trim();
        }
    }
}
