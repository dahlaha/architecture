package com.example.demo.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Value("${upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        String uploadPathStr = uploadDir.toString().replace("\\", "/");
        
        if (!uploadPathStr.endsWith("/")) {
            uploadPathStr += "/";
        }
        
        logger.debug("Configuring upload path: {}", uploadPathStr);
        
        // Serve uploaded files
        registry.addResourceHandler("/uploads/**", "/images/covers/**")
                .addResourceLocations("file:" + uploadPathStr)
                .setCachePeriod(3600);
        
        // Serve static resources
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
                
        // Serve default images
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);
                
        logger.debug("Resource handlers configured successfully");
    }
} 