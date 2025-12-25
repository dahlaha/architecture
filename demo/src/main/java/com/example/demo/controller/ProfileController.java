package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.models.Friendship;
import com.example.demo.models.FriendshipStatus;
import com.example.demo.models.Quote;
import com.example.demo.models.ReadingStatus;
import com.example.demo.models.User;
import com.example.demo.models.UserActivity;
import com.example.demo.models.UserBook;
import com.example.demo.models.UserStats;
import com.example.demo.repositories.FriendshipRepository;
import com.example.demo.repositories.QuoteRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.UserActivityRepository;
import com.example.demo.repositories.UserBookRepository;
import com.example.demo.repositories.UserRepository;

@Controller
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Value("${upload.path:/uploads}")
    private String uploadPath;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBookRepository userBookRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @PostMapping("/profile/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }

        try {
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

            // Delete old avatar if exists
            if (user.getAvatarPath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(uploadPath, user.getAvatarPath()));
                } catch (IOException e) {
                    logger.warn("Could not delete old avatar: " + e.getMessage());
                }
            }

            // Update user's avatar path
            user.setAvatarPath(filename);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("message", "Аватар успешно обновлен");
        } catch (IOException e) {
            logger.error("Error uploading avatar: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при загрузке аватара");
        }

        return "redirect:/profile";
    }

    @GetMapping("/profile")
    public String showProfile(@RequestParam(required = false) String status,
                            Authentication authentication,
                            Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }

        return showUserProfile(user, status, authentication, model, true);
    }

    @GetMapping("/user/{username}")
    public String viewUserProfile(@PathVariable String username,
                                @RequestParam(required = false) String status,
                                Authentication authentication,
                                Model model) {
        User targetUser = userRepository.findByUsername(username);
        if (targetUser == null) {
            return "redirect:/error/404";
        }

        // Если пользователь пытается посмотреть свой профиль, перенаправляем на /profile
        if (authentication != null && 
            authentication.getName().equals(username)) {
            return "redirect:/profile" + (status != null ? "?status=" + status : "");
        }

        return showUserProfile(targetUser, status, authentication, model, false);
    }

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        return "edit-profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(
            @RequestParam String email,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }

        // Проверка, не занят ли email другим пользователем
        Optional<User> existingUserWithEmail = userRepository.findByEmail(email);
        if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Этот email уже используется другим пользователем");
            return "redirect:/profile/edit";
        }

        // Обновление данных пользователя
        user.setEmail(email);
        userRepository.save(user);

        // Создание записи об активности
        UserActivity activity = new UserActivity();
        activity.setUser(user);
        activity.setType("PROFILE_UPDATE");
        activity.setDescription("Обновил(а) профиль");
        userActivityRepository.save(activity);

        redirectAttributes.addFlashAttribute("message", "Профиль успешно обновлен");
        return "redirect:/profile";
    }

    private String showUserProfile(User profileUser, 
                                 String status,
                                 Authentication authentication,
                                 Model model,
                                 boolean isOwnProfile) {
        // Get user's books
        List<UserBook> userBooks;
        if (status != null) {
            try {
                ReadingStatus readingStatus = ReadingStatus.valueOf(status);
                userBooks = userBookRepository.findByUserAndStatus(profileUser, readingStatus);
                model.addAttribute("status", status);
            } catch (IllegalArgumentException e) {
                userBooks = userBookRepository.findByUser(profileUser);
            }
        } else {
            userBooks = userBookRepository.findByUser(profileUser);
        }

        // Get friend requests only for own profile
        List<Friendship> pendingRequests = isOwnProfile ? 
            friendshipRepository.findByReceiverAndStatus(profileUser, FriendshipStatus.PENDING) :
            List.of();
        
        // Get friends list
        List<User> friends = friendshipRepository.findByRequesterOrReceiverAndStatus(profileUser, profileUser, FriendshipStatus.ACCEPTED)
            .stream()
            .map(friendship -> friendship.getRequester().equals(profileUser) ? 
                 friendship.getReceiver() : friendship.getRequester())
            .collect(Collectors.toList());

        // Check friendship status if viewing other's profile
        boolean canAddFriend = false;
        boolean isFriend = false;
        if (!isOwnProfile && authentication != null) {
            User currentUser = userRepository.findByUsername(authentication.getName());
            Optional<Friendship> friendship = friendshipRepository.findByRequesterAndReceiver(currentUser, profileUser);
            if (friendship.isEmpty()) {
                friendship = friendshipRepository.findByRequesterAndReceiver(profileUser, currentUser);
            }
            
            if (friendship.isPresent()) {
                isFriend = friendship.get().getStatus() == FriendshipStatus.ACCEPTED;
                canAddFriend = friendship.get().getStatus() == FriendshipStatus.REJECTED;
            } else {
                canAddFriend = true;
            }
        }

        // Calculate user stats
        UserStats stats = calculateUserStats(profileUser);
        stats.setFriendsCount(friends.size());

        // Get user activities
        List<UserActivity> activities = userActivityRepository.findByUserOrderByDateDesc(profileUser);

        // Get favorite books
        List<UserBook> favoriteBooks = userBookRepository.findByUserAndIsFavoriteTrue(profileUser);

        // Get user's quotes
        List<Quote> userQuotes = quoteRepository.findByUserOrderByCreatedAtDesc(profileUser);

        model.addAttribute("user", profileUser);
        model.addAttribute("userBooks", userBooks);
        model.addAttribute("stats", stats);
        model.addAttribute("activities", activities);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("friends", friends);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("canAddFriend", canAddFriend);
        model.addAttribute("isFriend", isFriend);
        model.addAttribute("status", status);
        model.addAttribute("favoriteBooks", favoriteBooks);
        model.addAttribute("userQuotes", userQuotes);

        return isOwnProfile ? "profile" : "user-profile";
    }

    private UserStats calculateUserStats(User user) {
        UserStats stats = new UserStats();
        
        // Get all user's books
        List<UserBook> userBooks = userBookRepository.findByUser(user);
        
        // Set total books count
        stats.setTotalBooks(userBooks.size());
        
        // Count books by status
        for (UserBook book : userBooks) {
            if (book.getStatus() == null) continue;
            
            switch (book.getStatus()) {
                case FINISHED:
                    stats.setBooksRead(stats.getBooksRead() + 1);
                    stats.incrementGenreCount(book.getBook().getGenre());
                    break;
                case READING:
                    stats.setReadingBooks(stats.getReadingBooks() + 1);
                    break;
                case WANT_TO_READ:
                    stats.setPlannedBooks(stats.getPlannedBooks() + 1);
                    break;
                case DROPPED:
                    stats.setDroppedBooks(stats.getDroppedBooks() + 1);
                    break;
            }
        }

        // Подсчет количества комментариев пользователя
        int commentsCount = reviewRepository.countByUser(user);
        stats.setCommentsCount(commentsCount);

        return stats;
    }
} 