package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.models.Book;
import com.example.demo.models.Quote;
import com.example.demo.models.ReadingStatus;
import com.example.demo.models.Review;
import com.example.demo.models.User;
import com.example.demo.models.UserBook;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.QuoteRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.UserBookRepository;
import com.example.demo.repositories.UserRepository;

@Controller
@RequestMapping("/books")
public class BookController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    
    @Value("${upload.path:/uploads}")
    private String uploadPath;
    
    @Autowired
    private BookRepository bookRepo;
    
    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private UserBookRepository userBookRepo;

    @Autowired
    private ReviewRepository reviewRepo;

    @Autowired
    private QuoteRepository quoteRepository;

    @GetMapping
    public String listBooks(@RequestParam(required = false) String genre, Model model, Authentication authentication) {
        List<Book> books;
        if (genre != null && !genre.isEmpty()) {
            books = bookRepo.findByGenre(genre);
        } else {
            books = bookRepo.findAll();
        }
        
        // Получаем список всех уникальных жанров
        List<String> genres = bookRepo.findAll().stream()
            .map(Book::getGenre)
            .distinct()
            .filter(g -> g != null && !g.isEmpty())
            .collect(Collectors.toList());
        
        // Если пользователь авторизован, получаем статусы его книг
        if (authentication != null) {
            User user = userRepo.findByUsername(authentication.getName());
            if (user != null) {
                List<UserBook> userBooks = userBookRepo.findByUser(user);
                Map<Long, UserBook> userBookMap = userBooks.stream()
                    .collect(Collectors.toMap(
                        ub -> ub.getBook().getId(),
                        ub -> ub,
                        (existing, replacement) -> existing // В случае дубликатов оставляем первую запись
                    ));
                model.addAttribute("userBooks", userBookMap);
            }
        }
        
        model.addAttribute("books", books);
        model.addAttribute("genres", genres);
        model.addAttribute("selectedGenre", genre);
        return "books";
    }

    @PostMapping("/add")
    public String addBook(@RequestParam("bookId") Long bookId,
                         @RequestParam("status") String statusStr,
                         @RequestParam(value = "returnTo", required = false) String returnTo,
                         @RequestParam(value = "genre", required = false) String genre,
                         Authentication authentication) {
        logger.info("Attempting to add book with ID: {} and status: {}", bookId, statusStr);
        
        if (authentication == null) {
            logger.warn("Authentication is null");
            return "redirect:/login";
        }
        
        logger.info("User authenticated as: {}", authentication.getName());
        User user = userRepo.findByUsername(authentication.getName());
        if (user == null) {
            logger.warn("User not found in database: {}", authentication.getName());
            return "redirect:/login";
        }

        try {
            // Проверяем, не добавлена ли уже эта книга
            Book book = bookRepo.findById(bookId).orElseThrow();
            if (userBookRepo.existsByUserAndBook(user, book)) {
                logger.warn("Book already exists in user's list");
                return getRedirectUrl(returnTo, bookId, "error=already_exists", genre);
            }

            UserBook userBook = new UserBook();
            userBook.setUser(user);
            userBook.setBook(book);
            
            try {
                ReadingStatus status = ReadingStatus.valueOf(statusStr);
                userBook.setStatus(status);
                
                // Устанавливаем дату завершения, если статус "Прочитано"
                if (status == ReadingStatus.FINISHED) {
                    userBook.setFinishedDate(LocalDateTime.now());
                }
                
                userBookRepo.save(userBook);
                logger.info("Successfully added book to user's list with status: {}", status);
                return getRedirectUrl(returnTo, bookId, "success", genre);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid status value: {}", statusStr);
                return getRedirectUrl(returnTo, bookId, "error=invalid_status", genre);
            }
        } catch (Exception e) {
            logger.error("Error adding book to user's list", e);
            return getRedirectUrl(returnTo, bookId, "error", genre);
        }
    }

    @PostMapping("/update-status")
    public String updateBookStatus(@RequestParam("bookId") Long bookId,
                                 @RequestParam("status") String statusStr,
                                 @RequestParam(value = "returnTo", required = false) String returnTo,
                                 @RequestParam(value = "genre", required = false) String genre,
                                 Authentication authentication) {
        try {
            User user = userRepo.findByUsername(authentication.getName());
            Book book = bookRepo.findById(bookId).orElseThrow();
            UserBook userBook = userBookRepo.findByUserAndBook(user, book);
            
            ReadingStatus newStatus = ReadingStatus.valueOf(statusStr);
            
            if (userBook != null) {
                // Если статус меняется на FINISHED, устанавливаем дату завершения
                if (newStatus == ReadingStatus.FINISHED && userBook.getStatus() != ReadingStatus.FINISHED) {
                    userBook.setFinishedDate(LocalDateTime.now());
                } else if (newStatus != ReadingStatus.FINISHED) {
                    userBook.setFinishedDate(null);
                }
                userBook.setStatus(newStatus);
                userBookRepo.save(userBook);
            }
            
            return getRedirectUrl(returnTo, bookId, "success", genre);
        } catch (Exception e) {
            logger.error("Error updating book status", e);
            return getRedirectUrl(returnTo, bookId, "error", genre);
        }
    }

    @PostMapping("/remove")
    public String removeBook(@RequestParam("bookId") Long bookId,
                           @RequestParam(value = "returnTo", required = false) String returnTo,
                           @RequestParam(value = "genre", required = false) String genre,
                           Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepo.findByUsername(authentication.getName());
            Book book = bookRepo.findById(bookId).orElseThrow();
            UserBook userBook = userBookRepo.findByUserAndBook(user, book);
            
            if (userBook != null) {
                userBookRepo.delete(userBook);
                return getRedirectUrl(returnTo, bookId, "success", genre);
            }
            
            return getRedirectUrl(returnTo, bookId, "error", genre);
        } catch (Exception e) {
            logger.error("Error removing book", e);
            return getRedirectUrl(returnTo, bookId, "error", genre);
        }
    }

    @PostMapping("/rate")
    public String rateBook(@RequestParam("bookId") Long bookId,
                          @RequestParam("rating") Integer rating,
                          Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepo.findByUsername(authentication.getName());
            Book book = bookRepo.findById(bookId).orElseThrow();
            
            // Получаем существующую запись UserBook
            UserBook userBook = userBookRepo.findByUserAndBook(user, book);
            
            // Проверяем валидность оценки (1-10)
            if (rating < 1 || rating > 10) {
                return "redirect:/books/book/" + bookId + "?error=invalid_rating";
            }
            
            // Если запись существует и у неё есть статус, обновляем оценку
            if (userBook != null && userBook.getStatus() != null) {
            userBook.setRating(rating);
            userBookRepo.save(userBook);
            return "redirect:/books/book/" + bookId + "?success=rating_updated";
            } else {
                return "redirect:/books/book/" + bookId + "?error=no_status";
            }
        } catch (Exception e) {
            logger.error("Error rating book", e);
            return "redirect:/books/book/" + bookId + "?error=rating_failed";
        }
    }

    @PostMapping("/review/add")
    public String addReview(@RequestParam("bookId") Long bookId,
                          @RequestParam("review") String reviewText,
                          @RequestParam(value = "parentId", required = false) Long parentId,
                          Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepo.findByUsername(authentication.getName());
            Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));

            // Создаем новый отзыв
            Review review = new Review();
            review.setUser(user);
            review.setBook(book);
            review.setText(reviewText);
            review.setCreatedAt(LocalDateTime.now());
            
            // Если это ответ на другой отзыв
            if (parentId != null) {
                Review parentReview = reviewRepo.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Родительский отзыв не найден"));
                review.setParentReview(parentReview);
            }
            
            reviewRepo.save(review);
            
            return "redirect:/books/book/" + bookId + "?success=review_added";
        } catch (Exception e) {
            logger.error("Error adding review", e);
            return "redirect:/books/book/" + bookId + "?error=add_failed";
        }
    }

    @PostMapping("/review/edit")
    public String editReview(@RequestParam("reviewId") Long reviewId,
                           @RequestParam("bookId") Long bookId,
                           @RequestParam("review") String reviewText,
                           Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepo.findByUsername(authentication.getName());
            Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не найден"));
            
            // Проверяем, принадлежит ли отзыв текущему пользователю
            if (!review.getUser().equals(user)) {
                return "redirect:/books/book/" + bookId + "?error=unauthorized";
            }
            
            // Обновляем текст отзыва
            review.setText(reviewText);
            reviewRepo.save(review);
            
            return "redirect:/books/book/" + bookId + "?success=review_updated";
        } catch (Exception e) {
            logger.error("Error editing review", e);
            return "redirect:/books/book/" + bookId + "?error=edit_failed";
        }
    }

    @PostMapping("/review/delete")
    public String deleteReview(@RequestParam("reviewId") Long reviewId,
                             @RequestParam("bookId") Long bookId,
                             Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepo.findByUsername(authentication.getName());
            Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не найден"));
            
            // Проверяем, принадлежит ли отзыв текущему пользователю
            if (!review.getUser().equals(user)) {
                return "redirect:/books/book/" + bookId + "?error=unauthorized";
            }
            
            // Удаляем отзыв
            reviewRepo.delete(review);
            
            return "redirect:/books/book/" + bookId + "?success=review_deleted";
        } catch (Exception e) {
            logger.error("Error deleting review", e);
            return "redirect:/books/book/" + bookId + "?error=delete_failed";
        }
    }

    @PostMapping("/upload-cover")
    public String uploadCover(@RequestParam("bookId") Long bookId,
                            @RequestParam("cover") MultipartFile file,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // Get the book
            Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Книга не найдена"));

            // Create uploads directory if it doesn't exist
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(filename);

            // Save the file
            Files.copy(file.getInputStream(), filePath);

            // Delete old cover if exists
            if (book.getCoverUrl() != null) {
                try {
                    Files.deleteIfExists(Paths.get(uploadPath, book.getCoverUrl()));
                } catch (IOException e) {
                    logger.warn("Could not delete old cover: " + e.getMessage());
                }
            }

            // Update book's cover URL
            book.setCoverUrl(filename);
            bookRepo.save(book);

            redirectAttributes.addFlashAttribute("message", "Обложка успешно обновлена");
        } catch (IOException e) {
            logger.error("Error uploading cover: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при загрузке обложки");
        }

        return "redirect:/books/book/" + bookId;
    }

    @PostMapping("/favorite/toggle")
    public String toggleFavorite(@RequestParam("bookId") Long bookId,
                               Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepo.findByUsername(authentication.getName());
            Book book = bookRepo.findById(bookId).orElseThrow();
            
            // Получаем или создаем запись UserBook
            UserBook userBook = userBookRepo.findByUserAndBook(user, book);
            if (userBook == null) {
                userBook = new UserBook();
                userBook.setUser(user);
                userBook.setBook(book);
            }
            
            // Переключаем статус избранного
            userBook.setFavorite(!userBook.isFavorite());
            userBookRepo.save(userBook);
            
            return "redirect:/books/book/" + bookId + "?success=favorite_updated";
        } catch (Exception e) {
            logger.error("Error toggling favorite status", e);
            return "redirect:/books/book/" + bookId + "?error=favorite_failed";
        }
    }

    private String getRedirectUrl(String returnTo, Long bookId, String queryParam, String genre) {
        UriComponentsBuilder builder;
        
        if ("book".equals(returnTo)) {
            builder = UriComponentsBuilder.fromPath("/books/book/" + bookId);
        } else if ("recommendations".equals(returnTo)) {
            builder = UriComponentsBuilder.fromPath("/recommendations");
        } else {
            builder = UriComponentsBuilder.fromPath("/books");
        }
        
        // Add query parameters
        if (genre != null && !genre.isEmpty()) {
            builder.queryParam("genre", genre);
        }
        
        if (queryParam != null && !queryParam.isEmpty()) {
            if (queryParam.contains("=")) {
                String[] parts = queryParam.split("=", 2);
                builder.queryParam(parts[0], parts[1]);
            } else {
                builder.queryParam(queryParam);
            }
        }
        
        return "redirect:" + builder.encode().build().toUriString();
    }

    @GetMapping("/book/{id}")
    public String viewBook(@PathVariable Long id, 
                         @RequestParam(required = false) String sort,
                         Model model, 
                         Authentication authentication) {
        Book book = bookRepo.findById(id).orElse(null);
        if (book == null) {
            return "redirect:/books";
        }

        // Получаем цитаты для книги
        List<Quote> quotes = quoteRepository.findByBookOrderByCreatedAtDesc(book);
        model.addAttribute("quotes", quotes);

        // Get current user's book status if authenticated
        UserBook userBook = null;
        boolean isFavorite = false;
        if (authentication != null) {
            User user = userRepo.findByUsername(authentication.getName());
            userBook = userBookRepo.findByUserAndBook(user, book);
            if (userBook != null) {
                isFavorite = userBook.isFavorite();
            }
        }

        // Get all root reviews (without parent)
        List<Review> reviews;
        if ("old".equals(sort)) {
            reviews = reviewRepo.findByBookAndParentReviewIsNullOrderByCreatedAtAsc(book);
        } else {
            reviews = reviewRepo.findByBookAndParentReviewIsNullOrderByCreatedAtDesc(book);
        }

        // Count books by status
        long readingCount = userBookRepo.countByBookAndStatus(book, ReadingStatus.READING);
        long finishedCount = userBookRepo.countByBookAndStatus(book, ReadingStatus.FINISHED);
        long plannedCount = userBookRepo.countByBookAndStatus(book, ReadingStatus.WANT_TO_READ);
        long droppedCount = userBookRepo.countByBookAndStatus(book, ReadingStatus.DROPPED);

        // Calculate average rating
        List<UserBook> ratedBooks = userBookRepo.findByBookAndRatingIsNotNull(book);
        double averageRating = 0.0;
        int totalRatings = 0;
        
        if (!ratedBooks.isEmpty()) {
            int sumRatings = ratedBooks.stream()
                .mapToInt(UserBook::getRating)
                .sum();
            totalRatings = ratedBooks.size();
            averageRating = Math.round((double) sumRatings / totalRatings * 10.0) / 10.0;
        }

        model.addAttribute("book", book);
        model.addAttribute("userBook", userBook);
        model.addAttribute("reviews", reviews);
        model.addAttribute("readingCount", readingCount);
        model.addAttribute("finishedCount", finishedCount);
        model.addAttribute("plannedCount", plannedCount);
        model.addAttribute("droppedCount", droppedCount);
        model.addAttribute("isAuthenticated", authentication != null);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("totalRatings", totalRatings);
        model.addAttribute("currentSort", sort != null ? sort : "new");
        model.addAttribute("isFavorite", isFavorite);

        return "book-details";
    }
}
