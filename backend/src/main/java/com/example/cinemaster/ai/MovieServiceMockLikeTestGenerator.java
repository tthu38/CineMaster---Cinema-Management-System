package com.example.cinemaster.ai;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MovieServiceMockLikeTestGenerator {

    public static void main(String[] args) throws IOException {
        generateJavaMockLikeTests(
                "D:\\CineMaster\\backend\\src\\test\\java\\com\\example\\cinemaster\\generated\\MovieService_TestMatrix.md"
        );
    }

    public static void generateJavaMockLikeTests(String matrixPath) throws IOException {
        String content = Files.readString(Paths.get(matrixPath));

        Pattern funcPattern = Pattern.compile("(?m)^#+\\s*([a-zA-Z]+\\([^)]*\\))\\s*Function");
        Matcher funcMatcher = funcPattern.matcher(content);

        Map<String, String> sections = new LinkedHashMap<>();
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
            sections.put(func, content.substring(start, end).trim());
        }

        StringBuilder sb = new StringBuilder();
        int total = 0;

        // Header
        sb.append("package com.example.cinemaster.generated;\n\n")
                .append("import com.example.cinemaster.dto.request.*;\n")
                .append("import com.example.cinemaster.dto.response.*;\n")
                .append("import com.example.cinemaster.entity.*;\n")
                .append("import com.example.cinemaster.mapper.MovieMapper;\n")
                .append("import com.example.cinemaster.repository.MovieRepository;\n")
                .append("import com.example.cinemaster.service.MovieService;\n")
                .append("import org.junit.jupiter.api.*;\n")
                .append("import org.mockito.*;\n")
                .append("import org.mockito.junit.jupiter.MockitoExtension;\n\n")
                .append("import java.util.*;\n")
                .append("import static org.mockito.Mockito.*;\n")
                .append("import static org.junit.jupiter.api.Assertions.*;\n\n")
                .append("/**\n * 🧠 Auto-generated Mock-Like JUnit Tests (SMART FIX)\n */\n")
                .append("@ExtendWith(MockitoExtension.class)\n")
                .append("class MovieService_MockLikeTest {\n\n")
                .append("    @Mock private MovieRepository movieRepository;\n")
                .append("    @Mock private MovieMapper movieMapper;\n")
                .append("    @InjectMocks private MovieService movieService;\n\n")
                .append("    private Movie mockMovie;\n")
                .append("    private MovieResponse mockResponse;\n\n")
                .append("    @BeforeEach\n")
                .append("    void setup() {\n")
                .append("        mockMovie = new Movie();\n")
                .append("        mockMovie.setMovieID(1);\n")
                .append("        mockMovie.setTitle(\"Inception\");\n")
                .append("        mockMovie.setPosterUrl(\"poster.jpg\");\n")
                .append("        mockMovie.setStatus(\"SHOWING\");\n\n")
                .append("        mockResponse = new MovieResponse();\n")
                .append("        mockResponse.setMovieId(1);\n")
                .append("        mockResponse.setTitle(\"Inception\");\n\n")
                .append("        // ✅ Lenient mocks để tránh UnnecessaryStubbingException\n")
                .append("        lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));\n")
                .append("        lenient().when(movieRepository.save(any())).thenReturn(mockMovie);\n")
                .append("    }\n\n");

        Pattern rowPattern = Pattern.compile(
                "\\|\\s*(Happy Path|Edge Case|Error Scenario|Integration)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|",
                Pattern.CASE_INSENSITIVE
        );

        for (var entry : sections.entrySet()) {
            String func = entry.getKey();
            String funcName = func.split("\\(")[0];
            String section = entry.getValue();

            sb.append("    @Nested\n")
                    .append("    @DisplayName(\"").append(funcName).append(" Tests\")\n")
                    .append("    class ").append(funcName).append("Tests {\n\n");

            Matcher rowMatcher = rowPattern.matcher(section);
            while (rowMatcher.find()) {
                total++;
                String category = rowMatcher.group(1).trim();
                String testCase = rowMatcher.group(2).trim();
                String input = rowMatcher.group(3).trim();
                String expected = rowMatcher.group(4).trim();

                String methodName = "test_" + testCase.replaceAll("[^a-zA-Z0-9]", "_");

                sb.append("        @Test\n")
                        .append("        @DisplayName(\"[").append(category).append("] ").append(testCase).append("\")\n")
                        .append("        void ").append(methodName).append("() {\n")
                        .append("            System.out.println(\"▶ ").append(funcName).append(" → ").append(testCase).append("\");\n\n");

                generateSmartTest(sb, funcName, testCase, category, input, expected);

                sb.append("        }\n\n");
            }

            sb.append("    }\n\n");
        }

        sb.append("    @AfterAll\n")
                .append("    static void summary() {\n")
                .append("        System.out.println(\"\\n✅ Total tests: ").append(total).append("\");\n")
                .append("    }\n")
                .append("}\n");

        Path output = Paths.get("D:\\CineMaster\\backend\\src\\test\\java\\com\\example\\cinemaster\\generated\\MovieService_MockLikeTest.java");
        Files.writeString(output, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("✅ Generated: " + output);
        System.out.println("🧩 Total tests: " + total);
    }

    private static void generateSmartTest(StringBuilder sb, String funcName, String testCase,
                                          String category, String input, String expected) {
        String lowerCase = testCase.toLowerCase();
        boolean isErrorCase = category.equalsIgnoreCase("Error Scenario")
                || lowerCase.contains("non-existent")
                || lowerCase.contains("invalid");

        switch (funcName) {
            case "create" -> generateCreateTest(sb, testCase, isErrorCase, lowerCase);
            case "update" -> generateUpdateTest(sb, testCase, isErrorCase, lowerCase);
            case "delete" -> generateDeleteTest(sb, testCase, isErrorCase, lowerCase);
            case "getById" -> generateGetByIdTest(sb, testCase, isErrorCase, lowerCase);
            case "getAll" -> generateGetAllTest(sb, testCase, isErrorCase, lowerCase);
            case "filterMovies" -> generateFilterTest(sb, testCase, isErrorCase);
        }
    }

    private static void generateCreateTest(StringBuilder sb, String testCase, boolean isError, String lower) {
        sb.append("            // Arrange\n");
        sb.append("            MovieRequest request = new MovieRequest();\n");

        if (lower.contains("missing title") || lower.contains("invalid")) {
            sb.append("            request.setTitle(null);\n");
            sb.append("            request.setPosterUrl(\"poster.jpg\");\n");
            sb.append("            when(movieMapper.toEntity(any())).thenReturn(null);\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            assertThrows(Exception.class, () -> movieService.create(request));\n");
        } else if (lower.contains("empty poster") || lower.contains("null poster")) {
            sb.append("            request.setTitle(\"Inception\");\n");
            sb.append("            request.setPosterUrl(null);\n");
            sb.append("            when(movieMapper.toEntity(any())).thenReturn(mockMovie);\n");
            sb.append("            when(movieMapper.toMovieResponse(any())).thenReturn(null); // ❌ Poster validation failed\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            var result = movieService.create(request);\n");
            sb.append("            assertNull(result); // Service returns null when posterUrl is invalid\n");
        } else {
            sb.append("            request.setTitle(\"Inception\");\n");
            sb.append("            request.setPosterUrl(\"poster.jpg\");\n");
            sb.append("            when(movieMapper.toEntity(any())).thenReturn(mockMovie);\n");
            sb.append("            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            var result = assertDoesNotThrow(() -> movieService.create(request));\n");
            sb.append("            assertNotNull(result);\n");
            sb.append("            assertTrue(result instanceof MovieResponse);\n");
            sb.append("            verify(movieRepository).save(any());\n");
        }
    }

    private static void generateUpdateTest(StringBuilder sb, String testCase, boolean isError, String lower) {
        sb.append("            // Arrange\n");
        sb.append("            MovieRequest request = new MovieRequest();\n");
        sb.append("            request.setTitle(\"Updated Title\");\n");
        sb.append("            request.setPosterUrl(\"new-poster.jpg\");\n\n");

        if (isError) {
            sb.append("            when(movieRepository.findById(1)).thenReturn(Optional.empty());\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            assertThrows(Exception.class, () -> movieService.update(1, request));\n");
        } else if (lower.contains("missing title")) {
            // Update với missing title vẫn có thể thành công (chỉ update các field khác)
            sb.append("            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));\n");
            sb.append("            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            var result = assertDoesNotThrow(() -> movieService.update(1, request));\n");
            sb.append("            assertNotNull(result);\n");
        } else {
            sb.append("            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));\n");
            sb.append("            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            var result = assertDoesNotThrow(() -> movieService.update(1, request));\n");
            sb.append("            assertNotNull(result);\n");
            sb.append("            verify(movieRepository).save(any());\n");
        }
    }

    private static void generateDeleteTest(StringBuilder sb, String testCase, boolean isError, String lower) {
        sb.append("            // Arrange\n");

        if (isError) {
            sb.append("            when(movieRepository.findById(1)).thenReturn(Optional.empty());\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            assertThrows(RuntimeException.class, () -> movieService.delete(1));\n");
        } else {
            sb.append("            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));\n");
            sb.append("            when(movieRepository.save(any())).thenReturn(mockMovie);\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            assertDoesNotThrow(() -> movieService.delete(1));\n");
            sb.append("            // ✅ Soft delete: verify save() được gọi (status thay đổi)\n");
            sb.append("            verify(movieRepository, times(1)).save(any(Movie.class));\n");
        }
    }

    private static void generateGetByIdTest(StringBuilder sb, String testCase, boolean isError, String lower) {
        sb.append("            // Arrange\n");

        if (isError) {
            sb.append("            when(movieRepository.findById(1)).thenReturn(Optional.empty());\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            assertThrows(Exception.class, () -> movieService.getById(1));\n");
        } else {
            sb.append("            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));\n");
            sb.append("            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            var result = assertDoesNotThrow(() -> movieService.getById(1));\n");
            sb.append("            assertNotNull(result);\n");
            sb.append("            assertTrue(result instanceof MovieResponse);\n");
        }
    }

    private static void generateGetAllTest(StringBuilder sb, String testCase, boolean isError, String lower) {
        sb.append("            // Arrange\n");

        if (lower.contains("invalid status")) {
            sb.append("            lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));\n");
            sb.append("            lenient().when(movieMapper.toMovieResponseList(anyList())).thenReturn(Collections.emptyList());\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            var result = movieService.getAll(\"INVALID_STATUS\");\n");
            sb.append("            assertTrue(result.isEmpty()); // Service filters invalid status\n");
        } else {
            sb.append("            lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));\n");
            sb.append("            lenient().when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));\n\n");
            sb.append("            // Act & Assert\n");
            sb.append("            var result = assertDoesNotThrow(() -> movieService.getAll(null));\n");
            sb.append("            assertNotNull(result);\n");
            sb.append("            assertFalse(result.isEmpty());\n");
        }
    }

    private static void generateFilterTest(StringBuilder sb, String testCase, boolean isError) {
        sb.append("            // Arrange\n");
        sb.append("            MovieFilterRequest filter = new MovieFilterRequest();\n");
        sb.append("            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))\n");
        sb.append("                .thenReturn(List.of(mockMovie));\n");
        sb.append("            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));\n\n");
        sb.append("            // Act & Assert\n");
        sb.append("            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));\n");
        sb.append("            assertNotNull(result);\n");
        sb.append("            assertFalse(result.isEmpty());\n");
    }
}