package com.moneyflow.moneyflow.controller;

import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Hiển thị trang đăng ký
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User()); // Thêm một đối tượng User rỗng vào model
        return "register"; // Trả về tên view (file register.html)
    }

    // Xử lý yêu cầu đăng ký người dùng
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        try {
            userService.registerUser(user);
            return "redirect:/login?success"; // Đăng ký thành công, chuyển hướng đến trang đăng nhập
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage()); // Thêm thông báo lỗi vào model
            return "register"; // Quay lại trang đăng ký với lỗi
        }
    }

    // Hiển thị trang đăng nhập (đã được cấu hình trong SecurityConfig)
    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; // Trả về tên view (file login.html)
    }
}