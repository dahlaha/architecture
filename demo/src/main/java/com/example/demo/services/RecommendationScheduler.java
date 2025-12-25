package com.example.demo.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;

@Service
public class RecommendationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationScheduler.class);
    
    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    public RecommendationScheduler(RecommendationService recommendationService,
                                 UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    // Обновление рекомендаций каждый день в 3 часа ночи
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void updateRecommendations() {
        logger.info("Starting scheduled recommendation update");
        try {
            List<User> users = userRepository.findAll();
            for (User user : users) {
                try {
                    recommendationService.generateRecommendations(user);
                    logger.info("Generated recommendations for user: {}", user.getUsername());
                } catch (Exception e) {
                    logger.error("Error generating recommendations for user {}: {}", 
                               user.getUsername(), e.getMessage());
                }
            }
            logger.info("Completed scheduled recommendation update");
        } catch (Exception e) {
            logger.error("Error in recommendation update scheduler: {}", e.getMessage());
        }
    }
} 