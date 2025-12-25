package com.example.demo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.models.Friendship;
import com.example.demo.models.FriendshipStatus;
import com.example.demo.models.User;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // Найти все запросы дружбы, где пользователь является отправителем или получателем
    List<Friendship> findByRequesterOrReceiver(User requester, User receiver);

    // Найти запросы дружбы по статусу
    List<Friendship> findByStatus(FriendshipStatus status);

    // Найти конкретный запрос дружбы между двумя пользователями
    Optional<Friendship> findByRequesterAndReceiver(User requester, User receiver);

    // Проверить существование дружбы между пользователями
    boolean existsByRequesterAndReceiver(User requester, User receiver);

    // Найти входящие запросы на дружбу
    List<Friendship> findByReceiverAndStatus(User receiver, FriendshipStatus status);

    // Найти друзей пользователя
    List<Friendship> findByRequesterOrReceiverAndStatus(User requester, User receiver, FriendshipStatus status);
}