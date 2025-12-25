package com.example.demo.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.models.Friendship;
import com.example.demo.models.FriendshipStatus;
import com.example.demo.models.User;
import com.example.demo.models.UserActivity;
import com.example.demo.repositories.FriendshipRepository;
import com.example.demo.repositories.UserActivityRepository;
import com.example.demo.repositories.UserRepository;

@Controller
public class FriendshipController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @PostMapping("/friends/add")
    public String sendFriendRequest(@RequestParam("username") String username,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User requester = userRepository.findByUsername(authentication.getName());
        User receiver = userRepository.findByUsername(username);

        if (receiver == null) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/profile";
        }

        if (requester.getId().equals(receiver.getId())) {
            redirectAttributes.addFlashAttribute("error", "Вы не можете добавить себя в друзья");
            return "redirect:/profile";
        }

        // Проверяем, существует ли уже запрос на дружбу
        Optional<Friendship> existingFriendship = friendshipRepository
            .findByRequesterAndReceiver(requester, receiver);

        if (existingFriendship.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Запрос уже отправлен");
            return "redirect:/profile";
        }

        // Создаем новый запрос на дружбу
        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setReceiver(receiver);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);

        // Создаем запись об активности
        UserActivity activity = new UserActivity();
        activity.setUser(requester);
        activity.setType("FRIEND_REQUEST_SENT");
        activity.setDescription("Отправлен запрос на дружбу пользователю " + receiver.getUsername());
        userActivityRepository.save(activity);

        redirectAttributes.addFlashAttribute("message", "Запрос на дружбу отправлен");
        return "redirect:/profile";
    }

    @PostMapping("/friends/accept")
    public String acceptFriendRequest(@RequestParam("friendshipId") Long friendshipId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        Optional<Friendship> optionalFriendship = friendshipRepository.findById(friendshipId);

        if (optionalFriendship.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Запрос не найден");
            return "redirect:/profile";
        }

        Friendship friendship = optionalFriendship.get();
        if (!friendship.getReceiver().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "У вас нет прав для этого действия");
            return "redirect:/profile";
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        // Создаем записи об активности для обоих пользователей
        UserActivity activity1 = new UserActivity();
        activity1.setUser(user);
        activity1.setType("FRIEND_REQUEST_ACCEPTED");
        activity1.setDescription("Принят запрос на дружбу от " + friendship.getRequester().getUsername());
        userActivityRepository.save(activity1);

        UserActivity activity2 = new UserActivity();
        activity2.setUser(friendship.getRequester());
        activity2.setType("FRIEND_REQUEST_ACCEPTED");
        activity2.setDescription(user.getUsername() + " принял(а) ваш запрос на дружбу");
        userActivityRepository.save(activity2);

        redirectAttributes.addFlashAttribute("message", "Запрос на дружбу принят");
        return "redirect:/profile";
    }

    @PostMapping("/friends/reject")
    public String rejectFriendRequest(@RequestParam("friendshipId") Long friendshipId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName());
        Optional<Friendship> optionalFriendship = friendshipRepository.findById(friendshipId);

        if (optionalFriendship.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Запрос не найден");
            return "redirect:/profile";
        }

        Friendship friendship = optionalFriendship.get();
        if (!friendship.getReceiver().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "У вас нет прав для этого действия");
            return "redirect:/profile";
        }

        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);

        redirectAttributes.addFlashAttribute("message", "Запрос на дружбу отклонен");
        return "redirect:/profile";
    }

    @PostMapping("/friends/remove")
    public String removeFriend(@RequestParam(value = "friendshipId", required = false) Long friendshipId,
                             @RequestParam(value = "username", required = false) String username,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findByUsername(authentication.getName());
        
        // Handle removal by username
        if (username != null) {
            User targetUser = userRepository.findByUsername(username);
            if (targetUser == null) {
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/profile";
            }

            // Find friendship in either direction
            Optional<Friendship> friendship = friendshipRepository.findByRequesterAndReceiver(currentUser, targetUser);
            if (friendship.isEmpty()) {
                friendship = friendshipRepository.findByRequesterAndReceiver(targetUser, currentUser);
            }

            if (friendship.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Дружба не найдена");
                return "redirect:/profile";
            }

            friendshipId = friendship.get().getId();
        }

        // Handle removal by friendshipId
        if (friendshipId == null) {
            redirectAttributes.addFlashAttribute("error", "Некорректные параметры запроса");
            return "redirect:/profile";
        }

        Optional<Friendship> optionalFriendship = friendshipRepository.findById(friendshipId);

        if (optionalFriendship.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Дружба не найдена");
            return "redirect:/profile";
        }

        Friendship friendship = optionalFriendship.get();
        if (!friendship.getRequester().getId().equals(currentUser.getId()) && 
            !friendship.getReceiver().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "У вас нет прав для этого действия");
            return "redirect:/profile";
        }

        // Create activity records for both users
        UserActivity activity1 = new UserActivity();
        activity1.setUser(currentUser);
        activity1.setType("FRIEND_REMOVED");
        activity1.setDescription("Удален из друзей: " + 
            (friendship.getRequester().getId().equals(currentUser.getId()) ? 
             friendship.getReceiver().getUsername() : friendship.getRequester().getUsername()));
        userActivityRepository.save(activity1);

        UserActivity activity2 = new UserActivity();
        activity2.setUser(friendship.getRequester().getId().equals(currentUser.getId()) ? 
                         friendship.getReceiver() : friendship.getRequester());
        activity2.setType("FRIEND_REMOVED");
        activity2.setDescription(currentUser.getUsername() + " удалил вас из друзей");
        userActivityRepository.save(activity2);

        friendshipRepository.delete(friendship);

        redirectAttributes.addFlashAttribute("message", "Пользователь удален из друзей");
        return "redirect:/profile";
    }
} 