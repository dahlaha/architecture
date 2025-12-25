package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BookClubException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBookClubException(BookClubException ex, HttpServletRequest request) {
        logger.error("BookClub error: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", ex.getMessage());
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        logger.error("Entity not found: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", "Запрашиваемый ресурс не найден");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logger.error("Access denied: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("message", "У вас нет доступа к этому ресурсу");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "Произошла непредвиденная ошибка");
        mav.addObject("url", request.getRequestURL());
        return mav;
    }
} 