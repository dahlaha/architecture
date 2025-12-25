package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.models.BookRecommendation;
import com.example.demo.models.User;

@Repository
public interface BookRecommendationRepository extends JpaRepository<BookRecommendation, Long> {
    List<BookRecommendation> findByUserOrderByScoreDesc(User user);
    
    void deleteByUser(User user);
} 