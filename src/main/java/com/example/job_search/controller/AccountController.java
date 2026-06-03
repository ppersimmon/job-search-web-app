package com.example.job_search.controller;
import com.example.job_search.entity.User;
import com.example.job_search.repository.UserRepository;
import com.example.job_search.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import com.example.job_search.dto.UserRegisterDto;
import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final UserRepository userRepository;

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
    public String showAccountPage(Model model) {
        model.addAttribute("savedVacancies", new ArrayList<>());
        return "account";
    }
}
