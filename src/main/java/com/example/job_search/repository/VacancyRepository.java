package com.example.job_search.repository;

import com.example.job_search.entity.Vacancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, Long>{
    Page<Vacancy> findByIsActiveTrue(Pageable pageable);

    boolean existsByOriginalUrl(String originalUrl);
}
