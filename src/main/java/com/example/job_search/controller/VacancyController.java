package com.example.job_search.controller;

import com.example.job_search.parser.WorkUaParserService;
import com.example.job_search.entity.Vacancy;
import com.example.job_search.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class VacancyController {
    private final VacancyRepository vacancyRepository;
    private final WorkUaParserService parserService;

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageSize = 12;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("id").descending());
        Page<Vacancy> vacancyPage = vacancyRepository.findAll(pageable);
        model.addAttribute("vacancies", vacancyPage.getContent());
        model.addAttribute("vacancyPage", vacancyPage);
        return "index";
    }

    @GetMapping("/parse")
    @ResponseBody
    public String runParser() {
        parserService.parseDiverseVacancies();
        return "Parsing is started";
    }
}
