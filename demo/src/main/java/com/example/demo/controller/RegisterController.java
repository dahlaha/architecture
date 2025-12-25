package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             Model model) {
        
        // Проверка существования пользователя
        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "Пользователь с таким именем уже существует");
            return "register";
        }

        // Проверка существования email
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email уже используется");
            return "register";
        }

        // Проверка совпадения паролей
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Пароли не совпадают");
            return "register";
        }

        // Создание нового пользователя
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true); // Устанавливаем аккаунт как активный
        user.setLocked(false); // Устанавливаем аккаунт как не заблокированный

        try {
            userRepository.save(user);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при регистрации. Попробуйте позже.");
            return "register";
        }
    }
} 