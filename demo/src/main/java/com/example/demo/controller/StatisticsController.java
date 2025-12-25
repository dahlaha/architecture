package com.example.demo.controller;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.models.ReadingStatus;
import com.example.demo.models.User;
import com.example.demo.models.UserBook;
import com.example.demo.repositories.UserBookRepository;
import com.example.demo.repositories.UserRepository;

@RestController
@RequestMapping("/api/user")
public class StatisticsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBookRepository userBookRepository;

    @GetMapping("/reading-statistics")
    public Map<String, Object> getReadingStatistics(Authentication authentication,
            @RequestParam(defaultValue = "all") String period,
            @RequestParam(defaultValue = "all") String type) {
        User user = userRepository.findByUsername(authentication.getName());
        return getUserStatistics(user, period, type);
    }

    @GetMapping("/{username}/reading-statistics")
    public ResponseEntity<Map<String, Object>> getUserReadingStatistics(@PathVariable String username,
            @RequestParam(defaultValue = "all") String period,
            @RequestParam(defaultValue = "all") String type) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(getUserStatistics(user, period, type));
    }

    private Map<String, Object> getUserStatistics(User user, String period, String type) {
        List<UserBook> finishedBooks = userBookRepository.findByUserAndStatus(user, ReadingStatus.FINISHED);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("readStats", getReadStats(finishedBooks, period, type));
        statistics.put("genreStats", getGenreStats(finishedBooks, period, type));

        return statistics;
    }

    private List<Map<String, Object>> getReadStats(List<UserBook> finishedBooks, String period, String type) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime startDate;
        final LocalDateTime endDate;
        final String dateFormat;
        final String labelFormat;

        switch (type) {
            case "month":
                // Парсим месяц в формате "YYYY-MM"
                final YearMonth yearMonth = YearMonth.parse(period);
                startDate = yearMonth.atDay(1).atStartOfDay();
                endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
                dateFormat = "yyyy-MM-dd";
                labelFormat = "d MMMM";
                break;
            case "year":
                // Парсим год
                final int year = Integer.parseInt(period);
                startDate = LocalDateTime.of(year, 1, 1, 0, 0);
                endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59);
                dateFormat = "yyyy-MM";
                labelFormat = "MMMM";
                break;
            default: // "all"
                startDate = finishedBooks.stream()
                    .map(UserBook::getFinishedDate)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(now.minusYears(1));
                endDate = now;
                dateFormat = "yyyy";
                labelFormat = "yyyy";
                break;
        }

        // Создаем Map для всех периодов
        Map<String, Long> periodCount = new LinkedHashMap<>();
        
        // Инициализируем все периоды нулевыми значениями
        LocalDateTime currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String periodKey = currentDate.format(DateTimeFormatter.ofPattern(dateFormat));
            periodCount.put(periodKey, 0L);
            
            // Увеличиваем дату в зависимости от типа периода
            if (type.equals("month")) {
                currentDate = currentDate.plusDays(1);
            } else if (type.equals("year")) {
                currentDate = currentDate.plusMonths(1);
            } else {
                currentDate = currentDate.plusYears(1);
            }
        }

        // Добавляем данные о прочитанных книгах
        finishedBooks.stream()
            .filter(book -> book.getFinishedDate() != null && 
                          !book.getFinishedDate().isBefore(startDate) &&
                          !book.getFinishedDate().isAfter(endDate))
            .forEach(book -> {
                String periodKey = book.getFinishedDate()
                    .format(DateTimeFormatter.ofPattern(dateFormat));
                periodCount.merge(periodKey, 1L, Long::sum);
            });

        // Форматируем данные для вывода
        return periodCount.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Map<String, Object> periodData = new HashMap<>();
                LocalDateTime date = LocalDateTime.parse(entry.getKey() + 
                    (type.equals("month") ? "T00:00:00" : 
                     type.equals("year") ? "-01T00:00:00" : "-01-01T00:00:00"));
                periodData.put("label", date.format(DateTimeFormatter.ofPattern(labelFormat)));
                periodData.put("count", entry.getValue());
                return periodData;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getGenreStats(List<UserBook> finishedBooks, String period, String type) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime startDate;
        final LocalDateTime endDate;

        switch (type) {
            case "month":
                final YearMonth yearMonth = YearMonth.parse(period);
                startDate = yearMonth.atDay(1).atStartOfDay();
                endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
                break;
            case "year":
                final int year = Integer.parseInt(period);
                startDate = LocalDateTime.of(year, 1, 1, 0, 0);
                endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59);
                break;
            default: // "all"
                startDate = finishedBooks.stream()
                    .map(UserBook::getFinishedDate)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(now.minusYears(1));
                endDate = now;
                break;
        }

        // Группируем книги по жанрам за выбранный период
        Map<String, Long> genreCount = finishedBooks.stream()
            .filter(book -> book.getFinishedDate() != null && 
                          !book.getFinishedDate().isBefore(startDate) &&
                          !book.getFinishedDate().isAfter(endDate))
            .map(book -> book.getBook().getGenre())
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                genre -> genre,
                Collectors.counting()
            ));

        // Форматируем для вывода
        return genreCount.entrySet().stream()
            .map(entry -> {
                Map<String, Object> genreData = new HashMap<>();
                genreData.put("genre", entry.getKey());
                genreData.put("count", entry.getValue());
                return genreData;
            })
            .collect(Collectors.toList());
    }
} 