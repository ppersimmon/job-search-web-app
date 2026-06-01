package com.example.job_search.controller;

import com.example.job_search.parser.WorkUaParserService;
import com.example.job_search.service.VacancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class VacancyController {
    private final VacancyService vacancyService;
    private final WorkUaParserService parserService;

    @GetMapping("/")
    public String showHomePage(Model model) {
        model.addAttribute("vacancies", vacancyService.getAllVacancies());
        return "index";
    }

    @GetMapping("/parse")
    @ResponseBody
    public String runParser() {
        parserService.parseDiverseVacancies();
        return "Parsing is started";
    }
}
