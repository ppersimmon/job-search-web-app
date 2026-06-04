package com.example.job_search.dto;

import com.example.job_search.entity.Vacancy;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VacancyDto {
    private Long id;
    private String title;
    private String previewText;
    private String originalUrl;
    private String salary;
    private String city;
    private String companyName;
    private String categoryName;

    public VacancyDto(Vacancy vacancy) {
        this.id = vacancy.getId();
        this.title = vacancy.getTitle();
        this.previewText = vacancy.getPreviewText();
        this.originalUrl = vacancy.getOriginalUrl();
        this.salary = vacancy.getSalary();
        this.city = vacancy.getCity();

        this.companyName = (vacancy.getCompany() != null && vacancy.getCompany().getName() != null)
                ? vacancy.getCompany().getName()
                : "Анонімна компанія";

        this.categoryName = (vacancy.getCategory() != null)
                ? vacancy.getCategory().getName()
                : null;
    }
}