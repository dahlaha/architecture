package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.models.Book;
import com.example.demo.models.Quote;
import com.example.demo.models.User;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findByBookOrderByCreatedAtDesc(Book book);
    List<Quote> findByUserOrderByCreatedAtDesc(User user);
    int countByUser(User user);
} 