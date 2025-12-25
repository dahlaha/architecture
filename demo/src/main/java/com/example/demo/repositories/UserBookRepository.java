package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.models.Book;
import com.example.demo.models.ReadingStatus;
import com.example.demo.models.User;
import com.example.demo.models.UserBook;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    // Найти все книги пользователя
    List<UserBook> findByUser(User user);

    // Найти книги пользователя по статусу
    List<UserBook> findByUserAndStatus(User user, ReadingStatus status);

    // Найти конкретную связь пользователь-книга (возвращает первую найденную запись)
    @Query("SELECT ub FROM UserBook ub WHERE ub.user = :user AND ub.book = :book ORDER BY ub.id ASC")
    List<UserBook> findAllByUserAndBook(@Param("user") User user, @Param("book") Book book);

    default UserBook findByUserAndBook(User user, Book book) {
        List<UserBook> results = findAllByUserAndBook(user, book);
        return results.isEmpty() ? null : results.get(0);
    }

    // Проверить существование связи
    boolean existsByUserAndBook(User user, Book book);

    // Удалить все книги пользователя по статусу
    void deleteByUserAndStatus(User user, ReadingStatus status);

    // Find reviews for a book
    List<UserBook> findByBookAndReviewIsNotNull(Book book);

    // Count books by status
    long countByBookAndStatus(Book book, ReadingStatus status);

    // Find books by rating
    List<UserBook> findByBookAndRatingIsNotNull(Book book);

    // Find favorite books for a user
    List<UserBook> findByUserAndIsFavoriteTrue(User user);

    // Find books by genre
    @Query("SELECT DISTINCT b FROM Book b WHERE b.genre = :genre")
    List<Book> findBooksByGenre(@Param("genre") String genre);
}
