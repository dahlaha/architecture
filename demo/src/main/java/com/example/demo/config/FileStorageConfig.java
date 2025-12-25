package com.example.demo.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class FileStorageConfig {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfig.class);

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.avatar-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            // Create main upload directory
            Path uploadDirectory = Paths.get(uploadPath);
            Files.createDirectories(uploadDirectory);
            logger.debug("Created upload directory: {}", uploadDirectory.toAbsolutePath());
            
            // Create avatar directory
            Path avatarDirectory = Paths.get(uploadDir);
            Files.createDirectories(avatarDirectory);
            logger.debug("Created avatar directory: {}", avatarDirectory.toAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Failed to create upload directories", e);
            throw new RuntimeException("Could not create upload directories!", e);
        }
    }
} 