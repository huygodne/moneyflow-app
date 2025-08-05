package com.moneyflow.moneyflow.controller;

import com.moneyflow.moneyflow.dto.UserUpdateDTO;
import com.moneyflow.moneyflow.dto.PasswordChangeDTO; // THÊM IMPORT NÀY
import com.moneyflow.moneyflow.entity.User;
import com.moneyflow.moneyflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showProfile(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
        userUpdateDTO.setUsername(currentUser.getUsername());
        userUpdateDTO.setEmail(currentUser.getEmail());
        // --- CẬP NHẬT DÒNG NÀY ĐỂ THÊM FULLNAME ---
        userUpdateDTO.setFullName(currentUser.getFullName());
        // ------------------------------------------

        model.addAttribute("userUpdateDTO", userUpdateDTO);
        model.addAttribute("currentPage", "profile");
        return "user/profile";
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String updateProfile(@Valid @ModelAttribute("userUpdateDTO") UserUpdateDTO userUpdateDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng chưa được xác thực.");
            return "redirect:/login";
        }

        // Đảm bảo username không bị null khi binding lại form nếu có lỗi ở trường khác
        // (nếu username là readonly, nó sẽ không được gửi lại trong POST request nếu không có hidden field)
        // Dòng này đảm bảo userUpdateDTO có username để validate nếu cần
        if (userUpdateDTO.getUsername() == null || userUpdateDTO.getUsername().isEmpty()) {
            userUpdateDTO.setUsername(currentUser.getUsername());
        }

        if (result.hasErrors()) {
            model.addAttribute("currentPage", "profile");
            return "user/profile";
        }

        try {
            userService.updateUserProfile(currentUser, userUpdateDTO);
            redirectAttributes.addFlashAttribute("message", "Cập nhật thông tin cá nhân thành công!");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            result.rejectValue("email", "email.duplicate", e.getMessage());
            model.addAttribute("currentPage", "profile");
            return "user/profile";
        }
    }

    // --- THÊM CÁC PHƯƠNG THỨC CHO ĐỔI MẬT KHẨU ---

    @GetMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String showChangePasswordForm(Model model) {
        // Khởi tạo một DTO rỗng để bind vào form
        model.addAttribute("passwordChangeDTO", new PasswordChangeDTO());
        model.addAttribute("currentPage", "profile"); // Đánh dấu trang hiện tại cho navbar
        return "user/change-password"; // Tên view mới để hiển thị form đổi mật khẩu
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(@Valid @ModelAttribute("passwordChangeDTO") PasswordChangeDTO passwordChangeDTO,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng chưa được xác thực.");
            return "redirect:/login";
        }

        // Server-side validation: Kiểm tra mật khẩu mới và xác nhận mật khẩu có khớp nhau không
        if (!passwordChangeDTO.getNewPassword().equals(passwordChangeDTO.getConfirmNewPassword())) {
            result.rejectValue("confirmNewPassword", "password.mismatch", "Xác nhận mật khẩu mới không khớp.");
        }

        if (result.hasErrors()) {
            model.addAttribute("currentPage", "profile");
            return "user/change-password"; // Trở lại form nếu có lỗi validation
        }

        try {
            userService.changePassword(currentUser, passwordChangeDTO.getOldPassword(), passwordChangeDTO.getNewPassword());
            redirectAttributes.addFlashAttribute("message", "Đổi mật khẩu thành công!");
            return "redirect:/profile"; // Chuyển hướng về trang profile chính sau khi đổi mật khẩu
        } catch (IllegalArgumentException e) {
            // Xử lý lỗi từ UserService (ví dụ: mật khẩu cũ không đúng)
            if (e.getMessage().contains("Mật khẩu cũ không đúng")) {
                result.rejectValue("oldPassword", "password.old.invalid", e.getMessage());
            } else if (e.getMessage().contains("Mật khẩu mới không được trùng")) {
                result.rejectValue("newPassword", "password.new.sameasold", e.getMessage());
            } else {
                // Lỗi chung
                model.addAttribute("errorMessage", e.getMessage());
            }
            model.addAttribute("currentPage", "profile");
            return "user/change-password"; // Trở lại form với lỗi
        }
    }
    // ---------------------------------------------------------------------------------
}