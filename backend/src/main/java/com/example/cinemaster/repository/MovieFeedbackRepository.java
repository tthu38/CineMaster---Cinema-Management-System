package com.example.cinemaster.repository;

import com.example.cinemaster.entity.MovieFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MovieFeedbackRepository extends JpaRepository<MovieFeedback, Integer> {
    List<MovieFeedback> findByMovie_MovieID(Integer movieId);
    List<MovieFeedback> findByAccount_AccountID(Integer accountId);
    int countByAccount_AccountIDAndIsSpam(Integer accountId, Boolean isSpam);

    @Modifying
    @Transactional
    void deleteByAccount_AccountIDAndIsSpamTrue(Integer accountId);

    @Modifying
    @Transactional
    void deleteByAccount_AccountID(Integer accountId);


    @Query(value = """
   SELECT TOP 5
       m.MovieID AS movieId,
       m.Title AS title,
       m.Genre AS genre,
       AVG(CAST(f.Rating AS FLOAT)) AS rating
   FROM MovieFeedback f
   JOIN Movie m ON m.MovieID = f.MovieID
   GROUP BY m.MovieID, m.Title, m.Genre
   ORDER BY AVG(CAST(f.Rating AS FLOAT)) DESC
""", nativeQuery = true)
    List<Object[]> findTopRatedMoviesSQL();

    @Query(value = """
   SELECT TOP 5
       m.MovieID AS movieId,
       m.Title AS title,
       m.Genre AS genre,
       COALESCE(AVG(CAST(f.Rating AS FLOAT)), 0) AS rating
   FROM Movie m
   LEFT JOIN MovieFeedback f ON m.MovieID = f.MovieID
   WHERE LOWER(m.Genre) LIKE LOWER(CONCAT('%', :genre, '%'))
   GROUP BY m.MovieID, m.Title, m.Genre
   ORDER BY rating DESC
""", nativeQuery = true)
    List<Object[]> findTopRatedMoviesByGenreSQL(@Param("genre") String genre);


}
