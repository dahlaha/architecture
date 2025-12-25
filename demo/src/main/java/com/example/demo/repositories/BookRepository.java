package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.models.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
    // Поиск по названию (регистронезависимый)
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Поиск по автору
    List<Book> findByAuthorContainingIgnoreCase(String author);

    // Поиск по жанру
    List<Book> findByGenre(String genre);

    // Кастомный запрос для поиска книг не в списке пользователя
    @Query("SELECT b FROM Book b WHERE b.id NOT IN " +
            "(SELECT ub.book.id FROM UserBook ub WHERE ub.user.id = :userId)")
    List<Book> findBooksNotInUserList(@Param("userId") Long userId);

    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Book> searchBooks(@Param("query") String query);
    
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);
}
