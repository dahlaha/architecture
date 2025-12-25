package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = "Произошла непредвиденная ошибка";
        String errorDetails = "";
        String errorImage = "bi-exclamation-circle";
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            switch(statusCode) {
                case 404:
                    errorMessage = "Страница не найдена";
                    errorDetails = "Запрашиваемая страница не существует или была перемещена";
                    errorImage = "bi-question-circle";
                    break;
                case 403:
                    errorMessage = "Доступ запрещен";
                    errorDetails = "У вас нет прав для доступа к этой странице";
                    errorImage = "bi-lock";
                    break;
                case 500:
                    errorMessage = "Внутренняя ошибка сервера";
                    errorDetails = "Произошла ошибка при обработке вашего запроса";
                    errorImage = "bi-exclamation-triangle";
                    break;
                default:
                    errorMessage = "Произошла ошибка (" + statusCode + ")";
                    errorDetails = "Что-то пошло не так при обработке вашего запроса";
            }
            
            model.addAttribute("statusCode", statusCode);
        }
        
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("errorDetails", errorDetails);
        model.addAttribute("errorImage", errorImage);
        
        return "error";
    }
} 