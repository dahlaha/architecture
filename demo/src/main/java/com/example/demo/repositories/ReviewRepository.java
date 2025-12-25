package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.models.Book;
import com.example.demo.models.Review;
import com.example.demo.models.User;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Находим все корневые отзывы (без родителя) для книги, сортированные по дате (новые)
    List<Review> findByBookAndParentReviewIsNullOrderByCreatedAtDesc(Book book);
    
    // Находим все корневые отзывы (без родителя) для книги, сортированные по дате (старые)
    List<Review> findByBookAndParentReviewIsNullOrderByCreatedAtAsc(Book book);
    
    // Находим все ответы на конкретный отзыв, сортированные по дате
    List<Review> findByParentReviewOrderByCreatedAtAsc(Review parentReview);

    int countByUser(User user);
} 