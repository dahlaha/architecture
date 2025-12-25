package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.models.Book;
import com.example.demo.repositories.BookRepository;

@Controller
public class SearchController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/search")
    public String searchBooks(@RequestParam(required = false) String query, Model model) {
        if (query != null && !query.trim().isEmpty()) {
            List<Book> searchResults = bookRepository.searchBooks(query.trim());
            model.addAttribute("books", searchResults);
            model.addAttribute("query", query);
        }
        return "search";
    }
} 