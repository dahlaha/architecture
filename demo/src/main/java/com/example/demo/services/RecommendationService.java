package com.example.demo.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.Book;
import com.example.demo.models.BookRecommendation;
import com.example.demo.models.ReadingStatus;
import com.example.demo.models.User;
import com.example.demo.repositories.BookRecommendationRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.UserBookRepository;

@Service
public class RecommendationService {
    private final BookRecommendationRepository recommendationRepository;
    private final UserBookRepository userBookRepository;
    private final BookRepository bookRepository;

    public RecommendationService(
            BookRecommendationRepository recommendationRepository,
            UserBookRepository userBookRepository,
            BookRepository bookRepository) {
        this.recommendationRepository = recommendationRepository;
        this.userBookRepository = userBookRepository;
        this.bookRepository = bookRepository;
    }

    public List<BookRecommendation> getRecommendationsForUser(User user) {
        // Получаем все книги пользователя
        List<Book> userBooks = userBookRepository.findByUser(user)
                .stream()
                .map(ub -> ub.getBook())
                .collect(Collectors.toList());

        // Получаем все рекомендации
        List<BookRecommendation> allRecommendations = recommendationRepository.findByUserOrderByScoreDesc(user);

        // Фильтруем рекомендации, оставляя только те книги, которых нет в списках пользователя
        return allRecommendations.stream()
                .filter(recommendation -> !userBooks.contains(recommendation.getBook()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void generateRecommendations(User user) {
        // Получаем все прочитанные книги пользователя
        List<Book> readBooks = userBookRepository.findByUserAndStatus(user, ReadingStatus.FINISHED)
                .stream()
                .map(ub -> ub.getBook())
                .collect(Collectors.toList());

        // Получаем все книги пользователя
        List<Book> userBooks = userBookRepository.findByUser(user)
                .stream()
                .map(ub -> ub.getBook())
                .collect(Collectors.toList());

        // Получаем жанры прочитанных книг
        Map<String, Integer> genreCounts = new HashMap<>();
        for (Book book : readBooks) {
            if (book.getGenre() != null && !book.getGenre().isEmpty()) {
                genreCounts.merge(book.getGenre(), 1, Integer::sum);
            }
        }

        // Удаляем старые рекомендации
        recommendationRepository.deleteByUser(user);

        // Генерируем новые рекомендации
        List<BookRecommendation> recommendations = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : genreCounts.entrySet()) {
            String genre = entry.getKey();
            int count = entry.getValue();
            
            // Находим книги этого жанра, которых нет в списках пользователя
            List<Book> genreBooks = bookRepository.findByGenre(genre);
            for (Book book : genreBooks) {
                if (!userBooks.contains(book)) {
                    // Рассчитываем среднюю оценку книги
                    double averageRating = userBookRepository.findByBookAndRatingIsNotNull(book)
                            .stream()
                            .mapToInt(ub -> ub.getRating())
                            .average()
                            .orElse(5.0); // Если оценок нет, используем среднее значение 5

                    // Нормализуем оценку от 0 до 1 (предполагая, что максимальная оценка 10)
                    double normalizedRating = averageRating / 10.0;

                    // Рассчитываем итоговый score, учитывая и жанр, и оценку
                    // 60% веса отдается жанру, 40% - оценке
                    double genreScore = count * 0.8;
                    double ratingScore = normalizedRating * 4.0; // Максимальный вклад оценки - 4.0
                    double finalScore = genreScore + ratingScore;

                    BookRecommendation recommendation = new BookRecommendation();
                    recommendation.setUser(user);
                    recommendation.setBook(book);
                    recommendation.setScore(finalScore);
                    recommendation.setCreatedAt(LocalDateTime.now());
                    recommendation.setRead(false);
                    recommendations.add(recommendation);
                }
            }
        }

        // Сортируем рекомендации по score и сохраняем
        recommendations.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));
        recommendationRepository.saveAll(recommendations);
    }

    @Transactional
    public void markAsRead(Long recommendationId, User user) {
        BookRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found"));
        
        if (!recommendation.getUser().equals(user)) {
            throw new RuntimeException("Access denied");
        }
        
        recommendation.setRead(true);
        recommendationRepository.save(recommendation);
    }
} 