package com.example.job_search.controller;

import com.example.job_search.dto.UserRegisterDto;
import com.example.job_search.dto.VacancyDto;
import com.example.job_search.entity.User;
import com.example.job_search.entity.Vacancy;
import com.example.job_search.repository.UserRepository;
import com.example.job_search.repository.VacancyRepository;
import com.example.job_search.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final VacancyRepository vacancyRepository;

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("userDto", new UserRegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userDto") UserRegisterDto userDto,
                               BindingResult bindingResult,
                               Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        if (!userDto.getPassword().equals(userDto.getPasswordConfirm())) {
            model.addAttribute("error", "Паролі не збігаються");
            return "register";
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            model.addAttribute("error", "Користувач з такою поштою вже існує");
            return "register";
        }

        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(userDto.getPassword());

        userService.registerUser(user);

        return "redirect:/login";
    }

    @GetMapping("/account")
    public String showAccountPage(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        List<VacancyDto> savedVacanciesDto = user.getFavoriteVacancies().stream()
                .map(VacancyDto::new)
                .toList();

        model.addAttribute("savedVacancies", savedVacanciesDto);
        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());

        return "account";
    }

    @PostMapping("/account/favorites/toggle")
    @ResponseBody
    public String toggleFavorite(@RequestParam Long vacancyId, Principal principal) {
        if (principal == null) {
            return "unauthorized";
        }

        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Vacancy vacancy = vacancyRepository.findById(vacancyId).orElseThrow();

        boolean isFavorite = user.getFavoriteVacancies().stream()
                .anyMatch(v -> v.getId().equals(vacancy.getId()));

        if (isFavorite) {
            user.getFavoriteVacancies().removeIf(v -> v.getId().equals(vacancy.getId()));
        } else {
            user.getFavoriteVacancies().add(vacancy);
        }

        userRepository.save(user);

        return isFavorite ? "removed" : "added";
    }
}
