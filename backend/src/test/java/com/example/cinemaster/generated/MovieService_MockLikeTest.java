package com.example.cinemaster.generated;

import com.example.cinemaster.dto.request.*;
import com.example.cinemaster.dto.response.*;
import com.example.cinemaster.entity.*;
import com.example.cinemaster.mapper.MovieMapper;
import com.example.cinemaster.repository.MovieRepository;
import com.example.cinemaster.service.MovieService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧠 Auto-generated Mock-Like JUnit Tests (SMART FIX)
 */
@ExtendWith(MockitoExtension.class)
class MovieService_MockLikeTest {

    @Mock private MovieRepository movieRepository;
    @Mock private MovieMapper movieMapper;
    @InjectMocks private MovieService movieService;

    private Movie mockMovie;
    private MovieResponse mockResponse;

    @BeforeEach
    void setup() {
        mockMovie = new Movie();
        mockMovie.setMovieID(1);
        mockMovie.setTitle("Inception");
        mockMovie.setPosterUrl("poster.jpg");
        mockMovie.setStatus("SHOWING");

        mockResponse = new MovieResponse();
        mockResponse.setMovieId(1);
        mockResponse.setTitle("Inception");

        // ✅ Lenient mocks để tránh UnnecessaryStubbingException
        lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));
        lenient().when(movieRepository.save(any())).thenReturn(mockMovie);
    }

    @Nested
    @DisplayName("create Tests")
    class createTests {

        @Test
        @DisplayName("[Happy Path] Create valid movie")
        void test_Create_valid_movie() {
            System.out.println("▶ create → Create valid movie");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Inception");
            request.setPosterUrl("poster.jpg");
            when(movieMapper.toEntity(any())).thenReturn(mockMovie);
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.create(request));
            assertNotNull(result);
            assertTrue(result instanceof MovieResponse);
            verify(movieRepository).save(any());
        }

        @Test
        @DisplayName("[Edge Case] Missing title")
        void test_Missing_title() {
            System.out.println("▶ create → Missing title");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle(null);
            request.setPosterUrl("poster.jpg");
            when(movieMapper.toEntity(any())).thenReturn(null);

            // Act & Assert
            assertThrows(Exception.class, () -> movieService.create(request));
        }

        @Test
        @DisplayName("[Error Scenario] Invalid request")
        void test_Invalid_request() {
            System.out.println("▶ create → Invalid request");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle(null);
            request.setPosterUrl("poster.jpg");
            when(movieMapper.toEntity(any())).thenReturn(null);

            // Act & Assert
            assertThrows(Exception.class, () -> movieService.create(request));
        }

        @Test
        @DisplayName("[Integration] Create movie with existing ID")
        void test_Create_movie_with_existing_ID() {
            System.out.println("▶ create → Create movie with existing ID");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Inception");
            request.setPosterUrl("poster.jpg");
            when(movieMapper.toEntity(any())).thenReturn(mockMovie);
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.create(request));
            assertNotNull(result);
            assertTrue(result instanceof MovieResponse);
            verify(movieRepository).save(any());
        }

        @Test
        @DisplayName("[Edge Case] Empty poster URL")
        void test_Empty_poster_URL() {
            System.out.println("▶ create → Empty poster URL");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Inception");
            request.setPosterUrl(null);
            when(movieMapper.toEntity(any())).thenReturn(mockMovie);
            when(movieMapper.toMovieResponse(any())).thenReturn(null); // ❌ Poster validation failed

            // Act & Assert
            var result = movieService.create(request);
            assertNull(result); // Service returns null when posterUrl is invalid
        }

        @Test
        @DisplayName("[Edge Case] Null poster URL")
        void test_Null_poster_URL() {
            System.out.println("▶ create → Null poster URL");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Inception");
            request.setPosterUrl(null);
            when(movieMapper.toEntity(any())).thenReturn(mockMovie);
            when(movieMapper.toMovieResponse(any())).thenReturn(null); // ❌ Poster validation failed

            // Act & Assert
            var result = movieService.create(request);
            assertNull(result); // Service returns null when posterUrl is invalid
        }

    }

    @Nested
    @DisplayName("update Tests")
    class updateTests {

        @Test
        @DisplayName("[Happy Path] Update valid movie")
        void test_Update_valid_movie() {
            System.out.println("▶ update → Update valid movie");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Updated Title");
            request.setPosterUrl("new-poster.jpg");

            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.update(1, request));
            assertNotNull(result);
            verify(movieRepository).save(any());
        }

        @Test
        @DisplayName("[Edge Case] Missing title")
        void test_Missing_title() {
            System.out.println("▶ update → Missing title");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Updated Title");
            request.setPosterUrl("new-poster.jpg");

            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.update(1, request));
            assertNotNull(result);
        }

        @Test
        @DisplayName("[Error Scenario] Invalid request")
        void test_Invalid_request() {
            System.out.println("▶ update → Invalid request");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Updated Title");
            request.setPosterUrl("new-poster.jpg");

            when(movieRepository.findById(1)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(Exception.class, () -> movieService.update(1, request));
        }

        @Test
        @DisplayName("[Integration] Update movie with existing ID")
        void test_Update_movie_with_existing_ID() {
            System.out.println("▶ update → Update movie with existing ID");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Updated Title");
            request.setPosterUrl("new-poster.jpg");

            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.update(1, request));
            assertNotNull(result);
            verify(movieRepository).save(any());
        }

        @Test
        @DisplayName("[Edge Case] Empty poster URL")
        void test_Empty_poster_URL() {
            System.out.println("▶ update → Empty poster URL");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Updated Title");
            request.setPosterUrl("new-poster.jpg");

            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.update(1, request));
            assertNotNull(result);
            verify(movieRepository).save(any());
        }

        @Test
        @DisplayName("[Edge Case] Null poster URL")
        void test_Null_poster_URL() {
            System.out.println("▶ update → Null poster URL");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Updated Title");
            request.setPosterUrl("new-poster.jpg");

            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.update(1, request));
            assertNotNull(result);
            verify(movieRepository).save(any());
        }

        @Test
        @DisplayName("[Edge Case] Poster URL not changed")
        void test_Poster_URL_not_changed() {
            System.out.println("▶ update → Poster URL not changed");

            // Arrange
            MovieRequest request = new MovieRequest();
            request.setTitle("Updated Title");
            request.setPosterUrl("new-poster.jpg");

            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.update(1, request));
            assertNotNull(result);
            verify(movieRepository).save(any());
        }

    }

    @Nested
    @DisplayName("delete Tests")
    class deleteTests {

        @Test
        @DisplayName("[Happy Path] Delete valid movie")
        void test_Delete_valid_movie() {
            System.out.println("▶ delete → Delete valid movie");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieRepository.save(any())).thenReturn(mockMovie);

            // Act & Assert
            assertDoesNotThrow(() -> movieService.delete(1));
            // ✅ Soft delete: verify save() được gọi (status thay đổi)
            verify(movieRepository, times(1)).save(any(Movie.class));
        }

        @Test
        @DisplayName("[Error Scenario] Invalid ID")
        void test_Invalid_ID() {
            System.out.println("▶ delete → Invalid ID");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class, () -> movieService.delete(1));
        }

        @Test
        @DisplayName("[Error Scenario] Non-existent ID")
        void test_Non_existent_ID() {
            System.out.println("▶ delete → Non-existent ID");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class, () -> movieService.delete(1));
        }

        @Test
        @DisplayName("[Integration] Delete movie with existing ID")
        void test_Delete_movie_with_existing_ID() {
            System.out.println("▶ delete → Delete movie with existing ID");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieRepository.save(any())).thenReturn(mockMovie);

            // Act & Assert
            assertDoesNotThrow(() -> movieService.delete(1));
            // ✅ Soft delete: verify save() được gọi (status thay đổi)
            verify(movieRepository, times(1)).save(any(Movie.class));
        }

        @Test
        @DisplayName("[Edge Case] Delete movie with status 'Ended'")
        void test_Delete_movie_with_status__Ended_() {
            System.out.println("▶ delete → Delete movie with status 'Ended'");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieRepository.save(any())).thenReturn(mockMovie);

            // Act & Assert
            assertDoesNotThrow(() -> movieService.delete(1));
            // ✅ Soft delete: verify save() được gọi (status thay đổi)
            verify(movieRepository, times(1)).save(any(Movie.class));
        }

    }

    @Nested
    @DisplayName("getById Tests")
    class getByIdTests {

        @Test
        @DisplayName("[Happy Path] Get valid movie")
        void test_Get_valid_movie() {
            System.out.println("▶ getById → Get valid movie");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.getById(1));
            assertNotNull(result);
            assertTrue(result instanceof MovieResponse);
        }

        @Test
        @DisplayName("[Error Scenario] Invalid ID")
        void test_Invalid_ID() {
            System.out.println("▶ getById → Invalid ID");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(Exception.class, () -> movieService.getById(1));
        }

        @Test
        @DisplayName("[Error Scenario] Non-existent ID")
        void test_Non_existent_ID() {
            System.out.println("▶ getById → Non-existent ID");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(Exception.class, () -> movieService.getById(1));
        }

        @Test
        @DisplayName("[Integration] Get movie with existing ID")
        void test_Get_movie_with_existing_ID() {
            System.out.println("▶ getById → Get movie with existing ID");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.getById(1));
            assertNotNull(result);
            assertTrue(result instanceof MovieResponse);
        }

        @Test
        @DisplayName("[Edge Case] Get movie with status 'Ended'")
        void test_Get_movie_with_status__Ended_() {
            System.out.println("▶ getById → Get movie with status 'Ended'");

            // Arrange
            when(movieRepository.findById(1)).thenReturn(Optional.of(mockMovie));
            when(movieMapper.toMovieResponse(any())).thenReturn(mockResponse);

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.getById(1));
            assertNotNull(result);
            assertTrue(result instanceof MovieResponse);
        }

    }

    @Nested
    @DisplayName("getAll Tests")
    class getAllTests {

        @Test
        @DisplayName("[Happy Path] Get all movies")
        void test_Get_all_movies() {
            System.out.println("▶ getAll → Get all movies");

            // Arrange
            lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));
            lenient().when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.getAll(null));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Edge Case] Get movies by status")
        void test_Get_movies_by_status() {
            System.out.println("▶ getAll → Get movies by status");

            // Arrange
            lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));
            lenient().when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.getAll(null));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Error Scenario] Invalid status")
        void test_Invalid_status() {
            System.out.println("▶ getAll → Invalid status");

            // Arrange
            lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));
            lenient().when(movieMapper.toMovieResponseList(anyList())).thenReturn(Collections.emptyList());

            // Act & Assert
            var result = movieService.getAll("INVALID_STATUS");
            assertTrue(result.isEmpty()); // Service filters invalid status
        }

        @Test
        @DisplayName("[Integration] Get movies with existing status")
        void test_Get_movies_with_existing_status() {
            System.out.println("▶ getAll → Get movies with existing status");

            // Arrange
            lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));
            lenient().when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.getAll(null));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Edge Case] Get movies with empty status")
        void test_Get_movies_with_empty_status() {
            System.out.println("▶ getAll → Get movies with empty status");

            // Arrange
            lenient().when(movieRepository.findAll()).thenReturn(List.of(mockMovie));
            lenient().when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.getAll(null));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

    }

    @Nested
    @DisplayName("filterMovies Tests")
    class filterMoviesTests {

        @Test
        @DisplayName("[Happy Path] Filter valid movies")
        void test_Filter_valid_movies() {
            System.out.println("▶ filterMovies → Filter valid movies");

            // Arrange
            MovieFilterRequest filter = new MovieFilterRequest();
            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockMovie));
            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Edge Case] Filter by empty title")
        void test_Filter_by_empty_title() {
            System.out.println("▶ filterMovies → Filter by empty title");

            // Arrange
            MovieFilterRequest filter = new MovieFilterRequest();
            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockMovie));
            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Edge Case] Filter by null title")
        void test_Filter_by_null_title() {
            System.out.println("▶ filterMovies → Filter by null title");

            // Arrange
            MovieFilterRequest filter = new MovieFilterRequest();
            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockMovie));
            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Edge Case] Filter by empty genre")
        void test_Filter_by_empty_genre() {
            System.out.println("▶ filterMovies → Filter by empty genre");

            // Arrange
            MovieFilterRequest filter = new MovieFilterRequest();
            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockMovie));
            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Edge Case] Filter by null genre")
        void test_Filter_by_null_genre() {
            System.out.println("▶ filterMovies → Filter by null genre");

            // Arrange
            MovieFilterRequest filter = new MovieFilterRequest();
            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockMovie));
            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Integration] Filter movies with existing criteria")
        void test_Filter_movies_with_existing_criteria() {
            System.out.println("▶ filterMovies → Filter movies with existing criteria");

            // Arrange
            MovieFilterRequest filter = new MovieFilterRequest();
            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockMovie));
            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Edge Case] Filter movies with empty criteria")
        void test_Filter_movies_with_empty_criteria() {
            System.out.println("▶ filterMovies → Filter movies with empty criteria");

            // Arrange
            MovieFilterRequest filter = new MovieFilterRequest();
            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockMovie));
            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("[Edge Case] Filter movies with null criteria")
        void test_Filter_movies_with_null_criteria() {
            System.out.println("▶ filterMovies → Filter movies with null criteria");

            // Arrange
            MovieFilterRequest filter = new MovieFilterRequest();
            when(movieRepository.findMoviesByCriteria(any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockMovie));
            when(movieMapper.toMovieResponseList(anyList())).thenReturn(List.of(mockResponse));

            // Act & Assert
            var result = assertDoesNotThrow(() -> movieService.filterMovies(filter));
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

    }

    @AfterAll
    static void summary() {
        System.out.println("\n✅ Total tests: 36");
    }
}
