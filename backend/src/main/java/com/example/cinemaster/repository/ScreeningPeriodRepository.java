package com.example.cinemaster.repository;


import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.entity.ScreeningPeriod;
import com.example.cinemaster.entity.Showtime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;


@Repository
public interface ScreeningPeriodRepository extends JpaRepository<ScreeningPeriod, Integer> {


    @EntityGraph(attributePaths = {"movie", "branch"})
    @Override
    List<ScreeningPeriod> findAll();


    @EntityGraph(attributePaths = {"movie", "branch"})
    List<ScreeningPeriod> findByBranch_Id(Integer branchId);


    @EntityGraph(attributePaths = {"movie", "branch"})
    @Query("""
       SELECT p FROM ScreeningPeriod p
       WHERE (:branchId IS NULL OR p.branch.id = :branchId)
         AND (
               :onDate IS NULL
               OR (p.startDate <= :onDate AND p.endDate >= :onDate)
             )
         AND (p.isActive IS NULL OR p.isActive = TRUE)
       ORDER BY p.startDate
   """)
    List<ScreeningPeriod> findActive(
            @Param("branchId") Integer branchId,
            @Param("onDate") LocalDate onDate
    );


    @Query("""
   SELECT DISTINCT sp.movie
   FROM ScreeningPeriod sp
   WHERE sp.branch.id = :branchId
     AND sp.branch.isActive = true
     AND sp.startDate <= CURRENT_DATE
     AND sp.endDate >= CURRENT_DATE
     AND sp.isActive = true
""")
    List<Movie> findNowShowingMoviesByBranchId(@Param("branchId") Integer branchId);


    @Query("""
          SELECT sp FROM ScreeningPeriod sp
          WHERE sp.startDate > CURRENT_DATE
          AND sp.isActive = true
          """)
    List<ScreeningPeriod> findComingSoon();


    @Query("""
   SELECT DISTINCT sp.movie
   FROM ScreeningPeriod sp
   WHERE sp.branch.isActive = true
     AND sp.startDate <= CURRENT_DATE
     AND sp.endDate >= CURRENT_DATE
     AND sp.isActive = true
""")
    List<Movie> findMoviesNowShowingAllBranches();


    List<ScreeningPeriod> findByMovie_MovieID(Integer movieId);


    @Query("""
SELECT DISTINCT sp.branch
FROM ScreeningPeriod sp
WHERE sp.movie.movieID = :movieId
 AND sp.startDate <= CURRENT_DATE
 AND sp.endDate >= CURRENT_DATE
 AND sp.isActive = true
""")
    List<Branch> findBranchesShowingMovie(@Param("movieId") Integer movieId);
    @Query("""
    SELECT sp FROM ScreeningPeriod sp
    JOIN sp.movie m
    WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
      AND (sp.isActive IS NULL OR sp.isActive = TRUE)
    ORDER BY sp.startDate ASC
""")
    List<ScreeningPeriod> searchByMovieTitle(@Param("keyword") String keyword);
}

