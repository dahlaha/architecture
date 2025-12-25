package com.example.demo.repositories;

import com.example.demo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Поиск по имени пользователя
    User findByUsername(String username);

    // Поиск по email
    Optional<User> findByEmail(String email);

    // Проверка существования пользователя по имени
    boolean existsByUsername(String username);

    // Проверка существования пользователя по email
    boolean existsByEmail(String email);
}
