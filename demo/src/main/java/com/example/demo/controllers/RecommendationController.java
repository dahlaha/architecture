package com.example.demo.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.models.BookRecommendation;
import com.example.demo.models.User;
import com.example.demo.services.RecommendationService;
import com.example.demo.services.UserService;

@Controller
@RequestMapping("/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final UserService userService;

    public RecommendationController(RecommendationService recommendationService, UserService userService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    @GetMapping
    public String getRecommendations(Model model) {
        User currentUser = userService.getCurrentUser();
        List<BookRecommendation> recommendations = recommendationService.getRecommendationsForUser(currentUser);
        model.addAttribute("recommendations", recommendations);
        return "recommendations/list";
    }

    @PostMapping("/generate")
    @ResponseBody
    public String generateRecommendations() {
        User currentUser = userService.getCurrentUser();
        recommendationService.generateRecommendations(currentUser);
        return "Рекомендации успешно обновлены";
    }

    @PostMapping("/{id}/mark-read")
    @ResponseBody
    public String markAsRead(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        recommendationService.markAsRead(id, currentUser);
        return "Книга отмечена как прочитанная";
    }
} 