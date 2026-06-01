package com.example.job_search.repository;

import com.example.job_search.entity.Vacancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, Long>{
    @Query("SELECT v FROM Vacancy v WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:city IS NULL OR :city = '' OR LOWER(v.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
            "(:category IS NULL OR :category = '' OR v.category.name = :category)")
    Page<Vacancy> findFilteredVacancies(
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("category") String category,
            Pageable pageable);

    boolean existsByOriginalUrl(String originalUrl);
    Page<Vacancy> findByIsActiveTrue(Pageable pageable);
}
