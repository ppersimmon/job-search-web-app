package com.example.job_search.controller;
import com.example.job_search.entity.Vacancy;
import com.example.job_search.dto.VacancyDto;
import com.example.job_search.entity.User;
import com.example.job_search.repository.VacancyRepository;
import com.example.job_search.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class VacancyController {
    private final VacancyRepository vacancyRepository;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model) {

        int pageSize = 12;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("id").descending());
        Page<Vacancy> vacancyPage = vacancyRepository.findFilteredVacancies(keyword, city, category, pageable);
        Page<VacancyDto> vacancyDtoPage = vacancyPage.map(VacancyDto::new);

        List<Long> savedVacancyIds = new ArrayList<>();
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                savedVacancyIds = user.getFavoriteVacancies().stream()
                        .map(Vacancy::getId)
                        .toList();
            }
        }

        model.addAttribute("vacancies", vacancyDtoPage.getContent());
        model.addAttribute("vacancyPage", vacancyDtoPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("city", city);
        model.addAttribute("category", category);
        model.addAttribute("savedVacancyIds", savedVacancyIds);

        return "index";
    }
}
