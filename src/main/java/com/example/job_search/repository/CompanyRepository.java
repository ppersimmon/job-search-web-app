package com.example.job_search.repository;

import com.example.job_search.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>{
    Optional<Company> findByName(String name);
}
