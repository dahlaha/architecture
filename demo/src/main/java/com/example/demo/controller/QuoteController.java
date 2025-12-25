package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.models.Book;
import com.example.demo.models.Quote;
import com.example.demo.models.User;
import com.example.demo.models.UserActivity;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.QuoteRepository;
import com.example.demo.repositories.UserActivityRepository;
import com.example.demo.repositories.UserRepository;

@Controller
public class QuoteController {

    private static final Logger logger = LoggerFactory.getLogger(QuoteController.class);

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @PostMapping("/books/{bookId}/quotes/add")
    public String addQuote(@PathVariable Long bookId,
                          @RequestParam String text,
                          @RequestParam(required = false) String page,
                          @RequestParam(required = false) String chapter,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("error", "Для добавления цитат необходимо войти в систему");
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/login";
        }

        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            redirectAttributes.addFlashAttribute("error", "Книга не найдена");
            return "redirect:/books";
        }

        Quote quote = new Quote();
        quote.setUser(user);
        quote.setBook(book);
        quote.setText(text.trim());
        quote.setPage(page != null ? page.trim() : null);
        quote.setChapter(chapter != null ? chapter.trim() : null);
        quoteRepository.save(quote);

        // Создаем запись об активности
        UserActivity activity = new UserActivity();
        activity.setUser(user);
        activity.setType("QUOTE_ADDED");
        activity.setDescription("Добавил(а) цитату из книги \"" + book.getTitle() + "\"");
        userActivityRepository.save(activity);

        redirectAttributes.addFlashAttribute("message", "Цитата успешно добавлена");
        return "redirect:/books/book/" + bookId;
    }

    @PostMapping("/quotes/{quoteId}/delete")
    public String deleteQuote(@PathVariable Long quoteId,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }

        Quote quote = quoteRepository.findById(quoteId).orElse(null);
        if (quote == null) {
            redirectAttributes.addFlashAttribute("error", "Цитата не найдена");
            return "redirect:/books";
        }

        // Проверяем, является ли пользователь автором цитаты
        if (!quote.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "У вас нет прав для удаления этой цитаты");
            return "redirect:/books/book/" + quote.getBook().getId();
        }

        Long bookId = quote.getBook().getId();
        quoteRepository.delete(quote);

        // Создаем запись об активности
        UserActivity activity = new UserActivity();
        activity.setUser(user);
        activity.setType("QUOTE_DELETED");
        activity.setDescription("Удалил(а) цитату из книги \"" + quote.getBook().getTitle() + "\"");
        userActivityRepository.save(activity);

        redirectAttributes.addFlashAttribute("message", "Цитата удалена");
        return "redirect:/books/book/" + bookId;
    }

    @PostMapping("/quotes/{quoteId}/edit")
    public String editQuote(@PathVariable Long quoteId,
                          @RequestParam String text,
                          @RequestParam(required = false) String page,
                          @RequestParam(required = false) String chapter,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("error", "Для редактирования цитат необходимо войти в систему");
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/login";
        }

        try {
            Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Цитата не найдена"));

            // Проверяем, является ли пользователь автором цитаты
            if (!quote.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "У вас нет прав для редактирования этой цитаты");
                return "redirect:/books/book/" + quote.getBook().getId();
            }

            // Обновляем данные цитаты
            quote.setText(text.trim());
            quote.setPage(page != null ? page.trim() : null);
            quote.setChapter(chapter != null ? chapter.trim() : null);
            quoteRepository.save(quote);

            redirectAttributes.addFlashAttribute("message", "Цитата успешно обновлена");
            return "redirect:/books/book/" + quote.getBook().getId();
        } catch (Exception e) {
            logger.error("Error editing quote", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при редактировании цитаты");
            return "redirect:/books";
        }
    }
} 