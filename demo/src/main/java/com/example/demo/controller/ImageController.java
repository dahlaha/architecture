package com.example.demo.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/images")
public class ImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Value("${upload.path:/uploads}")
    private String uploadPath;

    @Value("${upload.avatar-dir}")
    private String uploadDir;

    @GetMapping("/default-book-cover.jpg")
    public void getDefaultBookCover(HttpServletResponse response) throws IOException {
        response.setContentType("image/svg+xml");
        InputStream is = new ClassPathResource("static/images/default-book-cover.svg").getInputStream();
        IOUtils.copy(is, response.getOutputStream());
    }

    @GetMapping("/covers/{filename}")
    public void getBookCover(@PathVariable String filename, HttpServletResponse response) throws IOException {
        Path file = Paths.get(uploadPath, filename);
        logger.debug("Looking for book cover at: {}", file.toString());
        
        if (Files.exists(file)) {
            String contentType = Files.probeContentType(file);
            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            logger.debug("Serving book cover: {} with content type: {}", filename, contentType);
            Files.copy(file, response.getOutputStream());
        } else {
            logger.debug("Book cover not found: {}, serving default cover", filename);
            getDefaultBookCover(response);
        }
    }

    @GetMapping("/default-avatar.png")
    public void getDefaultAvatar(HttpServletResponse response) throws IOException {
        InputStream is = new ClassPathResource("static/images/default-avatar.png").getInputStream();
        IOUtils.copy(is, response.getOutputStream());
    }

    @GetMapping("/avatars/{filename}")
    public void getAvatar(@PathVariable String filename, HttpServletResponse response) throws IOException {
        Path file = Paths.get(uploadDir, filename);
        if (Files.exists(file)) {
            Files.copy(file, response.getOutputStream());
        } else {
            getDefaultAvatar(response);
        }
    }
}