package com.moneyflow.moneyflow.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController { // Đổi tên thành CustomErrorController để tránh xung đột

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = "Đã xảy ra lỗi không xác định.";
        int statusCode = 500; // Mặc định là lỗi nội bộ server

        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorMessage = "Trang không tìm thấy (404).";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorMessage = "Bạn không có quyền truy cập trang này (403).";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorMessage = "Lỗi máy chủ nội bộ (500). Vui lòng thử lại sau.";
            }
            // Thêm các mã lỗi khác nếu cần
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("timestamp", java.time.LocalDateTime.now());
        model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

        return "error"; // Trả về tên view là error.html
    }
}